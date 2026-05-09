package dev.nohus.rift.opportunities

import androidx.compose.ui.text.AnnotatedString
import dev.nohus.rift.compose.RiftOpportunityCardCategory
import dev.nohus.rift.compose.RiftOpportunityCardType
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.contribution_method_attack_fw_complex_16px
import dev.nohus.rift.generated.resources.contribution_method_damage_ship_16px
import dev.nohus.rift.generated.resources.contribution_method_defend_fw_complex_16px
import dev.nohus.rift.generated.resources.contribution_method_deliver_item_16px
import dev.nohus.rift.generated.resources.contribution_method_destroy_npc_16px
import dev.nohus.rift.generated.resources.contribution_method_destroy_ship_16px
import dev.nohus.rift.generated.resources.contribution_method_earn_loyalty_point_16px
import dev.nohus.rift.generated.resources.contribution_method_lost_ship_16px
import dev.nohus.rift.generated.resources.contribution_method_manual_16px
import dev.nohus.rift.generated.resources.contribution_method_manufacture_item_16px
import dev.nohus.rift.generated.resources.contribution_method_mine_material_16px
import dev.nohus.rift.generated.resources.contribution_method_remote_boost_shields_16px
import dev.nohus.rift.generated.resources.contribution_method_remote_repair_armor_16px
import dev.nohus.rift.generated.resources.contribution_method_salvage_wreck_16px
import dev.nohus.rift.generated.resources.contribution_method_scan_signatures_16px
import dev.nohus.rift.generated.resources.contribution_method_ship_insurance_16px
import dev.nohus.rift.network.esi.models.BroadcastLocation
import dev.nohus.rift.network.esi.models.OpportunityCareer
import dev.nohus.rift.opportunities.GetOpportunityContributionAttributesUseCase.OpportunityContributionAttribute
import dev.nohus.rift.opportunities.GetOpportunityContributionAttributesUseCase.OpportunityContributionAttributeType
import dev.nohus.rift.repositories.GetSolarSystemChipStateUseCase
import dev.nohus.rift.repositories.SolarSystemChipLocation
import dev.nohus.rift.repositories.SolarSystemChipState
import org.jetbrains.compose.resources.DrawableResource
import java.math.BigInteger
import java.util.UUID

object OpportunitiesUtils {

    fun getOpportunityCategory(opportunity: Opportunity): RiftOpportunityCardCategory {
        return when (opportunity.details.career) {
            OpportunityCareer.Unspecified -> RiftOpportunityCardCategory.Unclassified
            OpportunityCareer.Explorer -> RiftOpportunityCardCategory.Explorer
            OpportunityCareer.Industrialist -> RiftOpportunityCardCategory.Industrialist
            OpportunityCareer.Enforcer -> RiftOpportunityCardCategory.Enforcer
            OpportunityCareer.SoldierOfFortune -> RiftOpportunityCardCategory.SoldierOfFortune
        }
    }

    data class OpportunityCategoryMetadata(
        val name: String,
        val icon: DrawableResource? = null,
        val tooltip: String? = null,
        val progressUnit: String?,
        val rewardPer: String?,
    )

    fun getOpportunityType(opportunity: Opportunity): RiftOpportunityCardType {
        val metadata = getOpportunityTypeMetadata(opportunity)
        return RiftOpportunityCardType(
            text = AnnotatedString(metadata.name),
            icon = metadata.icon,
            tooltip = metadata.tooltip,
        )
    }

