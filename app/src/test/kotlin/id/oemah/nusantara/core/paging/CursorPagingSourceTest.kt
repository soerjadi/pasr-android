package id.oemah.nusantara.core.paging

import androidx.paging.PagingSource.LoadParams
import androidx.paging.PagingSource.LoadResult
import id.oemah.nusantara.core.AppError
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CursorPagingSourceTest {
    @Test fun a_page_uses_the_servers_next_cursor_as_its_next_key() = runBlocking {
        val source = CursorPagingSource<String> { cursor, _ ->
            Result.success(CursorPage(items = listOf("a", "b"), nextCursor = if (cursor == null) "c2" else null))
        }

        val first = source.load(LoadParams.Refresh(null, 20, false)) as LoadResult.Page
        val second = source.load(LoadParams.Append("c2", 20, false)) as LoadResult.Page

        assertEquals(listOf("a", "b"), first.data)
        assertEquals("c2", first.nextKey)
        assertNull(first.prevKey)
        assertNull(second.nextKey)
    }

    @Test fun the_requested_load_size_is_passed_through_as_the_limit() = runBlocking {
        var seenLimit = 0
        val source = CursorPagingSource<String> { _, limit -> seenLimit = limit; Result.success(CursorPage(emptyList(), null)) }

        source.load(LoadParams.Refresh(null, 20, false))

        assertEquals(20, seenLimit)
    }

    @Test fun a_failure_becomes_a_load_error_carrying_the_app_error() = runBlocking {
        val source = CursorPagingSource<String> { _, _ -> Result.failure(AppError.Offline) }

        val result = source.load(LoadParams.Refresh(null, 20, false))

        assertEquals(AppError.Offline, (result as LoadResult.Error).throwable)
    }
}
