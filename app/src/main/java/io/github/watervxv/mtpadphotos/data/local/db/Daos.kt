package io.github.watervxv.mtpadphotos.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {
    @Query("SELECT * FROM albums ORDER BY sort_order ASC, name ASC")
    fun observeAll(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums ORDER BY sort_order ASC, name ASC")
    suspend fun getAll(): List<AlbumEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(albums: List<AlbumEntity>)

    @Query("DELETE FROM albums WHERE is_smart = 0")
    suspend fun deleteAllUserAlbums()

    @Query("DELETE FROM albums WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT mtime FROM albums WHERE id = :id")
    suspend fun getMtime(id: Long): Long?
}

@Dao
interface MediaFileDao {
    @Query("SELECT * FROM media_files WHERE album_id = :albumId AND is_deleted = 0 ORDER BY token_at ASC")
    fun observeByAlbum(albumId: String): Flow<List<MediaFileEntity>>

    @Query("SELECT * FROM media_files WHERE album_id = :albumId AND is_deleted = 0 ORDER BY token_at ASC")
    suspend fun getByAlbum(albumId: String): List<MediaFileEntity>

    @Query("SELECT id, md5 FROM media_files WHERE album_id = :albumId AND is_deleted = 0")
    suspend fun getIdMd5Snapshot(albumId: String): List<IdMd5Tuple>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(files: List<MediaFileEntity>)

    // 删除日志：文件在服务端真删除，所有相册维度统一标记
    @Query("UPDATE media_files SET is_deleted = 1 WHERE id IN (:ids)")
    suspend fun markDeletedGlobally(ids: List<Long>)

    // 相册内 diff：仅标记该相册维度下消失的文件
    @Query("UPDATE media_files SET is_deleted = 1 WHERE album_id = :albumId AND id IN (:ids)")
    suspend fun markDeletedInAlbum(albumId: String, ids: List<Long>)

    @Query("DELETE FROM media_files WHERE album_id = :albumId")
    suspend fun deleteByAlbum(albumId: String)

    @Query("SELECT * FROM media_files WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): MediaFileEntity?

    @Query("SELECT COUNT(*) FROM media_files WHERE album_id = :albumId AND is_deleted = 0")
    fun observeCount(albumId: String): Flow<Int>
}

data class IdMd5Tuple(
    val id: Long,
    val md5: String?
)

@Dao
interface PlayPositionDao {
    @Query("SELECT * FROM play_positions WHERE album_id = :albumId LIMIT 1")
    suspend fun get(albumId: String): PlayPositionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(position: PlayPositionEntity)

    @Query("DELETE FROM play_positions WHERE album_id != :albumId")
    suspend fun clearExcept(albumId: String)
}

@Dao
interface SyncStateDao {
    @Query("SELECT * FROM sync_state WHERE id = 1 LIMIT 1")
    suspend fun get(): SyncStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(state: SyncStateEntity)
}

@Dao
interface DeleteLogCursorDao {
    @Query("SELECT * FROM delete_log_cursor WHERE id = 1 LIMIT 1")
    suspend fun get(): DeleteLogCursorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(cursor: DeleteLogCursorEntity)
}
