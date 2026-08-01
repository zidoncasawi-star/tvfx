package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.ChannelEntity
import com.example.ui.components.CategoryFilterChips
import com.example.ui.components.ChannelCard
import com.example.ui.theme.NetflixRed
import com.example.ui.viewmodel.PlayingMedia
import android.app.Activity
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.ui.platform.LocalContext

@Composable
fun LiveTvScreen(
    channels: List<ChannelEntity>,
    xtreamCategories: List<com.example.model.XtreamCategoryEntity>,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit,
    onPlayChannel: (PlayingMedia) -> Unit,
    onToggleFavorite: (ChannelEntity) -> Unit,
    onLoadCategoryStreams: (String) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600
    
    val context = LocalContext.current
    DisposableEffect(Unit) {
        var activity: Activity? = null
        var currentContext = context
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) {
                activity = currentContext
                break
            }
            currentContext = currentContext.baseContext
        }
        
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

    var searchCategoryQuery by remember { mutableStateOf("") }
    var searchChannelQuery by remember { mutableStateOf("") }
    var previewChannel by remember { mutableStateOf<ChannelEntity?>(null) }

    val categories = remember(channels, xtreamCategories) {
        val list = mutableListOf("الكل")
        if (xtreamCategories.isNotEmpty()) {
            list.addAll(xtreamCategories.map { it.id })
        } else {
            list.addAll(channels.map { it.category }.distinct())
        }
        list.distinct()
    }

    val categoryNames = remember(channels, xtreamCategories) {
        val map = mutableMapOf<String, String>()
        map["الكل"] = "الكل"
        xtreamCategories.forEach { map[it.id] = it.name }
        channels.forEach { if (!map.containsKey(it.category)) map[it.category] = it.category }
        map
    }

    LaunchedEffect(selectedCategory) {
        if (selectedCategory != "الكل" && xtreamCategories.any { it.id == selectedCategory }) {
            val hasChannels = channels.any { it.category == selectedCategory }
            if (!hasChannels) {
                onLoadCategoryStreams(selectedCategory)
            }
        }
    }

    val filteredChannels = remember(channels, selectedCategory, searchChannelQuery) {
        channels.filter {
            (selectedCategory == "الكل" || it.category == selectedCategory) &&
                    (searchChannelQuery.isEmpty() || it.name.contains(searchChannelQuery, ignoreCase = true))
        }.sortedWith(
            compareByDescending<ChannelEntity> { it.isFavorite }
                .thenBy { it.channelNum }
                .thenBy { it.name }
        )
    }

    LaunchedEffect(filteredChannels) {
        if (previewChannel == null && filteredChannels.isNotEmpty()) {
            previewChannel = filteredChannels.first()
        } else if (filteredChannels.isNotEmpty() && !filteredChannels.contains(previewChannel)) {
            previewChannel = filteredChannels.first()
        }
    }

    val paneBorder = Color.White.copy(alpha = 0.05f)

    if (isWideScreen) {
        val filteredCategories = remember(categories, searchCategoryQuery) {
            categories.filter {
                categoryNames[it]?.contains(searchCategoryQuery, ignoreCase = true) == true
            }
        }
        
        Row(modifier = modifier.fillMaxSize().background(Color.Black)) {
            // Pane 1: Categories (Left)
            Column(
                modifier = Modifier
                    .width(220.dp)
                    .fillMaxHeight()
                    .background(Color.Black)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onExit,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "البث المباشر",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                HorizontalDivider(color = paneBorder)
                TextField(
                    value = searchCategoryQuery,
                    onValueChange = { searchCategoryQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .height(48.dp),
                    placeholder = { Text("بحث الأقسام...", color = Color.Gray, fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchCategoryQuery.isNotEmpty()) {
                            IconButton(onClick = { searchCategoryQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "مسح", tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                )

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredCategories) { catId ->
                        val catName = categoryNames[catId] ?: catId
                        val isSelected = catId == selectedCategory

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCategorySelect(catId) }
                                .background(if (isSelected) Color.White.copy(alpha = 0.05f) else Color.Transparent)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = catName.uppercase(),
                                color = if (isSelected) Color.White else Color.LightGray,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        HorizontalDivider(color = paneBorder)
                    }
                }
            }
            
            VerticalDivider(color = paneBorder)

            // Pane 2: Channels List (Middle)
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .background(Color.Black)
            ) {
                TextField(
                    value = searchChannelQuery,
                    onValueChange = { searchChannelQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .height(48.dp),
                    placeholder = { Text("بحث القنوات...", color = Color.Gray, fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchChannelQuery.isNotEmpty()) {
                            IconButton(onClick = { searchChannelQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "مسح", tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                )

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredChannels.size) { index ->
                        val channel = filteredChannels[index]
                        val isPreviewed = previewChannel?.id == channel.id
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    previewChannel = channel
                                }
                                .background(if (isPreviewed) NetflixRed.copy(alpha = 0.15f) else Color.Transparent)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = (index + 1).toString(),
                                color = if (isPreviewed) NetflixRed else Color.Gray,
                                fontSize = 12.sp,
                                modifier = Modifier.width(30.dp)
                            )
                            
                            AsyncImage(
                                model = channel.logoUrl.ifBlank { "https://via.placeholder.com/150/000000/FFFFFF/?text=TV" },
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .padding(end = 12.dp),
                                contentScale = ContentScale.Fit
                            )
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = channel.name,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = if (isPreviewed) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (channel.epgNow.isNotBlank()) {
                                    Text(
                                        text = channel.epgNow,
                                        color = Color.LightGray,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            
                            IconButton(
                                onClick = { onToggleFavorite(channel) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "تفضيل",
                                    tint = if (channel.isFavorite) NetflixRed else Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = paneBorder)
                    }
                }
            }

            VerticalDivider(color = paneBorder)

            // Pane 3: Preview & EPG (Right)
            Column(
                modifier = Modifier
                    .weight(1.8f)
                    .fillMaxHeight()
                    .background(Color.Black)
            ) {
                if (previewChannel != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.2f)
                            .background(Color.Black)
                            .clickable {
                                onPlayChannel(
                                    PlayingMedia(
                                        id = previewChannel!!.id,
                                        title = previewChannel!!.name,
                                        streamUrl = previewChannel!!.streamUrl,
                                        posterUrl = previewChannel!!.logoUrl,
                                        category = previewChannel!!.category,
                                        type = "LIVE",
                                        channelList = channels
                                    )
                                )
                            }
                    ) {
                        com.example.ui.components.InlinePlayer(
                            mediaUrl = previewChannel!!.streamUrl,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Intercept clicks to go fullscreen
                        Box(modifier = Modifier.fillMaxSize().background(Color.Transparent))
                    }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color.Black)
                            .padding(24.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = NetflixRed,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "LIVE TV",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = previewChannel!!.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        val epgNow = previewChannel!!.epgNow
                        if (epgNow.isNotBlank()) {
                            Text(text = "NOW PLAYING", color = NetflixRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(text = epgNow, color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                        } else {
                            Text(text = "NO EPG FOUND", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    } else {
        // Mobile Layout (Vertical)
        Column(modifier = modifier.fillMaxSize().background(Color.Black)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onExit,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "القنوات المباشرة",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            HorizontalDivider(color = paneBorder)
            TextField(
                value = searchChannelQuery,
                onValueChange = { searchChannelQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(48.dp),
                placeholder = { Text("بحث في القنوات...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                trailingIcon = {
                    if (searchChannelQuery.isNotEmpty()) {
                        IconButton(onClick = { searchChannelQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح", tint = Color.Gray)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )

            CategoryFilterChips(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelect = onCategorySelect,
                categoryNames = categoryNames
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredChannels, key = { it.id }) { channel ->
                    ChannelCard(
                        channel = channel,
                        onClick = {
                            onPlayChannel(
                                PlayingMedia(
                                    id = channel.id,
                                    title = channel.name,
                                    streamUrl = channel.streamUrl,
                                    posterUrl = channel.logoUrl,
                                    category = channel.category,
                                    type = "LIVE",
                                    channelList = channels
                                )
                            )
                        },
                        onFavoriteToggle = { onToggleFavorite(channel) },
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f/9f)
                    )
                }
            }
        }
    }
}
