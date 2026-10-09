package io.github.watervxv.mtpadphotos.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import io.github.watervxv.mtpadphotos.MainActivity
import io.github.watervxv.mtpadphotos.data.local.prefs.SecureKeyStore
import io.github.watervxv.mtpadphotos.data.remote.AuthManager
import io.github.watervxv.mtpadphotos.data.repo.AlbumRepository
import io.github.watervxv.mtpadphotos.data.media.CacheManager
import io.github.watervxv.mtpadphotos.data.repo.SearchRepository
import io.github.watervxv.mtpadphotos.data.repo.SettingsRepository
import io.github.watervxv.mtpadphotos.data.sync.SyncManager
import io.github.watervxv.mtpadphotos.util.NetworkMonitor
import kotlinx.coroutines.launch
import io.github.watervxv.mtpadphotos.ui.main.MainScreen
import io.github.watervxv.mtpadphotos.ui.onboarding.OnboardingScreen
import io.github.watervxv.mtpadphotos.ui.pin.PinGateScreen
import io.github.watervxv.mtpadphotos.ui.settings.SettingsScreen
import io.github.watervxv.mtpadphotos.ui.slideshow.SlideshowScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val MAIN = "main"
    const val SLIDESHOW = "slideshow/{albumId}"
    const val SETTINGS = "settings"
    const val PIN_GATE = "pin_gate"

    fun slideshow(albumId: String) = "slideshow/$albumId"
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    albumRepository: AlbumRepository,
    settingsRepository: SettingsRepository,
    secureKeyStore: SecureKeyStore,
    authManager: AuthManager,
    networkMonitor: NetworkMonitor,
    syncManager: SyncManager,
    searchRepository: SearchRepository,
    cacheManager: CacheManager,
    modifier: Modifier = Modifier
) {
    val onboardingCompleted by settingsRepository.onboardingCompleted.collectAsState()
    val scope = rememberCoroutineScope()
    val startDestination = when {
        !onboardingCompleted -> Routes.ONBOARDING
        secureKeyStore.hasPin() && !MainActivity.pinVerifiedThisProcess -> Routes.PIN_GATE
        else -> Routes.MAIN
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                authManager = authManager,
                settingsRepository = settingsRepository,
                onComplete = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.MAIN) {
            MainScreen(
                albumRepository = albumRepository,
                settingsRepository = settingsRepository,
                authManager = authManager,
                networkMonitor = networkMonitor,
                syncManager = syncManager,
                searchRepository = searchRepository,
                onAlbumClick = { album ->
                    navController.navigate(Routes.slideshow(album.id))
                },
                onSettingsClick = {
                    navController.navigate(Routes.SETTINGS)
                },
                onLogout = {
                    scope.launch { authManager.clearCredentials() }
                    settingsRepository.resetOnboarding()
                    settingsRepository.setInitialSyncCompleted(false)
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = Routes.SLIDESHOW,
            arguments = listOf(navArgument("albumId") { type = NavType.StringType })
        ) { backStackEntry ->
            val albumId = backStackEntry.arguments?.getString("albumId") ?: return@composable
            SlideshowScreen(
                albumId = albumId,
                albumRepository = albumRepository,
                settingsRepository = settingsRepository,
                syncManager = syncManager,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                settingsRepository = settingsRepository,
                cacheManager = cacheManager,
                secureKeyStore = secureKeyStore,
                authManager = authManager,
                onBack = { navController.popBackStack() },
                onServerConfigSaved = {
                    // 服务器配置变更后立即强制重同步，刷新相册与封面
                    scope.launch { syncManager.syncAll(force = true) }
                }
            )
        }
        composable(Routes.PIN_GATE) {
            PinGateScreen(
                secureKeyStore = secureKeyStore,
                onVerified = {
                    MainActivity.pinVerifiedThisProcess = true
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.PIN_GATE) { inclusive = true }
                    }
                }
            )
        }
    }
}
