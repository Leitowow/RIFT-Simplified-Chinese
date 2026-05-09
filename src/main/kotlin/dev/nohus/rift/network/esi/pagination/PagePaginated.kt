package dev.nohus.rift.network.esi.pagination

import dev.nohus.rift.network.Result
import dev.nohus.rift.network.Result.Failure
import dev.nohus.rift.network.Result.Success
import dev.nohus.rift.network.requests.Reply
import kotlin.collections.plusAssign
import kotlin.text.toIntOrNull

suspend fun <T> fetchPagePaginated(
    onProgressUpdate: suspend (loadedItems: Int) -> Unit = { _ -> },
    request: suspend (page: Int) -> Result<Reply<List<T>>>,
): Result<List<T>> {
    val items = mutableListOf<T>()
    var page = 1
    while (true) {
        when (val result = request(page)) {
            is Success -> {
                items += result.data.body
                val pages = result.data.headers["x-pages"]?.toIntOrNull() ?: 1
                onProgressUpdate(items.size)
                if (page >= pages) break
                page++
            }
            is Failure -> return result
        }
    }
    return Success(items)
}
