# Tarung Nusantara

*Satu Sarung Tinju, Seribu Kisah Nusantara*

Naskah Game Lengkap (Game Design Document) · Platform: Android · Genre: Tinju 2D · Target: ringan, offline, mabar Bluetooth

## 1. Ringkasan Game

**Tarung Nusantara** adalah game tinju 2D untuk Android dengan petarung dari berbagai daerah Indonesia. Pemain memilih petinju, berlatih, bertanding di turnamen antardaerah, dan mengejar gelar **Juara Nusantara**. Game ini bisa dimainkan sepenuhnya tanpa internet, dan bisa diadu langsung dengan teman lewat **Bluetooth**.

| Item | Keterangan |
| --- | --- |
| Nama | Tarung Nusantara |
| Platform | Android 7.0 ke atas |
| Orientasi | Landscape |
| Ukuran APK | Di bawah 30 MB |
| Spesifikasi minimum | RAM 2 GB, 60 FPS stabil |
| Koneksi | Offline penuh, mabar via Bluetooth |
| Iklan | Tidak ada iklan yang mengganggu |
| Bahasa | Indonesia (bisa ditambah Inggris) |
| Pemain | 1 pemain atau 2 pemain (Bluetooth) |

**Pilar desain:** (1) ringan di semua HP, (2) kontrol sederhana tapi pertarungan berisi, (3) budaya Nusantara terasa di setiap detail, (4) adil tanpa wajib bayar.

## 2. Cerita dan Dunia

### Prolog

Setiap lima tahun, para petarung terbaik dari seluruh penjuru negeri berkumpul dalam turnamen **Tarung Nusantara**. Pemenangnya digelari **Juara Nusantara**, kehormatan tertinggi bagi seorang petinju. Tahun ini, seorang petarung muda dari kampung kecil menerima undangan yang tak ia sangka. Namanya belum dikenal, tetapi tekadnya sudah bulat.

> *"Aku tidak datang untuk menjadi terkenal. Aku datang untuk membawa nama kampungku ke atas ring."*

### Struktur Cerita (Mode Karier)

Pemain menempuh 6 bab. Tiap bab berisi 3 pertandingan dan 1 pertandingan penentu (bos daerah).

| Bab | Daerah | Arena | Lawan Penentu |
| --- | --- | --- | --- |
| 1 | Jawa | Pelataran Keraton | Raden Bimo |
| 2 | Sumatera Utara | Tepi Danau Toba | Bonar Sihombing |
| 3 | Sulawesi Selatan | Pelabuhan Makassar | Daeng Rani |
| 4 | Kalimantan | Rumah Betang | Tamang Bulan |
| 5 | Bali | Pura di Tengah Sawah | Ni Luh Sari |
| 6 | Final Nasional | Stadion Garuda | Sang Garuda Bertopeng |

### Alur Singkat

- **Bab 1:** Pemain tiba di Jawa. Raden Bimo, petarung tenang, mengajarkan arti sabar dan menunggu celah.
- **Bab 2:** Di Toba, Bonar menunjukkan bahwa kekuatan tanpa kendali bisa dikalahkan oleh ketepatan.
- **Bab 3:** Di Makassar, Daeng Rani menguji kecepatan dan keberanian pemain di tengah angin pelabuhan.
- **Bab 4:** Di Kalimantan, Tamang Bulan mengajarkan ketahanan dan keteguhan.
- **Bab 5:** Di Bali, Ni Luh Sari menunjukkan keindahan gerak dan irama dalam bertarung.
- **Bab 6:** Final melawan **Sang Garuda Bertopeng**, juara bertahan yang identitasnya misterius. Setelah menang, topengnya dibuka: ternyata ia adalah petarung tua yang rindu bertemu penantang sejati.

### Epilog

> *"Gelar ini bukan milikmu seorang. Ini milik setiap daerah yang kau bawa di dalam tinjumu."*

## 3. Karakter

