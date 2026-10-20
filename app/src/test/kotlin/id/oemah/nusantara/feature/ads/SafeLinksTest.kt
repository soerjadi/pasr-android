package id.oemah.nusantara.feature.ads

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeLinksTest {
    @Test fun http_and_https_links_with_a_host_are_openable() {
        assertTrue(SafeLinks.isOpenable("https://a.id/x"))
        assertTrue(SafeLinks.isOpenable("http://a.id"))
        assertTrue(SafeLinks.isOpenable("HTTPS://a.id/x"))
    }

    @Test fun other_schemes_blank_and_hostless_links_are_not_openable() {
        assertFalse(SafeLinks.isOpenable("javascript:alert(1)"))
        assertFalse(SafeLinks.isOpenable("intent://scan/#Intent;scheme=zxing;end"))
        assertFalse(SafeLinks.isOpenable("file:///etc/passwd"))
        assertFalse(SafeLinks.isOpenable("content://contacts/people"))
        assertFalse(SafeLinks.isOpenable(""))
        assertFalse(SafeLinks.isOpenable("   "))
        assertFalse(SafeLinks.isOpenable("https"))
        assertFalse(SafeLinks.isOpenable("https://"))
        assertFalse(SafeLinks.isOpenable("not a url"))
    }
}
