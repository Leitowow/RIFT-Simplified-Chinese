package dev.nohus.rift.opportunities.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlendModeColorFilter
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.nohus.rift.characters.repositories.LocalCharactersRepository.LocalCharacter
import dev.nohus.rift.compose.AsyncAllianceLogo
import dev.nohus.rift.compose.AsyncCharacterPortrait
import dev.nohus.rift.compose.AsyncCorporationLogo
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ClickableAlliance
import dev.nohus.rift.compose.ClickableCharacter
import dev.nohus.rift.compose.ClickableCorporation
import dev.nohus.rift.compose.LinkText
import dev.nohus.rift.compose.MulticolorIconType
import dev.nohus.rift.compose.PointerInteractionStateHolder
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftCircularCharacterPortrait
import dev.nohus.rift.compose.RiftImageButton
import dev.nohus.rift.compose.RiftMulticolorIcon
import dev.nohus.rift.compose.RiftOpportunityContainerFlair
import dev.nohus.rift.compose.RiftOpportunityTypeIcon
import dev.nohus.rift.compose.RiftSolarSystemChip
import dev.nohus.rift.compose.RiftTable
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.Sort
import dev.nohus.rift.compose.SortingColumn
import dev.nohus.rift.compose.TableCell.RichTableCell
import dev.nohus.rift.compose.TableCell.TextTableCell
import dev.nohus.rift.compose.TableRow
import dev.nohus.rift.compose.VerticalDivider
import dev.nohus.rift.compose.fadingRightEdge
import dev.nohus.rift.compose.getNow
import dev.nohus.rift.compose.pointerInteraction
import dev.nohus.rift.compose.produceCorporationColors
import dev.nohus.rift.compose.sharedTransitionElement
import dev.nohus.rift.compose.text.LinkedText
import dev.nohus.rift.compose.text.toPlainString
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.EveColors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.calendar_16px
import dev.nohus.rift.generated.resources.checkmark_16px
import dev.nohus.rift.generated.resources.contribution_16px
import dev.nohus.rift.generated.resources.corporation_project_state_time_16px
import dev.nohus.rift.generated.resources.isk
import dev.nohus.rift.generated.resources.navigate_back_16px
import dev.nohus.rift.generated.resources.open_window_16px
import dev.nohus.rift.generated.resources.ratio_16px
import dev.nohus.rift.generated.resources.spaceship_command_16px
import dev.nohus.rift.generated.resources.stripes_tile
import dev.nohus.rift.network.Result
import dev.nohus.rift.network.esi.models.OpportunityState
import dev.nohus.rift.network.esi.models.ParticipationState
import dev.nohus.rift.opportunities.AgeRequirement
import dev.nohus.rift.opportunities.Contributors
import dev.nohus.rift.opportunities.Corporation
import dev.nohus.rift.opportunities.Creator
import dev.nohus.rift.opportunities.GetOpportunityContributionAttributesUseCase.OpportunityContributionAttribute
import dev.nohus.rift.opportunities.OpportunitiesUtils.getOpportunityCategory
import dev.nohus.rift.opportunities.OpportunitiesUtils.getOpportunityTypeMetadata
import dev.nohus.rift.opportunities.Opportunity
import dev.nohus.rift.opportunities.OpportunityCategoryFilter
import dev.nohus.rift.opportunities.OpportunityConfiguration
import dev.nohus.rift.opportunities.OpportunityType
import dev.nohus.rift.utils.formatDate
import dev.nohus.rift.utils.formatDateTime2
import dev.nohus.rift.utils.formatDuration
import dev.nohus.rift.utils.formatIsk
import dev.nohus.rift.utils.formatNumber
import dev.nohus.rift.utils.plural
import dev.nohus.rift.utils.withColor
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

