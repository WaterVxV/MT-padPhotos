package io.github.watervxv.mtpadphotos.ui.pin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** 4 个圆点进度指示（已输入几个数字）。 */
@Composable
fun PinDots(
    length: Int,
    value: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        repeat(length) { i ->
            Text(
                text = if (i < value) "●" else "○",
                style = MaterialTheme.typography.headlineMedium,
                color = if (i < value) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

/** 数字键盘（0-9 / 删除 / 清空），受控组件。 */
@Composable
fun PinPad(
    value: String,
    onValueChange: (String) -> Unit,
    pinLength: Int = 4,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        PinPadRow(listOf("1", "2", "3"), value, onValueChange, pinLength)
        PinPadRow(listOf("4", "5", "6"), value, onValueChange, pinLength)
        PinPadRow(listOf("7", "8", "9"), value, onValueChange, pinLength)
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            PinKey("删除", small = true) { if (value.isNotEmpty()) onValueChange(value.dropLast(1)) }
            PinKey("0") { if (value.length < pinLength) onValueChange(value + "0") }
            PinKey("清空", small = true) { onValueChange("") }
        }
    }
}

@Composable
private fun PinPadRow(
    keys: List<String>,
    value: String,
    onValueChange: (String) -> Unit,
    pinLength: Int
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth()
    ) {
        keys.forEach { k ->
            PinKey(k) { if (value.length < pinLength) onValueChange(value + k) }
        }
    }
}

@Composable
private fun PinKey(
    label: String,
    small: Boolean = false,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick, modifier = Modifier.padding(4.dp)) {
        Text(
            text = label,
            style = if (small) MaterialTheme.typography.titleMedium
            else MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
    }
}
