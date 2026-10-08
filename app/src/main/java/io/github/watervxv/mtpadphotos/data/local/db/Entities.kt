package io.github.watervxv.mtpadphotos.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val description: String?,
    @ColumnInfo(name = "cover_md5") val coverMd5: String?,
    val count: Int,
    @ColumnInfo(name = "is_smart") val isSmart: Boolean = false,
    @ColumnInfo(name = "smart_type") val smartType: String? = null,
    @ColumnInfo(name = "mtime") val mtime: Long? = null,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0
)

@Entity(
    tableName = "media_files",
    primaryKeys = ["id", "album_id"],
    indices = [Index(value = ["album_id", "token_at"]), Index(value = ["md5"])]
)
data class MediaFileEntity(
    val id: Long,
    @ColumnInfo(name = "album_id") val albumId: String,
    val md5: String,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "token_at") val tokenAt: Long,
    @ColumnInfo(name = "file_type") val fileType: String?,
    val width: Int?,
    val height: Int?,
    val duration: Int?,
    @ColumnInfo(name = "is_video") val isVideo: Boolean = false,
    val province: String?,
    val city: String?,
    val district: String?,
    @ColumnInfo(name = "make") val make: String?,
    @ColumnInfo(name = "model") val model: String?,
    @ColumnInfo(name = "synced_at") val syncedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false
)

@Entity(tableName = "play_positions")
data class PlayPositionEntity(
    @PrimaryKey @ColumnInfo(name = "album_id") val albumId: String,
    @ColumnInfo(name = "file_id") val fileId: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "last_album_sync_at") val lastAlbumSyncAt: Long = 0,
    @ColumnInfo(name = "last_delete_log_time") val lastDeleteLogTime: Long = 0,
    @ColumnInfo(name = "is_admin_key") val isAdminKey: Boolean? = null
)

@Entity(tableName = "delete_log_cursor")
data class DeleteLogCursorEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "last_delete_time") val lastDeleteTime: Long = 0,
    @ColumnInfo(name = "last_page_no") val lastPageNo: Int = 1,
    @ColumnInfo(name = "consumed_all") val consumedAll: Boolean = false
)
