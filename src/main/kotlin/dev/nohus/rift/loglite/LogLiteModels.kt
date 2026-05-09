package dev.nohus.rift.loglite

import java.time.Instant
import java.util.UUID

data class LogMessage(
    val client: Client,
    val timestamp: Instant,
    val severity: LogSeverity,
    val module: String,
    val channel: String,
    val message: String,
)

data class Client(
    val uuid: UUID = UUID.randomUUID(),
    val pid: Long,
    val machine: String,
    val executable: String,
)

enum class LogSeverity {
    Info,
    Notice,
    Warn,
    Error,
    Count,
    Unknown,
}
