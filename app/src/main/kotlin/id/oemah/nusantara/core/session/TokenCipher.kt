package id.oemah.nusantara.core.session

interface TokenCipher {
    fun encrypt(plain: String): String
    fun decrypt(cipher: String): String
}