Setiap petarung punya 5 statistik (skala 1 sampai 10): **Kekuatan, Kecepatan, Pertahanan, Stamina, Counter**.

### Petarung Awal (Terbuka dari Awal)

**1. Raden Bimo (Jawa)**

- Gaya: Counter dan sabar
- Statistik: Kekuatan 6, Kecepatan 6, Pertahanan 8, Stamina 7, Counter 9
- Jurus Spesial: **Pukulan Balik Gatotkaca**, tangkisan sempurna yang membalas dengan hantaman besar
- Dialog pembuka: *"Tenang. Air yang tenang menghanyutkan."*
- Dialog menang: *"Sabar itu juga pukulan."*

**2. Bonar Sihombing (Batak)**

- Gaya: Power
- Statistik: Kekuatan 10, Kecepatan 4, Pertahanan 6, Stamina 6, Counter 5
- Jurus Spesial: **Hantaman Toba**, satu pukulan berat yang menembus tangkisan
- Dialog pembuka: *"Horas! Jangan harap kau berdiri lama."*
- Dialog menang: *"Kuat itu bukan sombong, itu kenyataan."*

**3. Daeng Rani (Bugis)**

- Gaya: Cepat dan lincah
- Statistik: Kekuatan 5, Kecepatan 10, Pertahanan 5, Stamina 7, Counter 6
- Jurus Spesial: **Badai Pinisi**, rentetan pukulan cepat 6 kali
- Dialog pembuka: *"Layar sudah terkembang, bersiaplah!"*
- Dialog menang: *"Ombak tak pernah menunggu."*

**4. Tamang Bulan (Dayak)**

- Gaya: Tahan banting
- Statistik: Kekuatan 7, Kecepatan 5, Pertahanan 9, Stamina 10, Counter 5
- Jurus Spesial: **Amukan Mandau**, kebal sesaat lalu serangan beruntun
- Dialog pembuka: *"Hutan mengajariku bertahan."*
- Dialog menang: *"Akar yang dalam tak mudah tumbang."*

**5. Ni Luh Sari (Bali)**

- Gaya: Kombo dan irama
- Statistik: Kekuatan 6, Kecepatan 8, Pertahanan 6, Stamina 6, Counter 7
- Jurus Spesial: **Tarian Barong**, kombo 5 pukulan dengan gerak menari
- Dialog pembuka: *"Ikuti iramaku, kalau sanggup."*
- Dialog menang: *"Gerak yang indah selalu mengena."*

**6. Sutan Malin (Minang)**

- Gaya: Seimbang, ahli silek
- Statistik: Kekuatan 7, Kecepatan 7, Pertahanan 7, Stamina 7, Counter 7
- Jurus Spesial: **Gelombang Silek**, gerakan mengelak lalu menyerang dari samping
- Dialog pembuka: *"Alam takambang jadi guru."*
- Dialog menang: *"Ilmu itu dipakai, bukan dipamerkan."*

### Petarung yang Bisa Dibuka

| Petarung | Daerah | Cara Membuka | Gaya |
| --- | --- | --- | --- |
| Bang Jaelani | Betawi | Selesaikan Bab 2 | Nekat dan spontan, banyak serangan acak |
| Yance Wenda | Papua | Selesaikan Bab 4 | Cepat dan kuat, stamina tinggi |
| Cut Zahra | Aceh | Selesaikan Bab 5 | Presisi, serangan tepat sasaran |
| Sang Garuda Bertopeng | Misterius | Selesaikan Bab 6 | Menguasai semua gaya, statistik 8 semua |

### Karakter Pendukung

- **Pak Guru Wiryo:** pelatih pemain, memandu tutorial.
- **Ibu Lastri:** penjual jamu di tiap arena, memberi bonus kecil.
- **Komentator Bung Tomo Jr.:** suara komentator yang menyoroti jalannya pertandingan.

## 4. Sistem Pertarungan

### Kontrol

