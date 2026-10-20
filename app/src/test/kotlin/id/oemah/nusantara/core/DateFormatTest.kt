package id.oemah.nusantara.core

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class DateFormatTest {
    @Test fun formats_date_and_time_in_jakarta_time() {
        val at = Instant.parse("2026-10-15T03:24:00Z") // 10:24 in UTC+7
        assertEquals("15 Okt 2026, 10:24", DateFormat.dateTime(at))
        assertEquals("15 Okt 2026", DateFormat.date(at))
    }

    @Test fun late_evening_utc_rolls_over_to_the_next_day_in_jakarta() {
        assertEquals("16 Okt 2026, 01:30", DateFormat.dateTime(Instant.parse("2026-10-15T18:30:00Z")))
    }

    @Test fun month_names_are_the_same_on_every_device() {
        val months = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
        months.forEachIndexed { index, name ->
            val iso = "2026-%02d-10T05:00:00Z".format(index + 1)
            assertEquals("10 $name 2026", DateFormat.date(Instant.parse(iso)))
        }
    }

    @Test fun iso_strings_from_the_api_are_formatted_too() {
        assertEquals("15 Okt 2026, 10:24", DateFormat.dateTime("2026-10-15T03:24:00Z"))
    }

    @Test fun an_unparsable_timestamp_is_shown_as_is_instead_of_crashing() {
        assertEquals("kemarin", DateFormat.dateTime("kemarin"))
        assertEquals("kemarin", DateFormat.date("kemarin"))
    }
}
