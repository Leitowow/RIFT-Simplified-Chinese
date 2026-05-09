package dev.nohus.rift.opportunities

import dev.nohus.rift.characters.repositories.LocalCharactersRepository.LocalCharacter
import dev.nohus.rift.compose.text.FormattedText
import dev.nohus.rift.network.Result
import dev.nohus.rift.network.esi.models.Archetype
import dev.nohus.rift.network.esi.models.ConflictType
import dev.nohus.rift.network.esi.models.CorporationId
import dev.nohus.rift.network.esi.models.DockableLocation
import dev.nohus.rift.network.esi.models.Faction
import dev.nohus.rift.network.esi.models.Identity
import dev.nohus.rift.network.esi.models.Item
import dev.nohus.rift.network.esi.models.Location
import dev.nohus.rift.network.esi.models.OpportunityCareer
import dev.nohus.rift.network.esi.models.OpportunityState
import dev.nohus.rift.network.esi.models.OwnerType
import dev.nohus.rift.network.esi.models.ParticipationState
import dev.nohus.rift.network.esi.models.SignatureTypeId
import dev.nohus.rift.opportunities.GetOpportunityContributionAttributesUseCase.OpportunityContributionAttributeType
import dev.nohus.rift.repositories.SolarSystemChipState
import dev.nohus.rift.repositories.character.CharacterDetailsRepository.CharacterDetails
import java.time.Instant

data class Opportunity(
    val type: OpportunityType,
    val creator: Creator,
    val currentProgress: Long,
    val desiredProgress: Long,
    val id: String,
    val lastModified: Instant,
    val name: String,
    val reward: Reward?,
    val state: OpportunityState,
    val details: OpportunityDetails,
    val contributions: List<Contribution>,
    val contributors: Contributors,
    val eligibleCharacters: List<LocalCharacter>,
)

enum class OpportunityType {
    CorporationProject,
    FreelanceJob,
}

data class Reward(
    val initial: Double,
    val remaining: Double,
)

data class Creator(
    val characterId: Int,
    val characterName: String,
    val corporation: Corporation,
)

data class Corporation(
    val id: Int,
    val name: String,
)

data class Contribution(
    val characterId: Int,
    val characterName: String,
    val contribution: Result<Long>,
    val participationState: Result<ParticipationState>,
)

sealed interface Contributors {
    data class Available(val contributors: List<Contributor>) : Contributors
    data object Empty : Contributors
    data object NoAccess : Contributors
    data class Error(val message: String) : Contributors
}

data class Contributor(
    val characterId: Int,
    val details: CharacterDetails?,
    val contributed: Long,
    val participationState: ParticipationState,
)

data class OpportunityDetails(
    val debugDetails: String,
    val configuration: OpportunityConfiguration,
    val contributionAttributes: List<OpportunityContributionAttributeType>,
    val ageRequirement: AgeRequirement?,
    val solarSystemChipState: SolarSystemChipState?,
    val matchingFilters: List<OpportunityCategoryFilter>,
    val participationLimit: Long?,
    val rewardPerContribution: Double?,
    val submissionLimit: Long?,
    val submissionMultiplier: Double?,
    val career: OpportunityCareer,
    val created: Instant,
    val description: FormattedText,
    val expires: Instant?,
    val finished: Instant?,
)

data class AgeRequirement(
    val minimumAge: Int?,
    val maximumAge: Int?,
)

sealed interface OpportunityConfiguration {
    data class CaptureFwComplex(
        val archetypes: List<Archetype>?,
        val factions: List<Faction>?,
        val locations: List<Location>?,
    ) : OpportunityConfiguration

    data class DamageShip(
        val identities: List<Identity>?,
        val locations: List<Location>?,
        val ships: List<Item>?,
    ) : OpportunityConfiguration

    data class DefendFwComplex(
        val archetypes: List<Archetype>?,
        val factions: List<Faction>?,
        val locations: List<Location>?,
    ) : OpportunityConfiguration

    data class DeliverItem(
        val dockingLocations: List<DockableLocation>?,
        val items: List<Item>?,
        val officeId: Long?,
    ) : OpportunityConfiguration

    data class DestroyNpc(
        val locations: List<Location>?,
    ) : OpportunityConfiguration

    data class DestroyShip(
        val identities: List<Identity>?,
        val locations: List<Location>?,
        val ships: List<Item>?,
    ) : OpportunityConfiguration

    data class EarnLoyaltyPoint(
        val corporations: List<CorporationId>?,
    ) : OpportunityConfiguration

    data class ShipInsurance(
        val conflictType: ConflictType,
        val identities: List<Identity>?,
        val locations: List<Location>?,
        val reimburseImplants: Boolean,
        val ships: List<Item>?,
    ) : OpportunityConfiguration

    data class LostShip(
        val identities: List<Identity>?,
        val locations: List<Location>?,
        val ships: List<Item>?,
    ) : OpportunityConfiguration

    data object Manual : OpportunityConfiguration

    data class ManufactureItem(
        val dockingLocations: List<DockableLocation>?,
        val items: List<Item>?,
        val owner: OwnerType,
    ) : OpportunityConfiguration

    data class MineMaterial(
        val locations: List<Location>?,
        val materials: List<Item>?,
    ) : OpportunityConfiguration

    data class RemoteBoostShield(
        val identities: List<Identity>?,
        val locations: List<Location>?,
        val ships: List<Item>?,
    ) : OpportunityConfiguration

    data class RemoteRepairArmor(
        val identities: List<Identity>?,
        val locations: List<Location>?,
        val ships: List<Item>?,
    ) : OpportunityConfiguration

    data class SalvageWreck(
        val locations: List<Location>?,
    ) : OpportunityConfiguration

    data class ScanSignature(
        val locations: List<Location>?,
        val signatures: List<SignatureTypeId>?,
    ) : OpportunityConfiguration

    data class Unknown(
        val type: String,
    ) : OpportunityConfiguration
}
