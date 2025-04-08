package dev.nohus.rift.utils

import org.koin.core.annotation.Single

@Single
class IsLinuxBinaryFoundUseCase(
    private val commandRunner: CommandRunner,
) {

    operator fun invoke(binary: String): Boolean {
        return commandRunner.run("which", binary).exitStatus == 0
    }
}
