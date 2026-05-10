package dev.nohus.rift.wallet.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.nohus.rift.compose.MulticolorIconType
import dev.nohus.rift.compose.RiftDropdown
import dev.nohus.rift.compose.RiftMulticolorIcon
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.wallet.WalletFilters
import dev.nohus.rift.wallet.WalletViewModel
import java.time.Duration

@Composable
fun TimespanDropdown(
    state: WalletViewModel.UiState,
    onFiltersUpdate: (WalletFilters) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = modifier,
    ) {
        RiftDropdown(
            items = state.availableTimestamps,
            selectedItem = state.filters.timeSpan,
            onItemSelected = { onFiltersUpdate(state.filters.copy(timeSpan = it)) },
            getItemName = {
                if (it < Duration.ofDays(2)) {
                    "最近 ${it.toHours()} 小时"
                } else {
                    "最近 ${it.toDays()} 天"
                }
            },
        )

        RiftTooltipArea(
            text = """
                ESI 仅提供最近 30 天的钱包流水，
                但 RIFT 会长期保存这些记录。
                
                这意味着如果你使用 RIFT 一年，
                你就能拥有过去一整年的完整钱包历史。
            """.trimIndent(),
        ) {
            RiftMulticolorIcon(
                type = MulticolorIconType.Info,
            )
        }
    }
}
