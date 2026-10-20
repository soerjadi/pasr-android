package id.oemah.nusantara.feature.orders

import androidx.paging.PagingData
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.oemah.nusantara.api.OrdersApi
import id.oemah.nusantara.api.model.Order
import id.oemah.nusantara.core.network.apiCall
import id.oemah.nusantara.core.paging.CursorPage
import id.oemah.nusantara.core.paging.cursorPager
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

enum class OrderTab(val apiValue: String, val label: String) {
    ALL("ALL", "Semua"),
    PROCESSING("PROCESSING", "Diproses"),
    READY("READY", "Siap Diambil"),
    DONE("DONE", "Selesai"),
}

interface OrdersRepository {
    suspend fun order(id: Long): Result<Order>
    fun orders(tab: OrderTab): Flow<PagingData<Order>>
}

class DefaultOrdersRepository @Inject constructor(private val api: OrdersApi) : OrdersRepository {
    override suspend fun order(id: Long): Result<Order> = apiCall { api.getOrder(id) }

    override fun orders(tab: OrderTab): Flow<PagingData<Order>> {
        val param = OrdersApi.TabListOrders.entries.first { it.value == tab.apiValue }
        return cursorPager { cursor, limit ->
            apiCall { api.listOrders(param, cursor, limit) }.map { CursorPage(it.items, it.nextCursor) }
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class OrdersModule {
    @Binds abstract fun ordersRepository(impl: DefaultOrdersRepository): OrdersRepository
}
