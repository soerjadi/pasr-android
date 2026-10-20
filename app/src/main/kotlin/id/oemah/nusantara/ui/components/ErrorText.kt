package id.oemah.nusantara.ui.components

import id.oemah.nusantara.core.AppError

private const val GENERIC = "Terjadi kesalahan. Coba lagi."

fun AppError.userMessage(): String = when (this) {
    AppError.Offline -> "Tidak ada koneksi internet. Periksa sambungan Anda lalu coba lagi."
    AppError.Server -> "Terjadi gangguan pada server. Coba lagi nanti."
    AppError.Unauthorized -> "Sesi Anda berakhir. Silakan masuk lagi."
    is AppError.Conflict -> message?.takeIf { it.isNotBlank() } ?: GENERIC
    is AppError.Rejected -> message?.takeIf { it.isNotBlank() } ?: GENERIC
    is AppError.Unknown -> GENERIC
}
