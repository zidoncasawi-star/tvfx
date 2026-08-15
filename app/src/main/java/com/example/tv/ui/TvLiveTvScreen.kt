package com.example.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.example.data.ShortEpgEntry
import com.example.model.ChannelEntity
import com.example.model.XtreamCategoryEntity
import com.example.tv.theme.TvBg
import com.example.tv.theme.TvCard
import com.example.tv.theme.TvFocusBorder
import com.example.tv.theme.TvPanel
import com.example.tv.theme.TvRed
import com.example.tv.theme.TvTextGray
import com.example.tv.theme.TvTextWhite
import com.example.tv.ui.components.TvFocusable
import com.example.tv.ui.components.TvTextField
import com.example.ui.components.InlinePlayer

/** شاشة القنوات المباشرة — نفس تخطيط .tv-layout ثلاثي الأعمدة (تصنيفات | قنوات | معاينة+EPG). */
@Composable
fun TvLiveTvScreen(
    channels: List<ChannelEntity>,
    categories: List<XtreamCategoryEntity>,
    onLoadCategoryStreams: (categoryId: String) -> Unit,
    onFetchEpg: suspend (ChannelEntity) -> List<ShortEpgEntry> = { emptyList() },
    onPlayChannel: (ChannelEntity) -> Unit,
    isChannelRecording: (String) -> Boolean = { false },
    onStartRecording: (ChannelEntity, String, Int) -> Unit = { _, _, _ -> },
    onStopRecording: (ChannelEntity) -> Unit = {},
    isPreviewPaused: Boolean = false
) {
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    // القناة المركَّز عليها حالياً بالريموت — تُشغَّل كمعاينة مصغّرة ويُجلب EPG الخاص بها، دون فتح
    // المشغّل الرئيسي بملء الشاشة (والذي يفتح فقط عند الضغط الفعلي/OK على القناة)
    var previewChannel by remember { mutableStateOf<ChannelEntity?>(channels.firstOrNull()) }
    var currentEpg by remember { mutableStateOf<List<ShortEpgEntry>>(emptyList()) }

    // كان الانتقال بالريموت من عمود القنوات يميناً نحو زر التسجيل/قسم EPG يفشل غالباً — البحث
    // المكاني التلقائي في Compose لا يجد مساراً مضموناً عبر الفجوة الكبيرة بين الأعمدة. نُحدِّد
    // هذا المسار صراحة بدل الاعتماد فقط على الهندسة المكانية التلقائية
    val recordButtonFocusRequester = remember { FocusRequester() }
    val epgNowPlayingFocusRequester = remember { FocusRequester() }

    // كانت قائمة القنوات تعرض جميع القنوات المحمَّلة محلياً دائماً بلا أي فلترة حسب التصنيف
    // المختار — الضغط على تصنيف كان يُبرزه أحمر (تركيز فقط) ويطلب تحميل قنواته، لكن العمود
    // كان يستمر بعرض نفس القائمة غير المُصنَّفة القديمة. نفس منطق فلترة الهاتف تماماً هنا.
    val filtered = remember(channels, search, selectedCategoryId) {
        val base = if (selectedCategoryId == null) channels else channels.filter { it.category == selectedCategoryId }
        if (search.isBlank()) base else base.filter { it.name.contains(search, ignoreCase = true) }
    }

    // إن لم تعد القناة المعاينة حالياً ضمن قائمة التصنيف الجديد (أو عند أول تحميل)، نعاين
    // أول قناة في القائمة الجديدة بدل الإبقاء على معاينة قناة من تصنيف آخر لم يعد ظاهراً
    LaunchedEffect(filtered) {
        if (filtered.isNotEmpty() && previewChannel?.id !in filtered.map { it.id }) {
            previewChannel = filtered.first()
        }
    }

    LaunchedEffect(previewChannel?.id) {
        currentEpg = previewChannel?.let { runCatching { onFetchEpg(it) }.getOrDefault(emptyList()) } ?: emptyList()
    }

    Row(modifier = Modifier.fillMaxSize().background(TvBg)) {
        // تصنيفات
        LazyColumn(
            modifier = Modifier
                .width(220.dp)
                .fillMaxHeight()
                .background(TvPanel)
                .padding(vertical = 16.dp, horizontal = 10.dp)
                .focusRestorer()
        ) {
            items(categories, key = { it.id }) { cat ->
                val active = selectedCategoryId == cat.id
                TvFocusable(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    onClick = {
                        selectedCategoryId = cat.id
                        onLoadCategoryStreams(cat.id)
                    }
                ) { _ ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (active) TvRed else Color.Transparent)
                            .padding(horizontal = 14.dp, vertical = 11.dp)
                    ) {
                        Text(
                            cat.name,
                            color = if (active) TvTextWhite else TvTextGray,
                            fontSize = 13.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // قائمة القنوات
        Column(modifier = Modifier.fillMaxHeight().width(320.dp)) {
            TvTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = "Search in category...",
                modifier = Modifier.fillMaxWidth().padding(14.dp)
            )
            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading channels...", color = TvTextGray, fontSize = 13.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp).focusRestorer()) {
                    items(filtered, key = { it.id }) { ch ->
                        TvChannelRow(
                            channel = ch,
                            isPreviewing = previewChannel?.id == ch.id,
                            onFocus = { previewChannel = ch },
                            onClick = { onPlayChannel(ch) },
                            rightFocusRequester = recordButtonFocusRequester
                        )
                    }
                }
            }
        }

        // معاينة مصغّرة + EPG للقناة المركَّز عليها حالياً
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            val channel = previewChannel
            if (channel == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Select a channel to preview", color = TvTextGray, fontSize = 14.sp)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black)
                ) {
                    InlinePlayer(
                        mediaUrl = channel.streamUrl,
                        isPaused = isPreviewPaused,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .background(TvRed, RoundedCornerShape(4.dp))
                    ) {
                        Text(
                            "LIVE",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            channel.name,
                            color = TvTextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Press OK on the channel to watch fullscreen",
                            color = TvTextGray,
                            fontSize = 12.sp
                        )
                    }
                    val recording = isChannelRecording(channel.id)
                    val hasNowPlayingEpgButton = currentEpg.any { it.isNowPlaying }
                    TvRecordButton(
                        isRecording = recording,
                        onClick = {
                            if (recording) onStopRecording(channel)
                            else onStartRecording(channel, channel.name, 120)
                        },
                        modifier = Modifier
                            .focusRequester(recordButtonFocusRequester)
                            .focusProperties {
                                if (hasNowPlayingEpgButton) down = epgNowPlayingFocusRequester
                            }
                    )
                }

                Spacer(Modifier.height(18.dp))
                Text("Program Guide", color = TvTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                if (currentEpg.isEmpty()) {
                    Text("No program guide data available for this channel.", color = TvTextGray, fontSize = 12.sp)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(currentEpg, key = { "${it.title}_${it.start}" }) { entry ->
                            TvEpgRow(
                                entry = entry,
                                isRecording = isChannelRecording(channel.id) && entry.isNowPlaying,
                                onRecordClick = {
                                    if (isChannelRecording(channel.id)) onStopRecording(channel)
                                    else onStartRecording(channel, entry.title, estimateEpgDurationMinutes(entry))
                                },
                                recordButtonModifier = if (entry.isNowPlaying) {
                                    Modifier
                                        .focusRequester(epgNowPlayingFocusRequester)
                                        .focusProperties { up = recordButtonFocusRequester }
                                } else Modifier
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvEpgRow(
    entry: ShortEpgEntry,
    isRecording: Boolean,
    onRecordClick: () -> Unit,
    recordButtonModifier: Modifier = Modifier
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (entry.isNowPlaying) TvCard else Color.Transparent, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (entry.isNowPlaying) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(TvRed, RoundedCornerShape(3.dp))
            )
            Spacer(Modifier.width(8.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                entry.title,
                color = if (entry.isNowPlaying) TvTextWhite else TvTextGray,
                fontSize = 13.sp,
                fontWeight = if (entry.isNowPlaying) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (entry.start.isNotBlank() && entry.end.isNotBlank()) {
                Text("${entry.start} - ${entry.end}", color = TvTextGray, fontSize = 11.sp)
            }
        }
        if (entry.isNowPlaying) {
            TvRecordButton(isRecording = isRecording, onClick = onRecordClick, compact = true, modifier = recordButtonModifier)
        }
    }
}

/** يقدّر مدة برنامج EPG بالدقائق من نصّي "HH:mm" — يفترض عبور منتصف الليل إن كانت نهايته أصغر من بدايته */
private fun estimateEpgDurationMinutes(entry: ShortEpgEntry): Int {
    return try {
        val (sh, sm) = entry.start.split(":").map { it.toInt() }
        val (eh, em) = entry.end.split(":").map { it.toInt() }
        var diff = (eh * 60 + em) - (sh * 60 + sm)
        if (diff <= 0) diff += 24 * 60
        diff.coerceIn(5, 6 * 60)
    } catch (e: Exception) {
        60
    }
}

@Composable
private fun TvRecordButton(isRecording: Boolean, onClick: () -> Unit, compact: Boolean = false, modifier: Modifier = Modifier) {
    val size = if (compact) 32.dp else 44.dp
    Surface(
        onClick = onClick,
        modifier = modifier.size(size),
        shape = ClickableSurfaceDefaults.shape(shape = CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isRecording) TvRed else Color.White.copy(alpha = 0.08f),
            focusedContainerColor = if (isRecording) TvRed else Color.White.copy(alpha = 0.22f)
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(androidx.compose.foundation.BorderStroke(0.dp, Color.Transparent)),
            focusedBorder = Border(androidx.compose.foundation.BorderStroke(2.dp, TvFocusBorder))
        )
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                contentDescription = if (isRecording) "Stop recording" else "Record",
                tint = Color.White,
                modifier = Modifier.size(if (compact) 14.dp else 18.dp)
            )
        }
    }
}

@Composable
private fun TvChannelRow(
    channel: ChannelEntity,
    isPreviewing: Boolean,
    onFocus: () -> Unit,
    onClick: () -> Unit,
    rightFocusRequester: FocusRequester? = null
) {
    TvFocusable(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (rightFocusRequester != null) Modifier.focusProperties { right = rightFocusRequester }
                else Modifier
            ),
        shape = RoundedCornerShape(8.dp),
        onFocus = onFocus,
        onClick = onClick
    ) { focused ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isPreviewing) TvRed.copy(alpha = 0.18f) else Color.Transparent, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF111111))
            ) {
                if (channel.logoUrl.isNotBlank()) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = channel.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Text(
                channel.name,
                color = Color(0xFFEEEEEE),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}