    fun getOpportunityTypeMetadata(opportunity: Opportunity): OpportunityCategoryMetadata {
        return when (val configuration = opportunity.details.configuration) {
            is OpportunityConfiguration.CaptureFwComplex -> OpportunityCategoryMetadata(
                name = "Capture Factional Warfare Complexes",
                icon = Res.drawable.contribution_method_attack_fw_complex_16px,
                tooltip = "Capture complexes for your militia in Factional Warfare.",
                progressUnit = "Complexes captured",
                rewardPer = "complex captured",
            )

            is OpportunityConfiguration.DamageShip -> OpportunityCategoryMetadata(
                name = "Damage Capsuleers",
                icon = Res.drawable.contribution_method_damage_ship_16px,
                tooltip = "Damage ships piloted by capsuleers.",
                progressUnit = "Damage dealt",
                rewardPer = "point of damage",
            )

            is OpportunityConfiguration.DefendFwComplex -> OpportunityCategoryMetadata(
                name = "Defend Factional Warfare Complexes",
                icon = Res.drawable.contribution_method_defend_fw_complex_16px,
                tooltip = "Defend complexes for your militia in Factional Warfare.",
                progressUnit = "Complexes defended",
                rewardPer = "complex defended",
            )

            is OpportunityConfiguration.DeliverItem -> OpportunityCategoryMetadata(
                name = "Deliver",
                icon = Res.drawable.contribution_method_deliver_item_16px,
                tooltip = "Deliver items of a specific type\nto the Projects hangar in a\ncorporation office.",
                progressUnit = "Items delivered",
                rewardPer = "item delivered",
            )

            is OpportunityConfiguration.DestroyNpc -> OpportunityCategoryMetadata(
                name = "Destroy Non-Capsuleers",
                icon = Res.drawable.contribution_method_destroy_npc_16px,
                tooltip = "Destroy ships piloted by non-capsuleers.",
                progressUnit = "Non-capsuleers destroyed",
                rewardPer = "kill",
            )

            is OpportunityConfiguration.DestroyShip -> OpportunityCategoryMetadata(
                name = "Destroy Capsuleer's Ship",
                icon = Res.drawable.contribution_method_destroy_ship_16px,
                tooltip = "Destroy ships piloted by capsuleers.",
                progressUnit = "Capsuleers destroyed",
                rewardPer = "kill",
            )

            is OpportunityConfiguration.EarnLoyaltyPoint -> OpportunityCategoryMetadata(
                name = "Earn Loyalty Points",
                icon = Res.drawable.contribution_method_earn_loyalty_point_16px,
                tooltip = "Earn Loyalty Points from either any source or a specific corporation.",
                progressUnit = "Loyalty points earned",
                rewardPer = "loyalty point earned",
            )

            is OpportunityConfiguration.LostShip -> OpportunityCategoryMetadata(
                name = "Ship Loss to Capsuleers",
                icon = Res.drawable.contribution_method_lost_ship_16px,
                tooltip = "Receive a corporation ISK payout for losing a ship.",
                progressUnit = "ships lost",
                rewardPer = "ship lost",
            )

            OpportunityConfiguration.Manual -> OpportunityCategoryMetadata(
                name = "Manual",
                icon = Res.drawable.contribution_method_manual_16px,
                tooltip = "Manually update progress for any participation or measurement that can't be automatically tracked.",
                progressUnit = null,
                rewardPer = "unit of progress",
            )

            is OpportunityConfiguration.ManufactureItem -> OpportunityCategoryMetadata(
                name = "Manufacture",
                icon = Res.drawable.contribution_method_manufacture_item_16px,
                tooltip = "Install manufacturing jobs for items of a specific type.",
                progressUnit = "Items manufactured",
                rewardPer = "item manufactured",
            )

            is OpportunityConfiguration.MineMaterial -> OpportunityCategoryMetadata(
                name = "Mine Materials",
                icon = Res.drawable.contribution_method_mine_material_16px,
                tooltip = "Mine raw materials.",
                progressUnit = "Materials mined",
                rewardPer = "unit",
            )

            is OpportunityConfiguration.RemoteBoostShield -> OpportunityCategoryMetadata(
                name = "Remote Boost Shield",
                icon = Res.drawable.contribution_method_remote_boost_shields_16px,
                tooltip = "Remote boost a capsuleer's shields.",
                progressUnit = "HP boosted",
                rewardPer = "hit point boosted",
            )

            is OpportunityConfiguration.RemoteRepairArmor -> OpportunityCategoryMetadata(
                name = "Remote Repair Armor",
                icon = Res.drawable.contribution_method_remote_repair_armor_16px,
                tooltip = "Remote repair a capsuleer's armor.",
                progressUnit = "HP repaired",
                rewardPer = "hit point repaired",
            )

            is OpportunityConfiguration.SalvageWreck -> OpportunityCategoryMetadata(
                name = "Salvage Wrecks",
                icon = Res.drawable.contribution_method_salvage_wreck_16px,
                tooltip = "Successfully salvage wrecks of any kind.",
                progressUnit = "Wrecks salvaged",
                rewardPer = "wreck salvaged",
            )

            is OpportunityConfiguration.ScanSignature -> OpportunityCategoryMetadata(
                name = "Scan Signatures",
                icon = Res.drawable.contribution_method_scan_signatures_16px,
                tooltip = "Scan cosmic signatures to 100% resolution with a probe scanner.",
                progressUnit = "Signatures scanned",
                rewardPer = "signature scanned",
            )

            is OpportunityConfiguration.ShipInsurance -> OpportunityCategoryMetadata(
                name = "Ship Insurance",
                icon = Res.drawable.contribution_method_ship_insurance_16px,
                tooltip = "Receive ISK compensation for losing a ship, based on the Kill Report market value of the ship and its fitting.\n\nThis project provides an additional insurance paid by the corporation for the ships lost by its members. It works in addition to the ship insurance that capsuleers can purchase themselves in stations.",
                progressUnit = "Compensated",
                rewardPer = null,
            )

            is OpportunityConfiguration.Unknown -> OpportunityCategoryMetadata(
                name = configuration.type,
                tooltip = "Project of an unknown type.",
                progressUnit = null,
                rewardPer = null,
            )
        }
    }

