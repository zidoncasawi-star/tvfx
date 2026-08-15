package com.example.tv.ui.components

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
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
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.example.model.ChannelEntity
import com.example.model.Episode
import com.example.tv.theme.TvCard
import com.example.tv.theme.TvFocusBorder
import com.example.tv.theme.TvRed
import com.example.tv.theme.TvTextGray
import kotlinx.coroutines.delay

private data class TvAudioTrackOption(
    val trackGroup: androidx.media3.common.TrackGroup,
    val trackIndex: Int,
    val label: String,
    val isSelected: Boolean
)

private enum class TvResizeMode(val mode: Int, val label: String) {
    FIT(AspectRatioFrameLayout.RESIZE_MODE_FIT, "Fit"),
    FILL(AspectRatioFrameLayout.RESIZE_MODE_FILL, "Fill"),
    ZOOM(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, "Zoom")
}

/**
 * مشغّل فيديو مخصّص للتلفاز — يعيد بناء كل ما يوفّره ExoPlayerView الخاص بالهاتف، لكن بأزرار
 * قابلة للتركيز عبر D-pad (نفس نمط TvFocusable/Surface المستخدم في بقية شاشات التلفاز) بدل
 * IconButton العادي غير المخصص للتنقل بالريموت. زر الرجوع (Back) يُعالَج من الخارج في
 * TvMainActivity عبر BackHandler، وليس هنا.
 */
