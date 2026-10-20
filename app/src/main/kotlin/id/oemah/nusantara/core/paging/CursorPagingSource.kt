package id.oemah.nusantara.core.paging

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.flow.Flow

data class CursorPage<T>(val items: List<T>, val nextCursor: String?)

/** Pages through a server list that hands out an opaque `nextCursor` (no total count, no random access). */
class CursorPagingSource<T : Any>(
    private val loadPage: suspend (cursor: String?, limit: Int) -> Result<CursorPage<T>>,
) : PagingSource<String, T>() {
    override fun getRefreshKey(state: PagingState<String, T>): String? = null

    override suspend fun load(params: LoadParams<String>): LoadResult<String, T> =
        loadPage(params.key, params.loadSize).fold(
            onSuccess = { LoadResult.Page(data = it.items, prevKey = null, nextKey = it.nextCursor) },
            onFailure = { LoadResult.Error(it) },
        )
}

const val PAGE_SIZE = 20 // the API's default page size; its maximum is 50

fun <T : Any> cursorPager(
    loadPage: suspend (cursor: String?, limit: Int) -> Result<CursorPage<T>>,
): Flow<PagingData<T>> =
    Pager(PagingConfig(pageSize = PAGE_SIZE, initialLoadSize = PAGE_SIZE, enablePlaceholders = false)) {
        CursorPagingSource(loadPage)
    }.flow
