package id.oemah.nusantara.feature.review

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import id.oemah.nusantara.core.AppError
import id.oemah.nusantara.feature.social.SocialRepository
import id.oemah.nusantara.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
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
class WriteReviewViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val social = mockk<SocialRepository>()

    private fun viewModel() = WriteReviewViewModel(
        SavedStateHandle(mapOf("orderItemId" to 8L, "productName" to "Keripik Tempe")), social,
    )

    @Test fun the_product_name_comes_from_the_route() {
        assertEquals("Keripik Tempe", viewModel().productName)
    }

    @Test fun submitting_is_only_possible_after_choosing_a_rating() = runTest {
        val vm = viewModel()
        assertFalse(vm.canSubmit.value)

        vm.setRating(4)

        assertTrue(vm.canSubmit.value)
    }

    @Test fun ratings_outside_one_to_five_are_ignored() = runTest {
        val vm = viewModel()

        vm.setRating(0); vm.setRating(6); vm.setRating(-2)

        assertEquals(0, vm.rating.value)
        assertFalse(vm.canSubmit.value)
    }

    @Test fun the_text_is_capped_at_500_characters() = runTest {
        val vm = viewModel()

        vm.setText("a".repeat(900))

        assertEquals(500, vm.text.value.length)
    }

    @Test fun the_text_is_trimmed_when_sent() = runTest {
        coEvery { social.submitReview(8, 5, "Enak sekali") } returns Result.success(Unit)
        val vm = viewModel(); vm.setRating(5); vm.setText("   Enak sekali  ")

        vm.submit(); runCurrent()

        coVerify(exactly = 1) { social.submitReview(8, 5, "Enak sekali") }
    }

    @Test fun a_blank_text_is_sent_as_no_text() = runTest {
        coEvery { social.submitReview(8, 3, null) } returns Result.success(Unit)
        val vm = viewModel(); vm.setRating(3); vm.setText("    ")

        vm.submit(); runCurrent()

        coVerify(exactly = 1) { social.submitReview(8, 3, null) }
    }

    @Test fun success_closes_with_a_thank_you() = runTest {
        coEvery { social.submitReview(any(), any(), any()) } returns Result.success(Unit)
        val vm = viewModel(); vm.setRating(5)

        vm.events.test {
            vm.submit(); runCurrent()
            assertEquals(ReviewEvent.Done("Terima kasih atas ulasan Anda."), awaitItem())
        }
    }

    @Test fun reviewing_twice_says_so_and_closes() = runTest {
        coEvery { social.submitReview(any(), any(), any()) } returns Result.failure(AppError.Conflict("ALREADY_REVIEWED", "x"))
        val vm = viewModel(); vm.setRating(5)

        vm.events.test {
            vm.submit(); runCurrent()
            assertEquals(ReviewEvent.Done("Ulasan sudah dikirim."), awaitItem())
        }
    }

    @Test fun another_failure_shows_an_error_and_allows_trying_again() = runTest {
        coEvery { social.submitReview(any(), any(), any()) } returns Result.failure(AppError.Offline)
        val vm = viewModel(); vm.setRating(5)

        vm.submit(); runCurrent()

        assertEquals("Tidak ada koneksi internet. Periksa sambungan Anda lalu coba lagi.", vm.error.value)
        assertTrue(vm.canSubmit.value)

        vm.setRating(4)
        assertNull(vm.error.value)
    }

    @Test fun a_second_tap_while_sending_is_ignored() = runTest {
        val gate = CompletableDeferred<Unit>()
        coEvery { social.submitReview(any(), any(), any()) } coAnswers { gate.await(); Result.success(Unit) }
        val vm = viewModel(); vm.setRating(5)

        vm.submit(); vm.submit(); runCurrent()
        assertFalse(vm.canSubmit.value)
        gate.complete(Unit); runCurrent()

        coVerify(exactly = 1) { social.submitReview(any(), any(), any()) }
    }
}
