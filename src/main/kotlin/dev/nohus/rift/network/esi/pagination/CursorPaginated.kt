package dev.nohus.rift.network.esi.pagination

import dev.nohus.rift.network.Result
import dev.nohus.rift.network.Result.Failure
import dev.nohus.rift.network.Result.Success
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * [Pagination](https://developers.eveonline.com/blog/changing-pagination-turning-a-new-page)
 */
@Serializable
data class Cursor(
    @SerialName("before")
    val before: String? = null,

    @SerialName("after")
    val after: String? = null,
)

@Serializable
abstract class CursorPaginated<T> {
    @SerialName("cursor")
    val cursor: Cursor? = null

    abstract val items: List<T>
}

/**
 * Fetches cursor-paginated data.
 * If after is null, all data will be fetched.
 * If after is not null, only data after this cursor will be fetched.
 *
 * Returns the results and an after cursor for fetching future data.
 */
suspend fun <T> fetchCursorPaginated(
    after: String?,
    request: suspend (before: String?, after: String?) -> Result<CursorPaginated<T>>,
): Result<Pair<List<T>, String?>> {
    when (val initialResponse = request(null, after)) {
        is Failure -> return initialResponse
        is Success -> {
            return coroutineScope {
                val items = initialResponse.data.items.toMutableList()

                val beforeItemsDeferred = async {
                    if (after != null) {
                        // We are only going forward, so no need to fetch before items
                        return@async Success(emptyList())
                    }
                    val beforeItems = mutableListOf<T>()
                    var before = initialResponse.data.cursor?.before
                    while (before != null) {
                        when (val response = request(before, null)) {
                            is Failure -> return@async response
                            is Success -> {
                                beforeItems.addAll(0, response.data.items)
                                before = response.data.cursor?.before ?: break
                            }
                        }
                    }
                    Success(beforeItems.toList())
                }

                val afterItemsDeferred = async {
                    if (after == null) {
                        // We are only going back, so no need to fetch after items
                        return@async Success(emptyList<T>() to null)
                    }
                    val afterItems = mutableListOf<T>()
                    var after = initialResponse.data.cursor?.after
                    while (after != null) {
                        when (val response = request(null, after)) {
                            is Failure -> return@async response
                            is Success -> {
                                afterItems.addAll(response.data.items)
                                after = response.data.cursor?.after ?: break
                            }
                        }
                    }
                    Success(afterItems.toList() to after)
                }

                val beforeItems = when (val result = beforeItemsDeferred.await()) {
                    is Failure -> return@coroutineScope result
                    is Success -> result.data
                }
                val (afterItems, nextAfter) = when (val result = afterItemsDeferred.await()) {
                    is Failure -> return@coroutineScope result
                    is Success -> result.data
                }

                val newAfter = nextAfter ?: initialResponse.data.cursor?.after ?: after

                Success((beforeItems + items + afterItems) to newAfter)
            }
        }
    }
}
