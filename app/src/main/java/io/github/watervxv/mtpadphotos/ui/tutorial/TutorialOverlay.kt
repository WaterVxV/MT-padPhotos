package io.github.watervxv.mtpadphotos.ui.tutorial

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private data class TutorialStep(
    val title: String,
    val message: String,
    val highlightXFraction: Float,
    val highlightYFraction: Float,
    val highlightWidthDp: Dp,
    val highlightHeightDp: Dp,
    val bubbleAlignment: Alignment
)

private val steps = listOf(
    TutorialStep(
        title = "从这里开始",
        message = "点击任意相册，进入该相册的全屏轮播播放。顶部三个固定相册（全部项目 / 照片 / 视频）始终可用。",
        highlightXFraction = 0.12f,
        highlightYFraction = 0.35f,
        highlightWidthDp = 160.dp,
        highlightHeightDp = 120.dp,
        bubbleAlignment = Alignment.Center
    ),
    TutorialStep(
        title = "轻触即唤出操作",
        message = "单击屏幕任意位置唤出顶部信息与按钮；再单击空白处即可隐藏，回归纯净画面。顶部显示当前照片的拍摄时间与地点。",
        highlightXFraction = 0.5f,
        highlightYFraction = 0.12f,
        highlightWidthDp = 300.dp,
        highlightHeightDp = 60.dp,
        bubbleAlignment = Alignment.TopCenter
    ),
    TutorialStep(
        title = "切换播放顺序",
        message = "在这里循环切换「正序 / 倒序 / 随机」三种顺序，按钮旁小字显示当前模式。系统会记住每个相册你停在哪一张，下次自动续播。",
        highlightXFraction = 0.12f,
        highlightYFraction = 0.88f,
        highlightWidthDp = 120.dp,
        highlightHeightDp = 48.dp,
        bubbleAlignment = Alignment.BottomStart
    ),
    TutorialStep(
        title = "手动跳转与查看详情",
        message = "想跳到特定照片？点这里打开缩略图列表，点击任意项直接跳转。点右上角可查看拍摄设备、尺寸等详细信息。",
        highlightXFraction = 0.88f,
        highlightYFraction = 0.88f,
        highlightWidthDp = 120.dp,
        highlightHeightDp = 48.dp,
        bubbleAlignment = Alignment.BottomEnd
    )
)

@Composable
fun TutorialOverlay(
    onFinish: () -> Unit,
    onSkip: () -> Unit
) {
    var currentStepIndex by remember { mutableStateOf(0) }
    val step = steps[currentStepIndex]
    val isLast = currentStepIndex == steps.size - 1

    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }

    val highlightWidthPx = with(density) { step.highlightWidthDp.toPx() }
    val highlightHeightPx = with(density) { step.highlightHeightDp.toPx() }
    val highlightLeft = screenWidthPx * step.highlightXFraction - highlightWidthPx / 2f
    val highlightTop = screenHeightPx * step.highlightYFraction - highlightHeightPx / 2f

    Box(modifier = Modifier.fillMaxSize()) {
        // 遮罩 + 高亮镂空
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color.Black.copy(alpha = 0.75f))
            drawClearHole(
                left = highlightLeft.coerceAtLeast(0f),
                top = highlightTop.coerceAtLeast(0f),
                width = highlightWidthPx,
                height = highlightHeightPx
            )
        }

        // 高亮边框
        Box(
            modifier = Modifier
                .offset(
                    x = with(density) { highlightLeft.coerceAtLeast(0f).toDp() },
                    y = with(density) { highlightTop.coerceAtLeast(0f).toDp() }
                )
                .size(step.highlightWidthDp, step.highlightHeightDp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Transparent)
        )

        // 提示气泡
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = step.bubbleAlignment
        ) {
            TutorialBubble(
                step = step,
                stepNumber = currentStepIndex + 1,
                totalSteps = steps.size,
                isLast = isLast,
                onNext = {
                    if (isLast) onFinish()
                    else currentStepIndex++
                },
                onSkip = onSkip
            )
        }
    }
}

private fun DrawScope.drawClearHole(left: Float, top: Float, width: Float, height: Float) {
    drawRoundRect(
        color = Color.Transparent,
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = CornerRadius(24f, 24f),
        blendMode = BlendMode.Clear
    )
}

@Composable
private fun TutorialBubble(
    step: TutorialStep,
    stepNumber: Int,
    totalSteps: Int,
    isLast: Boolean,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(32.dp)
            .width(360.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp)
    ) {
        Text(
            text = step.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = step.message,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onSkip) {
                Text("跳过")
            }
            Text(
                text = "$stepNumber / $totalSteps",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onNext) {
                Text(if (isLast) "开始使用" else "下一步")
            }
        }
    }
}
