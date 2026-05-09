package dev.nohus.rift.utils.openwindows

import dev.nohus.rift.utils.CommandRunner
import dev.nohus.rift.utils.IsLinuxBinaryFoundUseCase
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class LinuxGetOpenEveClientsUseCase(
    private val commandRunner: CommandRunner,
    private val isLinuxBinaryFound: IsLinuxBinaryFoundUseCase,
) : GetOpenEveClientsUseCase {

    enum class Method {
        XWinInfo,
        WMCtrl,
    }

    private val method = detectAvailableMethod()

    private val xWinInfoRegex = """ "EVE - (?<character>[A-z0-9 '-]{3,37})": """.toRegex()
    private val wmCtrlRegex = """ EVE - (?<character>[A-z0-9 '-]{3,37})""".toRegex()

    private fun detectAvailableMethod(): Method? {
        return when {
            isLinuxBinaryFound("wmctrl") -> Method.WMCtrl
            isLinuxBinaryFound("xwininfo") -> Method.XWinInfo
            else -> null
        }
    }

    override fun invoke(): List<String>? {
        return try {
            when (method) {
                Method.XWinInfo -> getUsingXWinInfo()
                Method.WMCtrl -> getUsingWMCtrl()
                null -> null
            }
        } catch (e: IllegalStateException) {
            logger.error(e) { "Could not get open Eve clients" }
            return null
        }
    }

    private fun getUsingWMCtrl(): List<String>? {
        val output = commandRunner.run("wmctrl", "-l").output
        return output.lines().mapNotNull { line ->
            val match = wmCtrlRegex.find(line) ?: return@mapNotNull null
            match.groups["character"]!!.value
        }
    }

    private fun getUsingXWinInfo(): List<String>? {
        val output = commandRunner.run("xwininfo", "-root", "-children", "-tree").output
        return output.lines().mapNotNull { line ->
            val match = xWinInfoRegex.find(line) ?: return@mapNotNull null
            match.groups["character"]!!.value
        }
    }
}
