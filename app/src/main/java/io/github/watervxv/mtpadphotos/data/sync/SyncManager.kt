package io.github.watervxv.mtpadphotos.data.sync

import io.github.watervxv.mtpadphotos.data.local.db.DeleteLogCursorDao
import io.github.watervxv.mtpadphotos.data.local.db.DeleteLogCursorEntity
import io.github.watervxv.mtpadphotos.data.local.db.MediaFileDao
import io.github.watervxv.mtpadphotos.data.remote.AuthManager
import io.github.watervxv.mtpadphotos.data.remote.MtPhotoApi
import io.github.watervxv.mtpadphotos.data.remote.dto.DeleteLogItemDto
import io.github.watervxv.mtpadphotos.data.repo.DefaultAlbumRepository
import io.github.watervxv.mtpadphotos.util.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicInteger

data class SyncProgress(
    val isSyncing: Boolean = false,
    val total: Int = 0,
    val processed: Int = 0,
    val message: String = "",
    val error: String? = null
)

class SyncManager(
    // 假数据模式下传 null，syncAll 直接跳过
    private val albumRepository: DefaultAlbumRepository?,
    private val api: MtPhotoApi?,
    private val authManager: AuthManager,
    private val mediaFileDao: MediaFileDao?,
    private val deleteLogCursorDao: DeleteLogCursorDao?,
    val connectionMonitor: ConnectionMonitor,
    private val networkMonitor: NetworkMonitor
) {
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError

    // /file-delete-log 返回 403（非管理员 Key）时置 true，UI 可提示已降级为客户端 diff
    private val _deleteLogDegraded = MutableStateFlow(false)
    val deleteLogDegraded: StateFlow<Boolean> = _deleteLogDegraded

    private val _syncProgress = MutableStateFlow(SyncProgress())
    val syncProgress: StateFlow<SyncProgress> = _syncProgress

    /** 标识当前是否连接真实数据层；假数据模式下 UI 不应显示同步遮罩。 */
    val isRealSyncEnabled = albumRepository != null && api != null

    /** 上次同步成功时间（进程内），用于节流，避免每次返回主界面都全量重同步。 */
    @Volatile
    private var lastSyncSuccessAt: Long = 0L

    companion object {
        /** 距上次成功同步不足该间隔时，非 force 调用直接跳过。 */
        private const val MIN_SYNC_INTERVAL_MS = 5 * 60 * 1000L
    }

    suspend fun syncAll(force: Boolean = false): Result<Unit> {
        if (albumRepository == null || api == null) {
            // 假数据验收模式：不执行真实同步，进度置为完成态
            _syncProgress.value = SyncProgress(isSyncing = false, total = 0, processed = 0)
            return Result.success(Unit)
        }
        if (!networkMonitor.isOnlineNow()) {
            val error = "无网络连接"
            _syncProgress.value = SyncProgress(error = error)
            connectionMonitor.onUnreachable()
            return Result.failure(IllegalStateException(error))
        }
        // 防止并发同步（例如下拉刷新时后台同步正在进行）
        if (_isSyncing.value) {
            return Result.success(Unit)
        }
        // 节流：距上次成功同步不足 5 分钟时跳过（下拉刷新/重试用 force = true 绕过）
        if (!force && lastSyncSuccessAt > 0 &&
            System.currentTimeMillis() - lastSyncSuccessAt < MIN_SYNC_INTERVAL_MS
        ) {
            return Result.success(Unit)
        }
        _isSyncing.value = true
        _lastError.value = null
        _syncProgress.value = SyncProgress(isSyncing = true, message = "正在统计照片数量…")
        return try {
            authManager.ensureAuthCode().getOrThrow()
            val repo = albumRepository!!

            val processed = AtomicInteger(0)
            val progress: (Int) -> Unit = { count ->
                _syncProgress.value = _syncProgress.value.copy(processed = processed.addAndGet(count))
            }

            _syncProgress.value = _syncProgress.value.copy(total = 0, message = "正在同步相册列表…")
            repo.syncAlbums(onProgress = progress).getOrThrow()

            // 只同步相册列表与删除日志；全量时间线/视频/往年今日已随智能相册下线移除
            _syncProgress.value = _syncProgress.value.copy(message = "正在清理本地记录…")
            syncDeleteLog()

            _syncProgress.value = SyncProgress(isSyncing = false, total = 0, processed = 0)
            lastSyncSuccessAt = System.currentTimeMillis()
            connectionMonitor.onConnected()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is java.io.IOException) connectionMonitor.onUnreachable()
            _lastError.value = e.message
            _syncProgress.value = _syncProgress.value.copy(isSyncing = false, error = e.message)
            Result.failure(e)
        } finally {
            _isSyncing.value = false
        }
    }

    private suspend fun syncDeleteLog() {
        val api = this.api ?: return
        val mediaFileDao = this.mediaFileDao ?: return
        val deleteLogCursorDao = this.deleteLogCursorDao ?: return
        val cursor = deleteLogCursorDao.get() ?: DeleteLogCursorEntity()
        var pageNo = cursor.lastPageNo.coerceAtLeast(1)
        var lastDeleteTime = cursor.lastDeleteTime
        var consumedAll = false
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            .apply { timeZone = TimeZone.getTimeZone("UTC") }

        while (!consumedAll) {
            val response = api.getDeleteLog(pageSize = 20, pageNo = pageNo)
            if (!response.isSuccessful) {
                if (response.code() == 403) {
                    // 非管理员 Key，记录降级，不再重试
                    _deleteLogDegraded.value = true
                    return
                }
                throw io.github.watervxv.mtpadphotos.data.repo.ApiException(
                    response.code(),
                    "删除日志同步失败"
                )
            }
            val body = response.body() ?: break
            val list = body.list ?: emptyList()
            if (list.isEmpty()) {
                consumedAll = true
                break
            }

            val newItems = mutableListOf<DeleteLogItemDto>()
            for (item in list) {
                val time = item.deleteTime?.let { parseDeleteTime(it, formatter) } ?: 0L
                if (time <= lastDeleteTime) {
                    consumedAll = true
                    break
                }
                newItems.add(item)
                if (time > lastDeleteTime) lastDeleteTime = time
            }

            if (newItems.isNotEmpty()) {
                mediaFileDao.markDeletedGlobally(newItems.map { it.id })
            }

            if (list.size < 20) {
                consumedAll = true
            } else {
                pageNo++
            }
        }

        deleteLogCursorDao.save(
            DeleteLogCursorEntity(
                lastDeleteTime = lastDeleteTime,
                lastPageNo = if (consumedAll) 1 else pageNo,
                consumedAll = consumedAll
            )
        )
    }

    private fun parseDeleteTime(time: String, formatter: SimpleDateFormat): Long {
        return try {
            formatter.parse(time)?.time ?: 0L
        } catch (_: Exception) {
            0L
        }
    }
}
