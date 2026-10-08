package io.github.watervxv.mtpadphotos.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.github.watervxv.mtpadphotos.data.remote.AuthManager
import io.github.watervxv.mtpadphotos.data.repo.SettingsRepository
import io.github.watervxv.mtpadphotos.util.RomDetector
import io.github.watervxv.mtpadphotos.util.RomFamily
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    authManager: AuthManager,
    settingsRepository: SettingsRepository,
    onComplete: () -> Unit
) {
    var serverUrl by remember { mutableStateOf("http://192.168.1.x:8063") }
    var apiKey by remember { mutableStateOf("sk_live_") }
    var apiKeyVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var isVerifying by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val romHint = remember {
        when (RomDetector.detect()) {
            RomFamily.XIAOMI -> "提示（小米系统）：请在「设置 → 应用设置 → 自启动」开启自启，并在「省电与电池 → 应用智能省电」中设为「无限制」，否则重启后无法自动进入相册。"
            RomFamily.OPPO -> "提示（OPPO/Realme 系统）：请在「设置 → 应用管理 → 自启动管理」开启自启，并在「电池」中关闭「应用冻结」。"
            RomFamily.VIVO -> "提示（vivo 系统）：请在「i管家 → 应用管理 → 自启动管理」开启自启，并在「电池」中允许后台耗电。"
            RomFamily.SAMSUNG -> "提示（三星系统）：请在「设置 → 应用程序 → 自动运行」开启自动运行，并在「电池」中关闭休眠。"
            RomFamily.OTHER -> "提示：首次使用请前往系统设置，为本应用开启「自启动」与「省电白名单」，以保证相册常驻运行。"
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "MT轮播相册",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "首次使用，请配置 NAS 与偏好",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text("NAS 地址") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = onboardingFieldColors()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                        Icon(
                            imageVector = if (apiKeyVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (apiKeyVisible) "隐藏 API Key" else "显示 API Key",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = onboardingFieldColors()
            )
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = romHint,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(24.dp))

            errorMessage?.let { msg ->
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                enabled = !isVerifying,
                onClick = {
                    if (isVerifying) {
                        return@Button
                    }
                    val normalizedUrl = serverUrl.trim().trimEnd('/')
                        .let { if (it.startsWith("http://") || it.startsWith("https://")) it else "http://$it" }
                    val trimmedKey = apiKey.trim()
                    when {
                        normalizedUrl.length < 12 -> {
                            errorMessage = "NAS 地址格式不正确"
                        }
                        trimmedKey.isBlank() -> {
                            errorMessage = "请输入 API Key"
                        }
                        else -> {
                            isVerifying = true
                            errorMessage = null
                            scope.launch {
                                authManager.saveCredentials(normalizedUrl, trimmedKey)
                                authManager.refreshAuthCode().fold(
                                    onSuccess = {
                                        settingsRepository.completeOnboarding()
                                        onComplete()
                                    },
                                    onFailure = { e ->
                                        authManager.clearCredentials()
                                        isVerifying = false
                                        errorMessage = when (e) {
                                            is java.net.UnknownHostException -> "无法解析地址，请检查 NAS 地址"
                                            is java.net.ConnectException -> "无法连接 NAS，请检查地址与网络"
                                            is java.net.SocketTimeoutException -> "连接超时，请检查 NAS 地址与网络"
                                            else -> e.message?.takeIf { it.isNotBlank() } ?: "登录失败，请检查地址与 API Key"
                                        }
                                    }
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("进入主界面")
            }
        }
    }
}

@Composable
private fun onboardingFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
)
