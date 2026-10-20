package id.oemah.nusantara.feature.seller

import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import app.cash.turbine.test
import id.oemah.nusantara.api.model.SellerDetail
import id.oemah.nusantara.core.AppError
import id.oemah.nusantara.core.UiEvent
import id.oemah.nusantara.core.UiState
import id.oemah.nusantara.core.network.FakeSessionStore
import id.oemah.nusantara.core.network.session
import id.oemah.nusantara.feature.catalog.CatalogRepository
import id.oemah.nusantara.feature.catalog.ProductQuery
import id.oemah.nusantara.feature.products.detail.testSeller
import id.oemah.nusantara.feature.social.SocialRepository
import id.oemah.nusantara.testing.FakeCartRepository
import id.oemah.nusantara.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SellerViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val catalog = mockk<CatalogRepository>()
    private val social = mockk<SocialRepository>()

    private fun detail(following: Boolean? = false, whatsapp: String = "+6281234567890") = SellerDetail(
        seller = testSeller().copy(whatsapp = whatsapp), description = "Aneka makanan ringan.",
        followerCount = 340, isFollowing = following,
    )

    private fun viewModel(
        detail: Result<SellerDetail> = Result.success(detail()),
        loggedIn: Boolean = true,
    ): SellerViewModel {
        coEvery { catalog.seller(3) } returns detail
        every { catalog.products(any()) } returns flowOf(PagingData.empty())
        every { catalog.sellerReviews(any()) } returns flowOf(PagingData.empty())
        return SellerViewModel(
            SavedStateHandle(mapOf("sellerId" to 3L)), catalog, social, FakeCartRepository(),
            FakeSessionStore(if (loggedIn) session() else null),
        )
    }

    @Test fun loads_the_seller() = runTest {
        val vm = viewModel(); runCurrent()

        assertEquals("Bu Siti", (vm.state.value as UiState.Content).value.seller.name)
    }

    @Test fun a_load_failure_is_an_error_state() = runTest {
        val vm = viewModel(Result.failure(AppError.Server)); runCurrent()

        assertEquals(UiState.Error(AppError.Server), vm.state.value)
    }

    @Test fun the_storefront_lists_only_this_sellers_products_and_reviews() = runTest {
        val vm = viewModel()
        vm.products
        vm.reviews

        verify { catalog.products(ProductQuery(sellerId = 3)) }
        verify { catalog.sellerReviews(3) }
    }

    @Test fun following_while_logged_out_asks_for_login_and_calls_nothing() = runTest {
        val vm = viewModel(loggedIn = false); runCurrent()

        vm.events.test {
            vm.toggleFollow(); runCurrent()
            assertEquals(UiEvent.LoginRequired, awaitItem())
        }
        coVerify(exactly = 0) { social.setFollowing(any(), any()) }
        assertFalse(vm.following.value)
    }

    @Test fun following_flips_immediately_and_adjusts_the_follower_count() = runTest {
        coEvery { social.setFollowing(3, true) } returns Result.success(Unit)
        val vm = viewModel(); runCurrent()
        assertEquals(340, vm.followerCount.value)

        vm.toggleFollow()

        assertTrue(vm.following.value)
        assertEquals(341, vm.followerCount.value)
        runCurrent()
        coVerify(exactly = 1) { social.setFollowing(3, true) }
    }

    @Test fun a_failed_follow_rolls_back_the_state_and_the_count() = runTest {
        coEvery { social.setFollowing(3, true) } returns Result.failure(AppError.Offline)
        val vm = viewModel(); runCurrent()

        vm.events.test {
            vm.toggleFollow(); runCurrent()
            assertTrue(awaitItem() is UiEvent.Message)
        }
        assertFalse(vm.following.value)
        assertEquals(340, vm.followerCount.value)
    }

    @Test fun a_seller_the_user_already_follows_starts_followed_and_unfollowing_lowers_the_count() = runTest {
        coEvery { social.setFollowing(3, false) } returns Result.success(Unit)
        val vm = viewModel(Result.success(detail(following = true))); runCurrent()
        assertTrue(vm.following.value)
        assertEquals(340, vm.followerCount.value)

        vm.toggleFollow(); runCurrent()

        assertFalse(vm.following.value)
        assertEquals(339, vm.followerCount.value)
    }

    @Test fun whatsapp_link_is_built_from_the_sellers_number() = runTest {
        val vm = viewModel(); runCurrent()

        assertEquals("https://wa.me/6281234567890", vm.whatsappUrl.value)
    }

    @Test fun a_malformed_whatsapp_number_hides_the_chat_button() = runTest {
        val vm = viewModel(Result.success(detail(whatsapp = "12345"))); runCurrent()

        assertNull(vm.whatsappUrl.value)
    }
}
