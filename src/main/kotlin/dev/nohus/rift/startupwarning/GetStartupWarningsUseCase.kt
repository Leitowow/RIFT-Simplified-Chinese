package dev.nohus.rift.startupwarning

import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.startupwarning.HasIncorrectSystemTimeUseCase.SystemTimeStatus.Incorrect
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
                    append("你的电脑时钟与标准时间相差 ")
                    val absoluteOffset = systemTimeStatus.offset.abs()
                    val minutes = absoluteOffset.toMinutes()
                    val seconds = absoluteOffset.toSecondsPart()
                    append("${minutes} 分 ${seconds} 秒，")
                    if (systemTimeStatus.offset.isNegative) {
                        append("当前时间偏慢。")
                    } else {
                        append("当前时间偏快。")
                    }
                    append("请校准系统时间，否则可能出现无法接收报警等问题。")
                }
                add(
                    StartupWarning(
                        id = "incorrect system time",
                        title = "系统时间不正确",
                        description = text,
                    ),
                )
            }
            if (hasNonEnglishEveClient()) {
                add(
                    StartupWarning(
                        id = "non-english client",
                        title = "非英语 EVE 客户端",
                        description = """
                            你的 EVE 客户端语言不是英语。
                            依赖读取游戏日志的 RIFT 功能将无法正常工作。
                        """.trimIndent(),
                    ),
                )
            }
            if (hasFullScreenEveClient()) {
                add(
                    StartupWarning(
                        id = "fullscreen client",
                        title = "全屏 EVE 客户端",
                        description = """
                            你的 EVE 客户端正在使用全屏模式运行。
                            这可能导致 RIFT 窗口无法覆盖显示在游戏上方。
                            
                            建议改用固定窗口或窗口模式。
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
                            检测到你正在运行 MSI Afterburner 或 RivaTuner。
                            这类程序会向 RIFT 注入代码，已知可能导致卡死或崩溃。
                        """.trimIndent(),
                    ),
                )
            }
            val accountMessages = getAccountsWithDisabledChatLogs()
            if (accountMessages.isNotEmpty()) {
                add(
                    StartupWarning(
                        id = "chat logs disabled v2",
                        title = "聊天日志已禁用",
                        description = buildString {
                            appendLine("请在 EVE 设置的 Gameplay 分类中启用“Log Chat to File（聊天记录写入文件）”。否则 RIFT 无法读取情报消息并触发报警。")
                            appendLine()
                            if (accountMessages.size == 1) {
                                append("以下账号关闭了该选项：")
                            } else {
                                append("以下 ${accountMessages.size} 个账号关闭了该选项：")
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
                            你的系统未安装 "xwininfo"、"xprop" 或 "wmctrl"。它们通常包含在 "x11-utils"、"wmctrl" 等软件包中。缺少这些依赖时，RIFT 将无法检查角色在线状态。
                        """.trimIndent(),
                    ),
                )
            }
            if (isRunningOldVersionUseCase()) {
                add(
                    StartupWarning(
                        id = "old version",
                        title = "版本过旧",
                        description = """
                            你正在使用较旧版本的 RIFT。请在“关于”窗口查看版本信息并手动更新到最新版本。
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
