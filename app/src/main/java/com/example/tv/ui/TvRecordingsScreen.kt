package com.example.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.example.model.RecordingEntity
import com.example.tv.theme.TvBg
import com.example.tv.theme.TvCard
import com.example.tv.theme.TvFocusBorder
import com.example.tv.theme.TvRed
import com.example.tv.theme.TvTextGray
import com.example.tv.theme.TvTextWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** شاشة التسجيلات — تطابق قسم Recordings في سطح المكتب: قائمة تسجيلات بحالتها، تشغيل أو حذف. */
@Composable
fun TvRecordingsScreen(
    recordings: List<RecordingEntity>,
    onPlayRecording: (RecordingEntity) -> Unit,
    onStopRecording: (RecordingEntity) -> Unit,
    onDeleteRecording: (RecordingEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(TvBg).padding(horizontal = 40.dp, vertical = 24.dp)) {
        Text("Recordings", color = TvTextWhite, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Spacer(Modifier.height(16.dp))

        if (recordings.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No recordings yet. Press Record on a live channel to start.", color = TvTextGray, fontSize = 14.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.focusRestorer()) {
                items(recordings, key = { it.id }) { rec ->
                    TvRecordingRow(rec, onPlayRecording, onStopRecording, onDeleteRecording)
                }
            }
        }
    }
}

@Composable
private fun TvRecordingRow(
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
            .background(TvCard, RoundedCornerShape(10.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isRecording) {
                    Box(modifier = Modifier.size(8.dp).background(TvRed, CircleShape))
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = rec.programTitle.ifBlank { rec.channelName },
                    color = TvTextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${rec.channelName} · $dateStr · $durationStr" + if (isRecording) " · Recording" else "",
                color = if (isRecording) TvRed else TvTextGray,
                fontSize = 12.sp
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (isRecording) {
                TvRoundActionButton(icon = Icons.Default.Stop, contentDescription = "Stop", onClick = { onStopRecording(rec) })
            } else {
                TvRoundActionButton(icon = Icons.Default.PlayArrow, contentDescription = "Play", onClick = { onPlayRecording(rec) })
            }
            TvRoundActionButton(icon = Icons.Default.Delete, contentDescription = "Delete", onClick = { onDeleteRecording(rec) })
        }
    }
}

@Composable
private fun TvRoundActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(40.dp),
        shape = ClickableSurfaceDefaults.shape(shape = CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.08f),
            focusedContainerColor = Color.White.copy(alpha = 0.22f)
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(androidx.compose.foundation.BorderStroke(0.dp, Color.Transparent)),
            focusedBorder = Border(androidx.compose.foundation.BorderStroke(2.dp, TvFocusBorder))
        )
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(imageVector = icon, contentDescription = contentDescription, tint = TvTextWhite, modifier = Modifier.size(18.dp))
        }
    }
}
