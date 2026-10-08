package io.github.watervxv.mtpadphotos

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import io.github.watervxv.mtpadphotos.di.AppContainer
import io.github.watervxv.mtpadphotos.ui.navigation.AppNavHost
import io.github.watervxv.mtpadphotos.ui.theme.MTPadPhotosTheme

class MainActivity : ComponentActivity() {

    companion object {
        @Volatile
        var pinVerifiedThisProcess = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars()

        val appContainer = (application as MTPadPhotosApp).appContainer

        setContent {
            val themeMode by appContainer.settingsRepository.themeMode.collectAsState()
            MTPadPhotosTheme(themeMode = themeMode) {
                val navController = rememberNavController()
                AppNavHost(
                    navController = navController,
                    albumRepository = appContainer.albumRepository,
                    settingsRepository = appContainer.settingsRepository,
                    secureKeyStore = appContainer.secureKeyStore,
                    authManager = appContainer.authManager,
                    networkMonitor = appContainer.networkMonitor,
                    syncManager = appContainer.syncManager,
                    searchRepository = appContainer.searchRepository,
                    cacheManager = appContainer.cacheManager,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // MIUI 等 ROM 在窗口失焦/重获焦点后会重新显示系统栏，这里重新隐藏
        if (hasFocus) {
            window.decorView.post { hideSystemBars() }
        }
    }

    /**
     * 沉浸模式：隐藏状态栏/导航栏，消除主题色上下边条；边缘滑动可临时唤出系统栏。
     * 新 API（WindowInsetsControllerCompat）与旧版沉浸标志双保险，兼顾 OEM ROM 兼容性。
     */
    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
    }
}
