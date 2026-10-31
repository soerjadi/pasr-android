package id.oemah.nusantara.feature.profile

import id.oemah.nusantara.api.MeApi
import id.oemah.nusantara.api.model.CreateProductRequest
import id.oemah.nusantara.api.model.CreateStoreRequest
import id.oemah.nusantara.api.model.DeviceRequest
import id.oemah.nusantara.api.model.SellerProduct
import id.oemah.nusantara.api.model.StoreStatus
import id.oemah.nusantara.api.model.StoreSummary
import id.oemah.nusantara.api.model.UpdateMeRequest
import id.oemah.nusantara.api.model.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response

class ProfileRepositoryTest {
    private val api = mockk<MeApi>()
    private val repo = DefaultProfileRepository(api)
    private val user = User(id = 1, phone = "+6281234567890", name = "Budi")

    @Test fun me_returns_the_current_user() = runTest {
        coEvery { api.getMe() } returns Response.success(user)
        assertEquals("Budi", repo.me().getOrThrow().name)
    }

    @Test fun updating_the_name_sends_it_and_returns_the_user() = runTest {
        coEvery { api.updateMe(UpdateMeRequest("Budi")) } returns Response.success(user)
        assertEquals(user, repo.updateName("Budi").getOrThrow())
    }

    @Test fun registering_a_device_sends_the_push_token() = runTest {
        coEvery { api.registerDevice(DeviceRequest("tok")) } returns Response.success(Unit)

        repo.registerDevice("tok").getOrThrow()

        coVerify(exactly = 1) { api.registerDevice(DeviceRequest("tok")) }
    }

    @Test fun creating_a_store_posts_the_form() = runTest {
        val request = CreateStoreRequest("Warung Bu Sari", 3, "Sari", "Sambirejo", "RT 2")
        val created = StoreSummary(7, "Warung Bu Sari", StoreStatus.PENDING, 0)
        coEvery { api.createMyStore(request) } returns Response.success(created)

        assertEquals(created, repo.createStore(request).getOrThrow())
    }

    @Test fun creating_a_product_posts_the_form() = runTest {
        val request = CreateProductRequest("Nasi pecel", 15_000, 10)
        val created = SellerProduct(31, "Nasi pecel", 15_000, 10)
        coEvery { api.createMyProduct(request) } returns Response.success(created)

        assertEquals(created, repo.createProduct(request).getOrThrow())
    }
}
