package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChannelEntity
import com.example.ui.theme.LiveRed
import com.example.ui.theme.NetflixRed

data class EpgProgramItem(
    val title: String,
    val timeRange: String,
    val description: String,
    val isCurrent: Boolean,
    val progress: Float = 0f
)

@Composable
fun EpgScheduleDialog(
    channel: ChannelEntity,
    onDismiss: () -> Unit,
    onPlayChannel: () -> Unit
) {
    val epgPrograms = listOf(
        EpgProgramItem(
            title = channel.epgNow.ifEmpty { "Live Channel Broadcast" },
            timeRange = "19:00 - 20:30 (Now)",
            description = "Live coverage of top events and matches with our featured analysis studio.",
            isCurrent = true,
            progress = 0.65f
        ),
        EpgProgramItem(
            title = channel.epgNext.ifEmpty { "Next Program" },
            timeRange = "20:30 - 21:30",
            description = "Comprehensive analysis and today's top highlights and goals with a panel of experts.",
            isCurrent = false
        ),
        EpgProgramItem(
            title = "Sports News & Roundup",
            timeRange = "21:30 - 22:30",
            description = "Quick news coverage of the latest global and local sports developments.",
            isCurrent = false
        ),
        EpgProgramItem(
            title = "Documentary Movie Night",
            timeRange = "22:30 - 00:00",
            description = "Exclusive showing of the achievements and history of great athletes through the ages.",
            isCurrent = false
        )
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.95f),
        containerColor = Color(0xFF1A1A1E),
        titleContentColor = Color.White,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = NetflixRed
                    )
                    Column {
                        Text(
                            text = channel.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Electronic Program Guide (EPG)",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.heightIn(max = 360.dp)
                ) {
                    items(epgPrograms) { item ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (item.isCurrent) Color(0xFF261D1D) else Color(0xFF222228)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = if (item.isCurrent) {
                                androidx.compose.foundation.BorderStroke(1.dp, LiveRed)
                            } else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = if (item.isCurrent) LiveRed else Color.LightGray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = item.timeRange,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.isCurrent) LiveRed else Color.LightGray
                                        )
                                    }

                                    if (item.isCurrent) {
                                        Surface(
                                            color = LiveRed,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "Live Now",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = item.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = item.description,
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )

                                if (item.isCurrent) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { item.progress },
                                        color = LiveRed,
                                        trackColor = Color.DarkGray,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onPlayChannel()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = NetflixRed)
            ) {
                Text("Watch Channel Now", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.Gray)
            }
        }
    )
}
