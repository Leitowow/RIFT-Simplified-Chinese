package dev.nohus.rift.logs

import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single
import java.io.IOException
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isDirectory
import kotlin.io.path.name

private val logger = KotlinLogging.logger {}

@Single
class MatchChatLogFilenameUseCase {

    private val chatLogFilenameRegex = """^(?<name>.*)_(?<date>[0-9]{8})_(?<time>[0-9]{6})(_(?<characterid>[0-9]+))?\.txt$""".toRegex()
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

    operator fun invoke(file: Path): ChatLogFile? {
        try {
            if (file.isDirectory()) return null
            val match = chatLogFilenameRegex.find(file.name) ?: run {
                logger.debug { "Chat log filename \"${file.name}\" could not be parsed: did not match regex" }
                return null
            }
            val name = match.groups["name"]!!.value
            val date = match.groups["date"]!!.value
            val time = match.groups["time"]!!.value
            val characterId = match.groups["characterid"]?.value?.toIntOrNull() ?: run {
                // Old log files do not contain the character ID
                logger.error { "Chat log filename \"${file.name}\" could not be parsed: no character ID" }
                return null
            }
            val dateTime = LocalDateTime.parse("$date$time", dateFormatter)
            try {
                val lastModified = file.getLastModifiedTime().toInstant()
                return ChatLogFile(file, name, dateTime, characterId, lastModified)
            } catch (e: IOException) {
                logger.error(e) { "Chat log filename \"${file.name}\" could not be parsed: file not found" }
                return null
            }
        } catch (e: DateTimeParseException) {
            logger.error(e) { "Chat log filename \"${file.name}\" could not be parsed: date time invalid" }
            return null
        }
    }
}
