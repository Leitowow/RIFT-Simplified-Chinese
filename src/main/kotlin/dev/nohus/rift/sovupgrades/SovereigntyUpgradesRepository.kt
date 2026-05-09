package dev.nohus.rift.sovupgrades

import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.SolarSystemsRepository.MapSolarSystem
import dev.nohus.rift.repositories.TypesRepository
import dev.nohus.rift.repositories.TypesRepository.Type
import dev.nohus.rift.settings.persistence.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single

@Single
class SovereigntyUpgradesRepository(
    private val settings: Settings,
    private val solarSystemsRepository: SolarSystemsRepository,
    private val typesRepository: TypesRepository,
) {

    private val _upgrades = MutableStateFlow(getUpgrades())
    val upgrades = _upgrades.asStateFlow()

    val upgradeGroupIds = listOf(
        4768, // Sovereignty Hub Anomaly Detection Upgrades
        4772, // Sovereignty Hub Service Infrastructure Upgrade
        4838, // Sovereignty Hub Colony Resources Management Upgrades
        4839, // Sovereignty Hub System Effect Generator Upgrades
    )

    val groupedUpgradeTypes = listOf(
        listOf(
            "Minor Threat Detection Array 1",
            "Minor Threat Detection Array 2",
            "Minor Threat Detection Array 3",
        ),
        listOf(
            "Major Threat Detection Array 1",
            "Major Threat Detection Array 2",
            "Major Threat Detection Array 3",
        ),
        listOf(
            "Exploration Detector 1",
            "Exploration Detector 2",
            "Exploration Detector 3",
        ),
        listOf(
            "Tritanium Prospecting Array 1",
            "Tritanium Prospecting Array 2",
            "Tritanium Prospecting Array 3",
        ),
        listOf(
            "Pyerite Prospecting Array 1",
            "Pyerite Prospecting Array 2",
            "Pyerite Prospecting Array 3",
        ),
        listOf(
            "Mexallon Prospecting Array 1",
            "Mexallon Prospecting Array 2",
            "Mexallon Prospecting Array 3",
        ),
        listOf(
            "Isogen Prospecting Array 1",
            "Isogen Prospecting Array 2",
            "Isogen Prospecting Array 3",
        ),
        listOf(
            "Nocxium Prospecting Array 1",
            "Nocxium Prospecting Array 2",
            "Nocxium Prospecting Array 3",
        ),
        listOf(
            "Zydrine Prospecting Array 1",
            "Zydrine Prospecting Array 2",
            "Zydrine Prospecting Array 3",
        ),
        listOf(
            "Megacyte Prospecting Array 1",
            "Megacyte Prospecting Array 2",
            "Megacyte Prospecting Array 3",
        ),
        listOf(
            "Electric Stability Generator",
        ),
        listOf(
            "Exotic Stability Generator",
        ),
        listOf(
            "Gamma Stability Generator",
        ),
        listOf(
            "Plasma Stability Generator",
        ),
        listOf(
            "Advanced Logistics Network",
        ),
        listOf(
            "Cynosural Navigation",
        ),
        listOf(
            "Cynosural Suppression",
        ),
        listOf(
            "Supercapital Construction Facilities",
        ),
        listOf(
            "Power Monitoring Division 1",
            "Power Monitoring Division 2",
            "Power Monitoring Division 3",
        ),
        listOf(
            "Workforce Mecha-Tooling 1",
            "Workforce Mecha-Tooling 2",
            "Workforce Mecha-Tooling 3",
        ),
    ).mapNotNull { group ->
        group.mapNotNull { name -> typesRepository.getType(name) }.takeIf { it.isNotEmpty() }
    }

    private fun getUpgrades(): Map<MapSolarSystem, List<Type>> {
        return settings.sovereigntyUpgrades
            .mapNotNull { (systemName, typeIds) ->
                val system = solarSystemsRepository.getSystem(systemName) ?: return@mapNotNull null
                val types = typeIds.mapNotNull { typeId -> typesRepository.getType(typeId) }
                if (types.isEmpty()) return@mapNotNull null
                system to types
            }
            .toMap()
    }

    fun setUpgrades(upgrades: Map<MapSolarSystem, List<Type>>) {
        _upgrades.value = upgrades
        settings.sovereigntyUpgrades = upgrades
            .map { (system, types) ->
                system.name to types.map { it.id }
            }.toMap()
    }

    fun setUpgrades(system: MapSolarSystem, upgrades: List<Type>) {
        setUpgrades(this.upgrades.value + (system to upgrades))
    }
}