@Composable
fun DetailsView(
    opportunity: Opportunity,
    onBackClick: () -> Unit,
    onCategoryFilterClick: (OpportunityCategoryFilter) -> Unit,
    onViewInGameClick: () -> Unit,
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            modifier = Modifier.padding(Spacing.medium),
        ) {
            RiftImageButton(
                resource = Res.drawable.navigate_back_16px,
                size = 16.dp,
                tint = RiftTheme.colors.textPrimary,
                onClick = onBackClick,
            )

            Text(
                text = "机遇详情",
                style = RiftTheme.typography.detailSecondary,
            )
        }

        OpportunityStateInfo(opportunity.state)

        ProjectHeader(
            opportunity = opportunity,
            onCategoryFilterClick = onCategoryFilterClick,
            onViewInGameClick = onViewInGameClick,
            modifier = Modifier.sharedTransitionElement("card-${opportunity.id}"),
        )

        ScrollbarColumn(
            contentPadding = PaddingValues(start = Spacing.large, end = Spacing.medium, bottom = Spacing.large),
            hasScrollbarBackground = true,
            modifier = Modifier.padding(top = Spacing.large, end = Spacing.large, bottom = Spacing.large),
        ) {
            val metadata = getOpportunityTypeMetadata(opportunity)
            val progressColor = when (opportunity.state) {
                OpportunityState.Active, OpportunityState.Completed -> {
                    if (opportunity.currentProgress >= opportunity.desiredProgress) EveColors.successGreen else Color(0xFFA9DBE9)
                }
                OpportunityState.Closed -> EveColors.gunmetalGrey
                else -> EveColors.hotRed
            }
            val opportunityType = when (opportunity.type) {
                OpportunityType.CorporationProject -> "项目"
                OpportunityType.FreelanceJob -> "任务"
            }

            EndDateRow(opportunity)

            if (opportunity.details.ageRequirement != null) {
                AgeRequirementBanners(opportunity.details.ageRequirement, opportunity.eligibleCharacters)
                Spacer(Modifier.height(Spacing.veryLarge))
            }

            TitledSection(title = "进度") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(50.dp),
                ) {
                    ProgressGauge(
                        progress = opportunity.currentProgress,
                        maxProgress = opportunity.desiredProgress,
                        color = progressColor,
                        iconResource = metadata.icon,
                        progressUnit = metadata.progressUnit,
                        characterId = null,
                        characterName = null,
                        isIskProgress = opportunity.details.configuration is OpportunityConfiguration.ShipInsurance,
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.large),
                    ) {
                        if (opportunity.details.configuration is OpportunityConfiguration.ShipInsurance) {
                            opportunity.reward?.let { reward ->
                                val tooltip: @Composable () -> Unit = {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                                        modifier = Modifier
                                            .width(IntrinsicSize.Max)
                                            .padding(Spacing.large),
                                    ) {
                                        Text(
                                            text = "分配的 ISK 总额：",
                                            style = RiftTheme.typography.detailSecondary,
                                        )
                                        Text(
                                            text = formatIsk(reward.initial, withCents = true),
                                            style = RiftTheme.typography.bodyPrimary,
                                        )
                                        Text(
                                            text = "支付给所有克隆飞行员的补偿总额：",
                                            style = RiftTheme.typography.detailSecondary,
                                        )
                                        Text(
                                            text = formatIsk(reward.initial - reward.remaining, withCents = true),
                                            style = RiftTheme.typography.bodyPrimary,
                                        )
                                        Divider(
                                            color = RiftTheme.colors.textSecondary,
                                        )
                                        Text(
                                            text = "剩余补偿：",
                                            style = RiftTheme.typography.detailSecondary,
                                        )
                                        Text(
                                            text = formatIsk(reward.remaining, withCents = true),
                                            style = RiftTheme.typography.bodyPrimary,
                                        )
                                    }
                                }
                                if (opportunity.details.submissionLimit != null) {
                                    RewardInfo(
                                        text = formatIsk(opportunity.details.submissionLimit, withCents = false),
                                        caption = "单次损失补偿上限",
                                        icon = Res.drawable.spaceship_command_16px,
                                        tooltip = tooltip,
                                    )
                                } else {
                                    RewardInfo(
                                        text = formatIsk(reward.remaining, withCents = false),
                                        caption = "$opportunityType 剩余 ISK",
                                        icon = Res.drawable.spaceship_command_16px,
                                        tooltip = tooltip,
                                    )
                                }
                            }

                            if (opportunity.details.submissionMultiplier != null) {
                                RewardInfo(
                                    text = String.format("%.0f%%", opportunity.details.submissionMultiplier * 100),
                                    caption = "赔付比例",
                                    icon = Res.drawable.ratio_16px,
                                    tooltip = {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                                            modifier = Modifier
                                                .width(IntrinsicSize.Max)
                                                .padding(Spacing.large),
                                        ) {
                                            Text(
                                                text = "按舰船及配装损失价值进行补偿的比例。",
                                                style = RiftTheme.typography.bodyPrimary,
                                            )
                                        }
                                    },
                                )
                            }
                        } else {
                            opportunity.reward?.let { reward ->
                                RewardInfo(
                                    text = formatIsk(reward.remaining, withCents = false),
                                    caption = "$opportunityType 剩余 ISK",
                                    icon = Res.drawable.isk,
                                    tooltip = {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                                            modifier = Modifier
                                                .width(IntrinsicSize.Max)
                                                .padding(Spacing.large),
                                        ) {
                                            Text(
                                                text = "分配的 ISK 总额：",
                                                style = RiftTheme.typography.detailSecondary,
                                            )
                                            Text(
                                                text = formatIsk(reward.initial, withCents = true),
                                                style = RiftTheme.typography.bodyPrimary,
                                            )
                                            Text(
                                                text = "所有克隆飞行员已获得总额：",
                                                style = RiftTheme.typography.detailSecondary,
                                            )
                                            Text(
                                                text = formatIsk(reward.initial - reward.remaining, withCents = true),
                                                style = RiftTheme.typography.bodyPrimary,
                                            )
                                            Divider(
                                                color = RiftTheme.colors.textSecondary,
                                            )
                                            Text(
                                                text = "剩余 ISK：",
                                                style = RiftTheme.typography.detailSecondary,
                                            )
                                            Text(
                                                text = formatIsk(reward.remaining, withCents = true),
                                                style = RiftTheme.typography.bodyPrimary,
                                            )
                                        }
                                    },
                                )
                                opportunity.details.rewardPerContribution?.let {
                                    RewardInfo(
                                        text = formatIsk(it, withCents = false),
                                        caption = "每 ${metadata.rewardPer} 奖励",
                                        icon = Res.drawable.contribution_16px,
                                    )
                                    // TODO: Deliver jobs in-game shows value warnings / universal price %
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(Spacing.veryLarge))
            TitledSection(
                title = "你的贡献",
                tooltip = if (opportunity.details.participationLimit != null) {
                    "该项目的参与上限为\n${formatNumber(opportunity.details.participationLimit)}，意味着你的贡献最多只能达到该上限。"
                } else {
                    null
                },
            ) {
                opportunity.contributions.forEach { contribution ->
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(50.dp),
                    ) {
                        ProgressGauge(
                            progress = contribution.contribution.success ?: 0,
                            maxProgress = opportunity.details.participationLimit ?: opportunity.desiredProgress,
                            color = progressColor,
                            iconResource = null,
                            progressUnit = metadata.progressUnit,
                            characterId = contribution.characterId,
                            characterName = contribution.characterName,
                            isIskProgress = opportunity.details.configuration is OpportunityConfiguration.ShipInsurance,
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(Spacing.verySmall),
                        ) {
                            if (contribution.contribution is Result.Success && opportunity.details.rewardPerContribution != null) {
                                if (opportunity.details.configuration is OpportunityConfiguration.ShipInsurance) {
                                    if (opportunity.details.participationLimit != null) {
                                        val availableProgress = opportunity.details.participationLimit - contribution.contribution.data
                                        RewardInfo(
                                            text = formatIsk(availableProgress * opportunity.details.rewardPerContribution, withCents = false),
                                            caption = "剩余补偿",
                                            icon = Res.drawable.isk,
                                        )
                                    }
                                } else {
                                    if (opportunity.details.participationLimit != null) {
                                        val availableProgress = opportunity.details.participationLimit - contribution.contribution.data
                                        RewardInfo(
                                            text = formatIsk(availableProgress * opportunity.details.rewardPerContribution, withCents = false),
                                            caption = "仍可获得",
                                            icon = Res.drawable.isk,
                                        )
                                    }

                                    val totalEarnings = contribution.contribution.data * opportunity.details.rewardPerContribution
                                    contribution.contribution.success?.let {
                                        RewardInfo(
                                            text = formatIsk(totalEarnings, withCents = false),
                                            caption = "ISK 总收益",
                                            icon = Res.drawable.checkmark_16px,
                                            tooltip = {
                                                Column(
                                                    verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                                                    modifier = Modifier
                                                        .width(IntrinsicSize.Max)
                                                        .padding(Spacing.large),
                                                ) {
                                                    Text(
                                                        text = "我的总收益：",
                                                        style = RiftTheme.typography.detailSecondary,
                                                    )
                                                    Text(
                                                        text = formatIsk(totalEarnings, withCents = true),
                                                        style = RiftTheme.typography.bodyPrimary,
                                                    )
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (opportunity.contributions.isEmpty()) {
                    Text(
                        text = "你尚未参与该项目",
                        style = RiftTheme.typography.displaySecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.CenterHorizontally).fillMaxWidth(),
                    )
                }
            }
            Spacer(Modifier.height(Spacing.veryLarge))
            TitledSection(title = "参与者") {
                when (val result = opportunity.contributors) {
                    is Contributors.Available -> {
                        when (opportunity.type) {
                            OpportunityType.CorporationProject -> {
                                val rows = result.contributors.map { contributor ->
                                    val percent = contributor.contributed / opportunity.desiredProgress.toFloat()
                                    val payout = contributor.contributed * (opportunity.details.rewardPerContribution ?: 0.0)
                                    TableRow(
                                        id = contributor.characterId.toString(),
                                        cells = listOf(
                                            TextTableCell(contributor.details?.name ?: "${contributor.characterId}"),
                                            TextTableCell(formatNumber(contributor.contributed), sortingAmount = contributor.contributed.toDouble()),
                                            TextTableCell(String.format("%.1f%%", percent * 100), sortingAmount = percent.toDouble()),
                                            TextTableCell(formatIsk(payout, withCents = false), sortingAmount = payout),
                                        ),
                                        characterId = contributor.characterId,
                                    )
                                }
                                RiftTable(
                                    columns = listOf("贡献者", "总量", "占项目目标", "报酬"),
                                    rows = rows,
                                    extraSpacing = 30.dp,
                                    defaultSort = SortingColumn(1, Sort.Descending),
                                )
                            }
                            OpportunityType.FreelanceJob -> {
                                val rows = result.contributors.map { contributor ->
                                    val percent = contributor.contributed / opportunity.desiredProgress.toFloat()
                                    val payout = contributor.contributed * (opportunity.details.rewardPerContribution ?: 0.0)
                                    TableRow(
                                        id = contributor.characterId.toString(),
                                        cells = listOf(
                                            RichTableCell(contributor.details?.name ?: "${contributor.characterId}", 32.dp, 120.dp) {
                                                ClickableCharacter(contributor.characterId) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                                                    ) {
                                                        AsyncCharacterPortrait(
                                                            characterId = contributor.characterId,
                                                            size = 32,
                                                            modifier = Modifier
                                                                .size(24.dp)
                                                                .clip(CircleShape),
                                                        )
                                                        LinkText(
                                                            text = contributor.details?.name ?: contributor.characterId.toString(),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Clip,
                                                            softWrap = false,
                                                        )
                                                    }
                                                }
                                            },
                                            RichTableCell(contributor.details?.corporationName ?: "${contributor.details?.corporationId}", 32.dp, 120.dp) {
                                                if (contributor.details?.corporationId != null) {
                                                    ClickableCorporation(contributor.details.corporationId) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                                                        ) {
                                                            AsyncCorporationLogo(
                                                                corporationId = contributor.details.corporationId,
                                                                size = 32,
                                                                modifier = Modifier
                                                                    .size(24.dp),
                                                            )
                                                            LinkText(
                                                                text = contributor.details.corporationName ?: "未知",
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Clip,
                                                                softWrap = false,
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    Text(text = "-")
                                                }
                                            },
                                            RichTableCell(contributor.details?.allianceName ?: "${contributor.details?.allianceId}", 32.dp, 120.dp) {
                                                if (contributor.details?.allianceId != null) {
                                                    ClickableAlliance(contributor.details.allianceId) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                                                        ) {
                                                            AsyncAllianceLogo(
                                                                allianceId = contributor.details.allianceId,
                                                                size = 32,
                                                                modifier = Modifier
                                                                    .size(24.dp),
                                                            )
                                                            LinkText(
                                                                text = contributor.details.allianceName ?: "未知",
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Clip,
                                                                softWrap = false,
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    Text(text = "-")
                                                }
                                            },
                                            TextTableCell(formatNumber(contributor.contributed), sortingAmount = contributor.contributed.toDouble()),
                                            TextTableCell(String.format("%.1f%%", percent * 100), sortingAmount = percent.toDouble()),
                                            TextTableCell(formatIsk(payout, withCents = false), sortingAmount = payout),
                                            RichTableCell(contributor.details?.allianceName ?: "${contributor.details?.allianceId}", 32.dp, 120.dp) {
                                                val (text, color) = when (contributor.participationState) {
                                                    ParticipationState.Unspecified -> "未指定" to RiftTheme.colors.textSecondary
                                                    ParticipationState.Commited -> "已接受" to EveColors.successGreen
                                                    ParticipationState.Kicked -> "已移除" to EveColors.dangerRed
                                                    ParticipationState.Resigned -> "已退出" to EveColors.warningOrange
                                                }
                                                Text(
                                                    text = text,
                                                    style = RiftTheme.typography.bodyPrimary.copy(color = color),
                                                )
                                            },
                                        ),
                                        characterId = null,
                                    )
                                }
                                RiftTable(
                                    columns = listOf("参与者", "军团", "联盟", "总量", "占目标值", "ISK 报酬", "状态"),
                                    rows = rows,
                                    extraSpacing = 30.dp,
                                    defaultSort = SortingColumn(1, Sort.Descending),
                                )
                            }
                        }
                    }
                    Contributors.Empty -> Text(
                        text = "未找到贡献记录",
                        style = RiftTheme.typography.displaySecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.CenterHorizontally).fillMaxWidth(),
                    )
                    Contributors.NoAccess -> Text(
                        text = "你不是项目管理员",
                        style = RiftTheme.typography.bodySecondary,
                    )
                    is Contributors.Error -> Text(
                        text = "无法加载贡献者：${result.message}",
                        style = RiftTheme.typography.bodySecondary,
                    )
                }
            }
            Spacer(Modifier.height(Spacing.veryLarge))
            TitledSection(title = "目标") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.large),
                ) {
                    OpportunityAttribute(
                        caption = "贡献方式",
                        icon = metadata.icon,
                        values = listOf(OpportunityContributionAttribute.Text(metadata.name, isPlain = true)),
                        tooltip = metadata.tooltip,
                    )

                    opportunity.details.contributionAttributes.forEach { attribute ->
                        OpportunityAttribute(
                            caption = attribute.name,
                            icon = attribute.icon,
                            values = attribute.values,
                            tooltip = attribute.description,
                        )
                    }
                }
            }
            Spacer(Modifier.height(Spacing.veryLarge))
            if (opportunity.details.description.toPlainString().isNotEmpty()) {
                TitledSection(title = "描述") {
                    LinkedText(
                        text = opportunity.details.description,
                        style = RiftTheme.typography.bodyPrimary,
                    )
                }
                Spacer(Modifier.height(Spacing.veryLarge))
            }
            TitledSection(title = "创建于 ${formatDate(opportunity.details.created)}") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(50.dp),
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                ) {
                    Creator(opportunity.creator)
                    Corporation(opportunity.creator.corporation)
                }
            }
        }
    }
}

