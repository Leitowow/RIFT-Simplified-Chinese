package dev.nohus.rift.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.onClick
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import dev.nohus.rift.compose.EntityInteractionProvider.Interaction
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.di.koin
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.TypesRepository.Type

@Composable
fun ClickableSystem(
    system: String,
    content: @Composable () -> Unit,
) {
    val repository: SolarSystemsRepository = remember { koin.get() }
    val systemId = repository.getSystemId(system) ?: run {
        content()
        return
    }
    ClickableSystem(systemId, content)
}

@Composable
fun ClickableSystem(
    systemId: Int?,
    content: @Composable () -> Unit,
) {
    ClickableLocation(systemId, null, null, null, content)
}

@Composable
fun ClickableLocation(
    systemId: Int?,
    locationId: Long?,
    locationTypeId: Int?,
    locationName: String?,
    content: @Composable () -> Unit,
) {
    if (systemId == null) {
        content()
        return
    }
    val interactionProvider: EntityInteractionProvider = remember { koin.get() }
    val interaction = interactionProvider.getLocation(systemId, locationId, locationTypeId, locationName)
    ClickableEntity(interaction, content)
}

@Composable
fun ClickableCharacter(
    characterId: Int?,
    content: @Composable () -> Unit,
) {
    if (characterId == null) {
        content()
        return
    }
    val interactionProvider: EntityInteractionProvider = remember { koin.get() }
    val interaction = interactionProvider.getCharacter(characterId)
    ClickableEntity(interaction, content)
}

@Composable
fun ClickableCorporation(
    corporationId: Int?,
    content: @Composable () -> Unit,
) {
    if (corporationId == null) {
        content()
        return
    }
    val interactionProvider: EntityInteractionProvider = remember { koin.get() }
    val interaction = interactionProvider.getCorporation(corporationId)
    ClickableEntity(interaction, content)
}

@Composable
fun ClickableAlliance(
    allianceId: Int?,
    content: @Composable () -> Unit,
) {
    if (allianceId == null) {
        content()
        return
    }
    val interactionProvider: EntityInteractionProvider = remember { koin.get() }
    val interaction = interactionProvider.getAlliance(allianceId)
    ClickableEntity(interaction, content)
}

@Composable
fun ClickableShip(
    type: Type,
    content: @Composable () -> Unit,
) {
    val interactionProvider: EntityInteractionProvider = remember { koin.get() }
    val interaction = interactionProvider.getShip(type)
    ClickableEntity(interaction, content)
}

@Composable
fun ClickableType(
    type: Type,
    content: @Composable () -> Unit,
) {
    val interactionProvider: EntityInteractionProvider = remember { koin.get() }
    val interaction = interactionProvider.getType(type)
    ClickableEntity(interaction, content)
}

@Composable
private fun ClickableEntity(
    interaction: Interaction,
    content: @Composable () -> Unit,
) {
    RiftContextMenuArea(
        items = interaction.contextMenuItems,
    ) {
        ClickableEntity(
            onClick = interaction.onClick,
            content = content,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClickableEntity(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .pointerHoverIcon(PointerIcon(Cursors.pointerDropdown))
            .onClick { onClick() },
    ) {
        content()
    }
}
