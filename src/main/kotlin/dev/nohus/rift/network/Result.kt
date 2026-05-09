package dev.nohus.rift.network

import kotlinx.coroutines.Deferred

sealed class Result<out T : Any?> {
    data class Success<out T : Any?>(val data: T) : Result<T>()
    data class Failure(val cause: Exception? = null) : Result<Nothing>()

    val success get() = (this as? Success<T>)?.data
    val failure get() = (this as? Failure)?.cause
    val isSuccess get() = this is Success
    val isFailure get() = this is Failure
    val successOrThrow get() = when (this) {
        is Success -> this.data
        is Failure -> throw this.cause ?: Exception("Unknown failure")
    }

    inline fun <R : Any?> map(transform: (T) -> R): Result<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Failure -> Failure(cause)
        }
    }

    inline fun mapFailure(transform: (Exception?) -> Exception?): Result<T> {
        return when (this) {
            is Success -> this
            is Failure -> Failure(transform(this.cause))
        }
    }

    inline fun <R : Any?> mapResult(transform: (T) -> Result<R>): Result<R> {
        return when (this) {
            is Success -> transform(data)
            is Failure -> Failure(cause)
        }
    }

    inline fun onFailure(action: (Exception?) -> Unit): Result<T> = apply {
        if (this is Failure) action(cause)
    }

    inline fun onSuccess(action: (T) -> Unit): Result<T> = apply {
        if (this is Success) action(data)
    }
}

inline fun <T1, T2, R> combine(
    result1: Result<T1>,
    result2: Result<T2>,
    transform: (T1, T2) -> R,
): Result<R> {
    return result1.mapResult { v1 ->
        result2.map { v2 -> transform(v1, v2) }
    }
}

inline fun <T1, T2, T3, R> combine(
    result1: Result<T1>,
    result2: Result<T2>,
    result3: Result<T3>,
    transform: (T1, T2, T3) -> R,
): Result<R> {
    return result1.mapResult { v1 ->
        result2.mapResult { v2 ->
            result3.map { v3 -> transform(v1, v2, v3) }
        }
    }
}

suspend inline fun <T1, T2, R> combine(
    result1: Deferred<Result<T1>>,
    result2: Deferred<Result<T2>>,
    transform: (T1, T2) -> R,
): Result<R> {
    return combine(result1.await(), result2.await(), transform)
}

suspend inline fun <T1, T2, T3, R> combine(
    result1: Deferred<Result<T1>>,
    result2: Deferred<Result<T2>>,
    result3: Deferred<Result<T3>>,
    transform: (T1, T2, T3) -> R,
): Result<R> {
    return combine(result1.await(), result2.await(), result3.await(), transform)
}
