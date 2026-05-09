package dev.nohus.rift.opportunities

import dev.nohus.rift.characters.repositories.LocalCharactersRepository.LocalCharacter
import dev.nohus.rift.compose.text.ParseEveFormattedTextUseCase
import dev.nohus.rift.network.esi.models.Archetype
import dev.nohus.rift.network.esi.models.ConflictType
import dev.nohus.rift.network.esi.models.CorporationProject
import dev.nohus.rift.network.esi.models.CorporationProjectConfiguration
import dev.nohus.rift.network.esi.models.CorporationsIdProjectsId
import dev.nohus.rift.network.esi.models.DockableLocation
import dev.nohus.rift.network.esi.models.Faction
import dev.nohus.rift.network.esi.models.FreelanceJob
import dev.nohus.rift.network.esi.models.FreelanceJobConfiguration
import dev.nohus.rift.network.esi.models.FreelanceJobParameter
import dev.nohus.rift.network.esi.models.FreelanceJobsId
import dev.nohus.rift.network.esi.models.Identity
import dev.nohus.rift.network.esi.models.Item
import dev.nohus.rift.network.esi.models.Location
import dev.nohus.rift.opportunities.GetOpportunityContributionAttributesUseCase.OpportunityContributionAttributeType
import dev.nohus.rift.repositories.SolarSystemChipState
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single

private val logger = KotlinLogging.logger {}