@Composable
private fun AgeRequirementBanners(
    ageRequirement: AgeRequirement,
    characters: List<LocalCharacter>,
) {
    val (metCharacters, unmetCharacters) = remember(ageRequirement, characters) {
        val now = Instant.now()
        characters
            .filter { it.info != null }
            .partition {
                val ageDays = Duration.between(it.info!!.birthday, now).toDays()
                val isMinimumMet = if (ageRequirement.minimumAge != null) ageDays >= ageRequirement.minimumAge else true
                val isMaximumMet = if (ageRequirement.maximumAge != null) ageDays <= ageRequirement.maximumAge else true
                isMinimumMet && isMaximumMet
            }
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        if (metCharacters.isNotEmpty()) {
            AgeRequirementBanner(ageRequirement, true, metCharacters)
        }
        if (unmetCharacters.isNotEmpty()) {
            AgeRequirementBanner(ageRequirement, false, unmetCharacters)
        }
    }
}

@Composable
private fun AgeRequirementBanner(
    ageRequirement: AgeRequirement,
    hasMetRequirements: Boolean,
    characters: List<LocalCharacter>,
) {
    val backgroundColor = if (hasMetRequirements) EveColors.copperOxideGreen.copy(alpha = 0.4f) else EveColors.cherryRed.copy(alpha = 0.4f)
    val headingColor = if (hasMetRequirements) EveColors.successGreen else EveColors.dangerRed
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(backgroundColor),
    ) {
        val stripesBitmap = imageResource(Res.drawable.stripes_tile)
        val brush = remember(stripesBitmap) { ShaderBrush(ImageShader(stripesBitmap, TileMode.Repeated, TileMode.Repeated)) }
        Box(
            Modifier
                .width(6.dp)
                .fillMaxHeight()
                .graphicsLayer {
                    colorFilter = BlendModeColorFilter(color = headingColor, blendMode = BlendMode.SrcIn)
                }
                .background(brush),
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
            modifier = Modifier.padding(Spacing.medium).weight(2f),
        ) {
            val title = if (hasMetRequirements) "满足舰龄要求" else "未满足舰龄要求"
            Text(
                text = title,
                style = RiftTheme.typography.bodyPrimary.copy(color = headingColor),
            )
            val text = buildString {
                append("仅对舰龄")
                ageRequirement.minimumAge?.let {
                    append("不少于 $it 天")
                }
                if (ageRequirement.minimumAge != null && ageRequirement.maximumAge != null) append(" 且")
                ageRequirement.maximumAge?.let {
                    append("不超过 $it 天")
                }
                append("的克隆飞行员开放。")
            }
            Text(
                text = text,
                style = RiftTheme.typography.bodyHighlighted,
            )
        }

        CharacterStack(characters, Modifier.weight(1f))
    }
}

