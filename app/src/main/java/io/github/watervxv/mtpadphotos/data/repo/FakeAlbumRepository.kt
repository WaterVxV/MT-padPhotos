package io.github.watervxv.mtpadphotos.data.repo

import io.github.watervxv.mtpadphotos.domain.model.Album
import io.github.watervxv.mtpadphotos.domain.model.GpsInfo
import io.github.watervxv.mtpadphotos.domain.model.MediaItem
import io.github.watervxv.mtpadphotos.domain.model.MediaType
import io.github.watervxv.mtpadphotos.domain.model.SmartAlbumType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

interface AlbumRepository {
    fun observeAlbums(): Flow<List<Album>>
    suspend fun getAlbum(albumId: String): Album?
    suspend fun getMediaItems(albumId: String): List<MediaItem>
    /**
     * 只读取本地缓存，不触发网络请求。
     */
    suspend fun getLocalMediaItems(albumId: String): List<MediaItem>
    /**
     * 强制从服务端刷新并返回最新数据。
     */
    suspend fun refreshMediaItems(albumId: String): Result<List<MediaItem>>
    suspend fun refreshFileDetails(fileId: String): Result<MediaItem>
    suspend fun getCoverPreview(albumId: String): String?
    /** 断点续播：读取相册上次播放到的文件 ID。 */
    suspend fun getPlayPosition(albumId: String): Long?
    /** 断点续播：保存相册当前播放的文件 ID。 */
    suspend fun savePlayPosition(albumId: String, fileId: Long)
}

class FakeAlbumRepository : AlbumRepository {

    private val albums = MutableStateFlow(generateAlbums())
    private val itemsCache = mutableMapOf<String, List<MediaItem>>()

    override fun observeAlbums(): Flow<List<Album>> = albums

    override suspend fun getAlbum(albumId: String): Album? =
        albums.value.find { it.id == albumId }

    override suspend fun getPlayPosition(albumId: String): Long? = null

    override suspend fun savePlayPosition(albumId: String, fileId: Long) {}

    override suspend fun getMediaItems(albumId: String): List<MediaItem> {
        delay(200)
        return itemsCache.getOrPut(albumId) { generateItemsForAlbum(albumId) }
    }

    override suspend fun getLocalMediaItems(albumId: String): List<MediaItem> {
        return getMediaItems(albumId)
    }

    override suspend fun refreshMediaItems(albumId: String): Result<List<MediaItem>> {
        return runCatching { getMediaItems(albumId) }
    }

    override suspend fun refreshFileDetails(fileId: String): Result<MediaItem> {
        val item = itemsCache.values.flatten().find { it.id == fileId }
        return item?.let { Result.success(it) } ?: Result.failure(NoSuchElementException())
    }

    override suspend fun getCoverPreview(albumId: String): String? = null

    private fun generateAlbums(): List<Album> {
        val smartAlbums = listOf(
            Album(
                id = "smart_all",
                name = "全部项目",
                coverMd5 = md5Of("all"),
                itemCount = 860,
                isSmart = true,
                smartType = SmartAlbumType.ALL
            ),
            Album(
                id = "smart_photos",
                name = "照片",
                coverMd5 = md5Of("photos"),
                itemCount = 720,
                isSmart = true,
                smartType = SmartAlbumType.PHOTOS
            ),
            Album(
                id = "smart_videos",
                name = "视频",
                coverMd5 = md5Of("videos"),
                itemCount = 140,
                isSmart = true,
                smartType = SmartAlbumType.VIDEOS
            )
        )
        val userAlbums = (1..20).map { idx ->
            Album(
                id = "album_$idx",
                name = listOf(
                    "2024 春节", "旅行·云南", "宝宝成长", "家庭聚会", "周末徒步",
                    "工作资料", "美食记录", "宠物日常", "2023 年末", "装修记录",
                    "朋友婚礼", "毕业典礼", "篮球赛", "露营", "海边日出",
                    "DIY 手工", "生日礼物", "公司年会", "公园野餐", "扫街随拍"
                ).getOrElse(idx - 1) { "相册 $idx" },
                coverMd5 = md5Of("cover_$idx"),
                itemCount = 15 + (idx * 7) % 120
            )
        }
        return smartAlbums + userAlbums
    }

    private fun generateItemsForAlbum(albumId: String): List<MediaItem> {
        val count = when (albumId) {
            "smart_all" -> 120
            "smart_photos" -> 100
            "smart_videos" -> 20
            else -> getStaticCountForUserAlbum(albumId)
        }
        val baseTime = 1704067200000L // 2024-01-01 00:00:00 UTC
        val locations = listOf(
            GpsInfo("北京市", "北京市", "朝阳区"),
            GpsInfo("云南省", "大理白族自治州", "大理市"),
            GpsInfo("广东省", "深圳市", "南山区"),
            GpsInfo("浙江省", "杭州市", "西湖区"),
            GpsInfo(null, "上海市", "黄浦区"),
            GpsInfo("四川省", "成都市", "锦江区")
        )
        return (0 until count).map { idx ->
            val isVideo = when (albumId) {
                "smart_videos" -> true
                "smart_photos" -> false
                else -> (idx % 7 == 0)
            }
            MediaItem(
                id = "${albumId}_item_$idx",
                md5 = md5Of("${albumId}_$idx"),
                fileName = if (isVideo) "VID_${idx.toString().padStart(4, '0')}.mp4" else "IMG_${idx.toString().padStart(4, '0')}.jpg",
                tokenAt = baseTime + idx * 86_400_000L,
                type = if (isVideo) MediaType.VIDEO else MediaType.PHOTO,
                width = if (isVideo) 1920 else 4000 + idx % 2000,
                height = if (isVideo) 1080 else 3000 + idx % 1500,
                duration = if (isVideo) 10 + idx % 120 else null,
                gpsInfo = locations[idx % locations.size],
                extra = io.github.watervxv.mtpadphotos.domain.model.ExtraInfo(
                    make = if (idx % 3 == 0) "Apple" else "Sony",
                    model = if (idx % 3 == 0) "iPhone 15 Pro" else "ILCE-7M4"
                )
            )
        }
    }

    private fun getStaticCountForUserAlbum(albumId: String): Int {
        val idx = albumId.removePrefix("album_").toIntOrNull() ?: 1
        return 15 + (idx * 7) % 120
    }

    private fun md5Of(seed: String): String {
        // 生成 32 位十六进制占位 MD5，仅用于假数据
        val bytes = seed.toByteArray(Charsets.UTF_8)
        val sb = StringBuilder()
        var value = 0L
        for (b in bytes) value = value * 31 + b.toInt()
        repeat(32) { i ->
            sb.append("0123456789abcdef"[((value shr (i * 4)) and 0xF).toInt()])
        }
        return sb.toString()
    }
}
