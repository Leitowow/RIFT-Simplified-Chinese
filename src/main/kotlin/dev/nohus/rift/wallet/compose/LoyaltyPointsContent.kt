package dev.nohus.rift.wallet.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.AsyncCorporationLogo
import dev.nohus.rift.compose.AsyncTypeIcon
import dev.nohus.rift.compose.ClickableLocation
import dev.nohus.rift.compose.RiftSearchField
import dev.nohus.rift.compose.RiftToggleButton
import dev.nohus.rift.compose.ScrollbarLazyVerticalGrid
import dev.nohus.rift.compose.ToggleButtonType
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.dynamicportraits.DynamicCharacterPortraitParallax
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.evermarks
import dev.nohus.rift.repositories.StationsRepository
import dev.nohus.rift.utils.formatNumber
import dev.nohus.rift.utils.multiplyBrightness
import dev.nohus.rift.wallet.WalletFilters
import dev.nohus.rift.wallet.WalletViewModel.LoadedData
import dev.nohus.rift.wallet.WalletViewModel.UiState
import org.jetbrains.compose.resources.painterResource

@Composable
fun LoyaltyPointsContent(
    state: UiState,
    data: LoadedData,
    onFiltersUpdate: (WalletFilters) -> Unit,
) {
    Column {
        fun updateFilters(update: WalletFilters.() -> WalletFilters) {
            onFiltersUpdate(state.filters.update())
        }

        if (data.loyaltyPointBalances.isNotEmpty()) {
            var isShowingTotal by remember { mutableStateOf(false) }
            val text = if (isShowingTotal) {
                "Loyalty points totals from all your characters"
            } else {
                "Loyalty points and the closest loyalty point store for each character"
            }
            Text(
                text = text,
                style = RiftTheme.typography.bodySecondary,
                modifier = Modifier.padding(bottom = Spacing.medium),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                RiftToggleButton(
                    text = "By character",
                    isSelected = !isShowingTotal,
                    type = ToggleButtonType.Left,
                    onClick = { isShowingTotal = false },
                    modifier = Modifier.width(150.dp),
                )
                RiftToggleButton(
                    text = "Total",
                    isSelected = isShowingTotal,
                    type = ToggleButtonType.Right,
                    onClick = { isShowingTotal = true },
                    modifier = Modifier.width(150.dp),
                )

                Spacer(Modifier.weight(1f))

                RiftSearchField(
                    search = state.filters.search,
                    isCompact = false,
                    onSearchChange = { updateFilters { copy(search = it.takeIf { it.isNotBlank() }) } },
                )
            }

            val balances = data.loyaltyPointBalances
                .sortedByDescending { it.balances.sumOf { it.balance } }
                .filter {
                    if (isShowingTotal) it.characterId == null else it.characterId != null
                }
            ScrollbarLazyVerticalGrid(
                columns = GridCells.Adaptive(240.dp),
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                contentPadding = PaddingValues(end = Spacing.medium),
                modifier = Modifier.padding(top = Spacing.large),
            ) {
                balances.forEach { (characterId, balances) ->
                    if (characterId != null) {
                        item(key = characterId, span = { GridItemSpan(maxLineSpan) }) {
                            val characterName = data.characters.firstOrNull { it.id == characterId }
                                ?.name ?: balances.toString()
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                                modifier = Modifier.animateItem(),
                            ) {
                                DynamicCharacterPortraitParallax(
                                    characterId = characterId,
                                    size = 48.dp,
                                    enterTimestamp = null,
                                    pointerInteractionStateHolder = null,
                                )
                                Text(
                                    text = characterName,
                                    style = RiftTheme.typography.headlinePrimary.copy(fontWeight = FontWeight.Bold),
                                )
                            }
                        }
                    }

                    val filteredBalances = balances.filter {
                        val search = state.filters.search?.lowercase() ?: return@filter true
                        search in it.name.lowercase()
                    }
                    if (filteredBalances.isNotEmpty()) {
                        items(filteredBalances.sortedByDescending { it.balance }, key = { "$characterId-${it.corporationId}" }) {
                            LoyaltyPointsBox(
                                modifier = Modifier.animateItem(),
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        if (it.corporationId == 1000419) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.size(48.dp).background(Color.Black.copy(alpha = 0.2f)),
                                            ) {
                                                Image(
                                                    painter = painterResource(Res.drawable.evermarks),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(42.dp),
                                                )
                                            }
                                        } else {
                                            AsyncCorporationLogo(
                                                corporationId = it.corporationId,
                                                size = 64,
                                                modifier = Modifier.size(48.dp),
                                            )
                                        }
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(Spacing.small),
                                        ) {
                                            Text(
                                                text = it.name,
                                                style = RiftTheme.typography.bodyPrimary.copy(fontWeight = FontWeight.Bold),
                                            )
                                            Text(
                                                text = formatNumber(it.balance),
                                                style = RiftTheme.typography.bodyPrimary,
                                            )
                                        }
                                    }
                                    if (it.closestLoyaltyPointStore != null) {
                                        Divider(color = Color.White.copy(alpha = 0.15f))
                                        Store(it.closestLoyaltyPointStore)
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            if (balances.isEmpty()) {
                                Text(
                                    text = "No loyalty points",
                                    style = RiftTheme.typography.bodySecondary,
                                )
                            } else {
                                Text(
                                    text = "No loyalty points matching your search",
                                    style = RiftTheme.typography.bodySecondary,
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Text(
                text = "No characters with loyalty points",
                style = RiftTheme.typography.displaySecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.large),
            )
        }
    }
}

@Composable
private fun Store(station: StationsRepository.Station?) {
    station?.let { store ->
        ClickableLocation(
            systemId = store.systemId,
            locationId = store.id.toLong(),
            locationTypeId = store.typeId,
            locationName = store.name,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncTypeIcon(
                    typeId = store.typeId,
                    modifier = Modifier.size(32.dp),
                )
                Text(
                    text = store.name,
                    style = RiftTheme.typography.detailSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = true,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LoyaltyPointsBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
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
