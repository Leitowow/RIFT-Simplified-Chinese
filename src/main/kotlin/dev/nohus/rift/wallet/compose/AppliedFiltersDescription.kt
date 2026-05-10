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
            WalletTab.Transactions -> append("显示 ")
            else -> append("基于 ")
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
                    TransferDirection.Income -> "收入"
                    TransferDirection.Expense -> "支出"
                    null -> "流水"
                },
            )

            else -> append("流水")
        }
        append(transactionsCount.plural)
        append("，来自 ")
        val walletsCount = journal?.map { it.wallet }?.distinct()?.size ?: 0
        withStyle(
            style = SpanStyle(color = RiftTheme.colors.textPrimary, fontWeight = FontWeight.Bold),
        ) {
            append("$walletsCount")
        }
        append(" 个钱包")

        if (tab == WalletTab.Transactions && state.filters.referenceTypes.isNotEmpty()) {
            append("，匹配 ")
            withStyle(
                style = SpanStyle(
                    color = RiftTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                ),
            ) {
                append(state.filters.referenceTypes.size.toString())
            }
            append(" 种类型")
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
