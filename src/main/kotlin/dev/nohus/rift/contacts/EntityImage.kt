package dev.nohus.rift.contacts

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.AsyncAllianceLogo
import dev.nohus.rift.compose.AsyncCorporationLogo
import dev.nohus.rift.compose.AsyncPlayerPortrait

@Composable
fun EntityImage(entity: ContactsRepository.Entity, size: Int) {
    when (entity.type) {
        ContactsRepository.EntityType.Character -> {
            AsyncPlayerPortrait(
                characterId = entity.id,
                size = size.coerceAtLeast(32),
                modifier = Modifier.size(size.dp),
            )
        }

        ContactsRepository.EntityType.Corporation -> {
            AsyncCorporationLogo(
                corporationId = entity.id,
                size = size.coerceAtLeast(32),
                modifier = Modifier.size(size.dp),
            )
        }

        ContactsRepository.EntityType.Alliance -> {
            AsyncAllianceLogo(
                allianceId = entity.id,
                size = size.coerceAtLeast(32),
                modifier = Modifier.size(size.dp),
            )
        }

        ContactsRepository.EntityType.Faction -> {
            AsyncCorporationLogo(
                corporationId = entity.id,
                size = size.coerceAtLeast(32),
                modifier = Modifier.size(size.dp),
            )
        }
    }
}
