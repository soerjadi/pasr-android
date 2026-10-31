package id.oemah.nusantara.feature.store

import id.oemah.nusantara.api.model.CreateProductRequest
import id.oemah.nusantara.api.model.CreateStoreRequest

/** Kelurahan the marketplace currently serves. */
val KELURAHAN = listOf("Sambirejo")

data class StoreFormState(
    val name: String = "",
    val categoryId: Long? = null,
    val ownerName: String = "",
    val kelurahan: String? = null,
    val address: String = "",
) {
    /** Null until every field is filled; the request is trimmed exactly as the server validates it. */
    fun toRequest(): CreateStoreRequest? {
        val name = name.trim()
        val owner = ownerName.trim()
        val address = address.trim()
        if (categoryId == null || kelurahan == null || name.isEmpty() || owner.isEmpty() || address.isEmpty()) return null
        return CreateStoreRequest(name = name, categoryId = categoryId, ownerName = owner, kelurahan = kelurahan, address = address)
    }

    companion object {
        const val MAX_NAME = 50
        const val MAX_ADDRESS = 200
    }
}

data class ProductFormState(
    val name: String = "",
    val price: String = "",
    val stock: String = "",
) {
    fun toRequest(): CreateProductRequest? {
        val name = name.trim()
        val price = price.toLongOrNull()?.takeIf { it in 1..MAX_PRICE }
        val stock = stock.toIntOrNull()?.takeIf { it in 0..MAX_STOCK }
        if (name.isEmpty() || price == null || stock == null) return null
        return CreateProductRequest(name = name, price = price, stock = stock)
    }

    companion object {
        const val MAX_NAME = 80
        const val MAX_PRICE = 100_000_000L
        const val MAX_STOCK = 9_999
    }
}
