package dev.nohus.rift.sovupgrades

import dev.nohus.rift.repositories.GetSystemsInRangeUseCase
import dev.nohus.rift.repositories.SolarSystemsRepository.MapSolarSystem
import dev.nohus.rift.repositories.TypesRepository
import dev.nohus.rift.repositories.TypesRepository.Type
import dev.nohus.rift.settings.persistence.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Single

@Single
class MapSovereigntyUpgradesController(
    sovereigntyUpgradesRepository: SovereigntyUpgradesRepository,
    private val typesRepository: TypesRepository,
    private val getSystemsInRangeUseCase: GetSystemsInRangeUseCase,
    private val settings: Settings,
) {

    data class MapSovereigntyUpgradesState(
        val selectedTypes: List<Type> = emptyList(),
        val upgrades: Map<Int, List<SovereigntyUpgrade>> = emptyMap(),
    )

    data class SovereigntyUpgrade(
        val type: Type,
        /**
         * If true, this upgrade is not present in the system but is affecting it from afar
         */
        val isEffect: Boolean,
    )

    private val _selectedTypes = MutableStateFlow(emptyList<Type>())
    val selectedTypes get() = _selectedTypes.value
    val state = combine(
        _selectedTypes,
        sovereigntyUpgradesRepository.upgrades.map { getSovereigntyUpgrades(it) },
    ) { selectedTypes, upgrades ->
        MapSovereigntyUpgradesState(
            selectedTypes = selectedTypes,
            upgrades = upgrades,
        )
    }

    init {
        loadSettings()
    }

    private fun getSovereigntyUpgrades(upgrades: Map<MapSolarSystem, List<Type>>): Map<Int, List<SovereigntyUpgrade>> {
        val areaOfEffectUpgrades = listOf(
            "Exploration Detector 1",
            "Exploration Detector 2",
            "Exploration Detector 3",
        ).map { typesRepository.getType(it)!! }

        val systemsWithAllUpgrades = upgrades
            .map { (system, upgrades) ->
                system.id to upgrades.map { upgrade -> SovereigntyUpgrade(upgrade, false) }
            }
        val systemsWithAoeUpgrades = upgrades.map { (system, upgrades) ->
            system.id to upgrades.filter { it in areaOfEffectUpgrades }
        }.filter { (_, upgrades) -> upgrades.isNotEmpty() }
        val systemsWithAoeEffects = systemsWithAoeUpgrades.flatMap { (systemId, upgrades) ->
            val affectedSystems = getSystemsInRangeUseCase(systemId, 5) - systemId
            affectedSystems.map { systemId ->
                val effects = upgrades.map { upgrade -> SovereigntyUpgrade(upgrade, isEffect = true) }
                systemId to effects
            }
        }

        return (systemsWithAllUpgrades + systemsWithAoeEffects)
            .groupBy { it.first }
            .map { (systemId, upgrades) ->
                systemId to upgrades.flatMap { it.second }
            }
            .toMap()
    }

    private fun loadSettings() {
        val upgradeTypeIds = settings.selectedSovereigntyUpgradeTypes
        val upgradeTypes = upgradeTypeIds.mapNotNull { typeId ->
            typesRepository.getType(typeId)
        }
        _selectedTypes.update { upgradeTypes }
    }

    fun onSovereigntyUpgradeTypesUpdate(types: List<Type>) {
        _selectedTypes.update { types }
        settings.selectedSovereigntyUpgradeTypes = types.map { it.id }
    }
}