@Composable
private fun CharacterStack(
    characters: List<LocalCharacter>,
    modifier: Modifier = Modifier,
) {
    if (characters.isEmpty()) return
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        val now = Instant.now()
        RiftTooltipArea(
            tooltip = {
                Column(Modifier.padding(Spacing.large)) {
                    characters.sortedByDescending { it.info!!.birthday }.forEach {
                        val days = Duration.between(it.info!!.birthday, now).toDays()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                        ) {
                            AsyncCharacterPortrait(
                                characterId = it.characterId,
                                size = 32,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape),
                            )
                            Text(
                                text = it.info.name,
                                style = RiftTheme.typography.bodyPrimary,
                            )
                            Text(
                                text = "舰龄 ${formatNumber(days)} 天",
                                style = RiftTheme.typography.bodyHighlighted,
                            )
                        }
                    }
                }
            },
            modifier = Modifier
                .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive)),
        ) {
            Layout(
                content = {
                    characters.forEach { character ->
                        AsyncCharacterPortrait(
                            characterId = character.characterId,
                            size = 64,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape),
                        )
                    }
                },
            ) { measurables, constraints ->
                val placeables = measurables.map {
                    it.measure(
                        Constraints(
                            maxWidth = constraints.maxWidth,
                            maxHeight = constraints.maxHeight,
                        ),
                    )
                }
                val height = placeables.maxOf { it.height }
                val totalWidth = placeables.sumOf { it.width }
                val width = minOf(totalWidth, constraints.maxWidth)
                layout(width = width, height = height) {
                    val singleWidth = placeables.first().measuredWidth
                    val offset = if (placeables.size > 1) {
                        (width - singleWidth) / (placeables.size - 1)
                    } else {
                        0
                    }
                    placeables.forEachIndexed { index, placeable ->
                        val x = index * offset
                        placeable.placeRelative(x = x, y = 0)
                    }
                }
            }
        }
    }
}

