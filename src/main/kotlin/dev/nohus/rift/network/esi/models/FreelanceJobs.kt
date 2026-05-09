package dev.nohus.rift.network.esi.models

import dev.nohus.rift.network.IsoDateTimeSerializer
import dev.nohus.rift.network.esi.pagination.CursorPaginated
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
class FreelanceJobs(
    @SerialName("freelance_jobs")
    override val items: List<FreelanceJob>,
) : CursorPaginated<FreelanceJob>()

@Serializable
data class FreelanceJob(
    @SerialName("id")
    val id: String,

    @SerialName("last_modified")
    @Serializable(with = IsoDateTimeSerializer::class)
    val lastModified: Instant,

    @SerialName("name")
    val name: String,

    @SerialName("progress")
    val progress: FreelanceJobProgress,

    @SerialName("reward")
    val reward: FreelanceJobReward? = null,

    @SerialName("state")
    val state: OpportunityState,
)

@Serializable
data class FreelanceJobsId(
    @SerialName("access_and_visibility")
    val accessAndVisibility: AccessAndVisibility,

    @SerialName("configuration")
    val configuration: FreelanceJobConfiguration,

    @SerialName("contribution")
    val contributionSettings: FreelanceJobContributionSettings? = null,

    @SerialName("details")
    val details: FreelanceJobDetails,

    @SerialName("id")
    val id: String,

    @SerialName("last_modified")
    @Serializable(with = IsoDateTimeSerializer::class)
    val lastModified: Instant,

    @SerialName("name")
    val name: String,

    @SerialName("progress")
    val progress: FreelanceJobProgress,

    @SerialName("reward")
    val reward: FreelanceJobReward? = null,

    @SerialName("state")
    val state: OpportunityState,
)

@Serializable
data class AccessAndVisibility(
    @SerialName("acl_protected")
    val aclProtected: Boolean,

    @SerialName("broadcast_locations")
    val broadcastLocations: List<BroadcastLocation>? = null,

    @SerialName("restrictions")
    val restrictions: Restrictions? = null,
)

@Serializable
data class BroadcastLocation(
    @SerialName("id")
    val id: Long,

    @SerialName("name")
    val name: String,
)

@Serializable
data class Restrictions(
    @SerialName("minimum_age")
    val minimumAge: Long? = null,

    @SerialName("maximum_age")
    val maximumAge: Long? = null,
)

@Serializable
data class FreelanceJobContributionSettings(
    @SerialName("max_committed_participants")
    val maxCommitedParticipants: Long? = null,

    @SerialName("contribution_per_participant_limit")
    val participationLimit: Long? = null,

    @SerialName("reward_per_contribution")
    val rewardPerContribution: Double? = null,

    @SerialName("submission_limit")
    val submissionLimit: Long? = null,

    @SerialName("submission_multiplier")
    val submissionMultiplier: Double? = null,
)

@Serializable
data class FreelanceJobProgress(
    @SerialName("current")
    val current: Long,

    @SerialName("desired")
    val desired: Long,
)

@Serializable
data class FreelanceJobReward(
    @SerialName("initial")
    val initial: Double,

    @SerialName("remaining")
    val remaining: Double,
)

@Serializable
data class FreelanceJobConfiguration(
    @SerialName("method")
    val method: String,

    @SerialName("version")
    val version: Long,

    @SerialName("parameters")
    val parameters: Map<String, FreelanceJobParameter>,
)

@Serializable
data class FreelanceJobParameter(
    @SerialName("matcher")
    val matcher: MatcherParameter? = null,

    @SerialName("options")
    val options: OptionsParameter? = null,

    @SerialName("boolean")
    val boolean: BooleanParameter? = null,

    @SerialName("corporation_item_delivery")
    val corporationItemDelivery: CorporationItemDeliveryParameter? = null,
)

@Serializable
data class MatcherParameter(
    @SerialName("values")
    val values: List<MatcherValue>,
)

@Serializable
data class MatcherValue(
    @SerialName("value_type")
    val type: String,

    @SerialName("values")
    val values: List<String>,
)

@Serializable
data class OptionsParameter(
    @SerialName("selected")
    val selected: List<String>,
)

@Serializable
data class BooleanParameter(
    @SerialName("value")
    val value: Boolean,
)

@Serializable
data class CorporationItemDeliveryParameter(
    @SerialName("corporation_office_location")
    val corporationOfficeLocation: CorporationOfficeLocation,

    @SerialName("item_type")
    val itemType: ItemType,
)

@Serializable
data class CorporationOfficeLocation(
    @SerialName("values")
    val values: List<MatcherValue>,
)

@Serializable
data class ItemType(
    @SerialName("values")
    val values: List<MatcherValue>,
)

@Serializable
data class FreelanceJobDetails(
    @SerialName("career")
    val career: OpportunityCareer,

    @SerialName("created")
    @Serializable(with = IsoDateTimeSerializer::class)
    val created: Instant,

    @SerialName("creator")
    val creator: FreelanceJobCreator,

    @SerialName("description")
    val description: String,

    @SerialName("expires")
    @Serializable(with = IsoDateTimeSerializer::class)
    val expires: Instant? = null,

    @SerialName("finished")
    @Serializable(with = IsoDateTimeSerializer::class)
    val finished: Instant? = null,
)

@Serializable
data class FreelanceJobCreator(
    @SerialName("character")
    val character: FreelanceJobCreatorEntity,

    @SerialName("corporation")
    val corporation: FreelanceJobCreatorEntity,
)

@Serializable
data class FreelanceJobCreatorEntity(
    @SerialName("id")
    val id: Long,

    @SerialName("name")
    val name: String,
)

@Serializable
data class GetCharactersFreelanceJobsParticipation(
    @SerialName("contributed")
    val contributed: Long,

    @SerialName("last_modified")
    @Serializable(with = IsoDateTimeSerializer::class)
    val lastModified: Instant,

    @SerialName("state")
    val state: ParticipationState,
)

@Serializable
enum class ParticipationState {
    @SerialName("Unspecified")
    Unspecified,

    @SerialName("Committed")
    Commited,

    @SerialName("Kicked")
    Kicked,

    @SerialName("Resigned")
    Resigned,
}

@Serializable
class GetCorporationsFreelanceJobsParticipants(
    @SerialName("participants")
    override val items: List<FreelanceJobParticipant>,
) : CursorPaginated<FreelanceJobParticipant>()

@Serializable
data class FreelanceJobParticipant(
    @SerialName("contributed")
    val contributed: Long,

    @SerialName("id")
    val id: Long,

    @SerialName("name")
    val name: String,

    @SerialName("state")
    val state: ParticipationState,
)
