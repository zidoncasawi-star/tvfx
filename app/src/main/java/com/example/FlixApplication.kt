package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

// يضبط إعدادات تحميل وتخزين صور الأفلام/المسلسلات (Coil) لتقليل التقطّع أثناء التمرير السريع:
// - تخزين مؤقت في الذاكرة (25% من ذاكرة التطبيق المتاحة) لتجنب إعادة تحميل نفس الصورة عند التمرير للأعلى/الأسفل
// - تخزين مؤقت على القرص (250 ميجا) لتجنب إعادة التحميل من الشبكة بعد إغلاق التطبيق
// - crossfade أسرع (150ms بدل الافتراضي) لإحساس أكثر سلاسة
class FlixApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(250L * 1024 * 1024)
                    .build()
            }
            .crossfade(150)
            .respectCacheHeaders(false)
            .build()
    }
}
