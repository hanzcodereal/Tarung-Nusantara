# Laporan pemeriksaan

Mode anti-slop: DURING (dipilih lewat instruksi sesi). Pembacaan desain dan alasan dial ada di `DESIGN.md`.

## Sudah diperiksa

| Hal | Cara | Hasil |
| --- | --- | --- |
| Kontras teks pada palet | `contrast-check.py` (lihat `KONTRAS.txt`) | Semua pasangan lolos 4.5:1 setelah dua perbaikan |
| Tanda pisah em dash dan en dash di semua kode | pencarian otomatis | 0 ditemukan |
| Kurung dan kurawal seimbang di 26 berkas Kotlin | skrip | Seimbang |
| Semua impor proyek mengarah ke simbol yang ada, tanpa impor tak terpakai | skrip | Bersih |
| Aset gambar | dilihat langsung (arena, logo, ikon, penonton) | Terbaca, ikon disesuaikan dua kali |
| Loudness musik | ffmpeg ebur128 | sekitar -20 LUFS, puncak di bawah -4 dBFS |
| Ukuran aset | du | audio 3,3 MB, total APK diperkirakan jauh di bawah 30 MB |

## Belum bisa diperiksa

- **Kompilasi APK dan jalannya game di perangkat.** Lingkungan tempat saya bekerja tidak punya Kotlin, Gradle, Android SDK, ataupun internet. Kode ditulis dengan hati-hati dan diperiksa secara statis, tetapi build pertama bisa menemukan kesalahan kompilasi kecil. Bila ada, kirim log kesalahannya.
- **Bluetooth antar dua perangkat nyata**, keseimbangan pertarungan, dan terdengarnya musik: perlu dicoba langsung.
- Kinerja 60 FPS dan penggunaan RAM di perangkat yang lebih lama.
