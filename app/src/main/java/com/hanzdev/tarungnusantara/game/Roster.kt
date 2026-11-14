package com.hanzdev.tarungnusantara.game

enum class Headgear { NONE, UDENG, BLANGKON, DAYAK, SIGER, PECI, DETA, FEATHER, TOPENG, KERUDUNG, BUN }
enum class Outfit { PLAIN, BATIK, SARUNG, ULOS, BETANG, GALEMBONG, PAPUA, ACEH, GARUDA, BETAWI }

class Stats(val power: Int, val speed: Int, val defense: Int, val stamina: Int, val counter: Int)

class Strike(
    val frame: Int,
    val damage: Float,
    val reach: Float,
    val pierce: Boolean = false,
    val lunge: Float = 0f,
    val knockdown: Boolean = false
)

class SpecialDef(
    val nama: String,
    val deskripsi: String,
    val total: Int,
    val strikes: List<Strike>,
    val invulStart: Int = -1,
    val invulEnd: Int = -1,
    val counterStance: Boolean = false,
    val swapAt: Int = -1,
    val randomize: Boolean = false
)

data class Look(
    val skin: Int,
    val hair: Int,
    val female: Boolean,
    val head: Headgear,
    val outfit: Outfit,
    val cloth1: Int,
    val cloth2: Int,
    val glove: Int,
    val bulk: Float,
    val sleeves: Boolean = false,
    val headC1: Int = cloth1,
    val headC2: Int = cloth2
)

class FighterDef(
    val id: String,
    val nama: String,
    val daerah: String,
    val gaya: String,
    val stats: Stats,
    val special: SpecialDef,
    val dialogBuka: String,
    val dialogMenang: String,
    val look: Look,
    val caraBuka: String
)

class ArenaDef(
    val id: String,
    val nama: String,
    val daerah: String,
    val suasana: String,
    val ciri: String,
    val musik: String,
    val warnaLantai: Int,
    val warnaTali: Int
)

object Roster {
    private fun c(h: Long): Int = h.toInt()

