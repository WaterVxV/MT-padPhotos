package io.github.watervxv.mtpadphotos

import android.app.Application
import android.os.StatFs
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.key.Keyer
import coil.request.Options
import io.github.watervxv.mtpadphotos.di.AppContainer
import java.io.File

class MTPadPhotosApp : Application() {
    // 必须在 onCreate() 里初始化：字段初始化器在 Application 构造阶段执行，
    // 此时 this 还不是有效 Context（getApplicationContext() 返回 null），
    // 会导致 SecureKeyStore -> MasterKey.Builder(context) 抛 NPE。
    lateinit var appContainer: AppContainer

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
        setupCoil()
    }

    private fun setupCoil() {
        val cacheDir = File(cacheDir, "image_cache").apply { mkdirs() }
        val maxBytes = computeCacheLimitBytes(80)
        val imageLoader = ImageLoader.Builder(this)
            .components { add(StripAuthCodeKeyer()) }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir)
                    .maxSizeBytes(maxBytes)
                    .build()
            }
            .crossfade(true)
            .build()
        Coil.setImageLoader(imageLoader)
    }

    private fun computeCacheLimitBytes(percentOfAvailable: Int): Long {
        return try {
            val stat = StatFs(filesDir.path)
            val available = stat.availableBlocksLong * stat.blockSizeLong
            (available * percentOfAvailable / 100L).coerceAtLeast(100 * 1024 * 1024L)
        } catch (_: Exception) {
            512 * 1024 * 1024L
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        appContainer.onTerminate()
    }
}

/**
 * 图片缓存 key 剥离 auth_code 参数：
 * auth_code 每 24 小时轮换，若参与缓存 key，换码当天全部缓存失效。
 * 剥离后 key 只由 URL 路径+其余参数决定，磁盘缓存跨换码周期复用，
 * 且断网时（使用过期 auth_code 兑底）也能命中缓存。
 * 返回 null 时 Coil 回退到默认 keyer（原始 URL），适用于非 NAS 图片。
 */
private class StripAuthCodeKeyer : Keyer<String> {
    override fun key(data: String, options: Options): String? {
        // 非标准 http(s) URL 交回默认 keyer
        val schemeEnd = data.indexOf("://")
        if (schemeEnd < 0) return null
        val pathStart = data.indexOf('/', schemeEnd + 3)
        if (pathStart < 0) return null
        val pathAndQuery = data.substring(pathStart)
        val qs = pathAndQuery.indexOf('?')
        if (qs < 0) return pathAndQuery
        // 剥离 auth_code（24h 轮换），并去掉 scheme://host 前缀：
        // 服务器换地址/换端口后缓存 key 不变，离线缓存与封面缓存全部继续有效
        val kept = pathAndQuery.substring(qs + 1)
            .split('&')
            .filterNot { it.startsWith("auth_code=") }
        return if (kept.isEmpty()) pathAndQuery.substring(0, qs)
        else "${pathAndQuery.substring(0, qs)}?${kept.joinToString("&")}"
    }
}
