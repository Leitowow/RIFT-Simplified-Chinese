package dev.nohus.rift.startupwarning

import dev.nohus.rift.utils.IsLinuxBinaryFoundUseCase
import dev.nohus.rift.utils.OperatingSystem
import org.koin.core.annotation.Single

@Single
class IsMissingXWinInfoUseCase(
    private val operatingSystem: OperatingSystem,
    private val isLinuxBinaryFound: IsLinuxBinaryFoundUseCase,
) {

    operator fun invoke(): Boolean {
        if (operatingSystem != OperatingSystem.Linux) return false
        return !(isLinuxBinaryFound("xwininfo") && isLinuxBinaryFound("xprop"))
    }
}
