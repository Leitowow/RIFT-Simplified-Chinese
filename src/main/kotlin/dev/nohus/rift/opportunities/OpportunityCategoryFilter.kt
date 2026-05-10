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
    data object Feature : OpportunityCategoryFilterType("功能")
    data object CareerPath : OpportunityCategoryFilterType("职业路径")
    data object Activity : OpportunityCategoryFilterType("活动")
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
        name = "军团项目",
        type = OpportunityCategoryFilterType.Feature,
        description = "代表你的军团\n正在进行的项目。",
        icon = Res.drawable.corporation_management_16px,
    )

    data object FactionalWarfare : OpportunityCategoryFilter(
        order = 1,
        name = "势力战争",
        type = OpportunityCategoryFilterType.Feature,
        description = "你可代表已加入的势力\n参与势力战争地点，\n为战线提供支援。",
        icon = Res.drawable.flag_16px,
    )

    data object FreelanceJobs : OpportunityCategoryFilter(
        order = 2,
        name = "自由职业任务",
        type = OpportunityCategoryFilterType.Feature,
        description = "通过克隆飞行员发布的任务赚取 ISK、\n积累经验并与他人协作。",
        icon = Res.drawable.freelance_projects_16px,
    )

    data object Enforcer : OpportunityCategoryFilter(
        order = 3,
        name = "执法者",
        type = OpportunityCategoryFilterType.CareerPath,
        description = "适合专注“执法者”职业路径，\n或对对抗非克隆飞行员战斗\n感兴趣的机遇。",
        icon = Res.drawable.careerpaths_enforcer_16px,
    )

    data object Explorer : OpportunityCategoryFilter(
        order = 4,
        name = "探索者",
        type = OpportunityCategoryFilterType.CareerPath,
        description = "适合专注“探索者”职业路径，\n或对探索、扫描、破解\n感兴趣的机遇。",
        icon = Res.drawable.careerpaths_explorer_16px,
    )

    data object Industrialist : OpportunityCategoryFilter(
        order = 5,
        name = "工业家",
        type = OpportunityCategoryFilterType.CareerPath,
        description = "适合专注“工业家”职业路径，\n或对资源采集、制造、运输\n感兴趣的机遇。",
        icon = Res.drawable.careerpaths_industrialist_16px,
    )

    data object SoldierOfFortune : OpportunityCategoryFilter(
        order = 6,
        name = "雇佣兵",
        type = OpportunityCategoryFilterType.CareerPath,
        description = "适合专注“雇佣兵”职业路径，\n或对与其他克隆飞行员战斗\n感兴趣的机遇。",
        icon = Res.drawable.careerpaths_soldier_of_fortune_16px,
    )

    data object Combat : OpportunityCategoryFilter(
        order = 7,
        name = "战斗",
        type = OpportunityCategoryFilterType.Activity,
        description = "与敌对势力交战。",
        icon = Res.drawable.sword_16px,
    )

    data object CosmicSignatures : OpportunityCategoryFilter(
        order = 8,
        name = "宇宙信号",
        type = OpportunityCategoryFilterType.Activity,
        description = "需要先通过探针扫描定位，\n才能前往的地点。",
        icon = Res.drawable.pinpoint_probe_formation_32px,
    )

    data object Fleet : OpportunityCategoryFilter(
        order = 9,
        name = "舰队",
        type = OpportunityCategoryFilterType.Activity,
        description = "与其他克隆飞行员组成舰队，\n协作完成目标。",
    )

    data object Hauling : OpportunityCategoryFilter(
        order = 10,
        name = "运输",
        type = OpportunityCategoryFilterType.Activity,
        description = "将物品从一处运送到另一处。",
    )

    data object Logistics : OpportunityCategoryFilter(
        order = 11,
        name = "后勤",
        type = OpportunityCategoryFilterType.Activity,
        description = "使用远程模块为友方目标\n提供增益、维修或能量传输。",
    )

    data object Manufacturing : OpportunityCategoryFilter(
        order = 12,
        name = "制造",
        type = OpportunityCategoryFilterType.Activity,
        description = "使用蓝图生产物品。",
    )

    data object Mining : OpportunityCategoryFilter(
        order = 13,
        name = "采矿",
        type = OpportunityCategoryFilterType.Activity,
        description = "从小行星中开采矿石。",
        icon = Res.drawable.mining_16px,
    )
}