@Composable
private fun EndDateRow(opportunity: Opportunity) {
    if (opportunity.details.expires != null || opportunity.details.finished != null) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            modifier = Modifier.height(IntrinsicSize.Min).wrapContentWidth(align = Alignment.Start, unbounded = true),
        ) {
            if (opportunity.details.expires != null && opportunity.state == OpportunityState.Active) {
                val expiresIn = Duration.between(getNow(), opportunity.details.expires).coerceAtLeast(Duration.ZERO)
                val iconColor = when {
                    expiresIn <= Duration.ofHours(6) -> EveColors.cherryRed
                    expiresIn <= Duration.ofDays(1) -> EveColors.duskyOrange
                    else -> RiftTheme.colors.textDisabled
                }
                val textColor = when {
                    expiresIn <= Duration.ofHours(6) -> EveColors.cherryRed
                    expiresIn <= Duration.ofDays(1) -> EveColors.duskyOrange
                    else -> RiftTheme.colors.textSecondary
                }
                val textHighlightColor = when {
                    expiresIn <= Duration.ofHours(6) -> EveColors.dangerRed
                    expiresIn <= Duration.ofDays(1) -> EveColors.warningOrange
                    else -> RiftTheme.colors.textPrimary
                }
                Image(
                    painter = painterResource(Res.drawable.corporation_project_state_time_16px),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(iconColor),
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = buildAnnotatedString {
                        append("剩余 ")
                        val style = RiftTheme.typography.bodySecondary.copy(color = textHighlightColor, fontWeight = FontWeight.Bold)
                        withStyle(style.toSpanStyle()) {
                            append(formatDuration(expiresIn))
                        }
                    },
                    style = RiftTheme.typography.bodySecondary.copy(color = textColor),
                )
                VerticalDivider(
                    color = RiftTheme.colors.textDisabled,
                    modifier = Modifier.padding(horizontal = Spacing.small),
                )
            }

            val finishDate = if (opportunity.state == OpportunityState.Active) {
                opportunity.details.expires ?: opportunity.details.finished
            } else {
                opportunity.details.finished
            }
            if (finishDate != null) {
                Image(
                    painter = painterResource(Res.drawable.calendar_16px),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(RiftTheme.colors.textDisabled),
                    modifier = Modifier.size(16.dp),
                )

                Text(
                    text = buildAnnotatedString {
                        append("结束时间 ")
                        withColor(RiftTheme.colors.textPrimary) {
                            append(formatDateTime2(finishDate))
                        }
                    },
                    style = RiftTheme.typography.bodySecondary,
                )
            }
        }
        Spacer(Modifier.height(Spacing.medium))
    }
}

