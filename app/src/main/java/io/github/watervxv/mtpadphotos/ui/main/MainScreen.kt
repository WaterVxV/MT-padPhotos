@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.watervxv.mtpadphotos.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.github.watervxv.mtpadphotos.data.media.MediaUrlBuilder
import io.github.watervxv.mtpadphotos.data.remote.AuthManager
import io.github.watervxv.mtpadphotos.data.repo.AlbumRepository
import io.github.watervxv.mtpadphotos.data.repo.SearchRepository
import io.github.watervxv.mtpadphotos.data.repo.SettingsRepository
import io.github.watervxv.mtpadphotos.data.sync.SyncManager
import io.github.watervxv.mtpadphotos.data.sync.SyncProgress
import io.github.watervxv.mtpadphotos.data.sync.SyncWorker
import io.github.watervxv.mtpadphotos.domain.model.Album
import io.github.watervxv.mtpadphotos.domain.model.SmartAlbumType
import io.github.watervxv.mtpadphotos.ui.rom.RomGuideDialog
import io.github.watervxv.mtpadphotos.ui.tutorial.TutorialOverlay
import io.github.watervxv.mtpadphotos.util.NetworkMonitor
import io.github.watervxv.mtpadphotos.util.RomDetector
import io.github.watervxv.mtpadphotos.util.RomFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    albumRepository: AlbumRepository,
    settingsRepository: SettingsRepository,
    authManager: AuthManager,
    networkMonitor: NetworkMonitor,
    syncManager: SyncManager,
    searchRepository: SearchRepository,
    onAlbumClick: (Album) -> Unit,
    onSettingsClick: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val albums by albumRepository.observeAlbums().collectAsState(initial = emptyList())
    val tutorialShown by settingsRepository.tutorialShown.collectAsState()
    val authCode by authManager.authCode.collectAsState(initial = null)
    val isOnline by networkMonitor.isOnline.collectAsState(initial = networkMonitor.isOnlineNow())
    val isSyncing by syncManager.isSyncing.collectAsState()
    val syncProgress by syncManager.syncProgress.collectAsState()
    val initialSyncCompleted by settingsRepository.initialSyncCompleted.collectAsState()
    val syncFrequency by settingsRepository.syncFrequency.collectAsState()
    val urlBuilder = remember(authManager.getServerUrl()) {
        MediaUrlBuilder(authManager.getServerUrl() ?: "")
    }
    var query by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<io.github.watervxv.mtpadphotos.domain.model.MediaItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val romGuideDismissed by settingsRepository.romGuideDismissed.collectAsState()
    var showRomGuide by remember { mutableStateOf(false) }
    val romGuide = remember { RomDetector.currentGuide() }
    var syncAttempted by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        if (query.isBlank()) {
            searchResults = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        delay(500)
        if (query.isNotBlank()) {
            val result = searchRepository.search(query)
            searchResults = result.getOrDefault(emptyList())
        }
        isSearching = false
    }

    // 进入主界面自动触发同步；异常在 SyncManager 内捕获，这里再做一层保护
    LaunchedEffect(Unit) {
        runCatching { syncManager.syncAll() }
    }

    // 记录是否已开始过同步，用于成功后标记 initialSyncCompleted
    LaunchedEffect(syncProgress.isSyncing) {
        if (syncProgress.isSyncing) syncAttempted = true
    }

    // 首次同步成功后标记完成，后续登录不再显示阻塞遮罩
    LaunchedEffect(syncProgress, syncAttempted, initialSyncCompleted) {
        if (initialSyncCompleted) return@LaunchedEffect
        if (syncAttempted && !syncProgress.isSyncing && syncProgress.error == null) {
            settingsRepository.setInitialSyncCompleted(true)
        }
    }

    LaunchedEffect(syncFrequency) {
        SyncWorker.schedule(context, syncFrequency)
    }

    val userAlbums = albums.filter { !it.isSmart }.filter {
        query.isBlank() || it.name.contains(query, ignoreCase = true)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        PullToRefreshBox(
            isRefreshing = isSyncing,
            onRefresh = { scope.launch { syncManager.syncAll(force = true) } },
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                TopBar(
                    query = query,
                    onQueryChange = { query = it },
                    onSettingsClick = onSettingsClick
                )
                if (!isOnline) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "离线模式",
                        color = Color(0xFFFFA000),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                } else if (isSyncing) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "同步中…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                if (romGuide.family != RomFamily.OTHER && !romGuideDismissed) {
                    Spacer(modifier = Modifier.height(8.dp))
                    RomGuideBanner(
                        familyName = romGuide.family.displayName,
                        onOpen = { showRomGuide = true },
                        onDismiss = { settingsRepository.dismissRomGuide() }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (query.isNotBlank()) {
                    SearchResultsSection(
                        results = searchResults,
                        isSearching = isSearching,
                        query = query
                    )
                } else {
                    Text(
                        text = "相册（${userAlbums.size}）",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AlbumGrid(
                        albums = userAlbums,
                        authCode = authCode,
                        urlBuilder = urlBuilder,
                        albumRepository = albumRepository,
                        onAlbumClick = onAlbumClick
                    )
                }
            }
        }

        if (showRomGuide) {
            RomGuideDialog(guide = romGuide, onDismiss = { showRomGuide = false })
        }

        if (!tutorialShown) {
            TutorialOverlay(
                onFinish = { settingsRepository.markTutorialShown() },
                onSkip = { settingsRepository.markTutorialShown() }
            )
        }

        // 首次登录后的阻塞式同步遮罩，完成后自动消失；假数据模式下不显示
        if (!initialSyncCompleted && syncManager.isRealSyncEnabled) {
            InitialSyncOverlay(
                progress = syncProgress,
                onRetry = { scope.launch { syncManager.syncAll(force = true) } },
                onLogout = onLogout
            )
        }
    }
}

