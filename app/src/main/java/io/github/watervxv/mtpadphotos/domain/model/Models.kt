package io.github.watervxv.mtpadphotos.domain.model

import androidx.annotation.Keep

@Keep
data class Album(
    val id: String,
    val name: String,
    val coverMd5: String?,
    val itemCount: Int,
    val isSmart: Boolean = false,
    val smartType: SmartAlbumType? = null
)

enum class SmartAlbumType { ALL, PHOTOS, VIDEOS, MEMORY }

@Keep
data class MediaItem(
    val id: String,
    val md5: String,
    val fileName: String,
    val tokenAt: Long,
    val type: MediaType,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Int? = null,
    val gpsInfo: GpsInfo? = null,
    val extra: ExtraInfo? = null,
    val thumbnailUrl: String? = null,
    val originalUrl: String? = null,
    val previewUrl: String? = null
) {
    val isVideo: Boolean get() = type == MediaType.VIDEO
}

enum class MediaType { PHOTO, VIDEO }

@Keep
data class GpsInfo(
    val province: String?,
    val city: String?,
    val district: String?
) {
    fun display(): String = listOfNotNull(province, city, district)
        .filter { it.isNotBlank() }
        .joinToString(" ")
}

@Keep
data class ExtraInfo(
    val make: String?,
    val model: String?
) {
    fun display(): String = listOfNotNull(make, model)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { "—" }
}

enum class PlayOrder { FORWARD, REVERSE, RANDOM }

enum class TransitionType { FADE, KEN_BURNS, SLIDE, CUT }

enum class ThemeMode { SYSTEM, LIGHT, DARK }
