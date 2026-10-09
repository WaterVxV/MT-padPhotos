package io.github.watervxv.mtpadphotos.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.github.watervxv.mtpadphotos.data.local.prefs.SecureKeyStore
import io.github.watervxv.mtpadphotos.data.media.CacheManager
import io.github.watervxv.mtpadphotos.data.remote.AuthManager
import io.github.watervxv.mtpadphotos.data.repo.SettingsRepository
import io.github.watervxv.mtpadphotos.domain.model.PlayOrder
import io.github.watervxv.mtpadphotos.domain.model.ThemeMode
import io.github.watervxv.mtpadphotos.domain.model.TransitionType
import io.github.watervxv.mtpadphotos.util.RomDetector
import io.github.watervxv.mtpadphotos.ui.pin.PinDots
import io.github.watervxv.mtpadphotos.ui.pin.PinPad
import io.github.watervxv.mtpadphotos.ui.rom.RomGuideDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    cacheManager: CacheManager,
    secureKeyStore: SecureKeyStore,
    authManager: AuthManager,
    onBack: () -> Unit,
    onServerConfigSaved: () -> Unit
) {
    val scope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            item { SettingsSectionTitle("播放") }
            item {
                val duration by settingsRepository.photoDurationSec.collectAsState()
                PhotoDurationSetting(
                    value = duration,
                    onValueChange = { settingsRepository.setPhotoDurationSec(it) }
                )
            }
            item {
                val playVideo by settingsRepository.playVideo.collectAsState()
                ToggleSetting(
                    title = "播放视频",
                    checked = playVideo,
                    onCheckedChange = { settingsRepository.setPlayVideo(it) }
                )
            }
            item {
                val videoSound by settingsRepository.videoSound.collectAsState()
                ToggleSetting(
                    title = "视频默认外放",
                    checked = videoSound,
                    onCheckedChange = { settingsRepository.setVideoSound(it) }
                )
            }
            item {
                val order by settingsRepository.playOrder.collectAsState()
                OrderSetting(
                    value = order,
                    onValueChange = { settingsRepository.setPlayOrder(it) }
                )
            }
            item {
                val transition by settingsRepository.transitionType.collectAsState()
                TransitionSetting(
                    value = transition,
                    onValueChange = { settingsRepository.setTransitionType(it) }
                )
            }

            item { SettingsSectionTitle("外观") }
            item {
                val theme by settingsRepository.themeMode.collectAsState()
                ThemeSetting(
                    value = theme,
                    onValueChange = { settingsRepository.setThemeMode(it) }
                )
            }

            item { SettingsSectionTitle("同步与缓存") }
            item {
                val freq by settingsRepository.syncFrequency.collectAsState()
                SyncFrequencySetting(
                    value = freq,
                    onValueChange = { settingsRepository.setSyncFrequency(it) }
                )
            }
            item {
                val cacheLimit by settingsRepository.cacheLimitPercent.collectAsState()
                CacheLimitSetting(
                    value = cacheLimit,
                    onValueChange = { settingsRepository.setCacheLimitPercent(it) }
                )
            }
            item {
                var cacheCleared by remember { mutableStateOf(false) }
                Button(
                    onClick = {
                        scope.launch {
                            cacheManager.clearCache()
                            cacheCleared = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(if (cacheCleared) "缓存已清理" else "清理缓存")
                }
            }

            item { SettingsSectionTitle("安全") }
            item {
                PinSection(
                    settingsRepository = settingsRepository,
                    secureKeyStore = secureKeyStore
                )
            }

            item { SettingsSectionTitle("系统") }
            item {
                var showRomGuide by remember { mutableStateOf(false) }
                SettingLink("开机自启与省电设置") { showRomGuide = true }
                if (showRomGuide) {
                    RomGuideDialog(
                        guide = RomDetector.currentGuide(),
                        onDismiss = { showRomGuide = false }
                    )
                }
            }

            item { SettingsSectionTitle("服务器") }
            item {
                ServerConnectionSection(
                    authManager = authManager,
                    secureKeyStore = secureKeyStore,
                    onSaved = onServerConfigSaved
                )
            }
        }
    }
}

