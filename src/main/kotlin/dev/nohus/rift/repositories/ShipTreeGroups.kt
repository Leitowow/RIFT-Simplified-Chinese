package dev.nohus.rift.repositories

import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.ship_tree_group_battlecruiser_64
import dev.nohus.rift.generated.resources.ship_tree_group_battleship_64
import dev.nohus.rift.generated.resources.ship_tree_group_capital_64
import dev.nohus.rift.generated.resources.ship_tree_group_capsule_64
import dev.nohus.rift.generated.resources.ship_tree_group_cruiser_64
import dev.nohus.rift.generated.resources.ship_tree_group_destroyer_64
import dev.nohus.rift.generated.resources.ship_tree_group_freighter_64
import dev.nohus.rift.generated.resources.ship_tree_group_frigate_64
import dev.nohus.rift.generated.resources.ship_tree_group_industrial_64
import dev.nohus.rift.generated.resources.ship_tree_group_industrialcommand_64
import dev.nohus.rift.generated.resources.ship_tree_group_miningbarge_64
import dev.nohus.rift.generated.resources.ship_tree_group_miningfrigate_64
import dev.nohus.rift.generated.resources.ship_tree_group_rookie_64
import dev.nohus.rift.generated.resources.ship_tree_group_shuttle_64
import dev.nohus.rift.generated.resources.ship_tree_group_supercapital_64
import dev.nohus.rift.generated.resources.ship_tree_group_titan_64
import org.jetbrains.compose.resources.DrawableResource

object ShipTreeGroups {

    data class Group(val name: String, val icon: DrawableResource)

    private val groups = mapOf<Int, Group>(
        4 to Group("Corvette", Res.drawable.ship_tree_group_rookie_64),
        8 to Group("Frigate", Res.drawable.ship_tree_group_frigate_64),
        9 to Group("Navy Frigate", Res.drawable.ship_tree_group_frigate_64),
        10 to Group("Interceptor", Res.drawable.ship_tree_group_frigate_64),
        11 to Group("Assault Frigate", Res.drawable.ship_tree_group_frigate_64),
        12 to Group("Covert Ops", Res.drawable.ship_tree_group_frigate_64),
        13 to Group("Electronic Attack Ship", Res.drawable.ship_tree_group_frigate_64),
        14 to Group("Destroyer", Res.drawable.ship_tree_group_destroyer_64),
        15 to Group("Interdictor", Res.drawable.ship_tree_group_destroyer_64),
        16 to Group("Cruiser", Res.drawable.ship_tree_group_cruiser_64),
        17 to Group("Navy Cruiser", Res.drawable.ship_tree_group_cruiser_64),
        18 to Group("Recon Ship", Res.drawable.ship_tree_group_cruiser_64),
        19 to Group("Heavy Assault Cruiser", Res.drawable.ship_tree_group_cruiser_64),
        20 to Group("Heavy Interdiction Cruiser", Res.drawable.ship_tree_group_cruiser_64),
        21 to Group("Logistics Cruisers", Res.drawable.ship_tree_group_cruiser_64),
        22 to Group("Strategic Cruiser", Res.drawable.ship_tree_group_cruiser_64),
        23 to Group("Battlecruiser", Res.drawable.ship_tree_group_battlecruiser_64),
        24 to Group("Command Ships", Res.drawable.ship_tree_group_battlecruiser_64),
        25 to Group("Navy Battlecruiser", Res.drawable.ship_tree_group_battlecruiser_64),
        26 to Group("Battleship", Res.drawable.ship_tree_group_battleship_64),
        27 to Group("Black Ops", Res.drawable.ship_tree_group_battleship_64),
        28 to Group("Marauder", Res.drawable.ship_tree_group_battleship_64),
        32 to Group("Dreadnought", Res.drawable.ship_tree_group_capital_64),
        33 to Group("Carrier", Res.drawable.ship_tree_group_supercapital_64),
        34 to Group("Titan", Res.drawable.ship_tree_group_titan_64),
        35 to Group("Shuttle", Res.drawable.ship_tree_group_shuttle_64),
        36 to Group("Hauler", Res.drawable.ship_tree_group_industrial_64),
        37 to Group("Freighter", Res.drawable.ship_tree_group_freighter_64),
        38 to Group("Jump Freighters", Res.drawable.ship_tree_group_freighter_64),
        40 to Group("Transport Ship", Res.drawable.ship_tree_group_industrial_64),
        41 to Group("Mining Frigate", Res.drawable.ship_tree_group_miningfrigate_64),
        42 to Group("Mining Barge", Res.drawable.ship_tree_group_miningbarge_64),
        43 to Group("Exhumer", Res.drawable.ship_tree_group_miningbarge_64),
        44 to Group("ORE Hauler", Res.drawable.ship_tree_group_industrial_64),
        45 to Group("Industrial Command Ship", Res.drawable.ship_tree_group_industrialcommand_64),
        46 to Group("Capital Industrial Ship", Res.drawable.ship_tree_group_freighter_64),
        47 to Group("Navy Battleship", Res.drawable.ship_tree_group_battleship_64),
        48 to Group("Expedition Frigate", Res.drawable.ship_tree_group_miningfrigate_64),
        50 to Group("Tactical Destroyer", Res.drawable.ship_tree_group_destroyer_64),
        93 to Group("Command Destroyer", Res.drawable.ship_tree_group_destroyer_64),
        94 to Group("Logistics Frigates", Res.drawable.ship_tree_group_frigate_64),
        96 to Group("Flag Cruiser", Res.drawable.ship_tree_group_cruiser_64),
        2101 to Group("Navy Destroyer", Res.drawable.ship_tree_group_destroyer_64),
        2102 to Group("Navy Dreadnought", Res.drawable.ship_tree_group_capital_64),
        2104 to Group("Lancer Dreadnought", Res.drawable.ship_tree_group_capital_64),
        2107 to Group("Capsule", Res.drawable.ship_tree_group_capsule_64),
    )

    operator fun get(id: Int): Group? = groups[id]
}
