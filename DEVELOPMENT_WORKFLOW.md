# SPG Development Workflow

Dokumen ini menetapkan alur kerja Git dan pengembangan resmi untuk Superd Power Guard (SPG).

## 1. Repository dan source of truth

Repository: `SUPPERDENOT/S.P.GUARDcheckpoint`

Source of Truth pengembangan saat ini:
`ui/industrial-instrument-antislop`

Branch rilis:
`main`

Branch historis yang dipertahankan:
- `hardening/p0-recovery`
- `feature/live-status-bar-p1`

Berdasarkan audit riwayat pada 17 November 2026, `ui/industrial-instrument-antislop` berada 64 commit di depan `hardening/p0-recovery`, 13 commit di depan `feature/live-status-bar-p1`, dan 216 commit di depan `main`. Ketiga branch tersebut merupakan leluhur dalam jalur riwayat branch UI saat audit dilakukan. Jangan menghapus branch historis hanya berdasarkan angka tersebut; periksa ulang sebelum tindakan penghapusan atau penggabungan.

## 2. Fungsi branch

### `main`
- Menyimpan versi stabil yang disetujui untuk rilis.
- Tidak digunakan untuk pekerjaan fitur harian.
- Integrasi ke branch ini hanya setelah pengujian dan pemeriksaan rilis.
- Tidak boleh diubah atau di-merge tanpa persetujuan pemilik proyek.

### `ui/industrial-instrument-antislop`
- Menjadi pusat pengembangan aktif dan acuan source terbaru.
- Pekerjaan rutin dapat dilakukan langsung di sini jika cakupannya kecil dan risikonya terkendali.
- Setiap pekerjaan harus dimulai dengan pemeriksaan branch, commit, status, dan diff.

### Branch sementara
- Tidak wajib untuk setiap perubahan.
- Gunakan untuk perbaikan terisolasi, eksperimen berisiko, atau pekerjaan paralel yang perlu dipisahkan.
- Buat dari commit terbaru `ui/industrial-instrument-antislop`, kecuali ada alasan teknis yang terdokumentasi untuk base lain.
- Setelah selesai, audit dan uji; integrasikan kembali ke source aktif.
- Jangan melanjutkan fitur lain dari branch sementara yang belum diintegrasikan.
- Branch sementara dapat dipensiunkan setelah integrasi diverifikasi. Penghapusan branch memerlukan pemeriksaan dan persetujuan.

## 3. Alur pekerjaan standar

1. Konfirmasi repository dan branch target.
2. Periksa SHA commit terakhir, status working tree, dan perubahan yang belum diamankan.
3. Periksa apakah source aktif sudah menjadi base yang benar.
4. Audit file dan dependensi yang relevan sebelum mengedit.
5. Pilih: perubahan langsung pada branch aktif atau branch sementara.
6. Buat perubahan sekecil dan seterukur mungkin.
7. Jalankan pemeriksaan statis, pengujian terkait, regression test, dan build yang relevan.
8. Tinjau diff untuk memastikan tidak ada perubahan di luar lingkup.
9. Laporkan hasil dengan SHA, daftar file, hasil tes/build, dan risiko tersisa.
10. Integrasikan branch sementara kembali ke source aktif setelah lolos pemeriksaan.

## 4. Aturan commit, push, dan merge

- Jangan commit, push, merge, rebase, force-push, atau menghapus branch tanpa persetujuan eksplisit pemilik proyek.
- Sebelum operasi Git yang mengubah riwayat atau remote, periksa ulang target branch dan SHA.
- Jangan menimpa perubahan yang belum diperiksa atau diamankan.
- Hindari force-push dan rewrite history.
- Gunakan pesan commit yang menjelaskan perubahan secara spesifik.
- `main` hanya menerima versi yang telah disetujui setelah validasi.
- Branch protection untuk `main` perlu dipertimbangkan dan diaktifkan setelah aturan CI serta akses kolaborator diverifikasi.

## 5. Standar pengujian

- Jalankan tes paling relevan terhadap perubahan.
- Jalankan regression suite yang mencakup perilaku terkait.
- Jalankan CI yang tersedia pada branch target.
- Workflow Android CI saat dokumen ini dibuat menjalankan UI audit dan perintah:
  `gradle --no-daemon testDebugUnitTest assembleDebug`
- Build atau test hanya boleh dilaporkan PASS jika hasil eksekusinya benar-benar tersedia dan berhasil.
- Pengujian unit tidak menggantikan pengujian perangkat nyata untuk perilaku Android yang bergantung pada OS, notifikasi, foreground service, atau perangkat keras.
- Catat kegagalan dan keterbatasan pengujian secara eksplisit.

## 6. Checkpoint dan laporan pekerjaan

Setiap pekerjaan yang selesai harus mencatat:
- Tanggal dan tujuan pekerjaan.
- Repository dan branch.
- SHA sebelum dan sesudah perubahan.
- File yang berubah.
- Ringkasan perubahan.
- Perintah dan hasil test/build.
- Status working tree jika tersedia.
- Risiko, keterbatasan, dan pekerjaan lanjutan.

## 7. Aturan keselamatan perubahan

- Pertahankan arsitektur dan perilaku yang sudah disepakati kecuali perubahan memang menjadi tujuan dan telah disetujui.
- Untuk service, database, session persistence, dan recovery, audit jalur kegagalan sebelum mengubah implementasi.
- Jangan menghapus data, checkpoint, atau mekanisme recovery hanya untuk membuat proses terlihat berhasil.
- Jika persistensi data belum terjamin, prioritaskan pemulihan data daripada mematikan service secara paksa.
- Jangan mengklaim perubahan, pengujian, build, atau deployment telah dilakukan jika belum diverifikasi.

## 8. Perubahan terhadap workflow

Dokumen ini adalah pedoman operasional. Jika struktur branch, CI, atau strategi rilis berubah, perbarui dokumen melalui perubahan yang ditinjau dan disetujui. Selalu audit keadaan repository terbaru; jangan menganggap angka commit atau status branch dalam dokumen ini selalu mutakhir.
