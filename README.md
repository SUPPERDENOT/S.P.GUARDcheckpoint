# SUPERD POWER GUARD

**Battery Diagnostics · Charging Intelligence · Battery Protection**

SuperD Power Guard adalah aplikasi Android untuk memantau kondisi baterai dan menganalisis perilaku pengisian daya secara langsung. Aplikasi mengumpulkan metrik baterai yang tersedia melalui Android, menyimpan riwayat sesi pengisian, dan memberikan indikasi ketika pola pengisian terdeteksi tidak normal.

> **Nama produk:** SUPERD POWER GUARD  
> **Nama launcher yang dituju:** SuperD Guard  
> **Application ID:** `com.superdenot.superdguard`

---

## Tentang proyek

SuperD Power Guard dikembangkan untuk membantu pengguna memahami apa yang terjadi pada baterai dan proses charging perangkat mereka—bukan sekadar menampilkan persentase baterai.

Fokus utamanya:
- pemantauan arus, tegangan, daya, suhu, dan level baterai;
- analisis sesi pengisian dan penyimpanan riwayat;
- deteksi indikasi anomali pengisian;
- pemantauan yang mengikuti status charger, dengan opsi pemantauan manual;
- pemulihan dan finalisasi data sesi secara aman ketika proses pemantauan terganggu.

Aplikasi ini **bukan** aplikasi parental control, aplikasi penghemat baterai ajaib, atau pengendali hardware universal.

## Fitur

### Pemantauan baterai langsung
Dashboard menampilkan data yang dapat dibaca dari perangkat, antara lain:
- level baterai;
- arus baterai (mA);
- tegangan (mV);
- estimasi daya (W);
- suhu baterai (°C);
- sumber daya yang terdeteksi Android (AC, USB, wireless, atau discharging);
- durasi sesi dan estimasi kapasitas yang masuk selama sesi.

Ketersediaan dan akurasi setiap metrik bergantung pada sensor, fuel gauge, kernel, serta implementasi Android pada masing-masing perangkat.

### Smart Charging Monitoring
- Pemantauan otomatis dimulai ketika perangkat mendeteksi charger terhubung.
- Sesi pengisian dihentikan dan difinalisasi ketika charger dilepas.
- Riwayat sesi yang sudah tersimpan tetap tersedia setelah pemantauan berhenti.
- Pemantauan manual juga dapat dimulai dari aplikasi.

### Charging Anomaly Detection
Mesin analisis memeriksa sejumlah pola, termasuk:
- arus pengisian rendah dalam periode tertentu;
- perubahan arah arus yang berulang;
- indikasi pengisian lambat atau arus bersih negatif saat charger terhubung;
- kondisi suhu tinggi yang disertai penurunan arus.

Peringatan merupakan **indikasi berbasis pembacaan sensor dan heuristik**, bukan diagnosis kerusakan baterai, charger, kabel, atau perangkat keras.

### Riwayat dan penyimpanan lokal
Data sesi, sampel baterai, ringkasan kesehatan, dan rekap harian dikelola menggunakan Room. Preferensi kapasitas desain baterai disimpan menggunakan DataStore.

### Ketahanan pemantauan
Implementasi mencakup:
- foreground service untuk pemantauan aktif;
- fallback pembacaan sensor ketika data service tidak lagi segar;
- pemeriksaan freshness timestamp, bukan hanya status service;
- checkpoint dan rekonsiliasi untuk membantu pemulihan sesi yang belum selesai;
- finalisasi sesi yang dirancang agar aman terhadap percobaan ulang.

## Batasan dan akurasi

Informasi berikut penting agar hasil aplikasi tidak disalahartikan:

1. **Protokol charging tidak ditebak.** Status protokol hanya ditampilkan jika perangkat/API menyediakan informasi yang dapat diverifikasi. Jika tidak tersedia, aplikasi menampilkan `Unknown` atau `Not Exposed by Device`. Tegangan, arus, dan daya tidak digunakan untuk menyimpulkan protokol maupun status fast charging.
2. **Arus dan daya bukan alat ukur laboratorium.** Nilainya mengikuti sensor dan pelaporan perangkat. Nilai dapat berbeda dari output adaptor atau pengukuran USB eksternal.
3. **Suhu yang ditampilkan adalah suhu baterai yang dilaporkan sistem**, bukan suhu permukaan casing atau suhu ruangan.
4. **Deteksi anomali bersifat heuristik.** Peringatan harus dipakai sebagai petunjuk pemeriksaan, bukan kesimpulan teknis final.
5. **Tidak ada janji auto-cut universal.** Android dan mayoritas perangkat tidak memberi aplikasi pihak ketiga akses umum untuk memutus aliran listrik charger. Pembatasan pengisian hanya mungkin jika produsen menyediakan mekanisme yang kompatibel.
6. **Dukungan sensor tidak seragam.** Sebagian perangkat dapat tidak mengekspos metrik tertentu. Nilai nol yang valid harus dibedakan dari data yang tidak tersedia; aplikasi menyimpan status ketersediaan metrik agar riwayat tidak mengarang pembacaan.

