package io.github.watervxv.mtpadphotos.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.watervxv.mtpadphotos.di.AppContainer
import java.util.concurrent.TimeUnit

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as io.github.watervxv.mtpadphotos.MTPadPhotosApp).appContainer
        // 复用容器的 SyncManager（假数据模式下 syncAll 自动跳过）
        return container.syncManager.syncAll().fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() }
        )
    }

    companion object {
        private const val WORK_NAME = "mtpadphotos_sync"

        fun schedule(context: Context, frequencyKey: String) {
            val workManager = WorkManager.getInstance(context)
            when (frequencyKey) {
                "off", "manual" -> workManager.cancelUniqueWork(WORK_NAME)
                else -> {
                    val minutes = when (frequencyKey) {
                        "15min" -> 15L
                        "30min" -> 30L
                        "1h" -> 60L
                        else -> 30L
                    }
                    val request = PeriodicWorkRequestBuilder<SyncWorker>(minutes, TimeUnit.MINUTES)
                        .setConstraints(
                            androidx.work.Constraints.Builder()
                                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                                .setRequiresBatteryNotLow(false)
                                .build()
                        )
                        .build()
                    workManager.enqueueUniquePeriodicWork(
                        WORK_NAME,
                        ExistingPeriodicWorkPolicy.UPDATE,
                        request
                    )
                }
            }
        }
    }
}
