package io.github.watervxv.mtpadphotos.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.watervxv.mtpadphotos.domain.model.PlayOrder
import io.github.watervxv.mtpadphotos.domain.model.ThemeMode
import io.github.watervxv.mtpadphotos.domain.model.TransitionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "mtpadphotos_settings")

class SettingsStore(private val context: Context) {

    private val dataStore = context.settingsDataStore

    private object Keys {
        val PHOTO_DURATION = intPreferencesKey("photo_duration_sec")
        val PLAY_VIDEO = booleanPreferencesKey("play_video")
        val VIDEO_SOUND = booleanPreferencesKey("video_sound")
        val PLAY_ORDER = stringPreferencesKey("play_order")
        val TRANSITION_TYPE = stringPreferencesKey("transition_type")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SYNC_FREQUENCY = stringPreferencesKey("sync_frequency")
        val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val CACHE_LIMIT = intPreferencesKey("cache_limit_percent")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val TUTORIAL_SHOWN = booleanPreferencesKey("tutorial_shown")
        val AUTH_CODE = stringPreferencesKey("auth_code")
        val AUTH_CODE_EXPIRES_AT = longPreferencesKey("auth_code_expires_at")
        val LAST_ALBUM_ID = stringPreferencesKey("last_album_id")
        val ROM_GUIDE_DISMISSED = booleanPreferencesKey("rom_guide_dismissed")
        val INITIAL_SYNC_COMPLETED = booleanPreferencesKey("initial_sync_completed")
    }

    val photoDurationSec: Flow<Int> = dataStore.data.map { it[Keys.PHOTO_DURATION] ?: 5 }
    val playVideo: Flow<Boolean> = dataStore.data.map { it[Keys.PLAY_VIDEO] ?: true }
    val videoSound: Flow<Boolean> = dataStore.data.map { it[Keys.VIDEO_SOUND] ?: false }
    val playOrder: Flow<PlayOrder> = dataStore.data.map {
        try {
            PlayOrder.valueOf(it[Keys.PLAY_ORDER] ?: PlayOrder.FORWARD.name)
        } catch (_: Exception) {
            PlayOrder.FORWARD
        }
    }
    val transitionType: Flow<TransitionType> = dataStore.data.map {
        try {
            TransitionType.valueOf(it[Keys.TRANSITION_TYPE] ?: TransitionType.FADE.name)
        } catch (_: Exception) {
            TransitionType.FADE
        }
    }
    val themeMode: Flow<ThemeMode> = dataStore.data.map {
        try {
            ThemeMode.valueOf(it[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    }
    val syncFrequency: Flow<String> = dataStore.data.map { it[Keys.SYNC_FREQUENCY] ?: "30min" }
    val pinEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.PIN_ENABLED] ?: false }
    val cacheLimitPercent: Flow<Int> = dataStore.data.map { it[Keys.CACHE_LIMIT] ?: 80 }
    val onboardingCompleted: Flow<Boolean> = dataStore.data.map { it[Keys.ONBOARDING_COMPLETED] ?: false }
    val tutorialShown: Flow<Boolean> = dataStore.data.map { it[Keys.TUTORIAL_SHOWN] ?: false }
    val authCode: Flow<String?> = dataStore.data.map { it[Keys.AUTH_CODE] }
    val authCodeExpiresAt: Flow<Long> = dataStore.data.map { it[Keys.AUTH_CODE_EXPIRES_AT] ?: 0L }
    val lastAlbumId: Flow<String?> = dataStore.data.map { it[Keys.LAST_ALBUM_ID] }
    val romGuideDismissed: Flow<Boolean> = dataStore.data.map { it[Keys.ROM_GUIDE_DISMISSED] ?: false }
    val initialSyncCompleted: Flow<Boolean> = dataStore.data.map { it[Keys.INITIAL_SYNC_COMPLETED] ?: false }

    suspend fun setPhotoDurationSec(value: Int) = dataStore.edit { it[Keys.PHOTO_DURATION] = value }
    suspend fun setPlayVideo(value: Boolean) = dataStore.edit { it[Keys.PLAY_VIDEO] = value }
    suspend fun setVideoSound(value: Boolean) = dataStore.edit { it[Keys.VIDEO_SOUND] = value }
    suspend fun setPlayOrder(value: PlayOrder) = dataStore.edit { it[Keys.PLAY_ORDER] = value.name }
    suspend fun setTransitionType(value: TransitionType) = dataStore.edit { it[Keys.TRANSITION_TYPE] = value.name }
    suspend fun setThemeMode(value: ThemeMode) = dataStore.edit { it[Keys.THEME_MODE] = value.name }
    suspend fun setSyncFrequency(value: String) = dataStore.edit { it[Keys.SYNC_FREQUENCY] = value }
    suspend fun setPinEnabled(value: Boolean) = dataStore.edit { it[Keys.PIN_ENABLED] = value }
    suspend fun setCacheLimitPercent(value: Int) = dataStore.edit { it[Keys.CACHE_LIMIT] = value }
    suspend fun completeOnboarding() = dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = true }
    suspend fun resetOnboarding() = dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = false }
    suspend fun markTutorialShown() = dataStore.edit { it[Keys.TUTORIAL_SHOWN] = true }
    suspend fun saveAuthCode(code: String, expiresAt: Long) = dataStore.edit {
        it[Keys.AUTH_CODE] = code
        it[Keys.AUTH_CODE_EXPIRES_AT] = expiresAt
    }
    suspend fun setLastAlbumId(value: String) = dataStore.edit { it[Keys.LAST_ALBUM_ID] = value }
    suspend fun dismissRomGuide() = dataStore.edit { it[Keys.ROM_GUIDE_DISMISSED] = true }
    suspend fun setInitialSyncCompleted(value: Boolean) = dataStore.edit { it[Keys.INITIAL_SYNC_COMPLETED] = value }
    suspend fun clearAuthCode() = dataStore.edit {
        it.remove(Keys.AUTH_CODE)
        it.remove(Keys.AUTH_CODE_EXPIRES_AT)
    }
}
