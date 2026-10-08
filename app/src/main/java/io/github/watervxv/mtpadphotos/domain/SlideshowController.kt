package io.github.watervxv.mtpadphotos.domain

import io.github.watervxv.mtpadphotos.domain.model.MediaItem
import io.github.watervxv.mtpadphotos.domain.model.PlayOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SlideshowController(
    items: List<MediaItem>,
    initialOrder: PlayOrder = PlayOrder.FORWARD,
    private val photoDurationMs: Long = 5000L,
    private val scope: CoroutineScope
) {
    private val _order = MutableStateFlow(initialOrder)
    val order: StateFlow<PlayOrder> = _order.asStateFlow()

    private val _playlist = MutableStateFlow(PlaylistBuilder.build(items, initialOrder))
    val playlist: StateFlow<List<MediaItem>> = _playlist.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _isPlaying = MutableStateFlow(true)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    val currentItem: StateFlow<MediaItem?> = MutableStateFlow(_playlist.value.getOrNull(_currentIndex.value))

    private var timerJob: Job? = null

    init {
        restartTimer()
    }

    fun setOrder(newOrder: PlayOrder) {
        if (_order.value == newOrder) return
        val current = currentItem.value
        _order.value = newOrder
        _playlist.value = PlaylistBuilder.build(_playlist.value, newOrder)
        val newIndex = current?.let { _playlist.value.indexOf(it) }?.takeIf { it >= 0 } ?: 0
        _currentIndex.value = newIndex
        updateCurrentItem()
        restartTimer()
    }

    fun setDuration(durationMs: Long) {
        if (photoDurationMs == durationMs) return
        // reflection-free: controller needs recreate for duration change in this M1 version
    }

    fun togglePlay() {
        _isPlaying.value = !_isPlaying.value
        if (_isPlaying.value) restartTimer() else timerJob?.cancel()
    }

    fun play() {
        _isPlaying.value = true
        restartTimer()
    }

    fun pause() {
        _isPlaying.value = false
        timerJob?.cancel()
    }

    fun next() {
        if (_playlist.value.isEmpty()) return
        _currentIndex.value = (_currentIndex.value + 1) % _playlist.value.size
        updateCurrentItem()
        restartTimer()
    }

    fun previous() {
        if (_playlist.value.isEmpty()) return
        _currentIndex.value = (_currentIndex.value - 1 + _playlist.value.size) % _playlist.value.size
        updateCurrentItem()
        restartTimer()
    }

    fun jumpTo(index: Int) {
        if (index !in _playlist.value.indices) return
        _currentIndex.value = index
        updateCurrentItem()
        restartTimer()
    }

    /**
     * M1-fix2 播放列表多选删除：仅修改内存中的 playlist。
     * 不删除 NAS 文件、不写 DeleteLogCursor、不持久化。
     */
    fun deleteAt(indices: Collection<Int>) {
        val toRemove = indices.filter { it in _playlist.value.indices }.toSet()
        if (toRemove.isEmpty()) return
        val currentId = currentItem.value?.id
        val newList = _playlist.value.filterIndexed { index, _ -> index !in toRemove }
        _playlist.value = newList
        _currentIndex.value = if (newList.isEmpty()) {
            0
        } else {
            currentId?.let { id -> newList.indexOfFirst { it.id == id } }
                ?.takeIf { it >= 0 }
                ?: minOf(_currentIndex.value, newList.size - 1)
        }
        updateCurrentItem()
        restartTimer()
    }

    /** M1-fix2 撤销删除：按原位置（升序）重新插入。 */
    fun restoreItems(entries: List<Pair<Int, MediaItem>>) {
        if (entries.isEmpty()) return
        val currentId = currentItem.value?.id
        val mutable = _playlist.value.toMutableList()
        entries.sortedBy { it.first }.forEach { (index, item) ->
            mutable.add(index.coerceIn(0, mutable.size), item)
        }
        _playlist.value = mutable
        _currentIndex.value = currentId?.let { id -> mutable.indexOfFirst { it.id == id } }
            ?.takeIf { it >= 0 } ?: 0
        updateCurrentItem()
        restartTimer()
    }

    /** M1-fix2 播放列表拖动排序：仅修改内存中的 playlist。 */
    fun moveItem(from: Int, to: Int) {
        val size = _playlist.value.size
        if (from !in 0 until size || to !in 0 until size || from == to) return
        val currentId = currentItem.value?.id
        val mutable = _playlist.value.toMutableList()
        val moved = mutable.removeAt(from)
        mutable.add(to, moved)
        _playlist.value = mutable
        _currentIndex.value = currentId?.let { id -> mutable.indexOfFirst { it.id == id } }
            ?.takeIf { it >= 0 } ?: to
        updateCurrentItem()
        restartTimer()
    }

    private fun updateCurrentItem() {
        (currentItem as MutableStateFlow).value = _playlist.value.getOrNull(_currentIndex.value)
    }

    private fun restartTimer() {
        timerJob?.cancel()
        if (!_isPlaying.value || _playlist.value.isEmpty()) return
        timerJob = scope.launch {
            while (isActive) {
                delay(photoDurationMs)
                if (!isActive) break
                _currentIndex.value = (_currentIndex.value + 1) % _playlist.value.size
                updateCurrentItem()
            }
        }
    }
}
