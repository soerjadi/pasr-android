package id.oemah.nusantara.feature.onboarding

import id.oemah.nusantara.testing.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.coEvery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class OnboardingViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val seen = MutableStateFlow(false)
    private val store = mockk<OnboardingStore>().also {
        every { it.seen } returns seen
        coEvery { it.markSeen() } answers { seen.value = true }
    }

    @Test fun it_is_unknown_until_the_store_has_answered_then_reports_not_seen() = runTest {
        val vm = OnboardingViewModel(store)
        assertNull(vm.seen.value)

        runCurrent()

        assertEquals(false, vm.seen.value)
    }

    @Test fun marking_seen_is_persisted_and_reported() = runTest {
        val vm = OnboardingViewModel(store); runCurrent()

        vm.markSeen(); runCurrent()

        coVerify(exactly = 1) { store.markSeen() }
        assertEquals(true, vm.seen.value)
    }

    @Test fun a_returning_user_is_reported_as_having_seen_it() = runTest {
        seen.value = true

        val vm = OnboardingViewModel(store); runCurrent()

        assertEquals(true, vm.seen.value)
    }
}
