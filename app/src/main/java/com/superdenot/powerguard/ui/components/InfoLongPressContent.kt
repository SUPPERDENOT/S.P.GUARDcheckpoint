package com.superdenot.powerguard.ui.components

internal data class InfoLongPressContent(
    val title: String,
    val body: String
)

internal object InfoLongPressContentFactory {
    fun statusBarIndicator(): InfoLongPressContent = InfoLongPressContent(
        title = "Indikator status bar",
        body = "Indikator status bar menampilkan parameter baterai yang dipilih pengguna pada area status bar. Komponen ini berbeda dari Monitoring Notification yang tampil di panel notifikasi."
    )

    fun headerStatus(
        statusLabel: String,
        isServiceRunning: Boolean,
        isChargingStatusAvailable: Boolean,
        isCharging: Boolean
    ): InfoLongPressContent {
        val chargingStatus = when {
            !isChargingStatusAvailable -> "Tidak tersedia"
            isCharging -> "Mengisi"
            else -> "Tidak mengisi"
        }
        val serviceStatus = if (isServiceRunning) "Aktif" else "Tidak berjalan"
        return InfoLongPressContent(
            title = "Status pemantauan dan pengisian",
            body = "Status kapsul: $statusLabel\n" +
                "Status pengisian dari perangkat: $chargingStatus\n" +
                "Layanan pemantauan latar belakang: $serviceStatus\n\n" +
                "Status pengisian dan status layanan adalah dua informasi berbeda. Label Mengisi tidak dengan sendirinya memastikan layanan pemantauan sedang berjalan."
        )
    }
}
