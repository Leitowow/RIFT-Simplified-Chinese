package dev.nohus.rift.pings

import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.repositories.MapStatusRepository
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.character.CharactersRepository
import dev.nohus.rift.standings.StandingsRepository
import org.koin.core.annotation.Single
import java.time.Instant

@Single
class ParsePingUseCase(
    private val charactersRepository: CharactersRepository,
    private val solarSystemsRepository: SolarSystemsRepository,
    private val mapStatusRepository: MapStatusRepository,
    private val standingsRepository: StandingsRepository,
) {
    data class Key(
        val names: List<String>,
        val canBeMultiline: Boolean = false,
    )

    private val fleetCommanderKey = Key(listOf("FC Name", "FC"))
    private val fleetKey = Key(listOf("Fleet name", "Fleet"))
    private val formupKey = Key(listOf("Formup Location", "Formup", "Loc"))
    private val papKey = Key(listOf("PAP Type", "Pap Type"))
    private val commsKey = Key(listOf("Comms"))
    private val doctrineKey = Key(listOf("Doctrine"), canBeMultiline = true)
    private val allKeys = listOf(fleetCommanderKey, fleetKey, formupKey, papKey, commsKey, doctrineKey)

    suspend operator fun invoke(
        timestamp: Instant,
        text: String,
    ): List<PingModel> {
        val cleanText = text
            .replace("\u200D", "")
            .replace("\uFEFF", "")
            .replace("PAP \nType:", "\nPAP Type:")
            .replace("[^\n]Doctrine:".toRegex(), "\nDoctrine:")

        if (!cleanText.contains("~~~ This was")) return emptyList() // Not a ping

        val pingTexts = splitMultiFleetPing(cleanText)
        return pingTexts.map { pingText ->
            val fleetCommander = getValue(pingText, fleetCommanderKey)?.let { parseFleetCommander(it) }
            val fleet = getValue(pingText, fleetKey)
            var formupLocations = getValue(pingText, formupKey)?.let { parseFormupLocations(it) } ?: emptyList()
            val papType = getValue(pingText, papKey)?.let { parsePapType(it) }
            val comms = getValue(pingText, commsKey)?.let { parseComms(it) }
            val doctrine = getValue(pingText, doctrineKey)?.let { parseDoctrine(it) }

            val description = pingText.lines()
                .asSequence()
                .map { it.trim() }
                .filterNot { it.startsWith("~~~ This was") }
                .let { lines ->
                    var filteredLines = lines.toList()
                    while (true) {
                        val indices = getValueIndices(filteredLines, allKeys) ?: break
                        filteredLines = filteredLines.withIndex().filter { it.index !in indices }.map { it.value }
                    }
                    filteredLines
                }
                .windowed(2) { (a, b) -> if (a.isEmpty() && b.isEmpty()) listOf() else listOf(a) }
                .flatten()
                .joinToString("\n")
                .trim()

            // Try to find a formup location in the description if it wasn't specified with a key
            if (formupLocations.isEmpty()) {
                formupLocations = description.lines().firstNotNullOfOrNull { line ->
                    parseFormupLocations(line)
                        .filter { it is FormupLocation.System }
                        .takeIf { it.isNotEmpty() }
                } ?: emptyList()
            }

            val signature = cleanText.lines().lastOrNull()?.let { lastLine ->
                val regex = """~~~ This was a (?<source>.*) ?broadcast from (?<sender>.*) to (?<target>.*) at .* ~~~""".toRegex()
                regex.find(lastLine)
            }
            val broadcastSource = signature?.groups?.get("source")?.value?.trim()?.takeIf { it.isNotBlank() }
            val sender = signature?.groups?.get("sender")?.value?.trim()?.takeIf { it.isNotBlank() }
            val target = signature?.groups?.get("target")?.value?.trim()?.takeIf { it.isNotBlank() }

            if (fleetCommander != null) {
                PingModel.FleetPing(
                    timestamp = timestamp,
                    sourceText = text,
                    description = description,
                    fleetCommander = fleetCommander,
                    fleet = fleet,
                    formupLocations = formupLocations,
                    papType = papType,
                    comms = comms,
                    doctrine = doctrine,
                    broadcastSource = broadcastSource,
                    target = target,
                )
            } else {
                val plainText = cleanText.lines()
                    .takeWhile { !it.startsWith("~~~ This was") }
                    .joinToString("\n")
                    .trim()
                PingModel.PlainText(
                    timestamp = timestamp,
                    sourceText = text,
                    text = plainText,
                    sender = sender,
                    target = target,
                )
            }
        }
    }

    /**
     * A ping can contain multiple fleets. Split the ping text into the individual constituent pings.
     */
    private fun splitMultiFleetPing(text: String): List<String> {
        val textBlocks = text.split("\n\n")
        val indicesOfTextBlocksWithFcs = textBlocks.withIndex().filter { (_, block) ->
            getValue(block, fleetCommanderKey) != null
        }.map { it.index }

        if (indicesOfTextBlocksWithFcs.size > 1) {
            // Multi-fleet ping, split
            var currentIndex = 0
            val splits = mutableListOf<String>()
            indicesOfTextBlocksWithFcs.forEach { index ->
                val toIndex = if (index == indicesOfTextBlocksWithFcs.last()) {
                    // This is the last split, so consume all text to the end for it
                    textBlocks.size
                } else {
                    index + 1
                }
                val blocksInThisSplit = textBlocks.subList(currentIndex, toIndex)
                splits += blocksInThisSplit.joinToString("\n\n")
                currentIndex = index + 1
            }
            return splits
        } else {
            // Normal ping, return as single
            return listOf(text)
        }
    }

    private suspend fun parseFleetCommander(text: String): FleetCommander {
        val characterId = charactersRepository.getCharacterId(Originator.Pings, text)
        return FleetCommander(text, characterId)
    }

    private fun parseFormupLocations(text: String): List<FormupLocation> {
        val splitRegex = """[\s/&]""".toRegex()
        return if (splitRegex in text) { // Multiple systems
            val locations = text.split(splitRegex).filterNot {
                it.trim().lowercase() in listOf("", "and", "or", "-")
            }.map(::parseFormupLocation)
            // Merge consecutive text formup locations
            return mutableListOf<FormupLocation>().apply {
                for (location in locations) {
                    val previous = lastOrNull()
                    if (previous != null && previous is FormupLocation.Text && location is FormupLocation.Text) {
                        this[lastIndex] = FormupLocation.Text("${previous.text} ${location.text}")
                    } else {
                        this += location
                    }
                }
            }
        } else {
            listOf(parseFormupLocation(text))
        }
    }

    private fun parseFormupLocation(text: String): FormupLocation {
        val textWithoutInterpunction = text.removeSuffix(",")
        var system = solarSystemsRepository.getFuzzySystem(textWithoutInterpunction, regionsHint = emptyList()) // Fast path
        if (system == null) { // System not found, try with system hints
            val friendlyAllianceIds = standingsRepository.getFriendlyAllianceIds()
            val friendlySystems = mapStatusRepository.status.value.mapNotNull {
                if (it.value.sovereignty?.allianceId in friendlyAllianceIds) it.key else null
            }
            system = solarSystemsRepository.getFuzzySystem(textWithoutInterpunction, regionsHint = emptyList(), systemHints = friendlySystems)
        }
        val solarSystemId = system?.id
        return if (solarSystemId != null) FormupLocation.System(solarSystemId) else FormupLocation.Text(text)
    }

    private fun parsePapType(text: String): PapType? {
        return when {
            text.lowercase().startsWith("strat") -> PapType.Strategic
            text.lowercase().startsWith("peace") -> PapType.Peacetime
            text.lowercase() in listOf("none") -> null
            else -> PapType.Text(text)
        }
    }

    private fun parseComms(text: String): Comms {
        val regex = """(?<channel>.*) (?<link>https://gnf\.lt/.*\.html)""".toRegex()
        regex.find(text)?.let { match ->
            return Comms.Mumble(
                channel = match.groups["channel"]!!.value,
                link = match.groups["link"]!!.value,
            )
        }
        return Comms.Text(text)
    }

    private fun parseDoctrine(text: String): Doctrine {
        return Doctrine(
            text = text,
            link = getDoctrineLink(text),
        )
    }

    private fun getDoctrineLink(text: String): String? {
        val doctrines = mapOf(
            "CFI" to "https://goonfleet.com/index.php/topic/353938-active-strat-cyclone-fleet-issue/",
            "SuperTrains" to "https://goonfleet.com/index.php/topic/342568-active-strat-supertrains-mainfleet-editionrokh/",
            "Techfleet" to "https://goonfleet.com/index.php/topic/327228-active%E2%80%94strat%E2%80%94techfleet/",
            "OSPREY NAVY ISSUE" to "https://goonfleet.com/index.php/topic/341744-active-peacetime-osprey-navy-issues/",
            "thrasher fleet issue" to "https://goonfleet.com/index.php/topic/349435-active-peacetime-tfis/",
            "ENI" to "https://goonfleet.com/index.php/topic/349390-active-peacetime-eni-fleet/",
            "Void Rays" to "https://goonfleet.com/index.php/topic/345055-active-strat-void-rays-mwd-kikis/",
            "FNI" to "https://goonfleet.com/index.php/topic/357801-active-strat-hammerfleet-fni/",
            "Mallet" to "https://goonfleet.com/index.php/topic/295651-active%E2%80%94strat%E2%80%94malletfleet-feroxes/",
            "Ferox" to "https://goonfleet.com/index.php/topic/295651-active%E2%80%94strat%E2%80%94malletfleet-feroxes/",
            "Leshaks" to "https://goonfleet.com/index.php/topic/337953-active-strat-leshaks-the-electric-heat-gun/",
            "dakka" to "https://goonfleet.com/index.php/topic/349471-active-strat-dakka-fleet-20-return-of-the-dakka/",
            "Sacs" to "https://goonfleet.com/index.php/topic/334896-active-strat-sacfleet-20-sacrilege/",
            "Stormbringers" to "https://goonfleet.com/index.php/topic/335865-active-strat-stormbringers-your-electric-gimmick-here/",
            "Harpy Fleet" to "https://goonfleet.com/index.php/topic/346057-active-strat-harpyfleet/",
            "Torp Bombers" to "https://goonfleet.com/index.php/topic/344458-active-strat-siegefleet-40-torp-bombers/",
            "Flycatchers" to "https://goonfleet.com/index.php/topic/326958-active-strat-flycatchers/",
            "BOMB BOMBERS" to "https://goonfleet.com/index.php/topic/312491-active-strat-bomb-bombers/",
            "hurricane" to "https://goonfleet.com/index.php/topic/295648-active-strat-mosh-masters-hurricanes/",
            "Kestrel" to "https://goonfleet.com/index.php/topic/314644-active-peacetime-kestrels/",
            "Caracal" to "https://goonfleet.com/index.php/topic/293358-active-peacetime-caracals/",
            "Cormorant" to "https://goonfleet.com/index.php/topic/299033-active-peacetime-cormorants/",
            "Tornado" to "https://goonfleet.com/index.php/topic/296788-active-peacetime-windrunners-tornados/",
            "Retri" to "https://goonfleet.com/index.php/topic/354268-active-strat-retributions/",
            "goof" to "https://goonfleet.com/index.php/topic/358839-active-war-goof-redeemers/",
            "Redeem" to "https://goonfleet.com/index.php/topic/358839-active-war-goof-redeemers/",
            "TOMAHAWK" to "https://goonfleet.com/index.php/topic/355156-active-strat-tomahawks-raven-navy-issues/",
            "Raven" to "https://goonfleet.com/index.php/topic/355156-active-strat-tomahawks-raven-navy-issues/",
            "Snail" to "https://goonfleet.com/index.php/topic/366187-active-strat-snail-fleet/",
            "Vultures" to "https://goonfleet.com/index.php/topic/369029-active-strat-vultures/",
            "Crusaders" to "https://goonfleet.com/index.php/topic/372184-active-strat-contraceptors/",
        )
        if (text.contains("(")) {
            val name = text.substringBefore("(")
            doctrines.entries
                .firstOrNull { it.key.lowercase() in name.lowercase() }
                ?.value
                ?.let { return it }
        }
        return doctrines.entries
            .firstOrNull { it.key.lowercase() in text.lowercase() }
            ?.value
    }

    private fun getValue(text: String, key: Key): String? {
        val lines = text.lines()
        val start = lines.indexOfFirst { line -> key.names.any { "$it:" in line } }
        if (start < 0) return null
        val keyName = key.names.firstOrNull { lines[start].startsWith(it) } ?: return null
        return lines.drop(start).withIndex()
            .takeWhile { (index, line) -> index == 0 || key.canBeMultiline && (":" !in line && line.isNotBlank()) }
            .joinToString("\n") { it.value }
            .removePrefix("$keyName:")
            .trim()
    }

    private fun getValueIndices(lines: List<String>, keys: List<Key>): List<Int>? {
        val names = keys.flatMap { it.names }
        val start = lines.indexOfFirst { line -> names.any { "$it:" in line } }
        if (start < 0) return null
        val key = keys.firstOrNull { it.names.any { name -> "$name:" in lines[start] } } ?: return null
        return lines.drop(start).withIndex()
            .takeWhile { (index, line) -> index == 0 || key.canBeMultiline && (":" !in line && line.isNotBlank()) }
            .map { start + it.index }
    }
}
