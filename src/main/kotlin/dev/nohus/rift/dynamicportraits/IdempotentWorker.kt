package dev.nohus.rift.dynamicportraits

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * A worker which produces Output from Input allowing concurrent calls.
 * Concurrent calls with the same input will only work on the input once and return the same output.
 * Outputs are cached and later calls will return the cached output for the same input.
 */
abstract class IdempotentWorker<Input : Any, Output : Any> {

    private val scope = CoroutineScope(SupervisorJob())

    private sealed interface InputClassification<Output> {
        /**
         * This input is already being calculated by another coroutine.
         * Contains the deferred which will be completed with the output when it's ready.
         */
        data class Existing<Output>(val deferred: Deferred<Output?>) : InputClassification<Output>

        /**
         * This input is new and not being calculated by another coroutine.
         * Contains the completable deferred which should be completed with the output when it's ready.
         */
        data class New<Output>(val completableDeferred: CompletableDeferred<Output?>) : InputClassification<Output>
    }

    private val mutex = Mutex()
    private val deferredByInput = mutableMapOf<Input, Deferred<Output?>>()
    private val outputByInput = ConcurrentHashMap<Input, Output>()
    protected open val hasInternalCache: Boolean = true

    /**
     * Returns cached output for the input if it was calculated previously, otherwise null.
     * If null, starts a background job to calculate the output for inclusion in the cache.
     */
    fun getOutputOrNull(input: Input): Output? {
        if (!hasInternalCache) throw IllegalStateException("Internal cache is disabled")
        outputByInput[input]?.let { return it }
        scope.launch { calculateOutputFromInput(input, isWaitingForExisting = false) }
        return null
    }

    /**
     * Returns the output for the input, either from a cached value if it was calculated previously or by calculating it.
     */
    suspend fun getOutput(input: Input): Output? {
        if (hasInternalCache) {
            outputByInput[input]?.let { return it }
        }
        return calculateOutputFromInput(input)
    }

    /**
     * Returns the output from the input.
     * Can be called concurrently from multiple threads and will only do the work once.
     */
    private suspend fun calculateOutputFromInput(
        input: Input,
        isWaitingForExisting: Boolean = true,
    ): Output? {
        val inputClassification = mutex.withLock {
            val deferred = deferredByInput[input]
            if (deferred == null) {
                InputClassification.New(CompletableDeferred())
            } else {
                InputClassification.Existing(deferred)
            }
        }
        return when (inputClassification) {
            is InputClassification.Existing -> {
                if (isWaitingForExisting) inputClassification.deferred.await() else null
            }
            is InputClassification.New -> {
                try {
                    mutex.withLock {
                        deferredByInput[input] = inputClassification.completableDeferred
                    }
                    process(input).also {
                        if (it != null && hasInternalCache) outputByInput[input] = it
                        inputClassification.completableDeferred.complete(it)
                    }
                } finally {
                    mutex.withLock {
                        deferredByInput -= input
                    }
                }
            }
        }
    }

    /**
     * Calculates the output for the input.
     */
    protected abstract suspend fun process(input: Input): Output?
}