    val bimo = FighterDef(
        "bimo", "Raden Bimo", "Jawa", "Counter dan sabar",
        Stats(6, 6, 8, 7, 9),
        SpecialDef(
            "Pukulan Balik Gatotkaca",
            "Tangkisan sempurna yang membalas dengan hantaman besar. Hanya berhasil bila lawan memukul saat kamu bersiap.",
            60, listOf(Strike(42, 45f, 190f, pierce = true, lunge = 40f, knockdown = true)), counterStance = true
        ),
        "Tenang. Air yang tenang menghanyutkan.", "Sabar itu juga pukulan.",
        Look(c(0xFFB9835A), c(0xFF1A1210), false, Headgear.BLANGKON, Outfit.BATIK, c(0xFF6B3E22), c(0xFFD8B26B), c(0xFFC2261D), 1.0f),
        "Tersedia dari awal"
    )
    val bonar = FighterDef(
        "bonar", "Bonar Sihombing", "Batak", "Power",
        Stats(10, 4, 6, 6, 5),
        SpecialDef(
            "Hantaman Toba", "Satu pukulan berat yang menembus tangkisan.",
            40, listOf(Strike(20, 40f, 190f, pierce = true, lunge = 55f, knockdown = true))
        ),
        "Horas! Jangan harap kau berdiri lama.", "Kuat itu bukan sombong, itu kenyataan.",
        Look(c(0xFFA9744C), c(0xFF171010), false, Headgear.DETA, Outfit.ULOS, c(0xFF8A2420), c(0xFF171717), c(0xFFC2261D), 1.12f),
        "Tersedia dari awal"
    )
    val rani = FighterDef(
        "rani", "Daeng Rani", "Bugis", "Cepat dan lincah",
        Stats(5, 10, 5, 7, 6),
        SpecialDef(
            "Badai Pinisi", "Rentetan pukulan cepat 6 kali.", 50,
            listOf(
                Strike(10, 6.5f, 160f, lunge = 18f), Strike(15, 6.5f, 160f, lunge = 18f), Strike(20, 6.5f, 160f, lunge = 18f),
                Strike(25, 6.5f, 160f, lunge = 18f), Strike(30, 6.5f, 160f, lunge = 18f), Strike(35, 6.5f, 160f, lunge = 18f, knockdown = true)
            )
        ),
        "Layar sudah terkembang, bersiaplah!", "Ombak tak pernah menunggu.",
        Look(c(0xFFB07A52), c(0xFF1A1210), false, Headgear.DETA, Outfit.SARUNG, c(0xFF8A2B2B), c(0xFFE3A72F), c(0xFFC2261D), 0.95f),
        "Tersedia dari awal"
    )
    val tamang = FighterDef(
        "tamang", "Tamang Bulan", "Dayak", "Tahan banting",
        Stats(7, 5, 9, 10, 5),
        SpecialDef(
            "Amukan Mandau", "Kebal sesaat, lalu serangan beruntun.", 56,
            listOf(Strike(24, 11f, 170f, lunge = 25f), Strike(32, 11f, 170f, lunge = 25f), Strike(40, 16f, 175f, lunge = 30f, knockdown = true)),
            invulStart = 0, invulEnd = 30
        ),
        "Hutan mengajariku bertahan.", "Akar yang dalam tak mudah tumbang.",
        Look(c(0xFFA3714A), c(0xFF171010), false, Headgear.DAYAK, Outfit.BETANG, c(0xFF8A2420), c(0xFF171717), c(0xFFC2261D), 1.08f),
        "Tersedia dari awal"
    )
    val sari = FighterDef(
        "sari", "Ni Luh Sari", "Bali", "Kombo dan irama",
        Stats(6, 8, 6, 6, 7),
        SpecialDef(
            "Tarian Barong", "Kombo 5 pukulan dengan gerak menari.", 54,
            listOf(
                Strike(12, 8f, 165f, lunge = 14f), Strike(18, 8f, 165f, lunge = 14f), Strike(24, 8f, 165f, lunge = 14f),
                Strike(30, 8f, 165f, lunge = 14f), Strike(38, 8f, 170f, lunge = 16f, knockdown = true)
            )
        ),
        "Ikuti iramaku, kalau sanggup.", "Gerak yang indah selalu mengena.",
        Look(c(0xFFC48A5E), c(0xFF171010), true, Headgear.BUN, Outfit.SARUNG, c(0xFF171717), c(0xFFF3E6CB), c(0xFFC2261D), 0.92f),
        "Tersedia dari awal"
    )
    val malin = FighterDef(
        "malin", "Sutan Malin", "Minang", "Seimbang, ahli silek",
        Stats(7, 7, 7, 7, 7),
        SpecialDef(
            "Gelombang Silek", "Mengelak lalu menyerang dari samping.", 42,
            listOf(Strike(24, 38f, 175f, lunge = 20f, knockdown = true)),
            invulStart = 4, invulEnd = 20, swapAt = 14
        ),
        "Alam takambang jadi guru.", "Ilmu itu dipakai, bukan dipamerkan.",
        Look(c(0xFFBA845C), c(0xFF171010), false, Headgear.DETA, Outfit.GALEMBONG, c(0xFF171717), c(0xFFE3A72F), c(0xFFC2261D), 1.0f),
        "Tersedia dari awal"
    )
    val jaelani = FighterDef(
        "jaelani", "Bang Jaelani", "Betawi", "Nekat dan spontan, banyak serangan acak",
        Stats(7, 7, 4, 6, 4),
        SpecialDef(
            "Pukulan Ondel-Ondel", "Lima pukulan beruntun dengan kekuatan acak.", 50,
            listOf(
                Strike(8, 8.5f, 160f, lunge = 14f), Strike(14, 8.5f, 160f, lunge = 14f), Strike(20, 8.5f, 160f, lunge = 14f),
                Strike(26, 8.5f, 160f, lunge = 14f), Strike(32, 8.5f, 165f, lunge = 14f, knockdown = true)
            ),
            randomize = true
        ),
        "Ayo, jangan sok jago di kampung orang!", "Nekat itu kadang jadi strategi.",
        Look(c(0xFFB47C54), c(0xFF171010), false, Headgear.PECI, Outfit.BETAWI, c(0xFF2F5D8A), c(0xFFF3E6CB), c(0xFFC2261D), 1.0f),
        "Selesaikan Bab 2"
    )
    val yance = FighterDef(
        "yance", "Yance Wenda", "Papua", "Cepat dan kuat, stamina tinggi",
        Stats(8, 8, 5, 9, 4),
        SpecialDef(
            "Serbuan Cenderawasih", "Tiga terjangan berat beruntun.", 50,
            listOf(Strike(12, 12f, 180f, lunge = 40f), Strike(24, 14f, 180f, lunge = 40f), Strike(36, 16f, 185f, lunge = 40f, knockdown = true))
        ),
        "Dari timur, kami datang membawa badai.", "Gunung tidak pernah mengeluh.",
        Look(c(0xFF5E3B2A), c(0xFF120C0A), false, Headgear.FEATHER, Outfit.PAPUA, c(0xFFB8281F), c(0xFFF3E6CB), c(0xFFC2261D), 1.06f),
        "Selesaikan Bab 4"
    )
    val zahra = FighterDef(
        "zahra", "Cut Zahra", "Aceh", "Presisi, serangan tepat sasaran",
        Stats(6, 7, 6, 7, 8),
        SpecialDef(
            "Rencong Presisi", "Dua tusukan tepat yang menembus tangkisan.", 40,
            listOf(Strike(16, 18f, 210f, pierce = true, lunge = 30f), Strike(26, 20f, 215f, pierce = true, lunge = 30f, knockdown = true))
        ),
        "Satu pukulan, satu sasaran.", "Tepat itu lebih tajam dari keras.",
        Look(c(0xFFC79A72), c(0xFF171010), true, Headgear.KERUDUNG, Outfit.ACEH, c(0xFF8A2420), c(0xFFE3A72F), c(0xFFC2261D), 0.92f, sleeves = true),
        "Selesaikan Bab 5"
    )
    val garuda = FighterDef(
        "garuda", "Sang Garuda Bertopeng", "Misterius", "Menguasai semua gaya",
        Stats(8, 8, 8, 8, 8),
        SpecialDef(
            "Murka Garuda", "Empat serangan beruntun, yang terakhir menembus tangkisan.", 56,
            listOf(
                Strike(12, 8f, 170f, lunge = 20f), Strike(22, 10f, 170f, lunge = 20f),
                Strike(32, 12f, 175f, lunge = 22f), Strike(44, 15f, 190f, pierce = true, lunge = 30f, knockdown = true)
            )
        ),
        "Tunjukkan bahwa kau layak.", "Sayap baru selalu lahir dari yang lama.",
        Look(c(0xFF8A5A3A), c(0xFF171010), false, Headgear.TOPENG, Outfit.GARUDA, c(0xFFB8281F), c(0xFFE3A72F), c(0xFFE3A72F), 1.05f),
        "Selesaikan Bab 6"
    )

