package id.oemah.nusantara.core

import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/**
 * Indonesian dates in Jakarta time. Month names are fixed here rather than taken from the device
 * locale, so they read the same on every phone ("Agu", never "Agt").
 */
object DateFormat {
    private val JAKARTA = ZoneId.of("Asia/Jakarta")
    private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")

    fun date(at: Instant): String {
        val local = at.atZone(JAKARTA)
        return "${local.dayOfMonth} ${MONTHS[local.monthValue - 1]} ${local.year}"
    }

    fun dateTime(at: Instant): String {
        val local = at.atZone(JAKARTA)
        return date(at) + ", " + String.format(Locale.ROOT, "%02d:%02d", local.hour, local.minute)
    }

    /** API timestamps are ISO-8601 strings; anything unparsable is shown as received. */
    fun date(iso: String): String = parse(iso)?.let(::date) ?: iso

    fun dateTime(iso: String): String = parse(iso)?.let(::dateTime) ?: iso

    private fun parse(iso: String): Instant? = runCatching { Instant.parse(iso) }.getOrNull()
}
