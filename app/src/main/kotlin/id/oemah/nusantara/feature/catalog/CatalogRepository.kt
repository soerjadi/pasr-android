package id.oemah.nusantara.feature.catalog

import androidx.paging.PagingData
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.oemah.nusantara.api.CatalogApi
import id.oemah.nusantara.api.model.Category
import id.oemah.nusantara.api.model.ProductDetail
import id.oemah.nusantara.api.model.ProductSummary
import id.oemah.nusantara.api.model.Review
import id.oemah.nusantara.api.model.SellerDetail
import id.oemah.nusantara.core.network.apiCall
import id.oemah.nusantara.core.paging.CursorPage
import id.oemah.nusantara.core.paging.cursorPager
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

data class ProductQuery(
    val categoryId: Long? = null,
    val query: String? = null,
    val featured: Boolean = false,
    val sellerId: Long? = null,
)

interface CatalogRepository {
    suspend fun categories(): Result<List<Category>>
    fun products(query: ProductQuery): Flow<PagingData<ProductSummary>>
    suspend fun product(id: Long): Result<ProductDetail>
    suspend fun seller(id: Long): Result<SellerDetail>
    fun sellerReviews(sellerId: Long): Flow<PagingData<Review>>
}

class DefaultCatalogRepository @Inject constructor(private val api: CatalogApi) : CatalogRepository {
    override suspend fun categories(): Result<List<Category>> = apiCall { api.listCategories() }.map { it.items }

    override fun products(query: ProductQuery): Flow<PagingData<ProductSummary>> = cursorPager { cursor, limit ->
        apiCall {
            api.listProducts(
                categoryId = query.categoryId,
                q = query.query?.trim()?.ifBlank { null },
                featured = query.featured.takeIf { it },
                sellerId = query.sellerId,
                cursor = cursor,
                limit = limit,
            )
        }.map { CursorPage(it.items, it.nextCursor) }
    }

    override suspend fun product(id: Long): Result<ProductDetail> = apiCall { api.getProduct(id) }

    override suspend fun seller(id: Long): Result<SellerDetail> = apiCall { api.getSeller(id) }

    override fun sellerReviews(sellerId: Long): Flow<PagingData<Review>> = cursorPager { cursor, limit ->
        apiCall { api.listSellerReviews(sellerId, cursor, limit) }.map { CursorPage(it.items, it.nextCursor) }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class CatalogModule {
    @Binds abstract fun catalogRepository(impl: DefaultCatalogRepository): CatalogRepository
}
