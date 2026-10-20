package id.oemah.nusantara.feature.social

import androidx.paging.PagingData
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.oemah.nusantara.api.SocialApi
import id.oemah.nusantara.api.model.ProductSummary
import id.oemah.nusantara.api.model.ReviewInput
import id.oemah.nusantara.core.network.apiCall
import id.oemah.nusantara.core.paging.CursorPage
import id.oemah.nusantara.core.paging.cursorPager
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Everything that needs a logged-in user: favorites, following and reviews. */
interface SocialRepository {
    fun favorites(): Flow<PagingData<ProductSummary>>
    suspend fun setFavorite(productId: Long, favorite: Boolean): Result<Unit>
    suspend fun setFollowing(sellerId: Long, following: Boolean): Result<Unit>
    suspend fun submitReview(orderItemId: Long, rating: Int, text: String?): Result<Unit>
}

class DefaultSocialRepository @Inject constructor(private val api: SocialApi) : SocialRepository {
    override fun favorites(): Flow<PagingData<ProductSummary>> = cursorPager { cursor, limit ->
        apiCall { api.listFavorites(cursor, limit) }.map { CursorPage(it.items, it.nextCursor) }
    }

    override suspend fun setFavorite(productId: Long, favorite: Boolean): Result<Unit> =
        apiCall { if (favorite) api.addFavorite(productId) else api.removeFavorite(productId) }

    override suspend fun setFollowing(sellerId: Long, following: Boolean): Result<Unit> =
        apiCall { if (following) api.followSeller(sellerId) else api.unfollowSeller(sellerId) }

    override suspend fun submitReview(orderItemId: Long, rating: Int, text: String?): Result<Unit> =
        apiCall { api.submitReview(orderItemId, ReviewInput(rating = rating, text = text)) }.map { }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SocialModule {
    @Binds abstract fun socialRepository(impl: DefaultSocialRepository): SocialRepository
}
