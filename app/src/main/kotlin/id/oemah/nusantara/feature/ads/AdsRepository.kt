package id.oemah.nusantara.feature.ads

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.oemah.nusantara.api.AdsApi
import id.oemah.nusantara.api.model.Ad
import id.oemah.nusantara.core.network.apiCall
import javax.inject.Inject

interface AdsRepository {
    suspend fun ads(placement: AdPlacement): Result<List<Ad>>
}

/** Ads are optional decoration: any failure becomes "no ads" and never an error state. */
suspend fun AdsRepository.adsOrEmpty(placement: AdPlacement): List<Ad> =
    ads(placement).getOrDefault(emptyList())

class DefaultAdsRepository @Inject constructor(private val api: AdsApi) : AdsRepository {
    override suspend fun ads(placement: AdPlacement): Result<List<Ad>> {
        val param = AdsApi.PlacementListAds.entries.first { it.value == placement.key }
        return apiCall { api.listAds(param) }.map { it.items }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AdsModule {
    @Binds abstract fun adsRepository(impl: DefaultAdsRepository): AdsRepository
}
