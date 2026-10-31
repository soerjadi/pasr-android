package id.oemah.nusantara.feature.store

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CountdownTest {
    @Test fun under_an_hour_is_minutes_and_seconds() = assertEquals("07:42", formatCountdown(462))

    @Test fun an_hour_or_more_adds_hours() = assertEquals("1:05:09", formatCountdown(3909))

    @Test fun negative_clamps_to_zero() = assertEquals("00:00", formatCountdown(-5))

    @Test fun seconds_until_deadline() {
        val now = Instant.parse("2026-10-29T10:00:00Z")
        assertEquals(462L, secondsUntil("2026-10-29T10:07:42Z", now))
    }

    @Test fun absent_or_garbled_deadline_is_null() {
        assertNull(secondsUntil(null))
        assertNull(secondsUntil("soon"))
    }
}
