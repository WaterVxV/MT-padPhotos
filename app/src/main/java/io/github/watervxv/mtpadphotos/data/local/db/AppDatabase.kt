package io.github.watervxv.mtpadphotos.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        AlbumEntity::class,
        MediaFileEntity::class,
        PlayPositionEntity::class,
        SyncStateEntity::class,
        DeleteLogCursorEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun albumDao(): AlbumDao
    abstract fun mediaFileDao(): MediaFileDao
    abstract fun playPositionDao(): PlayPositionDao
    abstract fun syncStateDao(): SyncStateDao
    abstract fun deleteLogCursorDao(): DeleteLogCursorDao
}
