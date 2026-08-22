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
    val xtreamPassword: String? = null,
    val activeDeviceType: String? = null
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
                message = "The admin panel address has not been configured yet."
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
                        val msg = json.optString("message", if (isActivated) "Subscription is active" else "Subscription not yet activated")
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
                            xtreamPassword = xPass.ifBlank { null },
                            activeDeviceType = json.optString("activeDeviceType", "").ifBlank { null }
                        )
                    }
                }
                return@withContext SubscriptionCheckResult(
                    isActivated = false,
                    message = "An error occurred while verifying the subscription. Server responded with ${response.code}"
                )
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            return@withContext SubscriptionCheckResult(
                isActivated = false,
                message = "Failed to connect to the admin panel: ${e.message ?: "Unknown error"}"
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
                    put("fullName", "Rodix User")
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
                put("message", "Registration successful (demo)")
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
     * يوافق على طلب ربط جهاز سطح مكتب مُدخَل من الهاتف (رمز 6 أرقام يظهر على شاشة الحاسوب)،
     * ويجعل سطح المكتب هو الجهاز النشط فوراً (يقفل الهاتف تلقائياً على غرار web.whatsapp.com).
     */
    suspend fun approveDesktopPairing(
        adminUrl: String,
        code: String,
        username: String,
        activationCode: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "${adminUrl.trimEnd('/')}/api/pair_approve.php"
            val bodyJson = JSONObject().apply {
                put("code", code)
                put("username", username)
                put("activationCode", activationCode)
            }
            val body = bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder().url(url).post(body).build()

            client.newCall(request).execute().use { response ->
                val respBody = response.body?.string() ?: return@withContext false
                return@withContext JSONObject(respBody).optBoolean("success", false)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            return@withContext false
        }
    }

    /** يجعل هذا الجهاز هو الجهاز النشط — يُستدعى بعد أي تسجيل دخول عادي (بدون ربط QR) */
    suspend fun setActiveDevice(adminUrl: String, username: String, activationCode: String, deviceType: String = "phone") = withContext(Dispatchers.IO) {
        try {
            val url = "${adminUrl.trimEnd('/')}/api/set_active_device.php"
            val bodyJson = JSONObject().apply {
                put("username", username)
                put("activationCode", activationCode)
                put("deviceType", deviceType)
            }
            val body = bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder().url(url).post(body).build()
            client.newCall(request).execute().close()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    /** يُطلق قفل الجهاز عند تسجيل الخروج الصريح، فيسمح للجهاز الآخر بالعمل من جديد */
    suspend fun clearActiveDevice(adminUrl: String, username: String, activationCode: String) = withContext(Dispatchers.IO) {
        try {
            val url = "${adminUrl.trimEnd('/')}/api/clear_active_device.php"
            val bodyJson = JSONObject().apply {
                put("username", username)
                put("activationCode", activationCode)
            }
            val body = bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder().url(url).post(body).build()
            client.newCall(request).execute().close()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    data class TvPairingCreateResult(val success: Boolean, val code: String? = null)
    data class TvPairingCheckResult(val status: String, val session: JSONObject? = null)

    /**
     * ينشئ كود ربط مؤقت (6 أرقام) لتطبيق التلفاز — نفس آلية pair_create.php المستخدمة أصلاً من
     * سطح المكتب، لكن مع deviceType="tv" حتى يُسجَّل التلفاز (لا سطح المكتب) كجهاز نشط عند الموافقة.
     */
    suspend fun createTvPairing(adminUrl: String): TvPairingCreateResult = withContext(Dispatchers.IO) {
        try {
            val url = "${adminUrl.trimEnd('/')}/api/pair_create.php"
            val bodyJson = JSONObject().apply { put("deviceType", "tv") }
            val body = bodyJson.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder().url(url).post(body).build()

            client.newCall(request).execute().use { response ->
                val respBody = response.body?.string() ?: return@withContext TvPairingCreateResult(false)
                val json = JSONObject(respBody)
                return@withContext TvPairingCreateResult(
                    success = json.optBoolean("success", false),
                    code = json.optString("code", "").ifBlank { null }
                )
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            return@withContext TvPairingCreateResult(false)
        }
    }

    /** يستطلعه التلفاز دورياً بعد createTvPairing بانتظار موافقة الهاتف — نفس pair_check.php */
    suspend fun checkTvPairing(adminUrl: String, code: String): TvPairingCheckResult = withContext(Dispatchers.IO) {
        try {
            val url = "${adminUrl.trimEnd('/')}/api/pair_check.php?code=${java.net.URLEncoder.encode(code, "UTF-8")}"
            val request = Request.Builder().url(url).get().build()

            client.newCall(request).execute().use { response ->
                val respBody = response.body?.string() ?: return@withContext TvPairingCheckResult("pending")
                val json = JSONObject(respBody)
                return@withContext TvPairingCheckResult(
                    status = json.optString("status", "pending"),
                    session = json.optJSONObject("session")
                )
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            return@withContext TvPairingCheckResult("pending")
        }
    }

    /** إشعار واحد يبثّه الأدمن من لوحة التحكم — نفس مصدر بيانات #notifPanelList في سطح المكتب */
    data class AppNotification(
        val id: Int,
        val title: String,
        val contentType: String,
        val content: String,
        val buttonLabel: String?,
        val buttonUrl: String?,
        val createdAt: Long
    )

    /** يجلب الإشعارات المفعّلة المستهدِفة لهذه المنصة أو "الكل" — نفس notifications_list.php المستخدم في سطح المكتب */
    suspend fun fetchNotifications(adminUrl: String, platform: String = "android"): List<AppNotification> = withContext(Dispatchers.IO) {
        if (adminUrl.isBlank()) return@withContext emptyList()
        try {
            val url = "${adminUrl.trimEnd('/')}/api/notifications_list.php?platform=$platform"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                val respBody = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(respBody)
                if (!json.optBoolean("success", false)) return@withContext emptyList()
                val arr = json.optJSONArray("notifications") ?: return@withContext emptyList()
                (0 until arr.length()).map { i ->
                    val n = arr.getJSONObject(i)
                    AppNotification(
                        id = n.optInt("id"),
                        title = n.optString("title"),
                        contentType = n.optString("contentType"),
                        content = n.optString("content"),
                        buttonLabel = n.optString("buttonLabel", "").ifBlank { null },
                        buttonUrl = n.optString("buttonUrl", "").ifBlank { null },
                        createdAt = n.optLong("createdAt")
                    )
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            emptyList()
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
