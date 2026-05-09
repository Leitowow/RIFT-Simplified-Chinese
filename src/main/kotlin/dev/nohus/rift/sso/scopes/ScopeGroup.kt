package dev.nohus.rift.sso.scopes

data class ScopeGroup(
    val name: String,
    val reasons: List<String>,
    val scopes: List<EsiScope>,
    val isRequired: Boolean = false,
)
