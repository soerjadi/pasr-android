package id.oemah.nusantara.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RupiahTest {
    @Test fun formats_with_dot_thousands_separator() {
        assertEquals("Rp 15.000", Rupiah(15_000).format())
        assertEquals("Rp 1.500", Rupiah(1_500).format())
        assertEquals("Rp 1.234.567", Rupiah(1_234_567).format())
    }

    @Test fun formats_zero() {
        assertEquals("Rp 0", Rupiah(0).format())
    }

    @Test fun multiplies_by_quantity() {
        assertEquals(Rupiah(45_000), Rupiah(15_000) * 3)
    }

    @Test fun adds() {
        assertEquals(Rupiah(3), Rupiah(1) + Rupiah(2))
    }

    @Test fun rejects_negative_amounts() {
        assertThrows(IllegalArgumentException::class.java) { Rupiah(-1) }
    }
}
