package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.RecordingEntity
import com.example.ui.theme.NetflixRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** شاشة التسجيلات على الهاتف — تطابق قسم Recordings في سطح المكتب. */
@Composable
fun RecordingsScreen(
    recordings: List<RecordingEntity>,
    onPlayRecording: (RecordingEntity) -> Unit,
    onStopRecording: (RecordingEntity) -> Unit,
    onDeleteRecording: (RecordingEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().background(Color.Black).padding(16.dp)) {
        Text("التسجيلات", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(16.dp))

        if (recordings.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا توجد تسجيلات بعد. اضغط زر التسجيل على قناة مباشرة للبدء.", color = Color.Gray, fontSize = 13.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(recordings, key = { it.id }) { rec ->
                    RecordingRow(rec, onPlayRecording, onStopRecording, onDeleteRecording)
                }
            }
        }
    }
}

@Composable
private fun RecordingRow(
    rec: RecordingEntity,
    onPlayRecording: (RecordingEntity) -> Unit,
    onStopRecording: (RecordingEntity) -> Unit,
    onDeleteRecording: (RecordingEntity) -> Unit
) {
    val isRecording = rec.status == "RECORDING"
    val dateStr = remember(rec.startedAt) { SimpleDateFormat("MMM d, HH:mm", Locale.ENGLISH).format(Date(rec.startedAt)) }
    val durationStr = remember(rec.durationMs) {
        val totalSec = (rec.durationMs / 1000).toInt()
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF181818), RoundedCornerShape(10.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isRecording) {
                    Box(modifier = Modifier.size(8.dp).background(NetflixRed, CircleShape))
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = rec.programTitle.ifBlank { rec.channelName },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${rec.channelName} · $dateStr · $durationStr" + if (isRecording) " · جاري التسجيل" else "",
                color = if (isRecording) NetflixRed else Color.Gray,
                fontSize = 11.sp
            )
        }

        if (isRecording) {
            IconButton(onClick = { onStopRecording(rec) }) {
                Icon(Icons.Default.Stop, contentDescription = "إيقاف", tint = Color.White)
            }
        } else {
            IconButton(onClick = { onPlayRecording(rec) }) {
                Icon(Icons.Default.PlayArrow, contentDescription = "تشغيل", tint = Color.White)
            }
        }
        IconButton(onClick = { onDeleteRecording(rec) }) {
            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Gray)
        }
    }
}
