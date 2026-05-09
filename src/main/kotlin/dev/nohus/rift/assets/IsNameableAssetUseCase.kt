package dev.nohus.rift.assets

import dev.nohus.rift.network.esi.models.Asset
import dev.nohus.rift.repositories.TypesRepository
import org.koin.core.annotation.Single

@Single
class IsNameableAssetUseCase(
    private val typesRepository: TypesRepository,
) {

    companion object {
        private const val GROUP_ID_CARGO_CONTAINER = 12
        private const val GROUP_ID_SECURE_CARGO_CONTAINER = 340
        private const val GROUP_ID_AUDIT_LOG_SECURE_CONTAINER = 448
        private const val GROUP_ID_FREIGHT_CONTAINER = 649
        private const val GROUP_ID_BIOMASS = 14
        private const val GROUP_ID_WRECK = 186
        private val nameableGroups = listOf(
            GROUP_ID_WRECK,
            GROUP_ID_CARGO_CONTAINER,
            GROUP_ID_SECURE_CARGO_CONTAINER,
            GROUP_ID_AUDIT_LOG_SECURE_CONTAINER,
            GROUP_ID_FREIGHT_CONTAINER,
            GROUP_ID_BIOMASS,
        )

        private const val CATEGORY_ID_SHIP = 6
        private const val CATEGORY_ID_DEPLOYABLE = 22
        private const val CATEGORY_ID_STRUCTURE = 65
        private val nameableCategories = listOf(
            CATEGORY_ID_SHIP,
            CATEGORY_ID_DEPLOYABLE,
            CATEGORY_ID_STRUCTURE,
        )
    }

    operator fun invoke(asset: Asset): Boolean {
        if (!asset.isSingleton) return false
        val type = typesRepository.getType(asset.typeId) ?: return false
        return type.categoryId in nameableCategories || type.groupId in nameableGroups
    }
}
