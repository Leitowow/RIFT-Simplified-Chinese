package dev.nohus.rift.opportunities

import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.characters.repositories.LocalCharactersRepository.LocalCharacter
import dev.nohus.rift.network.Result
import dev.nohus.rift.network.Result.Failure
import dev.nohus.rift.network.Result.Success
import dev.nohus.rift.network.esi.EsiApi
import dev.nohus.rift.network.esi.models.CorporationProject
import dev.nohus.rift.network.esi.models.CorporationProjectsQueryState
import dev.nohus.rift.network.esi.models.OpportunityState
import dev.nohus.rift.network.esi.models.ParticipationState
import dev.nohus.rift.network.esi.pagination.fetchCursorPaginated
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.opportunities.OpportunitiesUtils.getMatchingFilters
import dev.nohus.rift.opportunities.OpportunitiesUtils.getSolarSystemChipState
import dev.nohus.rift.repositories.GetSolarSystemChipStateUseCase
import dev.nohus.rift.repositories.IdRanges
import dev.nohus.rift.repositories.character.CharacterDetailsRepository
import dev.nohus.rift.sso.scopes.ScopeGroups
import dev.nohus.rift.utils.mapAsync
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.yield
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.minutes

private val logger = KotlinLogging.logger {}

@Single
class CorporationProjectsRepository(
    private val esiApi: EsiApi,
    private val localCharactersRepository: LocalCharactersRepository,
    private val characterDetailsRepository: CharacterDetailsRepository,
    private val mapper: OpportunitiesMapper,
    private val getSolarSystemChipStateUseCase: GetSolarSystemChipStateUseCase,
    private val getProjectContributionAttributesUseCase: GetOpportunityContributionAttributesUseCase,
) {

    data class Projects(
        val corporationProjects: List<CorporationProjects> = emptyList(),
        val isLoading: Boolean = false,
    )

    data class CorporationProjects(
        val corporation: Corporation,
        val opportunities: List<Opportunity> = emptyList(),
        val deletedProjects: Set<String> = emptySet(),
        val failedProjects: List<Exception?> = emptyList(),
    )

    private val _projects = MutableStateFlow(Projects())
    val projects = _projects.asStateFlow()

    private val reloadRequest = MutableStateFlow(false)
    private val loadingMutex = Mutex()
    private var isRealtime = false
    private var corporationAfterCursors: Map<Int, String?> = emptyMap()

    @OptIn(FlowPreview::class)
    suspend fun start() = coroutineScope {
        launch {
            while (true) {
                delay(1.minutes)
                if (isRealtime) {
                    reloadRequest.value = false
                    yield()
                    reloadRequest.value = true
                }
            }
        }
        launch {
            while (true) {
                delay(15.minutes)
                reloadRequest.value = true
            }
        }
        launch {
            localCharactersRepository.characters.debounce(500).collectLatest {
                reloadRequest.value = true
            }
        }
        launch {
            reloadRequest.filter { it }.collect {
                reloadRequest.value = false
                updateProjects()
            }
        }
    }

    fun reload() {
        if (!loadingMutex.isLocked) reloadRequest.value = true
    }

    fun setNeedsRealtimeUpdates(isRealtime: Boolean) {
        this.isRealtime = isRealtime
    }

    private suspend fun updateProjects() {
        loadingMutex.withLock {
            _projects.update { it.copy(isLoading = true) }
            getAllCorporationProjects().collect { updatedProjects ->
                val existingProjects = _projects.value.corporationProjects
                _projects.update {
                    it.copy(
                        corporationProjects = mergeUpdatedCorporationProjects(existingProjects, updatedProjects),
                    )
                }
            }
            _projects.update { it.copy(isLoading = false) }
        }
    }

    /**
     * Merges updated projects into existing projects
     */
    private fun mergeUpdatedCorporationProjects(
        existing: List<CorporationProjects>,
        updated: List<CorporationProjects>,
    ): List<CorporationProjects> {
        val existingByCorporation = existing.associateBy { it.corporation }
        val updatedByCorporation = updated.associateBy { it.corporation }
        val corporations = existingByCorporation.keys + updatedByCorporation.keys
        return corporations.mapNotNull { corporation ->
            val existing = existingByCorporation[corporation]
            val updated = updatedByCorporation[corporation]
            if (existing != null && updated != null) {
                val existingProjects = existing.opportunities.associateBy { it.id } - updated.deletedProjects
                val updatedProjects = updated.opportunities.associateBy { it.id }
                val mergedProjects = existingProjects + updatedProjects
                CorporationProjects(
                    corporation = corporation,
                    opportunities = mergedProjects.values.toList(),
                    failedProjects = updated.failedProjects,
                )
            } else {
                existing ?: updated
            }
        }
    }

    /**
     * Returns a map of all corporations from local characters and the characters that belong to them,
     * only considering characters that have the read projects scope
     */
    private fun getCorporations(): Map<Corporation, List<LocalCharacter>> {
        return localCharactersRepository.characters.value
            .filter { ScopeGroups.readProjects in it.scopes }
            .mapNotNull { character ->
                val corporation = character.info?.let {
                    Corporation(it.corporationId, it.corporationName)
                } ?: return@mapNotNull null
                corporation to character
            }
            .filterNot { IdRanges.isNpcCorporation(it.first.id) }
            .groupBy({ it.first }, { it.second })
    }

    private fun getAllCorporationProjects(): Flow<List<CorporationProjects>> {
        val flows = getCorporations()
            .entries
            .map { (corporation, characters) ->
                getCorporationProjects(corporation, characters)
            }
        return combine(flows) { latestArray ->
            latestArray.toList()
        }
    }

    /**
     * Returns a flow of all projects from the given corporation since the after cursor
     * Every time a new project result is ready, the flow emits with the updated list of all fetched projects
     */
    private fun getCorporationProjects(
        corporation: Corporation,
        characters: List<LocalCharacter>,
    ): Flow<CorporationProjects> = channelFlow {
        val projectManagers = characters.filter {
            it.info?.corporationRoles?.contains("Project_Manager") == true
        }.map { it.characterId }

        val mutex = Mutex()
        val opportunities: MutableList<Result<Opportunity>> = mutableListOf()
        val newCorporationAfter: MutableMap<Int, String?> = mutableMapOf()
        var deletedProjects: Set<String> = emptySet()
        suspend fun send() {
            val corporationProjects = mutex.withLock {
                CorporationProjects(
                    corporation = corporation,
                    opportunities = opportunities.filterIsInstance<Success<Opportunity>>().map { it.data },
                    deletedProjects = deletedProjects,
                    failedProjects = opportunities.filterIsInstance<Failure>().map { it.cause },
                )
            }
            send(corporationProjects)
        }
        send()

        val semaphore = Semaphore(10)

        val after = corporationAfterCursors[corporation.id]
        fetchCursorPaginated(after) { before, after ->
            esiApi.getCorporationsIdProjects(Originator.CorporationProjects, characters.first().characterId, corporation.id, before, after, state = CorporationProjectsQueryState.All)
        }.map { (projects, newAfter) ->
            deletedProjects = projects.filter { it.state == OpportunityState.Deleted }.map { it.id }.toSet()
            projects
                .filter { it.state != OpportunityState.Deleted }
                .map { project ->
                    async {
                        semaphore.withPermit {
                            getProject(
                                corporation = corporation,
                                characters = characters,
                                project = project,
                                projectManagers = projectManagers,
                            ).also {
                                mutex.withLock {
                                    opportunities += it
                                }
                                send()
                            }
                        }
                    }
                } to newAfter
        }
            .map { (projects, newAfter) -> projects.awaitAll() to newAfter }
            .onFailure { newCorporationAfter -= corporation.id }
            .onSuccess { (projects, newAfter) ->
                newCorporationAfter[corporation.id] = newAfter.takeIf { projects.all { it.isSuccess } }
            }
            .map { it.first }

        if (opportunities.all { it is Success }) {
            corporationAfterCursors = newCorporationAfter.toMap()
        } else {
            logger.error { "There were errors loading projects, resetting cursors" }
            corporationAfterCursors = emptyMap()
        }

        send()
    }

    private suspend fun CoroutineScope.getProject(
        corporation: Corporation,
        characters: List<LocalCharacter>,
        project: CorporationProject,
        projectManagers: List<Int>,
    ): Result<Opportunity> {
        val characterIds = characters.map { it.characterId }
        val cacheBuster = project.lastModified.epochSecond.toString()
        val detailsDeferred = async {
            esiApi.getCorporationsIdProjectsId(Originator.CorporationProjects, characterIds.first(), corporation.id, project.id, cacheBuster)
        }
        val contributionsDeferred = characters.map { character ->
            async {
                val contributionResult = esiApi.getCorporationsIdProjectsIdContribution(Originator.CorporationProjects, character.characterId, corporation.id, project.id, cacheBuster)
                    .map { it.contributed }
                Contribution(
                    characterId = character.characterId,
                    characterName = character.info?.name ?: "?",
                    contribution = contributionResult,
                    participationState = Success(ParticipationState.Unspecified),
                )
            }
        }
        val contributorsDeferred: Deferred<Contributors> = async {
            projectManagers.firstOrNull()?.let { projectManager ->
                fetchCursorPaginated(null) { before, after ->
                    esiApi.getCorporationsIdProjectsIdContributors(
                        originator = Originator.CorporationProjects,
                        characterId = projectManager,
                        corporationId = corporation.id,
                        projectId = project.id,
                        before = before,
                        after = after,
                        cacheBuster = cacheBuster,
                    )
                }.map { (contributors, _) ->
                    val list = contributors.mapAsync {
                        Contributor(
                            characterId = it.id.toInt(),
                            details = characterDetailsRepository.getCharacterDetails(Originator.CorporationProjects, it.id.toInt()),
                            contributed = it.contributed,
                            participationState = ParticipationState.Unspecified,
                        )
                    }
                    if (list.isNotEmpty()) Contributors.Available(list) else Contributors.Empty
                }.let {
                    when (it) {
                        is Failure -> Contributors.Error(it.cause?.message ?: "Unknown error")
                        is Success -> it.data
                    }
                }
            } ?: Contributors.NoAccess
        }
        val details = when (val details = detailsDeferred.await()) {
            is Success -> details.data
            is Failure -> return details
        }
        val configuration = mapper.toModel(details.configuration)
        val contributionAttributesDeferred = async {
            getProjectContributionAttributesUseCase(Originator.CorporationProjects, configuration, characterIds.first())
        }
        val matchingFilters = getMatchingFilters(
            baseType = OpportunityCategoryFilter.CorporationProjects,
            career = details.details.career,
            configuration = configuration,
        )
        val contributionAttributes = contributionAttributesDeferred.await()
        val nonEmptyContributions = contributionsDeferred.awaitAll()
            .filter { contribution -> contribution.contribution.success?.let { it > 0 } == true }
        return mapper.toModel(
            debugDetails = details.toString(),
            corporation = corporation,
            project = project,
            details = details,
            configuration = configuration,
            contributionAttributes = contributionAttributes,
            solarSystemChipState = getSolarSystemChipState(getSolarSystemChipStateUseCase, contributionAttributes),
            matchingFilters = matchingFilters,
            contributions = nonEmptyContributions,
            contributors = contributorsDeferred.await(),
            eligibleCharacters = characters,
        ).let { Success(it) }
    }
}
