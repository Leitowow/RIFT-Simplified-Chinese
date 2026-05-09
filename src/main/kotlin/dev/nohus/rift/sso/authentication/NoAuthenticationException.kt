package dev.nohus.rift.sso.authentication

import dev.nohus.rift.sso.scopes.EsiScope

class NoAuthenticationException(val characterId: Int, val scope: EsiScope?) : Exception()