@Composable
private fun InitialSyncOverlay(
    progress: SyncProgress,
    onRetry: () -> Unit,
    onLogout: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            val fraction = if (progress.total > 0) {
                (progress.processed.toFloat() / progress.total.toFloat()).coerceIn(0f, 1f)
            } else null

            if (fraction != null) {
                CircularProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.size(72.dp),
                    strokeWidth = 6.dp,
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(72.dp),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (progress.total > 0) "已同步 ${progress.processed} / ${progress.total} 张" else "正在同步…",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = progress.message,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f)
            )

            if (progress.error != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "同步失败：${progress.error}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onRetry) {
                        Text("重试")
                    }
                    OutlinedButton(onClick = onLogout) {
                        Text("退出登录")
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("搜索照片或相册") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )
        Spacer(modifier = Modifier.width(12.dp))
        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "设置",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun SmartAlbumRow(
    albums: List<Album>,
    authCode: String?,
    urlBuilder: MediaUrlBuilder,
    albumRepository: AlbumRepository,
    onAlbumClick: (Album) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(albums, key = { it.id }) { album ->
            AlbumCard(
                album = album,
                authCode = authCode,
                urlBuilder = urlBuilder,
                albumRepository = albumRepository,
                onClick = { onAlbumClick(album) },
                cardWidth = 160.dp,
                cardHeight = 120.dp
            )
        }
    }
}

@Composable
private fun AlbumGrid(
    albums: List<Album>,
    authCode: String?,
    urlBuilder: MediaUrlBuilder,
    albumRepository: AlbumRepository,
    onAlbumClick: (Album) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(albums, key = { it.id }) { album ->
            AlbumCard(
                album = album,
                authCode = authCode,
                urlBuilder = urlBuilder,
                albumRepository = albumRepository,
                onClick = { onAlbumClick(album) },
                cardWidth = 180.dp,
                cardHeight = 150.dp
            )
        }
    }
}

@Composable
private fun SearchResultsSection(
    results: List<io.github.watervxv.mtpadphotos.domain.model.MediaItem>,
    isSearching: Boolean,
    query: String
) {
    Column {
        Text(
            text = "「$query」搜索结果",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (isSearching) {
            Text("搜索中…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (results.isEmpty()) {
            Text("未找到结果", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(results.size) { index ->
                    val item = results[index]
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        AsyncImage(
                            model = item.thumbnailUrl ?: item.originalUrl,
                            contentDescription = item.fileName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        if (item.isVideo) {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.Center)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumCard(
    album: Album,
    authCode: String?,
    urlBuilder: MediaUrlBuilder,
    albumRepository: AlbumRepository,
    onClick: () -> Unit,
    cardWidth: Dp,
    cardHeight: Dp
) {
    Card(
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                val coverUrl = remember(album.coverMd5, authCode) {
                    album.coverMd5?.takeIf { it.isNotBlank() && !authCode.isNullOrBlank() }
                        ?.let { urlBuilder.thumbnailUrl(MediaUrlBuilder.ThumbnailType.S260, it, authCode!!) }
                }
                var fallbackUrl by remember(album.id) { mutableStateOf<String?>(null) }
                var fallbackReady by remember(album.id) { mutableStateOf(false) }
                LaunchedEffect(album.id, coverUrl) {
                    if (coverUrl == null && !fallbackReady) {
                        fallbackUrl = albumRepository.getCoverPreview(album.id)
                        fallbackReady = true
                    }
                }

                val displayUrl = coverUrl ?: fallbackUrl
                val isVideoAlbum = album.smartType == SmartAlbumType.VIDEOS || album.id == "smart_videos"
                if (displayUrl != null) {
                    AsyncImage(
                        model = displayUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    if (isVideoAlbum) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(36.dp)
                                .align(Alignment.Center)
                        )
                    }
                } else {
                    GradientPlaceholder(album = album, isVideoAlbum = isVideoAlbum)
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${album.itemCount} 项",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GradientPlaceholder(album: Album, isVideoAlbum: Boolean) {
    val hue = (album.id.hashCode() and 0x7FFFFFFF) % 360
    val c1 = Color.hsv(hue.toFloat(), 0.45f, 0.32f)
    val c2 = Color.hsv(hue.toFloat(), 0.45f, 0.48f)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(c1, c2))),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isVideoAlbum) Icons.Default.VideoLibrary else Icons.Default.Image,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
private fun RomGuideBanner(
    familyName: String,
    onOpen: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "检测到 $familyName：请开启「自启动」与「省电白名单」，保证相册常驻运行",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onOpen) { Text("查看") }
        TextButton(onClick = onDismiss) {
            Text("忽略", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