## Teknologi

| Komponen | Teknologi |
|---|---|
| Bahasa | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Arsitektur aplikasi | MVVM dengan pemisahan data, domain, service, dan UI |
| Asynchronous state | Kotlin Coroutines & Flow |
| Database lokal | Room |
| Preferensi | DataStore Preferences |
| Background scheduling | WorkManager |
| Build | Android Gradle Plugin 8.7.2, Kotlin 2.0.21, Gradle 8.9 |
| Android SDK | compileSdk 35, targetSdk 35, minSdk 26 |
| JVM target | Java 17 |

## Struktur kode

Direktori utama:

```text
app/src/main/java/com/superdenot/powerguard/
├── data/
│   ├── local/          # Room database, DAO, entity, recovery store
│   └── repository/     # Akses data dan preferensi
├── domain/
│   ├── engine/         # BatteryMonitorEngine dan AnomalyDetector
│   └── model/          # Model state baterai
├── receiver/           # Deteksi charger dan boot
├── service/            # Foreground battery monitoring service
├── ui/
│   ├── dashboard/      # Dashboard dan pemilihan live state
│   ├── viewmodel/      # State UI dan fallback sensor poller
│   └── theme/          # Tema aplikasi
└── worker/             # Rekap dan pekerjaan terjadwal
```

## Persyaratan pengembangan

- Android Studio yang mendukung Android Gradle Plugin 8.7.2.
- JDK 17.
- Gradle 8.9.
- Android SDK Platform 35.
- Perangkat atau emulator Android 8.0 (API 26) atau lebih baru untuk pengujian.

Repository saat ini menggunakan konfigurasi Gradle Wrapper, tetapi skrip `gradlew` tidak tersedia pada branch ini. Jalankan perintah menggunakan instalasi Gradle 8.9 yang tersedia di lingkungan pengembangan.

## Build dan pengujian

Build APK debug:

```bash
gradle --no-daemon assembleDebug
```

Jalankan unit test:

```bash
gradle --no-daemon testDebugUnitTest
```

Build sekaligus menjalankan unit test:

```bash
gradle --no-daemon testDebugUnitTest assembleDebug
```

APK debug, jika build berhasil, tersedia di:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Pengujian unit dan keberhasilan build tidak menggantikan pengujian pada perangkat fisik. Perilaku service, sensor, notifikasi, pembatasan background, dan akurasi metrik tetap perlu divalidasi pada beberapa merek dan model perangkat.

## Izin Android

Aplikasi mendeklarasikan izin yang diperlukan untuk foreground service, notifikasi, deteksi boot, dan beberapa pemeriksaan sistem. Akses Usage Stats bersifat khusus dan diperlukan hanya untuk konteks analisis penggunaan aplikasi; pengguna harus mengaktifkannya melalui pengaturan Android jika ingin menggunakan bagian tersebut.

Izin sistem dapat berbeda perilakunya menurut versi Android dan kebijakan masing-masing produsen. Berikan izin hanya jika fitur terkait memang digunakan.

## Status pengembangan

Proyek masih dalam tahap pengembangan dan validasi. Branch pengembangan aktif untuk pekerjaan ini adalah `fix/auto-start-coordinator-timing`. Status fitur dan verifikasi mengikuti commit serta hasil CI pada branch tersebut.

**Belum dianggap rilis stabil untuk semua perangkat.** Keberhasilan CI dan unit test menunjukkan pemeriksaan otomatis lolos pada lingkungan build, bukan jaminan bahwa seluruh kombinasi perangkat dan versi Android sudah tervalidasi.

Fitur yang belum tersedia atau belum tervalidasi secara menyeluruh tidak boleh dianggap sebagai kemampuan rilis, termasuk prediksi waktu penuh yang akurat untuk semua perangkat, pengaturan target charging yang didukung universal, dan pemutusan charger otomatis.

## Privasi

Data operasional aplikasi dirancang untuk disimpan secara lokal menggunakan Room dan DataStore. Fitur yang membaca statistik penggunaan aplikasi memerlukan izin Usage Access dari Android. Tinjau izin yang diminta dan pengaturan perangkat sebelum mengaktifkan fitur tersebut.

## Lisensi

Lisensi proyek belum ditetapkan dalam repository ini. Sampai file lisensi resmi ditambahkan, jangan menganggap kode ini sebagai perangkat lunak open-source dengan izin penggunaan ulang tertentu.

---

**SUPERD POWER GUARD**  
*Understand your battery. Monitor every charge. Protect with insight.*
