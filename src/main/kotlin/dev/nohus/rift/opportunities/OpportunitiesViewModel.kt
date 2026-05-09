package dev.nohus.rift.opportunities

import dev.nohus.rift.ViewModel
import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.compose.text.toPlainString
import dev.nohus.rift.game.GameUiController
import dev.nohus.rift.network.esi.models.OpportunityState
import dev.nohus.rift.repositories.IdRanges
import dev.nohus.rift.utils.sumOfDouble
import dev.nohus.rift.utils.toggle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory
import java.time.Duration
import java.time.Instant
import kotlin.collections.filter

@Factory
class OpportunitiesViewModel(
    private val corporationProjectsRepository: CorporationProjectsRepository,
    private val freelanceJobsRepository: FreelanceJobsRepository,
    private val gameUiController: GameUiController,
    private val localCharactersRepository: LocalCharactersRepository,
) : ViewModel() {

    data class UiState(
        val opportunities: List<Opportunity>? = null,
        val corporations: List<Corporation> = emptyList(),
        val view: View = View.ProjectsView,
        val corporationProjectsStats: CorporationProjectsStats? = null,
        val freelanceJobsStats: FreelanceJobsStats? = null,
        val lifecycleFilter: OpportunityLifecycleFilter = OpportunityLifecycleFilter.Active,
        val categoryFilters: CategoryFilters = CategoryFilters(),
        val corporationFilter: CorporationFilter = CorporationFilter.All,
        val participatingFilter: Boolean = false,
        val participatingCount: Int = 0,
        var isFiltersShown: Boolean = false,
        val sorting: OpportunitySorting = OpportunitySorting.Name,
        val search: String? = null,
        val isLoading: Boolean = false,
    ) {
        val primaryFilter get() = categoryFilters.enabled.firstOrNull().takeIf { !participatingFilter }
    }

    data class CorporationProjectsStats(
        val availableToYou: Double,
        val availableTotal: Double,
        val active: Int,
        val completedToday: Int,
        val completedThisWeek: Int,
    )

    data class FreelanceJobsStats(
        val available: Int,
        val corporations: Int,
        val accepted: Int,
    )

    sealed interface CorporationFilter {
        data class SpecificCorporation(val corporation: Corporation) : CorporationFilter
        data object MemberCorporations : CorporationFilter
        data object All : CorporationFilter
    }

    data class CategoryFilters(
        /**
         * Filters that have at least 1 opportunity matching it
         */
        val applicable: Set<OpportunityCategoryFilter> = emptySet(),

        /**
         * Filters that are currently enabled
         */
        val enabled: Set<OpportunityCategoryFilter> = emptySet(),

        /**
         * Count of opportunities for each filter
         */
        val opportunityCount: Map<OpportunityCategoryFilter?, Int> = emptyMap(),
    )

    sealed interface View {
        data object ProjectsView : View
        data class DetailsView(val opportunity: Opportunity) : View
    }

    enum class OpportunityLifecycleFilter {
        Active,
        History,
    }

    enum class OpportunitySorting {
        Name,
        NameReversed,
        DateCreated,
        DateCreatedReversed,
        TimeRemaining,
        TimeRemainingReversed,
        Progress,
        ProgressReversed,
        NumberOfJumps,
        NumberOfJumpsReversed,
        LastUpdated,
        LastUpdatedReversed,
    }

    private val _state = MutableStateFlow(
        UiState(
            isLoading = corporationProjectsRepository.projects.value.isLoading || freelanceJobsRepository.projects.value.isLoading,
        ),
    )
    val state = _state.asStateFlow()

    init {
        updateLoading()
        viewModelScope.launch {
            corporationProjectsRepository.projects.collect { projects ->
                updateLoading()
                updateOpportunities()
            }
        }
        viewModelScope.launch {
            freelanceJobsRepository.projects.collect { jobs ->
                updateLoading()
                updateOpportunities()
            }
        }
    }

    private fun updateLoading() {
        val isProjectsLoading = corporationProjectsRepository.projects.value.isLoading
        val isJobsLoading = freelanceJobsRepository.projects.value.isLoading
        _state.update { it.copy(isLoading = isProjectsLoading || isJobsLoading) }
    }

    fun onVisibilityChange(visible: Boolean) {
        viewModelScope.launch {
            corporationProjectsRepository.setNeedsRealtimeUpdates(visible)
            freelanceJobsRepository.setNeedsRealtimeUpdates(visible)
            if (visible) {
                corporationProjectsRepository.reload()
                freelanceJobsRepository.reload()
            }
        }
    }

    fun onLifecycleFilterChange(filter: OpportunityLifecycleFilter) {
        _state.update { it.copy(lifecycleFilter = filter) }
        updateOpportunities()
    }

    fun onIsFiltersShownChange(isShown: Boolean) {
        _state.update { it.copy(isFiltersShown = isShown) }
    }

    fun onCategoryFilterChange(filter: OpportunityCategoryFilter) {
        when (_state.value.view) {
            is View.DetailsView -> {
                _state.update {
                    it.copy(
                        view = View.ProjectsView,
                        categoryFilters = it.categoryFilters.copy(
                            enabled = if (filter !in it.categoryFilters.enabled) {
                                it.categoryFilters.enabled + filter
                            } else {
                                it.categoryFilters.enabled
                            },
                        ),
                    )
                }
            }
            View.ProjectsView -> {
                _state.update {
                    it.copy(categoryFilters = it.categoryFilters.copy(enabled = it.categoryFilters.enabled.toggle(filter)))
                }
            }
        }
        updateOpportunities()
    }

    fun onCategoryFilterSet(filter: OpportunityCategoryFilter?) {
        _state.update {
            it.copy(
                view = View.ProjectsView,
                categoryFilters = it.categoryFilters.copy(enabled = setOfNotNull(filter)),
                participatingFilter = false,
            )
        }
        updateOpportunities()
    }

    fun onSortingChange(sorting: OpportunitySorting) {
        _state.update { it.copy(sorting = sorting) }
        updateOpportunities()
    }

    fun onSearchChange(text: String) {
        val search = text.takeIf { it.isNotBlank() }?.trim()
        _state.update { it.copy(search = search) }
        updateOpportunities()
    }

    fun onCorporationFilterSelect(corporationFilter: CorporationFilter) {
        _state.update { it.copy(corporationFilter = corporationFilter) }
        updateOpportunities()
    }

    fun onParticipatingFilterChange(isEnabled: Boolean) {
        _state.update { it.copy(participatingFilter = isEnabled) }
        if (isEnabled) {
            _state.update { it.copy(categoryFilters = it.categoryFilters.copy(enabled = emptySet())) }
        }
        updateOpportunities()
    }

    fun onProjectClick(opportunity: Opportunity) {
        _state.update { it.copy(view = View.DetailsView(opportunity)) }
    }

    fun onViewInGameClick(opportunity: Opportunity) {
        when (opportunity.type) {
            OpportunityType.CorporationProject -> gameUiController.pushCorporationProject(opportunity.id, opportunity.name)
            OpportunityType.FreelanceJob -> gameUiController.pushFreelanceProject(opportunity.id, opportunity.name)
        }
    }

    fun onBackClick() {
        _state.update { it.copy(view = View.ProjectsView) }
    }

    private fun updateOpportunities() {
        // Corporation projects
        val corporationProjectsState = corporationProjectsRepository.projects.value
        val corporationProjects = corporationProjectsState.corporationProjects.flatMap { it.opportunities }

        // Freelance jobs
        val freelanceJobsState = freelanceJobsRepository.projects.value
        val freelanceJobs = freelanceJobsState.jobs.opportunities

        // Member corporations
        val memberCorporations = localCharactersRepository.characters.value.mapNotNull { character ->
            character.info ?: return@mapNotNull null
            if (IdRanges.isNpcCorporation(character.info.corporationId)) return@mapNotNull null
            Corporation(character.info.corporationId, character.info.corporationName)
        }.toSet()
        _state.update { it.copy(corporations = memberCorporations.toList()) }

        val opportunities = corporationProjects + freelanceJobs

        // Primary filter is always kept enabled even when not applicable, as it represents the chosen side navigation page
        val primaryFilter = _state.value.primaryFilter
        val filteredOpportunities = getFilteredOpportunities(opportunities)
        val applicableCategoryFilters = getApplicableCategoryFilters(filteredOpportunities)
        val enabledCategoryFilters = _state.value.categoryFilters.enabled.intersect(applicableCategoryFilters) + setOfNotNull(primaryFilter)
        val categoryFilterOpportunityCounts = getCategoryFilterOpportunityCounts(getOpportunitiesFilteredByLifecycle(opportunities))
        val participatingCount = getOpportunitiesFilteredByLifecycle(getOpportunitiesFilteredByParticipation(opportunities, isEnabled = true)).size

        val corporationProjectsStats = if (primaryFilter == OpportunityCategoryFilter.CorporationProjects) {
            getCorporationProjectsStats(getOpportunitiesFilteredByCorporation(corporationProjects))
        } else {
            null
        }
        val freelanceJobsStats = if (primaryFilter == OpportunityCategoryFilter.FreelanceJobs) {
            getFreelanceJobsStats(freelanceJobs)
        } else {
            null
        }

        _state.update {
            it.copy(
                opportunities = filteredOpportunities,
                corporationProjectsStats = corporationProjectsStats,
                freelanceJobsStats = freelanceJobsStats,
                categoryFilters = it.categoryFilters.copy(
                    applicable = applicableCategoryFilters,
                    enabled = enabledCategoryFilters,
                    opportunityCount = categoryFilterOpportunityCounts,
                ),
                participatingCount = participatingCount,
            )
        }

        // Update the detail view if it's open
        val viewedOpportunityId = (_state.value.view as? View.DetailsView)?.opportunity?.id
        if (viewedOpportunityId != null) {
            val updatedViewedProject = opportunities.firstOrNull { it.id == viewedOpportunityId }
            if (updatedViewedProject != null) {
                _state.update { it.copy(view = View.DetailsView(updatedViewedProject)) }
            }
        }
    }

    private fun getCorporationProjectsStats(opportunities: List<Opportunity>): CorporationProjectsStats {
        val now = Instant.now()
        val todayCutoff = now - Duration.ofDays(1)
        val weekCutoff = now - Duration.ofDays(7)
        val activeProjects = opportunities.filter { it.state == OpportunityState.Active }
        return CorporationProjectsStats(
            availableToYou = activeProjects.sumOfDouble {
                val maxContributionPerCharacter = it.details.participationLimit ?: it.desiredProgress
                val availableContribution = it.contributions.sumOf { contribution ->
                    val contribution = (contribution.contribution.success ?: 0L).coerceAtMost(maxContributionPerCharacter)
                    maxContributionPerCharacter - contribution
                }.coerceAtMost(it.desiredProgress)
                (it.details.rewardPerContribution?.times(availableContribution) ?: 0.0).coerceAtMost(it.reward?.remaining ?: 0.0)
            },
            availableTotal = activeProjects.sumOfDouble {
                it.reward?.remaining ?: 0.0
            },
            active = activeProjects.size,
            completedToday = opportunities.count {
                it.state == OpportunityState.Completed && it.details.finished?.isAfter(todayCutoff) == true
            },
            completedThisWeek = opportunities.count {
                it.state == OpportunityState.Completed && it.details.finished?.isAfter(weekCutoff) == true
            },
        )
    }

    private fun getFreelanceJobsStats(opportunities: List<Opportunity>): FreelanceJobsStats {
        val activeJobs = opportunities.filter { it.state == OpportunityState.Active }
        return FreelanceJobsStats(
            available = activeJobs.size,
            corporations = activeJobs.map { it.creator.corporation.id }.toSet().size,
            accepted = activeJobs.count { it.contributions.isNotEmpty() },
        )
    }

    private fun getFilteredOpportunities(opportunities: List<Opportunity>): List<Opportunity> {
        @Suppress("ComplexRedundantLet")
        val filteredOpportunities = opportunities
            .let { getOpportunitiesFilteredByParticipation(it, state.value.participatingFilter) }
            .let(::getOpportunitiesFilteredByLifecycle)
            .let(::getOpportunitiesFilteredByCorporation)
            .filter {
                state.value.search?.let { search ->
                    val hasMatchingName = it.name.contains(search, ignoreCase = true)
                    val hasMatchingDescription = it.details.description.toPlainString().contains(search, ignoreCase = true)
                    val hasMatchingDebugContent = if (search.startsWith(DEBUG_SEARCH_PREFIX)) {
                        it.details.debugDetails.contains(search.removePrefix(DEBUG_SEARCH_PREFIX))
                    } else {
                        false
                    }
                    hasMatchingName || hasMatchingDescription || hasMatchingDebugContent
                } ?: true
            }
            .filter { project ->
                val enabledFilters = state.value.categoryFilters.enabled
                if (enabledFilters.isEmpty()) return@filter true
                val matchingFilters = project.details.matchingFilters
                enabledFilters.all { it in matchingFilters }
            }

        val comparator = when (state.value.sorting) {
            OpportunitySorting.Name -> compareBy<Opportunity> { it.name.lowercase() }
            OpportunitySorting.NameReversed -> compareByDescending { it.name.lowercase() }
            OpportunitySorting.DateCreated -> compareBy { it.details.created }
            OpportunitySorting.DateCreatedReversed -> compareByDescending { it.details.created }
            OpportunitySorting.TimeRemaining -> compareBy { it.details.expires ?: Instant.MAX }
            OpportunitySorting.TimeRemainingReversed -> compareByDescending { it.details.expires ?: Instant.MAX }
            OpportunitySorting.Progress -> compareBy { it.currentProgress }
            OpportunitySorting.ProgressReversed -> compareByDescending { it.currentProgress }
            OpportunitySorting.NumberOfJumps -> compareBy { it.details.solarSystemChipState?.distance?.toFloat() ?: 0.5f }
            OpportunitySorting.NumberOfJumpsReversed -> compareByDescending { it.details.solarSystemChipState?.distance?.toFloat() ?: 0.5f }
            OpportunitySorting.LastUpdated -> compareBy { it.lastModified }
            OpportunitySorting.LastUpdatedReversed -> compareByDescending { it.lastModified }
        }.thenBy { it.id }
        val sortedProjects = filteredOpportunities.sortedWith(comparator)

        return sortedProjects
    }

    private fun getOpportunitiesFilteredByCorporation(opportunities: List<Opportunity>): List<Opportunity> {
        return opportunities.filter {
            val filter = state.value.corporationFilter
            filter == CorporationFilter.All ||
                filter == CorporationFilter.MemberCorporations && it.creator.corporation in state.value.corporations ||
                filter is CorporationFilter.SpecificCorporation && filter.corporation == it.creator.corporation
        }
    }

    private fun getOpportunitiesFilteredByLifecycle(opportunities: List<Opportunity>): List<Opportunity> {
        return opportunities
            .filter {
                when (state.value.lifecycleFilter) {
                    OpportunityLifecycleFilter.Active -> it.state == OpportunityState.Active
                    OpportunityLifecycleFilter.History -> it.state != OpportunityState.Active
                }
            }
    }

    private fun getOpportunitiesFilteredByParticipation(opportunities: List<Opportunity>, isEnabled: Boolean): List<Opportunity> {
        if (!isEnabled) return opportunities
        return opportunities
            .filter { opportunity -> opportunity.contributions.any { it.contribution.isSuccess } }
            .filter { opportunity ->
                if (state.value.lifecycleFilter == OpportunityLifecycleFilter.History) return@filter true
                val contributionsSum = opportunity.contributions.sumOf { it.contribution.success ?: 0L }
                val limitSum = ((opportunity.details.participationLimit ?: opportunity.desiredProgress) * opportunity.contributions.size)
                    .coerceAtMost(opportunity.desiredProgress)
                contributionsSum < limitSum
            }
    }

    private fun getApplicableCategoryFilters(opportunities: List<Opportunity>): Set<OpportunityCategoryFilter> {
        return opportunities
            .flatMap { it.details.matchingFilters }
            .distinct()
            .sortedBy { it.order }
            .toCollection(LinkedHashSet())
    }

    private fun getCategoryFilterOpportunityCounts(opportunities: List<Opportunity>): Map<OpportunityCategoryFilter?, Int> {
        return opportunities
            .flatMap { it.details.matchingFilters }
            .distinct()
            .associateWith { filter -> opportunities.count { filter in it.details.matchingFilters } }
            .plus(null to opportunities.size)
    }

    companion object {
        private const val DEBUG_SEARCH_PREFIX = "debug:"
    }
}
