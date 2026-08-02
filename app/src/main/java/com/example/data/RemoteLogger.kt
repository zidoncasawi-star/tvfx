package com.example.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// يرسل أحداث تشخيصية مهمة مباشرة لسيرفرنا (بديل عن الحاجة لتوصيل الهاتف وقراءة Logcat يدوياً).
// تظهر هذه السجلات مباشرة في admin/logs.php
object RemoteLogger {
    private const val ENDPOINT = "https://app.flixplayer.pro/api/log.php"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    fun log(
        username: String? = null,
        deviceId: String? = null,
        level: String = "DEBUG",
        tag: String,
        message: String
    ) {
        // إرسال في الخلفية بدون انتظار (Fire and forget) — لا يجب أن يؤخر أي عملية استيراد أو تشغيل
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val body = JSONObject().apply {
                    put("username", username ?: "")
                    put("device_id", deviceId ?: "")
                    put("level", level)
                    put("tag", tag)
                    put("message", message)
                }.toString().toRequestBody(JSON_MEDIA_TYPE)

                val request = Request.Builder().url(ENDPOINT).post(body).build()
                client.newCall(request).execute().close()
            } catch (e: Throwable) {
                // تجاهل أي فشل في الإرسال نفسه حتى لا يعطّل التطبيق
            }
        }
    }
}
