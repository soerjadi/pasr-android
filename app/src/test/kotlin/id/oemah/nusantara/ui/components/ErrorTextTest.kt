package id.oemah.nusantara.ui.components

import id.oemah.nusantara.core.AppError
import org.junit.Assert.assertEquals
import org.junit.Test

class ErrorTextTest {
    @Test fun offline() = assertEquals(
        "Tidak ada koneksi internet. Periksa sambungan Anda lalu coba lagi.",
        AppError.Offline.userMessage(),
    )

    @Test fun server() = assertEquals("Terjadi gangguan pada server. Coba lagi nanti.", AppError.Server.userMessage())

    @Test fun unauthorized() = assertEquals("Sesi Anda berakhir. Silakan masuk lagi.", AppError.Unauthorized.userMessage())

    @Test fun unknown() = assertEquals("Terjadi kesalahan. Coba lagi.", AppError.Unknown(null).userMessage())

    @Test fun rejected_uses_the_server_message_when_present() {
        assertEquals("Stok tidak cukup", AppError.Rejected("X", "Stok tidak cukup").userMessage())
        assertEquals("Terjadi kesalahan. Coba lagi.", AppError.Rejected("X", null).userMessage())
    }
}
