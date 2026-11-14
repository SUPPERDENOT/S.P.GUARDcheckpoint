# Superd Power Guard — Advanced Battery Charging & Anomaly Diagnostic App

Aplikasi Android profesional (`Kotlin` + `Jetpack Compose` + `Room` + `Foreground Service`) untuk memantau arus pengisian baterai (`mA`), daya (`Watt`), tegangan (`mV`), suhu (`°C`), total kapasitas masuk per sesi (`mAh`), estimasi kesehatan baterai secara real-time, serta deteksi dini *fake charging* dan *thermal throttling*.

## ✨ Fitur Utama
1. **Real-time Power & Current Monitoring**: Pemantauan arus masuk/keluar (`mA`), tegangan (`mV`), dan daya aktif (`Watt`).
2. **Advanced Anomaly Detector**: Deteksi dini *fake charging*, *slow charging*, dan *thermal throttling* secara real-time.
3. **Session & Health Tracking**: Pencatatan sesi pengisian daya, estimasi kapasitas nyata (`mAh`), persentase kesehatan baterai, serta analisis efisiensi CPU (*Deep Sleep Ratio*).
4. **Transparency & App Info (Read More)**: Halaman informasi lengkap mengenai lisensi, kebijakan privasi, syarat penggunaan, disclaimer pengukuran, catatan keamanan, serta kredit developer (`superdenot`).
5. **Automated GitHub Actions**: Build APK otomatis setiap kali kode diperbarui di repository GitHub.

---

## 🚀 Cara Build APK Otomatis via GitHub Actions

1. Buat repositori baru di GitHub dengan nama **PowerGuard**.
2. Upload seluruh isi folder `PowerGuard` (termasuk folder `.github/workflows/build-apk.yml` dan ikon baru di `mipmap`) ke branch `main`.
3. Buka tab **Actions** di repositori GitHub Anda dan tunggu proses **Build PowerGuard Debug APK** selesai (sekitar 2–4 menit).
4. Unduh file **`PowerGuard-Debug-APK`** pada bagian **Artifacts** di halaman hasil build.

---

## 📋 Lisensi & Hak Cipta
© 2026 superdenot. Seluruh hak cipta dilindungi undang-undang. Didistribusikan di bawah lisensi MIT / Open Source Proprietary.
