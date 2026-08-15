package com.example.data

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.model.RecordingEntity

/** واجهة بسيطة لبدء/إيقاف RecordingService من الـ ViewModel، دون تكرار منطق الـ Intent في كل مكان */
object RecordingManager {

    suspend fun startRecording(
        context: Context,
        playlistId: Long,
        channelId: String,
        channelName: String,
        streamUrl: String,
        categoryName: String,
        programTitle: String = "",
        durationMinutes: Int = 0
    ): Long {
        val dao = AppDatabase.getInstance(context).recordingDao()
        val recordingId = dao.insertRecording(
            RecordingEntity(
                playlistId = playlistId,
                channelId = channelId,
                channelName = channelName,
                programTitle = programTitle,
                categoryName = categoryName,
                plannedDurationMinutes = durationMinutes,
                status = "RECORDING"
            )
        )

        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_START
            putExtra(RecordingService.EXTRA_RECORDING_ID, recordingId)
            putExtra(RecordingService.EXTRA_STREAM_URL, streamUrl)
            putExtra(RecordingService.EXTRA_CHANNEL_NAME, channelName)
            putExtra(RecordingService.EXTRA_DURATION_MINUTES, durationMinutes)
        }
        ContextCompat.startForegroundService(context, intent)
        return recordingId
    }

    fun stopRecording(context: Context, recordingId: Long) {
        // يجب عدم استخدام startForegroundService هنا: عند استدعائها، يُلزم النظام الخدمة
        // باستدعاء startForeground() خلال ثوانٍ قليلة وإلا يقتل أندرويد التطبيق بالكامل فوراً
        // (ForegroundServiceDidNotStartInTimeException) — أمر "إيقاف" لا يستدعي startForeground
        // إطلاقاً (الخدمة تتوقف عن نفسها)، فيجب إرسال هذا الأمر عبر startService العادية بدلاً منه؛
        // بما أن الخدمة تكون بالفعل تعمل بصفة foreground من أمر البدء الأصلي، فهذا آمن تماماً
        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_STOP
            putExtra(RecordingService.EXTRA_RECORDING_ID, recordingId)
        }
        runCatching { context.startService(intent) }
    }

    suspend fun deleteRecording(context: Context, recording: RecordingEntity) {
        if (recording.status == "RECORDING") stopRecording(context, recording.id)
        if (recording.filePath.isNotBlank()) {
            runCatching { java.io.File(recording.filePath).delete() }
        }
        AppDatabase.getInstance(context).recordingDao().deleteRecording(recording)
    }
}