@OptIn(UnstableApi::class)
@Composable
fun TvPlayerView(
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
    val context = androidx.compose.ui.platform.LocalContext.current

    var isPlaying by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var isBuffering by remember { mutableStateOf(true) }
    var resizeModeState by remember { mutableStateOf(TvResizeMode.FIT) }
    var showChannelDrawer by remember { mutableStateOf(false) }

    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var sleepTimerRemainingSeconds by remember { mutableIntStateOf(0) }

    var availableAudioTracks by remember { mutableStateOf<List<TvAudioTrackOption>>(emptyList()) }
    var showAudioTrackDialog by remember { mutableStateOf(false) }

    val playPauseFocusRequester = remember { FocusRequester() }
    val rootFocusRequester = remember { FocusRequester() }
    val channelDrawerFirstItemFocusRequester = remember { FocusRequester() }

    // بدون تركيز أوّلي صريح عند فتح قائمة تبديل القنوات، يبقى التركيز منطقياً على زر الفتح
    // (خلف القائمة الآن) فتضيع كل ضغطات D-pad ولا يمكن التمرير داخل القائمة أو اختيار قناة
    LaunchedEffect(showChannelDrawer) {
        if (showChannelDrawer) {
            delay(80)
            runCatching { channelDrawerFirstItemFocusRequester.requestFocus() }
        }
    }

    // إخفاء أشرطة النظام فقط — التلفاز أفقي دائماً بلا حاجة لتدوير الشاشة
    DisposableEffect(Unit) {
        val activity = context as? android.app.Activity
        val window = activity?.window
        if (window != null) {
            WindowCompat.getInsetsController(window, window.decorView).apply {
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            val w = activity?.window
            if (w != null) {
                WindowCompat.getInsetsController(w, w.decorView).apply {
                    show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }
    }

    val exoPlayer = remember {
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        ExoPlayer.Builder(context, renderersFactory).build().apply {
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

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

    var isAutoReconnecting by remember { mutableStateOf(false) }
    var reconnectAttempt by remember { mutableIntStateOf(0) }
    var hasFailedPermanently by remember { mutableStateOf(false) }
    val maxReconnectAttempts = 5

    LaunchedEffect(isAutoReconnecting, reconnectAttempt) {
        if (isAutoReconnecting && reconnectAttempt <= maxReconnectAttempts) {
            delay(2500)
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    LaunchedEffect(isBuffering) {
        if (isBuffering) {
            delay(25_000)
            if (isBuffering && !hasFailedPermanently) {
                com.example.data.RemoteLogger.log(
                    level = "ERROR", tag = "TvPlayerDebug",
                    message = "Buffering timeout (25s) with no error/ready state. url=$mediaUrl"
                )
                isAutoReconnecting = false
                hasFailedPermanently = true
            }
        }
    }

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
                if (playbackState == Player.STATE_ENDED && type == "EPISODE" && episodeList.isNotEmpty()) {
                    val currentIndex = episodeList.indexOfFirst { it.streamUrl == mediaUrl }
                    val nextEpisode = if (currentIndex >= 0) episodeList.getOrNull(currentIndex + 1) else null
                    if (nextEpisode != null) onEpisodeSelect(nextEpisode)
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                val options = mutableListOf<TvAudioTrackOption>()
                tracks.groups.forEach { group ->
                    if (group.type == C.TRACK_TYPE_AUDIO) {
                        for (trackIndex in 0 until group.length) {
                            if (!group.isTrackSupported(trackIndex)) continue
                            val format = group.getTrackFormat(trackIndex)
                            val language = format.language
                            val languageName = if (!language.isNullOrBlank()) {
                                runCatching { java.util.Locale(language).displayLanguage }
                                    .getOrNull()?.takeIf { it.isNotBlank() } ?: language
                            } else null
                            val rawLabel = format.label?.takeIf { it.isNotBlank() }
                            val displayName = when {
                                rawLabel != null && languageName != null && !rawLabel.equals(languageName, ignoreCase = true) ->
                                    "$rawLabel ($languageName)"
                                rawLabel != null -> rawLabel
                                languageName != null -> languageName
                                else -> "Audio ${options.size + 1}"
                            }
                            options.add(
                                TvAudioTrackOption(
                                    trackGroup = group.mediaTrackGroup,
                                    trackIndex = trackIndex,
                                    label = displayName,
                                    isSelected = group.isTrackSelected(trackIndex)
                                )
                            )
                        }
                    }
                }
                val nameCounts = options.groupingBy { it.label }.eachCount()
                val seenSoFar = mutableMapOf<String, Int>()
                availableAudioTracks = options.map { option ->
                    if ((nameCounts[option.label] ?: 0) > 1) {
                        val nextIndex = (seenSoFar[option.label] ?: 0) + 1
                        seenSoFar[option.label] = nextIndex
                        option.copy(label = "${option.label} ($nextIndex)")
                    } else option
                }
            }
        }

        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    fun selectAudioTrack(option: TvAudioTrackOption) {
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setOverrideForType(TrackSelectionOverride(option.trackGroup, option.trackIndex))
            .build()
        showAudioTrackDialog = false
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
            duration = exoPlayer.duration.coerceAtLeast(0L)
            onProgressUpdate(currentPosition, duration)
            delay(1000)
        }
    }

    // إخفاء تلقائي للتحكم بعد 6 ثوان أثناء التشغيل — أطول قليلاً من الهاتف لأن الريموت أبطأ من اللمس
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(6000)
            showControls = false
        }
    }

    // عند ظهور التحكم نركّز زر التشغيل/الإيقاف (نقطة دخول D-pad)، وعند إخفائه نعيد التركيز لجذر
    // الشاشة نفسها حتى تبقى قادرة على استقبال أي ضغطة ريموت وإظهار التحكم من جديد — بدونها، بعد
    // اختفاء الأزرار لا يوجد أي عنصر مركَّز فتضيع كل ضغطات D-pad ولا تصل أبداً لمعالج onKeyEvent
    LaunchedEffect(showControls) {
        delay(50)
        if (showControls) {
            runCatching { playPauseFocusRequester.requestFocus() }
        } else {
            runCatching { rootFocusRequester.requestFocus() }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(rootFocusRequester)
            .focusable()
            .onKeyEvent { event ->
                // أي ضغطة D-pad (باستثناء الرجوع الذي تُديره TvMainActivity) تُظهر التحكم مجدداً
                if (event.type == KeyEventType.KeyDown && !showControls &&
                    event.key != Key.Back
                ) {
                    showControls = true
                    true
                } else false
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = resizeModeState.mode
                    layoutParams = android.widget.FrameLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView -> playerView.resizeMode = resizeModeState.mode },
            modifier = Modifier.fillMaxSize()
        )

        if (hasFailedPermanently) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Couldn't play this stream", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Check your internet connection or try another channel/title later.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        TvPrimaryButton(
                            text = "Retry",
                            onClick = {
                                hasFailedPermanently = false
                                reconnectAttempt = 0
                                exoPlayer.prepare()
                                exoPlayer.play()
                            }
                        )
                        TvOutlineButton(text = "Close", onClick = onClose)
                    }
                }
            }
        }

        if (isAutoReconnecting || isBuffering) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp).align(Alignment.TopCenter),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TvRed, strokeWidth = 2.dp)
                        Text(
                            text = if (isAutoReconnecting) "Reconnecting... ($reconnectAttempt/$maxReconnectAttempts)" else "Buffering...",
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

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
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp, vertical = 20.dp)
                        .align(Alignment.TopStart),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        TvIconButton(icon = Icons.Default.Close, contentDescription = "Close player", onClick = onClose)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                text = title,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (type == "LIVE") {
                                Box(
                                    modifier = Modifier.background(TvRed, RoundedCornerShape(4.dp))
                                ) {
                                    Text(
                                        text = "LIVE HD",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        TvIconButton(
                            icon = Icons.Default.Timer,
                            contentDescription = "Sleep timer",
                            active = sleepTimerRemainingSeconds > 0,
                            onClick = { showSleepTimerDialog = true }
                        )
                        if (availableAudioTracks.size > 1) {
                            TvIconButton(
                                icon = Icons.Default.Audiotrack,
                                contentDescription = "Audio track",
                                onClick = { showAudioTrackDialog = true }
                            )
                        }
                        TvIconButton(
                            icon = Icons.Default.AspectRatio,
                            contentDescription = "Aspect ratio",
                            onClick = {
                                resizeModeState = when (resizeModeState) {
                                    TvResizeMode.FIT -> TvResizeMode.FILL
                                    TvResizeMode.FILL -> TvResizeMode.ZOOM
                                    TvResizeMode.ZOOM -> TvResizeMode.FIT
                                }
                            }
                        )
                        if (type == "LIVE" && channelList.isNotEmpty()) {
                            TvIconButton(
                                icon = Icons.Default.Tv,
                                contentDescription = "Channel list",
                                active = showChannelDrawer,
                                onClick = { showChannelDrawer = !showChannelDrawer }
                            )
                        }
                    }
                }

                // Center transport controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(40.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TvIconButton(
                        icon = Icons.Default.Replay10,
                        contentDescription = "Rewind 10 seconds",
                        size = 52.dp,
                        iconSize = 26.dp,
                        onClick = {
                            val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                            exoPlayer.seekTo(newPos)
                        }
                    )

                    Surface(
                        onClick = {
                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                        },
                        modifier = Modifier
                            .size(76.dp)
                            .focusRequester(playPauseFocusRequester),
                        shape = ClickableSurfaceDefaults.shape(shape = CircleShape),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = TvRed,
                            focusedContainerColor = Color(0xFFFF1A24)
                        ),
                        border = ClickableSurfaceDefaults.border(
                            border = Border(BorderStroke(0.dp, Color.Transparent)),
                            focusedBorder = Border(BorderStroke(3.dp, TvFocusBorder))
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    TvIconButton(
                        icon = Icons.Default.Forward10,
                        contentDescription = "Forward 10 seconds",
                        size = 52.dp,
                        iconSize = 26.dp,
                        onClick = {
                            val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(duration)
                            exoPlayer.seekTo(newPos)
                        }
                    )
                }

                // Bottom timeline (VOD only)
                if (type != "LIVE" && duration > 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp, vertical = 20.dp)
                            .align(Alignment.BottomCenter)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(formatTvTimeMs(currentPosition), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(formatTvTimeMs(duration), color = Color.LightGray, fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(6.dp))
                        val progress = (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(3.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .fillMaxHeight()
                                    .background(TvRed, RoundedCornerShape(3.dp))
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Use ⏪ / ⏩ to seek · Screen: ${resizeModeState.label}",
                            color = TvTextGray,
                            fontSize = 11.sp,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
        }

        // Live channel switcher drawer
        if (showChannelDrawer && type == "LIVE") {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(340.dp)
                    .align(Alignment.CenterEnd)
                    .background(Color(0xFF141414).copy(alpha = 0.96f))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Live Channels", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        TvIconButton(icon = Icons.Default.Close, contentDescription = "Close list", onClick = { showChannelDrawer = false })
                    }
                    Spacer(Modifier.height(10.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(channelList) { index, ch ->
                            val isCurrent = ch.streamUrl == mediaUrl
                            TvFocusable(
                                onClick = {
                                    onChannelSelect(ch)
                                    showChannelDrawer = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(if (index == 0) Modifier.focusRequester(channelDrawerFirstItemFocusRequester) else Modifier),
                                shape = RoundedCornerShape(8.dp)
                            ) { focused ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isCurrent) TvRed else Color.Transparent)
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Tv, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            ch.name,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(ch.category, color = Color.LightGray, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showSleepTimerDialog) {
            TvSimpleDialog(title = "Sleep Timer", onDismiss = { showSleepTimerDialog = false }) {
                val options = listOf(15 to "15 minutes", 30 to "30 minutes", 60 to "60 minutes", 120 to "2 hours")
                options.forEach { (minutes, label) ->
                    TvDialogOptionRow(
                        label = label,
                        selected = false,
                        onClick = {
                            sleepTimerRemainingSeconds = minutes * 60
                            showSleepTimerDialog = false
                        }
                    )
                }
                if (sleepTimerRemainingSeconds > 0) {
                    Spacer(Modifier.height(8.dp))
                    TvPrimaryButton(
                        text = "Cancel active timer",
                        onClick = {
                            sleepTimerRemainingSeconds = 0
                            showSleepTimerDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (showAudioTrackDialog) {
            TvSimpleDialog(title = "Audio Track", onDismiss = { showAudioTrackDialog = false }) {
                availableAudioTracks.forEach { option ->
                    TvDialogOptionRow(
                        label = option.label,
                        selected = option.isSelected,
                        onClick = { selectAudioTrack(option) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TvIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    active: Boolean = false,
    size: androidx.compose.ui.unit.Dp = 44.dp,
    iconSize: androidx.compose.ui.unit.Dp = 22.dp
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = Modifier.size(size).onFocusChanged { focused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(shape = CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (active) TvRed else Color.Black.copy(alpha = 0.5f),
            focusedContainerColor = if (active) TvRed else Color.White.copy(alpha = 0.22f)
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(0.dp, Color.Transparent)),
            focusedBorder = Border(BorderStroke(2.dp, TvFocusBorder))
        )
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(imageVector = icon, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(iconSize))
        }
    }
}

@Composable
private fun TvSimpleDialog(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(440.dp)
                .background(TvCard, RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    TvIconButton(icon = Icons.Default.Close, contentDescription = "Close", onClick = onDismiss, size = 34.dp, iconSize = 16.dp)
                }
                Spacer(Modifier.height(14.dp))
                content()
            }
        }
    }
}

@Composable
private fun TvDialogOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    TvFocusable(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(8.dp)) { _ ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (selected) TvRed.copy(alpha = 0.2f) else Color.Transparent)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = TvRed)
        }
    }
}

private fun formatTvTimeMs(timeMs: Long): String {
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
