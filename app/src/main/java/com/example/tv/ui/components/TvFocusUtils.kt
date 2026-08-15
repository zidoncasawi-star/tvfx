package com.example.tv.ui.components

import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.delay

/**
 * محاولة تركيز واحدة بعد تأخير قصير (النمط المستخدم سابقاً في كل نوافذ/حوارات التلفاز) قد تفشل
 * بصمت — runCatching يبتلع الاستثناء — إن لم يكن العنصر الهدف قد اكتمل تخطيطه بعد (شاشة بطيئة،
 * صورة خلفية عبر Coil لم تُحمَّل بعد، إلخ)، فتبقى الشاشة بأكملها بلا أي عنصر مركَّز للأبد بلا أي
 * إعادة محاولة. دالة موحَّدة تُعيد المحاولة عدة مرات على فترات متباعدة حتى تنجح فعلاً.
 */
suspend fun FocusRequester.requestFocusWithRetry(
    attempts: Int = 10,
    initialDelayMs: Long = 80L,
    retryDelayMs: Long = 150L
) {
    repeat(attempts) { attempt ->
        delay(if (attempt == 0) initialDelayMs else retryDelayMs)
        val success = runCatching { requestFocus() }.isSuccess
        if (success) return
    }
}
