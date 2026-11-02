package id.oemah.nusantara.feature.store

import androidx.paging.PagingData
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.oemah.nusantara.api.MeApi
import id.oemah.nusantara.api.model.CreateProductRequest
import id.oemah.nusantara.api.model.CreateStoreRequest
import id.oemah.nusantara.api.model.ImageUpload
import id.oemah.nusantara.api.model.MyStore
import id.oemah.nusantara.api.model.ReadyRequest
import id.oemah.nusantara.api.model.RejectRequest
import id.oemah.nusantara.api.model.SellerProduct
import id.oemah.nusantara.api.model.SellerProductPage
import id.oemah.nusantara.api.model.SetOpenRequest
import id.oemah.nusantara.api.model.StoreOrder
import id.oemah.nusantara.api.model.StoreOrderPage
import id.oemah.nusantara.api.model.StoreStats
import id.oemah.nusantara.api.model.StoreSummary
import id.oemah.nusantara.api.model.StoreSummaryResponse
import id.oemah.nusantara.api.model.UpdateProductRequest
import id.oemah.nusantara.api.model.UpdateStoreRequest
import id.oemah.nusantara.core.network.apiCall
import id.oemah.nusantara.core.paging.CursorPage
import id.oemah.nusantara.core.paging.cursorPager
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

interface SellerRepository {
    // Store creation & profile
    suspend fun createStore(request: CreateStoreRequest): Result<StoreSummary>
    suspend fun getMyStore(): Result<MyStore>
    suspend fun updateMyStore(request: UpdateStoreRequest): Result<MyStore>
    suspend fun setMyStoreOpen(open: Boolean): Result<Unit>

    // Media
    suspend fun uploadImage(part: MultipartBody.Part): Result<ImageUpload>
    suspend fun uploadImage(bytes: ByteArray, filename: String = "image.jpg", mimeType: String = "image/jpeg"): Result<ImageUpload>

    // Product management
    suspend fun createProduct(request: CreateProductRequest): Result<SellerProduct>
    fun myProducts(status: MeApi.StatusListMyProducts? = null): Flow<PagingData<SellerProduct>>
    suspend fun listMyProducts(status: MeApi.StatusListMyProducts? = null, cursor: String? = null, limit: Int? = 20): Result<SellerProductPage>
    suspend fun getMyProduct(id: Long): Result<SellerProduct>
    suspend fun updateMyProduct(id: Long, request: UpdateProductRequest): Result<SellerProduct>
    suspend fun deleteMyProduct(id: Long): Result<Unit>

    // Dashboard & Stats
    suspend fun getMyStoreSummary(): Result<StoreSummaryResponse>
    suspend fun getMyStoreStats(range: MeApi.RangeGetMyStoreStats = MeApi.RangeGetMyStoreStats.TODAY): Result<StoreStats>

    // Incoming store orders
    fun myStoreOrders(tab: MeApi.TabListMyStoreOrders = MeApi.TabListMyStoreOrders.NEW): Flow<PagingData<StoreOrder>>
    suspend fun listMyStoreOrders(tab: MeApi.TabListMyStoreOrders = MeApi.TabListMyStoreOrders.NEW, cursor: String? = null, limit: Int? = 20): Result<StoreOrderPage>
    suspend fun getMyStoreOrder(id: Long): Result<StoreOrder>
    suspend fun acceptMyStoreOrder(id: Long): Result<StoreOrder>
    suspend fun rejectMyStoreOrder(id: Long, reason: RejectRequest.Reason, note: String? = null): Result<StoreOrder>
    suspend fun readyMyStoreOrder(id: Long, note: String? = null): Result<StoreOrder>
    suspend fun completeMyStoreOrder(id: Long): Result<StoreOrder>
}

class DefaultSellerRepository @Inject constructor(private val api: MeApi) : SellerRepository {
    override suspend fun createStore(request: CreateStoreRequest): Result<StoreSummary> =
        apiCall { api.createMyStore(request) }

