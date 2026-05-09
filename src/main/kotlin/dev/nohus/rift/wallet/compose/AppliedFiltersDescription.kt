package dev.nohus.rift.wallet.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.utils.formatNumber
import dev.nohus.rift.utils.plural
import dev.nohus.rift.wallet.TransferDirection
import dev.nohus.rift.wallet.WalletJournalItem
import dev.nohus.rift.wallet.WalletViewModel.UiState
import dev.nohus.rift.wallet.WalletViewModel.WalletTab

@Composable
fun AppliedFiltersDescription(
    state: UiState,
    journal: List<WalletJournalItem>?,
    tab: WalletTab,
    modifier: Modifier = Modifier,
) {
    val text = buildAnnotatedString {
        when (tab) {
            WalletTab.Transactions -> append("Showing ")
            else -> append("Based on ")
        }
        val transactionsCount = journal?.size ?: 0
        withStyle(
            style = SpanStyle(color = RiftTheme.colors.textPrimary, fontWeight = FontWeight.Bold),
        ) {
            append(formatNumber(transactionsCount))
        }
        append(" ")
        when (tab) {
            WalletTab.Transactions -> append(
                when (state.filters.direction) {
                    TransferDirection.Income -> "deposit"
                    TransferDirection.Expense -> "withdrawal"
                    null -> "transaction"
                },
            )

            else -> append("transaction")
        }
        append(transactionsCount.plural)
        append(" from ")
        val walletsCount = journal?.map { it.wallet }?.distinct()?.size ?: 0
        withStyle(
            style = SpanStyle(color = RiftTheme.colors.textPrimary, fontWeight = FontWeight.Bold),
        ) {
            append("$walletsCount")
        }
        append(" wallet${walletsCount.plural}")

        if (tab == WalletTab.Transactions && state.filters.referenceTypes.isNotEmpty()) {
            append(" matching ")
            withStyle(
                style = SpanStyle(
                    color = RiftTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                ),
            ) {
                append(state.filters.referenceTypes.size.toString())
            }
            append(" type${state.filters.referenceTypes.size.plural}")
        }
    }

    AnimatedContent(text) { text ->
        Text(
            text = text,
            style = RiftTheme.typography.bodySecondary,
            modifier = modifier,
        )
    }
}
