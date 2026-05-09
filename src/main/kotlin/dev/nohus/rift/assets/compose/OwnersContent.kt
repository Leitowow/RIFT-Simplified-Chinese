package dev.nohus.rift.assets.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.nohus.rift.assets.AssetsFilters
import dev.nohus.rift.assets.AssetsRepository.AssetOwner
import dev.nohus.rift.assets.AssetsViewModel.LoadedData
import dev.nohus.rift.assets.AssetsViewModel.UiState
import dev.nohus.rift.assets.OwnerType
import dev.nohus.rift.compose.AsyncCorporationLogo
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.VerticalGrid
import dev.nohus.rift.compose.rememberPointerInteractionStateHolder
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.dynamicportraits.DynamicCharacterPortraitParallax
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.window_assets
import dev.nohus.rift.utils.plural
import dev.nohus.rift.utils.toggle
import org.jetbrains.compose.resources.painterResource
import java.time.Duration
import java.time.Instant

@Composable
fun OwnersContent(
    state: UiState,
    data: LoadedData,
    onFiltersUpdate: (AssetsFilters) -> Unit,
) {
    Column {
        val text = if (data.owners.any { it is AssetOwner.Corporation }) {
            "Choose owners to filter assets"
        } else {
            "Choose characters to filter assets"
        }
        Text(
            text = text,
            style = RiftTheme.typography.bodySecondary,
            modifier = Modifier.padding(bottom = Spacing.medium),
        )

        ScrollbarColumn(
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            contentPadding = PaddingValues(end = Spacing.medium),
        ) {
            fun updateFilters(new: List<OwnerType>) {
                onFiltersUpdate(state.filters.copy(ownerTypes = new))
            }

            val filters = state.filters.ownerTypes
            val balances = data.balances.associateBy { it.owner }

            VerticalGrid(
                minColumnWidth = 200.dp,
                horizontalSpacing = Spacing.medium,
                verticalSpacing = Spacing.medium,
            ) {
                val totalBalance = balances
                    .filter { it.key is AssetOwner.Character }
                    .values
                    .sumOf { it.count }
                OwnerCard(
                    icon = {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(48.dp).background(Color.Black.copy(alpha = 0.2f)),
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.window_assets),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                            )
                        }
                    },
                    name = "All character assets",
                    isSelected = OwnerType.Character in filters,
                    onClick = {
                        val updated = filters
                            .toggle(OwnerType.Character)
                            .filter { it !is OwnerType.SpecificCharacter }
                        updateFilters(updated)
                    },
                    text = formatBalance(totalBalance),
                )

                val now = remember(data.owners) { Instant.now() }
                data.owners.filterIsInstance<AssetOwner.Character>()
                    .forEachIndexed { index, owner ->
                        val isSelected = OwnerType.SpecificCharacter(owner.character.characterId) in filters ||
                            OwnerType.Character in filters
                        val pointerInteractionStateHolder = rememberPointerInteractionStateHolder()
                        OwnerCard(
                            icon = {
                                DynamicCharacterPortraitParallax(
                                    characterId = owner.character.characterId,
                                    size = 48.dp,
                                    enterTimestamp = now + (Duration.ofMillis(100L + index * 100)),
                                    pointerInteractionStateHolder = pointerInteractionStateHolder,
                                )
                            },
                            name = owner.character.info?.name ?: "",
                            isSelected = isSelected,
                            onClick = {
                                val updated = filters
                                    .toggle(OwnerType.SpecificCharacter(owner.character.characterId))
                                    .filter { it !is OwnerType.Character }
                                updateFilters(updated)
                            },
                            text = formatBalance(balances[owner]?.count),
                            pointerInteractionStateHolder = pointerInteractionStateHolder,
                        )
                    }
            }

            val corporations = data.owners.filterIsInstance<AssetOwner.Corporation>()
            if (corporations.isNotEmpty()) {
                Divider(color = RiftTheme.colors.divider)
                VerticalGrid(
                    minColumnWidth = 200.dp,
                    horizontalSpacing = Spacing.medium,
                    verticalSpacing = Spacing.medium,
                ) {
                    corporations.forEach { corporation ->
                        OwnerCard(
                            icon = {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(48.dp).background(Color.Black.copy(alpha = 0.2f)),
                                ) {
                                    AsyncCorporationLogo(
                                        corporationId = corporation.corporationId,
                                        size = 64,
                                        modifier = Modifier.size(48.dp),
                                    )
                                }
                            },
                            name = corporation.corporationName,
                            isSelected = OwnerType.SpecificCorporation(corporation.corporationId) in filters,
                            onClick = {
                                val updated = filters
                                    .toggle(OwnerType.SpecificCorporation(corporation.corporationId))
                                updateFilters(updated)
                            },
                            text = formatBalance(balances[corporation]?.count),
                        )
                    }
                }
            }
        }
    }
}

private fun formatBalance(count: Int?): String {
    if (count == null) return "0 assets"
    return "$count asset${count.plural}"
}