    fun getMatchingFilters(
        baseType: OpportunityCategoryFilter,
        career: OpportunityCareer,
        configuration: OpportunityConfiguration?,
    ): List<OpportunityCategoryFilter> {
        return buildList {
            add(baseType)

            career.let {
                when (it) {
                    OpportunityCareer.Explorer -> OpportunityCategoryFilter.Explorer
                    OpportunityCareer.Industrialist -> OpportunityCategoryFilter.Industrialist
                    OpportunityCareer.Enforcer -> OpportunityCategoryFilter.Enforcer
                    OpportunityCareer.SoldierOfFortune -> OpportunityCategoryFilter.SoldierOfFortune
                    else -> null
                }
            }?.let { add(it) }

            when (configuration) {
                is OpportunityConfiguration.CaptureFwComplex -> listOf(OpportunityCategoryFilter.FactionalWarfare, OpportunityCategoryFilter.Combat)
                is OpportunityConfiguration.DamageShip -> listOf(OpportunityCategoryFilter.Combat)
                is OpportunityConfiguration.DefendFwComplex -> listOf(OpportunityCategoryFilter.FactionalWarfare, OpportunityCategoryFilter.Combat)
                is OpportunityConfiguration.DeliverItem -> listOf(OpportunityCategoryFilter.Hauling)
                is OpportunityConfiguration.DestroyNpc -> listOf(OpportunityCategoryFilter.Combat)
                is OpportunityConfiguration.DestroyShip -> listOf(OpportunityCategoryFilter.Combat)
                is OpportunityConfiguration.EarnLoyaltyPoint -> listOf()
                is OpportunityConfiguration.LostShip -> listOf(OpportunityCategoryFilter.Combat, OpportunityCategoryFilter.Fleet, OpportunityCategoryFilter.Logistics)
                OpportunityConfiguration.Manual -> listOf()
                is OpportunityConfiguration.ManufactureItem -> listOf(OpportunityCategoryFilter.Manufacturing)
                is OpportunityConfiguration.MineMaterial -> listOf(OpportunityCategoryFilter.Mining)
                is OpportunityConfiguration.RemoteBoostShield -> listOf(OpportunityCategoryFilter.Combat, OpportunityCategoryFilter.Fleet, OpportunityCategoryFilter.Logistics)
                is OpportunityConfiguration.RemoteRepairArmor -> listOf(OpportunityCategoryFilter.Combat, OpportunityCategoryFilter.Fleet, OpportunityCategoryFilter.Logistics)
                is OpportunityConfiguration.SalvageWreck -> listOf()
                is OpportunityConfiguration.ScanSignature -> listOf(OpportunityCategoryFilter.CosmicSignatures)
                is OpportunityConfiguration.ShipInsurance -> listOf(OpportunityCategoryFilter.Combat, OpportunityCategoryFilter.Fleet, OpportunityCategoryFilter.Logistics)
                is OpportunityConfiguration.Unknown -> listOf()
                null -> listOf()
            }.let { addAll(it) }
        }
    }

