package io.github.watervxv.mtpadphotos.data.repo

import io.github.watervxv.mtpadphotos.data.local.prefs.SettingsStore
import io.github.watervxv.mtpadphotos.domain.model.PlayOrder
import io.github.watervxv.mtpadphotos.domain.model.ThemeMode
import io.github.watervxv.mtpadphotos.domain.model.TransitionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DefaultSettingsRepository(
    private val settingsStore: SettingsStore,
    private val scope: CoroutineScope
) : SettingsRepository {

    override val photoDurationSec: StateFlow<Int> =
        settingsStore.photoDurationSec.stateIn(scope, SharingStarted.Eagerly, 5)
    override val playVideo: StateFlow<Boolean> =
        settingsStore.playVideo.stateIn(scope, SharingStarted.Eagerly, true)
    override val videoSound: StateFlow<Boolean> =
        settingsStore.videoSound.stateIn(scope, SharingStarted.Eagerly, false)
    override val playOrder: StateFlow<PlayOrder> =
        settingsStore.playOrder.stateIn(scope, SharingStarted.Eagerly, PlayOrder.FORWARD)
    override val transitionType: StateFlow<TransitionType> =
        settingsStore.transitionType.stateIn(scope, SharingStarted.Eagerly, TransitionType.FADE)
    override val themeMode: StateFlow<ThemeMode> =
        settingsStore.themeMode.stateIn(scope, SharingStarted.Eagerly, ThemeMode.SYSTEM)
    override val syncFrequency: StateFlow<String> =
        settingsStore.syncFrequency.stateIn(scope, SharingStarted.Eagerly, "30min")
    override val pinEnabled: StateFlow<Boolean> =
        settingsStore.pinEnabled.stateIn(scope, SharingStarted.Eagerly, false)
    override val cacheLimitPercent: StateFlow<Int> =
        settingsStore.cacheLimitPercent.stateIn(scope, SharingStarted.Eagerly, 80)
    override val onboardingCompleted: StateFlow<Boolean> =
        settingsStore.onboardingCompleted.stateIn(scope, SharingStarted.Eagerly, false)
    override val tutorialShown: StateFlow<Boolean> =
        settingsStore.tutorialShown.stateIn(scope, SharingStarted.Eagerly, false)
    override val lastAlbumId: StateFlow<String?> =
        settingsStore.lastAlbumId.stateIn(scope, SharingStarted.Eagerly, null)
    override val romGuideDismissed: StateFlow<Boolean> =
        settingsStore.romGuideDismissed.stateIn(scope, SharingStarted.Eagerly, false)
    override val initialSyncCompleted: StateFlow<Boolean> =
        settingsStore.initialSyncCompleted.stateIn(scope, SharingStarted.Eagerly, false)

    override fun setPhotoDurationSec(value: Int) { scope.launch { settingsStore.setPhotoDurationSec(value) } }
    override fun setPlayVideo(value: Boolean) { scope.launch { settingsStore.setPlayVideo(value) } }
    override fun setVideoSound(value: Boolean) { scope.launch { settingsStore.setVideoSound(value) } }
    override fun setPlayOrder(value: PlayOrder) { scope.launch { settingsStore.setPlayOrder(value) } }
    override fun setTransitionType(value: TransitionType) { scope.launch { settingsStore.setTransitionType(value) } }
    override fun setThemeMode(value: ThemeMode) { scope.launch { settingsStore.setThemeMode(value) } }
    override fun setSyncFrequency(value: String) { scope.launch { settingsStore.setSyncFrequency(value) } }
    override fun setPinEnabled(value: Boolean) { scope.launch { settingsStore.setPinEnabled(value) } }
    override fun setCacheLimitPercent(value: Int) { scope.launch { settingsStore.setCacheLimitPercent(value) } }
    override fun completeOnboarding() { scope.launch { settingsStore.completeOnboarding() } }
    override fun markTutorialShown() { scope.launch { settingsStore.markTutorialShown() } }
    override fun setLastAlbumId(value: String) { scope.launch { settingsStore.setLastAlbumId(value) } }
    override fun dismissRomGuide() { scope.launch { settingsStore.dismissRomGuide() } }
    override fun setInitialSyncCompleted(value: Boolean) { scope.launch { settingsStore.setInitialSyncCompleted(value) } }
    override fun resetOnboarding() { scope.launch { settingsStore.resetOnboarding() } }
    override fun clearAuthCode() { scope.launch { settingsStore.clearAuthCode() } }
}