@Composable
private fun ProgressGauge(
    progress: Long,
    maxProgress: Long,
    color: Color,
    iconResource: DrawableResource?,
    progressUnit: String?,
    characterId: Int?,
    characterName: String?,
    isIskProgress: Boolean = false,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.large),
    ) {
        val progressPercent = progress / maxProgress.toFloat()
        CorporationProjectProgressGauge(
            progress = progressPercent,
            color = color,
            iconResource = iconResource,
            characterId = characterId,
            characterName = characterName,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
                                    val suffix = if (isIskProgress) " 伊斯克" else ""
            Text(
                text = "${formatNumber(progress)}$suffix",
                style = RiftTheme.typography.headerPrimary.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                text = "/ ${formatNumber(maxProgress)}$suffix",
                style = RiftTheme.typography.detailPrimary,
            )
            Text(
                text = buildAnnotatedString {
                    append("${(progressPercent * 100).roundToInt()}%")
                    append(" ")
                    withColor(RiftTheme.colors.textSecondary) {
                        if (progressUnit != null) append(progressUnit)
                    }
                },
                style = RiftTheme.typography.detailPrimary,
            )
        }
    }
}

@Composable
private fun RewardInfo(
    text: String,
    caption: String,
    icon: DrawableResource,
    tooltip: @Composable (() -> Unit)? = null,
) {
    RiftTooltipArea(
        tooltip = tooltip,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(CutCornerShape(bottomEnd = 8.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .size(32.dp),
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = RiftTheme.colors.textSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
            Column {
                Text(
                    text = text,
                    style = RiftTheme.typography.bodyPrimary,
                )
                Text(
                    text = caption,
                    style = RiftTheme.typography.detailSecondary,
                )
            }
        }
    }
}

@Composable
private fun ProjectHeader(
    opportunity: Opportunity,
    onCategoryFilterClick: (OpportunityCategoryFilter) -> Unit,
    onViewInGameClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pointerInteractionStateHolder = remember { PointerInteractionStateHolder() }
    Box(
        modifier = modifier
            .pointerInteraction(pointerInteractionStateHolder)
            .height(IntrinsicSize.Min),
    ) {
        Surface(
            color = RiftTheme.colors.backgroundPrimaryDark,
        ) {
            val category = getOpportunityCategory(opportunity)
            val alpha by animateFloatAsState(
                if (pointerInteractionStateHolder.isHovered) {
                    0.75f
                } else {
                    0.5f
                },
            )
            RiftOpportunityContainerFlair(
                isHovered = pointerInteractionStateHolder.isHovered,
                image = category.flair,
                offset = DpOffset(26.dp, 26.dp),
                alpha = alpha,
            )

            CorporationColorsSwatch(opportunity, pointerInteractionStateHolder)

            Column(
                modifier = Modifier.padding(Spacing.large),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = Spacing.medium),
                ) {
                    RiftOpportunityTypeIcon(category.icon, null)
                    Spacer(Modifier.width(Spacing.medium))
                    Text(
                        text = category.name,
                        style = RiftTheme.typography.headerSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Visible,
                        softWrap = false,
                        modifier = Modifier
                            .weight(1f)
                            .clipToBounds()
                            .fadingRightEdge(),
                    )
                }

                opportunity.details.solarSystemChipState?.let {
                    RiftSolarSystemChip(it, isOnlyShowingClosest = true)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.heightIn(max = 96.dp),
                ) {
                    Column(Modifier.weight(2f)) {
                        Spacer(Modifier.height(Spacing.large))
                        Text(
                            text = opportunity.name,
                            style = RiftTheme.typography.headlinePrimary.copy(fontWeight = FontWeight.Bold),
                        )
                        Spacer(Modifier.height(Spacing.large))
                    }
                    Box(Modifier.weight(1f)) {
                        ClickableCorporation(opportunity.creator.corporation.id) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.veryLarge),
                            ) {
                                AsyncCorporationLogo(
                                    corporationId = opportunity.creator.corporation.id,
                                    size = 64,
                                    modifier = Modifier.size(64.dp),
                                )
                                Text(
                                    text = opportunity.creator.corporation.name,
                                    style = RiftTheme.typography.headlinePrimary,
                                )
                            }
                        }
                    }
                }

                OpportunityCategoryFilterChips(
                    filters = opportunity.details.matchingFilters.sortedBy { it.order },
                    enabledFilters = emptySet(),
                    onCategoryFilterChange = { onCategoryFilterClick(it) },
                )

                Spacer(Modifier.height(Spacing.large))

                RiftButton(
                    text = "在游戏内查看",
                    icon = Res.drawable.open_window_16px,
                    cornerCut = ButtonCornerCut.Both,
                    onClick = onViewInGameClick,
                    modifier = Modifier.width(400.dp),
                )
            }
        }
    }
}