    val all: List<FighterDef> = listOf(bimo, bonar, rani, tamang, sari, malin, jaelani, yance, zahra, garuda)
    val starters: List<FighterDef> = all.subList(0, 6)

    private val shortNames = mapOf(
        "bimo" to "Bimo", "bonar" to "Bonar", "rani" to "Rani", "tamang" to "Tamang", "sari" to "Sari",
        "malin" to "Malin", "jaelani" to "Jaelani", "yance" to "Yance", "zahra" to "Zahra", "garuda" to "Garuda"
    )

    fun shortName(d: FighterDef): String = shortNames[d.id] ?: d.nama

    fun byId(id: String): FighterDef = all.firstOrNull { it.id == id } ?: bimo

    val arenas: List<ArenaDef> = listOf(
        ArenaDef("keraton", "Pelataran Keraton", "Jawa", "Sore keemasan, gapura batu", "Obor menyala di sudut ring", "m_keraton.ogg", 0xFF4A2C1E.toInt(), 0xFFD8B26B.toInt()),
        ArenaDef("toba", "Tepi Danau Toba", "Sumatera Utara", "Pagi berkabut, perahu di kejauhan", "Air berkilau di latar", "m_toba.ogg", 0xFF3E4440.toInt(), 0xFFE8E0CC.toInt()),
        ArenaDef("makassar", "Pelabuhan Makassar", "Sulawesi Selatan", "Senja, kapal pinisi", "Bendera berkibar", "m_makassar.ogg", 0xFF3A2622.toInt(), 0xFFF0B070.toInt()),
        ArenaDef("betang", "Rumah Betang", "Kalimantan", "Malam, api unggun", "Ukiran kayu di dinding", "m_betang.ogg", 0xFF2B1A10.toInt(), 0xFFE08A3A.toInt()),
        ArenaDef("bali", "Pura di Tengah Sawah", "Bali", "Siang cerah, sawah hijau", "Penonton membawa penjor", "m_bali.ogg", 0xFF6B6A55.toInt(), 0xFFF3E6CB.toInt()),
        ArenaDef("garuda", "Stadion Garuda", "Final Nasional", "Malam, lampu sorot", "Penonton ramai, bendera merah putih", "m_garuda.ogg", 0xFF202A44.toInt(), 0xFFB8281F.toInt())
    )

    fun arenaById(id: String): ArenaDef = arenas.firstOrNull { it.id == id } ?: arenas[0]
}
