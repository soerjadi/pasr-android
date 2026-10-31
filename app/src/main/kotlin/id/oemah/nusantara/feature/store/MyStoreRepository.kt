package id.oemah.nusantara.feature.store

import id.oemah.nusantara.api.model.CreateProductRequest
import id.oemah.nusantara.api.model.CreateStoreRequest
import id.oemah.nusantara.api.model.SellerProduct
import id.oemah.nusantara.api.model.StoreSummary
import id.oemah.nusantara.api.model.User
import id.oemah.nusantara.core.session.SessionStore
import id.oemah.nusantara.feature.profile.ProfileRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

/**
 * The signed-in user's own store, shared by every screen that surfaces it (Home widget, Account card, tab badge).
 * The summary arrives on `getMe`; it is kept per user id so a different account never sees the previous one's store.
 */
@Singleton
class MyStoreRepository @Inject constructor(private val profile: ProfileRepository, private val sessions: SessionStore) {
    private val latest = MutableStateFlow<Pair<Long, StoreSummary?>?>(null)

    /** Null when logged out, before the first load, or when the user has no store. */
    val store: Flow<StoreSummary?> = combine(latest, sessions.session) { latest, session ->
        latest?.takeIf { it.first == session?.user?.id }?.second
    }

    fun update(user: User) {
        latest.value = user.id to user.store
    }

    /** Opens the store and publishes it, so Home, Akun and the tab badge switch to the seller view at once. */
    suspend fun createStore(request: CreateStoreRequest): Result<StoreSummary> =
        profile.createStore(request).onSuccess { created -> sessions.current()?.let { latest.value = it.user.id to created } }

    suspend fun createProduct(request: CreateProductRequest): Result<SellerProduct> = profile.createProduct(request)

    /** Keeps the last known store when the call fails: a flaky connection must not hide the seller's widget. */
    suspend fun refresh() {
        if (sessions.current() == null) return
        profile.me().onSuccess(::update)
    }
}
