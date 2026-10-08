package io.github.watervxv.mtpadphotos.data.repo

import io.github.watervxv.mtpadphotos.domain.model.PlayOrder
import io.github.watervxv.mtpadphotos.domain.model.ThemeMode
import io.github.watervxv.mtpadphotos.domain.model.TransitionType
import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val photoDurationSec: StateFlow<Int>
    val playVideo: StateFlow<Boolean>
    val videoSound: StateFlow<Boolean>
    val playOrder: StateFlow<PlayOrder>
    val transitionType: StateFlow<TransitionType>
    val themeMode: StateFlow<ThemeMode>
    val syncFrequency: StateFlow<String>
    val pinEnabled: StateFlow<Boolean>
    val cacheLimitPercent: StateFlow<Int>
    val onboardingCompleted: StateFlow<Boolean>
    val tutorialShown: StateFlow<Boolean>
    val lastAlbumId: StateFlow<String?>
    val romGuideDismissed: StateFlow<Boolean>
    val initialSyncCompleted: StateFlow<Boolean>

    fun setPhotoDurationSec(value: Int)
    fun setPlayVideo(value: Boolean)
    fun setVideoSound(value: Boolean)
    fun setPlayOrder(value: PlayOrder)
    fun setTransitionType(value: TransitionType)
    fun setThemeMode(value: ThemeMode)
    fun setSyncFrequency(value: String)
    fun setPinEnabled(value: Boolean)
    fun setCacheLimitPercent(value: Int)
    fun completeOnboarding()
    fun markTutorialShown()
    fun setLastAlbumId(value: String)
    fun dismissRomGuide()
    fun setInitialSyncCompleted(value: Boolean)
    fun resetOnboarding()
    fun clearAuthCode()
}
