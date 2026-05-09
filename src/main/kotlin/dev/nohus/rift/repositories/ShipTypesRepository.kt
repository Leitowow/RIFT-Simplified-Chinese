package dev.nohus.rift.repositories

import dev.nohus.rift.database.static.Ships
import dev.nohus.rift.database.static.StaticDatabase
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.brackets_battlecruiser_16
import dev.nohus.rift.generated.resources.brackets_battleship_16
import dev.nohus.rift.generated.resources.brackets_capsule_16
import dev.nohus.rift.generated.resources.brackets_carrier_16
import dev.nohus.rift.generated.resources.brackets_cruiser_16
import dev.nohus.rift.generated.resources.brackets_destroyer_16
import dev.nohus.rift.generated.resources.brackets_dreadnought_16
import dev.nohus.rift.generated.resources.brackets_forceauxiliary_16
import dev.nohus.rift.generated.resources.brackets_freighter_16
import dev.nohus.rift.generated.resources.brackets_frigate_16
import dev.nohus.rift.generated.resources.brackets_industrial_16
import dev.nohus.rift.generated.resources.brackets_industrialcommand_16
import dev.nohus.rift.generated.resources.brackets_miningbarge_16
import dev.nohus.rift.generated.resources.brackets_miningfrigate_16
import dev.nohus.rift.generated.resources.brackets_rookie_16
import dev.nohus.rift.generated.resources.brackets_shuttle_16
import dev.nohus.rift.generated.resources.brackets_supercarrier_16
import dev.nohus.rift.generated.resources.brackets_titan_16
import dev.nohus.rift.repositories.TypesRepository.Type
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.exposed.sql.selectAll
import org.koin.core.annotation.Single

