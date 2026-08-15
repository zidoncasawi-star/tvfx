package com.example.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** يُطلقه AlarmManager عند حلول موعد تسجيل EPG مجدوَل مسبقاً، حتى لو كان التطبيق مغلقاً تماماً */
class RecordingAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val recordingId = intent.getLongExtra(RecordingService.EXTRA_RECORDING_ID, -1L)
        if (recordingId <= 0L) return
        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                RecordingManager.startScheduledRecordingNow(appContext, recordingId)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
