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
                name = "占领势力战争据点",
                icon = Res.drawable.contribution_method_attack_fw_complex_16px,
                tooltip = "在势力战争中为你的民兵占领据点。",
                progressUnit = "已占领据点",
                rewardPer = "每占领 1 个据点",
            )

            is OpportunityConfiguration.DamageShip -> OpportunityCategoryMetadata(
                name = "对克隆飞行员造成伤害",
                icon = Res.drawable.contribution_method_damage_ship_16px,
                tooltip = "对克隆飞行员驾驶的舰船造成伤害。",
                progressUnit = "已造成伤害",
                rewardPer = "每点伤害",
            )

            is OpportunityConfiguration.DefendFwComplex -> OpportunityCategoryMetadata(
                name = "防守势力战争据点",
                icon = Res.drawable.contribution_method_defend_fw_complex_16px,
                tooltip = "在势力战争中为你的民兵防守据点。",
                progressUnit = "已防守据点",
                rewardPer = "每防守 1 个据点",
            )

            is OpportunityConfiguration.DeliverItem -> OpportunityCategoryMetadata(
                name = "交付",
                icon = Res.drawable.contribution_method_deliver_item_16px,
                tooltip = "将指定类型的物品交付到\n军团办公室中的项目机库。",
                progressUnit = "已交付物品",
                rewardPer = "每交付 1 件物品",
            )

            is OpportunityConfiguration.DestroyNpc -> OpportunityCategoryMetadata(
                name = "击毁非克隆飞行员",
                icon = Res.drawable.contribution_method_destroy_npc_16px,
                tooltip = "击毁非克隆飞行员驾驶的舰船。",
                progressUnit = "已击毁非克隆飞行员",
                rewardPer = "每次击毁",
            )

            is OpportunityConfiguration.DestroyShip -> OpportunityCategoryMetadata(
                name = "击毁克隆飞行员舰船",
                icon = Res.drawable.contribution_method_destroy_ship_16px,
                tooltip = "击毁克隆飞行员驾驶的舰船。",
                progressUnit = "已击毁克隆飞行员",
                rewardPer = "每次击毁",
            )

            is OpportunityConfiguration.EarnLoyaltyPoint -> OpportunityCategoryMetadata(
                name = "获取忠诚点",
                icon = Res.drawable.contribution_method_earn_loyalty_point_16px,
                tooltip = "从任意来源或指定军团获取忠诚点。",
                progressUnit = "已获得忠诚点",
                rewardPer = "每点忠诚点",
            )

            is OpportunityConfiguration.LostShip -> OpportunityCategoryMetadata(
                name = "被克隆飞行员击毁舰船",
                icon = Res.drawable.contribution_method_lost_ship_16px,
                tooltip = "舰船损失后可获得军团 ISK 补偿。",
                progressUnit = "已损失舰船",
                rewardPer = "每损失 1 艘舰船",
            )

            OpportunityConfiguration.Manual -> OpportunityCategoryMetadata(
                name = "手动",
                icon = Res.drawable.contribution_method_manual_16px,
                tooltip = "对无法自动追踪的参与或计量项目手动更新进度。",
                progressUnit = null,
                rewardPer = "每单位进度",
            )

            is OpportunityConfiguration.ManufactureItem -> OpportunityCategoryMetadata(
                name = "制造",
                icon = Res.drawable.contribution_method_manufacture_item_16px,
                tooltip = "为指定类型物品安装制造作业。",
                progressUnit = "已制造物品",
                rewardPer = "每制造 1 件物品",
            )

            is OpportunityConfiguration.MineMaterial -> OpportunityCategoryMetadata(
                name = "开采材料",
                icon = Res.drawable.contribution_method_mine_material_16px,
                tooltip = "开采原材料。",
                progressUnit = "已开采材料",
                rewardPer = "每单位",
            )

            is OpportunityConfiguration.RemoteBoostShield -> OpportunityCategoryMetadata(
                name = "远程护盾增效",
                icon = Res.drawable.contribution_method_remote_boost_shields_16px,
                tooltip = "远程增强克隆飞行员目标的护盾。",
                progressUnit = "已增效护盾值",
                rewardPer = "每点增效值",
            )

            is OpportunityConfiguration.RemoteRepairArmor -> OpportunityCategoryMetadata(
                name = "远程装甲维修",
                icon = Res.drawable.contribution_method_remote_repair_armor_16px,
                tooltip = "远程维修克隆飞行员目标的装甲。",
                progressUnit = "已维修装甲值",
                rewardPer = "每点维修值",
            )

            is OpportunityConfiguration.SalvageWreck -> OpportunityCategoryMetadata(
                name = "打捞残骸",
                icon = Res.drawable.contribution_method_salvage_wreck_16px,
                tooltip = "成功打捞任意类型的残骸。",
                progressUnit = "已打捞残骸",
                rewardPer = "每个残骸",
            )

            is OpportunityConfiguration.ScanSignature -> OpportunityCategoryMetadata(
                name = "扫描信号",
                icon = Res.drawable.contribution_method_scan_signatures_16px,
                tooltip = "使用探针扫描器将宇宙信号解析到 100%。",
                progressUnit = "已扫描信号",
                rewardPer = "每个信号",
            )

            is OpportunityConfiguration.ShipInsurance -> OpportunityCategoryMetadata(
                name = "舰船保险",
                icon = Res.drawable.contribution_method_ship_insurance_16px,
                tooltip = "舰船损失后可获得 ISK 补偿，依据击毁报告中的舰船与装备市场价值计算。\n\n该项目为军团成员损失舰船提供军团额外保险，且与克隆飞行员在空间站自行购买的舰船保险叠加生效。",
                progressUnit = "已补偿",
                rewardPer = null,
            )

            is OpportunityConfiguration.Unknown -> OpportunityCategoryMetadata(
                name = configuration.type,
                tooltip = "未知类型的项目。",
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
