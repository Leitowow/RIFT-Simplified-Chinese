package dev.nohus.rift.fleet

import dev.nohus.rift.network.esi.models.FleetMember
import dev.nohus.rift.network.esi.models.FleetsId

data class Fleet(
    val id: Long,
    val members: List<FleetMember>,
    val details: FleetsId,
)
