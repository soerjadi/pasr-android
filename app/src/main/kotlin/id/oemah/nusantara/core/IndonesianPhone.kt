package id.oemah.nusantara.core

object IndonesianPhone {
    private val NATIONAL_MOBILE = Regex("^8\\d{8,11}$")
    private val SEPARATORS = Regex("[\\s\\-().]")

    /** Returns the number as `+62…` or null when it is not an Indonesian mobile number. */
    fun normalize(input: String): String? {
        val compact = input.replace(SEPARATORS, "")
        val national = when {
            compact.startsWith("+62") -> compact.removePrefix("+62")
            compact.startsWith("62") -> compact.removePrefix("62")
            compact.startsWith("0") -> compact.removePrefix("0")
            else -> return null
        }
        return if (NATIONAL_MOBILE.matches(national)) "+62$national" else null
    }

    fun toWhatsAppUrl(e164: String): String? =
        normalize(e164)?.let { "https://wa.me/${it.removePrefix("+")}" }
}
