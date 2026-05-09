package dev.nohus.rift.network.requests

import dev.nohus.rift.sso.scopes.EsiScope
import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Scope(val value: KClass<out EsiScope>)
