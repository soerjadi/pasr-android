package id.oemah.nusantara.feature.ads

/** The only places ads may appear. There is deliberately no cart or checkout slot. */
enum class AdPlacement(val key: String) {
    HOME_PARTNER_CAROUSEL("home_partner_carousel"),
    HOME_SPONSORED_CARD("home_sponsored_card"),
    POST_PURCHASE("post_purchase"),
    WAIT_STATE("wait_state"),
}
