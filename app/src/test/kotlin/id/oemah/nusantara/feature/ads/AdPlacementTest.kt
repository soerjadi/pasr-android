package id.oemah.nusantara.feature.ads

import org.junit.Assert.assertEquals
import org.junit.Test

class AdPlacementTest {
    @Test fun there_are_exactly_the_four_spec_placements_and_none_for_cart_or_checkout() {
        assertEquals(
            setOf("home_partner_carousel", "home_sponsored_card", "post_purchase", "wait_state"),
            AdPlacement.entries.map { it.key }.toSet(),
        )
        assertEquals(4, AdPlacement.entries.size)
    }
}
