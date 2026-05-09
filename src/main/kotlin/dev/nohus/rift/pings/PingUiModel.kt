package dev.nohus.rift.pings

import dev.nohus.rift.compose.RiftOpportunityCardCategory
import dev.nohus.rift.compose.RiftOpportunityCardTopRight.RiftOpportunityCardCharacter
import dev.nohus.rift.repositories.SolarSystemChipState
import java.time.Instant

sealed class PingUiModel(
    open val timestamp: Instant,
    open val sourceText: String,
) {
    data class PlainText(
        override val timestamp: Instant,
        override val sourceText: String,
        val text: String,
        val sender: String?,
        val target: String?,
    ) : PingUiModel(timestamp, sourceText)
    data class FleetPing(
        override val timestamp: Instant,
        override val sourceText: String,
        val opportunityCategory: RiftOpportunityCardCategory,
        val description: String,
        val fleetCommander: RiftOpportunityCardCharacter,
        val fleet: String?,
        val formupLocations: SolarSystemChipState,
        val papType: PapType?,
        val comms: Comms?,
        val doctrine: Doctrine?,
        val target: String?,
    ) : PingUiModel(timestamp, sourceText)
}
