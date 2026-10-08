package io.github.watervxv.mtpadphotos.ui.pin

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.watervxv.mtpadphotos.data.local.prefs.SecureKeyStore
import kotlinx.coroutines.launch

private enum class GateStage { ANSWER, NEW_PIN, CONFIRM }

/** 冷启动 PIN 验证门。 */
@Composable
fun PinGateScreen(
    secureKeyStore: SecureKeyStore,
    onVerified: () -> Unit
) {
    var showForgot by remember { mutableStateOf(false) }
    if (showForgot) {
        ForgotPinFlow(
            secureKeyStore = secureKeyStore,
            onVerified = onVerified,
            onBack = { showForgot = false }
        )
    } else {
        PinEntry(
            secureKeyStore = secureKeyStore,
            onVerified = onVerified,
            onForgot = { showForgot = true }
        )
    }
}

@Composable
private fun PinEntry(
    secureKeyStore: SecureKeyStore,
    onVerified: () -> Unit,
    onForgot: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val shake = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(pin) {
        if (pin.length == 4) {
            if (secureKeyStore.verifyPin(pin)) {
                onVerified()
            } else {
                error = true
                pin = ""
                scope.launch {
                    repeat(3) {
                        shake.animateTo(-12f, tween(60))
                        shake.animateTo(12f, tween(60))
                    }
                    shake.snapTo(0f)
                }
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "输入 PIN",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier.graphicsLayer { translationX = shake.value }
                ) {
                    PinDots(length = 4, value = pin.length)
                }
                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.height(24.dp), contentAlignment = Alignment.Center) {
                    if (error) {
                        Text(
                            text = "PIN 错误，请重试",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                PinPad(
                    value = pin,
                    onValueChange = {
                        pin = it
                        error = false
                    }
                )
            }

            TextButton(
                onClick = onForgot,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            ) {
                Text("忘记密码？")
            }
        }
    }
}

@Composable
private fun ForgotPinFlow(
    secureKeyStore: SecureKeyStore,
    onVerified: () -> Unit,
    onBack: () -> Unit
) {
    var stage by remember { mutableStateOf(GateStage.ANSWER) }
    var answer by remember { mutableStateOf("") }
    var answerError by remember { mutableStateOf(false) }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var confirmError by remember { mutableStateOf(false) }

    val question = secureKeyStore.getSecurityQuestion().orEmpty()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = "找回 PIN",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.height(32.dp))

            when (stage) {
                GateStage.ANSWER -> {
                    Text(
                        text = "安全问题",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = question.ifBlank { "（未设置安全问题）" },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = answer,
                        onValueChange = { answer = it; answerError = false },
                        label = { Text("答案") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    if (answerError) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "答案错误，请重试",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            if (secureKeyStore.verifySecurityAnswer(answer)) {
                                answerError = false
                                stage = GateStage.NEW_PIN
                            } else {
                                answerError = true
                                answer = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = answer.isNotBlank()
                    ) {
                        Text("验证")
                    }
                }

                GateStage.NEW_PIN -> {
                    Text(
                        text = "设置新 PIN",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PinDots(length = 4, value = newPin.length)
                    Spacer(modifier = Modifier.height(16.dp))
                    PinPad(value = newPin, onValueChange = { newPin = it })
                    LaunchedEffect(newPin) {
                        if (newPin.length == 4) stage = GateStage.CONFIRM
                    }
                }

                GateStage.CONFIRM -> {
                    Text(
                        text = "确认新 PIN",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PinDots(length = 4, value = confirmPin.length)
                    Spacer(modifier = Modifier.height(16.dp))
                    PinPad(value = confirmPin, onValueChange = { confirmPin = it; confirmError = false })
                    if (confirmError) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "两次输入不一致",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    LaunchedEffect(confirmPin) {
                        if (confirmPin.length == 4) {
                            if (confirmPin == newPin) {
                                secureKeyStore.savePin(newPin)
                                onVerified()
                            } else {
                                confirmError = true
                                confirmPin = ""
                                newPin = ""
                                stage = GateStage.NEW_PIN
                            }
                        }
                    }
                }
            }
        }
    }
}
