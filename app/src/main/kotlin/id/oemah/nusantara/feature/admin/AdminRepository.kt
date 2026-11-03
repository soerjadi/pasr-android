package id.oemah.nusantara.feature.admin

import androidx.paging.PagingData
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.oemah.nusantara.api.AdminApi
import id.oemah.nusantara.api.model.AdCampaign
import id.oemah.nusantara.api.model.AdCampaignInput
import id.oemah.nusantara.api.model.AdCampaignPage
import id.oemah.nusantara.api.model.AdminOrder
import id.oemah.nusantara.api.model.AdminOrderPage
import id.oemah.nusantara.api.model.AdminProduct
import id.oemah.nusantara.api.model.AdminProductPatch
import id.oemah.nusantara.api.model.AdminStore
import id.oemah.nusantara.api.model.AdminStorePage
import id.oemah.nusantara.api.model.Category
import id.oemah.nusantara.api.model.CategoryInput
import id.oemah.nusantara.api.model.OrderStatus
import id.oemah.nusantara.api.model.ReadyRequest
import id.oemah.nusantara.api.model.RejectRequest
import id.oemah.nusantara.api.model.StoreOrder
import id.oemah.nusantara.api.model.StoreStatus
import id.oemah.nusantara.core.network.apiCall
import id.oemah.nusantara.core.paging.CursorPage
import id.oemah.nusantara.core.paging.cursorPager
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

interface AdminRepository {
    // Stores
    fun stores(status: StoreStatus? = null): Flow<PagingData<AdminStore>>
    suspend fun listStores(status: StoreStatus? = null, cursor: String? = null, limit: Int? = 20): Result<AdminStorePage>
    suspend fun verifyStore(id: Long): Result<AdminStore>
    suspend fun suspendStore(id: Long): Result<AdminStore>
    suspend fun reinstateStore(id: Long): Result<AdminStore>

    // Products
    suspend fun moderateProduct(id: Long, patch: AdminProductPatch): Result<AdminProduct>

    // Categories
    suspend fun createCategory(input: CategoryInput): Result<Category>
    suspend fun updateCategory(id: Long, input: CategoryInput): Result<Category>
    suspend fun deleteCategory(id: Long): Result<Unit>

    // Ad campaigns
    fun adCampaigns(): Flow<PagingData<AdCampaign>>
    suspend fun listAdCampaigns(cursor: String? = null, limit: Int? = 20): Result<AdCampaignPage>
    suspend fun createAdCampaign(input: AdCampaignInput): Result<AdCampaign>
    suspend fun updateAdCampaign(id: Long, input: AdCampaignInput): Result<AdCampaign>
    suspend fun deleteAdCampaign(id: Long): Result<Unit>

    // Orders
    fun orders(status: OrderStatus? = null): Flow<PagingData<AdminOrder>>
    suspend fun listOrders(status: OrderStatus? = null, cursor: String? = null, limit: Int? = 20): Result<AdminOrderPage>
    suspend fun acceptSellerOrder(id: Long): Result<StoreOrder>
    suspend fun rejectSellerOrder(id: Long, reason: RejectRequest.Reason, note: String? = null): Result<StoreOrder>
    suspend fun readySellerOrder(id: Long, note: String? = null): Result<StoreOrder>
    suspend fun completeSellerOrder(id: Long): Result<StoreOrder>
    suspend fun cancelSellerOrder(id: Long, reason: RejectRequest.Reason, note: String? = null): Result<StoreOrder>

    // Reviews
    suspend fun deleteReview(id: Long): Result<Unit>
}

class DefaultAdminRepository @Inject constructor(private val api: AdminApi) : AdminRepository {
    override fun stores(status: StoreStatus?): Flow<PagingData<AdminStore>> =
        cursorPager { cursor, limit ->
            apiCall { api.listAdminStores(status = status, cursor = cursor, limit = limit) }
                .map { CursorPage(it.items, it.nextCursor) }
        }