@Composable
private fun BoxScope.CorporationColorsSwatch(
    opportunity: Opportunity,
    pointerInteractionStateHolder: PointerInteractionStateHolder,
) {
    val colors by produceCorporationColors(opportunity.creator.corporation.id)
    val isActive = pointerInteractionStateHolder.isHovered
    val alpha by animateFloatAsState(if (isActive) 0.5f else 0.1f)
    val blur by animateFloatAsState(if (isActive) 4f else 0.5f)
    val extent by animateFloatAsState(if (isActive) 4f else 2f)
    Box(
        modifier = Modifier
            .align(Alignment.BottomEnd),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size((96 + extent).dp)
                .graphicsLayer(renderEffect = BlurEffect(blur, blur, edgeTreatment = TileMode.Decal))
                .clip(CutCornerShape(topStartPercent = 100, bottomEndPercent = 18))
                .background(colors.primary.copy(alpha = alpha)),
        ) {}
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .clip(CutCornerShape(topStartPercent = 100, bottomEndPercent = 18))
                .size(96.dp)
                .background(colors.primary),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .graphicsLayer(renderEffect = BlurEffect(blur, blur, edgeTreatment = TileMode.Decal))
                    .clip(CutCornerShape(topStartPercent = 100))
                    .size((48 + extent).dp)
                    .background(colors.secondary.copy(alpha = alpha)),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .clip(CutCornerShape(topStartPercent = 100))
                    .size(48.dp)
                    .background(colors.secondary),
            )
        }
    }
}