@Composable
private fun ServerConnectionSection(
    authManager: AuthManager,
    secureKeyStore: SecureKeyStore,
    onSaved: () -> Unit
) {
    // 预填当前已保存的配置；API Key 不落日志
    var serverUrl by remember { mutableStateOf(secureKeyStore.getServerUrl() ?: "") }
    var apiKey by remember { mutableStateOf(secureKeyStore.getApiKey() ?: "") }
    var keyVisible by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column {
        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it; isError = false },
            label = { Text("NAS 地址") },
            singleLine = true,
            enabled = !isVerifying,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it; isError = false },
            label = { Text("API Key") },
            singleLine = true,
            enabled = !isVerifying,
            visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { keyVisible = !keyVisible }) {
                    Icon(
                        imageVector = if (keyVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (keyVisible) "隐藏 API Key" else "显示 API Key"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        message?.let { msg ->
            Text(
                text = msg,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        Button(
            enabled = !isVerifying,
            onClick = {
                if (isVerifying) return@Button
                val normalizedUrl = serverUrl.trim().trimEnd('/')
                    .let { if (it.startsWith("http://") || it.startsWith("https://")) it else "http://$it" }
                val trimmedKey = apiKey.trim()
                when {
                    normalizedUrl.length < 12 -> {
                        isError = true
                        message = "NAS 地址格式不正确"
                    }
                    trimmedKey.isBlank() -> {
                        isError = true
                        message = "请输入 API Key"
                    }
                    else -> {
                        // 记住旧配置，验证失败时回滚，避免把可用配置改坏
                        val oldUrl = secureKeyStore.getServerUrl()
                        val oldKey = secureKeyStore.getApiKey()
                        isVerifying = true
                        message = null
                        scope.launch {
                            authManager.saveCredentials(normalizedUrl, trimmedKey)
                            authManager.refreshAuthCode().fold(
                                onSuccess = {
                                    isVerifying = false
                                    isError = false
                                    message = "已保存，服务器连接成功"
                                    onSaved()
                                },
                                onFailure = { e ->
                                    // 回滚到旧配置，保证原连接不被改坏
                                    if (oldUrl != null && oldKey != null) {
                                        authManager.saveCredentials(oldUrl, oldKey)
                                    }
                                    isVerifying = false
                                    isError = true
                                    message = when (e) {
                                        is java.net.UnknownHostException -> "无法解析地址，请检查 NAS 地址（配置未保存）"
                                        is java.net.ConnectException -> "无法连接 NAS，请检查地址与网络（配置未保存）"
                                        is java.net.SocketTimeoutException -> "连接超时，请检查 NAS 地址与网络（配置未保存）"
                                        else -> e.message?.takeIf { it.isNotBlank() } ?: "验证失败，配置未保存"
                                    }
                                }
                            )
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isVerifying) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (isVerifying) "正在验证…" else "保存并验证")
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}

@Composable
private fun ToggleSetting(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingLink(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "›",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleLarge
        )
    }
}

@Composable
private fun PhotoDurationSetting(value: Int, onValueChange: (Int) -> Unit) {
    val presets = listOf(5, 10, 15)
    val isCustom = value !in presets
    var showCustomDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text("照片展示时长：$value 秒", color = MaterialTheme.colorScheme.onSurface)
        Row(modifier = Modifier.padding(top = 8.dp)) {
            presets.forEach { sec ->
                TextButton(
                    onClick = { onValueChange(sec) },
                    enabled = sec != value
                ) {
                    Text(
                        text = "$sec 秒",
                        color = if (sec == value) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            TextButton(onClick = { showCustomDialog = true }) {
                Text(
                    text = if (isCustom) "自定义 (${value}s)" else "自定义",
                    color = if (isCustom) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    if (showCustomDialog) {
        CustomDurationDialog(
            initial = if (isCustom) value else 15,
            onConfirm = {
                onValueChange(it)
                showCustomDialog = false
            },
            onDismiss = { showCustomDialog = false }
        )
    }
}

@Composable
private fun CustomDurationDialog(
    initial: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initial.toString()) }
    val parsed = text.toIntOrNull()
    val valid = parsed != null && parsed in 1..300

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义时长") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("秒") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = text.isNotEmpty() && !valid
                )
                if (text.isNotEmpty() && !valid) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "请输入 1~300 之间的整数",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let(onConfirm) },
                enabled = valid
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun ThemeSetting(value: ThemeMode, onValueChange: (ThemeMode) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text("主题", color = MaterialTheme.colorScheme.onSurface)
        Row(modifier = Modifier.padding(top = 8.dp)) {
            ThemeMode.entries.forEach { mode ->
                val label = when (mode) {
                    ThemeMode.SYSTEM -> "跟随系统"
                    ThemeMode.LIGHT -> "浅色"
                    ThemeMode.DARK -> "深色"
                }
                TextButton(
                    onClick = { onValueChange(mode) },
                    enabled = mode != value
                ) {
                    Text(
                        text = label,
                        color = if (mode == value) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderSetting(value: PlayOrder, onValueChange: (PlayOrder) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text("播放顺序", color = MaterialTheme.colorScheme.onSurface)
        Row(modifier = Modifier.padding(top = 8.dp)) {
            PlayOrder.entries.forEach { order ->
                val label = when (order) {
                    PlayOrder.FORWARD -> "正序"
                    PlayOrder.REVERSE -> "倒序"
                    PlayOrder.RANDOM -> "随机"
                }
                TextButton(
                    onClick = { onValueChange(order) },
                    enabled = order != value
                ) {
                    Text(
                        text = label,
                        color = if (order == value) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun TransitionSetting(value: TransitionType, onValueChange: (TransitionType) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text("过渡效果", color = MaterialTheme.colorScheme.onSurface)
        Row(modifier = Modifier.padding(top = 8.dp)) {
            TransitionType.entries.forEach { type ->
                val label = when (type) {
                    TransitionType.FADE -> "淡入淡出"
                    TransitionType.KEN_BURNS -> "Ken Burns"
                    TransitionType.SLIDE -> "滑动"
                    TransitionType.CUT -> "硬切"
                }
                TextButton(
                    onClick = { onValueChange(type) },
                    enabled = type != value
                ) {
                    Text(
                        text = label,
                        color = if (type == value) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncFrequencySetting(value: String, onValueChange: (String) -> Unit) {
    val options = listOf("off" to "关闭", "15min" to "15 分钟", "30min" to "30 分钟", "1h" to "1 小时", "manual" to "仅手动")
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text("后台同步频率", color = MaterialTheme.colorScheme.onSurface)
        Row(modifier = Modifier.padding(top = 8.dp)) {
            options.forEach { (key, label) ->
                TextButton(
                    onClick = { onValueChange(key) },
                    enabled = key != value
                ) {
                    Text(
                        text = label,
                        color = if (key == value) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun CacheLimitSetting(value: Int, onValueChange: (Int) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = "缓存上限：$value% 可用空间",
            color = MaterialTheme.colorScheme.onSurface
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 20f..95f,
            steps = 14,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ---------------- PIN 安全区块 ----------------

@Composable
private fun PinSection(
    settingsRepository: SettingsRepository,
    secureKeyStore: SecureKeyStore
) {
    val pinEnabled by settingsRepository.pinEnabled.collectAsState()
    val pinActive = pinEnabled && secureKeyStore.hasPin()

    var showSetup by remember { mutableStateOf(false) }
    var showDisable by remember { mutableStateOf(false) }
    var showChange by remember { mutableStateOf(false) }
    var showQuestion by remember { mutableStateOf(false) }

    ToggleSetting(
        title = "App 启动 PIN 锁",
        checked = pinActive,
        onCheckedChange = { enable ->
            if (enable) showSetup = true else showDisable = true
        }
    )

    if (pinActive) {
        SettingLink("修改 PIN") { showChange = true }
        SettingLink("安全问题") { showQuestion = true }
    }

    if (showSetup) {
        PinSetupWizard(
            secureKeyStore = secureKeyStore,
            onDone = { settingsRepository.setPinEnabled(true) },
            onDismiss = { showSetup = false }
        )
    }
    if (showDisable) {
        PinDisableDialog(
            secureKeyStore = secureKeyStore,
            onDone = { settingsRepository.setPinEnabled(false) },
            onDismiss = { showDisable = false }
        )
    }
    if (showChange) {
        PinChangeDialog(
            secureKeyStore = secureKeyStore,
            onDismiss = { showChange = false }
        )
    }
    if (showQuestion) {
        SecurityQuestionDialog(
            secureKeyStore = secureKeyStore,
            onDismiss = { showQuestion = false }
        )
    }
}

@Composable
private fun PinSetupWizard(
    secureKeyStore: SecureKeyStore,
    onDone: () -> Unit,
    onDismiss: () -> Unit
) {
    var step by remember { mutableStateOf(0) } // 0=新PIN, 1=确认, 2=安全问题
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (step) {
                    0 -> "设置新 PIN"
                    1 -> "确认 PIN"
                    else -> "设置安全问题"
                }
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when (step) {
                    0 -> {
                        PinDots(length = 4, value = pin.length)
                        Spacer(modifier = Modifier.height(12.dp))
                        PinPad(value = pin, onValueChange = { pin = it })
                    }
                    1 -> {
                        PinDots(length = 4, value = confirm.length)
                        Spacer(modifier = Modifier.height(12.dp))
                        PinPad(value = confirm, onValueChange = { confirm = it; error = false })
                        if (error) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "两次输入不一致，请重新设置",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    else -> {
                        OutlinedTextField(
                            value = question,
                            onValueChange = { question = it },
                            label = { Text("安全问题（自定义）") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = answer,
                            onValueChange = { answer = it },
                            label = { Text("答案") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            when (step) {
                0 -> TextButton(
                    onClick = { step = 1 },
                    enabled = pin.length == 4
                ) { Text("下一步") }
                1 -> TextButton(
                    onClick = {
                        if (confirm == pin) {
                            error = false
                            step = 2
                        } else {
                            error = true
                            pin = ""
                            confirm = ""
                            step = 0
                        }
                    },
                    enabled = confirm.length == 4
                ) { Text("下一步") }
                else -> TextButton(
                    onClick = {
                        secureKeyStore.savePin(pin)
                        secureKeyStore.saveSecurityQuestion(question, answer)
                        onDone()
                        onDismiss()
                    },
                    enabled = question.isNotBlank() && answer.isNotBlank()
                ) { Text("完成") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun PinDisableDialog(
    secureKeyStore: SecureKeyStore,
    onDone: () -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("关闭 PIN") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "请输入当前 PIN",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                PinDots(length = 4, value = pin.length)
                Spacer(modifier = Modifier.height(12.dp))
                PinPad(value = pin, onValueChange = { pin = it; error = false })
                if (error) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "PIN 错误",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (secureKeyStore.verifyPin(pin)) {
                        secureKeyStore.clearPin()
                        secureKeyStore.clearSecurityQuestion()
                        onDone()
                        onDismiss()
                    } else {
                        error = true
                        pin = ""
                    }
                },
                enabled = pin.length == 4
            ) { Text("关闭") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun PinChangeDialog(
    secureKeyStore: SecureKeyStore,
    onDismiss: () -> Unit
) {
    var step by remember { mutableStateOf(0) } // 0=旧PIN, 1=新PIN, 2=确认
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (step) {
                    0 -> "验证旧 PIN"
                    1 -> "设置新 PIN"
                    else -> "确认新 PIN"
                }
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val current = when (step) {
                    0 -> oldPin
                    1 -> newPin
                    else -> confirm
                }
                PinDots(length = 4, value = current.length)
                Spacer(modifier = Modifier.height(12.dp))
                PinPad(
                    value = current,
                    onValueChange = { v ->
                        when (step) {
                            0 -> oldPin = v
                            1 -> newPin = v
                            else -> confirm = v
                        }
                        error = false
                    }
                )
                if (error) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (step == 0) "PIN 错误" else "两次输入不一致",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        },
        confirmButton = {
            when (step) {
                0 -> TextButton(
                    onClick = {
                        if (secureKeyStore.verifyPin(oldPin)) {
                            error = false
                            step = 1
                        } else {
                            error = true
                            oldPin = ""
                        }
                    },
                    enabled = oldPin.length == 4
                ) { Text("下一步") }
                1 -> TextButton(
                    onClick = { step = 2 },
                    enabled = newPin.length == 4
                ) { Text("下一步") }
                else -> TextButton(
                    onClick = {
                        if (confirm == newPin) {
                            secureKeyStore.savePin(newPin)
                            onDismiss()
                        } else {
                            error = true
                            newPin = ""
                            confirm = ""
                            step = 1
                        }
                    },
                    enabled = confirm.length == 4
                ) { Text("完成") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun SecurityQuestionDialog(
    secureKeyStore: SecureKeyStore,
    onDismiss: () -> Unit
) {
    var verified by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var question by remember { mutableStateOf(secureKeyStore.getSecurityQuestion() ?: "") }
    var answer by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (verified) "安全问题" else "验证 PIN") },
        text = {
            if (verified) {
                Column {
                    OutlinedTextField(
                        value = question,
                        onValueChange = { question = it },
                        label = { Text("安全问题（自定义）") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = answer,
                        onValueChange = { answer = it },
                        label = { Text("新答案") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "请输入当前 PIN",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    PinDots(length = 4, value = pin.length)
                    Spacer(modifier = Modifier.height(12.dp))
                    PinPad(value = pin, onValueChange = { pin = it; error = false })
                    if (error) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "PIN 错误",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (verified) {
                TextButton(
                    onClick = {
                        secureKeyStore.saveSecurityQuestion(question, answer)
                        onDismiss()
                    },
                    enabled = question.isNotBlank() && answer.isNotBlank()
                ) { Text("保存") }
            } else {
                TextButton(
                    onClick = {
                        if (secureKeyStore.verifyPin(pin)) {
                            verified = true
                            error = false
                        } else {
                            error = true
                            pin = ""
                        }
                    },
                    enabled = pin.length == 4
                ) { Text("验证") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
