package id.oemah.nusantara.feature.social

import id.oemah.nusantara.api.SocialApi
import id.oemah.nusantara.api.model.Review
import id.oemah.nusantara.api.model.ReviewInput
import id.oemah.nusantara.core.AppError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response

class SocialRepositoryTest {
    private val api = mockk<SocialApi>()
    private val repo = DefaultSocialRepository(api)

    @Test fun favoriting_adds_and_unfavoriting_removes() = runTest {
        coEvery { api.addFavorite(1) } returns Response.success(Unit)
        coEvery { api.removeFavorite(1) } returns Response.success(Unit)

        repo.setFavorite(1, true).getOrThrow()
        repo.setFavorite(1, false).getOrThrow()

        coVerify(exactly = 1) { api.addFavorite(1) }
        coVerify(exactly = 1) { api.removeFavorite(1) }
    }

    @Test fun following_follows_and_unfollowing_unfollows() = runTest {
        coEvery { api.followSeller(3) } returns Response.success(Unit)
        coEvery { api.unfollowSeller(3) } returns Response.success(Unit)

        repo.setFollowing(3, true).getOrThrow()
        repo.setFollowing(3, false).getOrThrow()

        coVerify(exactly = 1) { api.followSeller(3) }
        coVerify(exactly = 1) { api.unfollowSeller(3) }
    }

    @Test fun a_review_sends_the_rating_and_text_and_returns_unit() = runTest {
        coEvery { api.submitReview(8, ReviewInput(rating = 5, text = "Enak")) } returns
            Response.success(Review(id = 1, rating = 5, authorName = "A", createdAt = "2026-10-16T02:00:00Z"))

        assertEquals(Unit, repo.submitReview(8, 5, "Enak").getOrThrow())
    }

    @Test fun reviewing_twice_is_a_conflict() = runTest {
        coEvery { api.submitReview(any(), any()) } returns
            Response.error(409, """{"code":"ALREADY_REVIEWED","message":"sudah"}""".toResponseBody())

        assertEquals(AppError.Conflict("ALREADY_REVIEWED", "sudah"), repo.submitReview(8, 5, null).exceptionOrNull())
    }
}
