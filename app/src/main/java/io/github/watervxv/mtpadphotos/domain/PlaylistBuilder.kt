package io.github.watervxv.mtpadphotos.domain

import io.github.watervxv.mtpadphotos.domain.model.MediaItem
import io.github.watervxv.mtpadphotos.domain.model.PlayOrder
import kotlin.random.Random

object PlaylistBuilder {

    fun build(
        items: List<MediaItem>,
        order: PlayOrder,
        randomSeed: Int = 0
    ): List<MediaItem> = when (order) {
        PlayOrder.FORWARD -> items.sortedBy { it.tokenAt }
        PlayOrder.REVERSE -> items.sortedByDescending { it.tokenAt }
        PlayOrder.RANDOM -> items.shuffled(Random(randomSeed))
    }
}
