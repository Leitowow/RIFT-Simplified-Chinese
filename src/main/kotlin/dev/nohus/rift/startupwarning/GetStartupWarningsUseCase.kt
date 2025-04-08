package dev.nohus.rift.startupwarning

import dev.nohus.rift.settings.persistence.Settings
import org.koin.core.annotation.Single

@Single
class GetStartupWarningsUseCase(
    private val hasNonEnglishEveClient: HasNonEnglishEveClientUseCase,
    private val isRunningMsiAfterburner: IsRunningMsiAfterburnerUseCase,
    private val getAccountsWithDisabledChatLogs: GetAccountsWithDisabledChatLogsUseCase,
    private val isMissingXWinInfo: IsMissingXWinInfoUseCase,
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
                            You don't have "xwininfo" or "xprop" installed. Usually they are in a "x11-utils" package or similar. Without them, RIFT won't be able to check the online status of your characters.
                        """.trimIndent(),
                    ),
                )
            }
        }.filter { it.id !in settings.dismissedWarnings }
    }
}
