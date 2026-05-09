package dev.nohus.rift.opportunities

import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.careerpaths_enforcer_16px
import dev.nohus.rift.generated.resources.careerpaths_explorer_16px
import dev.nohus.rift.generated.resources.careerpaths_industrialist_16px
import dev.nohus.rift.generated.resources.careerpaths_soldier_of_fortune_16px
import dev.nohus.rift.generated.resources.corporation_management_16px
import dev.nohus.rift.generated.resources.flag_16px
import dev.nohus.rift.generated.resources.freelance_projects_16px
import dev.nohus.rift.generated.resources.mining_16px
import dev.nohus.rift.generated.resources.pinpoint_probe_formation_32px
import dev.nohus.rift.generated.resources.sword_16px
import org.jetbrains.compose.resources.DrawableResource

sealed class OpportunityCategoryFilterType(val name: String) {
    data object Feature : OpportunityCategoryFilterType("Feature")
    data object CareerPath : OpportunityCategoryFilterType("Career Path")
    data object Activity : OpportunityCategoryFilterType("Activity")
}

sealed class OpportunityCategoryFilter(
    val order: Int,
    val name: String,
    val type: OpportunityCategoryFilterType,
    val description: String,
    val icon: DrawableResource? = null,
) {
    data object CorporationProjects : OpportunityCategoryFilter(
        order = 0,
        name = "Corporation Projects",
        type = OpportunityCategoryFilterType.Feature,
        description = "Projects being run on behalf of your\ncorporation.",
        icon = Res.drawable.corporation_management_16px,
    )

    data object FactionalWarfare : OpportunityCategoryFilter(
        order = 1,
        name = "Factional Warfare",
        type = OpportunityCategoryFilterType.Feature,
        description = "Factional Warfare sites where you can support\nthe war effort on behalf of a faction you are\nenlisted with.",
        icon = Res.drawable.flag_16px,
    )

    data object FreelanceJobs : OpportunityCategoryFilter(
        order = 2,
        name = "Freelance Jobs",
        type = OpportunityCategoryFilterType.Feature,
        description = "Earn ISK, gain experience, and connect with\nothers through capsuleer-made jobs.",
        icon = Res.drawable.freelance_projects_16px,
    )

    data object Enforcer : OpportunityCategoryFilter(
        order = 3,
        name = "Enforcer",
        type = OpportunityCategoryFilterType.CareerPath,
        description = "Opportunities for those focused on the\nEnforcer Career path or who are interested in\nCombat against non-capsuleers.",
        icon = Res.drawable.careerpaths_enforcer_16px,
    )

    data object Explorer : OpportunityCategoryFilter(
        order = 4,
        name = "Explorer",
        type = OpportunityCategoryFilterType.CareerPath,
        description = "Opportunities for those focused on the\nExplorer Career path or who are interested in\nexploration, scanning, or hacking.",
        icon = Res.drawable.careerpaths_explorer_16px,
    )

    data object Industrialist : OpportunityCategoryFilter(
        order = 5,
        name = "Industrialist",
        type = OpportunityCategoryFilterType.CareerPath,
        description = "Opportunities for those focused on the\nIndustrialist Career path or who are interested\nin resource gathering, manufacturing, or hauling.",
        icon = Res.drawable.careerpaths_industrialist_16px,
    )

    data object SoldierOfFortune : OpportunityCategoryFilter(
        order = 6,
        name = "Soldier of Fortune",
        type = OpportunityCategoryFilterType.CareerPath,
        description = "Opportunities for those focused on the\nSoldier of Fortune Career path or who are interested\nin combat against other capsuleers.",
        icon = Res.drawable.careerpaths_soldier_of_fortune_16px,
    )

    data object Combat : OpportunityCategoryFilter(
        order = 7,
        name = "Combat",
        type = OpportunityCategoryFilterType.Activity,
        description = "Engaging with hostile forces.",
        icon = Res.drawable.sword_16px,
    )

    data object CosmicSignatures : OpportunityCategoryFilter(
        order = 8,
        name = "Cosmic Signatures",
        type = OpportunityCategoryFilterType.Activity,
        description = "A site that needs to be located by probe\nscanning before you can travel to it.",
        icon = Res.drawable.pinpoint_probe_formation_32px,
    )

    data object Fleet : OpportunityCategoryFilter(
        order = 9,
        name = "Fleet",
        type = OpportunityCategoryFilterType.Activity,
        description = "Form a fleet with other capsuleers to\ncooperate and complete objectives.",
    )

    data object Hauling : OpportunityCategoryFilter(
        order = 10,
        name = "Hauling",
        type = OpportunityCategoryFilterType.Activity,
        description = "Transporting items from location to location.",
    )

    data object Logistics : OpportunityCategoryFilter(
        order = 11,
        name = "Logistics",
        type = OpportunityCategoryFilterType.Activity,
        description = "Using remote modules to boost, repair, or\ntransfer energy to friendly targets.",
    )

    data object Manufacturing : OpportunityCategoryFilter(
        order = 12,
        name = "Manufacturing",
        type = OpportunityCategoryFilterType.Activity,
        description = "Using blueprints to produce items.",
    )

    data object Mining : OpportunityCategoryFilter(
        order = 13,
        name = "Mining",
        type = OpportunityCategoryFilterType.Activity,
        description = "Harvesting ore from asteroids.",
        icon = Res.drawable.mining_16px,
    )
}
