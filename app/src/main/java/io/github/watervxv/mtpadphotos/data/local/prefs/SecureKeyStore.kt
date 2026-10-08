package io.github.watervxv.mtpadphotos.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class SecureKeyStore(context: Context) {

    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "mtpadphotos_secure_keys",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_API_KEY = "api_key"
        private const val KEY_SERVER_URL = "server_url"

        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_SEC_QUESTION = "sec_question"
        private const val KEY_SEC_ANSWER_HASH = "sec_answer_hash"
        private const val KEY_SEC_ANSWER_SALT = "sec_answer_salt"

        private const val PBKDF2_ITERATIONS = 10000
        private const val SALT_BYTES = 16
    }

    fun saveApiKey(key: String) = prefs.edit().putString(KEY_API_KEY, key).apply()
    fun getApiKey(): String? = prefs.getString(KEY_API_KEY, null)
    fun clearApiKey() = prefs.edit().remove(KEY_API_KEY).apply()

    fun saveServerUrl(url: String) = prefs.edit().putString(KEY_SERVER_URL, url).apply()
    fun getServerUrl(): String? = prefs.getString(KEY_SERVER_URL, null)

    // ---- PIN 锁 ----

    fun savePin(pin: String) {
        val salt = newSalt()
        val hash = hashWithSalt(normalize(pin), salt)
        prefs.edit()
            .putString(KEY_PIN_SALT, encode(salt))
            .putString(KEY_PIN_HASH, hash)
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val saltB64 = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val hash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return hash == hashWithSalt(normalize(pin), decode(saltB64))
    }

    fun hasPin(): Boolean = prefs.contains(KEY_PIN_HASH) && prefs.contains(KEY_PIN_SALT)

    fun clearPin() = prefs.edit().remove(KEY_PIN_HASH).remove(KEY_PIN_SALT).apply()

    // ---- 安全问题找回 ----

    fun saveSecurityQuestion(question: String, answer: String) {
        val salt = newSalt()
        val hash = hashWithSalt(normalize(answer), salt)
        prefs.edit()
            .putString(KEY_SEC_QUESTION, question.trim())
            .putString(KEY_SEC_ANSWER_SALT, encode(salt))
            .putString(KEY_SEC_ANSWER_HASH, hash)
            .apply()
    }

    fun verifySecurityAnswer(answer: String): Boolean {
        val saltB64 = prefs.getString(KEY_SEC_ANSWER_SALT, null) ?: return false
        val hash = prefs.getString(KEY_SEC_ANSWER_HASH, null) ?: return false
        return hash == hashWithSalt(normalize(answer), decode(saltB64))
    }

    fun getSecurityQuestion(): String? = prefs.getString(KEY_SEC_QUESTION, null)

    fun clearSecurityQuestion() = prefs.edit()
        .remove(KEY_SEC_QUESTION)
        .remove(KEY_SEC_ANSWER_HASH)
        .remove(KEY_SEC_ANSWER_SALT)
        .apply()

    /**
     * 退出登录用：一次性清除 API Key、服务器地址与 PIN / 安全问题。
     * 不触碰本地数据库、磁盘缓存与播放偏好。
     */
    fun clearAll() {
        prefs.edit()
            .remove(KEY_API_KEY)
            .remove(KEY_SERVER_URL)
            .remove(KEY_PIN_HASH)
            .remove(KEY_PIN_SALT)
            .remove(KEY_SEC_QUESTION)
            .remove(KEY_SEC_ANSWER_HASH)
            .remove(KEY_SEC_ANSWER_SALT)
            .apply()
    }

    // ---- 工具 ----

    private fun normalize(s: String): String = s.trim().lowercase()

    private fun newSalt(): ByteArray {
        val salt = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(salt)
        return salt
    }

    private fun encode(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun decode(s: String): ByteArray =
        Base64.decode(s, Base64.NO_WRAP)

    private fun hashWithSalt(value: String, salt: ByteArray): String {
        val spec = PBEKeySpec(value.toCharArray(), salt, PBKDF2_ITERATIONS, 256)
        val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec)
            .encoded
        return encode(key)
    }
}
