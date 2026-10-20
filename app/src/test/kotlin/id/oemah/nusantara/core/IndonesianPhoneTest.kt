package id.oemah.nusantara.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IndonesianPhoneTest {
    @Test fun normalizes_common_mobile_formats_to_e164() {
        val expected = "+6281234567890"
        assertEquals(expected, IndonesianPhone.normalize("0812-3456-7890"))
        assertEquals(expected, IndonesianPhone.normalize("+62 812 3456 7890"))
        assertEquals(expected, IndonesianPhone.normalize("6281234567890"))
        assertEquals(expected, IndonesianPhone.normalize(" (0812) 3456 7890 "))
    }

    @Test fun rejects_empty_short_landline_letters_and_too_long() {
        assertNull(IndonesianPhone.normalize(""))
        assertNull(IndonesianPhone.normalize("081"))
        assertNull(IndonesianPhone.normalize("0212345678"))
        assertNull(IndonesianPhone.normalize("abc"))
        assertNull(IndonesianPhone.normalize("0812345678901234"))
    }

    @Test fun builds_whatsapp_url_from_e164() {
        assertEquals("https://wa.me/6281234567890", IndonesianPhone.toWhatsAppUrl("+6281234567890"))
    }

    @Test fun whatsapp_url_is_null_for_invalid_number() {
        assertNull(IndonesianPhone.toWhatsAppUrl("0812"))
    }
}
