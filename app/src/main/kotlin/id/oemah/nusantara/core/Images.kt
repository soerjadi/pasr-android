package id.oemah.nusantara.core

import id.oemah.nusantara.api.model.ImageSet

/** Smallest pre-resized variant that is not noticeably blurry for [targetPx] wide. */
fun ImageSet.pick(targetPx: Int): String = when {
    targetPx <= 320 -> small
    targetPx <= 640 -> medium
    else -> large
}
