package com.example.ui.components

import android.app.Activity
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.model.ChannelEntity
import com.example.model.Episode
import com.example.ui.theme.LiveRed
import com.example.ui.theme.NetflixRed
import kotlinx.coroutines.delay

private fun android.content.Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

enum class ResizeMode(val mode: Int, val labelAr: String) {
    FIT(AspectRatioFrameLayout.RESIZE_MODE_FIT, "احتواء"),
    FILL(AspectRatioFrameLayout.RESIZE_MODE_FILL, "تعبئة الشاشة"),
    ZOOM(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, "تكبير")
}

data class AudioTrackOption(
    val trackGroup: androidx.media3.common.TrackGroup,
    val trackIndex: Int,
    val label: String,
    val isSelected: Boolean
)

@OptIn(UnstableApi::class)
@Composable
fun ExoPlayerView(
    mediaUrl: String,
    title: String,
    type: String, // "LIVE", "MOVIE", "EPISODE"
    channelList: List<ChannelEntity> = emptyList(),
    episodeList: List<Episode> = emptyList(),
    initialPlaybackPositionMs: Long = 0L,
    onClose: () -> Unit,
    onProgressUpdate: (positionMs: Long, durationMs: Long) -> Unit = { _, _ -> },
    onChannelSelect: (ChannelEntity) -> Unit = {},
    onEpisodeSelect: (Episode) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var isPlaying by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var isBuffering by remember { mutableStateOf(true) }
    var resizeModeState by remember { mutableStateOf(ResizeMode.FIT) }
    var showChannelDrawer by remember { mutableStateOf(false) }

    // Sleep Timer States
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var sleepTimerRemainingSeconds by remember { mutableIntStateOf(0) } // 0 means inactive

    // Playback Speed State
    var currentPlaybackSpeed by remember { mutableFloatStateOf(1.0f) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    // Google Cast States
    var showCastDialog by remember { mutableStateOf(false) }
    var connectedCastDevice by remember { mutableStateOf<String?>(null) }
    var showCastRemoteControl by remember { mutableStateOf(false) }
    var castVolume by remember { mutableFloatStateOf(0.8f) }
    var isCastMuted by remember { mutableStateOf(false) }

    // Audio Track Selection State (مثل ميزة "Audio Track" في XCIPTV)
    var availableAudioTracks by remember { mutableStateOf<List<AudioTrackOption>>(emptyList()) }
    var showAudioTrackDialog by remember { mutableStateOf(false) }

    // EPG Dialog State
    var showEpgDialog by remember { mutableStateOf(false) }
    val currentChannel = remember(mediaUrl, channelList) {
        channelList.firstOrNull { it.streamUrl == mediaUrl } ?: ChannelEntity(
            id = "current_ch",
            playlistId = 1L,
            name = title,
            streamUrl = mediaUrl
        )
    }

    // Auto-Rotate Screen to Landscape and Lock Immersive Fullscreen on Playback Start
    var isLandscapeMode by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        // Automatically flip screen to landscape for video playback
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        // Enable complete immersive fullscreen (hide status and navigation bars)
        val window = activity?.window
        if (window != null) {
            WindowCompat.getInsetsController(window, window.decorView).apply {
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.systemBars())
            }
        }

        onDispose {
            // Restore original orientation and show system bars when video player is closed
            activity?.requestedOrientation = originalOrientation
            val w = activity?.window
            if (w != null) {
                WindowCompat.getInsetsController(w, w.decorView).apply {
                    show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }
    }

    // ExoPlayer Instance
    val exoPlayer = remember {
        // EXTENSION_RENDERER_MODE_ON: يفضّل فك التشفير العتادي المدمج بالجهاز دائماً عند توفره،
        // ولا يلجأ إلى امتداد FFmpeg (يدعم AC3/EAC3/DTS) إلا للترميزات التي لا يدعمها الجهاز أصلاً
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        ExoPlayer.Builder(context, renderersFactory).build().apply {
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Sleep timer countdown logic
    LaunchedEffect(sleepTimerRemainingSeconds) {
        if (sleepTimerRemainingSeconds > 0) {
            delay(1000L)
            sleepTimerRemainingSeconds -= 1
            if (sleepTimerRemainingSeconds <= 0) {
                exoPlayer.pause()
                onClose()
            }
        }
    }

    // Intelligent Auto-Reconnect States
    var isAutoReconnecting by remember { mutableStateOf(false) }
    var reconnectAttempt by remember { mutableIntStateOf(0) }
    var hasFailedPermanently by remember { mutableStateOf(false) }
    val maxReconnectAttempts = 5

    // Auto-Reconnect Coroutine
    LaunchedEffect(isAutoReconnecting, reconnectAttempt) {
        if (isAutoReconnecting && reconnectAttempt <= maxReconnectAttempts) {
            delay(2500)
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    // مهلة للتخزين المؤقت (Buffering) اللانهائي: أحياناً لا يرمي المشغّل أي خطأ إطلاقاً
    // (لا onPlayerError) ويبقى عالقاً في STATE_BUFFERING للأبد بصمت — بدون هذه المهلة
    // لن تظهر رسالة الفشل أبداً حتى لو أضفناها لمسار onPlayerError فقط
    LaunchedEffect(isBuffering) {
        if (isBuffering) {
            delay(25_000)
            if (isBuffering && !hasFailedPermanently) {
                com.example.data.RemoteLogger.log(
                    level = "ERROR", tag = "PlayerDebug",
                    message = "Buffering timeout (25s) with no error/ready state. url=$mediaUrl"
                )
                isAutoReconnecting = false
                hasFailedPermanently = true
            }
        }
    }

    // Load Uri
    DisposableEffect(mediaUrl) {
        val mediaItem = MediaItem.fromUri(mediaUrl)
        exoPlayer.setMediaItem(mediaItem)
        if (initialPlaybackPositionMs > 0L) {
            exoPlayer.seekTo(initialPlaybackPositionMs)
        }
        exoPlayer.prepare()
        exoPlayer.play()

        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                if (reconnectAttempt < maxReconnectAttempts) {
                    isAutoReconnecting = true
                    reconnectAttempt += 1
                } else {
                    // استنفدنا كل المحاولات — أوقف الدوران اللانهائي وأظهر رسالة فشل واضحة بدل شاشة معلَّقة للأبد
                    isAutoReconnecting = false
                    hasFailedPermanently = true
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = (playbackState == Player.STATE_BUFFERING)
                if (playbackState == Player.STATE_READY) {
                    duration = exoPlayer.duration.coerceAtLeast(0L)
                    isAutoReconnecting = false
                    reconnectAttempt = 0
                }
                // تمرير تلقائي للحلقة التالية فور انتهاء الحلقة الحالية (مثل نتفليكس)
                if (playbackState == Player.STATE_ENDED && type == "EPISODE" && episodeList.isNotEmpty()) {
                    val currentIndex = episodeList.indexOfFirst { it.streamUrl == mediaUrl }
                    val nextEpisode = if (currentIndex >= 0) episodeList.getOrNull(currentIndex + 1) else null
                    if (nextEpisode != null) {
                        onEpisodeSelect(nextEpisode)
                    }
                }
            }

            // مسارات الصوت المتاحة فعلياً في هذا البث (مثل ميزة "Audio Track" في XCIPTV) —
            // بعض ملفات MKV/HLS تحتوي أكثر من مسار صوت (لغات/دوبلاج مختلفة)
            override fun onTracksChanged(tracks: Tracks) {
                val options = mutableListOf<AudioTrackOption>()
                val debugLines = mutableListOf<String>()
                var audioGroupCount = 0
                tracks.groups.forEach { group ->
                    if (group.type == C.TRACK_TYPE_AUDIO) {
                        audioGroupCount++
                        for (trackIndex in 0 until group.length) {
                            val format = group.getTrackFormat(trackIndex)
                            val supported = group.isTrackSupported(trackIndex)
                            debugLines.add(
                                "g=$audioGroupCount i=$trackIndex supported=$supported mime=${format.sampleMimeType} " +
                                    "lang=${format.language} label=${format.label} ch=${format.channelCount} selected=${group.isTrackSelected(trackIndex)}"
                            )
                            if (!supported) continue
                            val language = format.language
                            val displayName = when {
                                !format.label.isNullOrBlank() -> format.label!!
                                !language.isNullOrBlank() -> runCatching {
                                    java.util.Locale(language).displayLanguage
                                }.getOrNull()?.takeIf { it.isNotBlank() } ?: language
                                else -> "مسار صوت ${options.size + 1}"
                            }
                            options.add(
                                AudioTrackOption(
                                    trackGroup = group.mediaTrackGroup,
                                    trackIndex = trackIndex,
                                    label = displayName,
                                    isSelected = group.isTrackSelected(trackIndex)
                                )
                            )
                        }
                    }
                }
                availableAudioTracks = options
                com.example.data.RemoteLogger.log(
                    level = "DEBUG", tag = "AudioTrackDebug",
                    message = "url=$mediaUrl audioGroups=$audioGroupCount resolvedOptions=${options.size} | ${debugLines.joinToString(" || ")}"
                )
            }
        }

        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    fun selectAudioTrack(option: AudioTrackOption) {
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setOverrideForType(TrackSelectionOverride(option.trackGroup, option.trackIndex))
            .build()
        showAudioTrackDialog = false
    }

    // Position progress polling
    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
            duration = exoPlayer.duration.coerceAtLeast(0L)
            onProgressUpdate(currentPosition, duration)
            delay(1000)
        }
    }

    // Auto-hide controls after 4 seconds
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(4000)
            showControls = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showControls = !showControls
            }
    ) {
        // Player Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // Custom Compose Overlay UI
                    resizeMode = resizeModeState.mode
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.resizeMode = resizeModeState.mode
            },
            modifier = Modifier.fillMaxSize()
        )

        // فشل نهائي بعد استنفاد كل محاولات إعادة الاتصال — رسالة واضحة بدل تجمّد دائري للأبد
        if (hasFailedPermanently) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "تعذّر تشغيل هذا البث",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "تأكد من اتصال الإنترنت أو جرّب قناة/فيلماً آخر لاحقاً.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(
                            onClick = {
                                hasFailedPermanently = false
                                reconnectAttempt = 0
                                exoPlayer.prepare()
                                exoPlayer.play()
                            },
                            color = NetflixRed,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "إعادة المحاولة",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                            )
                        }
                        Surface(
                            onClick = onClose,
                            color = Color.White.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "إغلاق",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }

        // Minimal Loading Indicator & Intelligent Reconnection Status Overlay
        if (isAutoReconnecting || isBuffering) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 16.dp)
                    .align(Alignment.TopCenter),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NetflixRed.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = NetflixRed,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = if (isAutoReconnecting) 
                                "إعادة الاتصال الذكي بالبث... (محاولة $reconnectAttempt/$maxReconnectAttempts)" 
                            else 
                                "جاري تحميل البث وتثبيت الإشارة...",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Custom Controls Overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
            ) {
                // Top Control Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .align(Alignment.TopStart),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق المشغل",
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = title,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (type == "LIVE") {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = LiveRed,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "بث مباشر HD",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Google Cast Button
                        IconButton(
                            onClick = { showCastDialog = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (connectedCastDevice != null) NetflixRed else Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = if (connectedCastDevice != null) Icons.Default.CastConnected else Icons.Default.Cast,
                                contentDescription = "بث الشاشة",
                                tint = Color.White
                            )
                        }

                        // Sleep Timer Button
                        IconButton(
                            onClick = { showSleepTimerDialog = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (sleepTimerRemainingSeconds > 0) NetflixRed else Color.Black.copy(alpha = 0.5f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "مؤقت النوم",
                                    tint = Color.White
                                )
                                if (sleepTimerRemainingSeconds > 0) {
                                    val remMin = sleepTimerRemainingSeconds / 60
                                    val remSec = sleepTimerRemainingSeconds % 60
                                    Text(
                                        text = String.format("%02d:%02d", remMin, remSec),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(start = 2.dp)
                                    )
                                }
                            }
                        }

                        // EPG Schedule Guide Button (For Live TV)
                        if (type == "LIVE") {
                            IconButton(
                                onClick = { showEpgDialog = true },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "دليل البرامج",
                                    tint = Color.White
                                )
                            }
                        }

                        // Audio Track Selector — يظهر فقط إن كان البث يحتوي أكثر من مسار صوت واحد فعلياً
                        if (availableAudioTracks.size > 1) {
                            IconButton(
                                onClick = { showAudioTrackDialog = true },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Audiotrack,
                                    contentDescription = "مسار الصوت",
                                    tint = Color.White
                                )
                            }
                        }

                        // Playback Speed Selector (For VOD Content)
                        if (type != "LIVE") {
                            IconButton(
                                onClick = { showSpeedDialog = true },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (currentPlaybackSpeed != 1.0f) NetflixRed else Color.Black.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "${currentPlaybackSpeed}x",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Specialized Cast Remote Control button when connected
                        if (connectedCastDevice != null) {
                            IconButton(
                                onClick = { showCastRemoteControl = true },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(NetflixRed)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SettingsRemote,
                                    contentDescription = "ريموت البث",
                                    tint = Color.White
                                )
                            }
                        }

                        // Screen Rotation Toggle Button
                        IconButton(
                            onClick = {
                                val act = context.findActivity()
                                if (act != null) {
                                    if (isLandscapeMode) {
                                        act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                        isLandscapeMode = false
                                    } else {
                                        act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                        isLandscapeMode = true
                                    }
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (!isLandscapeMode) NetflixRed else Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = if (isLandscapeMode) Icons.Default.ScreenRotation else Icons.Default.ScreenLockLandscape,
                                contentDescription = "تدوير الشاشة",
                                tint = Color.White
                            )
                        }

                        // Aspect Ratio Switcher Button
                        IconButton(
                            onClick = {
                                resizeModeState = when (resizeModeState) {
                                    ResizeMode.FIT -> ResizeMode.FILL
                                    ResizeMode.FILL -> ResizeMode.ZOOM
                                    ResizeMode.ZOOM -> ResizeMode.FIT
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = "أبعاد الشاشة",
                                tint = Color.White
                            )
                        }

                        // Live TV channel quick drawer button
                        if (type == "LIVE" && channelList.isNotEmpty()) {
                            IconButton(
                                onClick = { showChannelDrawer = !showChannelDrawer },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(NetflixRed)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tv,
                                    contentDescription = "قائمة القنوات",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                // Center Play / Pause & Skip 10s Buttons
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                            exoPlayer.seekTo(newPos)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "تراجع 10 ثواني",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                            } else {
                                exoPlayer.play()
                            }
                        },
                        modifier = Modifier
                            .size(68.dp)
                            .background(NetflixRed, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "إيقاف" else "تشغيل",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(duration)
                            exoPlayer.seekTo(newPos)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "تقديم 10 ثواني",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Bottom Timeline Control Bar (for VOD Movies / Series)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    if (type != "LIVE" && duration > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatTimeMs(currentPosition),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formatTimeMs(duration),
                                color = Color.LightGray,
                                fontSize = 12.sp
                            )
                        }

                        Slider(
                            value = currentPosition.toFloat(),
                            onValueChange = { newPos ->
                                currentPosition = newPos.toLong()
                            },
                            onValueChangeFinished = {
                                exoPlayer.seekTo(currentPosition)
                            },
                            valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = NetflixRed,
                                activeTrackColor = NetflixRed,
                                inactiveTrackColor = Color.Gray.copy(alpha = 0.5f)
                            )
                        )
                    }

                    Text(
                        text = "وضع الشاشة: ${resizeModeState.labelAr}",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }

        // Live Channel Switcher Drawer (Side Overlay)
        if (showChannelDrawer && type == "LIVE") {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(260.dp)
                    .align(Alignment.CenterEnd)
                    .background(Color(0xFF141414).copy(alpha = 0.95f))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "قائمة القنوات المباشرة",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        IconButton(onClick = { showChannelDrawer = false }) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                        }
                    }

                    HorizontalDivider(color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(channelList) { ch ->
                            val isCurrent = ch.streamUrl == mediaUrl
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onChannelSelect(ch)
                                        showChannelDrawer = false
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCurrent) NetflixRed else Color(0xFF222222)
                                ),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tv,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = ch.name,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = ch.category,
                                            color = Color.LightGray,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sleep Timer Selection Dialog
        if (showSleepTimerDialog) {
            AlertDialog(
                onDismissRequest = { showSleepTimerDialog = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
                modifier = Modifier.fillMaxWidth(0.95f),
                containerColor = Color(0xFF1E1E24),
                titleContentColor = Color.White,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = NetflixRed)
                        Text("مؤقت إيقاف التشغيل (Sleep Timer)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("حدد المدة الزمنية لإيقاف البث تلقائياً:", fontSize = 13.sp, color = Color.LightGray)

                        val options = listOf(
                            15 to "15 دقيقة",
                            30 to "30 دقيقة",
                            60 to "60 دقيقة (ساعة)",
                            120 to "120 دقيقة (ساعتان)"
                        )

                        options.forEach { (minutes, label) ->
                            Card(
                                onClick = {
                                    sleepTimerRemainingSeconds = minutes * 60
                                    showSleepTimerDialog = false
                                },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF282830)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(label, color = Color.White, fontWeight = FontWeight.Medium)
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                                }
                            }
                        }

                        if (sleepTimerRemainingSeconds > 0) {
                            Button(
                                onClick = {
                                    sleepTimerRemainingSeconds = 0
                                    showSleepTimerDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NetflixRed),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("إلغاء المؤقت النشط", color = Color.White)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showSleepTimerDialog = false }) {
                        Text("إلغاء", color = Color.Gray)
                    }
                }
            )
        }

        // Playback Speed Selection Dialog
        if (showSpeedDialog) {
            AlertDialog(
                onDismissRequest = { showSpeedDialog = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
                modifier = Modifier.fillMaxWidth(0.95f),
                containerColor = Color(0xFF1E1E24),
                titleContentColor = Color.White,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = NetflixRed)
                        Text("سرعة التشغيل", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val speeds = listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f)
                        speeds.forEach { speed ->
                            val isSelected = currentPlaybackSpeed == speed
                            Card(
                                onClick = {
                                    currentPlaybackSpeed = speed
                                    exoPlayer.setPlaybackSpeed(speed)
                                    showSpeedDialog = false
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) NetflixRed.copy(alpha = 0.2f) else Color(0xFF282830)
                                ),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, NetflixRed) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (speed == 1.0f) "1.0x (العادية)" else "${speed}x",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = NetflixRed)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showSpeedDialog = false }) {
                        Text("إلغاء", color = Color.Gray)
                    }
                }
            )
        }

        // Audio Track Selection Dialog (مثل ميزة "Audio Track" في XCIPTV)
        if (showAudioTrackDialog) {
            AlertDialog(
                onDismissRequest = { showAudioTrackDialog = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
                modifier = Modifier.fillMaxWidth(0.95f),
                containerColor = Color(0xFF1E1E24),
                titleContentColor = Color.White,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Audiotrack, contentDescription = null, tint = NetflixRed)
                        Text("مسار الصوت (Audio Track)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        availableAudioTracks.forEach { option ->
                            Card(
                                onClick = { selectAudioTrack(option) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (option.isSelected) NetflixRed.copy(alpha = 0.2f) else Color(0xFF282830)
                                ),
                                border = if (option.isSelected) androidx.compose.foundation.BorderStroke(1.dp, NetflixRed) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(option.label, color = Color.White, fontWeight = FontWeight.Medium)
                                    if (option.isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = NetflixRed)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showAudioTrackDialog = false }) {
                        Text("إلغاء", color = Color.Gray)
                    }
                }
            )
        }

        // Specialized Cast Remote Control Overlay
        if (showCastRemoteControl && connectedCastDevice != null) {
            AlertDialog(
                onDismissRequest = { showCastRemoteControl = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
                modifier = Modifier.fillMaxWidth(0.95f),
                containerColor = Color(0xFF18181C),
                titleContentColor = Color.White,
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CastConnected, contentDescription = null, tint = NetflixRed)
                            Column {
                                Text("ريموت التحكم في البث", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(connectedCastDevice!!, fontSize = 11.sp, color = Color.LightGray)
                            }
                        }
                        IconButton(onClick = { showCastRemoteControl = false }) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.Gray)
                        }
                    }
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Surface(
                            color = Color(0xFF25252B),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "جاري العرض الآن على التلفاز:",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Remote Playback Controls
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val newPos = (currentPosition - 10000L).coerceAtLeast(0L)
                                    currentPosition = newPos
                                    exoPlayer.seekTo(newPos)
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2C2C34))
                            ) {
                                Icon(Icons.Default.Replay10, contentDescription = "تراجع 10 ثوان", tint = Color.White)
                            }

                            IconButton(
                                onClick = {
                                    if (isPlaying) {
                                        exoPlayer.pause()
                                    } else {
                                        exoPlayer.play()
                                    }
                                },
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(NetflixRed)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "تشغيل / إيقاف",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    val newPos = (currentPosition + 10000L).coerceAtMost(if (duration > 0) duration else Long.MAX_VALUE)
                                    currentPosition = newPos
                                    exoPlayer.seekTo(newPos)
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2C2C34))
                            ) {
                                Icon(Icons.Default.Forward10, contentDescription = "تقدم 10 ثوان", tint = Color.White)
                            }
                        }

                        // Volume Controls
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(onClick = { isCastMuted = !isCastMuted }) {
                                    Icon(
                                        imageVector = if (isCastMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                        contentDescription = "كتم الصوت",
                                        tint = if (isCastMuted) NetflixRed else Color.White
                                    )
                                }

                                Slider(
                                    value = if (isCastMuted) 0f else castVolume,
                                    onValueChange = {
                                        castVolume = it
                                        isCastMuted = false
                                    },
                                    colors = SliderDefaults.colors(
                                        thumbColor = NetflixRed,
                                        activeTrackColor = NetflixRed,
                                        inactiveTrackColor = Color.DarkGray
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                Text(
                                    text = "${((if (isCastMuted) 0f else castVolume) * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    color = Color.LightGray,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }

                        // Disconnect button
                        OutlinedButton(
                            onClick = {
                                connectedCastDevice = null
                                showCastRemoteControl = false
                            },
                            border = androidx.compose.foundation.BorderStroke(1.dp, NetflixRed),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CastConnected, contentDescription = null, tint = NetflixRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("قطع الاتصال بالجهاز", color = NetflixRed, fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showCastRemoteControl = false }) {
                        Text("إغلاق الريموت", color = Color.Gray)
                    }
                }
            )
        }

        // Google Cast Dialog
        if (showCastDialog) {
            GoogleCastDialog(
                currentConnectedDevice = connectedCastDevice,
                mediaTitle = title,
                onConnectDevice = { devName ->
                    connectedCastDevice = devName
                    showCastDialog = false
                },
                onDisconnect = {
                    connectedCastDevice = null
                    showCastDialog = false
                },
                onDismiss = { showCastDialog = false }
            )
        }

        // EPG Schedule Dialog
        if (showEpgDialog && type == "LIVE") {
            EpgScheduleDialog(
                channel = currentChannel,
                onDismiss = { showEpgDialog = false },
                onPlayChannel = { showEpgDialog = false }
            )
        }
    }
}

private fun formatTimeMs(timeMs: Long): String {
    val totalSeconds = (timeMs / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hours = minutes / 60
    return if (hours > 0) {
        val remMin = minutes % 60
        String.format("%d:%02d:%02d", hours, remMin, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
