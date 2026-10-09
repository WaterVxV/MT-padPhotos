package io.github.watervxv.mtpadphotos.ui.slideshow

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import io.github.watervxv.mtpadphotos.data.repo.AlbumRepository
import io.github.watervxv.mtpadphotos.data.repo.SettingsRepository
import io.github.watervxv.mtpadphotos.data.sync.SyncManager
import io.github.watervxv.mtpadphotos.domain.SlideshowController
import io.github.watervxv.mtpadphotos.domain.model.MediaItem
import io.github.watervxv.mtpadphotos.domain.model.PlayOrder
import io.github.watervxv.mtpadphotos.domain.model.TransitionType
import io.github.watervxv.mtpadphotos.ui.icons.PlayOrderIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SlideshowScreen(
    albumId: String,
    albumRepository: AlbumRepository,
    settingsRepository: SettingsRepository,
    syncManager: SyncManager,
    onBack: () -> Unit
) {
    SlideshowScreenContent(
        albumId = albumId,
        albumRepository = albumRepository,
        settingsRepository = settingsRepository,
        syncManager = syncManager,
        onBack = onBack
    )
}

@Composable
private fun SlideshowScreenContent(
    albumId: String,
    albumRepository: AlbumRepository,
    settingsRepository: SettingsRepository,
    syncManager: SyncManager,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val playOrder by settingsRepository.playOrder.collectAsState()
    val transitionType by settingsRepository.transitionType.collectAsState()
    val photoDurationSec by settingsRepository.photoDurationSec.collectAsState()
    val playVideo by settingsRepository.playVideo.collectAsState()
    // 「视频」相册特权：即使全局关闭了视频播放，该相册仍保留并可播放视频
    val videoAllowed = playVideo

    var items by remember(albumId) { mutableStateOf<List<MediaItem>>(emptyList()) }
    var isLoading by remember(albumId) { mutableStateOf(true) }
    // 相册级后台刷新进行中（区别于「真的为空」，避免闪现「相册为空」）
    var isRefreshing by remember(albumId) { mutableStateOf(false) }
    val isGlobalSyncing by syncManager.isSyncing.collectAsState()
    val syncProgress by syncManager.syncProgress.collectAsState()
    var refreshError by remember(albumId) { mutableStateOf<String?>(null) }

    // 记录上次播放的相册，供开机自启直达
    LaunchedEffect(albumId) {
        settingsRepository.setLastAlbumId(albumId)
    }

    // 1. 先读本地缓存并立即显示（IO 线程执行：Keystore 解密/实体映射不占主线程，避免加载圈卡死）
    LaunchedEffect(albumId, videoAllowed) {
        isLoading = true
        refreshError = null
        val local = withContext(Dispatchers.IO) { albumRepository.getLocalMediaItems(albumId) }
        items = if (videoAllowed) local else local.filter { !it.isVideo }
        isLoading = false

        // 2. 后台刷新：全局同步正在跑时跳过，避免重复压力
        if (!isGlobalSyncing) {
            isRefreshing = true
            withContext(Dispatchers.IO) {
                runCatching {
                    albumRepository.refreshMediaItems(albumId).getOrThrow()
                }
            }.onSuccess { fresh ->
                items = if (videoAllowed) fresh else fresh.filter { !it.isVideo }
            }.onFailure { e ->
                refreshError = e.message
            }
            isRefreshing = false
        }
    }

    // 3. 全局同步完成后，重新加载本地最新数据
    LaunchedEffect(isGlobalSyncing) {
        if (!isGlobalSyncing) {
            val local = withContext(Dispatchers.IO) { albumRepository.getLocalMediaItems(albumId) }
            items = if (videoAllowed) local else local.filter { !it.isVideo }
        }
    }

    // 本地无数据但同步/刷新仍在进行时，显示加载态而不是误报「相册为空」
    if (items.isEmpty() && (isLoading || isRefreshing || isGlobalSyncing)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .pointerInput(Unit) { detectTapGestures { } },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(modifier = Modifier.size(72.dp))
                Spacer(modifier = Modifier.height(24.dp))

                val progressText = if (isGlobalSyncing && syncProgress.total > 0) {
                    "已同步 ${syncProgress.processed} / ${syncProgress.total} 张"
                } else {
                    "正在加载相册…"
                }
                Text(
                    text = progressText,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (syncProgress.message.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = syncProgress.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
        }
        return
    }

    if (items.isEmpty()) {
        BlockedStateScreen(
            message = refreshError?.let { "相册加载失败：$it" } ?: "相册为空",
            hint = "点击任意位置返回",
            tapAnywhereToExit = true,
            onBack = onBack
        )
        return
    }

    val controller = remember(items, photoDurationSec) {
        SlideshowController(
            items = items,
            initialOrder = playOrder,
            photoDurationMs = photoDurationSec * 1000L,
            scope = coroutineScope
        )
    }

    // 断点续播：按媒体文件 ID 记录/恢复播放位置（刷新打乱顺序也能对上）。
    // 只保留当前播放相册的进度：进入任何相册时清掉其他相册的记录
    LaunchedEffect(controller) {
        withContext(Dispatchers.IO) { albumRepository.clearOtherPlayPositions(albumId) }
        if (items.isNotEmpty()) {
            val savedFid = withContext(Dispatchers.IO) { albumRepository.getPlayPosition(albumId) }
            if (savedFid != null) {
                items.indexOfFirst { it.id.toLongOrNull() == savedFid }
                    .takeIf { it > 0 }
                    ?.let { controller.jumpTo(it) }
            }
        }
        // 恢复完成后再开始记录，避免启动时的初始位置覆盖已保存的进度
        var lastSaved: Long? = null
        controller.currentItem.collect { item ->
            val fid = item?.id?.toLongOrNull()
            if (fid != null && fid != lastSaved) {
                lastSaved = fid
                withContext(Dispatchers.IO) { albumRepository.savePlayPosition(albumId, fid) }
            }
        }
    }

    // 获取相册名称
    val album = remember(albumId) {
        kotlinx.coroutines.runBlocking { albumRepository.getAlbum(albumId) }
    }
    val albumName = album?.name ?: ""

    LaunchedEffect(playOrder) {
        controller.setOrder(playOrder)
    }

    SlideshowContent(
        controller = controller,
        transitionType = transitionType,
        albumRepository = albumRepository,
        videoAllowed = videoAllowed,
        photoDurationSec = photoDurationSec,
        albumName = albumName,
        onBack = onBack,
        onOrderChange = { settingsRepository.setPlayOrder(it) }
    )
}

/**
 * 加载中 / 相册为空等无内容状态的整屏页面。
 * 必须提供退出出口（左上角返回按钮 + 可选的整屏点击返回），避免死路页面。
 */
@Composable
private fun BlockedStateScreen(
    message: String,
    hint: String?,
    tapAnywhereToExit: Boolean,
    onBack: () -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    val iconColor = if (isDarkTheme) Color(0xFFEEEEEE) else Color(0xFF222222)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .then(if (tapAnywhereToExit) Modifier.clickableNoRipple(onBack) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = MaterialTheme.colorScheme.onBackground)
            if (hint != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = hint,
                    color = iconColor.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        // 左上角返回按钮，样式与轮播页顶部工具栏一致
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(48.dp)
                .clickableNoRipple(onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = iconColor,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun SlideshowContent(
    controller: SlideshowController,
    transitionType: TransitionType,
    albumRepository: AlbumRepository,
    videoAllowed: Boolean,
    photoDurationSec: Int,
    albumName: String,
    onBack: () -> Unit,
    onOrderChange: (PlayOrder) -> Unit
) {
    val currentItem by controller.currentItem.collectAsState()
    val currentIndex by controller.currentIndex.collectAsState()
    val isPlaying by controller.isPlaying.collectAsState()
    val currentOrder by controller.order.collectAsState()
    var controlsVisible by remember { mutableStateOf(true) }
    var showDetails by remember { mutableStateOf(false) }
    var showPlaylist by remember { mutableStateOf(false) }
    // 视频进度状态（仅控制层可见时更新，见 VideoPlayer 的 onProgress）
    var videoPositionMs by remember { mutableStateOf(0L) }
    var videoDurationMs by remember { mutableStateOf(0L) }
    var videoSeekTargetMs by remember { mutableStateOf<Long?>(null) }
    // 视频项的播放状态（与照片计时器分开管理）
    var videoPlaying by remember { mutableStateOf(true) }
    // 视频默认静音播放，可通过底部按钮取消静音
    var videoMuted by remember { mutableStateOf(true) }

    // 播放模式切换 Toast（屏幕中央提示，1.5 秒后自动淡出）
    var toastText by remember { mutableStateOf("") }
    var toastVisible by remember { mutableStateOf(false) }
    var toastTrigger by remember { mutableStateOf(0) }
    LaunchedEffect(toastTrigger) {
        if (toastTrigger > 0) {
            kotlinx.coroutines.delay(1500)
            toastVisible = false
        }
    }

    // 自动隐藏 UI 的计时器
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(controlsVisible, lastInteractionTime) {
        if (controlsVisible) {
            kotlinx.coroutines.delay(5000)
            if (System.currentTimeMillis() - lastInteractionTime >= 5000) {
                controlsVisible = false
            }
        }
    }

    // 预加载下一张的原图/缩略图，降低翻页白屏时间
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(currentItem?.id) {
        val playlist = controller.playlist.value
        val index = controller.currentIndex.value
        val next = playlist.getOrNull(index + 1) ?: playlist.firstOrNull()
        val url = next?.previewUrl ?: next?.originalUrl ?: next?.thumbnailUrl ?: return@LaunchedEffect
        val request = coil.request.ImageRequest.Builder(context)
            .data(url)
            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
            .build()
        coil.Coil.imageLoader(context).enqueue(request)
    }

    // 视频项：暂停照片计时器，由播放结束事件驱动翻页
    val isCurrentVideo = currentItem?.isVideo == true && videoAllowed && currentItem?.originalUrl != null
    LaunchedEffect(currentItem?.id, isCurrentVideo) {
        videoPlaying = true
        if (isCurrentVideo) controller.pause()
    }

    val isDarkTheme = isSystemInDarkTheme()
    val overlayBg = if (isDarkTheme) {
        Color(0x141414).copy(alpha = 0.65f)
    } else {
        Color(0xFFFFFF).copy(alpha = 0.65f)
    }
    val textColor = if (isDarkTheme) Color(0xFFEEEEEE) else Color(0xFF222222)
    val iconColor = if (isDarkTheme) Color(0xFFEEEEEE) else Color(0xFF222222)

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        if (dragAmount < -50) {
                            controller.next()
                            lastInteractionTime = System.currentTimeMillis()
                        } else if (dragAmount > 50) {
                            controller.previous()
                            lastInteractionTime = System.currentTimeMillis()
                        }
                    }
                }
        ) {
            AnimatedVisibility(visible = controlsVisible) {
                SlideshowTopBar(
                    currentItem = currentItem,
                    albumName = albumName,
                    textColor = textColor,
                    iconColor = iconColor,
                    overlayBg = overlayBg,
                    onBack = {
                        onBack()
                        lastInteractionTime = System.currentTimeMillis()
                    },
                    onDetails = {
                        showDetails = true
                        lastInteractionTime = System.currentTimeMillis()
                    },
                    onRestart = {
                        // 从头开始：恢复播放态后跳到第一张（restartTimer 需要 isPlaying=true）
                        controller.play()
                        controller.jumpTo(0)
                        lastInteractionTime = System.currentTimeMillis()
                    }
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickableNoRipple {
                        controlsVisible = !controlsVisible
                        lastInteractionTime = System.currentTimeMillis()
                    }
            ) {
                AnimatedContent(
                    targetState = currentItem,
                    transitionSpec = {
                        when (transitionType) {
                            // 交叉淡化：旧图同步淡出，既无黑闪间隙，也不会整张滞留在新图下面等转场结束才突兀消失
                            TransitionType.FADE -> fadeIn(tween(700)) togetherWith fadeOut(tween(700))
                            TransitionType.SLIDE -> slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                            TransitionType.CUT -> fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                            TransitionType.KEN_BURNS -> fadeIn(tween(900)) togetherWith fadeOut(tween(900))
                        }
                    },
                    label = "slideshow_transition"
                ) { item ->
                    if (item != null && item.isVideo && videoAllowed && item.originalUrl != null) {
                        VideoPlayer(
                            url = item.originalUrl!!,
                            play = videoPlaying,
                            muted = videoMuted,
                            onProgress = { posMs, durMs ->
                                // 仅控制层可见时上报，避免全屏播放时持续重组
                                if (controlsVisible) {
                                    videoPositionMs = posMs
                                    videoDurationMs = durMs
                                }
                            },
                            seekTargetMs = videoSeekTargetMs,
                            // 视频结束后恢复照片计时器：此前 controller 被 pause()，
                            // 直接 next() 不会重启定时器（restartTimer 见 isPlaying=false 直接返回），
                            // 导致“视频播完不继续轮播/首项为视频时永不轮播”。
                            onEnded = {
                                controller.play()
                                controller.next()
                            }
                        )
                    } else {
                        MediaPlaceholder(
                            item = item,
                            transitionType = transitionType,
                            photoDurationMs = photoDurationSec * 1000L,
                            index = currentIndex
                        )
                    }
                }
            }

            // 视频进度条：仅控制层可见且当前是视频时显示（全屏无控制层时隐藏）
            AnimatedVisibility(
                visible = controlsVisible && isCurrentVideo && videoDurationMs > 0
            ) {
                var dragPosMs by remember { mutableStateOf<Long?>(null) }
                val shownPosMs = dragPosMs ?: videoPositionMs.coerceIn(0L, videoDurationMs)
                Column {
                    Text(
                        text = "${formatMs(shownPosMs)} / ${formatMs(videoDurationMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(start = 40.dp)
                    )
                    Slider(
                        value = if (videoDurationMs > 0) {
                            shownPosMs.toFloat() / videoDurationMs
                        } else 0f,
                        onValueChange = { frac -> dragPosMs = (frac * videoDurationMs).toLong() },
                        onValueChangeFinished = {
                            dragPosMs?.let { videoSeekTargetMs = it }
                            dragPosMs = null
                            lastInteractionTime = System.currentTimeMillis()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                            .height(24.dp)
                    )
                }
            }

            AnimatedVisibility(visible = controlsVisible) {
                SlideshowBottomBar(
                    isPlaying = if (isCurrentVideo) videoPlaying else isPlaying,
                    showMuteButton = isCurrentVideo,
                    isMuted = videoMuted,
                    onMuteToggle = {
                        videoMuted = !videoMuted
                        lastInteractionTime = System.currentTimeMillis()
                    },
                    currentOrder = currentOrder,
                    iconColor = iconColor,
                    overlayBg = overlayBg,
                    onPlayPause = {
                        if (isCurrentVideo) videoPlaying = !videoPlaying else controller.togglePlay()
                        lastInteractionTime = System.currentTimeMillis()
                    },
                    onOrderToggle = {
                        val next = when (currentOrder) {
                            PlayOrder.FORWARD -> PlayOrder.REVERSE
                            PlayOrder.REVERSE -> PlayOrder.RANDOM
                            PlayOrder.RANDOM -> PlayOrder.FORWARD
                        }
                        onOrderChange(next)
                        toastText = when (next) {
                            PlayOrder.FORWARD -> "正序播放"
                            PlayOrder.REVERSE -> "倒序播放"
                            PlayOrder.RANDOM -> "随机播放"
                        }
                        toastVisible = true
                        toastTrigger++
                        lastInteractionTime = System.currentTimeMillis()
                    },
                    onPlaylist = {
                        showPlaylist = true
                        lastInteractionTime = System.currentTimeMillis()
                    }
                )
            }
        }

        if (showDetails && currentItem != null) {
            DetailsDialog(
                item = currentItem!!,
                albumRepository = albumRepository,
                onDismiss = { showDetails = false }
            )
        }

        if (showPlaylist) {
            PlaylistSheet(
                items = controller.playlist.collectAsState().value,
                currentIndex = controller.currentIndex.collectAsState().value,
                onDismiss = { showPlaylist = false },
                onItemClick = { index ->
                    controller.jumpTo(index)
                    showPlaylist = false
                },
                onDeleteItems = { indices -> controller.deleteAt(indices) },
                onMoveItem = { from, to -> controller.moveItem(from, to) },
                onRestoreItems = { removed -> controller.restoreItems(removed) }
            )
        }

        // 播放模式切换提示（屏幕中央，不拦截触摸事件）
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ModeToast(text = toastText, visible = toastVisible)
        }
    }
}

@Composable
private fun MediaPlaceholder(
    item: MediaItem?,
    transitionType: TransitionType,
    photoDurationMs: Long,
    index: Int
) {
    val kenBurns = item != null && !item.isVideo && transitionType == TransitionType.KEN_BURNS
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val displayUrl = item?.previewUrl ?: item?.originalUrl
        if (displayUrl != null) {
            KenBurnsImage(
                url = displayUrl,
                item = item,
                kenBurns = kenBurns,
                photoDurationMs = photoDurationMs,
                index = index
            )
        } else if (item?.thumbnailUrl != null) {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = item.fileName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (item?.isVideo == true) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                }
                Text(
                    text = item?.fileName ?: "无内容",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
        if (item?.isVideo == true) {
            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun KenBurnsImage(
    url: String,
    item: MediaItem?,
    kenBurns: Boolean,
    photoDurationMs: Long,
    index: Int
) {
    val progress = remember(url) { Animatable(0f) }
    LaunchedEffect(url, kenBurns) {
        progress.snapTo(0f)
        if (kenBurns) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = photoDurationMs.coerceAtLeast(1000L).toInt(),
                    easing = LinearEasing
                )
            )
        }
    }
    val p = if (kenBurns) progress.value else 0f
    val mode = index % 4
    val scale = when {
        !kenBurns -> 1f
        mode == 3 -> 1.08f - 0.08f * p
        else -> 1f + 0.08f * p
    }
    val tx = when {
        !kenBurns -> 0f
        mode == 0 -> (p * 2f - 1f) * 0.03f
        mode == 1 -> (1f - p * 2f) * 0.03f
        else -> 0f
    }
    val ty = when {
        !kenBurns -> 0f
        mode == 0 -> (p * 2f - 1f) * 0.03f
        mode == 1 -> (1f - p * 2f) * 0.03f
        else -> 0f
    }

    AsyncImage(
        model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
            .data(url)
            .crossfade(false) // 避免“AnimatedContent 淡入 + Coil 二次淡入”叠加造成黑屏闪烁
            .build(),
        contentDescription = item?.fileName,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val w = size.width
                val h = size.height
                scaleX = scale
                scaleY = scale
                translationX = tx * w
                translationY = ty * h
            },
        contentScale = ContentScale.Fit
    )
}

