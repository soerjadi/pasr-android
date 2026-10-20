package id.oemah.nusantara.feature.ads

import id.oemah.nusantara.api.AdsApi
import id.oemah.nusantara.api.model.Ad
import id.oemah.nusantara.api.model.AdList
import id.oemah.nusantara.api.model.AdPlacementKey
import id.oemah.nusantara.api.model.ImageSet
import id.oemah.nusantara.core.AppError
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response

class AdsRepositoryTest {
    private val api = mockk<AdsApi>()
    private val repo = DefaultAdsRepository(api)
    private val ad = Ad(
        id = 1, placement = AdPlacementKey.WAIT_STATE, title = "T",
        image = ImageSet("s", "m", "l"), targetUrl = "https://a.id", label = "Iklan",
    )

    @Test fun each_placement_is_requested_with_its_api_key() = runBlocking {
        val requested = slot<AdsApi.PlacementListAds>()
        coEvery { api.listAds(capture(requested)) } returns Response.success(AdList(emptyList()))

        AdPlacement.entries.forEach { placement ->
            repo.ads(placement)
            assertEquals(placement.key, requested.captured.value)
        }
    }

    @Test fun returns_the_ads_in_server_order() = runBlocking {
        coEvery { api.listAds(any()) } returns Response.success(AdList(listOf(ad, ad.copy(id = 2))))

        assertEquals(listOf(1L, 2L), repo.ads(AdPlacement.WAIT_STATE).getOrThrow().map { it.id })
    }

    @Test fun adsOrEmpty_hides_every_failure() = runBlocking {
        coEvery { api.listAds(any()) } throws IOException("offline")
        assertEquals(AppError.Offline, repo.ads(AdPlacement.WAIT_STATE).exceptionOrNull())
        assertEquals(emptyList<Ad>(), repo.adsOrEmpty(AdPlacement.WAIT_STATE))

        coEvery { api.listAds(any()) } returns Response.error(500, okhttp3.ResponseBody.create(null, ""))
        assertEquals(emptyList<Ad>(), repo.adsOrEmpty(AdPlacement.WAIT_STATE))
    }
}
