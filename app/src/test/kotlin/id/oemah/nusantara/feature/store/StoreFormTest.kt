package id.oemah.nusantara.feature.store

import id.oemah.nusantara.api.model.CreateProductRequest
import id.oemah.nusantara.api.model.CreateStoreRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StoreFormTest {
    private val filled = StoreFormState(name = " Warung Bu Sari ", categoryId = 3, ownerName = "Sari", kelurahan = "Sambirejo", address = " RT 2 depan masjid ")

    @Test fun a_complete_store_form_is_trimmed_into_a_request() {
        assertEquals(CreateStoreRequest("Warung Bu Sari", 3, "Sari", "Sambirejo", "RT 2 depan masjid"), filled.toRequest())
    }

    @Test fun every_store_field_is_required() {
        assertNull(filled.copy(name = "  ").toRequest())
        assertNull(filled.copy(categoryId = null).toRequest())
        assertNull(filled.copy(ownerName = "").toRequest())
        assertNull(filled.copy(kelurahan = null).toRequest())
        assertNull(filled.copy(address = " ").toRequest())
    }

    @Test fun a_product_needs_a_name_a_positive_price_and_a_stock() {
        assertEquals(CreateProductRequest("Nasi pecel", 15_000, 10), ProductFormState("Nasi pecel", "15000", "10").toRequest())
        assertNull(ProductFormState("", "15000", "10").toRequest())
        assertNull(ProductFormState("Nasi", "0", "10").toRequest())
        assertNull(ProductFormState("Nasi", "", "10").toRequest())
        assertNull(ProductFormState("Nasi", "15000", "").toRequest())
    }

    @Test fun zero_stock_is_allowed_but_absurd_values_are_not() {
        assertEquals(0, ProductFormState("Nasi", "1000", "0").toRequest()?.stock)
        assertNull(ProductFormState("Nasi", "1000000000", "1").toRequest())
        assertNull(ProductFormState("Nasi", "1000", "10000").toRequest())
    }
}
