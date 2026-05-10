package dev.nohus.rift.wallet.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.AsyncCorporationLogo
import dev.nohus.rift.compose.LoadingSpinnerAmbient
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.VerticalGrid
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.dynamicportraits.DynamicCharacterPortraitParallax
import dev.nohus.rift.utils.formatNumber
import dev.nohus.rift.utils.multiplyBrightness
import dev.nohus.rift.utils.withColor
import dev.nohus.rift.wallet.WalletRepository

@Composable
fun WalletLoadingProgress(
    loading: WalletRepository.LoadingState,
    stage: WalletRepository.LoadingStage,
) {
    ScrollbarColumn(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(Spacing.large),
    ) {
        LoadingSpinnerAmbient()
        Spacer(Modifier.height(Spacing.medium))
        when (stage) {
            WalletRepository.LoadingStage.LoadingJournal -> {
                Text(
                    text = "正在加载流水…",
                    style = RiftTheme.typography.headlinePrimary,
                )
            }

            WalletRepository.LoadingStage.LoadingDatabase -> {
                Text(
                    text = "正在处理流水…",
                    style = RiftTheme.typography.headlinePrimary,
                )
            }

            WalletRepository.LoadingStage.LoadingTypeDetails -> {
                Text(
                    text = "正在加载流水明细…",
                    style = RiftTheme.typography.headlinePrimary,
                )
            }

            WalletRepository.LoadingStage.LoadingDivisionNames -> {
                Text(
                    text = "正在加载军团分账名称…",
                    style = RiftTheme.typography.headlinePrimary,
                )
            }

            WalletRepository.LoadingStage.LoadingBalances -> {
                Text(
                    text = "正在加载余额…",
                    style = RiftTheme.typography.headlinePrimary,
                )
            }
        }

        Spacer(Modifier.height(Spacing.large))

        AnimatedContent(stage) { stage ->
            when (stage) {
                WalletRepository.LoadingStage.LoadingJournal,
                WalletRepository.LoadingStage.LoadingDatabase,
                -> {
                    VerticalGrid(
                        minColumnWidth = 300.dp,
                        horizontalSpacing = Spacing.medium,
                        verticalSpacing = Spacing.medium,
                    ) {
                        loading.characters.forEach { character ->
                            LoadingBox {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    DynamicCharacterPortraitParallax(
                                        characterId = character.characterId,
                                        size = 48.dp,
                                        enterTimestamp = null,
                                        pointerInteractionStateHolder = null,
                                    )
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(Spacing.small),
                                    ) {
                                        Text(
                                            text = character.name,
                                            style = RiftTheme.typography.bodyPrimary,
                                        )
                                        Divider(color = Color.White.copy(alpha = 0.15f))
                                        when (stage) {
                                            WalletRepository.LoadingStage.LoadingJournal, WalletRepository.LoadingStage.LoadingDatabase -> {
                                                val text = if (character.isJournalLoaded) {
                                                    buildAnnotatedString {
                                                        withColor(RiftTheme.colors.textSecondary) {
                                                            appendLine("已加载 ${formatNumber(character.loadedJournalItems)} 条钱包流水")
                                                            appendLine("已加载 ${formatNumber(character.loadedTransactions)} 条市场交易")
                                                        }
                                                    }
                                                } else {
                                                    buildAnnotatedString {
                                                        appendLine("${formatNumber(character.loadedJournalItems)} 条钱包流水，加载中…")
                                                        appendLine("${formatNumber(character.loadedTransactions)} 条市场交易，加载中…")
                                                    }
                                                }.trim() as AnnotatedString
                                                Text(
                                                    text = text,
                                                    style = RiftTheme.typography.bodyPrimary,
                                                )
                                            }
                                            WalletRepository.LoadingStage.LoadingTypeDetails -> {}
                                            WalletRepository.LoadingStage.LoadingDivisionNames -> {}
                                            WalletRepository.LoadingStage.LoadingBalances -> {}
                                        }
                                    }
                                }
                            }
                        }
                        loading.corporationDivisions.forEach { corpDivision ->
                            LoadingBox {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    AsyncCorporationLogo(
                                        corporationId = corpDivision.corporationId,
                                        size = 64,
                                        modifier = Modifier.size(48.dp),
                                    )
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(Spacing.small),
                                    ) {
                                        Text(
                                            text = corpDivision.name,
                                            style = RiftTheme.typography.bodyPrimary,
                                        )
                                        Divider(color = Color.White.copy(alpha = 0.15f))
                                        when (stage) {
                                            WalletRepository.LoadingStage.LoadingJournal, WalletRepository.LoadingStage.LoadingDatabase -> {
                                                val text = if (corpDivision.isJournalLoaded) {
                                                    buildAnnotatedString {
                                                        withColor(RiftTheme.colors.textSecondary) {
                                                            appendLine("已加载 ${formatNumber(corpDivision.loadedJournalItems)} 条钱包流水")
                                                            appendLine("已加载 ${formatNumber(corpDivision.loadedTransactions)} 条市场交易")
                                                        }
                                                    }
                                                } else {
                                                    buildAnnotatedString {
                                                        appendLine("${formatNumber(corpDivision.loadedJournalItems)} 条钱包流水，加载中…")
                                                        appendLine("${formatNumber(corpDivision.loadedTransactions)} 条市场交易，加载中…")
                                                    }
                                                }.trim() as AnnotatedString
                                                Text(
                                                    text = text,
                                                    style = RiftTheme.typography.bodyPrimary,
                                                )
                                            }
                                            WalletRepository.LoadingStage.LoadingTypeDetails -> {}
                                            WalletRepository.LoadingStage.LoadingDivisionNames -> {}
                                            WalletRepository.LoadingStage.LoadingBalances -> {}
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                WalletRepository.LoadingStage.LoadingTypeDetails -> {
                    Text(
                        text = buildAnnotatedString {
                            appendLine("${formatNumber(loading.totalJournalItems)} 条流水涉及")
                            append("${loading.typeDetails.structureIds} 个建筑与空间站")
                            if (loading.typeDetails.isStructuresLoaded) {
                                appendLine(", done!")
                            } else {
                                appendLine("…")
                            }
                            append("${loading.typeDetails.characterIds} 个角色")
                            if (loading.typeDetails.isCharactersLoaded) {
                                appendLine(", done!")
                            } else {
                                appendLine("…")
                            }
                            append("${loading.typeDetails.groupIds} 个军团与联盟")
                            if (loading.typeDetails.isGroupsLoaded) {
                                appendLine(", done!")
                            } else {
                                appendLine("…")
                            }
                            appendLine("${loading.typeDetails.systemIds} 个星系…")
                            appendLine("${loading.typeDetails.typeIds} 个类型…")
                        }.trim() as AnnotatedString,
                        style = RiftTheme.typography.headerSecondary,
                    )
                }

                WalletRepository.LoadingStage.LoadingDivisionNames -> {}
                WalletRepository.LoadingStage.LoadingBalances -> {}
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LoadingBox(
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(IntrinsicSize.Max)
            .height(IntrinsicSize.Max),
    ) {
        val shape = CutCornerShape(bottomEnd = 8.dp)
        val color = Color(0xFF6E966B)
        val alpha = 0.1f
        Box(
            modifier = Modifier
                .alpha(alpha)
                .clip(shape)
                .background(color.multiplyBrightness(2f)),
        ) {
            Box(
                modifier = Modifier
                    .padding(end = 2.dp)
                    .clip(shape)
                    .background(color),
            ) {
                Spacer(Modifier.fillMaxSize())
            }
        }
        Box(
            modifier = Modifier
                .padding(end = 2.dp)
                .padding(Spacing.small),
        ) {
            content()
        }
    }
}
