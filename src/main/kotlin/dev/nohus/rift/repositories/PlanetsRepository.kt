package dev.nohus.rift.repositories

import dev.nohus.rift.database.static.Planets
import dev.nohus.rift.database.static.StaticDatabase
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.planet_background_barren
import dev.nohus.rift.generated.resources.planet_background_gas
import dev.nohus.rift.generated.resources.planet_background_ice
import dev.nohus.rift.generated.resources.planet_background_lava
import dev.nohus.rift.generated.resources.planet_background_oceanic
import dev.nohus.rift.generated.resources.planet_background_plasma
import dev.nohus.rift.generated.resources.planet_background_storm
import dev.nohus.rift.generated.resources.planet_background_temperate
import dev.nohus.rift.generated.resources.planet_barren
import dev.nohus.rift.generated.resources.planet_gas
import dev.nohus.rift.generated.resources.planet_ice
import dev.nohus.rift.generated.resources.planet_lava
import dev.nohus.rift.generated.resources.planet_ocean
import dev.nohus.rift.generated.resources.planet_plasma
import dev.nohus.rift.generated.resources.planet_storm
import dev.nohus.rift.generated.resources.planet_temperate
import dev.nohus.rift.repositories.PlanetTypes.PlanetType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.exposed.sql.selectAll
import org.koin.core.annotation.Single

@Single
class PlanetsRepository(
    staticDatabase: StaticDatabase,
) {

    data class Planet(
        val id: Int,
        val systemId: Int,
        val type: PlanetType,
        val name: String,
        val radius: Float,
    )

    private val scope = CoroutineScope(Job())
    private lateinit var planetsBySystemId: Map<Int, List<Planet>>
    private lateinit var planetsById: Map<Int, Planet>
    private val hasLoaded = CompletableDeferred<Unit>()

    init {
        scope.launch(Dispatchers.IO) {
            val typesById = PlanetTypes.types.associateBy { it.typeId }
            val planets = staticDatabase.transaction {
                Planets.selectAll().toList()
            }.map {
                Planet(
                    id = it[Planets.id],
                    systemId = it[Planets.systemId],
                    type = typesById[it[Planets.typeId]]!!,
                    name = it[Planets.name],
                    radius = it[Planets.radius],
                )
            }
            planetsBySystemId = planets.groupBy { it.systemId }
            planetsById = planets.associateBy { it.id }
            hasLoaded.complete(Unit)
        }
    }

    private fun blockUntilLoaded() {
        runBlocking {
            hasLoaded.await()
        }
    }

    fun getPlanets(): Map<Int, List<Planet>> {
        blockUntilLoaded()
        return planetsBySystemId
    }

    fun getPlanetById(id: Int): Planet? {
        blockUntilLoaded()
        return planetsById[id]
    }
}

object PlanetTypes {

    data class PlanetType(
        val typeId: Int,
        val name: String,
        val icon: DrawableResource,
        val background: DrawableResource,
    )

    val types = listOf(
        PlanetType(11, "Temperate", Res.drawable.planet_temperate, Res.drawable.planet_background_temperate),
        PlanetType(12, "Ice", Res.drawable.planet_ice, Res.drawable.planet_background_ice),
        PlanetType(13, "Gas", Res.drawable.planet_gas, Res.drawable.planet_background_gas),
        PlanetType(2014, "Oceanic", Res.drawable.planet_ocean, Res.drawable.planet_background_oceanic),
        PlanetType(2015, "Lava", Res.drawable.planet_lava, Res.drawable.planet_background_lava),
        PlanetType(2016, "Barren", Res.drawable.planet_barren, Res.drawable.planet_background_barren),
        PlanetType(2017, "Storm", Res.drawable.planet_storm, Res.drawable.planet_background_storm),
        PlanetType(2063, "Plasma", Res.drawable.planet_plasma, Res.drawable.planet_background_plasma),

        PlanetType(30889, "Shattered", Res.drawable.planet_lava, Res.drawable.planet_background_lava),
        PlanetType(73911, "Scorched Barren", Res.drawable.planet_barren, Res.drawable.planet_background_barren),
    )
}
