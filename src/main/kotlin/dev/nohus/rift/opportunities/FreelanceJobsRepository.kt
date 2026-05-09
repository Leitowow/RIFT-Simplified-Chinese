package dev.nohus.rift.opportunities

import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.characters.repositories.LocalCharactersRepository.LocalCharacter
import dev.nohus.rift.network.Result
import dev.nohus.rift.network.Result.Failure
import dev.nohus.rift.network.Result.Success
import dev.nohus.rift.network.esi.EsiApi
import dev.nohus.rift.network.esi.models.FreelanceJob
import dev.nohus.rift.network.esi.models.OpportunityState
import dev.nohus.rift.network.esi.pagination.fetchCursorPaginated
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.opportunities.OpportunitiesUtils.getMatchingFilters
import dev.nohus.rift.opportunities.OpportunitiesUtils.getSolarSystemChipState
import dev.nohus.rift.repositories.GetSolarSystemChipStateUseCase
import dev.nohus.rift.repositories.character.CharacterDetailsRepository
import dev.nohus.rift.sso.scopes.ScopeGroups
import dev.nohus.rift.utils.mapAsync
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
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
import kotlin.collections.filter
import kotlin.collections.map
import kotlin.time.Duration.Companion.minutes

private val logger = KotlinLogging.logger {}

