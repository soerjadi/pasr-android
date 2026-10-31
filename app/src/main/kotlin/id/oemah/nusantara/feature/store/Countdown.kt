package id.oemah.nusantara.feature.store

import java.time.Duration
import java.time.Instant
import java.util.Locale

/** "07:42" for under an hour, "1:05:09" beyond. */
fun formatCountdown(seconds: Long): String {
    val s = seconds.coerceAtLeast(0)
    return if (s >= 3600) {
        String.format(Locale.ROOT, "%d:%02d:%02d", s / 3600, s % 3600 / 60, s % 60)
    } else {
        String.format(Locale.ROOT, "%02d:%02d", s / 60, s % 60)
    }
}

/** Whole seconds from [now] until the ISO-8601 [acceptBy]; null when absent or unparsable. */
fun secondsUntil(acceptBy: String?, now: Instant = Instant.now()): Long? {
    val deadline = acceptBy?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
    return Duration.between(now, deadline).seconds
}
