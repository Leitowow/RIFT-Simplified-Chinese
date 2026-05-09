package dev.nohus.rift.settings

import dev.nohus.rift.repositories.JumpBridgesRepository.JumpBridgeConnection
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.SolarSystemsRepository.MapSolarSystem
import dev.nohus.rift.utils.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Single

@Factory
class JumpBridgesParser(
    private val solarSystemsRepository: SolarSystemsRepository,
) {

    private val systems = solarSystemsRepository.getSovSystems().map { it.name }

    sealed interface ParsingResult {
        data object Empty : ParsingResult
        data object TooShort : ParsingResult
        data class ParsedNotEnough(val lines: List<ParsedLine>) : ParsingResult
        data class ParsedValid(val lines: List<ParsedLine>, val connections: List<JumpBridgeConnection>) : ParsingResult
    }

    sealed class ParsedLine(open val text: String) {
        data class Connection(override val text: String, val connection: JumpBridgeConnection) : ParsedLine(text)
        data class NoSystems(override val text: String) : ParsedLine(text)
        data class OneSystem(override val text: String, val system: MapSolarSystem) : ParsedLine(text)
        data class TooManySystems(override val text: String, val systems: List<MapSolarSystem>) : ParsedLine(text)
    }

    suspend fun parse(text: String): List<JumpBridgeConnection> {
        return (parseResult(text) as? ParsingResult.ParsedValid)?.connections ?: emptyList()
    }

    suspend fun parseResult(text: String): ParsingResult = withContext(Dispatchers.Default) {
        if (text.isBlank()) return@withContext ParsingResult.Empty
        val lines = text.trim().lines()
        if (lines.size < 2) return@withContext ParsingResult.TooShort

        val parsedLines = lines.map { line ->
            val systemsInLine = systems
                .filter { it in line }
                .sortedBy { line.indexOf(it) }
                .mapNotNull {
                    solarSystemsRepository.getSystem(it)
                }
            when (systemsInLine.size) {
                0 -> ParsedLine.NoSystems(line)
                1 -> ParsedLine.OneSystem(line, systemsInLine[0])
                2 -> ParsedLine.Connection(line, JumpBridgeConnection(systemsInLine[0], systemsInLine[1]))
                else -> ParsedLine.TooManySystems(line, systemsInLine)
            }
        }
        val connections = parsedLines.filterIsInstance<ParsedLine.Connection>().map { it.connection }
        if (connections.size >= 2) {
            ParsingResult.ParsedValid(parsedLines, connections)
        } else {
            ParsingResult.ParsedNotEnough(parsedLines)
        }
    }
}
