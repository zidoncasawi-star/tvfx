package com.example.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * AlarmManager يفقد كل المنبّهات المضبوطة عند إعادة تشغيل الجهاز (ما لم تُضبط كمنبّهات دائمة
 * خاصة)، فبدون هذا المستقبِل تختفي كل التسجيلات المجدولة بصمت بعد أي إعادة تشغيل — نعيد
 * ضبطها جميعاً هنا فور اكتمال الإقلاع
 */
class BootRecordingRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                RecordingManager.rescheduleAllPendingAlarms(appContext)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
