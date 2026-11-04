# Video Merger 1080p — Android Native (Kotlin)

Aplikasi Android native untuk menggabungkan dua video secara berurutan menjadi satu MP4 Full HD. Dibangun dengan Kotlin dan Jetpack Media3 Transformer.

## Fitur versi awal
- Memilih dua video dari galeri melalui Android document picker.
- Menggabungkan Video 1 lalu Video 2.
- Mengonversi output ke MP4 dengan video H.264 dan audio AAC.
- Menargetkan output 1920 × 1080 (16:9) dan menyesuaikan gambar agar tidak terpotong.
- Mempertahankan audio dari klip yang memiliki audio (Media3 akan menyusun track audio dari sequence).
- Menyimpan hasil ke folder Movies/VideoMerger.

## Cara mendapatkan APK hanya dengan HP Android + GitHub

Proyek ini memakai GitHub Actions untuk membangun APK di cloud, sehingga tidak harus memasang Android Studio di HP.

1. Unduh ZIP proyek dan ekstrak.
2. Buat repository baru di https://github.com/new, misalnya `video-merger-kotlin`.
3. Upload seluruh isi folder proyek ke root repository. Pastikan folder `.github/workflows` ikut terunggah.
4. Buka tab **Actions** di repository.
5. Pilih workflow **Build Android APK**. Jika diminta, aktifkan GitHub Actions.
6. Jalankan **Run workflow**, atau tunggu build otomatis setelah push ke branch `main`.
7. Setelah workflow selesai dengan status hijau, buka run tersebut.
8. Di bagian **Artifacts**, unduh `VideoMerger-debug-apk`.
9. Ekstrak ZIP artifact, lalu instal `app-debug.apk` di Android. Jika diminta, izinkan pemasangan aplikasi dari sumber tersebut.

## Struktur proyek
```text
VideoMergerKotlin/
├── .github/workflows/build-apk.yml
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/projectsore/videomerger/MainActivity.kt
├── build.gradle.kts
├── gradle.properties
└── settings.gradle.kts
```

`app/debug.keystore` wajib ikut terunggah ke GitHub.

## Pengujian awal
- Coba dua klip pendek terlebih dahulu (5–15 detik).
- Gunakan video MP4/H.264 yang umum untuk tes awal.
- Pastikan baterai cukup dan perangkat tidak terlalu panas.
- Pemrosesan 1080p dapat memakan waktu dan ruang penyimpanan. Jangan tutup aplikasi saat ekspor.
- Jika perangkat tidak mendukung encoder/format tertentu, ekspor bisa gagal.

## Catatan kualitas dan format
Resolusi output ditargetkan 1920×1080. Video sumber yang resolusinya lebih rendah tidak akan memperoleh detail asli 1080p hanya karena diperbesar. Klip dengan rasio berbeda akan disesuaikan ke kanvas 16:9 dengan skala fit; tampilan akhir mengikuti perilaku efek Presentation Media3. Keberhasilan 1080p tetap bergantung pada kemampuan encoder perangkat.

## Troubleshooting
- **Cara membaca error build:** di halaman run yang gagal, buka langkah `Ringkasan error (jika gagal)`, atau unduh artifact `build-log` (berisi log lengkap).
- **"Aplikasi tidak terpasang" saat menimpa versi lama:** proyek ini sudah memakai `app/debug.keystore` tetap. Jika APK lama dibuat sebelum keystore ini ada, hapus (uninstall) aplikasi lama sekali, lalu pasang APK baru. Setelah itu update berikutnya bisa langsung menimpa.
- **Build gagal:** buka tab Actions > pilih run gagal > baca log langkah `Build debug APK`. Periksa apakah seluruh folder dan file tersembunyi `.github` sudah terunggah.
- **APK tidak terpasang:** pastikan mengunduh dan mengekstrak artifact ZIP, lalu instal APK di dalamnya.
- **Ekspor gagal:** coba dua video pendek MP4/H.264. Format kamera tertentu, HDR, variable frame rate, atau codec tertentu mungkin tidak kompatibel di semua perangkat.
- **Aplikasi berhenti saat ekspor:** tutup aplikasi lain, kosongkan ruang penyimpanan, dan uji video yang lebih pendek.

## Catatan pengembangan
Versi ini adalah fondasi proyek, bukan aplikasi yang sudah diuji di perangkat fisik. Build GitHub Actions akan memeriksa kompilasi. Setelah APK berhasil dibuat, tetap lakukan pengujian di HP Android. UI saat ini sengaja sederhana untuk memprioritaskan fungsi inti.
