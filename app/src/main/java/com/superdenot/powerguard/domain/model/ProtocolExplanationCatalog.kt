package com.superdenot.powerguard.domain.model

data class ProtocolExplanation(
    val title: String,
    val powerRange: String,
    val summary: String
)

/**
 * Explanations describe the meaning and limitations of detector labels.
 * They do not claim that source capability metadata is measured charger output.
 */
object ProtocolExplanationCatalog {
    private val explanations = mapOf(
        "AC — Daya Standar" to ProtocolExplanation(
            title = "Kemampuan AC standar",
            powerRange = "Metadata kemampuan berada di bawah ambang 10 W dan 8,5 V.",
            summary = "Android mengekspos informasi kemampuan sumber AC, tetapi tidak mengidentifikasi protokol pengisian tertentu. Ini bukan pengukuran daya keluaran adaptor."
        ),
        "AC — Kemampuan Daya Tinggi" to ProtocolExplanation(
            title = "Kemampuan daya AC tinggi",
            powerRange = "Estimasi kemampuan metadata ≥ 10 W.",
            summary = "Label dihitung dari arus maksimum × tegangan maksimum yang dilaporkan BatteryManager. Nilai ini menunjukkan kemampuan sumber yang dilaporkan perangkat, bukan daya aktual yang sedang masuk ke baterai."
        ),
        "AC — Kemampuan 9V+" to ProtocolExplanation(
            title = "Kemampuan tegangan AC 9V+",
            powerRange = "Tegangan maksimum metadata ≥ 8.500 mV.",
            summary = "Perangkat melaporkan kemampuan tegangan maksimum setidaknya 8,5 V. Informasi ini tidak membuktikan bahwa USB Power Delivery, Quick Charge, atau protokol tertentu sedang digunakan."
        ),
        "USB Arus Tinggi (SDP/CDP?)" to ProtocolExplanation(
            title = "Sumber USB dengan arus maksimum tinggi",
            powerRange = "Arus maksimum metadata ≥ 900 mA.",
            summary = "Nilai kemampuan USB yang dilaporkan perangkat melewati ambang klasifikasi internal. Tanda tanya dipertahankan karena metadata ini tidak dapat memastikan apakah sumbernya SDP atau CDP."
        ),
        "USB Arus Standar (SDP?)" to ProtocolExplanation(
            title = "Sumber USB dengan arus maksimum standar",
            powerRange = "Arus maksimum metadata < 900 mA.",
            summary = "Perangkat melaporkan kemampuan arus USB di bawah ambang klasifikasi internal. Label ini bukan identifikasi pasti USB SDP."
        ),
        "Fast Charge (HAL)" to ProtocolExplanation(
            title = "Fast Charge — indikator HAL umum",
            powerRange = "Tidak tersedia dari label HAL umum.",
            summary = "HAL perangkat melaporkan status generik Fast/Adaptive/FastCharge. Nama protokol proprietary tidak diekspos, sehingga aplikasi tidak menebak PD, QC, AFC, VOOC, atau standar lain."
        ),
        "Tidak Terhubung" to ProtocolExplanation(
            title = "Tidak ada sumber pengisian terhubung",
            powerRange = "Tidak berlaku.",
            summary = "Perangkat tidak sedang mengisi daya atau tidak mendeteksi sumber daya eksternal."
        ),
        "Tidak Diekspos Perangkat (SELinux)" to ProtocolExplanation(
            title = "Akses metadata dibatasi",
            powerRange = "Tidak tersedia.",
            summary = "Pembacaan metadata ditolak oleh sistem dengan indikasi permission denial. Pembatasan akses tidak berarti protokol pengisian tertentu sedang digunakan."
        ),
        "Tidak Diekspos Perangkat" to ProtocolExplanation(
            title = "Informasi tidak diekspos perangkat",
            powerRange = "Tidak tersedia.",
            summary = "Perangkat atau Android tidak menyediakan nilai protokol yang dapat diverifikasi melalui sumber yang diperiksa. Aplikasi tidak menyimpulkan protokol dari arus, tegangan, atau watt."
        ),
        "AC — Kemampuan Daya Tidak Tersedia" to ProtocolExplanation(
            title = "Kemampuan sumber AC tidak tersedia",
            powerRange = "Tidak tersedia.",
            summary = "Sumber AC terdeteksi, tetapi metadata arus maksimum dan tegangan maksimum tidak cukup untuk mengklasifikasikan kemampuannya."
        ),
        "USB — Arus Maksimum Tidak Tersedia" to ProtocolExplanation(
            title = "Kemampuan arus USB tidak tersedia",
            powerRange = "Tidak tersedia.",
            summary = "Sumber USB terdeteksi, tetapi perangkat tidak menyediakan metadata arus maksimum yang dapat digunakan."
        )
    )

    fun get(label: String): ProtocolExplanation = explanations[label] ?: ProtocolExplanation(
        title = label.ifBlank { "Informasi protokol tidak tersedia" },
        powerRange = "Tidak dapat ditentukan dari label ini.",
        summary = "Label ini berasal dari nilai eksplisit perangkat atau belum memiliki penjelasan khusus. Aplikasi tidak mengubahnya menjadi klaim protokol lain."
    )

    fun supportedLabels(): Set<String> = explanations.keys
}
