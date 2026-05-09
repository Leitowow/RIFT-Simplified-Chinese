package dev.nohus.rift.startupwarning

import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.startupwarning.HasIncorrectSystemTimeUseCase.SystemTimeStatus.Incorrect
import dev.nohus.rift.utils.plural
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single

private val logger = KotlinLogging.logger {}

@Single
class GetStartupWarningsUseCase(
    private val hasNonEnglishEveClient: HasNonEnglishEveClientUseCase,
    private val hasFullScreenEveClient: HasFullScreenEveClientUseCase,
    private val isRunningMsiAfterburner: IsRunningMsiAfterburnerUseCase,
    private val getAccountsWithDisabledChatLogs: GetAccountsWithDisabledChatLogsUseCase,
    private val isMissingXWinInfo: IsMissingXWinInfoUseCase,
    private val hasIncorrectSystemTimeUseCase: HasIncorrectSystemTimeUseCase,
    private val isRunningOldVersionUseCase: IsRunningOldVersionUseCase,
    private val settings: Settings,
) {

    data class StartupWarning(
        val id: String,
        val title: String,
        val description: String,
        val detail: String? = null,
    )

    suspend operator fun invoke(): List<StartupWarning> {
        return buildList {
            val systemTimeStatus = hasIncorrectSystemTimeUseCase()
            if (systemTimeStatus is Incorrect) {
                val text = buildString {
                    append("The clock on your computer is ")
                    val absoluteOffset = systemTimeStatus.offset.abs()
                    val minutes = absoluteOffset.toMinutes()
                    val seconds = absoluteOffset.toSecondsPart()
                    append("$minutes minute${minutes.plural} and $seconds second${seconds.plural} ")
                    if (systemTimeStatus.offset.isNegative) {
                        append("behind the real time. ")
                    } else {
                        append("ahead of the real time. ")
                    }
                    append("You need to set your clock to the correct time to prevent issues like not receiving alerts.")
                }
                add(
                    StartupWarning(
                        id = "incorrect system time",
                        title = "Incorrect system time",
                        description = text,
                    ),
                )
            }
            if (hasNonEnglishEveClient()) {
                add(
                    StartupWarning(
                        id = "non-english client",
                        title = "Non-English EVE Client",
                        description = """
                            Your EVE client is set to a language other than English.
                            RIFT features based on reading game logs won't work.
                        """.trimIndent(),
                    ),
                )
            }
            if (hasFullScreenEveClient()) {
                add(
                    StartupWarning(
                        id = "fullscreen client",
                        title = "Fullscreen EVE Client",
                        description = """
                            Your EVE client is set to run in fullscreen mode.
                            You might not be able to put RIFT windows on top of it.
                            
                            It's recommended to use Fixed Window or Window mode.
                        """.trimIndent(),
                    ),
                )
            }
            if (isRunningMsiAfterburner()) {
                add(
                    StartupWarning(
                        id = "msi afterburner",
                        title = "MSI Afterburner",
                        description = """
                            You are running MSI Afterburner or RivaTuner, which is known to inject code into RIFT that causes freezes and crashes.
                        """.trimIndent(),
                    ),
                )
            }
            val accountMessages = getAccountsWithDisabledChatLogs()
            if (accountMessages.isNotEmpty()) {
                add(
                    StartupWarning(
                        id = "chat logs disabled v2",
                        title = "Chat logs are disabled",
                        description = buildString {
                            appendLine("You need to enable the \"Log Chat to File\" option in EVE Settings, in the Gameplay section. RIFT won't be able to read intel messages or trigger alerts otherwise.")
                            appendLine()
                            if (accountMessages.size == 1) {
                                append("You have it disabled on this account:")
                            } else {
                                append("You have it disabled on these ${accountMessages.size} accounts:")
                            }
                        },
                        detail = accountMessages.joinToString("\n"),
                    ),
                )
            }
            if (isMissingXWinInfo()) {
                add(
                    StartupWarning(
                        id = "missing x11-utils",
                        title = "Missing dependency",
                        description = """
                            You don't have "xwininfo", "xprop", or "wmctrl" installed. Usually they are in a "x11-utils" package, "wmctrl" package, or similar. Without them, RIFT won't be able to check the online status of your characters.
                        """.trimIndent(),
                    ),
                )
            }
            if (isRunningOldVersionUseCase()) {
                add(
                    StartupWarning(
                        id = "old version",
                        title = "Outdated version",
                        description = """
                            You are running an outdated version of RIFT. Please check the About window to update to the latest version.
                        """.trimIndent(),
                    ),
                )
            }
        }
            .also {
                if (it.isNotEmpty()) {
                    logger.warn { "Startup warnings: ${it.joinToString("\n") { warning -> listOfNotNull(warning.id, warning.description, warning.detail).joinToString() }}" }
                }
            }
            .filter { it.id !in settings.dismissedWarnings }
    }
}