@Single
class OpportunitiesMapper(
    val parseEveFormattedTextUseCase: ParseEveFormattedTextUseCase,
) {

    fun toModel(
        debugDetails: String,
        job: FreelanceJob,
        details: FreelanceJobsId,
        configuration: OpportunityConfiguration,
        contributionAttributes: List<OpportunityContributionAttributeType>,
        solarSystemChipState: SolarSystemChipState?,
        matchingFilters: List<OpportunityCategoryFilter>,
        contributions: List<Contribution>,
        contributors: Contributors,
        eligibleCharacters: List<LocalCharacter>,
    ): Opportunity {
        val creator = details.details.creator.let {
            Creator(
                characterId = it.character.id.toInt(),
                characterName = it.character.name,
                corporation = Corporation(it.corporation.id.toInt(), it.corporation.name),
            )
        }
        val details = details.let {
            val ageRequirement = it.accessAndVisibility.restrictions?.let { restrictions ->
                AgeRequirement(restrictions.minimumAge?.toInt(), restrictions.maximumAge?.toInt())
            }
            OpportunityDetails(
                debugDetails = debugDetails,
                configuration = configuration,
                contributionAttributes = contributionAttributes,
                ageRequirement = ageRequirement,
                solarSystemChipState = solarSystemChipState,
                matchingFilters = matchingFilters,
                participationLimit = it.contributionSettings?.participationLimit,
                rewardPerContribution = it.contributionSettings?.rewardPerContribution,
                submissionLimit = it.contributionSettings?.submissionLimit,
                submissionMultiplier = it.contributionSettings?.submissionMultiplier,
                career = it.details.career,
                created = it.details.created,
                description = parseEveFormattedTextUseCase(it.details.description),
                expires = it.details.expires,
                finished = it.details.finished,
            )
        }
        return Opportunity(
            type = OpportunityType.FreelanceJob,
            creator = creator,
            currentProgress = job.progress.current,
            desiredProgress = job.progress.desired,
            id = job.id,
            lastModified = job.lastModified,
            name = job.name,
            reward = job.reward?.let { Reward(it.initial, it.remaining) },
            state = job.state,
            details = details,
            contributions = contributions,
            contributors = contributors,
            eligibleCharacters = eligibleCharacters,
        )
    }

    fun toModel(
        debugDetails: String,
        corporation: Corporation,
        project: CorporationProject,
        details: CorporationsIdProjectsId,
        configuration: OpportunityConfiguration,
        contributionAttributes: List<OpportunityContributionAttributeType>,
        solarSystemChipState: SolarSystemChipState?,
        matchingFilters: List<OpportunityCategoryFilter>,
        contributions: List<Contribution>,
        contributors: Contributors,
        eligibleCharacters: List<LocalCharacter>,
    ): Opportunity {
        val creator = details.creator.let {
            Creator(
                characterId = it.id,
                characterName = it.name,
                corporation = corporation,
            )
        }
        val details = details.let {
            OpportunityDetails(
                debugDetails = debugDetails,
                configuration = configuration,
                contributionAttributes = contributionAttributes,
                ageRequirement = null,
                solarSystemChipState = solarSystemChipState,
                matchingFilters = matchingFilters,
                participationLimit = it.contributionSettings?.participationLimit,
                rewardPerContribution = it.contributionSettings?.rewardPerContribution,
                submissionLimit = it.contributionSettings?.submissionLimit,
                submissionMultiplier = it.contributionSettings?.submissionMultiplier,
                career = it.details.career,
                created = it.details.created,
                description = parseEveFormattedTextUseCase(it.details.description),
                expires = it.details.expires,
                finished = it.details.finished,
            )
        }
        return Opportunity(
            type = OpportunityType.CorporationProject,
            creator = creator,
            currentProgress = project.progress.current,
            desiredProgress = project.progress.desired,
            id = project.id,
            lastModified = project.lastModified,
            name = project.name,
            reward = project.reward?.let { Reward(it.initial, it.remaining) },
            state = project.state,
            details = details,
            contributions = contributions,
            contributors = contributors,
            eligibleCharacters = eligibleCharacters,
        )
    }

    fun toModel(configuration: FreelanceJobConfiguration): OpportunityConfiguration {
        if (configuration.version != 1L) {
            return OpportunityConfiguration.Unknown(type = "${configuration.method}-v${configuration.version}")
        }
        return try {
            when (configuration.method) {
                "BoostShield" -> OpportunityConfiguration.RemoteBoostShield(
                    identities = getIdentitiesParameter(configuration.parameters, "boosted_identity"),
                    locations = getLocationParameter(configuration.parameters),
                    ships = getShipParameter(configuration.parameters, "boosted_ship"),
                )
                "CaptureFWComplex" -> OpportunityConfiguration.CaptureFwComplex(
                    archetypes = getArchetypeParameter(configuration.parameters),
                    factions = getFactionParameter(configuration.parameters),
                    locations = getLocationParameter(configuration.parameters),
                )
                "DamageShip" -> OpportunityConfiguration.DamageShip(
                    identities = getIdentitiesParameter(configuration.parameters, "damaged_identity"),
                    locations = getLocationParameter(configuration.parameters),
                    ships = getShipParameter(configuration.parameters, "damaged_ship"),
                )
                "DefendFWComplex" -> OpportunityConfiguration.DefendFwComplex(
                    archetypes = getArchetypeParameter(configuration.parameters),
                    factions = getFactionParameter(configuration.parameters),
                    locations = getLocationParameter(configuration.parameters),
                )
                "DeliverItem" -> OpportunityConfiguration.DeliverItem(
                    dockingLocations = getDeliveryDockableLocationParameter(configuration.parameters),
                    items = getDeliveryItemParameter(configuration.parameters),
                    officeId = null,
                )
                "KillCapsuleer" -> OpportunityConfiguration.DestroyShip(
                    identities = getIdentitiesParameter(configuration.parameters, "destroyed_identity"),
                    locations = getLocationParameter(configuration.parameters),
                    ships = getShipParameter(configuration.parameters, "destroyed_ship"),
                )
                "KillNPC" -> OpportunityConfiguration.DestroyNpc(
                    locations = getLocationParameter(configuration.parameters),
                )
                "MineOre" -> OpportunityConfiguration.MineMaterial(
                    locations = getLocationParameter(configuration.parameters),
                    materials = getOreParameter(configuration.parameters),
                )
                "RepairArmor" -> OpportunityConfiguration.RemoteRepairArmor(
                    identities = getIdentitiesParameter(configuration.parameters, "repaired_identity"),
                    locations = getLocationParameter(configuration.parameters),
                    ships = getShipParameter(configuration.parameters, "repaired_ship"),
                )
                "ShipInsurance" -> OpportunityConfiguration.ShipInsurance(
                    conflictType = if (configuration.parameters["require_capsuleer_involvement"]?.boolean?.value == true) ConflictType.Pvp else ConflictType.Any,
                    identities = getIdentitiesParameter(configuration.parameters, "attacker_identity"),
                    locations = getLocationParameter(configuration.parameters),
                    reimburseImplants = configuration.parameters["cover_implants"]?.boolean?.value == true,
                    ships = getShipParameter(configuration.parameters, "victim_ship"),
                )
                else -> OpportunityConfiguration.Unknown(type = configuration.method)
            }
        } catch (e: IllegalArgumentException) {
            logger.error { "Failed to parse opportunity configuration: ${e.message}" }
            OpportunityConfiguration.Unknown(type = configuration.method)
        }
    }

    private fun getLocationParameter(parameters: Map<String, FreelanceJobParameter>): List<Location>? {
        parameters["location"]?.matcher?.let { matcher ->
            return matcher.values.flatMap { value ->
                when (value.type) {
                    "solarsystem" -> value.values.map { Location(solarSystemId = it.toLong()) }
                    "constellation" -> value.values.map { Location(constellationId = it.toLong()) }
                    "region" -> value.values.map { Location(regionId = it.toLong()) }
                    else -> throw IllegalArgumentException("Unknown location type ${value.type}")
                }
            }.takeIf { it.isNotEmpty() }
        }
        return null
    }

    private fun getDeliveryDockableLocationParameter(parameters: Map<String, FreelanceJobParameter>): List<DockableLocation>? {
        parameters["corporation_item_delivery"]?.corporationItemDelivery?.let { delivery ->
            return delivery.corporationOfficeLocation.values.flatMap { value ->
                when (value.type) {
                    "station" -> value.values.map { DockableLocation(stationId = it.toLong()) }
                    "structure" -> value.values.map { DockableLocation(structureId = it.toLong()) }
                    else -> throw IllegalArgumentException("Unknown delivery location type ${value.type}")
                }
            }.takeIf { it.isNotEmpty() }
        }
        return null
    }

    private fun getDeliveryItemParameter(parameters: Map<String, FreelanceJobParameter>): List<Item>? {
        parameters["corporation_item_delivery"]?.corporationItemDelivery?.let { delivery ->
            return delivery.itemType.values.flatMap { value ->
                when (value.type) {
                    "item_type" -> value.values.map { Item(typeId = it.toLong()) }
                    "item_group" -> value.values.map { Item(groupId = it.toLong()) }
                    "item_category" -> {
                        // This is not in spec, but old jobs might have it
                        emptyList()
                    }
                    else -> throw IllegalArgumentException("Unknown delivery item type ${value.type}, $parameters")
                }
            }.takeIf { it.isNotEmpty() }
        }
        return null
    }

    private fun getShipParameter(parameters: Map<String, FreelanceJobParameter>, key: String): List<Item>? {
        parameters[key]?.matcher?.let { matcher ->
            return matcher.values.flatMap { value ->
                when (value.type) {
                    "ship_type" -> value.values.map { Item(typeId = it.toLong()) }
                    "ship_class" -> value.values.map { Item(groupId = it.toLong()) }
                    else -> throw IllegalArgumentException("Unknown ship type ${value.type}")
                }
            }.takeIf { it.isNotEmpty() }
        }
        return null
    }

    private fun getOreParameter(parameters: Map<String, FreelanceJobParameter>): List<Item>? {
        parameters["ore"]?.matcher?.let { matcher ->
            return matcher.values.flatMap { value ->
                when (value.type) {
                    "ore_type" -> value.values.map { Item(typeId = it.toLong()) }
                    "ore_group" -> value.values.map { Item(groupId = it.toLong()) }
                    else -> throw IllegalArgumentException("Unknown ore type ${value.type}")
                }
            }.takeIf { it.isNotEmpty() }
        }
        return null
    }

    private fun getIdentitiesParameter(parameters: Map<String, FreelanceJobParameter>, key: String): List<Identity>? {
        parameters[key]?.matcher?.let { matcher ->
            return matcher.values.flatMap { value ->
                when (value.type) {
                    "character" -> value.values.map { Identity(characterId = it.toLong()) }
                    "corporation" -> value.values.map { Identity(corporationId = it.toLong()) }
                    "alliance" -> value.values.map { Identity(allianceId = it.toLong()) }
                    "faction" -> value.values.map { Identity(factionId = it.toLong()) }
                    else -> throw IllegalArgumentException("Unknown identity type ${value.type}")
                }
            }.takeIf { it.isNotEmpty() }
        }
        return null
    }

    private fun getFactionParameter(parameters: Map<String, FreelanceJobParameter>): List<Faction>? {
        parameters["empire_faction"]?.matcher?.let { matcher ->
            return matcher.values.flatMap { value ->
                when (value.type) {
                    "faction" -> value.values.map { Faction(factionId = it.toLong()) }
                    else -> throw IllegalArgumentException("Unknown faction type ${value.type}")
                }
            }.takeIf { it.isNotEmpty() }
        }
        return null
    }

    private fun getArchetypeParameter(parameters: Map<String, FreelanceJobParameter>): List<Archetype>? {
        parameters["fw_complex_type"]?.matcher?.let { matcher ->
            return matcher.values.flatMap { value ->
                when (value.type) {
                    "archetype" -> value.values.map { Archetype(archetypeId = it.toLong()) }
                    else -> throw IllegalArgumentException("Unknown archetype type ${value.type}")
                }
            }.takeIf { it.isNotEmpty() }
        }
        return null
    }

    fun toModel(configuration: CorporationProjectConfiguration): OpportunityConfiguration = with(configuration) {
        captureFwComplex?.let {
            return OpportunityConfiguration.CaptureFwComplex(
                archetypes = it.archetypes,
                factions = it.factions,
                locations = it.locations,
            )
        }
        damageShip?.let {
            return OpportunityConfiguration.DamageShip(
                identities = it.identities,
                locations = it.locations,
                ships = it.ships,
            )
        }
        defendFwComplex?.let {
            return OpportunityConfiguration.DefendFwComplex(
                archetypes = it.archetypes,
                factions = it.factions,
                locations = it.locations,
            )
        }
        deliverItem?.let {
            return OpportunityConfiguration.DeliverItem(
                dockingLocations = it.dockingLocations,
                items = it.items,
                officeId = it.officeId,
            )
        }
        destroyNpc?.let {
            return OpportunityConfiguration.DestroyNpc(
                locations = it.locations,
            )
        }
        destroyShip?.let {
            return OpportunityConfiguration.DestroyShip(
                identities = it.identities,
                locations = it.locations,
                ships = it.ships,
            )
        }
        earnLoyaltyPoint?.let {
            return OpportunityConfiguration.EarnLoyaltyPoint(
                corporations = it.corporations,
            )
        }
        shipInsurance?.let {
            return OpportunityConfiguration.ShipInsurance(
                conflictType = it.conflictType,
                identities = it.identities,
                locations = it.locations,
                reimburseImplants = it.reimburseImplants,
                ships = it.ships,
            )
        }
        lostShip?.let {
            return OpportunityConfiguration.LostShip(
                identities = it.identities,
                locations = it.locations,
                ships = it.ships,
            )
        }
        manual?.let {
            return OpportunityConfiguration.Manual
        }
        manufactureItem?.let {
            return OpportunityConfiguration.ManufactureItem(
                dockingLocations = it.dockingLocations,
                items = it.items,
                owner = it.owner,
            )
        }
        mineMaterial?.let {
            return OpportunityConfiguration.MineMaterial(
                locations = it.locations,
                materials = it.materials,
            )
        }
        remoteBoostShield?.let {
            return OpportunityConfiguration.RemoteBoostShield(
                identities = it.identities,
                locations = it.locations,
                ships = it.ships,
            )
        }
        remoteRepairArmor?.let {
            return OpportunityConfiguration.RemoteRepairArmor(
                identities = it.identities,
                locations = it.locations,
                ships = it.ships,
            )
        }
        salvageWreck?.let {
            return OpportunityConfiguration.SalvageWreck(
                locations = it.locations,
            )
        }
        scanSignature?.let {
            return OpportunityConfiguration.ScanSignature(
                locations = it.locations,
                signatures = it.signatures,
            )
        }
        unknown?.let {
            return OpportunityConfiguration.Unknown(
                type = it.type,
            )
        }
        return OpportunityConfiguration.Unknown(type = "Unknown")
    }
}
