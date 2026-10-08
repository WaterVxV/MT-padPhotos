package io.github.watervxv.mtpadphotos.data.media

import android.content.Context
import android.os.StatFs
import coil.Coil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class CacheManager(private val context: Context) {

    private val cacheDir: File
        get() = File(context.cacheDir, "image_cache").apply { mkdirs() }

    suspend fun clearCache(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Coil.imageLoader(context).diskCache?.clear()
            cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cacheSizeBytes(): Long = withContext(Dispatchers.IO) {
        runCatching {
            var size = 0L
            cacheDir.listFiles()?.forEach { file ->
                size += file.walkTopDown().map { if (it.isFile) it.length() else 0L }.sum()
            }
            size
        }.getOrDefault(0L)
    }

    fun computeMaxBytes(percentOfAvailable: Int): Long {
        return try {
            val stat = StatFs(context.filesDir.path)
            val available = stat.availableBlocksLong * stat.blockSizeLong
            (available * percentOfAvailable / 100L).coerceAtLeast(100 * 1024 * 1024L)
        } catch (_: Exception) {
            512 * 1024 * 1024L
        }
    }
}
