package id.oemah.nusantara.feature.checkout

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.oemah.nusantara.api.OrdersApi
import id.oemah.nusantara.api.model.CartLineInput
import id.oemah.nusantara.api.model.CartRequest
import id.oemah.nusantara.api.model.CartValidation
import id.oemah.nusantara.core.network.apiCall
import id.oemah.nusantara.feature.cart.CartLine
import javax.inject.Inject

interface CheckoutRepository {
    /** The server's view of the cart: current prices, stock, and each seller's pickup details. */
    suspend fun validate(lines: List<CartLine>): Result<CartValidation>
}

class DefaultCheckoutRepository @Inject constructor(private val api: OrdersApi) : CheckoutRepository {
    override suspend fun validate(lines: List<CartLine>): Result<CartValidation> =
        apiCall { api.validateCart(lines.toRequest()) }
}

internal fun List<CartLine>.toRequest() = CartRequest(map { CartLineInput(productId = it.productId, quantity = it.quantity) })

@Module
@InstallIn(SingletonComponent::class)
abstract class CheckoutModule {
    @Binds abstract fun checkoutRepository(impl: DefaultCheckoutRepository): CheckoutRepository
}
