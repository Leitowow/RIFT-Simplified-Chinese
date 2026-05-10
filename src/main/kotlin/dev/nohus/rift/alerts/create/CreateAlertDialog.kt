package dev.nohus.rift.alerts.create

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.onClick
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.rememberWindowState
import dev.nohus.rift.alerts.create.CreateAlertViewModel.UiState
import dev.nohus.rift.alerts.create.FormAnswer.CharacterAnswer
import dev.nohus.rift.alerts.create.FormAnswer.ContactsLabelAnswer
import dev.nohus.rift.alerts.create.FormAnswer.FreeformTextAnswer
import dev.nohus.rift.alerts.create.FormAnswer.IntelChannelAnswer
import dev.nohus.rift.alerts.create.FormAnswer.JumpsRangeAnswer
import dev.nohus.rift.alerts.create.FormAnswer.MultipleChoiceAnswer
import dev.nohus.rift.alerts.create.FormAnswer.PlanetaryIndustryColoniesAnswer
import dev.nohus.rift.alerts.create.FormAnswer.SingleChoiceAnswer
import dev.nohus.rift.alerts.create.FormAnswer.SoundAnswer
import dev.nohus.rift.alerts.create.FormAnswer.SpecificCharactersAnswer
import dev.nohus.rift.alerts.create.FormAnswer.SystemAnswer
import dev.nohus.rift.characters.repositories.LocalCharactersRepository.LocalCharacter
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.PointerInteractionStateHolder
import dev.nohus.rift.compose.RequirementIcon
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftCheckbox
import dev.nohus.rift.compose.RiftCheckboxWithLabel
import dev.nohus.rift.compose.RiftDialog
import dev.nohus.rift.compose.RiftDropdown
import dev.nohus.rift.compose.RiftFileChooserButton
import dev.nohus.rift.compose.RiftImageButton
import dev.nohus.rift.compose.RiftRadioButton
import dev.nohus.rift.compose.RiftTabBar
import dev.nohus.rift.compose.RiftTextField
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.Tab
import dev.nohus.rift.compose.hoverBackground
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.contacts.ContactsRepository.Label
import dev.nohus.rift.di.koin
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.play
import dev.nohus.rift.generated.resources.window_loudspeaker_icon
import dev.nohus.rift.get
import dev.nohus.rift.planetaryindustry.PlanetaryIndustryRepository.ColonyItem
import dev.nohus.rift.utils.sound.Sound
import dev.nohus.rift.utils.sound.SoundPlayer
import dev.nohus.rift.utils.toRegexOrNull
import dev.nohus.rift.utils.withColor
import dev.nohus.rift.viewModel
import dev.nohus.rift.windowing.WindowManager
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.nameWithoutExtension

@Composable
fun WindowScope.CreateAlertDialog(
    inputModel: CreateAlertInputModel,
    parentWindowState: WindowManager.RiftWindowState,
    onDismiss: () -> Unit,
) {
    val viewModel: CreateAlertViewModel = viewModel(inputModel)
    val state by viewModel.state.collectAsState()

    if (state.dismissEvent.get()) onDismiss()

    val title = when (inputModel) {
        CreateAlertInputModel.New -> "新建告警"
        is CreateAlertInputModel.EditAction -> "编辑告警动作"
    }
    RiftDialog(
        title = title,
        icon = Res.drawable.window_loudspeaker_icon,
        parentState = parentWindowState,
        state = rememberWindowState(width = 400.dp, height = Dp.Unspecified),
        onCloseClick = onDismiss,
    ) {
        CreateAlertDialogContent(
            state = state,
            onFormPendingAnswer = viewModel::onFormPendingAnswer,
            onBackClick = viewModel::onBackClick,
            onContinueClick = viewModel::onContinueClick,
        )
    }
}