@Single
class FreelanceJobsRepository(
    private val esiApi: EsiApi,
    private val localCharactersRepository: LocalCharactersRepository,
    private val characterDetailsRepository: CharacterDetailsRepository,
    private val mapper: OpportunitiesMapper,
    private val getSolarSystemChipStateUseCase: GetSolarSystemChipStateUseCase,
    private val getProjectContributionAttributesUseCase: GetOpportunityContributionAttributesUseCase,
) {

    data class Jobs(
        val jobs: FreelanceJobs = FreelanceJobs(),
        val isLoading: Boolean = false,
    )

    data class FreelanceJobs(
        val opportunities: List<Opportunity> = emptyList(),
        val deletedJobs: Set<String> = emptySet(),
        val failedJobs: List<Exception?> = emptyList(),
    )

    private val _projects = MutableStateFlow(Jobs())
    val projects = _projects.asStateFlow()

    private val reloadRequest = MutableStateFlow(false)
    private val loadingMutex = Mutex()
    private var isRealtime = false
    private var publicAfterCursor: String? = null
    private var corporationAfterCursors: Map<Int, String> = emptyMap()
    private var jobIdToCorporationId: Map<String, Int> = emptyMap()

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
                updateJobs()
            }
        }
    }

    fun reload() {
        if (!loadingMutex.isLocked) reloadRequest.value = true
    }

    fun setNeedsRealtimeUpdates(isRealtime: Boolean) {
        this.isRealtime = isRealtime
    }

    private suspend fun updateJobs() {
        loadingMutex.withLock {
            _projects.update { it.copy(isLoading = true) }
            getAllFreelanceJobs().collect { updatedJobs ->
                val existingJobs = _projects.value.jobs.opportunities
                _projects.update {
                    it.copy(
                        jobs = mergeUpdatedFreelanceJobs(existingJobs, updatedJobs),
                    )
                }
            }
            _projects.update { it.copy(isLoading = false) }
        }
    }

    /**
     * Merges updated jobs into existing jobs
     */
    private fun mergeUpdatedFreelanceJobs(
        existing: List<Opportunity>,
        updated: FreelanceJobs,
    ): FreelanceJobs {
        val existingJobs = existing.associateBy { it.id } - updated.deletedJobs
        val updatedJobs = updated.opportunities.associateBy { it.id }
        val mergedJobs = (existingJobs + updatedJobs).values.toList()
        return FreelanceJobs(
            opportunities = mergedJobs,
            deletedJobs = updated.deletedJobs,
            failedJobs = updated.failedJobs,
        )
    }

    /**
     * Returns a flow of all freelance jobs since the after cursors
     * Every time a new job result is ready, the flow emits with the updated list of all fetched jobs
     */
    private fun getAllFreelanceJobs(): Flow<FreelanceJobs> = channelFlow {
        val allCharacters = localCharactersRepository.characters.value
        if (allCharacters.isEmpty()) return@channelFlow
        val corporationIdToProjectManager = allCharacters
            .filter { ScopeGroups.readJobs in it.scopes }
            .filter { it.info?.corporationRoles?.contains("Project_Manager") == true }
            .groupBy { it.info?.corporationId }
            .mapNotNull { (corporationId, projectManagers) ->
                corporationId ?: return@mapNotNull null
                corporationId to projectManagers.first()
            }.toMap()

        val mutex = Mutex()
        val opportunities: MutableList<Result<Opportunity>> = mutableListOf()
        var newPublicAfter: String? = null
        val newCorporationAfter: MutableMap<Int, String> = mutableMapOf()
        var deletedJobs: Set<String> = emptySet()
        suspend fun send() {
            val freelanceJobs = mutex.withLock {
                FreelanceJobs(
                    opportunities = opportunities.filterIsInstance<Success<Opportunity>>().map { it.data },
                    deletedJobs = deletedJobs,
                    failedJobs = opportunities.filterIsInstance<Failure>().map { it.cause },
                )
            }
            send(freelanceJobs)
        }

        val semaphore = Semaphore(10)

        val jobsToParticipatingCharacters = allCharacters
            .filter { ScopeGroups.readJobs in it.scopes }
            .map { character ->
                async {
                    semaphore.withPermit {
                        character to (
                            esiApi.getCharactersIdFreelanceJobs(
                                Originator.FreelanceJobs,
                                character.characterId,
                            ).map { it.items }.success ?: emptyList()
                            )
                    }
                }
            }.awaitAll().flatMap { (character, jobs) ->
                jobs
                    .filter { it.state != OpportunityState.Deleted }
                    .map { it to character }
            }.groupBy { it.first.id }.values.associate { pairs ->
                // Pairs of jobs grouped by ID and participating character. All jobs are the same job at this point.
                val job = pairs.first().first
                val participatingCharacters = pairs.map { it.second }
                job to participatingCharacters
            }.toMap()
        val jobIdsToParticipatingCharacters = jobsToParticipatingCharacters.mapKeys { it.key.id }

        val jobsToProjectManager = corporationIdToProjectManager.values
            .map { projectManager ->
                async {
                    semaphore.withPermit {
                        val after = corporationAfterCursors[projectManager.info!!.corporationId]
                        projectManager to fetchCursorPaginated(after) { before, after ->
                            esiApi.getCorporationsIdFreelanceJobs(
                                Originator.FreelanceJobs,
                                projectManager.characterId,
                                projectManager.info.corporationId,
                                before,
                                after,
                            )
                        }
                    }
                }
            }.awaitAll().flatMap { (projectManager, corporationJobs) ->
                corporationJobs.map { (corporationJobs, newAfter) ->
                    if (newAfter != null) {
                        newCorporationAfter += projectManager.info!!.corporationId to newAfter
                    }
                    corporationJobs.map { it to projectManager }
                }.success ?: emptyList()
            }.toMap()
        val jobIdToProjectManager = jobsToProjectManager.mapKeys { it.key.id }
        jobIdToCorporationId += jobsToProjectManager.map { it.key.id to it.value.info!!.corporationId }.toMap()

        val publicJobsResult = fetchCursorPaginated(publicAfterCursor) { before, after ->
            esiApi.getFreelanceJobs(Originator.FreelanceJobs, allCharacters.first().characterId, before, after)
        }.map { (jobs, newAfter) ->
            if (newAfter != null) newPublicAfter = newAfter
            jobs
        }
        val publicJobs = publicJobsResult.success ?: emptyList()
        val publicJobIds = publicJobs.map { it.id }

        val jobs = (jobsToParticipatingCharacters.keys + jobsToProjectManager.keys + publicJobs).distinctBy { it.id }
        deletedJobs = jobs.filter { it.state == OpportunityState.Deleted }.map { it.id }.toSet()
        jobs
            .filter { it.state != OpportunityState.Deleted }
            .map { job ->
                async {
                    val participatingCharacters = jobIdsToParticipatingCharacters[job.id] ?: emptyList()
                    val projectManagerWeGotThisJobFrom = jobIdToProjectManager[job.id]
                    val projectManager = projectManagerWeGotThisJobFrom ?: jobIdToCorporationId[job.id]?.let { corporationId -> corporationIdToProjectManager[corporationId] }
                    val characters = if (job.id in publicJobIds) allCharacters else participatingCharacters + listOfNotNull(projectManagerWeGotThisJobFrom)
                    semaphore.withPermit {
                        getJob(
                            characters = characters,
                            participants = participatingCharacters,
                            projectManager = projectManager,
                            job = job,
                        ).also {
                            mutex.withLock {
                                opportunities += it
                            }
                            send()
                        }
                    }
                }
            }
            .awaitAll()

        if (opportunities.all { it is Success }) {
            publicAfterCursor = newPublicAfter
            corporationAfterCursors = newCorporationAfter.toMap()
        } else {
            logger.error { "There were errors loading jobs, resetting cursors" }
            publicAfterCursor = null
            corporationAfterCursors = emptyMap()
        }

        send()
    }

    /**
     * @param characters - characters eligible for the job
     */
    private suspend fun CoroutineScope.getJob(
        characters: List<LocalCharacter>,
        participants: List<LocalCharacter>,
        projectManager: LocalCharacter?,
        job: FreelanceJob,
    ): Result<Opportunity> {
        val characterIds = characters.map { it.characterId }
        val cacheBuster = job.lastModified.epochSecond.toString()
        val detailsDeferred = async {
            esiApi.getFreelanceJobsId(Originator.FreelanceJobs, characterIds.first(), job.id, cacheBuster)
        }
        val contributionsDeferred = participants.map { character ->
            async {
                val participationResult = esiApi.getCharactersIdFreelanceJobsIdParticipation(Originator.FreelanceJobs, character.characterId, job.id, cacheBuster)
                Contribution(
                    characterId = character.characterId,
                    characterName = character.info?.name ?: "?",
                    contribution = participationResult.map { it.contributed },
                    participationState = participationResult.map { it.state },
                )
            }
        }
        val contributorsDeferred = async {
            if (projectManager == null) return@async Contributors.NoAccess
            fetchCursorPaginated(null) { before, after ->
                esiApi.getCorporationsIdFreelanceJobsIdParticipants(
                    originator = Originator.FreelanceJobs,
                    characterId = projectManager.characterId,
                    corporationId = projectManager.info!!.corporationId,
                    jobId = job.id,
                    before = before,
                    after = after,
                    cacheBuster = cacheBuster,
                )
            }.map { (contributors, _) ->
                val list = contributors.mapAsync {
                    Contributor(
                        characterId = it.id.toInt(),
                        details = characterDetailsRepository.getCharacterDetails(Originator.FreelanceJobs, it.id.toInt()),
                        contributed = it.contributed,
                        participationState = it.state,
                    )
                }
                if (list.isNotEmpty()) Contributors.Available(list) else Contributors.Empty
            }.let {
                when (it) {
                    is Failure -> Contributors.Error(it.cause?.message ?: "Unknown error")
                    is Success -> it.data
                }
            }
        }
        val details = when (val details = detailsDeferred.await()) {
            is Success -> details.data
            is Failure -> return details
        }
        val configuration = mapper.toModel(details.configuration)
        val contributionAttributesDeferred = async {
            getProjectContributionAttributesUseCase(Originator.FreelanceJobs, configuration, characterIds.first())
        }
        val matchingFilters = getMatchingFilters(
            baseType = OpportunityCategoryFilter.FreelanceJobs,
            career = details.details.career,
            configuration = configuration,
        )
        val contributionAttributes = contributionAttributesDeferred.await()
        return mapper.toModel(
            debugDetails = details.toString(),
            job = job,
            details = details,
            configuration = configuration,
            contributionAttributes = contributionAttributes,
            solarSystemChipState = getSolarSystemChipState(getSolarSystemChipStateUseCase, contributionAttributes, details.accessAndVisibility.broadcastLocations),
            matchingFilters = matchingFilters,
            contributions = contributionsDeferred.awaitAll(),
            contributors = contributorsDeferred.await(),
            eligibleCharacters = characters,
        ).let { Success(it) }
    }
}
