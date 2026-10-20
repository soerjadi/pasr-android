package id.oemah.nusantara.feature.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler
import java.net.URI

object SafeLinks {
    /** Only plain web links: ad content is untrusted and must not launch intents, files or scripts. */
    fun isOpenable(url: String): Boolean {
        val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return false
        return uri.scheme?.lowercase() in WEB_SCHEMES && !uri.host.isNullOrBlank()
    }

    private val WEB_SCHEMES = setOf("http", "https")
}

/** Opens a link in the browser; does nothing when no app can handle it. */
@Composable
fun rememberLinkOpener(): (String) -> Unit {
    val handler = LocalUriHandler.current
    return remember(handler) {
        { url: String -> runCatching { handler.openUri(url) }; Unit }
    }
}
