package io.github.watervxv.mtpadphotos.data.remote

import io.github.watervxv.mtpadphotos.data.remote.dto.AlbumDetailDto
import io.github.watervxv.mtpadphotos.data.remote.dto.AlbumDto
import io.github.watervxv.mtpadphotos.data.remote.dto.ApiInfoResponse
import io.github.watervxv.mtpadphotos.data.remote.dto.AreaFilesMd5Response
import io.github.watervxv.mtpadphotos.data.remote.dto.AuthCodeRequest
import io.github.watervxv.mtpadphotos.data.remote.dto.AuthCodeResponse
import io.github.watervxv.mtpadphotos.data.remote.dto.DeleteLogResponse
import io.github.watervxv.mtpadphotos.data.remote.dto.ExifInfoResponse
import io.github.watervxv.mtpadphotos.data.remote.dto.FileInIdsResponse
import io.github.watervxv.mtpadphotos.data.remote.dto.FileStreamLinkResponse
import io.github.watervxv.mtpadphotos.data.remote.dto.FilesInfoResponse
import io.github.watervxv.mtpadphotos.data.remote.dto.FilesV2GroupDto
import io.github.watervxv.mtpadphotos.data.remote.dto.FilesV2Response
import io.github.watervxv.mtpadphotos.data.remote.dto.GalleryListResponse
import io.github.watervxv.mtpadphotos.data.remote.dto.IdsRequest
import io.github.watervxv.mtpadphotos.data.remote.dto.MemoryRequest
import io.github.watervxv.mtpadphotos.data.remote.dto.RecentFilesResponse
import io.github.watervxv.mtpadphotos.data.remote.dto.SearchV2Request
import io.github.watervxv.mtpadphotos.data.remote.dto.SearchV2Response
import io.github.watervxv.mtpadphotos.data.remote.dto.TimelineResponse
import kotlinx.serialization.json.JsonElement
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MtPhotoApi {

    @GET("api-info")
    suspend fun getApiInfo(): Response<ApiInfoResponse>

    @POST("auth/auth_code")
    suspend fun getAuthCode(@Body request: AuthCodeRequest): Response<AuthCodeResponse>

    @GET("api-album")
    suspend fun getAlbums(): Response<List<AlbumDto>>

    @GET("api-album/{id}")
    suspend fun getAlbumDetail(@Path("id") id: Long): Response<AlbumDetailDto>

    @GET("api-album/filesV2/{id}")
    suspend fun getAlbumFilesV2(
        @Path("id") id: Long,
        @Query("listVer") listVer: String = "v2"
    ): Response<FilesV2Response>

    @GET("gateway/filesInCategoriesV2")
    suspend fun getFilesInCategories(
        @Query("galleryIds") galleryIds: String,
        @Query("type") type: String = "videos"
    ): Response<FilesV2Response>

    @GET("gateway/filesInTimelineV2")
    suspend fun getFilesInTimelineV2(
        @Query("galleryIds") galleryIds: String
    ): Response<FilesV2Response>

    @GET("gateway/myGalleryList")
    suspend fun getMyGalleryList(): Response<GalleryListResponse>

    @GET("gateway/recentFiles")
    suspend fun getRecentFiles(
        @Query("galleryIds") galleryIds: String? = null
    ): Response<RecentFilesResponse>

    @GET("file-delete-log")
    suspend fun getDeleteLog(
        @Query("pageSize") pageSize: Int = 20,
        @Query("pageNo") pageNo: Int = 1
    ): Response<DeleteLogResponse>

    @POST("gateway/areaFilesMD5")
    suspend fun getAreaFilesMd5(@Body request: IdsRequest): Response<AreaFilesMd5Response>

    @POST("gateway/filesInfo")
    suspend fun getFilesInfo(@Body request: IdsRequest): Response<FilesInfoResponse>

    @POST("gateway/fileInIds")
    suspend fun getFileInIds(@Body request: IdsRequest): Response<FileInIdsResponse>

    @GET("gateway/exifInfo/{id}")
    suspend fun getExifInfo(@Path("id") id: Long): Response<ExifInfoResponse>

    @POST("gateway/searchV2")
    suspend fun searchV2(@Body request: SearchV2Request): Response<SearchV2Response>

    @GET("gateway/fileStreamLink/{id}")
    suspend fun getFileStreamLink(@Path("id") id: Long): Response<FileStreamLinkResponse>

    @GET("gateway/timeline")
    suspend fun getTimeline(): Response<TimelineResponse>

    /**
     * 那年今日（M3-fix2）。响应外层结构未完全确认（{list:[...]} / {result:[...]} / 裸数组），
     * 用 JsonElement 接收后在 DefaultAlbumRepository.parseMemoryGroups 做兼容解析。
     */
    @POST("gateway/memory")
    suspend fun getMemory(@Body request: MemoryRequest = MemoryRequest()): Response<JsonElement>
}
