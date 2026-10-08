package io.github.watervxv.mtpadphotos.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthCodeRequest(
    @SerialName("api_key") val apiKey: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null
)

@Serializable
data class AuthCodeResponse(
    @SerialName("auth_code") val authCode: String? = null,
    // MT Photo 对无效 Key 也返回 201，错误原因在 msg 字段（如“api_key无效或已过期”）
    @SerialName("msg") val msg: String? = null
)

@Serializable
data class ApiInfoResponse(
    val version: String? = null,
    val activated: Boolean? = null,
    val dbStatus: String? = null,
    val build: String? = null,
    val arch: String? = null,
    val platform: String? = null,
    @SerialName("tzOffset") val tzOffset: Int? = null,
    @SerialName("faceRegVer") val faceRegVer: String? = null
)

@Serializable
data class AlbumDto(
    val id: Long,
    val name: String? = null,
    val desc: String? = null,
    val cover: String? = null,
    val count: Long = 0,
    @SerialName("mtime") val mtime: String? = null,
    @SerialName("create_time") val createTime: String? = null,
    @SerialName("startTime") val startTime: String? = null,
    @SerialName("endTime") val endTime: String? = null
)

@Serializable
data class AlbumDetailDto(
    val id: Long,
    val name: String? = null,
    val desc: String? = null,
    val cover: String? = null,
    val count: Long = 0,
    val weights: Long? = null,
    val theme: String? = null,
    val files: List<Long>? = null,
    @SerialName("mtime") val mtime: String? = null
)

@Serializable
data class FilesV2Response(
    val result: List<FilesV2GroupDto>? = null,
    @SerialName("duplicateFiles") val duplicateFiles: Map<String, List<Long>>? = null,
    @SerialName("totalCount") val totalCount: Long = 0
)

@Serializable
data class FilesV2GroupDto(
    // 服务端 filesV2/filesInCategoriesV2/filesInTimelineV2 实际返回字段名为 "day"，分组内列表字段名为 "list"
    @SerialName("day") val date: String? = null,
    @SerialName("list") val files: List<FilesV2FileDto>? = null
)

@Serializable
data class FilesV2FileDto(
    val id: Long,
    @SerialName("MD5") val md5: String? = null,
    @SerialName("fileName") val fileName: String? = null,
    @SerialName("tokenAt") val tokenAt: String? = null,
    @SerialName("fileType") val fileType: String? = null,
    val width: Long? = null,
    val height: Long? = null,
    val duration: Double? = null
)

typealias RecentFilesResponse = List<RecentFileDto>

@Serializable
data class RecentFileDto(
    val id: Long,
    @SerialName("MD5") val md5: String? = null,
    @SerialName("fileName") val fileName: String? = null,
    @SerialName("filePath") val filePath: String? = null,
    @SerialName("tokenAt") val tokenAt: String? = null
)

@Serializable
data class DeleteLogResponse(
    val count: Long = 0,
    val list: List<DeleteLogItemDto>? = null
)

@Serializable
data class DeleteLogItemDto(
    val id: Long,
    @SerialName("filePath") val filePath: String? = null,
    @SerialName("fileName") val fileName: String? = null,
    @SerialName("fileSize") val fileSize: Long? = null,
    @SerialName("deleteTime") val deleteTime: String? = null,
    @SerialName("userId") val userId: Long? = null,
    val type: Int? = null
)

@Serializable
data class IdsRequest(
    val ids: List<Long>,
    @SerialName("albumId") val albumId: Long? = null,
    @SerialName("albumType") val albumType: String? = null,
    val type: String? = null
)

@Serializable
data class AreaFilesMd5Response(
    val list: List<AreaFileDto>? = null
)

@Serializable
data class AreaFileDto(
    val id: Long,
    @SerialName("MD5") val md5: String? = null,
    @SerialName("fileName") val fileName: String? = null,
    @SerialName("tokenAt") val tokenAt: Long? = null,
    val duration: Long? = null
)

// 服务端 /gateway/filesInfo 实际返回 { list: [...] }，不是裸数组
@Serializable
data class FilesInfoResponse(
    val list: List<FileInfoDto>? = null
)

@Serializable
data class FileInfoDto(
    val id: Long,
    @SerialName("MD5") val md5: String? = null,
    @SerialName("fileName") val fileName: String? = null,
    // 服务端返回的是字符串型数字，如 "8312469"
    @SerialName("fileSize") val fileSize: String? = null,
    @SerialName("fileType") val fileType: String? = null,
    // 服务端返回秒级浮点数，如 13.2
    val duration: Double? = null,
    @SerialName("tokenAt") val tokenAt: String? = null,
    @SerialName("livePhotosVideoId") val livePhotosVideoId: Long? = null,
    @SerialName("filePath") val filePath: String? = null,
    val width: Long? = null,
    val height: Long? = null,
    @SerialName("m_rotate") val mRotate: Long? = null
)

@Serializable
data class FileInIdsResponse(
    val result: List<FileInIdsGroupDto>? = null
)

@Serializable
data class FileInIdsGroupDto(
    val date: String? = null,
    val files: List<FileInIdsFileDto>? = null
)

@Serializable
data class FileInIdsFileDto(
    val id: Long,
    @SerialName("MD5") val md5: String? = null,
    @SerialName("fileName") val fileName: String? = null,
    @SerialName("tokenAt") val tokenAt: Long? = null
)

@Serializable
data class ExifInfoResponse(
    val Make: String? = null,
    val Model: String? = null,
    @SerialName("DateTimeOriginal") val dateTimeOriginal: String? = null,
    @SerialName("ExposureTime") val exposureTime: String? = null,
    @SerialName("FNumber") val fNumber: String? = null,
    @SerialName("ISOSpeedRatings") val isoSpeedRatings: String? = null,
    @SerialName("FocalLength") val focalLength: String? = null,
    @SerialName("GPSLatitude") val gpsLatitude: String? = null,
    @SerialName("GPSLongitude") val gpsLongitude: String? = null
)

@Serializable
data class SearchV2Request(
    val keyword: String? = null,
    @SerialName("pageSize") val pageSize: Int? = 50,
    @SerialName("pageNo") val pageNo: Int? = 1
)

@Serializable
data class SearchV2Response(
    val list: List<SearchResultDto>? = null,
    val total: Long? = null
)

@Serializable
data class SearchResultDto(
    val id: Long,
    @SerialName("MD5") val md5: String? = null,
    @SerialName("fileName") val fileName: String? = null,
    @SerialName("tokenAt") val tokenAt: Long? = null
)

@Serializable
data class FileStreamLinkResponse(
    val link: String? = null,
    val ttl: Long? = null
)

typealias GalleryListResponse = List<GalleryDto>

@Serializable
data class GalleryDto(
    val id: Long,
    val name: String? = null,
    @SerialName("forUpload") val forUpload: Boolean? = null,
    @SerialName("multi") val multi: Boolean? = null
)

@Serializable
data class TimelineResponse(
    val list: List<TimelineMonthDto>? = null
)

@Serializable
data class TimelineMonthDto(
    val month: String? = null,
    val count: Long? = null,
    @SerialName("coverId") val coverId: Long? = null
)

@Serializable
data class MemoryRequest(
    val month: Int? = null,
    val day: Int? = null,
    @SerialName("galleryIds") val galleryIds: String? = null
)