- **Kiri layar:** joystick atau tombol geser (maju, mundur, tunduk)
- **Kanan layar:** 4 tombol: **Jab, Hook, Uppercut, Tangkis**
- **Tombol Jurus:** muncul saat meteran jurus penuh
- **Hindar:** geser cepat dua kali ke kiri atau kanan

### Sistem Angka

| Aksi | Damage | Stamina | Kecepatan |
| --- | --- | --- | --- |
| Jab | 5 | 4 | Sangat cepat |
| Hook | 10 | 8 | Sedang |
| Uppercut | 15 | 12 | Lambat |
| Tangkis | 0 (kurangi 70% damage) | 3 per tangkisan | Instan |
| Tangkis sempurna (0,2 detik pertama) | 0 (batalkan damage + celah lawan) | 0 | Instan |
| Hindar | 0 | 6 | Cepat |
| Jurus Spesial | 35 hingga 45 | Gratis | Bervariasi |

- **HP:** 100 (dapat ditingkatkan lewat upgrade)
- **Stamina:** 100, pulih 10 per detik saat tidak menyerang
- **Meteran Jurus:** terisi 8% saat memukul, 5% saat terkena pukul, 15% saat tangkisan sempurna
- **Pingsan (Dizzy):** terkena 3 pukulan berat beruntun membuat lawan limbung 1,5 detik

### Aturan Pertandingan

- 3 ronde, tiap ronde 60 detik (bisa diubah di Duel Cepat: 1, 3, atau 5 ronde)
- **KO:** HP lawan habis
- **TKO:** lawan jatuh 3 kali dalam satu ronde
- **Poin:** bila waktu habis, pemenang ditentukan dari total damage bersih
- Antar-ronde: jeda 10 detik, HP pulih 15%

### Kecerdasan Buatan (AI)

| Tingkat | Perilaku |
| --- | --- |
| Mudah | Menyerang acak, jarang menangkis |
| Sedang | Menangkis pola dasar, membalas serangan |
| Sulit | Membaca kebiasaan pemain, memakai counter dan jurus |
| Legenda | Sulit ditebak, reaksi sangat cepat, tetapi tetap bisa dikalahkan |

AI mencatat 3 serangan terakhir pemain untuk menyesuaikan pertahanannya agar pertandingan tetap terasa adil.

## 5. Mode Permainan

### Mode Karier

- 6 bab, masing-masing 3 laga biasa dan 1 laga penentu
- Hadiah koin tiap laga, bonus bila menang KO
- Cerita singkat dan dialog sebelum tiap bab

### Mode Arcade

- Lawan beruntun tanpa henti, 1 nyawa
- Tiap 5 kemenangan, lawan naik satu tingkat kesulitan
- Skor tertinggi disimpan di perangkat

### Mode Duel Cepat

- Pilih petarung, arena, tingkat AI, dan jumlah ronde
- Cocok untuk latihan atau bermain santai

### Mode Latihan

- Tutorial dipandu Pak Guru Wiryo
- Kantong tinju untuk berlatih kombo
- Menampilkan statistik pukulan dan akurasi

### Mode Mabar Bluetooth

**Tanpa server dan tanpa internet.** Dua HP terhubung langsung lewat Bluetooth.

**Alur penggunaan:**

1. Pemain A memilih **Buat Ruangan** (menjadi host)
2. Pemain B memilih **Cari Ruangan** dan memilih perangkat A
3. Keduanya memilih petarung dan arena
4. Pertandingan dimulai setelah kedua pemain menekan **Siap**

**Aturan teknis:**

- Host menghitung hasil pertarungan, client hanya mengirim input
- Input dikirim sekitar 20 kali per detik (data sangat kecil)
- Bluetooth Classic (RFCOMM) untuk kestabilan
- Izin Android 12 ke atas: `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE`

**Fitur:**

- Pilihan 1, 3, atau 5 ronde
- Tombol **Rematch** setelah pertandingan
- Indikator kekuatan sinyal
- Sambung ulang otomatis bila koneksi putus kurang dari 5 detik
- Papan skor menang-kalah antar dua pemain selama sesi

