package com.hanzdev.tarungnusantara.data

import com.hanzdev.tarungnusantara.game.Level

class Chapter(
    val nomor: Int,
    val daerah: String,
    val arenaId: String,
    val arena: String,
    val bossId: String,
    val boss: String,
    val cerita: String,
    val levelAwal: Int
)

object Teks {
    const val VERSI = "1.0.0"
    const val DEVELOPER = "Hanz Dev Official"
    const val EMAIL = "hanzzcodee@gmail.com"
    const val DUKUNG_URL = "https://saweria.co/hanzreally"
    const val TAGLINE = "Satu Sarung Tinju, Seribu Kisah Nusantara"

    const val PROLOG = "Setiap lima tahun, para petarung terbaik dari seluruh penjuru negeri berkumpul dalam turnamen Tarung Nusantara. " +
        "Pemenangnya digelari Juara Nusantara, kehormatan tertinggi bagi seorang petinju. Tahun ini, seorang petarung muda dari kampung kecil " +
        "menerima undangan yang tak ia sangka. Namanya belum dikenal, tetapi tekadnya sudah bulat."
    const val PROLOG_KUTIPAN = "Aku tidak datang untuk menjadi terkenal. Aku datang untuk membawa nama kampungku ke atas ring."
    const val EPILOG = "Gelar ini bukan milikmu seorang. Ini milik setiap daerah yang kau bawa di dalam tinjumu."

    val chapters = listOf(
        Chapter(1, "Jawa", "keraton", "Pelataran Keraton", "bimo", "Raden Bimo",
            "Kamu tiba di Jawa. Raden Bimo, petarung yang tenang, mengajarkan arti sabar dan menunggu celah.", Level.MUDAH),
        Chapter(2, "Sumatera Utara", "toba", "Tepi Danau Toba", "bonar", "Bonar Sihombing",
            "Di Toba, Bonar menunjukkan bahwa kekuatan tanpa kendali bisa dikalahkan oleh ketepatan.", Level.MUDAH),
        Chapter(3, "Sulawesi Selatan", "makassar", "Pelabuhan Makassar", "rani", "Daeng Rani",
            "Di Makassar, Daeng Rani menguji kecepatan dan keberanianmu di tengah angin pelabuhan.", Level.SEDANG),
        Chapter(4, "Kalimantan", "betang", "Rumah Betang", "tamang", "Tamang Bulan",
            "Di Kalimantan, Tamang Bulan mengajarkan ketahanan dan keteguhan.", Level.SEDANG),
        Chapter(5, "Bali", "bali", "Pura di Tengah Sawah", "sari", "Ni Luh Sari",
            "Di Bali, Ni Luh Sari menunjukkan keindahan gerak dan irama dalam bertarung.", Level.SULIT),
        Chapter(6, "Final Nasional", "garuda", "Stadion Garuda", "garuda", "Sang Garuda Bertopeng",
            "Final melawan Sang Garuda Bertopeng, juara bertahan yang identitasnya misterius.", Level.SULIT)
    )

    val tips = listOf(
        "Jab menghemat tenaga, Uppercut menguras tenaga.",
        "Tangkis di saat yang tepat membuka celah lawan.",
        "Setiap daerah punya gaya bertarung sendiri.",
        "Jamu menambah semangat sebelum bertanding."
    )

    val tutorial = listOf(
        "Selamat datang, petarung muda. Mari kita mulai dari dasar.",
        "Tekan Jab untuk pukulan cepat. Lalu ulangi dengan irama.",
        "Hook lebih berat dari Jab, Uppercut paling berat. Tunduk menghindari Jab dan Hook.",
        "Tangkis tepat saat lawan memukul, itu namanya tangkisan sempurna.",
        "Geser cepat dua kali ke kiri atau kanan untuk Hindar.",
        "Lihat meteran jurus? Kalau penuh, lepaskan jurus spesialmu!"
    )

    val komentar = hashMapOf(
        30 to "Ronde dimulai! Siapa yang akan unggul?",
        31 to "Kena! Itu pukulan yang keras sekali!",
        32 to "Pertahanan yang memukau!",
        33 to "KO! Pertandingan selesai!",
        34 to "Keputusan juri! Pemenangnya adalah...",
        35 to "Dia limbung!",
        36 to "Luar biasa!"
    )

    const val STAMINA_HABIS = "Stamina habis! Atur napasmu."
    const val JURUS_PENUH = "Meteran jurus penuh!"
    const val LAWAN_SIAP = "Lawan siap. Mulai pertandingan?"
    const val KONEKSI_PUTUS = "Koneksi terputus, mencoba tersambung kembali..."
    const val KOIN_KURANG = "Koin tidak cukup."
}
