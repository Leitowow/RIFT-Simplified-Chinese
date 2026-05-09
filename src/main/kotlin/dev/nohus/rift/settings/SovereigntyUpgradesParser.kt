package dev.nohus.rift.settings

import dev.nohus.rift.repositories.JumpBridgesRepository.JumpBridgeConnection
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.SolarSystemsRepository.MapSolarSystem
import dev.nohus.rift.repositories.TypesRepository
import dev.nohus.rift.repositories.TypesRepository.Type
import dev.nohus.rift.sovupgrades.SovereigntyUpgradesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Single

@Factory
class SovereigntyUpgradesParser(
    private val solarSystemsRepository: SolarSystemsRepository,
    private val typesRepository: TypesRepository,
    private val sovereigntyUpgradesRepository: SovereigntyUpgradesRepository,
) {

    private val allSystems = solarSystemsRepository.getSovSystems().map { it.name }

    sealed interface ParsingResult {
        data object Empty : ParsingResult
        data class ParsedNotEnough(val lines: List<ParsedLine>) : ParsingResult
        data class ParsedValid(val lines: List<ParsedLine>, val upgrades: Map<MapSolarSystem, List<Type>>) : ParsingResult
    }

    sealed class ParsedLine(open val text: String) {
        data class SystemWithUpgrades(override val text: String, val system: MapSolarSystem, val upgrades: List<Type>) : ParsedLine(text)
        data class NoSystem(override val text: String) : ParsedLine(text)
        data class NoUpgrades(override val text: String, val system: MapSolarSystem) : ParsedLine(text)
    }

    suspend fun parse(text: String): Map<MapSolarSystem, List<Type>> {
        return (parseResult(text) as? ParsingResult.ParsedValid)?.upgrades ?: emptyMap()
    }

    suspend fun parseResult(text: String): ParsingResult = withContext(Dispatchers.Default) {
        if (text.isBlank()) return@withContext ParsingResult.Empty
        val lines = text.trim().lines()

        val parsedLines = lines.map { line ->
            val systemName = allSystems.firstOrNull { it in line } ?: return@map ParsedLine.NoSystem(line)
            val system = solarSystemsRepository.getSystem(systemName) ?: return@map ParsedLine.NoSystem(line)
            val types = typesRepository.findTypesInText(line)
                .filter { it.groupId in sovereigntyUpgradesRepository.upgradeGroupIds }
                .toList()
            if (types.isEmpty()) return@map ParsedLine.NoUpgrades(line, system)
            ParsedLine.SystemWithUpgrades(line, system, types)
        }
        val upgrades = parsedLines.filterIsInstance<ParsedLine.SystemWithUpgrades>().associate { it.system to it.upgrades }
        if (upgrades.isNotEmpty()) {
            ParsingResult.ParsedValid(parsedLines, upgrades)
        } else {
            ParsingResult.ParsedNotEnough(parsedLines)
        }
    }
}
