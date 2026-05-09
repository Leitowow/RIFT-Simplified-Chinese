package dev.nohus.rift.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import dev.nohus.rift.di.koin
import dev.nohus.rift.map.systemcolor.EntityColorRepository
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.utils.multiplyBrightness

data class CorporationColors(
    val primary: Color,
    val secondary: Color,
)

private val defaultColors = CorporationColors(Color.Transparent, Color.Transparent)

@Composable
fun produceCorporationColors(corporationId: Int): State<CorporationColors> {
    val entityColorRepository: EntityColorRepository = remember { koin.get() }
    val initial = remember(corporationId) {
        entityColorRepository.getCorporationColorOrNull(Originator.FreelanceJobs, corporationId)?.getCorporationColors()
    }
    return produceState(initialValue = initial ?: defaultColors, corporationId) {
        if (initial == null) {
            val color = entityColorRepository.getCorporationColor(Originator.FreelanceJobs, corporationId)
            if (color != null) {
                value = color.getCorporationColors()
            }
        }
    }
}

private fun Color.getCorporationColors() = CorporationColors(
    primary = this,
    secondary = this.multiplyBrightness(2.5f),
)