@Single
class ShipTypesRepository(
    staticDatabase: StaticDatabase,
    private val typesRepository: TypesRepository,
) {

    private val lowercaseShipNames: Map<String, String>
    private val shipClasses: Map<Int, String> // Ship ID -> Ship class
    private val shipNicknames: Map<String, String>
    private val navyVariants: Map<String, String>
    private val fleetVariants: Map<String, String>

    init {
        val rows = staticDatabase.transaction {
            Ships.selectAll().toList()
        }
        val shipNames = rows.map { it[Ships.name] }.toSet()
        lowercaseShipNames = shipNames.associateBy { it.lowercase() }
        shipClasses = rows.associate { it[Ships.typeId] to it[Ships.shipClass] }
        shipNicknames = mapOf(
            "kiki" to "Kikimora",
            "iki" to "Ikitursa",
            "stileto" to "Stiletto",
            "stilleto" to "Stiletto",
            "stilletto" to "Stiletto",
            "shuttle" to "Shuttle",
            "pod" to "Capsule",
            "execuror" to "Exequror",
            "exequoror" to "Exequror",
            "exeq" to "Exequror",
            "incursis" to "Incursus",
            "cerb" to "Cerberus",
            "orthus" to "Orthrus",
            "retri" to "Retribution",
            "sythe" to "Scythe",
            "trasher" to "Thrasher",
            "auguror" to "Augoror",
            "porp" to "Porpoise",
            "bni" to "Brutix Navy Issue",
            "eni" to "Exequror Navy Issue",
        )
        navyVariants = mapOf(
            "Slicer" to "Imperial Navy Slicer",
            "Crucifier" to "Crucifier Navy Issue",
            "Magnate" to "Magnate Navy Issue",
            "Hookbill" to "Caldari Navy Hookbill",
            "Griffin" to "Griffin Navy Issue",
            "Heron" to "Heron Navy Issue",
            "Comet" to "Federation Navy Comet",
            "Maulus" to "Maulus Navy Issue",
            "Imicus" to "Imicus Navy Issue",
            "Coercer" to "Coercer Navy Issue",
            "Cormorant" to "Cormorant Navy Issue",
            "Catalyst" to "Catalyst Navy Issue",
            "Augoror" to "Augoror Navy Issue",
            "Omen" to "Omen Navy Issue",
            "Caracal" to "Caracal Navy Issue",
            "Osprey" to "Osprey Navy Issue",
            "Exequror" to "Exequror Navy Issue",
            "Vexor" to "Vexor Navy Issue",
            "Harbinger" to "Harbinger Navy Issue",
            "Prophecy" to "Prophecy Navy Issue",
            "Drake" to "Drake Navy Issue",
            "Ferox" to "Ferox Navy Issue",
            "Brutix" to "Brutix Navy Issue",
            "Myrmidon" to "Myrmidon Navy Issue",
            "Apocalypse" to "Apocalypse Navy Issue",
            "Armageddon" to "Armageddon Navy Issue",
            "Scorpion" to "Scorpion Navy Issue",
            "Raven" to "Raven Navy Issue",
            "Megathron" to "Megathron Navy Issue",
            "Dominix" to "Dominix Navy Issue",
            "Revelation" to "Revelation Navy Issue",
            "Phoenix" to "Phoenix Navy Issue",
            "Moros" to "Moros Navy Issue",
        ).mapKeys { (k, _) -> k.lowercase() }
        fleetVariants = mapOf(
            "Firetail" to "Republic Fleet Firetail",
            "Vigil" to "Vigil Fleet Issue",
            "Probe" to "Probe Fleet Issue",
            "Thrasher" to "Thrasher Fleet Issue",
            "Scythe" to "Scythe Fleet Issue",
            "Stabber" to "Stabber Fleet Issue",
            "Hurricane" to "Hurricane Fleet Issue",
            "Cyclone" to "Cyclone Fleet Issue",
            "Tempest" to "Tempest Fleet Issue",
            "Typhoon" to "Typhoon Fleet Issue",
            "Naglfar" to "Naglfar Fleet Issue",
        ).mapKeys { (k, _) -> k.lowercase() }
    }

    fun getFuzzyShip(name: String): Type? {
        val fullShipName = getFuzzyShipName(name) ?: return null
        return if (fullShipName == "Shuttle") {
            typesRepository.getType(672) // Caldari Shuttle
        } else {
            typesRepository.getType(fullShipName)
        }
    }

    /**
     * @param name Potential ship name
     * @return Full name of the ship or null if it's not a ship name
     */
    private fun getFuzzyShipName(name: String): String? {
        val lowercase = name.lowercase()
        val isNavy = lowercase.startsWith("navy ") xor lowercase.endsWith(" navy")
        val isFleet = lowercase.startsWith("fleet ")
        return when {
            isNavy -> {
                val baseName = lowercase.removePrefix("navy ").removeSuffix(" navy").trim()
                getFullShip(baseName)?.let { navyVariants[it.lowercase()] } ?: navyVariants[baseName]
            }
            isFleet -> {
                val baseName = lowercase.removePrefix("fleet ").trim()
                getFullShip(baseName)?.let { fleetVariants[it.lowercase()] } ?: fleetVariants[baseName]
            }
            else -> getFullShip(lowercase)
        }
    }

    private fun getFullShip(lowercase: String): String? {
        return when {
            lowercaseShipNames.contains(lowercase) -> lowercaseShipNames.getValue(lowercase)
            shipNicknames.contains(lowercase) -> shipNicknames.getValue(lowercase)
            else -> return null
        }
    }

    fun getShipClass(id: Int): String? {
        return shipClasses[id]
    }

    fun getShipClasses(): List<String> {
        return shipClasses.values.distinct()
    }

    fun getShipBracketIcon(id: Int): DrawableResource? {
        val shipClass = getShipClass(id) ?: return null
        return getShipClassBracket(shipClass)
    }

    private fun getShipClassBracket(shipClass: String): DrawableResource? {
        return when (shipClass) {
            "Capsule" -> Res.drawable.brackets_capsule_16
            "Shuttle" -> Res.drawable.brackets_shuttle_16
            "Corvette" -> Res.drawable.brackets_rookie_16
            "Frigate" -> Res.drawable.brackets_frigate_16
            "Assault Frigate" -> Res.drawable.brackets_frigate_16
            "Interceptor" -> Res.drawable.brackets_frigate_16
            "Electronic Attack Ship" -> Res.drawable.brackets_frigate_16
            "Covert Ops" -> Res.drawable.brackets_frigate_16
            "Logistics Frigate" -> Res.drawable.brackets_frigate_16
            "Prototype Exploration Ship" -> Res.drawable.brackets_frigate_16
            "Stealth Bomber" -> Res.drawable.brackets_frigate_16
            "Destroyer" -> Res.drawable.brackets_destroyer_16
            "Command Destroyer" -> Res.drawable.brackets_destroyer_16
            "Tactical Destroyer" -> Res.drawable.brackets_destroyer_16
            "Interdictor" -> Res.drawable.brackets_destroyer_16
            "Cruiser" -> Res.drawable.brackets_cruiser_16
            "Combat Recon Ship" -> Res.drawable.brackets_cruiser_16
            "Flag Cruiser" -> Res.drawable.brackets_cruiser_16
            "Force Recon Ship" -> Res.drawable.brackets_cruiser_16
            "Heavy Assault Cruiser" -> Res.drawable.brackets_cruiser_16
            "Heavy Interdiction Cruiser" -> Res.drawable.brackets_cruiser_16
            "Logistics" -> Res.drawable.brackets_cruiser_16
            "Strategic Cruiser" -> Res.drawable.brackets_cruiser_16
            "Attack Battlecruiser" -> Res.drawable.brackets_battlecruiser_16
            "Combat Battlecruiser" -> Res.drawable.brackets_battlecruiser_16
            "Command Ship" -> Res.drawable.brackets_battlecruiser_16
            "Battleship" -> Res.drawable.brackets_battleship_16
            "Black Ops" -> Res.drawable.brackets_battleship_16
            "Marauder" -> Res.drawable.brackets_battleship_16
            "Carrier" -> Res.drawable.brackets_carrier_16
            "Force Auxiliary" -> Res.drawable.brackets_forceauxiliary_16
            "Supercarrier" -> Res.drawable.brackets_supercarrier_16
            "Dreadnought" -> Res.drawable.brackets_dreadnought_16
            "Lancer Dreadnought" -> Res.drawable.brackets_dreadnought_16
            "Titan" -> Res.drawable.brackets_titan_16
            "Hauler" -> Res.drawable.brackets_industrial_16
            "Blockade Runner" -> Res.drawable.brackets_industrial_16
            "Deep Space Transport" -> Res.drawable.brackets_industrial_16
            "Expedition Frigate" -> Res.drawable.brackets_miningfrigate_16
            "Mining Barge" -> Res.drawable.brackets_miningbarge_16
            "Exhumer" -> Res.drawable.brackets_miningbarge_16
            "Industrial Command Ship" -> Res.drawable.brackets_industrialcommand_16
            "Freighter" -> Res.drawable.brackets_freighter_16
            "Jump Freighter" -> Res.drawable.brackets_freighter_16
            "Capital Industrial Ship" -> Res.drawable.brackets_freighter_16
            "Expedition Command Ship" -> Res.drawable.brackets_battlecruiser_16
            else -> null
        }
    }
}
