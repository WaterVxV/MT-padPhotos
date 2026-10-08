package io.github.watervxv.mtpadphotos.di

import android.content.Context
import androidx.room.Room
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.github.watervxv.mtpadphotos.data.local.db.AppDatabase
import io.github.watervxv.mtpadphotos.data.local.prefs.SecureKeyStore
import io.github.watervxv.mtpadphotos.data.local.prefs.SettingsStore
import io.github.watervxv.mtpadphotos.data.media.CacheManager
import io.github.watervxv.mtpadphotos.data.remote.AuthInterceptor
import io.github.watervxv.mtpadphotos.data.remote.DynamicBaseUrlInterceptor
import io.github.watervxv.mtpadphotos.data.remote.AuthManager
import io.github.watervxv.mtpadphotos.data.remote.MtPhotoApi
import io.github.watervxv.mtpadphotos.data.repo.AlbumRepository
import io.github.watervxv.mtpadphotos.data.repo.DefaultAlbumRepository
import io.github.watervxv.mtpadphotos.data.repo.DefaultSettingsRepository
import io.github.watervxv.mtpadphotos.data.repo.SearchRepository
import io.github.watervxv.mtpadphotos.data.repo.SettingsRepository
import io.github.watervxv.mtpadphotos.data.sync.SyncManager
import io.github.watervxv.mtpadphotos.util.NetworkMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

class AppContainer(context: Context) {

    companion object {
        /**
         * 假数据验收开关（M1 验收门禁）：
         * true  = 使用 FakeAlbumRepository，界面全流程用假数据，不同步真实 NAS；
         * false = 使用 DefaultAlbumRepository + Room + MT Photo 真实数据层。
         * M1 验收通过后改回 false 即可恢复数据层。
         */
        const val USE_FAKE_DATA = false

    }

    private val applicationScope = kotlinx.coroutines.CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val secureKeyStore: SecureKeyStore = SecureKeyStore(context)
    val settingsStore: SettingsStore = SettingsStore(context)

    val settingsRepository: SettingsRepository by lazy {
        DefaultSettingsRepository(settingsStore, applicationScope)
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        isLenient = true
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor(DynamicBaseUrlInterceptor { secureKeyStore.getServerUrl() })
            .addInterceptor(AuthInterceptor { secureKeyStore.getApiKey() })
            .addInterceptor(logging)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        val baseUrl = secureKeyStore.getServerUrl()?.trimEnd('/') ?: "http://localhost/"
        Retrofit.Builder()
            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    val mtPhotoApi: MtPhotoApi by lazy { retrofit.create(MtPhotoApi::class.java) }

    val authManager: AuthManager by lazy {
        AuthManager(mtPhotoApi, secureKeyStore, settingsStore)
    }

    val networkMonitor: NetworkMonitor by lazy { NetworkMonitor(context) }

    val cacheManager: CacheManager by lazy { CacheManager(context) }

    val syncManager: SyncManager by lazy {
        val realMode = !USE_FAKE_DATA
        SyncManager(
            albumRepository = albumRepository as? DefaultAlbumRepository,
            api = if (realMode) mtPhotoApi else null,
            authManager = authManager,
            mediaFileDao = if (realMode) database.mediaFileDao() else null,
            deleteLogCursorDao = if (realMode) database.deleteLogCursorDao() else null,
            networkMonitor = networkMonitor
        )
    }

    val database: AppDatabase by lazy {
        Room.databaseBuilder(context, AppDatabase::class.java, "mtpadphotos.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    val albumRepository: AlbumRepository by lazy {
        when {
            USE_FAKE_DATA -> io.github.watervxv.mtpadphotos.data.repo.FakeAlbumRepository()
            else -> DefaultAlbumRepository(
                api = mtPhotoApi,
                albumDao = database.albumDao(),
                mediaFileDao = database.mediaFileDao(),
                playPositionDao = database.playPositionDao(),
                authManager = authManager
            )
        }
    }

    val searchRepository: SearchRepository by lazy {
        SearchRepository(mtPhotoApi, authManager)
    }

    fun onTerminate() {
        applicationScope.cancel()
    }
}