@Composable
private fun Creator(creator: Creator) {
    ClickableCharacter(creator.characterId) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.veryLarge),
        ) {
            RiftCircularCharacterPortrait(
                characterId = creator.characterId,
                name = creator.characterName,
                hasPadding = false,
                size = 64.dp,
            )
            Column {
                Text(
                    text = "发布者",
                    style = RiftTheme.typography.bodySecondary,
                )
                LinkText(
                    text = creator.characterName,
                    normalStyle = RiftTheme.typography.headerPrimary.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    hoveredStyle = RiftTheme.typography.headerPrimary.copy(
                        color = RiftTheme.colors.textLink,
                        fontWeight = FontWeight.Bold,
                    ),
                    hasHoverCursor = false,
                )
            }
        }
    }
}

@Composable
private fun Corporation(corporation: Corporation) {
    ClickableCorporation(corporation.id) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.veryLarge),
        ) {
            AsyncCorporationLogo(
                corporationId = corporation.id,
                size = 64,
                modifier = Modifier.size(64.dp),
            )
            LinkText(
                text = corporation.name,
                normalStyle = RiftTheme.typography.headerPrimary.copy(
                    fontWeight = FontWeight.Bold,
                ),
                hoveredStyle = RiftTheme.typography.headerPrimary.copy(
                    color = RiftTheme.colors.textLink,
                    fontWeight = FontWeight.Bold,
                ),
                hasHoverCursor = false,
            )
        }
    }
}

@Composable
private fun TitledSection(
    title: String,
    tooltip: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            Text(
                text = title,
                style = RiftTheme.typography.headlineSecondary,
            )
            if (tooltip != null) {
                RiftTooltipArea(tooltip) {
                    RiftMulticolorIcon(MulticolorIconType.Info)
                }
            }
        }
        content()
    }
}
