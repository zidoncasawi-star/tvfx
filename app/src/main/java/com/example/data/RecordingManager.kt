package com.example.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
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
                streamUrl = streamUrl,
                programTitle = programTitle,
                categoryName = categoryName,
                plannedDurationMinutes = durationMinutes,
                status = "RECORDING"
            )
        )
        launchRecordingService(context, recordingId, streamUrl, channelName, durationMinutes)
        return recordingId
    }

    private fun launchRecordingService(context: Context, recordingId: Long, streamUrl: String, channelName: String, durationMinutes: Int) {
        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_START
            putExtra(RecordingService.EXTRA_RECORDING_ID, recordingId)
            putExtra(RecordingService.EXTRA_STREAM_URL, streamUrl)
            putExtra(RecordingService.EXTRA_CHANNEL_NAME, channelName)
            putExtra(RecordingService.EXTRA_DURATION_MINUTES, durationMinutes)
        }
        ContextCompat.startForegroundService(context, intent)
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
        if (recording.status == "SCHEDULED") cancelScheduledRecording(context, recording.id)
        if (recording.filePath.isNotBlank()) {
            runCatching { java.io.File(recording.filePath).delete() }
        }
        AppDatabase.getInstance(context).recordingDao().deleteRecording(recording)
    }

    /**
     * يجدول تسجيل برنامج مستقبلي من دليل البرامج (EPG) — يُنشئ صفاً بحالة "SCHEDULED" فوراً
     * (يظهر في شاشة التسجيلات كـ"مجدوَل") ويضبط منبّهاً دقيقاً (AlarmManager) يبدأ التسجيل
     * الفعلي تلقائياً عند حلول الوقت، حتى لو كان التطبيق مغلقاً تماماً في تلك اللحظة.
     */
    suspend fun scheduleRecording(
        context: Context,
        playlistId: Long,
        channelId: String,
        channelName: String,
        streamUrl: String,
        categoryName: String,
        programTitle: String,
        startAtMs: Long,
        durationMinutes: Int
    ): Long {
        val dao = AppDatabase.getInstance(context).recordingDao()
        val recordingId = dao.insertRecording(
            RecordingEntity(
                playlistId = playlistId,
                channelId = channelId,
                channelName = channelName,
                streamUrl = streamUrl,
                programTitle = programTitle,
                categoryName = categoryName,
                scheduledStartAtMs = startAtMs,
                plannedDurationMinutes = durationMinutes,
                status = "SCHEDULED"
            )
        )
        setAlarm(context, recordingId, startAtMs)
        return recordingId
    }

    fun cancelScheduledRecording(context: Context, recordingId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        alarmManager?.cancel(alarmPendingIntent(context, recordingId))
    }

    /** يُستدعى من RecordingAlarmReceiver فقط عند حلول موعد تسجيل مجدوَل فعلياً */
    suspend fun startScheduledRecordingNow(context: Context, recordingId: Long) {
        val dao = AppDatabase.getInstance(context).recordingDao()
        val recording = dao.getRecordingById(recordingId) ?: return
        if (recording.status != "SCHEDULED") return // أُلغي أو بدأ بالفعل
        dao.updateRecording(recording.copy(status = "RECORDING", startedAt = System.currentTimeMillis()))
        launchRecordingService(context, recordingId, recording.streamUrl, recording.channelName, recording.plannedDurationMinutes)
    }

    private fun alarmPendingIntent(context: Context, recordingId: Long): PendingIntent {
        val intent = Intent(context, RecordingAlarmReceiver::class.java).apply {
            putExtra(RecordingService.EXTRA_RECORDING_ID, recordingId)
        }
        return PendingIntent.getBroadcast(
            context, recordingId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * يُستدعى من BootRecordingRescheduleReceiver بعد كل إعادة تشغيل للجهاز: AlarmManager يفقد
     * كل المنبّهات المضبوطة عند إيقاف تشغيل الجهاز، فيجب إعادة ضبطها يدوياً، وإن كان موعد أي
     * تسجيل قد فات أثناء إغلاق الجهاز فيبدأ فوراً بدل أن يُفقد بصمت
     */
    suspend fun rescheduleAllPendingAlarms(context: Context) {
        val dao = AppDatabase.getInstance(context).recordingDao()
        val now = System.currentTimeMillis()
        dao.getScheduledRecordingsSync().forEach { recording ->
            if (recording.scheduledStartAtMs in 1 until now) {
                startScheduledRecordingNow(context, recording.id)
            } else {
                setAlarm(context, recording.id, recording.scheduledStartAtMs)
            }
        }
    }

    private fun setAlarm(context: Context, recordingId: Long, startAtMs: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = alarmPendingIntent(context, recordingId)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                // المستخدم لم يمنح إذن "Alarms & reminders" — نتراجع لمنبّه غير دقيق تماماً
                // (قد يتأخر بضع دقائق) بدل تعطّل التطبيق بالكامل بـ SecurityException
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startAtMs, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startAtMs, pendingIntent)
            }
        } catch (e: SecurityException) {
            runCatching { alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startAtMs, pendingIntent) }
        }
    }
}
