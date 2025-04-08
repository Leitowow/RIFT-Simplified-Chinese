package dev.nohus.rift.contacts

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.onClick
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.nohus.rift.characters.repositories.LocalCharactersRepository.LocalCharacter
import dev.nohus.rift.compose.AsyncAllianceLogo
import dev.nohus.rift.compose.AsyncCorporationLogo
import dev.nohus.rift.compose.AsyncPlayerPortrait
import dev.nohus.rift.compose.AsyncTypeIcon
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.ClickableAlliance
import dev.nohus.rift.compose.ClickableCorporation
import dev.nohus.rift.compose.ClickableLocation
import dev.nohus.rift.compose.ClickablePlayer
import dev.nohus.rift.compose.ClickableSystem
import dev.nohus.rift.compose.ContextMenuItem
import dev.nohus.rift.compose.ExpandChevron
import dev.nohus.rift.compose.LoadingSpinner
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftContextMenuArea
import dev.nohus.rift.compose.RiftDropdown
import dev.nohus.rift.compose.RiftImageButton
import dev.nohus.rift.compose.RiftSearchField
import dev.nohus.rift.compose.RiftTabBar
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.ScrollbarLazyColumn
import dev.nohus.rift.compose.Tab
import dev.nohus.rift.compose.hoverBackground
import dev.nohus.rift.compose.modifyIf
import dev.nohus.rift.compose.modifyIfNotNull
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.contacts.ContactsRepository.Contact
import dev.nohus.rift.contacts.ContactsRepository.Entity
import dev.nohus.rift.contacts.ContactsRepository.EntityType
import dev.nohus.rift.contacts.ContactsViewModel.ContactsTab
import dev.nohus.rift.contacts.ContactsViewModel.Filter
import dev.nohus.rift.contacts.ContactsViewModel.UiState
import dev.nohus.rift.contacts.SearchRepository.SearchCategory
import dev.nohus.rift.contacts.SearchRepository.SearchResult
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.constellation
import dev.nohus.rift.generated.resources.contact_allcontacts
import dev.nohus.rift.generated.resources.contact_alliance
import dev.nohus.rift.generated.resources.contact_blocked
import dev.nohus.rift.generated.resources.contact_character
import dev.nohus.rift.generated.resources.contact_corporation
import dev.nohus.rift.generated.resources.contact_faction
import dev.nohus.rift.generated.resources.contact_standings
import dev.nohus.rift.generated.resources.contact_tag
import dev.nohus.rift.generated.resources.contact_watched
import dev.nohus.rift.generated.resources.flag_background
import dev.nohus.rift.generated.resources.flag_negative
import dev.nohus.rift.generated.resources.flag_neutral
import dev.nohus.rift.generated.resources.flag_positive
import dev.nohus.rift.generated.resources.region
import dev.nohus.rift.generated.resources.window_contacts
import dev.nohus.rift.generated.resources.window_titlebar_tune
import dev.nohus.rift.network.AsyncResource
import dev.nohus.rift.standings.Standing
import dev.nohus.rift.standings.StandingUtils
import dev.nohus.rift.standings.StandingUtils.formatStanding
import dev.nohus.rift.standings.getFlagColor
import dev.nohus.rift.utils.toggle
import dev.nohus.rift.utils.viewModel
import dev.nohus.rift.utils.withColor
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun ContactsWindow(
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: ContactsViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    RiftWindow(
        title = "Contacts",
        icon = Res.drawable.window_contacts,
        state = windowState,
        onCloseClick = onCloseRequest,
        titleBarContent = { height ->
            ToolbarRow(
                state = state,
                fixedHeight = height,
                onTabSelected = viewModel::onTabSelected,
            )
        },
        withContentPadding = false,
    ) {
        ContactsWindowContent(
            state = state,
            onFilterClick = viewModel::onFilterClick,
            onContactSearchChange = viewModel::onContactSearchChange,
            onEdit = viewModel::onEdit,
            onReloadClick = viewModel::onReloadClick,
            onSearchCategoriesChange = viewModel::onSearchCategoriesChange,
            onSearchChange = viewModel::onSearchChange,
            onSearchConfirm = viewModel::onSearchConfirm,
        )

        val editDialog = state.editDialog
        if (editDialog != null) {
            EditContactDialog(
                dialog = editDialog,
                parentWindowState = windowState,
                onConfirmClick = viewModel::onEditConfirmClick,
                onDeleteClick = viewModel::onDeleteClick,
                onDismiss = viewModel::onEditDismiss,
            )
        }
    }
}

