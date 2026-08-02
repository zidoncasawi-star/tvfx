package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SubscriptionCheckResult(
    val isActivated: Boolean,
    val message: String,
    val expiresAt: Long? = null,
    val xtreamHost: String? = null,
    val xtreamUsername: String? = null,
    val xtreamPassword: String? = null
)

object AdminPanelClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    /**
     * Checks if the user's subscription has been activated by the administrator.
     */
    suspend fun checkSubscriptionStatus(
        adminUrl: String,
        username: String,
        activationCode: String
    ): SubscriptionCheckResult = withContext(Dispatchers.IO) {
        // لا يوجد أي حساب Xtream افتراضي/تجريبي مكتوب في الكود — الاعتماد حصرياً على لوحة التحكم.
        // إن كان عنوان لوحة التحكم فارغاً أو غير مضبوط، الحساب يبقى ببساطة "غير مفعَّل" بانتظار سيرفر حقيقي.
        if (adminUrl.isBlank() || adminUrl.startsWith("http://mock") || adminUrl.startsWith("mock")) {
            return@withContext SubscriptionCheckResult(
                isActivated = false,
                message = "لم يتم ضبط عنوان لوحة التحكم بعد."
            )
        }

        try {
            val baseUrl = adminUrl.trimEnd('/')
            val url = "$baseUrl/api/status.php?username=$username&code=$activationCode"
            
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val json = JSONObject(responseBody)
                        val isActivated = json.optBoolean("activated", false) || json.optString("status").lowercase() == "active"
                        val msg = json.optString("message", if (isActivated) "الاشتراك نشط ومفعل" else "الاشتراك غير مفعل بعد")
                        val expiresAt = if (json.has("expiresAt")) json.optLong("expiresAt") else null
                        
                        val xHost = json.optString("xtreamHost", "").ifBlank { json.optString("xtream_host", "") }
                        val xUser = json.optString("xtreamUsername", "").ifBlank { json.optString("xtream_username", "") }
                        val xPass = json.optString("xtreamPassword", "").ifBlank { json.optString("xtream_password", "") }

                        return@withContext SubscriptionCheckResult(
                            isActivated = isActivated,
                            message = msg,
                            expiresAt = expiresAt,
                            xtreamHost = xHost.ifBlank { null },
                            xtreamUsername = xUser.ifBlank { null },
                            xtreamPassword = xPass.ifBlank { null }
                        )
                    }
                }
                return@withContext SubscriptionCheckResult(
                    isActivated = false,
                    message = "حدث خطأ أثناء التحقق من الاشتراك. الخادم استجاب بـ ${response.code}"
                )
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            return@withContext SubscriptionCheckResult(
                isActivated = false,
                message = "فشل الاتصال بلوحة التحكم الخارجية: ${e.message ?: "خطأ غير معروف"}"
            )
        }
    }

    /**
     * Authenticates the user against the external Admin Panel.
     */
    suspend fun loginUserOnAdminPanel(
        adminUrl: String,
        identifier: String,
        pass: String
    ): JSONObject? = withContext(Dispatchers.IO) {
        if (adminUrl.isBlank() || adminUrl.startsWith("http://mock") || adminUrl.startsWith("mock")) {
            // Mock Login
            if (identifier == "rodix_user" || identifier == "user@rodixtv.com") {
                return@withContext JSONObject().apply {
                    put("success", true)
                    put("fullName", "مستخدم روديكس")
                    put("email", "user@rodixtv.com")
                    put("username", "rodix_user")
                    put("phoneNumber", "0500000000")
                    put("activationCode", "RDX-DEMO-99")
                }
            }
            return@withContext null
        }

        try {
            val url = "${adminUrl.trimEnd('/')}/api/login.php"
            val bodyJson = JSONObject().apply {
                put("identifier", identifier)
                put("password", pass)
            }

            val body = bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val respBody = response.body?.string()
                if (respBody != null) {
                    try {
                        return@withContext JSONObject(respBody)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            return@withContext null
        } catch (e: Throwable) {
            e.printStackTrace()
            return@withContext null
        }
    }

    /**
     * Registers a new user account on the external Admin Panel.
     */
    suspend fun registerUserOnAdminPanel(
        adminUrl: String,
        username: String,
        email: String,
        pass: String,
        phone: String,
        deviceId: String
    ): JSONObject? = withContext(Dispatchers.IO) {
        if (adminUrl.isBlank() || adminUrl.startsWith("http://mock") || adminUrl.startsWith("mock")) {
            return@withContext JSONObject().apply {
                put("success", true)
                put("message", "تم التسجيل بنجاح (تجريبي)")
            }
        }

        try {
            val url = "${adminUrl.trimEnd('/')}/api/register.php"
            val bodyJson = JSONObject().apply {
                put("username", username)
                put("email", email)
                put("password", pass)
                put("phone", phone)
                put("device_id", deviceId)
            }

            val body = bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val respBody = response.body?.string()
                if (respBody != null) {
                    try {
                        return@withContext JSONObject(respBody)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            return@withContext null
        } catch (e: Throwable) {
            e.printStackTrace()
            return@withContext null
        }
    }

    /**
     * Tracks app installation/open by sending the device ID to the external Admin Panel.
     */
    suspend fun trackInstall(adminUrl: String, deviceId: String) = withContext(Dispatchers.IO) {
        if (adminUrl.isBlank() || adminUrl.startsWith("http://mock") || adminUrl.startsWith("mock")) {
            return@withContext
        }

        try {
            val url = "${adminUrl.trimEnd('/')}/api/track_install.php"
            val bodyJson = JSONObject().apply {
                put("device_id", deviceId)
            }

            val body = bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                // Fire and forget
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}
