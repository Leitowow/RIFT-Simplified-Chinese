package dev.nohus.rift.assets

import dev.nohus.rift.assets.AssetsViewModel.SortType

data class AssetsFilters(
    val ownerTypes: List<OwnerType> = emptyList(),
    val search: String? = null,
    val sort: SortType = SortType.Distance,
)

sealed interface OwnerType {
    data object Character : OwnerType
    data class SpecificCharacter(val characterId: Int) : OwnerType
    data class SpecificCorporation(val corporationId: Int) : OwnerType
}
