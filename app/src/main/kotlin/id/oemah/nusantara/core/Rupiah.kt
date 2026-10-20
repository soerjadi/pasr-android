package id.oemah.nusantara.core

/** Integer rupiah. Never floating point. */
@JvmInline
value class Rupiah(val amount: Long) {
    init {
        require(amount >= 0) { "Rupiah cannot be negative: $amount" }
    }

    operator fun plus(other: Rupiah): Rupiah = Rupiah(Math.addExact(amount, other.amount))

    operator fun times(quantity: Int): Rupiah = Rupiah(Math.multiplyExact(amount, quantity.toLong()))

    /** "Rp 15.000" — dot thousands separator regardless of device locale. */
    fun format(): String = "Rp " + amount.toString().reversed().chunked(3).joinToString(".").reversed()
}
