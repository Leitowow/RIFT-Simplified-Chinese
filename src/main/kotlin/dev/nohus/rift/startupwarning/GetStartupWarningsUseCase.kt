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
                        title = "检测到EVE非英文客户端",
                        description = """
                            您的客户端使用的是非英文语言，RIFT的预警功能将可能无法正常工作。
                            但是！它一般情况下都正常工作。
                        """.trimIndent(),
                    ),
                )
            }
            if (isRunningMsiAfterburner()) {
                add(
                    StartupWarning(
                        id = "msi afterburner",
                        title = "检测到微星小飞机",
                        description = """
                            你正在使用微星小飞机等超频软件，这可能会导致RIFT崩溃。
                        """.trimIndent(),
                    ),
                )
            }
            val accountMessages = getAccountsWithDisabledChatLogs()
            if (accountMessages.isNotEmpty()) {
                add(
                    StartupWarning(
                        id = "chat logs disabled v2",
                        title = "聊天记录已关闭",
                        description = buildString {
                            appendLine("您需要启用EVE设置中的“记录聊天到文件”选项，否则RIFT将无法读取情报信息或触发预警。")
                            appendLine()
                            if (accountMessages.size == 1) {
                                append("您在以下账户中关闭了聊天记录：")
                            } else {
                                append("您在以下${accountMessages.size}个账户中关闭了聊天记录：")
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
                        title = "缺少依赖",
                        description = """
                            您没有安装“xwininfo”或“xprop”。通常它们在“x11-utils”包或类似的包中。没有它们，RIFT将无法检查您的角色的在线状态。
                        """.trimIndent(),
                    ),
                )
            }
        }.filter { it.id !in settings.dismissedWarnings }
    }
}