    override suspend fun getMyStore(): Result<MyStore> =
        apiCall { api.getMyStore() }

    override suspend fun updateMyStore(request: UpdateStoreRequest): Result<MyStore> =
        apiCall { api.updateMyStore(request) }

    override suspend fun setMyStoreOpen(open: Boolean): Result<Unit> =
        apiCall { api.setMyStoreOpen(SetOpenRequest(open)) }

    override suspend fun uploadImage(part: MultipartBody.Part): Result<ImageUpload> =
        apiCall { api.uploadImage(part) }

    override suspend fun uploadImage(bytes: ByteArray, filename: String, mimeType: String): Result<ImageUpload> {
        val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", filename, requestBody)
        return uploadImage(part)
    }

    override suspend fun createProduct(request: CreateProductRequest): Result<SellerProduct> =
        apiCall { api.createMyProduct(request) }

    override fun myProducts(status: MeApi.StatusListMyProducts?): Flow<PagingData<SellerProduct>> =
        cursorPager { cursor, limit ->
            apiCall { api.listMyProducts(status = status, cursor = cursor, limit = limit) }
                .map { CursorPage(it.items, it.nextCursor) }
        }

    override suspend fun listMyProducts(
        status: MeApi.StatusListMyProducts?,
        cursor: String?,
        limit: Int?,
    ): Result<SellerProductPage> =
        apiCall { api.listMyProducts(status = status, cursor = cursor, limit = limit) }

    override suspend fun getMyProduct(id: Long): Result<SellerProduct> =
        apiCall { api.getMyProduct(id) }

    override suspend fun updateMyProduct(id: Long, request: UpdateProductRequest): Result<SellerProduct> =
        apiCall { api.updateMyProduct(id, request) }

    override suspend fun deleteMyProduct(id: Long): Result<Unit> =
        apiCall { api.deleteMyProduct(id) }

    override suspend fun getMyStoreSummary(): Result<StoreSummaryResponse> =
        apiCall { api.getMyStoreSummary() }

    override suspend fun getMyStoreStats(range: MeApi.RangeGetMyStoreStats): Result<StoreStats> =
        apiCall { api.getMyStoreStats(range) }

    override fun myStoreOrders(tab: MeApi.TabListMyStoreOrders): Flow<PagingData<StoreOrder>> =
        cursorPager { cursor, limit ->
            apiCall { api.listMyStoreOrders(tab = tab, cursor = cursor, limit = limit) }
                .map { CursorPage(it.items, it.nextCursor) }
        }

    override suspend fun listMyStoreOrders(
        tab: MeApi.TabListMyStoreOrders,
        cursor: String?,
        limit: Int?,
    ): Result<StoreOrderPage> =
        apiCall { api.listMyStoreOrders(tab = tab, cursor = cursor, limit = limit) }

    override suspend fun getMyStoreOrder(id: Long): Result<StoreOrder> =
        apiCall { api.getMyStoreOrder(id) }

    override suspend fun acceptMyStoreOrder(id: Long): Result<StoreOrder> =
        apiCall { api.acceptMyStoreOrder(id) }

    override suspend fun rejectMyStoreOrder(id: Long, reason: RejectRequest.Reason, note: String?): Result<StoreOrder> =
        apiCall { api.rejectMyStoreOrder(id, RejectRequest(reason = reason, note = note)) }

    override suspend fun readyMyStoreOrder(id: Long, note: String?): Result<StoreOrder> =
        apiCall { api.readyMyStoreOrder(id, note?.let { ReadyRequest(it) }) }

    override suspend fun completeMyStoreOrder(id: Long): Result<StoreOrder> =
        apiCall { api.completeMyStoreOrder(id) }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SellerModule {
    @Binds abstract fun sellerRepository(impl: DefaultSellerRepository): SellerRepository
}