## 6. Arena

| Arena | Suasana | Musik | Ciri Khas |
| --- | --- | --- | --- |
| Pelataran Keraton (Jawa) | Sore keemasan, gapura batu | Gamelan pelan | Obor menyala di sudut ring |
| Tepi Danau Toba | Pagi berkabut, perahu di kejauhan | Gondang dan suling | Air berkilau di latar |
| Pelabuhan Makassar | Senja, kapal pinisi | Kecapi dan gendang cepat | Bendera berkibar |
| Rumah Betang (Kalimantan) | Malam, api unggun | Sape dan gong | Ukiran kayu di dinding |
| Pura di Sawah (Bali) | Siang cerah, sawah hijau | Gamelan Bali ceria | Penonton membawa penjor |
| Stadion Garuda (Final) | Malam, lampu sorot | Orkestra Nusantara | Penonton ramai, bendera merah putih |

Setiap arena punya penonton sederhana (sprite berulang) agar tetap ringan.

## 7. Toko, Koin, dan Upgrade

### Mata Uang

- **Koin:** didapat dari bermain. Tidak ada pembelian dengan uang asli yang wajib.

### Cara Mendapat Koin

| Aksi | Koin |
| --- | --- |
| Menang laga Karier | 100 |
| Menang KO | +50 |
| Menang tanpa kena pukul | +100 |
| Laga penentu bab | 300 |
| Arcade per 5 kemenangan | 150 |
| Hadiah harian | 50 |

### Isi Toko

**Sarung Tinju:** Merah Klasik (gratis), Biru Samudra (200), Emas Nusantara (800), Batik Parang (1.200).

**Ikat Kepala dan Aksesori:** Udeng Bali (300), Blangkon Jawa (300), Ikat Kepala Dayak (400), Siger Lampung (500).

**Kostum Adat:** Celana Batik, Sarung Bugis, Rompi Ulos, Cawat Betang (masing-masing 600 hingga 900).

**Upgrade Statistik (maksimal 5 level):** HP +5 per level, Stamina +5 per level, Kekuatan +1 per level, Kecepatan +1 per level. Harga naik tiap level (150, 300, 500, 800, 1.200).

**Jamu (item sekali pakai):** Jamu Kunyit (pulihkan sedikit HP di awal ronde), Jamu Beras Kencur (stamina pulih lebih cepat), Jamu Temulawak (meteran jurus mulai 30%).

## 8. Pencapaian

| Pencapaian | Syarat |
| --- | --- |
| Pukulan Pertama | Menang pertandingan pertama |
| Tangan Besi | Menang 10 kali dengan KO |
| Tak Tersentuh | Menang tanpa terkena pukulan |
| Raja Counter | 20 tangkisan sempurna dalam satu pertandingan |
| Penjelajah Nusantara | Menamatkan semua 6 bab |
| Juara Nusantara | Mengalahkan Sang Garuda Bertopeng |
| Kolektor | Membuka semua petarung |
| Tahan Banting | Menang dengan HP di bawah 5% |
| Sahabat Mabar | Bermain 10 kali lewat Bluetooth |
| Legenda | Menang 100 pertandingan |

## 9. Layar dan Menu

1. **Layar Pembuka:** logo, musik gamelan pendek
2. **Menu Utama:** Karier, Arcade, Duel Cepat, Mabar Bluetooth, Latihan, Toko, Pencapaian, Pengaturan
3. **Pilih Petarung:** tampilan karakter, statistik, dan jurus
4. **Pilih Arena**
5. **Layar Pertandingan:** bar HP dan stamina di atas, meteran jurus di bawah, tombol kontrol di sisi
6. **Layar Hasil:** pemenang, koin didapat, statistik pukulan
7. **Toko dan Garasi Kostum**
8. **Pengaturan:** suara, musik, getar, tingkat kesulitan, posisi tombol, bahasa

## 10. Audio

