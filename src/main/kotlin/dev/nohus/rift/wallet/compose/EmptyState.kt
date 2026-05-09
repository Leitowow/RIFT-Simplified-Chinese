package dev.nohus.rift.wallet.compose

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.wallet.WalletViewModel

@Composable
fun EmptyState(state: WalletViewModel.UiState) {
    val isFiltering = state.filters.direction != null ||
        state.filters.walletTypes.isNotEmpty() ||
        state.filters.referenceTypes.isNotEmpty() ||
        state.filters.search != null
    val text = if (isFiltering) "All transactions filtered out" else "No transactions"
    Text(
        text = text,
        style = RiftTheme.typography.displaySecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.large),
    )
}
