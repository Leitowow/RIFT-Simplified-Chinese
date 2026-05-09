package dev.nohus.rift.loglite

sealed interface LogLiteAction {
    data class AccountId(val id: Int) : LogLiteAction
    data class CharacterId(val id: Int) : LogLiteAction
    data class AutopilotPath(val ids: List<Int>) : LogLiteAction
}

data class ClientLogLiteAction(
    val action: LogLiteAction,
    val client: Client,
)
