package dev.nohus.rift.wallet.compose

import androidx.compose.runtime.Composable
import dev.nohus.rift.compose.theme.RiftTheme

@Composable
fun getAmountColor(amount: Double) = if (amount >= 0) RiftTheme.colors.successGreen else RiftTheme.colors.hotRed