    override suspend fun listStores(
        status: StoreStatus?,
        cursor: String?,
        limit: Int?,
    ): Result<AdminStorePage> =
        apiCall { api.listAdminStores(status = status, cursor = cursor, limit = limit) }

    override suspend fun verifyStore(id: Long): Result<AdminStore> =
        apiCall { api.verifyAdminStore(id) }

    override suspend fun suspendStore(id: Long): Result<AdminStore> =
        apiCall { api.suspendAdminStore(id) }

    override suspend fun reinstateStore(id: Long): Result<AdminStore> =
        apiCall { api.reinstateAdminStore(id) }

    override suspend fun moderateProduct(id: Long, patch: AdminProductPatch): Result<AdminProduct> =
        apiCall { api.updateAdminProduct(id, patch) }

    override suspend fun createCategory(input: CategoryInput): Result<Category> =
        apiCall { api.createAdminCategory(input) }

    override suspend fun updateCategory(id: Long, input: CategoryInput): Result<Category> =
        apiCall { api.updateAdminCategory(id, input) }

    override suspend fun deleteCategory(id: Long): Result<Unit> =
        apiCall { api.deleteAdminCategory(id) }

    override fun adCampaigns(): Flow<PagingData<AdCampaign>> =
        cursorPager { cursor, limit ->
            apiCall { api.listAdCampaigns(cursor = cursor, limit = limit) }
                .map { CursorPage(it.items, it.nextCursor) }
        }

    override suspend fun listAdCampaigns(
        cursor: String?,
        limit: Int?,
    ): Result<AdCampaignPage> =
        apiCall { api.listAdCampaigns(cursor = cursor, limit = limit) }

    override suspend fun createAdCampaign(input: AdCampaignInput): Result<AdCampaign> =
        apiCall { api.createAdCampaign(input) }

    override suspend fun updateAdCampaign(id: Long, input: AdCampaignInput): Result<AdCampaign> =
        apiCall { api.updateAdCampaign(id, input) }

    override suspend fun deleteAdCampaign(id: Long): Result<Unit> =
        apiCall { api.deleteAdCampaign(id) }

    override fun orders(status: OrderStatus?): Flow<PagingData<AdminOrder>> =
        cursorPager { cursor, limit ->
            apiCall { api.listAdminOrders(status = status, cursor = cursor, limit = limit) }
                .map { CursorPage(it.items, it.nextCursor) }
        }

    override suspend fun listOrders(
        status: OrderStatus?,
        cursor: String?,
        limit: Int?,
    ): Result<AdminOrderPage> =
        apiCall { api.listAdminOrders(status = status, cursor = cursor, limit = limit) }

    override suspend fun acceptSellerOrder(id: Long): Result<StoreOrder> =
        apiCall { api.acceptAdminSellerOrder(id) }

    override suspend fun rejectSellerOrder(
        id: Long,
        reason: RejectRequest.Reason,
        note: String?,
    ): Result<StoreOrder> =
        apiCall { api.rejectAdminSellerOrder(id, RejectRequest(reason, note)) }

    override suspend fun readySellerOrder(id: Long, note: String?): Result<StoreOrder> =
        apiCall { api.readyAdminSellerOrder(id, note?.let { ReadyRequest(it) }) }

    override suspend fun completeSellerOrder(id: Long): Result<StoreOrder> =
        apiCall { api.completeAdminSellerOrder(id) }

    override suspend fun cancelSellerOrder(
        id: Long,
        reason: RejectRequest.Reason,
        note: String?,
    ): Result<StoreOrder> =
        apiCall { api.cancelAdminSellerOrder(id, RejectRequest(reason, note)) }

    override suspend fun deleteReview(id: Long): Result<Unit> =
        apiCall { api.deleteAdminReview(id) }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AdminModule {
    @Binds abstract fun adminRepository(impl: DefaultAdminRepository): AdminRepository
}
