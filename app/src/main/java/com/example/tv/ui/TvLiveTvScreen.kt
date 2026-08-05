package com.example.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.example.model.ChannelEntity
import com.example.model.XtreamCategoryEntity
import com.example.tv.theme.TvBg
import com.example.tv.theme.TvBorder
import com.example.tv.theme.TvCard
import com.example.tv.theme.TvPanel
import com.example.tv.theme.TvRed
import com.example.tv.theme.TvTextGray
import com.example.tv.theme.TvTextWhite
import com.example.tv.ui.components.TvFocusable
import com.example.tv.ui.components.TvTextField

/** شاشة القنوات المباشرة — نفس تخطيط .tv-layout ثنائي العمود (تصنيفات | قنوات)، والتشغيل ملء الشاشة. */
@Composable
fun TvLiveTvScreen(
    channels: List<ChannelEntity>,
    categories: List<XtreamCategoryEntity>,
    onLoadCategoryStreams: (categoryId: String) -> Unit,
    onPlayChannel: (ChannelEntity) -> Unit
) {
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }

    val filtered = remember(channels, search) {
        if (search.isBlank()) channels else channels.filter { it.name.contains(search, ignoreCase = true) }
    }

    Row(modifier = Modifier.fillMaxSize().background(TvBg)) {
        // تصنيفات
        LazyColumn(
            modifier = Modifier
                .width(240.dp)
                .fillMaxHeight()
                .background(TvPanel)
                .padding(vertical = 16.dp, horizontal = 10.dp)
        ) {
            items(categories) { cat ->
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
        Column(modifier = Modifier.fillMaxHeight().width(360.dp)) {
            TvTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = "Search in category...",
                modifier = Modifier.fillMaxWidth().padding(14.dp)
            )
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                items(filtered) { ch ->
                    TvChannelRow(channel = ch, onClick = { onPlayChannel(ch) })
                }
            }
        }

        // مساحة فارغة تمثل معاينة الفئة/الشعار — التشغيل الفعلي يفتح مشغلاً ملء الشاشة عند اختيار قناة
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            Box(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun TvChannelRow(channel: ChannelEntity, onClick: () -> Unit) {
    TvFocusable(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        onClick = onClick
    ) { focused ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
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
