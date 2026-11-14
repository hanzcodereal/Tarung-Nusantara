# Tarung Nusantara

Game tinju 2D bertema budaya Nusantara untuk Android. Versi **1.0.0**.

- Developer: **Hanz Dev Official**
- Kontak: hanzzcodee@gmail.com
- Dukung: https://saweria.co/hanzreally

Bahasa pemrograman: Kotlin. Tanpa pustaka pihak ketiga (hanya Android SDK), menggambar dengan Canvas dan SurfaceView, antarmuka dalam bahasa Indonesia. Dokumen rancangan sumber: `Tarung_Nusantara.md`.

## Menjalankan build APK (manual, release)

Build tidak berjalan otomatis. Jalankan sendiri:

1. Unggah isi folder ini ke repositori GitHub (akar repositori berisi `app/`, `build.gradle.kts`, `.github/`).
2. Buka tab **Actions**, pilih **Build APK Release**, klik **Run workflow**.
3. Setelah selesai, unduh artefak **TarungNusantara-1.0.0-release-apk** di bagian bawah halaman proses itu.

APK yang dihasilkan adalah **release** (R8 dan penyusutan resource aktif), bukan debug, dengan versionName `1.0.0` dan versionCode `10000`.

### Tanda tangan APK

APK release harus ditandatangani agar bisa dipasang.

- **Tanpa pengaturan apa pun:** workflow membuat keystore baru tiap build. APK bisa dipasang, tetapi build berikutnya tidak bisa menimpa pemasangan sebelumnya (harus copot dulu, data game ikut hilang).
- **Disarankan:** buat satu keystore dan simpan di Settings > Secrets and variables > Actions dengan nama `KEYSTORE_BASE64` (hasil `base64 -w0 release.keystore`), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Simpan berkas keystore dengan aman, jangan dimasukkan ke repositori.

### Build di komputer sendiri

Butuh JDK 17 dan Android SDK 34, serta Gradle 8.9 terpasang (repositori ini tidak menyertakan Gradle wrapper).

```
gradle :app:assembleRelease
```

Tanpa variabel `KEYSTORE_PATH`, hasilnya `app-release-unsigned.apk`.

## Yang ada di dalam game

- Mode: Karier (6 bab, 3 laga biasa dan 1 laga penentu per bab, cerita dan dialog), Arcade (1 nyawa, tingkat naik tiap 5 kemenangan, skor tertinggi disimpan), Duel Cepat, Latihan (tutorial Pak Guru Wiryo 6 langkah, kantong tinju, statistik akurasi), Mabar Bluetooth.
- 10 petarung dengan statistik dan jurus spesial masing-masing, 6 arena, 4 tingkat AI (AI mencatat 3 serangan terakhir pemain).
- Pertarungan: Jab, Hook, Uppercut, tunduk, tangkis dan tangkisan sempurna, hindar (geser cepat dua kali), stamina, meteran jurus, pingsan sesaat (limbung), jatuh, KO, TKO, poin.
- Toko dan garasi: sarung tinju, aksesori kepala, kostum adat, upgrade statistik 5 level, jamu sekali pakai. Koin tidak bisa dibeli dengan uang asli.
- 10 pencapaian, hadiah harian, pengaturan (suara, musik, komentator, getar, kurangi gerak, tingkat kesulitan bawaan, posisi tombol).
- Bluetooth Classic (RFCOMM): host menghitung pertandingan, client mengirim input sekitar 20 kali per detik, rematch, papan skor sesi, sambung ulang otomatis sampai 5 detik, indikator kualitas sambungan (latensi terukur), izin Android 12 ke atas.

## Struktur

```
app/src/main/java/com/hanzdev/tarungnusantara/
  MainActivity.kt        navigasi layar
  game/                  mesin pertarungan, AI, penggambar, kontrol sentuh, GameActivity
  bluetooth/             sambungan, protokol, sesi
  data/                  penyimpanan, toko, pencapaian, karier, teks
  audio/                 efek, musik, komentator
  ui/                    layar menu dan HUD
app/src/main/assets/     arena (WebP), audio (OGG), huruf, logo, penonton
tools/assets/            skrip pembuat semua aset
docs/                    DESIGN.md, KONTRAS.txt, LISENSI-ASET.md, LAPORAN.md
```

Membuat ulang aset: `python3 tools/assets/gen_ui.py app/src/main`, `python3 tools/assets/gen_arenas.py app/src/main/assets/arena`, `python3 tools/assets/gen_audio.py app/src/main/assets/audio`.

## Bagian yang saya tambahkan karena dokumen rancangan belum menentukan

Ini keputusan saya, silakan ubah:

- Harga jamu (100, 150, 200) dan harga kostum (Celana Batik 600, Sarung Bugis 700, Rompi Ulos 800, Cawat Betang 900).
- Statistik, jurus, dan dialog empat petarung yang bisa dibuka (Jaelani, Yance, Cut Zahra, Sang Garuda Bertopeng), serta nama jurus tiap petarung.
- Angka keseimbangan pertarungan (kecepatan tiap pukulan, damage, biaya stamina, jendela tangkisan sempurna, rumus pengaruh statistik).
- Skor Arcade (100 poin per kemenangan), bonus Ibu Lastri (satu jamu kunyit gratis per bab), jeda 10 detik antar-ronde bisa tidak dilewati.
- Komentator berupa teks di layar dan, bila perangkat punya suara bahasa Indonesia, dibacakan oleh text-to-speech bawaan Android.
- Petarung digambar dari kerangka oleh kode (bukan atlas sprite), supaya kostum dan sarung tinju dari toko bisa dipakai di semua petarung. Ukuran APK jauh di bawah batas 30 MB.