- **Musik:** setiap arena punya satu lagu daerah berbentuk loop pendek (1 sampai 2 menit), format OGG
- **Efek suara:** pukulan ringan, pukulan berat, tangkisan, KO, bel ronde, sorak penonton
- **Komentator:** kalimat singkat, misalnya *"Pukulan telak!"*, *"Dia limbung!"*, *"Luar biasa!"*
- **Bel ronde:** gong kecil khas Nusantara

## 11. Teks dan Dialog dalam Game

### Tutorial (Pak Guru Wiryo)

- *"Selamat datang, petarung muda. Mari kita mulai dari dasar."*
- *"Tekan Jab untuk pukulan cepat. Lalu ulangi dengan irama."*
- *"Tangkis tepat saat lawan memukul, itu namanya tangkisan sempurna."*
- *"Lihat meteran jurus? Kalau penuh, lepaskan jurus spesialmu!"*

### Komentator

- Awal ronde: *"Ronde pertama dimulai! Siapa yang akan unggul?"*
- Pukulan telak: *"Kena! Itu pukulan yang keras sekali!"*
- Tangkisan sempurna: *"Pertahanan yang memukau!"*
- KO: *"KO! Pertandingan selesai!"*
- Menang poin: *"Keputusan juri! Pemenangnya adalah..."*

### Pesan Sistem

- *"Stamina habis! Atur napasmu."*
- *"Meteran jurus penuh!"*
- *"Koneksi terputus, mencoba tersambung kembali..."*
- *"Lawan siap. Mulai pertandingan?"*
- *"Koin tidak cukup."*

### Teks Layar Loading (acak)

- *"Jab menghemat tenaga, Uppercut menguras tenaga."*
- *"Tangkis di saat yang tepat membuka celah lawan."*
- *"Setiap daerah punya gaya bertarung sendiri."*
- *"Jamu menambah semangat sebelum bertanding."*

## 12. Spesifikasi Teknis

- **Bahasa dan mesin:** Kotlin dengan libGDX, atau Godot. Alternatif sederhana: Kotlin dengan Canvas/SurfaceView
- **Grafis:** sprite 2D, satu atlas per karakter, format WebP
- **Audio:** OGG, pemutaran efek memakai SoundPool
- **Penyimpanan:** SharedPreferences untuk koin, kemajuan, dan pengaturan
- **Target performa:** 60 FPS, penggunaan RAM di bawah 200 MB
- **Ukuran APK:** di bawah 30 MB
- **Optimasi:** objek dipakai ulang (object pooling), tanpa alokasi saat bermain, tekstur dikompres
- **Keamanan data:** data lokal tanpa informasi pribadi

### Struktur Kode (saran)

```
com.hanzdev.tarungnusantara/
  MainActivity
  game/
    GameActivity
    Fighter
    Combat
    AIController
    InputHandler
  bluetooth/
    BluetoothManager
    Lobby
    SyncProtocol
  data/
    SaveData
    Shop
    Achievements
  ui/
    Menu
    CharacterSelect
    Hud
  audio/
    SoundManager
```

## 13. Rencana Pengembangan

| Tahap | Isi | Perkiraan |
| --- | --- | --- |
| 1 | Prototipe: 2 petarung, 1 arena, pertarungan dasar | 1 sampai 2 minggu |
| 2 | 6 petarung, AI, 3 arena, HUD lengkap | 2 sampai 3 minggu |
| 3 | Mode Karier, Arcade, Toko, Pencapaian | 2 minggu |
| 4 | Mabar Bluetooth | 2 minggu |
| 5 | Polesan: suara, efek, uji coba, optimasi | 1 sampai 2 minggu |
| 6 | Rilis dan perbaikan berdasarkan masukan | Berkelanjutan |

## 14. Penutup

> *Ini bukan sekadar pertarungan. Ini tentang siapa yang paling layak membawa nama daerahnya ke puncak.*
>
> **Pilih petarungmu. Naikkan sarung tinjumu. Tarung Nusantara dimulai.**
