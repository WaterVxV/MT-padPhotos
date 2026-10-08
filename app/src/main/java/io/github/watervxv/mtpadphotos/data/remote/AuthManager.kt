package io.github.watervxv.mtpadphotos.data.remote

import io.github.watervxv.mtpadphotos.data.local.prefs.SecureKeyStore
import io.github.watervxv.mtpadphotos.data.local.prefs.SettingsStore
import io.github.watervxv.mtpadphotos.data.remote.dto.AuthCodeRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.net.URLEncoder

class AuthManager(
    private val api: MtPhotoApi,
    private val secureKeyStore: SecureKeyStore,
    private val settingsStore: SettingsStore
) {
    companion object {
        private const val AUTH_CODE_TTL_MS = 24 * 60 * 60 * 1000L
        private const val REFRESH_THRESHOLD_MS = 60 * 60 * 1000L
    }

    val authCode: Flow<String?> = settingsStore.authCode
    val authCodeExpiresAt: Flow<Long> = settingsStore.authCodeExpiresAt

    fun getServerUrl(): String? = secureKeyStore.getServerUrl()
    fun getApiKey(): String? = secureKeyStore.getApiKey()

    suspend fun ensureAuthCode(): Result<String> {
        val now = System.currentTimeMillis()
        val currentCode = settingsStore.authCode.first()
        val expiresAt = settingsStore.authCodeExpiresAt.first()

        if (!currentCode.isNullOrBlank() && now < (expiresAt - REFRESH_THRESHOLD_MS)) {
            return Result.success(currentCode)
        }

        return refreshAuthCode()
    }

    /**
     * 离线兑底：优先换新码；服务器不可达时回退到本地存储的（可能已过期的）auth_code，
     * 让 URL 仍可构建，命中图片磁盘缓存（缓存 key 已剥离 auth_code）。
     */
    suspend fun usableAuthCode(): String? =
        ensureAuthCode().getOrElse { settingsStore.authCode.first() }

    suspend fun refreshAuthCode(): Result<String> {
        val apiKey = secureKeyStore.getApiKey()
            ?: return Result.failure(IllegalStateException("API Key 未配置"))

        return try {
            val response = api.getAuthCode(AuthCodeRequest(apiKey = apiKey))
            if (response.isSuccessful) {
                val code = response.body()?.authCode
                if (!code.isNullOrBlank()) {
                    val expiresAt = System.currentTimeMillis() + AUTH_CODE_TTL_MS
                    settingsStore.saveAuthCode(code, expiresAt)
                    Result.success(code)
                } else {
                    // MT Photo 对无效 Key 也返回 201，真实原因在响应的 msg 字段
                    val serverMsg = response.body()?.msg
                    Result.failure(
                        IllegalStateException(
                            serverMsg?.takeIf { it.isNotBlank() }
                                ?: "服务器未返回 auth_code，请检查 API Key 与服务器版本"
                        )
                    )
                }
            } else {
                Result.failure(ApiException(response.code(), "获取 auth_code 失败: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun encodeAuthCodeForUrl(code: String): String =
        URLEncoder.encode(code, "UTF-8")

    fun saveCredentials(serverUrl: String, apiKey: String) {
        secureKeyStore.saveServerUrl(serverUrl.trimEnd('/'))
        secureKeyStore.saveApiKey(apiKey)
    }

    suspend fun clearCredentials() {
        secureKeyStore.clearApiKey()
        settingsStore.clearAuthCode()
    }
}

class ApiException(val code: Int, message: String) : Exception(message)