@Composable
private fun ContactsWindowContent(
    state: UiState,
    onFilterClick: (Filter) -> Unit,
    onContactSearchChange: (String) -> Unit,
    onEdit: (entity: Entity, owner: Entity) -> Unit,
    onReloadClick: () -> Unit,
    onSearchCategoriesChange: (List<SearchCategory>) -> Unit,
    onSearchChange: (String) -> Unit,
    onSearchConfirm: (LocalCharacter?) -> Unit,
) {
    Column {
        val offset = LocalDensity.current.run { 1.dp.toPx() }
        Box(
            modifier = Modifier
                .graphicsLayer(translationY = -offset)
                .fillMaxWidth()
                .height(1.dp)
                .background(RiftTheme.colors.borderGreyLight),
        )
        Box(
            modifier = Modifier.padding(Spacing.large),
        ) {
            when (state.selectedTab) {
                ContactsTab.Contacts -> ContactsTabContent(
                    state = state,
                    onFilterClick = onFilterClick,
                    onSearchChange = onContactSearchChange,
                    onEdit = onEdit,
                    onReloadClick = onReloadClick,
                )

                ContactsTab.Search -> {
                    SearchTabContent(
                        state = state,
                        onSearchChange = onSearchChange,
                        onSearchConfirm = onSearchConfirm,
                        onSearchCategoriesChange = onSearchCategoriesChange,
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactsTabContent(
    state: UiState,
    onFilterClick: (Filter) -> Unit,
    onSearchChange: (String) -> Unit,
    onEdit: (entity: Entity, owner: Entity) -> Unit,
    onReloadClick: () -> Unit,
) {
    Row {
        ContactFilters(state, onFilterClick)
        Column {
            Row(
                modifier = Modifier
                    .padding(start = Spacing.medium, bottom = Spacing.medium),
            ) {
                Spacer(Modifier.weight(1f))
                RiftSearchField(
                    search = state.contactSearch,
                    isCompact = false,
                    onSearchChange = onSearchChange,
                )
            }
            ContactsList(
                state = state,
                onEdit = onEdit,
                onReloadClick = onReloadClick,
            )
        }
    }
}

@Composable
private fun ToolbarRow(
    state: UiState,
    fixedHeight: Dp,
    onTabSelected: (ContactsTab) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RiftTabBar(
            tabs = listOf(
                Tab(
                    id = ContactsTab.Contacts.ordinal,
                    title = "Contacts",
                    isCloseable = false,
                ),
                Tab(
                    id = ContactsTab.Search.ordinal,
                    title = "Search",
                    isCloseable = false,
                ),
            ),
            selectedTab = state.selectedTab.ordinal,
            onTabSelected = { tab ->
                onTabSelected(ContactsTab.entries.firstOrNull { it.ordinal == tab } ?: ContactsTab.Contacts)
            },
            onTabClosed = {},
            withUnderline = false,
            withWideTabs = true,
            fixedHeight = fixedHeight,
        )
    }
}

@Composable
private fun ContactsList(
    state: UiState,
    onEdit: (entity: Entity, owner: Entity) -> Unit,
    onReloadClick: () -> Unit,
) {
    ScrollbarLazyColumn {
        if (state.filteredContacts.isNotEmpty()) {
            items(state.filteredContacts, key = { "${it.owner.id}-${it.entity.id}" }) {
                Contact(
                    contact = it,
                    onEdit = { onEdit(it.entity, it.owner) },
                    modifier = Modifier.animateItem(),
                )
            }
        } else {
            item {
                EmptyState(state.isContactsLoading, state.contacts)
            }
        }

        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.medium),
            ) {
                if (state.contacts.isNotEmpty()) {
                    Text("Delayed up to 5 minutes")
                }
                AnimatedContent(
                    state.isContactsLoading,
                    modifier = Modifier
                        .height(36.dp)
                        .padding(start = Spacing.medium),
                ) {
                    if (it) {
                        LoadingSpinner(
                            modifier = Modifier.size(36.dp),
                        )
                    } else {
                        RiftButton(
                            text = "Reload",
                            type = ButtonType.Secondary,
                            onClick = onReloadClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactFilters(
    state: UiState,
    onFilterClick: (Filter) -> Unit,
) {
    ScrollbarColumn(
        hasScrollbarBackground = true,
        isFillWidth = false,
    ) {
        Column(
            modifier = Modifier
                .width(230.dp)
                .padding(end = Spacing.medium),
        ) {
            ContactFilterRow(
                image = Res.drawable.contact_allcontacts,
                text = "All Contacts [${state.contacts.size}]",
                currentFilter = state.filter,
                filter = Filter.All,
                onClick = onFilterClick,
            )

            Spacer(Modifier.height(Spacing.medium))
            ContactFilterRow(
                image = Res.drawable.contact_standings,
                text = "Standings",
                currentFilter = state.filter,
                filter = null,
            )
            ContactFilterRow(
                imageContent = { FlagIcon(10f, Modifier.padding(Spacing.verySmall)) },
                text = "Excellent Standing [${state.contacts.count { it.standingLevel == Standing.Excellent }}]",
                currentFilter = state.filter,
                filter = Filter.Standings(Standing.Excellent),
                onClick = onFilterClick,
            )
            ContactFilterRow(
                imageContent = { FlagIcon(5f, Modifier.padding(Spacing.verySmall)) },
                text = "Good Standing [${state.contacts.count { it.standingLevel == Standing.Good }}]",
                currentFilter = state.filter,
                filter = Filter.Standings(Standing.Good),
                onClick = onFilterClick,
            )
            ContactFilterRow(
                imageContent = { FlagIcon(0f, Modifier.padding(Spacing.verySmall)) },
                text = "Neutral Standing [${state.contacts.count { it.standingLevel == Standing.Neutral }}]",
                currentFilter = state.filter,
                filter = Filter.Standings(Standing.Neutral),
                onClick = onFilterClick,
            )
            ContactFilterRow(
                imageContent = { FlagIcon(-5f, Modifier.padding(Spacing.verySmall)) },
                text = "Bad Standing [${state.contacts.count { it.standingLevel == Standing.Bad }}]",
                currentFilter = state.filter,
                filter = Filter.Standings(Standing.Bad),
                onClick = onFilterClick,
            )
            ContactFilterRow(
                imageContent = { FlagIcon(-10f, Modifier.padding(Spacing.verySmall)) },
                text = "Terrible Standing [${state.contacts.count { it.standingLevel == Standing.Terrible }}]",
                currentFilter = state.filter,
                filter = Filter.Standings(Standing.Terrible),
                onClick = onFilterClick,
            )

            Spacer(Modifier.height(Spacing.medium))
            ContactFilterRow(
                image = Res.drawable.contact_tag,
                text = "Labels",
                currentFilter = state.filter,
                filter = null,
            )

            for (owner in state.ownerLabels) {
                val ownerContacts = state.contacts.filter { it.owner == owner.first }
                ContactOwnerRow(
                    entity = owner.first,
                    count = ownerContacts.size,
                    currentFilter = state.filter,
                    filter = Filter.Owner(owner.first),
                    onClick = onFilterClick,
                )
                if (owner.second.isNotEmpty()) {
                    ContactLabelRow(
                        text = "No label",
                        count = ownerContacts.count { it.labels.isEmpty() },
                        currentFilter = state.filter,
                        filter = Filter.Unlabeled(owner.first),
                        onClick = onFilterClick,
                    )
                }
                for (label in owner.second) {
                    ContactLabelRow(
                        text = label,
                        count = ownerContacts.count { label in it.labels },
                        currentFilter = state.filter,
                        filter = Filter.Label(owner.first, label),
                        onClick = onFilterClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactOwnerRow(
    entity: Entity,
    count: Int,
    currentFilter: Filter,
    filter: Filter,
    onClick: (Filter) -> Unit,
) {
    ContactFilterRow(
        imageContent = { EntityImage(entity, 16) },
        text = "${entity.name} [$count]",
        textStyle = RiftTheme.typography.bodyHighlighted,
        currentFilter = currentFilter,
        filter = filter,
        onClick = onClick,
    )
}

@Composable
private fun ContactLabelRow(
    text: String,
    count: Int,
    currentFilter: Filter,
    filter: Filter,
    onClick: (Filter) -> Unit,
) {
    ContactFilterRow(
        imageContent = { Spacer(Modifier.width(16.dp)) },
        text = "$text [$count]",
        normalColor = null,
        currentFilter = currentFilter,
        filter = filter,
        onClick = onClick,
    )
}

@Composable
private fun ContactFilterRow(
    image: DrawableResource,
    text: String,
    currentFilter: Filter,
    filter: Filter?,
    onClick: ((Filter) -> Unit)? = null,
) {
    ContactFilterRow(
        imageContent = {
            Image(
                painter = painterResource(image),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        },
        text = text,
        currentFilter = currentFilter,
        filter = filter,
        onClick = onClick,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContactFilterRow(
    imageContent: @Composable () -> Unit,
    text: String,
    textStyle: TextStyle = RiftTheme.typography.bodyPrimary,
    normalColor: Color? = RiftTheme.colors.windowBackgroundSecondary,
    currentFilter: Filter,
    filter: Filter?,
    onClick: ((Filter) -> Unit)?,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = Modifier
            .modifyIfNotNull(filter) { filter ->
                modifyIfNotNull(onClick) { onClick ->
                    onClick { onClick(filter) }
                }
            }
            .modifyIf(filter != null) {
                hoverBackground(
                    hoverColor = RiftTheme.colors.backgroundSelected,
                    pressColor = RiftTheme.colors.backgroundPrimaryLight,
                    normalColor = if (filter == currentFilter) RiftTheme.colors.backgroundSelected else normalColor,
                )
            }
            .fillMaxWidth()
            .padding(Spacing.small),
    ) {
        imageContent()
        Text(
            text = text,
            style = textStyle,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Contact(
    contact: Contact,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        ClickableEntity(
            id = contact.entity.id,
            type = contact.entity.type,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier
                    .padding(horizontal = Spacing.medium)
                    .onClick { onEdit() }
                    .hoverBackground()
                    .padding(vertical = Spacing.verySmall)
                    .padding(start = Spacing.verySmall)
                    .padding(end = Spacing.small),
            ) {
                ContactOwner(contact.owner)
                ContactType(contact.entity.type)
                EntityImage(contact.entity, 32)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = Spacing.medium),
                ) {
                    Text(
                        text = contact.entity.name,
                        style = RiftTheme.typography.titlePrimary,
                    )
                    if (contact.labels.isNotEmpty()) {
                        Text(
                            text = contact.labels.joinToString(", "),
                            style = RiftTheme.typography.bodySecondary,
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                ) {
                    Text(
                        text = formatStanding(contact.standing),
                        style = RiftTheme.typography.bodyPrimary,
                    )
                    if (contact.isBlocked) BlockIcon()
                    if (contact.isWatched) WatchIcon()
                    FlagIcon(contact.standing)
                }
            }
        }
    }
}

@Composable
private fun WatchIcon() {
    RiftTooltipArea("You are watching this character") {
        Box(
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.contact_watched),
                contentDescription = null,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

@Composable
private fun BlockIcon() {
    RiftTooltipArea("You have blocked this character") {
        Box(
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.contact_blocked),
                contentDescription = null,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

@Composable
private fun FlagIcon(standing: Float, modifier: Modifier = Modifier) {
    val level = StandingUtils.getStandingLevel(standing)
    val tooltip = when (level) {
        Standing.Terrible -> "Pilot has Terrible Standing"
        Standing.Bad -> "Pilot has Bad Standing"
        Standing.Neutral -> "Pilot has No Standing"
        Standing.Good -> "Pilot has Good Standing"
        Standing.Excellent -> "Pilot has Excellent Standing"
    }
    RiftTooltipArea(tooltip, modifier) {
        Box(
            contentAlignment = Alignment.Center,
        ) {
            val tint = level.getFlagColor().copy(alpha = 0.75f)
            Image(
                painter = painterResource(Res.drawable.flag_background),
                contentDescription = null,
                colorFilter = ColorFilter.tint(tint),
                modifier = Modifier.size(12.dp),
            )
            val icon = when (level) {
                Standing.Terrible, Standing.Bad -> Res.drawable.flag_negative
                Standing.Neutral -> Res.drawable.flag_neutral
                Standing.Good, Standing.Excellent -> Res.drawable.flag_positive
            }
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(8.dp),
            )
        }
    }
}

@Composable
private fun ContactOwner(owner: Entity) {
    val tooltip = buildAnnotatedString {
        val text = when (owner.type) {
            EntityType.Character -> "This is a contact of your character"
            EntityType.Corporation -> "This is a contact of your corporation"
            EntityType.Alliance -> "This is a contact of your alliance"
            EntityType.Faction -> ""
        }
        appendLine(text)
        withColor(RiftTheme.colors.textHighlighted) {
            append(owner.name)
        }
    }
    RiftTooltipArea(tooltip) {
        EntityImage(owner, 32)
    }
}

@Composable
private fun ContactType(type: EntityType) {
    val tooltip = when (type) {
        EntityType.Character -> "This contact is a character"
        EntityType.Corporation -> "This contact is a corporation"
        EntityType.Alliance -> "This contact is an alliance"
        EntityType.Faction -> "This contact is a faction"
    }
    RiftTooltipArea(tooltip) {
        when (type) {
            EntityType.Character -> Image(
                painter = painterResource(Res.drawable.contact_character),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )

            EntityType.Corporation -> Image(
                painter = painterResource(Res.drawable.contact_corporation),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )

            EntityType.Alliance -> Image(
                painter = painterResource(Res.drawable.contact_alliance),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )

            EntityType.Faction -> Image(
                painter = painterResource(Res.drawable.contact_faction),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

@Composable
private fun EmptyState(isLoading: Boolean, contacts: List<Contact>) {
    Text(
        text = if (isLoading) {
            "Loading contacts…"
        } else if (contacts.isNotEmpty()) {
            "All contacts filtered out"
        } else {
            "No contacts loaded"
        },
        style = RiftTheme.typography.titlePrimary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.large),
    )
}

@Composable
private fun SearchTabContent(
    state: UiState,
    onSearchChange: (String) -> Unit,
    onSearchConfirm: (LocalCharacter?) -> Unit,
    onSearchCategoriesChange: (List<SearchCategory>) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.large),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.mediumLarge),
        ) {
            var selectedCharacter: LocalCharacter? by remember { mutableStateOf(state.characters.firstOrNull()) }
            RiftSearchField(
                search = state.search,
                isCompact = false,
                onSearchChange = onSearchChange,
                onSearchConfirm = { onSearchConfirm(selectedCharacter) },
                modifier = Modifier.weight(1f),
            )
            RiftTooltipArea("Character to search from.\nThis determines non-public structure results.") {
                RiftDropdown(
                    items = state.characters,
                    selectedItem = selectedCharacter,
                    onItemSelected = { selectedCharacter = it },
                    getItemName = { it?.let { it.info.success?.name ?: "${it.characterId}" } ?: "Select character" },
                )
            }
            RiftContextMenuArea(
                items = buildList {
                    add(
                        ContextMenuItem.HeaderItem(
                            text = "Search Type",
                        ),
                    )
                    add(
                        ContextMenuItem.RadioItem(
                            text = "Any",
                            isSelected = state.searchCategories.size == SearchCategory.entries.size,
                            onClick = { onSearchCategoriesChange(SearchCategory.entries) },
                        ),
                    )
                    for (category in SearchCategory.entries) {
                        add(
                            ContextMenuItem.RadioItem(
                                text = category.displayName,
                                isSelected = state.searchCategories.size == 1 && category in state.searchCategories,
                                onClick = { onSearchCategoriesChange(listOf(category)) },
                            ),
                        )
                    }
                },
                acceptsLeftClick = true,
            ) {
                RiftImageButton(
                    resource = Res.drawable.window_titlebar_tune,
                    size = 16.dp,
                    onClick = {},
                )
            }
        }
        when (val results = state.searchResults) {
            is AsyncResource.Error -> {
                val message = results.exception?.message ?: ""
                val text = if ("No results" in message) {
                    "No results"
                } else if ("does not meet the minimum length of 3" in message) {
                    "The search term needs to be at least 3 characters long"
                } else if ("'search' is required" in message) {
                    "You need to enter a search term"
                } else if ("timeout" in message) {
                    "Search request timed out"
                } else if ("No character selected" in message) {
                    "No character selected"
                } else {
                    "Search request failed"
                }
                Text(
                    text = text,
                    style = RiftTheme.typography.headlinePrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            AsyncResource.Loading -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    LoadingSpinner(
                        modifier = Modifier.size(36.dp),
                    )
                }
            }

            is AsyncResource.Ready -> {
                ScrollbarColumn(
                    contentPadding = PaddingValues(end = Spacing.medium),
                    hasScrollbarBackground = false,
                ) {
                    var expandedCategories: List<SearchCategory> by remember { mutableStateOf(emptyList()) }
                    for (category in SearchCategory.entries) {
                        val items = results.value[category] ?: emptyList()
                        if (items.isNotEmpty()) {
                            SearchCategoryHeader(
                                category = category,
                                isExpanded = category in expandedCategories,
                                items = items,
                                onClick = { expandedCategories = expandedCategories.toggle(category) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchCategoryHeader(
    category: SearchCategory,
    isExpanded: Boolean,
    items: List<SearchResult>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(RiftTheme.colors.windowBackgroundSecondary)
                .hoverBackground()
                .padding(vertical = Spacing.small)
                .onClick { onClick() },
        ) {
            ExpandChevron(isExpanded = isExpanded)
            Text(
                text = "${category.displayName} (${items.size})",
                style = RiftTheme.typography.bodyPrimary,
                maxLines = 1,
                overflow = TextOverflow.Visible,
                softWrap = false,
                modifier = Modifier.clipToBounds(),
            )
        }
        AnimatedVisibility(isExpanded) {
            Column {
                for (item in items) {
                    SearchResultRow(category, item)
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    category: SearchCategory,
    item: SearchResult,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .hoverBackground()
            .padding(vertical = Spacing.verySmall)
            .padding(start = Spacing.verySmall)
            .padding(end = Spacing.small),
    ) {
        when (category) {
            SearchCategory.Agents -> AsyncPlayerPortrait(
                characterId = item.id.toInt(),
                size = 32,
                modifier = Modifier.size(32.dp),
            )
            SearchCategory.Alliance -> AsyncAllianceLogo(
                allianceId = item.id.toInt(),
                size = 32,
                modifier = Modifier.size(32.dp),
            )
            SearchCategory.Characters -> {
                ClickablePlayer(item.id.toInt()) {
                    AsyncPlayerPortrait(
                        characterId = item.id.toInt(),
                        size = 32,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
            SearchCategory.Constellation -> {
                Image(
                    painter = painterResource(Res.drawable.constellation),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
            SearchCategory.Corporations -> AsyncCorporationLogo(
                corporationId = item.id.toInt(),
                size = 32,
                modifier = Modifier.size(32.dp),
            )
            SearchCategory.Factions -> AsyncCorporationLogo(
                corporationId = item.id.toInt(),
                size = 32,
                modifier = Modifier.size(32.dp),
            )
            SearchCategory.InventoryTypes -> AsyncTypeIcon(
                typeId = item.id.toInt(),
                nameHint = item.name,
                modifier = Modifier.size(32.dp),
            )
            SearchCategory.Regions -> {
                Image(
                    painter = painterResource(Res.drawable.region),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
            SearchCategory.SolarSystems -> {
                AsyncTypeIcon(
                    typeId = item.typeId,
                    nameHint = item.name,
                    modifier = Modifier.size(32.dp),
                )
            }
            SearchCategory.Stations -> {
                AsyncTypeIcon(
                    typeId = item.typeId,
                    nameHint = item.name,
                    modifier = Modifier.size(32.dp),
                )
            }
            SearchCategory.Structures -> {
                AsyncTypeIcon(
                    typeId = item.typeId,
                    nameHint = item.name,
                    modifier = Modifier.size(32.dp),
                )
            }
        }

        if (item.characterDetails != null) {
            ClickableCorporation(item.characterDetails.corporationId) {
                RiftTooltipArea(item.characterDetails.corporationName ?: "") {
                    AsyncCorporationLogo(
                        corporationId = item.characterDetails.corporationId,
                        size = 32,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
            if (item.characterDetails.allianceId != null) {
                ClickableAlliance(item.characterDetails.allianceId) {
                    RiftTooltipArea(item.characterDetails.allianceName ?: "") {
                        AsyncAllianceLogo(
                            allianceId = item.characterDetails.allianceId,
                            size = 32,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
            } else {
                Spacer(Modifier.size(32.dp))
            }
        }

        Box(Modifier.weight(1f)) {
            val textContent = movableContentOf {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.medium),
                ) {
                    Text(
                        text = item.name,
                        style = RiftTheme.typography.titlePrimary,
                    )
                    if (item.description != null) {
                        Text(
                            text = item.description,
                            style = RiftTheme.typography.bodySecondary,
                        )
                    }
                }
            }
            when (category) {
                SearchCategory.Characters -> {
                    ClickablePlayer(item.id.toInt()) {
                        textContent()
                    }
                }
                SearchCategory.Corporations -> {
                    ClickableCorporation(item.id.toInt()) {
                        textContent()
                    }
                }
                SearchCategory.Alliance -> {
                    ClickableAlliance(item.id.toInt()) {
                        textContent()
                    }
                }
                SearchCategory.Factions -> {
                    ClickableCorporation(item.id.toInt()) {
                        textContent()
                    }
                }
                SearchCategory.SolarSystems -> {
                    ClickableSystem(item.name) {
                        textContent()
                    }
                }
                SearchCategory.Stations -> {
                    ClickableLocation(item.systemId, item.id) {
                        textContent()
                    }
                }
                SearchCategory.Structures -> {
                    ClickableLocation(item.systemId, item.id) {
                        textContent()
                    }
                }
                else -> {
                    textContent()
                }
            }
        }

        if (item.standing != null) {
            FlagIcon(item.standing, Modifier.padding(Spacing.verySmall))
        }
    }
}
