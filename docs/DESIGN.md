# Arah Desain Tarung Nusantara

Pembacaan desain: game tinju 2D Android untuk pemain Indonesia remaja dan dewasa, bergaya poster wayang dan batik yang hangat. Dial: ENERGY 3 / RHYTHM 2 / MOTION 2.

Alasan tiap dial:
- ENERGY 3: ini game laga, layar harus terasa tegang sebelum bel ronde. Judul besar, satu aksen merah.
- RHYTHM 2: layar menu memakai satu kerangka (judul, isi, tombol bawah), tetapi Pilih Petarung, Peta Karier, dan Toko punya komposisi sendiri karena isinya memang berbeda.
- MOTION 2: gerak dipakai untuk umpan balik (pukulan, tangkisan, jurus). Menu hanya berpindah singkat. Semua gerak non-esensial mati bila pemain menyalakan "Kurangi gerak".

## Palet (3 inti, 1 aksen, netral)

| Peran | Warna | Alasan |
| --- | --- | --- |
| Sogan (inti) | `#231710` | Cokelat batik sogan, latar semua layar. Hangat, bukan hitam dingin. |
| Nila (inti) | `#1D2A4F` | Biru nila pewarna kain, untuk panel dan kartu. |
| Kunyit (inti) | `#E3A72F` | Emas kunyit, untuk judul, bingkai, bar HP. |
| Cabai (aksen) | `#B8281F` | Satu-satunya aksen: tombol aksi utama, bar jurus, KO. |
| Krem (netral) | `#F3E6CB` | Teks dan kertas. |

Kontras teks diperiksa dengan `contrast-check.py` dari skill antislop-human, hasilnya di `docs/KONTRAS.txt`.

## Huruf

- **Lora Bold** untuk judul dan nama petarung: serif berkarakter goresan pena, terasa seperti naskah dan prasasti, cocok dengan nuansa budaya.
- **Poppins Medium dan Bold** untuk angka HUD, label tombol, dan teks kecil: terbaca jelas di layar HP kecil.

## Motif identitas

**Kawung** (empat elips saling bertumpuk, pola batik klasik Jawa) dipakai sebagai satu motif berulang: pita judul, bingkai kartu terpilih, dan ikon aplikasi. Dipakai karena ini game budaya Nusantara dan satu motif yang konsisten membuatnya tidak tertukar dengan game tinju lain. Tidak dipakai sebagai latar halaman penuh.

## Alasan keputusan penting (R-31)

- Tombol kontrol 72dp ke atas: ditekan ibu jari saat panik, bukan kursor.
- Bar HP berlabel dan berangka, tidak hanya berwarna, agar pemain buta warna tetap bisa membaca.
- Koin, jamu, dan upgrade tidak punya pembelian uang asli: sesuai pilar "adil tanpa wajib bayar".
- Tidak ada statistik komunitas, ulasan, atau klaim performa di dalam game. Semua angka yang tampil dihitung dari data lokal pemain.
- Mode Bluetooth menampilkan kualitas sambungan dari latensi yang diukur, bukan "kekuatan sinyal" yang tidak bisa dibaca saat sudah tersambung.
- Bahasa Inggris belum dibuat: pengaturan bahasa memberi label "Bahasa Inggris belum tersedia", bukan tombol yang tidak berfungsi.

## Aset

Semua gambar dan suara dibuat dengan skrip di `tools/assets/` (Python, Pillow, NumPy, ffmpeg). Tidak ada aset pihak ketiga selain dua huruf berlisensi SIL OFL 1.1 (lihat `docs/LISENSI-ASET.md`).
