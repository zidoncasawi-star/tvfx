package com.example.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.model.RecordingEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * خدمة أمامية (Foreground Service) تسجّل قناة بث مباشر إلى ملف محلي بينما تعمل في الخلفية —
 * تعتمد نفس أسلوب "piggyback" المستخدم في تطبيق سطح المكتب: تجلب قائمة تشغيل HLS (m3u8) بشكل
 * دوري وتُلحِق كل مقطع (.ts) جديد لم يُنزَّل بعد بنهاية ملف واحد، فينتج ملف .ts متسلسل قابل
 * للتشغيل مباشرة (بدون إعادة ترميز)، دون الحاجة لتشغيل الفيديو فعلياً أثناء التسجيل.
 */
class RecordingService : Service() {

    companion object {
        const val ACTION_START = "com.example.action.START_RECORDING"
        const val ACTION_STOP = "com.example.action.STOP_RECORDING"
        const val EXTRA_RECORDING_ID = "recordingId"
        const val EXTRA_STREAM_URL = "streamUrl"
        const val EXTRA_CHANNEL_NAME = "channelName"
        const val EXTRA_DURATION_MINUTES = "durationMinutes"

        private const val CHANNEL_ID = "recordings_channel"
        private const val NOTIF_ID_BASE = 9000
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private val activeJobs = mutableMapOf<Long, Job>()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val recordingId = intent?.getLongExtra(EXTRA_RECORDING_ID, -1L) ?: -1L
        if (recordingId <= 0) return START_NOT_STICKY

        when (intent?.action) {
            ACTION_STOP -> {
                val job = activeJobs.remove(recordingId)
                if (job != null) {
                    job.cancel()
                    stopIfIdle()
                } else {
                    // لا توجد مهمة تسجيل نشطة بهذا المعرّف في هذه العملية (مثلاً إن أُعيد تشغيل
                    // النظام لخدمة التسجيل أو انهارت فعُيِّن activeJobs من جديد فارغة) — يبقى صف
                    // قاعدة البيانات عالقاً على حالة "RECORDING" للأبد بلا هذا الإصلاح اليدوي، لأنه
                    // لا يوجد أي Job لإلغائه يستدعي finalizeRecording. ويجب استدعاء stopIfIdle()
                    // فقط بعد اكتمال الكتابة في قاعدة البيانات، وإلا فقد يُدمَّر onDestroy() الخدمة
                    // (serviceJob.cancel()) قبل أن يكتمل هذا النداء غير المتزامن أصلاً
                    val dao = AppDatabase.getInstance(applicationContext).recordingDao()
                    serviceScope.launch {
                        dao.getRecordingById(recordingId)?.let {
                            if (it.status == "RECORDING") dao.updateRecording(it.copy(status = "COMPLETED"))
                        }
                        stopIfIdle()
                    }
                }
            }
            else -> {
                val streamUrl = intent?.getStringExtra(EXTRA_STREAM_URL) ?: return START_NOT_STICKY
                val channelName = intent.getStringExtra(EXTRA_CHANNEL_NAME) ?: "Recording"
                val durationMinutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, 0)

                startForeground(NOTIF_ID_BASE + recordingId.toInt(), buildNotification(recordingId, channelName))
                val job = serviceScope.launch {
                    runRecordingLoop(recordingId, streamUrl, channelName, durationMinutes)
                }
                activeJobs[recordingId] = job
            }
        }
        return START_STICKY
    }

    private fun stopIfIdle() {
        if (activeJobs.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private suspend fun runRecordingLoop(recordingId: Long, streamUrl: String, channelName: String, durationMinutes: Int) {
        val dao = AppDatabase.getInstance(applicationContext).recordingDao()
        val outDir = File(applicationContext.getExternalFilesDir(null), "recordings").apply { mkdirs() }
        val outFile = File(outDir, "rec_${recordingId}_${System.currentTimeMillis()}.ts")
        val startedAt = System.currentTimeMillis()
        val downloadedSegments = LinkedHashSet<String>()
        var failureStreak = 0

        try {
            FileOutputStream(outFile, true).use { fos ->
                while (true) {
                    val elapsedMs = System.currentTimeMillis() - startedAt
                    if (durationMinutes > 0 && elapsedMs >= durationMinutes * 60_000L) break

                    try {
                        val segmentUrls = fetchSegmentUrls(streamUrl)
                        val newOnes = segmentUrls.filter { it !in downloadedSegments }
                        for (segUrl in newOnes) {
                            downloadSegmentInto(segUrl, fos)
                            downloadedSegments.add(segUrl)
                            // نحدّ حجم الذاكرة المُتتبَّعة — لا حاجة لتذكّر أكثر من آخر 500 مقطع لتفادي إعادة تنزيلها
                            if (downloadedSegments.size > 500) {
                                val first = downloadedSegments.first()
                                downloadedSegments.remove(first)
                            }
                        }
                        failureStreak = 0
                    } catch (e: Throwable) {
                        failureStreak++
                        RemoteLogger.log(level = "ERROR", tag = "RecordingService", message = "recordingId=$recordingId fetch failed (${failureStreak}): ${e.message}")
                        if (failureStreak >= 10) break
                    }

                    dao.getRecordingById(recordingId)?.let {
                        dao.updateRecording(it.copy(durationMs = System.currentTimeMillis() - startedAt, filePath = outFile.absolutePath))
                    }

                    delay(5000)
                }
            }

            finalizeRecording(dao, recordingId, outFile, startedAt, failed = false)
        } catch (e: kotlinx.coroutines.CancellationException) {
            // هذا هو المسار الطبيعي عند إيقاف التسجيل يدوياً: onStartCommand يُلغي activeJobs[recordingId]
            // مباشرة، فتُقاطَع هذه الحلقة بـ CancellationException. لا يجب معاملتها كفشل طالما أن
            // الملف الناتج يحوي بيانات فعلية. الأهم: أي نداء suspend لاحق (مثل dao.updateRecording)
            // يُنفَّذ من نفس هذه الروتين المُلغاة فعلياً سيُلغى فوراً بدوره ولن يُنفَّذ إطلاقاً ما لم
            // يُغلَّف بـ NonCancellable — وهذا بالضبط سبب بقاء التسجيلات المُوقَفة يدوياً عالقة إلى
            // الأبد على حالة "RECORDING" دون أن تُحفَظ أبداً بحالتها النهائية الصحيحة
            withContext(NonCancellable) {
                finalizeRecording(dao, recordingId, outFile, startedAt, failed = false)
            }
        } catch (e: Throwable) {
            RemoteLogger.log(level = "ERROR", tag = "RecordingService", message = "recordingId=$recordingId fatal: ${e.message}")
            withContext(NonCancellable) {
                finalizeRecording(dao, recordingId, outFile, startedAt, failed = true)
            }
        } finally {
            activeJobs.remove(recordingId)
            stopIfIdle()
        }
    }

    private suspend fun finalizeRecording(dao: RecordingDao, recordingId: Long, outFile: File, startedAt: Long, failed: Boolean) {
        dao.getRecordingById(recordingId)?.let {
            dao.updateRecording(
                it.copy(
                    status = if (!failed && outFile.length() > 0) "COMPLETED" else "FAILED",
                    durationMs = System.currentTimeMillis() - startedAt,
                    filePath = outFile.absolutePath
                )
            )
        }
    }

    /** يجلب قائمة تشغيل m3u8 (يتبع master playlist تلقائياً إن وُجد) ويستخرج روابط المقاطع المطلَقة */
    private fun fetchSegmentUrls(playlistUrl: String): List<String> {
        val text = executeGetText(playlistUrl) ?: return emptyList()
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }

        // Master playlist: يحتوي متغيّرات بجودات مختلفة — نختار أول واحد فقط ونطلب مقاطعه بدلاً منه
        val variantLine = lines.firstOrNull { it.startsWith("#EXT-X-STREAM-INF") }
        if (variantLine != null) {
            val variantIndex = lines.indexOf(variantLine)
            val variantUri = lines.getOrNull(variantIndex + 1)
            if (variantUri != null && !variantUri.startsWith("#")) {
                return fetchSegmentUrls(resolveUrl(playlistUrl, variantUri))
            }
        }

        return lines.filter { !it.startsWith("#") }.map { resolveUrl(playlistUrl, it) }
    }

    private fun resolveUrl(baseUrl: String, maybeRelative: String): String {
        if (maybeRelative.startsWith("http://") || maybeRelative.startsWith("https://")) return maybeRelative
        return try {
            java.net.URL(java.net.URL(baseUrl), maybeRelative).toString()
        } catch (e: Exception) {
            maybeRelative
        }
    }

    private fun executeGetText(url: String): String? {
        return try {
            client.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
                if (response.isSuccessful) response.body?.string() else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun downloadSegmentInto(segmentUrl: String, out: FileOutputStream) {
        client.newCall(Request.Builder().url(segmentUrl).get().build()).execute().use { response ->
            if (response.isSuccessful) {
                response.body?.byteStream()?.use { input ->
                    input.copyTo(out)
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(CHANNEL_ID, "Recordings", NotificationManager.IMPORTANCE_LOW)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(recordingId: Long, channelName: String): Notification {
        val stopIntent = Intent(this, RecordingService::class.java).apply {
            action = ACTION_STOP
            putExtra(EXTRA_RECORDING_ID, recordingId)
        }
        val stopPendingIntent = PendingIntent.getService(
            this, recordingId.toInt(), stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Recording: $channelName")
            .setContentText("Recording in progress")
            .setSmallIcon(R.drawable.ic_flag_all)
            .setOngoing(true)
            .addAction(0, "Stop", stopPendingIntent)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
