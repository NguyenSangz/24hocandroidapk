package com.example.data.local.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.core.content.edit
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Lưu trữ phiên đăng nhập PocketBase (CHỈ lưu JWT token & userId, tuyệt đối KHÔNG lưu mật khẩu).
 */
interface TokenStorage {
    fun saveSession(token: String, userId: String)
    fun getToken(): String?
    fun getUserId(): String?
    fun clearSession()
}

/**
 * Lưu trữ token được mã hóa bằng AES-256-GCM với khóa nằm trong Android Keystore.
 */
class KeystoreTokenStorage(context: Context) : TokenStorage {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun saveSession(token: String, userId: String) {
        if (token.isBlank() || userId.isBlank()) return
        try {
            val encryptedToken = encrypt(token)
            prefs.edit {
                putString(KEY_TOKEN_ENC, encryptedToken)
                putString(KEY_USER_ID, userId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Không thể mã hóa token bằng AndroidKeyStore: ${e.message}")
        }
    }

    override fun getToken(): String? {
        val raw = prefs.getString(KEY_TOKEN_ENC, null) ?: return null
        return try {
            decrypt(raw)
        } catch (e: Exception) {
            Log.w(TAG, "Token lưu trữ không giải mã được, xóa phiên cục bộ an toàn: ${e.message}")
            clearSession()
            null
        }
    }

    override fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    override fun clearSession() {
        prefs.edit {
            remove(KEY_TOKEN_ENC)
            remove(KEY_USER_ID)
        }
    }

    private fun getOrCreateAesKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateAesKey())
        val iv = cipher.iv
        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(1 + iv.size + cipherBytes.size)
        combined[0] = iv.size.toByte()
        System.arraycopy(iv, 0, combined, 1, iv.size)
        System.arraycopy(cipherBytes, 0, combined, 1 + iv.size, cipherBytes.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String {
        val combined = Base64.decode(encoded, Base64.NO_WRAP)
        val ivLength = combined[0].toInt() and 0xFF
        val iv = combined.copyOfRange(1, 1 + ivLength)
        val cipherBytes = combined.copyOfRange(1 + ivLength, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateAesKey(), GCMParameterSpec(128, iv))
        val plainBytes = cipher.doFinal(cipherBytes)
        return String(plainBytes, Charsets.UTF_8)
    }

    companion object {
        private const val TAG = "KeystoreTokenStorage"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "hoc_tot_thcs_pb_token_aes_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val PREFS_NAME = "pb_secure_session_prefs"
        private const val KEY_TOKEN_ENC = "pb_token_enc"
        private const val KEY_USER_ID = "pb_user_id"
    }
}

/**
 * TokenStorage trong bộ nhớ dành riêng cho Unit Test / @Preview.
 */
class InMemoryTokenStorage : TokenStorage {
    private var token: String? = null
    private var userId: String? = null

    override fun saveSession(token: String, userId: String) {
        this.token = token
        this.userId = userId
    }

    override fun getToken(): String? = token
    override fun getUserId(): String? = userId

    override fun clearSession() {
        token = null
        userId = null
    }
}
