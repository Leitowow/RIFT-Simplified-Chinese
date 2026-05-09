package dev.nohus.rift.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import dev.nohus.rift.compose.theme.EveColors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing

@Composable
fun KeyName(name: String, key: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(Spacing.large),
    ) {
        Text(
            text = name,
            style = RiftTheme.typography.bodyPrimary,
        )
        Box(
            modifier = Modifier
                .padding(start = Spacing.medium)
                .background(EveColors.coalBlack)
                .padding(Spacing.small),
        ) {
            Text(
                text = key,
                style = RiftTheme.typography.bodySecondary.copy(fontWeight = FontWeight.Bold),
            )
        }
    }
}
