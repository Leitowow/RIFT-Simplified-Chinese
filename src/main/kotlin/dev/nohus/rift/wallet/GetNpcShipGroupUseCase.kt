package dev.nohus.rift.wallet

import dev.nohus.rift.repositories.TypesRepository
import dev.nohus.rift.wallet.GetNpcShipGroupUseCase.NpcShipGroup.Battlecruiser
import dev.nohus.rift.wallet.GetNpcShipGroupUseCase.NpcShipGroup.Battleship
import dev.nohus.rift.wallet.GetNpcShipGroupUseCase.NpcShipGroup.CapitalIndustrialShip
import dev.nohus.rift.wallet.GetNpcShipGroupUseCase.NpcShipGroup.Carrier
import dev.nohus.rift.wallet.GetNpcShipGroupUseCase.NpcShipGroup.Cruiser
import dev.nohus.rift.wallet.GetNpcShipGroupUseCase.NpcShipGroup.Destroyer
import dev.nohus.rift.wallet.GetNpcShipGroupUseCase.NpcShipGroup.Dreadnought
import dev.nohus.rift.wallet.GetNpcShipGroupUseCase.NpcShipGroup.Drone
import dev.nohus.rift.wallet.GetNpcShipGroupUseCase.NpcShipGroup.FighterBomber
import dev.nohus.rift.wallet.GetNpcShipGroupUseCase.NpcShipGroup.FighterDrone
import dev.nohus.rift.wallet.GetNpcShipGroupUseCase.NpcShipGroup.ForceAuxiliary
import dev.nohus.rift.wallet.GetNpcShipGroupUseCase.NpcShipGroup.Freighter
import dev.nohus.rift.wallet.GetNpcShipGroupUseCase.NpcShipGroup.Frigate
import dev.nohus.rift.wallet.GetNpcShipGroupUseCase.NpcShipGroup.Hauler
import dev.nohus.rift.wallet.GetNpcShipGroupUseCase.NpcShipGroup.Industrial
import dev.nohus.rift.wallet.GetNpcShipGroupUseCase.NpcShipGroup.Shuttle
import dev.nohus.rift.wallet.GetNpcShipGroupUseCase.NpcShipGroup.Structure
import dev.nohus.rift.wallet.GetNpcShipGroupUseCase.NpcShipGroup.Supercarrier
import dev.nohus.rift.wallet.GetNpcShipGroupUseCase.NpcShipGroup.Titan
import dev.nohus.rift.wallet.GetNpcShipGroupUseCase.NpcShipGroup.TriglavianRogueDrone
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single

private val logger = KotlinLogging.logger {}

@Single
class GetNpcShipGroupUseCase {

    enum class NpcShipGroup(val displayName: String) {
        Drone("Drone"),
        TriglavianRogueDrone("Triglavian Rogue Drone"),
        FighterDrone("Fighter Drone"),
        FighterBomber("Fighter Bomber"),
        Shuttle("Shuttle"),
        Frigate("Frigate"),
        Destroyer("Destroyer"),
        Cruiser("Cruiser"),
        Battlecruiser("Battlecruiser"),
        Industrial("Industrial"),
        Structure("Structure"),
        Hauler("Hauler"),
        Battleship("Battleship"),
        Carrier("Carrier"),
        Freighter("Freighter"),
        ForceAuxiliary("Force Auxiliary"),
        Dreadnought("Dreadnought"),
        CapitalIndustrialShip("Capital Industrial Ship"),
        Supercarrier("Supercarrier"),
        Titan("Titan"),
    }

    operator fun invoke(type: TypesRepository.Type): NpcShipGroup? {
        val group = fromEntityOverviewShipGroupId(type.dogmas.entityOverviewShipGroupId)
            ?: fromGroupId(type.groupId)
        if (group == null) {
            logger.warn { "Unknown ship group: ${type.name}" }
        }
        return group
    }

    private fun fromEntityOverviewShipGroupId(id: Int?): NpcShipGroup? = when (id) {
        25 -> Frigate
        26 -> Cruiser
        27 -> Battleship
        28 -> Hauler
        30 -> Titan
        31 -> Shuttle
        419 -> Battlecruiser
        420 -> Destroyer
        485 -> Dreadnought
        513 -> Freighter
        547 -> Carrier
        549 -> FighterDrone
        659 -> Supercarrier
        883 -> CapitalIndustrialShip
        1023 -> FighterBomber
        else -> null
    }

    private fun fromGroupId(id: Int): NpcShipGroup? = when (id) {
        383 -> Structure // Destructible Sentry Gun

        1452 -> Drone // Irregular Drone
        1454 -> FighterDrone // Irregular Fighter
        1566 -> Shuttle // Irregular Shuttle
        1568 -> Frigate // Irregular Frigate
        1664 -> Destroyer // Irregular Destroyer
        1665 -> Cruiser // Irregular Cruiser
        1666 -> Battlecruiser // Irregular Battlecruiser
        1667 -> Battleship // Irregular Battleship
        1724 -> Dreadnought // Irregular Dreadnought
        1726 -> Carrier // Irregular Carrier
        1895 -> Industrial // Irregular Industrial
        1926 -> Freighter // Irregular Freighter
        1927 -> Structure // Irregular Structure

        1764 -> Frigate // ♦ Mining Frigate
        1765 -> Industrial // ♦ Mining Barge
        1766 -> Industrial // ♦ Mining Exhumer
        1767 -> Hauler // ♦ Mining Hauler
        1803 -> Frigate // ♦ Frigate
        1813 -> Cruiser // ♦ Cruiser
        1814 -> Battleship // ♦ Battleship
        1878 -> Titan // ♦ Titan
        1879 -> ForceAuxiliary // ♦ Force Auxiliary
        1880 -> Dreadnought // ♦ Dreadnought
        1896 -> Industrial // ♦ Industrial Command
        1909 -> Battlecruiser // ♦ Battlecruiser

        4037 -> TriglavianRogueDrone // Rogue Drone Entities
        else -> null
    }
}