    fun getSolarSystemChipState(
        getSolarSystemChipStateUseCase: GetSolarSystemChipStateUseCase,
        contributionAttributes: List<OpportunityContributionAttributeType>,
        broadcastLocations: List<BroadcastLocation>? = null,
    ): SolarSystemChipState? {
        val attributeLocations = contributionAttributes.flatMap { it.values }.mapNotNull { value ->
            when (value) {
                is OpportunityContributionAttribute.SolarSystem -> SolarSystemChipLocation.SolarSystem(value.solarSystem.id)
                is OpportunityContributionAttribute.Constellation -> SolarSystemChipLocation.Constellation(value.constellation.id)
                is OpportunityContributionAttribute.Region -> SolarSystemChipLocation.Region(value.region.id)
                is OpportunityContributionAttribute.Station -> value.solarSystem?.id?.let { SolarSystemChipLocation.SolarSystem(it) }
                is OpportunityContributionAttribute.Structure -> value.solarSystem?.id?.let { SolarSystemChipLocation.SolarSystem(it) }
                else -> null
            }
        }
        val broadcastLocations = broadcastLocations?.map {
            SolarSystemChipLocation.SolarSystem(it.id.toInt())
        }
        val solarSystemChipLocations = attributeLocations + broadcastLocations.orEmpty()
        if (solarSystemChipLocations.isEmpty()) return null
        return getSolarSystemChipStateUseCase(solarSystemChipLocations)
    }

    fun uuidToInt128(uuid: String): String {
        val uuid = UUID.fromString(uuid)
        val mostSigBits = uuid.mostSignificantBits
        val leastSigBits = uuid.leastSignificantBits

        // Create an unsigned mask (2^64 - 1 using BigInteger)
        @Suppress("SpellCheckingInspection")
        val mask = BigInteger("FFFFFFFFFFFFFFFF", 16)

        // Apply the mask to convert signed long to unsigned BigInteger
        val high = BigInteger.valueOf(mostSigBits).and(mask)
        val low = BigInteger.valueOf(leastSigBits).and(mask)

        // Combine the high and low bits into a 128-bit integer
        return high.shiftLeft(64).or(low).toString()
    }

    fun int128ToUuid(int128: String): String {
        // Parse the 128-bit integer from the string
        val bigInt = BigInteger(int128)

        // Split into the most significant and least significant bits
        @Suppress("SpellCheckingInspection")
        val mask = BigInteger("FFFFFFFFFFFFFFFF", 16)
        val leastSigBits = bigInt.and(mask).toLong() // Extract lower 64 bits
        val mostSigBits = bigInt.shiftRight(64).and(mask).toLong() // Extract upper 64 bits

        // Create the UUID from the most and least significant bits
        return UUID(mostSigBits, leastSigBits).toString()
    }
}
