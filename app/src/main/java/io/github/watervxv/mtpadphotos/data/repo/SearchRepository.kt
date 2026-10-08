package io.github.watervxv.mtpadphotos.data.repo

import io.github.watervxv.mtpadphotos.data.media.MediaUrlBuilder
import io.github.watervxv.mtpadphotos.data.remote.AuthManager
import io.github.watervxv.mtpadphotos.data.remote.MtPhotoApi
import io.github.watervxv.mtpadphotos.data.remote.dto.IdsRequest
import io.github.watervxv.mtpadphotos.data.remote.dto.SearchV2Request
import io.github.watervxv.mtpadphotos.domain.model.ExtraInfo
import io.github.watervxv.mtpadphotos.domain.model.GpsInfo
import io.github.watervxv.mtpadphotos.domain.model.MediaItem
import io.github.watervxv.mtpadphotos.domain.model.MediaType

class SearchRepository(
    private val api: MtPhotoApi,
    private val authManager: AuthManager
) {
    companion object {
        private const val BATCH_SIZE = 200
    }

    suspend fun search(keyword: String): Result<List<MediaItem>> = runCatching {
        val authCode = authManager.ensureAuthCode().getOrThrow()
        val response = api.searchV2(SearchV2Request(keyword = keyword, pageSize = 200))
        if (!response.isSuccessful) throw ApiException(response.code(), "搜索失败")
        val ids = response.body()?.list?.map { it.id } ?: emptyList()
        if (ids.isEmpty()) return Result.success(emptyList())

        val details = mutableMapOf<Long, io.github.watervxv.mtpadphotos.data.remote.dto.FileInfoDto>()
        ids.chunked(BATCH_SIZE).forEach { chunk ->
            val info = api.getFilesInfo(IdsRequest(ids = chunk))
            if (info.isSuccessful) {
                info.body()?.list?.forEach { details[it.id] = it }
            }
        }

        val urlBuilder = MediaUrlBuilder(authManager.getServerUrl() ?: "")
        ids.mapNotNull { id ->
            details[id]?.let { info ->
                val isVideo = info.fileType?.equals("MP4", true) == true || info.fileType?.equals("MOV", true) == true
                MediaItem(
                    id = id.toString(),
                    md5 = info.md5 ?: "",
                    fileName = info.fileName ?: "",
                    tokenAt = info.tokenAt?.toLongOrNull() ?: 0L,
                    type = if (isVideo) MediaType.VIDEO else MediaType.PHOTO,
                    width = info.width?.toInt(),
                    height = info.height?.toInt(),
                    duration = info.duration?.toInt(),
                    gpsInfo = null,
                    extra = null,
                    thumbnailUrl = info.md5?.let {
                        urlBuilder.thumbnailUrl(MediaUrlBuilder.ThumbnailType.S260, it, authCode)
                    },
                    originalUrl = info.md5?.let {
                        urlBuilder.originalUrl(id, it, authCode)
                    }
                )
            }
        }
    }
}