private fun formatMs(ms: Long): String = "%d:%02d".format(ms / 60000, (ms / 1000) % 60)

@Composable
private fun VideoPlayer(
    url: String,
    play: Boolean,
    muted: Boolean = true,
    onProgress: (positionMs: Long, durationMs: Long) -> Unit = { _, _ -> },
    seekTargetMs: Long? = null,
    onEnded: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val letterboxColor = MaterialTheme.colorScheme.background.toArgb()
    val exoPlayer = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(ExoMediaItem.fromUri(url))
            repeatMode = Player.REPEAT_MODE_OFF
            volume = if (muted) 0f else 1f
            prepare()
            playWhenReady = true
        }
    }

    // 首次播放失败自动重试一次：转码流冷启动或 auth_code 竞态时，第二次通常成功
    var videoRetried by remember(url) { mutableStateOf(false) }
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) onEnded()
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("SlideshowVideo", "视频播放失败: ${error.errorCodeName}", error)
                if (!videoRetried) {
                    videoRetried = true
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        exoPlayer.seekTo(0)
                        exoPlayer.prepare()
                        exoPlayer.play()
                    }, 1000L)
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            // 先 stop 释放解码器与 surface 再 release：
            // 对转码中的大视频直接 release 可能阻塞主线程数秒，表现为退出后画面卡住
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.release()
        }
    }

    LaunchedEffect(play) {
        if (play) exoPlayer.play() else exoPlayer.pause()
    }

    LaunchedEffect(muted) {
        exoPlayer.volume = if (muted) 0f else 1f
    }

    // 进度上报：每 500ms 采样一次（rememberUpdatedState 保证拿到最新回调）
    val progressCallback by rememberUpdatedState(onProgress)
    LaunchedEffect(exoPlayer) {
        while (true) {
            val dur = exoPlayer.duration
            if (dur > 0) progressCallback(exoPlayer.currentPosition, dur)
            delay(500)
        }
    }

    // 进度条跳转请求
    LaunchedEffect(seekTargetMs) {
        if (seekTargetMs != null) exoPlayer.seekTo(seekTargetMs)
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                setBackgroundColor(letterboxColor)
                player = exoPlayer
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun SlideshowTopBar(
    currentItem: MediaItem?,
    albumName: String,
    textColor: Color,
    iconColor: Color,
    overlayBg: Color,
    onBack: () -> Unit,
    onDetails: () -> Unit,
    onRestart: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(overlayBg)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 返回按钮
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickableNoRipple(onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.size(12.dp))

            // 照片信息（日期时间 + 地点）
            currentItem?.let { item ->
                val dateText = remember(item.tokenAt) {
                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(item.tokenAt))
                }
                val location = item.gpsInfo?.display()?.ifBlank { null }
                val infoText = buildString {
                    append(dateText)
                    if (location != null) {
                        append(" ")
                        append(location)
                    }
                }
                Text(
                    text = infoText,
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            } ?: Spacer(modifier = Modifier.weight(1f))

            // 相册名称（居中）
            Text(
                text = albumName,
                color = textColor,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.CenterVertically),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            // 从头开始：跳回第一张重新播放
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickableNoRipple(onRestart),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Replay,
                    contentDescription = "从头开始",
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            // ⋮ 更多菜单按钮
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickableNoRipple(onDetails),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "更多",
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun SlideshowBottomBar(
    isPlaying: Boolean,
    showMuteButton: Boolean = false,
    isMuted: Boolean = true,
    onMuteToggle: () -> Unit = {},
    currentOrder: PlayOrder,
    iconColor: Color,
    overlayBg: Color,
    onPlayPause: () -> Unit,
    onOrderToggle: () -> Unit,
    onPlaylist: () -> Unit
) {
    val orderIcon = when (currentOrder) {
        PlayOrder.FORWARD -> PlayOrderIcons.Ascending
        PlayOrder.REVERSE -> PlayOrderIcons.Descending
        PlayOrder.RANDOM -> PlayOrderIcons.Shuffle
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(overlayBg)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左下角：播放模式 + 静音切换（仅当前项为视频时显示）
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clickableNoRipple(onOrderToggle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = orderIcon,
                        contentDescription = "播放模式",
                        tint = iconColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
                if (showMuteButton) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clickableNoRipple(onMuteToggle),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isMuted) "取消静音" else "静音",
                            tint = iconColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // 底部中央：播放/暂停
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clickableNoRipple(onPlayPause),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    tint = iconColor,
                    modifier = Modifier.size(48.dp)
                )
            }

            // 右下角：播放列表
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clickableNoRipple(onPlaylist),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = "播放列表",
                    tint = iconColor,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun ModeToast(text: String, visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(300))
    ) {
        Box(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = text,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun DetailsDialog(
    item: MediaItem,
    albumRepository: AlbumRepository,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var detailed by remember(item.id) { mutableStateOf(item) }
    var isLoading by remember(item.id) { mutableStateOf(false) }

    LaunchedEffect(item.id) {
        if (detailed.gpsInfo == null || detailed.extra == null) {
            isLoading = true
            val result = albumRepository.refreshFileDetails(item.id)
            result.getOrNull()?.let { detailed = it }
            isLoading = false
        }
    }

    val dateText = remember(detailed.tokenAt) {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(detailed.tokenAt))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("详细信息") },
        text = {
            Column {
                if (isLoading) {
                    Text("补全信息中…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                DetailLine("拍摄时间", dateText)
                DetailLine("地点", detailed.gpsInfo?.display()?.ifBlank { "—" } ?: "—")
                DetailLine("文件名", detailed.fileName)
                DetailLine(
                    "尺寸",
                    if (detailed.width != null && detailed.height != null) "${detailed.width} × ${detailed.height}" else "—"
                )
                DetailLine("时长", if (detailed.duration != null) "${detailed.duration}s" else "—")
                DetailLine("拍摄设备", detailed.extra?.display() ?: "—")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "$label：",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.35f)
        )
        Text(
            text = value,
            modifier = Modifier.weight(0.65f)
        )
    }
}

// 多选模式下拖拽累计偏移（非状态，避免每帧重组）
private class DragAccumulator {
    var total: Offset = Offset.Zero
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun PlaylistSheet(
    items: List<MediaItem>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onItemClick: (Int) -> Unit,
    onDeleteItems: (List<Int>) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onRestoreItems: (List<Pair<Int, MediaItem>>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // 打开时定位到当前播放项，避免每次都从顶部翻起
    val gridState = rememberLazyGridState()
    LaunchedEffect(Unit) {
        if (items.isNotEmpty()) {
            gridState.scrollToItem(currentIndex.coerceIn(0, items.size - 1))
        }
    }

    // 简化方案：长按 ≥500ms 进入多选模式；多选下勾选项支持拖动重新排序
    var multiSelect by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val dragAcc = remember { DragAccumulator() }
    val cellBounds = remember { mutableStateMapOf<Int, Rect>() }

    // 删除撤销缓冲（3 秒内可撤销）
    var undoBuffer by remember { mutableStateOf<List<Pair<Int, MediaItem>>?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun exitMultiSelect() {
        multiSelect = false
        selectedIds = emptySet()
        draggingIndex = null
    }

    fun performDelete() {
        val indices = items.withIndex().filter { it.value.id in selectedIds }.map { it.index }
        if (indices.isEmpty()) return
        val removed = indices.map { it to items[it] }
        onDeleteItems(indices)
        undoBuffer = removed
        exitMultiSelect()
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "已删除 ${removed.size} 项",
                actionLabel = "撤销",
                duration = SnackbarDuration.Indefinite
            )
            if (result == SnackbarResult.ActionPerformed) {
                undoBuffer?.let { onRestoreItems(it) }
                if (undoBuffer === removed) undoBuffer = null
            }
        }
        scope.launch {
            kotlinx.coroutines.delay(3000)
            if (undoBuffer === removed) {
                snackbarHostState.currentSnackbarData?.dismiss()
                undoBuffer = null
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .padding(16.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        if (multiSelect) exitMultiSelect()
                    })
                }
        ) {
            Text(
                text = "播放列表",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.size(12.dp))
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(items.size, key = { items[it].id }) { index ->
                    val item = items[index]
                    val isCurrent = index == currentIndex
                    val isSelected = item.id in selectedIds
                    val isDragging = draggingIndex == index
                    val itemIdState by rememberUpdatedState(item.id)
                    val itemIndexState by rememberUpdatedState(index)
                    val currentItems by rememberUpdatedState(items)
                    Box(
                        modifier = Modifier
                            .animateItemPlacement()
                            .size(100.dp)
                            .zIndex(if (isDragging) 1f else 0f)
                            .graphicsLayer {
                                val scale = if (isDragging) 1.08f else 1f
                                scaleX = scale
                                scaleY = scale
                                shape = RoundedCornerShape(8.dp)
                                shadowElevation = if (isDragging) 8.dp.toPx() else 0f
                            }
                            .onGloballyPositioned { cellBounds[index] = it.boundsInRoot() }
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .then(
                                if (isCurrent || isSelected) {
                                    Modifier.border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                } else Modifier
                            )
                            .clickableNoRipple {
                                if (multiSelect) {
                                    selectedIds = if (itemIdState in selectedIds) {
                                        selectedIds - itemIdState
                                    } else {
                                        selectedIds + itemIdState
                                    }
                                } else {
                                    onItemClick(itemIndexState)
                                }
                            }
                            .pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        // 长按触发：进入多选并勾选当前项（简化方案），随后可直接拖动
                                        multiSelect = true
                                        selectedIds = selectedIds + itemIdState
                                        dragAcc.total = Offset.Zero
                                        draggingIndex = itemIndexState
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        val dragging = draggingIndex
                                            ?: return@detectDragGesturesAfterLongPress
                                        dragAcc.total += amount
                                        val draggedBounds = cellBounds[dragging]
                                            ?: return@detectDragGesturesAfterLongPress
                                        val center = draggedBounds.center + dragAcc.total
                                        val target = cellBounds.minByOrNull { (_, rect) ->
                                            (rect.center - center).getDistance()
                                        }?.key
                                            ?: return@detectDragGesturesAfterLongPress
                                        if (target != dragging && target in currentItems.indices) {
                                            onMoveItem(dragging, target)
                                            draggingIndex = target
                                        }
                                    },
                                    onDragEnd = { draggingIndex = null },
                                    onDragCancel = { draggingIndex = null }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val url = item.thumbnailUrl ?: item.originalUrl
                        if (url != null) {
                            AsyncImage(
                                model = url,
                                contentDescription = item.fileName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        if (!multiSelect) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(4.dp)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else Color.Black.copy(alpha = 0.45f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        if (item.isVideo) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            if (multiSelect) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "已选 ${selectedIds.size} 项",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row {
                        TextButton(
                            onClick = { if (selectedIds.isNotEmpty()) showDeleteConfirm = true }
                        ) {
                            Text("删除", color = MaterialTheme.colorScheme.error)
                        }
                        TextButton(onClick = { exitMultiSelect() }) {
                            Text("取消")
                        }
                    }
                }
            }

            SnackbarHost(hostState = snackbarHostState)
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除") },
            text = { Text("确定从播放列表中删除选中的 ${selectedIds.size} 张图片吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    performDelete()
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("撤销")
                }
            }
        )
    }
}

@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this then clickable(
        indication = null,
        interactionSource = remember { MutableInteractionSource() }
    ) { onClick() }
