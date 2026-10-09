package io.github.watervxv.mtpadphotos.data.repo

import io.github.watervxv.mtpadphotos.data.local.db.AlbumDao
import io.github.watervxv.mtpadphotos.data.local.db.AlbumEntity
import io.github.watervxv.mtpadphotos.data.local.db.IdMd5Tuple
import io.github.watervxv.mtpadphotos.data.local.db.MediaFileDao
import io.github.watervxv.mtpadphotos.data.local.db.MediaFileEntity
import io.github.watervxv.mtpadphotos.data.local.db.PlayPositionDao
import io.github.watervxv.mtpadphotos.data.local.db.PlayPositionEntity
import io.github.watervxv.mtpadphotos.data.media.MediaUrlBuilder
import io.github.watervxv.mtpadphotos.data.remote.AuthManager
import io.github.watervxv.mtpadphotos.data.remote.MtPhotoApi
import io.github.watervxv.mtpadphotos.data.remote.dto.AlbumDto
import io.github.watervxv.mtpadphotos.data.remote.dto.FileInfoDto
import io.github.watervxv.mtpadphotos.data.remote.dto.FilesV2FileDto
import io.github.watervxv.mtpadphotos.domain.model.Album
import io.github.watervxv.mtpadphotos.domain.model.ExtraInfo
import io.github.watervxv.mtpadphotos.domain.model.GpsInfo
import io.github.watervxv.mtpadphotos.domain.model.MediaItem
import io.github.watervxv.mtpadphotos.domain.model.MediaType
import io.github.watervxv.mtpadphotos.domain.model.SmartAlbumType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class DefaultAlbumRepository(
    private val api: MtPhotoApi,
    private val albumDao: AlbumDao,
    private val mediaFileDao: MediaFileDao,
    private val playPositionDao: PlayPositionDao,
    private val authManager: AuthManager
) : AlbumRepository {

    private val urlBuilder: MediaUrlBuilder
        get() = MediaUrlBuilder(authManager.getServerUrl() ?: "")

    /**
     * 防止多个重量级同步并发执行。
     */
    private val heavySyncMutex = Mutex()

    /**
     * 限制 filesInfo 并行请求数，避免 4GB 内存平板被压垮。
     */
    private val filesInfoSemaphore = Semaphore(permits = 3)

    /**
     * 记录每个相册最近一次后台刷新成功的时间，用于刷新节流。
     */
    private val lastRefreshAt = java.util.concurrent.ConcurrentHashMap<String, Long>()

    companion object {
        private const val SMART_ALL_ID = "smart_all"
        private const val SMART_PHOTOS_ID = "smart_photos"
        private const val SMART_VIDEOS_ID = "smart_videos"
        private const val SMART_MEMORY_ID = "smart_memory"
        private const val BATCH_SIZE = 200

        /**
         * 空结果保护阈值：服务端返回数量不足本地 50% 时，不执行删除标记。
         */
        private const val SAFE_DELETE_RATIO = 0.5

        /**
         * 单个相册两次后台刷新的最小间隔，避免每次打开相册都全量重拉。
         */
        private const val MIN_REFRESH_INTERVAL_MS = 5 * 60 * 1000L
    }

    override fun observeAlbums(): Flow<List<Album>> = combine(
        albumDao.observeAll(),
        mediaFileDao.observeCount(SMART_ALL_ID),
        mediaFileDao.observeCount(SMART_PHOTOS_ID),
        mediaFileDao.observeCount(SMART_VIDEOS_ID),
        mediaFileDao.observeCount(SMART_MEMORY_ID)
    ) { userAlbums, allCount, photosCount, videosCount, memoryCount ->
        val smart = listOf(
            Album(SMART_ALL_ID, "全部项目", null, allCount, true, SmartAlbumType.ALL),
            Album(SMART_PHOTOS_ID, "照片", null, photosCount, true, SmartAlbumType.PHOTOS),
            Album(SMART_VIDEOS_ID, "视频", null, videosCount, true, SmartAlbumType.VIDEOS),
            Album(SMART_MEMORY_ID, "往年今日", null, memoryCount, true, SmartAlbumType.MEMORY)
        )
        smart + userAlbums.map { it.toDomain() }
    }

    override suspend fun getAlbum(albumId: String): Album? = when (albumId) {
        SMART_ALL_ID -> Album(SMART_ALL_ID, "全部项目", null, 0, true, SmartAlbumType.ALL)
        SMART_PHOTOS_ID -> Album(SMART_PHOTOS_ID, "照片", null, 0, true, SmartAlbumType.PHOTOS)
        SMART_VIDEOS_ID -> Album(SMART_VIDEOS_ID, "视频", null, 0, true, SmartAlbumType.VIDEOS)
        SMART_MEMORY_ID -> Album(SMART_MEMORY_ID, "往年今日", null, 0, true, SmartAlbumType.MEMORY)
        else -> albumDao.getAll().find { it.id.toString() == albumId }?.toDomain()
    }

    override suspend fun getMediaItems(albumId: String): List<MediaItem> {
        val local = getLocalMediaItems(albumId)
        return refreshMediaItems(albumId).getOrDefault(local).ifEmpty { local }
    }

    override suspend fun getLocalMediaItems(albumId: String): List<MediaItem> {
        // 离线兑底：换码失败时回退本地存储的 auth_code，保证已缓存图片可看
        val authCode = authManager.usableAuthCode()
        return loadLocalItems(albumId, authCode)
    }

    override suspend fun getPlayPosition(albumId: String): Long? =
        playPositionDao.get(albumId)?.fileId

    override suspend fun savePlayPosition(albumId: String, fileId: Long) {
        playPositionDao.save(PlayPositionEntity(albumId = albumId, fileId = fileId))
    }

    override suspend fun clearOtherPlayPositions(albumId: String) {
        playPositionDao.clearExcept(albumId)
    }

    override suspend fun refreshMediaItems(albumId: String): Result<List<MediaItem>> = runCatching {
        val authCode = authManager.ensureAuthCode().getOrThrow()
        val now = System.currentTimeMillis()
        val last = lastRefreshAt[albumId] ?: 0L
        if (now - last >= MIN_REFRESH_INTERVAL_MS) {
            heavySyncMutex.withLock {
                refreshAlbumFiles(albumId)
            }
            lastRefreshAt[albumId] = now
        }
        loadLocalItems(albumId, authCode)
    }

    private suspend fun loadLocalItems(albumId: String, authCode: String?): List<MediaItem> {
        val items = mediaFileDao.getByAlbum(albumId).map { it.toDomain(authCode) }
        return if (albumId == SMART_PHOTOS_ID) items.filter { !it.isVideo } else items
    }

    suspend fun syncAlbums(onProgress: (Int) -> Unit = {}): Result<Unit> = runCatching {
        authManager.ensureAuthCode().getOrThrow()
        val response = api.getAlbums()
        if (!response.isSuccessful) throw ApiException(response.code(), "获取相册列表失败")
        val dtos = response.body() ?: emptyList()
        val entities = dtos.mapIndexed { index, dto -> dto.toEntity(index) }
        albumDao.deleteAllUserAlbums()
        albumDao.insertAll(entities)
        if (entities.isNotEmpty()) onProgress(entities.size)
    }

    private suspend fun refreshAlbumFiles(albumId: String) {
        when (albumId) {
            SMART_ALL_ID, SMART_PHOTOS_ID -> refreshAllItems()
            SMART_VIDEOS_ID -> refreshVideosInternal()
            SMART_MEMORY_ID -> refreshMemory()
            else -> refreshUserAlbum(albumId.toLong())
        }
    }

    // 注意：本方法不加 heavySyncMutex，锁由调用方（refreshMediaItems 外层）持有。
    // kotlinx 的 Mutex 不可重入，同协程重复加锁会永久挂起。
    private suspend fun refreshUserAlbum(albumId: Long, onProgress: (Int) -> Unit = {}) {
        val response = api.getAlbumFilesV2(albumId)
        if (!response.isSuccessful) throw ApiException(response.code(), "获取相册文件失败")
        val groups = response.body()?.result ?: emptyList()
        val idMd5List = groups.flatMap { it.files ?: emptyList() }.filter { it.md5 != null }
        updateFilesForAlbum(albumId.toString(), idMd5List, onProgress = onProgress)
    }

    /** 供外部（SyncManager）调用：内部自行加锁。 */
    suspend fun refreshVideos(onProgress: (Int) -> Unit = {}): Int =
        heavySyncMutex.withLock { refreshVideosInternal(onProgress) }

    /** 供 refreshAlbumFiles 在已持有 heavySyncMutex 时调用，本方法自身不加锁。 */
    private suspend fun refreshVideosInternal(onProgress: (Int) -> Unit = {}): Int {
        val galleryResp = api.getMyGalleryList()
        if (!galleryResp.isSuccessful) throw ApiException(galleryResp.code(), "获取图库列表失败")
        val galleries = galleryResp.body() ?: emptyList()
        if (galleries.isEmpty()) return 0
        val galleryIds = galleries.joinToString("_") { it.id.toString() }
        val response = api.getFilesInCategories(galleryIds = galleryIds, type = "videos")
        if (!response.isSuccessful) throw ApiException(response.code(), "获取视频分类失败")
        val groups = response.body()?.result ?: emptyList()
        val idMd5List = groups.flatMap { it.files ?: emptyList() }.filter { it.md5 != null }
        updateFilesForAlbum(SMART_VIDEOS_ID, idMd5List, forceVideo = true, onProgress = onProgress)
        return idMd5List.size
    }

    /**
     * 往年今日（M3-fix2）：同步 memory 数据到 smart_memory 虚拟相册。
     * 由 SyncManager.syncAll() 与进入轮播页的 getMediaItems() 触发；
     * 响应结构未完全确认，兼容 {list:[...]} / {result:[...]} / 裸数组三种形态，失败静默降级。
     */
    suspend fun syncMemory(onProgress: (Int) -> Unit = {}): Result<Unit> = runCatching {
        authManager.ensureAuthCode().getOrThrow()
        heavySyncMutex.withLock { refreshMemory(onProgress) }
    }

    // 注意：本方法不加 heavySyncMutex，锁由调用方（syncMemory 或 refreshMediaItems 外层）持有。
    private suspend fun refreshMemory(onProgress: (Int) -> Unit = {}) {
        val response = api.getMemory()
        if (!response.isSuccessful) throw ApiException(response.code(), "获取往年今日失败")
        val idMd5List = parseMemoryGroups(response.body())
            .flatMap { group -> group.ids.map { FilesV2FileDto(it, null) } }
        updateFilesForAlbum(SMART_MEMORY_ID, idMd5List, onProgress = onProgress)
    }

    private data class MemoryGroup(val year: Long?, val cover: String?, val ids: List<Long>)

    private fun parseMemoryGroups(element: JsonElement?): List<MemoryGroup> {
        val array = when (element) {
            is JsonArray -> element
            is JsonObject -> (element["list"] as? JsonArray) ?: (element["result"] as? JsonArray)
            else -> null
        } ?: return emptyList()
        return array.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val ids = (obj["ids"] as? JsonArray)
                ?.mapNotNull { (it as? JsonPrimitive)?.content?.toLongOrNull() }
                ?: return@mapNotNull null
            if (ids.isEmpty()) return@mapNotNull null
            val year = (obj["year"] as? JsonPrimitive)?.content?.toLongOrNull()
            val cover = (obj["cover"] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content
            MemoryGroup(year, cover, ids)
        }
    }

    suspend fun refreshAllItems(onProgress: (Int) -> Unit = {}): Int {
        val galleryResp = api.getMyGalleryList()
        if (!galleryResp.isSuccessful) throw ApiException(galleryResp.code(), "获取图库列表失败")
        val galleries = galleryResp.body() ?: emptyList()
        if (galleries.isEmpty()) return 0
        val galleryIds = galleries.joinToString("_") { it.id.toString() }

        // 用 filesInTimelineV2 拉取全部文件（而不是 recentFiles 仅最近添加）
        val response = api.getFilesInTimelineV2(galleryIds = galleryIds)
        if (!response.isSuccessful) throw ApiException(response.code(), "获取全部文件失败")
        val groups = response.body()?.result ?: emptyList()
        val idMd5List = groups.flatMap { it.files ?: emptyList() }.filter { it.md5 != null }
        updateFilesForAlbum(SMART_ALL_ID, idMd5List, onProgress = onProgress)
        refreshPhotos()
        return idMd5List.size
    }

    private suspend fun refreshPhotos() {
        val photos = mediaFileDao.getByAlbum(SMART_ALL_ID).filter { !it.isVideo }
        mediaFileDao.deleteByAlbum(SMART_PHOTOS_ID)
        mediaFileDao.insertAll(photos.map { it.copy(albumId = SMART_PHOTOS_ID) })
    }

    private suspend fun updateFilesForAlbum(
        albumId: String,
        idMd5List: List<FilesV2FileDto>,
        forceVideo: Boolean = false,
        onProgress: (Int) -> Unit = {}
    ) {
        val existing = mediaFileDao.getIdMd5Snapshot(albumId).toSet()
        val incomingIds = idMd5List.map { it.id }.toSet()

        // 空结果保护：服务端返回数量明显少于本地时，不标记删除。
        val shouldProtect = existing.size > 100 && incomingIds.size < existing.size * SAFE_DELETE_RATIO
        if (shouldProtect) {
            android.util.Log.w(
                "DefaultAlbumRepository",
                "Album $albumId 服务端返回 ${incomingIds.size} 条，本地已有 ${existing.size} 条，跳过删除标记"
            )
        } else {
            // 标记删除：本地有但远端没有
            val toDelete = existing.filter { it.id !in incomingIds }.map { it.id }
            if (toDelete.isNotEmpty()) mediaFileDao.markDeletedInAlbum(albumId, toDelete)
        }

        // 新增或更新
        val idsNeedDetail = idMd5List.filter { tuple ->
            existing.none { it.id == tuple.id && it.md5 == tuple.md5 }
        }.map { it.id }

        if (idsNeedDetail.isEmpty()) return

        val details = fetchFileDetails(idsNeedDetail)
        val entities = idMd5List.mapNotNull { tuple ->
            details[tuple.id]?.let { info ->
                // filesInfo 不返回 fileType，用时间线/分类接口里的 fileType 作为提示
                info.toEntity(
                    albumId = albumId,
                    md5Value = tuple.md5 ?: info.md5 ?: "",
                    fileTypeHint = tuple.fileType
                )
            }
        }
        mediaFileDao.insertAll(entities)
        if (entities.isNotEmpty()) onProgress(entities.size)
    }

    private suspend fun fetchFileDetails(ids: List<Long>): Map<Long, FileInfoDto> = coroutineScope {
        ids.chunked(BATCH_SIZE)
            .map { chunk ->
                async {
                    filesInfoSemaphore.withPermit {
                        val resp = api.getFilesInfo(io.github.watervxv.mtpadphotos.data.remote.dto.IdsRequest(ids = chunk))
                        if (resp.isSuccessful) {
                            resp.body()?.list ?: emptyList()
                        } else {
                            emptyList()
                        }
                    }
                }
            }
            .awaitAll()
            .flatten()
            .associateBy { it.id }
    }

    private fun AlbumEntity.toDomain(): Album = Album(
        id = id.toString(),
        name = name,
        coverMd5 = coverMd5,
        itemCount = count,
        isSmart = isSmart,
        smartType = smartType?.let { SmartAlbumType.valueOf(it) }
    )

    private fun AlbumDto.toEntity(sortOrder: Int): AlbumEntity = AlbumEntity(
        id = id,
        name = name ?: "",
        description = desc,
        coverMd5 = cover,
        count = count.toInt(),
        mtime = parseMtime(mtime),
        sortOrder = sortOrder
    )

    private fun parseMtime(mtime: String?): Long? = try {
        mtime?.let { java.time.Instant.parse(it).toEpochMilli() }
    } catch (_: Exception) {
        null
    }

    private fun FileInfoDto.toEntity(
        albumId: String,
        md5Value: String,
        fileTypeHint: String? = null
    ): MediaFileEntity {
        val effectiveType = fileType ?: fileTypeHint
        val video = isVideo(effectiveType)
        return MediaFileEntity(
            id = id,
            albumId = albumId,
            md5 = md5Value,
            fileName = fileName ?: "",
            tokenAt = parseTokenAt(tokenAt),
            fileType = effectiveType,
            width = width?.toInt(),
            height = height?.toInt(),
            duration = duration?.toInt(),
            isVideo = video,
            province = null,
            city = null,
            district = null,
            make = null,
            model = null
        )
    }

    private fun parseTokenAt(tokenAt: String?): Long = try {
        tokenAt?.toLongOrNull()
            ?: tokenAt?.let { java.time.Instant.parse(it).toEpochMilli() }
            ?: 0L
    } catch (_: Exception) {
        0L
    }

    private fun MediaFileEntity.toDomain(authCode: String?): MediaItem {
        val thumb = authCode?.let { urlBuilder.thumbnailUrl(MediaUrlBuilder.ThumbnailType.S260, md5, it) }
        // 视频走实时转码流，照片走原图
        val original = authCode?.let {
            if (isVideo) urlBuilder.videoTranscodeUrl(id, md5, it)
            else urlBuilder.originalUrl(id, md5, it)
        }
        // 照片轮播展示用 proxy 中等画质（NAS 实测 79ms/218KB，原图 527ms/2MB）
        val preview = authCode?.takeIf { !isVideo }?.let {
            urlBuilder.thumbnailUrl(MediaUrlBuilder.ThumbnailType.PROXY, md5, it)
        }
        return MediaItem(
            id = id.toString(),
            md5 = md5,
            fileName = fileName,
            tokenAt = tokenAt,
            type = if (isVideo) MediaType.VIDEO else MediaType.PHOTO,
            width = width,
            height = height,
            duration = duration,
            gpsInfo = if (province != null || city != null || district != null) {
                GpsInfo(province, city, district)
            } else null,
            extra = if (make != null || model != null) ExtraInfo(make, model) else null,
            thumbnailUrl = thumb,
            originalUrl = original,
            previewUrl = preview
        )
    }

    override suspend fun refreshFileDetails(fileId: String): Result<MediaItem> = runCatching {
        val id = fileId.toLong()
        val authCode = authManager.ensureAuthCode().getOrThrow()
        val response = api.getExifInfo(id)
        if (!response.isSuccessful) throw ApiException(response.code(), "获取 EXIF 失败")
        val exif = response.body()

        val entity = mediaFileDao.getById(id)
            ?: throw NoSuchElementException("文件不存在")

        val updated = entity.copy(
            make = exif?.Make,
            model = exif?.Model
        )
        mediaFileDao.insertAll(listOf(updated))
        updated.toDomain(authCode)
    }

    override suspend fun getCoverPreview(albumId: String): String? {
        val first = mediaFileDao.getByAlbum(albumId).firstOrNull() ?: return null
        val authCode = authManager.ensureAuthCode().getOrNull() ?: return null
        return urlBuilder.thumbnailUrl(MediaUrlBuilder.ThumbnailType.S260, first.md5, authCode)
    }

    private fun isVideo(fileType: String?): Boolean {
        if (fileType == null) return false
        return fileType.equals("MP4", true) || fileType.equals("MOV", true) || fileType.equals("VIDEO", true)
    }
}

class ApiException(val code: Int, message: String) : Exception(message)
