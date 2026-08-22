package com.example.util

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object SecurityUtils {
    private const val ALGORITHM = "AES"
    private val KEY_BYTES = byteArrayOf(
        0x46, 0x6C, 0x69, 0x78, 0x54, 0x56, 0x53, 0x65,
        0x63, 0x75, 0x72, 0x65, 0x4B, 0x65, 0x79, 0x31
    )

    /** تشفير عكسي — يُستخدم فقط لكلمات مرور Xtream الخاصة بقوائم التشغيل، والتي يحتاج
        التطبيق نفسه لاستعادتها لاحقاً لبناء روابط البث. غير مناسب إطلاقاً لكلمة مرور حساب
        المستخدم (استخدمي hashPassword/verifyPassword لتلك بدلاً منه) */
    fun encrypt(plainText: String): String {
        if (plainText.isBlank()) return ""
        return try {
            val key = SecretKeySpec(KEY_BYTES, ALGORITHM)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            plainText
        }
    }

    fun decrypt(cipherText: String): String {
        if (cipherText.isBlank()) return ""
        return try {
            val key = SecretKeySpec(KEY_BYTES, ALGORITHM)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, key)
            val decodedBytes = Base64.decode(cipherText, Base64.NO_WRAP)
            String(cipher.doFinal(decodedBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            cipherText
        }
    }

    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PBKDF2_ITERATIONS = 120_000
    private const val PBKDF2_KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16

    /** تشفير أحادي الاتجاه حقيقي (PBKDF2 + ملح عشوائي لكل مستخدم) لكلمة مرور حساب المستخدم —
        لا يمكن استرجاع كلمة المرور الأصلية منه إطلاقاً حتى مع معرفة الكود بالكامل، بخلاف
        encrypt/decrypt أعلاه. الصيغة المخزَّنة: "ملح_Base64:هاش_Base64" */
    fun hashPassword(passwordRaw: String): String {
        val salt = ByteArray(SALT_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = pbkdf2(passwordRaw, salt)
        return "${Base64.encodeToString(salt, Base64.NO_WRAP)}:${Base64.encodeToString(hash, Base64.NO_WRAP)}"
    }

    fun verifyPassword(passwordRaw: String, stored: String): Boolean {
        return try {
            val parts = stored.split(":")
            if (parts.size != 2) return false
            val salt = Base64.decode(parts[0], Base64.NO_WRAP)
            val expectedHash = Base64.decode(parts[1], Base64.NO_WRAP)
            val actualHash = pbkdf2(passwordRaw, salt)
            actualHash.contentEquals(expectedHash)
        } catch (e: Exception) {
            false
        }
    }

    private fun pbkdf2(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        return factory.generateSecret(spec).encoded
    }
}