@Composable
private fun CreateAlertDialogContent(
    state: UiState,
    onFormPendingAnswer: (FormAnswer) -> Unit,
    onBackClick: () -> Unit,
    onContinueClick: () -> Unit,
) {
    Column {
        if (state.formAnswers.isNotEmpty()) {
            Column {
                for ((question, answer) in state.formAnswers) {
                    val answerText = (question to answer).toAnswerString(state.characters)
                    if (answerText != null) {
                        val text = buildAnnotatedString {
                            append(question.title)
                            append(" ")
                            withStyle(SpanStyle(color = RiftTheme.colors.textPrimary)) {
                                append(answerText)
                            }
                        }
                        Text(
                            text = text,
                            style = RiftTheme.typography.bodySecondary,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = Spacing.small),
                        )
                    }
                }
            }
        }
        Column {
            AnimatedContent(state.formQuestion) { formQuestion ->
                if (formQuestion != null) {
                    val highlightAnimationState = remember { AnimationState(0f) }
                    LaunchedEffect(state.highlightQuestionEvent) {
                        if (state.highlightQuestionEvent.get()) {
                            highlightAnimationState.animateTo(1f, animationSpec = tween(300))
                            highlightAnimationState.animateTo(0f, animationSpec = tween(300))
                        }
                    }
                    Box(
                        modifier = Modifier.alpha(1f - (0.5f * highlightAnimationState.value)),
                    ) {
                        FormQuestion(
                            formQuestion = formQuestion,
                            isPendingAnswerValid = state.isPendingAnswerValid,
                            pendingAnswerInvalidReason = state.pendingAnswerInvalidReason,
                            characters = state.characters,
                            intelChannels = state.intelChannels,
                            sounds = state.sounds,
                            recentTargets = state.recentTargets,
                            colonies = state.colonies,
                            labels = state.labels,
                            onFormAnswer = onFormPendingAnswer,
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                modifier = Modifier.padding(top = Spacing.medium),
            ) {
                RiftButton(
                    text = "返回",
                    cornerCut = ButtonCornerCut.BottomLeft,
                    type = ButtonType.Secondary,
                    onClick = onBackClick,
                    modifier = Modifier.weight(1f),
                )
                val label = if (state.formQuestion != null) "继续" else "完成"
                RiftButton(
                    text = label,
                    onClick = onContinueClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun FormQuestion(
    formQuestion: FormQuestion,
    isPendingAnswerValid: Boolean?,
    pendingAnswerInvalidReason: String?,
    characters: List<LocalCharacter>,
    intelChannels: List<String>,
    sounds: List<Sound>,
    recentTargets: Set<String>,
    colonies: List<ColonyItem>,
    labels: List<Label>,
    onFormAnswer: (FormAnswer) -> Unit,
) {
    Column {
        Text(
            text = formQuestion.title,
            style = RiftTheme.typography.headerSecondary,
            modifier = Modifier
                .padding(vertical = Spacing.medium),
        )
        when (formQuestion) {
            is FormQuestion.SingleChoiceQuestion -> {
                ScrollbarColumn(
                    modifier = Modifier.heightIn(max = 200.dp),
                ) {
                    var selected: FormChoiceItem? by remember { mutableStateOf(null) }
                    for (item in formQuestion.items) {
                        ListSelectorRow(
                            text = item.text,
                            description = item.description,
                            isMultipleChoice = false,
                            isSelected = item == selected,
                            onSelect = {
                                selected = item
                                onFormAnswer(SingleChoiceAnswer(item.id))
                            },
                        )
                    }
                }
            }

            is FormQuestion.MultipleChoiceQuestion -> {
                ScrollbarColumn(
                    modifier = Modifier.heightIn(max = 200.dp),
                ) {
                    var selected: List<FormChoiceItem> by remember { mutableStateOf(emptyList()) }
                    for (item in formQuestion.items) {
                        ListSelectorRow(
                            text = item.text,
                            description = item.description,
                            isMultipleChoice = true,
                            isSelected = item in selected,
                            onSelect = {
                                if (item in selected) selected -= item else selected += item
                                onFormAnswer(MultipleChoiceAnswer(selected))
                            },
                        )
                    }
                }
            }

            is FormQuestion.SystemQuestion -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .heightIn(min = 36.dp) // For requirement icon
                        .fillMaxWidth(),
                ) {
                    var system: String by remember { mutableStateOf("") }
                    val placeholder = if (formQuestion.allowEmpty) {
                        "星系名，可留空"
                    } else {
                        "星系名"
                    }
                    if (formQuestion.allowEmpty) {
                        LaunchedEffect(Unit) {
                            onFormAnswer(SystemAnswer(""))
                        }
                    }
                    RiftTextField(
                        text = system,
                        placeholder = placeholder,
                        onTextChanged = {
                            system = it
                            onFormAnswer(SystemAnswer(it))
                        },
                        modifier = Modifier.weight(1f),
                    )
                    if (isPendingAnswerValid != null) {
                        RequirementIcon(
                            isFulfilled = isPendingAnswerValid,
                            fulfilledTooltip = "星系有效",
                            notFulfilledTooltip = "星系不存在",
                            modifier = Modifier.padding(start = Spacing.medium),
                        )
                    }
                }
            }

            is FormQuestion.JumpsRangeQuestion -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    var min: Int by remember { mutableStateOf(0) }
                    var max: Int by remember { mutableStateOf(0) }

                    fun getItemName(jumps: Int) = if (jumps == 0) "同星系" else "$jumps 跳"

                    LaunchedEffect(formQuestion) {
                        onFormAnswer(JumpsRangeAnswer(minJumps = min, maxJumps = max))
                    }
                    Text(
                        text = "从：",
                        style = RiftTheme.typography.headerPrimary,
                    )
                    val maxJumps = 16 // 0 - 15
                    RiftDropdown(
                        items = List(maxJumps) { it },
                        selectedItem = min,
                        onItemSelected = {
                            min = it
                            if (max < min) max = min
                            onFormAnswer(JumpsRangeAnswer(minJumps = min, maxJumps = max))
                        },
                        getItemName = { getItemName(it) },
                        maxItems = 5,
                    )
                    Text(
                        text = " 到：",
                        style = RiftTheme.typography.headerPrimary,
                    )
                    RiftDropdown(
                        items = List(maxJumps - min) { min + it },
                        selectedItem = max,
                        onItemSelected = {
                            max = it
                            onFormAnswer(JumpsRangeAnswer(minJumps = min, maxJumps = max))
                        },
                        getItemName = { getItemName(it) },
                        maxItems = 5,
                    )
                }
            }

            is FormQuestion.OwnedCharacterQuestion -> {
                if (characters.isNotEmpty()) {
                    var selected: Int by remember { mutableStateOf(characters.first().characterId) }
                    LaunchedEffect(formQuestion) {
                        onFormAnswer(CharacterAnswer(selected))
                    }
                    RiftDropdown(
                        items = characters.map { it.characterId },
                        selectedItem = selected,
                        onItemSelected = { characterId ->
                            selected = characterId
                            onFormAnswer(CharacterAnswer(characterId))
                        },
                        getItemName = { characterId ->
                            characters.firstOrNull { it.characterId == characterId }?.info?.name ?: "$characterId"
                        },
                    )
                } else {
                    Text(
                        text = "没有可选角色。",
                        style = RiftTheme.typography.headerPrimary,
                    )
                }
            }

            is FormQuestion.IntelChannelQuestion -> {
                if (intelChannels.isNotEmpty()) {
                    var selected: String by remember { mutableStateOf(intelChannels.first()) }
                    LaunchedEffect(formQuestion) {
                        onFormAnswer(IntelChannelAnswer(selected))
                    }
                    RiftDropdown(
                        items = intelChannels,
                        selectedItem = selected,
                        onItemSelected = {
                            selected = it
                            onFormAnswer(IntelChannelAnswer(it))
                        },
                        getItemName = { it },
                    )
                } else {
                    Text(
                        text = "没有可选的预警频道。",
                        style = RiftTheme.typography.headerPrimary,
                    )
                }
            }

            is FormQuestion.SoundQuestion -> {
                val soundPlayer: SoundPlayer = remember { koin.get() }
                var selected: Sound? by remember { mutableStateOf(null) }
                var selectedTabIndex by remember { mutableStateOf(0) }
                RiftTabBar(
                    tabs = listOf(
                        Tab(0, "内置音效", isCloseable = false),
                        Tab(1, "自定义音效", isCloseable = false),
                    ),
                    selectedTab = selectedTabIndex,
                    onTabSelected = { selectedTabIndex = it },
                    onTabClosed = {},
                )
                ScrollbarColumn(
                    modifier = Modifier.height(180.dp),
                ) {
                    if (selectedTabIndex == 0) {
                        for (sound in sounds) {
                            ListSelectorRow(
                                text = sound.name,
                                description = null,
                                isMultipleChoice = false,
                                isSelected = sound == selected,
                                onSelect = {
                                    selected = sound
                                    onFormAnswer(SoundAnswer.BuiltInSound(sound))
                                },
                                rightContent = {
                                    RiftImageButton(
                                        Res.drawable.play,
                                        size = 16.dp,
                                        onClick = { soundPlayer.play(sound.resource) },
                                        modifier = Modifier.padding(horizontal = Spacing.small),
                                    )
                                },
                            )
                        }
                    } else {
                        Text(
                            text = "选择音效文件：",
                            style = RiftTheme.typography.bodyPrimary,
                            modifier = Modifier.padding(top = Spacing.medium),
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                            modifier = Modifier
                                .padding(top = Spacing.medium)
                                .heightIn(min = 36.dp),
                        ) {
                            var text by remember { mutableStateOf("") }
                            RiftTextField(
                                text = text,
                                onTextChanged = {
                                    text = it
                                    onFormAnswer(SoundAnswer.CustomSound(it))
                                },
                                modifier = Modifier.weight(1f),
                            )
                            AnimatedVisibility(isPendingAnswerValid != null) {
                                RequirementIcon(
                                    isFulfilled = isPendingAnswerValid ?: false,
                                    fulfilledTooltip = "音效文件路径有效",
                                    notFulfilledTooltip = pendingAnswerInvalidReason ?: "无效",
                                )
                            }
                            RiftFileChooserButton(
                                typesDescription = "WAV 音频",
                                extensions = listOf("wav"),
                                onFileChosen = {
                                    text = it.absolutePathString()
                                    onFormAnswer(SoundAnswer.CustomSound(it.absolutePathString()))
                                },
                            )
                            RiftImageButton(
                                Res.drawable.play,
                                size = 16.dp,
                                onClick = { soundPlayer.playFile(text) },
                                modifier = Modifier.padding(horizontal = Spacing.small),
                            )
                        }
                        Text(
                            text = "可用播放按钮试听。",
                            style = RiftTheme.typography.bodyPrimary,
                            modifier = Modifier.padding(vertical = Spacing.medium),
                        )
                    }
                }
            }

            is FormQuestion.SpecificCharactersQuestion -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .heightIn(min = 36.dp) // For requirement icon
                        .fillMaxWidth(),
                ) {
                    var text: String by remember { mutableStateOf("") }
                    val placeholder = if (formQuestion.allowEmpty) {
                        "多个角色名用英文逗号分隔，可留空"
                    } else {
                        "多个角色名用英文逗号分隔"
                    }
                    if (formQuestion.allowEmpty) {
                        LaunchedEffect(Unit) {
                            onFormAnswer(SpecificCharactersAnswer(emptyList()))
                        }
                    }
                    RiftTextField(
                        text = text,
                        placeholder = placeholder,
                        onTextChanged = {
                            text = it
                            val splitCharacters = text
                                .split(",")
                                .map { it.trim() }
                                .filterNot { it.isBlank() }
                            onFormAnswer(SpecificCharactersAnswer(splitCharacters))
                        },
                        modifier = Modifier.weight(1f),
                    )
                    if (isPendingAnswerValid != null) {
                        RequirementIcon(
                            isFulfilled = isPendingAnswerValid,
                            fulfilledTooltip = "角色名有效",
                            notFulfilledTooltip = pendingAnswerInvalidReason ?: "角色名无效",
                            modifier = Modifier.padding(start = Spacing.medium),
                        )
                    }
                }
            }

            is FormQuestion.CombatTargetQuestion -> {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        var text: String by remember { mutableStateOf("") }
                        LaunchedEffect(Unit) {
                            onFormAnswer(FreeformTextAnswer(""))
                        }
                        RiftTextField(
                            text = text,
                            placeholder = formQuestion.placeholder,
                            onTextChanged = {
                                text = it
                                onFormAnswer(FreeformTextAnswer(it.trim()))
                            },
                            modifier = Modifier.weight(1f),
                        )
                        if (recentTargets.isNotEmpty()) {
                            Spacer(Modifier.width(Spacing.medium))
                            RiftDropdown(
                                items = recentTargets.toList(),
                                selectedItem = "最近目标",
                                onItemSelected = {
                                    text = it
                                    onFormAnswer(FreeformTextAnswer(it.trim()))
                                },
                                getItemName = {
                                    if (it.length > 20) it.take(20) + "…" else it
                                },
                            )
                        }
                    }
                    val helpText = if (recentTargets.isNotEmpty()) {
                        "可从上方「最近目标」下拉选择。"
                    } else {
                        "在游戏内锁定/攻击目标后，可将目标名填入上方。"
                    }
                    Text(
                        text = helpText,
                        style = RiftTheme.typography.bodyPrimary,
                        modifier = Modifier.padding(top = Spacing.medium),
                    )
                }
            }

            is FormQuestion.PlanetaryIndustryColoniesQuestion -> {
                AnimatedContent(colonies.isNotEmpty()) { isNotEmpty ->
                    Column {
                        if (isNotEmpty) {
                            Text(
                                text = "留空表示任意殖民地。",
                                style = RiftTheme.typography.bodySecondary,
                                modifier = Modifier.padding(bottom = Spacing.small),
                            )
                            LaunchedEffect(Unit) {
                                onFormAnswer(PlanetaryIndustryColoniesAnswer(emptyList()))
                            }
                            ScrollbarColumn(
                                modifier = Modifier.heightIn(max = 200.dp),
                            ) {
                                var selected: List<String> by remember { mutableStateOf(emptyList()) }
                                for (item in colonies) {
                                    val id = item.colony.id
                                    ListSelectorRow(
                                        text = item.colony.planet.name,
                                        description = item.characterName,
                                        isMultipleChoice = true,
                                        isSelected = id in selected,
                                        onSelect = {
                                            if (item.colony.id in selected) selected -= id else selected += id
                                            onFormAnswer(PlanetaryIndustryColoniesAnswer(selected))
                                        },
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "暂无殖民地。\n请打开行星工业窗口刷新。",
                                style = RiftTheme.typography.headerPrimary,
                            )
                        }
                    }
                }
            }

            is FormQuestion.FreeformTextQuestion -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                ) {
                    var isRegex by remember { mutableStateOf(false) }
                    var text: String by remember { mutableStateOf("") }
                    var testText: String by remember { mutableStateOf("") }

                    val regex = if (isRegex) text.toRegexOrNull(RegexOption.IGNORE_CASE) else null
                    val match = regex?.find(testText)

                    LaunchedEffect(text, regex) {
                        val answer = if (regex != null) text else text.trim()
                        onFormAnswer(FreeformTextAnswer(answer, regex != null))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        RiftTextField(
                            text = text,
                            placeholder = formQuestion.placeholder,
                            onTextChanged = { text = it },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (formQuestion.isRegexAllowed) {
                        RiftCheckboxWithLabel(
                            label = "使用正则",
                            tooltip = "过滤条件将按正则表达式解析。\n匹配不区分大小写。\n\n可在网上查阅正则表达式教程。",
                            isChecked = isRegex,
                            onCheckedChange = { isRegex = it },
                        )
                        AnimatedVisibility(isRegex) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                RiftTextField(
                                    text = testText,
                                    placeholder = "输入测试文本，检查是否匹配",
                                    onTextChanged = {
                                        testText = it
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                                AnimatedContent(match, contentKey = { it != null }) { match ->
                                    if (match != null) {
                                        Text(
                                            text = buildAnnotatedString {
                                                val trimmed = match.value.trim()
                                                if (trimmed.isNotBlank()) {
                                                    append("匹配：")
                                                    withColor(RiftTheme.colors.textSpecialHighlighted) {
                                                        append(trimmed)
                                                    }
                                                } else {
                                                    append("有匹配")
                                                }
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.MiddleEllipsis,
                                            style = RiftTheme.typography.bodyPrimary,
                                            modifier = Modifier
                                                .padding(start = Spacing.medium)
                                                .widthIn(max = 150.dp),
                                        )
                                    }
                                }
                                RequirementIcon(
                                    isFulfilled = match != null,
                                    fulfilledTooltip = "该消息会触发此告警",
                                    notFulfilledTooltip = "该消息不会触发此告警",
                                    modifier = Modifier.padding(start = Spacing.small),
                                )
                            }
                        }
                    }
                }
            }

            is FormQuestion.ContactsLabelQuestion -> {
                if (labels.isNotEmpty()) {
                    ScrollbarColumn(
                        modifier = Modifier.heightIn(max = 200.dp),
                    ) {
                        var selected: List<Label> by remember { mutableStateOf(emptyList()) }
                        for (label in labels) {
                            ListSelectorRow(
                                text = label.name,
                                description = label.owner.name,
                                isMultipleChoice = true,
                                isSelected = label in selected,
                                onSelect = {
                                    if (label in selected) selected -= label else selected += label
                                    onFormAnswer(ContactsLabelAnswer(selected))
                                },
                            )
                        }
                    }
                } else {
                    Text(
                        text = "暂无联系人标签。\n请打开联系人窗口。",
                        style = RiftTheme.typography.headerPrimary,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListSelectorRow(
    text: String,
    description: String?,
    isMultipleChoice: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
    rightContent: @Composable () -> Unit = {},
) {
    val pointerInteractionStateHolder = remember { PointerInteractionStateHolder() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = Modifier
            .fillMaxWidth()
            .hoverBackground(pointerInteractionStateHolder = pointerInteractionStateHolder)
            .onClick { onSelect() }
            .padding(Spacing.medium),
    ) {
        if (isMultipleChoice) {
            RiftCheckbox(
                isChecked = isSelected,
                onCheckedChange = { onSelect() },
                pointerInteractionStateHolder = pointerInteractionStateHolder,
            )
        } else {
            RiftRadioButton(
                isChecked = isSelected,
                onChecked = onSelect,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
        ) {
            val style = if (isSelected) {
                RiftTheme.typography.headerPrimary.copy(color = RiftTheme.colors.primary)
            } else {
                RiftTheme.typography.headerPrimary
            }
            Text(
                text = text,
                style = style,
            )
            if (description != null) {
                Text(
                    text = description,
                    style = RiftTheme.typography.bodySecondary,
                )
            }
        }
        rightContent()
    }
}

private fun Pair<FormQuestion, FormAnswer>.toAnswerString(
    characters: List<LocalCharacter>,
): String? {
    val question = first
    val answer = second
    return when (question) {
        is FormQuestion.SingleChoiceQuestion -> {
            val id = (answer as SingleChoiceAnswer).id
            question.items.first { it.id == id }.text
        }

        is FormQuestion.MultipleChoiceQuestion -> {
            val ids = (answer as MultipleChoiceAnswer).ids
            question.items.filter { it.id in ids }.joinToString { it.text }
        }

        is FormQuestion.SystemQuestion -> {
            (answer as SystemAnswer).system.takeIf { it.isNotEmpty() }
        }

        is FormQuestion.JumpsRangeQuestion -> {
            val (min, max) = (answer as JumpsRangeAnswer).let { it.minJumps to it.maxJumps }
            if (min == 0 && max == 0) {
                "仅本星系"
            } else if (min == 0) {
                "最远 $max 跳"
            } else if (min == max) {
                "正好 $max 跳"
            } else {
                "$min–$max 跳"
            }
        }

        is FormQuestion.OwnedCharacterQuestion -> {
            val characterId = (answer as CharacterAnswer).characterId
            characters.firstOrNull { it.characterId == characterId }?.info?.name ?: "$characterId"
        }

        is FormQuestion.IntelChannelQuestion -> {
            (answer as IntelChannelAnswer).channel
        }

        is FormQuestion.SoundQuestion -> {
            when (val soundAnswer = answer as SoundAnswer) {
                is SoundAnswer.BuiltInSound -> soundAnswer.item.name
                is SoundAnswer.CustomSound -> Path.of(soundAnswer.path).nameWithoutExtension
            }
        }

        is FormQuestion.SpecificCharactersQuestion -> {
            val specificCharacters = (answer as SpecificCharactersAnswer).characters
            when {
                specificCharacters.isEmpty() -> null
                specificCharacters.size == 1 -> specificCharacters.single()
                else -> "${specificCharacters.size} 个指定角色"
            }
        }

        is FormQuestion.CombatTargetQuestion -> {
            (answer as FreeformTextAnswer).text.takeIf { it.isNotBlank() }
        }

        is FormQuestion.PlanetaryIndustryColoniesQuestion -> {
            val colonies = (answer as PlanetaryIndustryColoniesAnswer).colonies
            when {
                colonies.isEmpty() -> "任意殖民地"
                colonies.size == 1 -> "指定殖民地"
                else -> "${colonies.size} 个指定殖民地"
            }
        }

        is FormQuestion.FreeformTextQuestion -> {
            (answer as FreeformTextAnswer).text.takeIf { it.isNotBlank() }
        }

        is FormQuestion.ContactsLabelQuestion -> {
            val labels = (answer as ContactsLabelAnswer).labels
            if (labels.size == 1) {
                labels.single().name
            } else {
                "${labels.size} 个标签"
            }
        }
    }
}
