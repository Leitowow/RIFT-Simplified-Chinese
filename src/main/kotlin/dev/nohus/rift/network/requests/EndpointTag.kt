package dev.nohus.rift.network.requests

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class EndpointTag(val value: KClass<out Endpoint>)
