package dev.nohus.rift.contacts

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.AsyncAllianceLogo
import dev.nohus.rift.compose.AsyncCharacterPortrait
import dev.nohus.rift.compose.AsyncCorporationLogo
import dev.nohus.rift.dynamicportraits.DynamicCharacterPortraitParallax

@Composable
fun EntityImage(entity: ContactsRepository.Entity, size: Int) {
    when (entity.type) {
        ContactsRepository.EntityType.Character -> {
            if (size >= 32) {
                DynamicCharacterPortraitParallax(
                    characterId = entity.id,
                    size = size.dp,
                    enterTimestamp = null,
                    pointerInteractionStateHolder = null,
                )
            } else {
                AsyncCharacterPortrait(
                    characterId = entity.id,
                    size = size.coerceAtLeast(32),
                    modifier = Modifier.size(size.dp),
                )
            }
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
