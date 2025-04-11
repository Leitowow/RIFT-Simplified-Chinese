package dev.nohus.rift.about

import dev.hydraulic.conveyor.control.SoftwareUpdateController
import dev.nohus.rift.about.UpdateController.UpdateAvailability.NO_UPDATE
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

private val logger = KotlinLogging.logger {}

@Single
class UpdateController {

    private val controller: SoftwareUpdateController? = null // 禁用更新控制器

    enum class UpdateAvailability {
        NOT_PACKAGED, UNKNOWN, NO_UPDATE, UPDATE_MANUAL, UPDATE_AUTOMATIC
    }

    suspend fun isUpdateAvailable(): UpdateAvailability = withContext(Dispatchers.IO) {
        // 始终返回 NO_UPDATE，禁用更新检查
        return@withContext NO_UPDATE
    }

    fun triggerUpdate() {
        // 禁用更新触发
    }
}
