package io.github.watervxv.mtpadphotos.ui.rom

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.watervxv.mtpadphotos.util.RomGuide

/**
 * 开机自启与省电白名单引导弹窗（M3-2）。
 * 主界面提示条与设置页共用。
 */
@Composable
fun RomGuideDialog(guide: RomGuide, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("开机自启与省电设置") },
        text = { RomGuideContent(guide) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("知道了") }
        }
    )
}

@Composable
fun RomGuideContent(guide: RomGuide) {
    Column {
        Text(
            text = "当前设备：${guide.family.displayName}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        GuideBlock(title = "1. 开启自启动", steps = guide.autostartSteps)
        Spacer(modifier = Modifier.height(12.dp))
        GuideBlock(title = "2. 省电白名单", steps = guide.batterySteps)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = guide.note,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GuideBlock(title: String, steps: List<String>) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        steps.forEach { step ->
            Text(
                text = "• $step",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}
