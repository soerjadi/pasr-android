package id.oemah.nusantara.core

import id.oemah.nusantara.api.model.ImageSet
import org.junit.Assert.assertEquals
import org.junit.Test

class ImagesTest {
    private val images = ImageSet(small = "s", medium = "m", large = "l")

    @Test fun picks_small_up_to_320px() {
        assertEquals("s", images.pick(300))
        assertEquals("s", images.pick(320))
    }

    @Test fun picks_medium_up_to_640px() {
        assertEquals("m", images.pick(321))
        assertEquals("m", images.pick(640))
    }

    @Test fun picks_large_above_640px() {
        assertEquals("l", images.pick(641))
        assertEquals("l", images.pick(700))
    }
}
