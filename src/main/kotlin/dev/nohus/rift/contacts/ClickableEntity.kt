package dev.nohus.rift.contacts

import androidx.compose.runtime.Composable
import dev.nohus.rift.compose.ClickableAlliance
import dev.nohus.rift.compose.ClickableCorporation
import dev.nohus.rift.compose.ClickablePlayer
import dev.nohus.rift.contacts.ContactsRepository.EntityType

@Composable
fun ClickableEntity(id: Int, type: EntityType, content: @Composable () -> Unit) {
    when (type) {
        EntityType.Character -> ClickablePlayer(id, content)
        EntityType.Corporation -> ClickableCorporation(id, content)
        EntityType.Alliance -> ClickableAlliance(id, content)
        EntityType.Faction -> content()
    }
}
