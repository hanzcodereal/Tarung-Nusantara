package com.hanzdev.tarungnusantara.data

import com.hanzdev.tarungnusantara.game.Roster

class Ach(val id: String, val nama: String, val syarat: String)

class MatchSummary(
    val won: Boolean,
    val byKo: Boolean,
    val hitsTaken: Int,
    val perfects: Int,
    val hpFraction: Float,
    val bluetooth: Boolean,
    val beatGarudaFinal: Boolean
)

object Achievements {
    val list = listOf(
        Ach("pertama", "Pukulan Pertama", "Menang pertandingan pertama"),
        Ach("tangan_besi", "Tangan Besi", "Menang 10 kali dengan KO"),
        Ach("tak_tersentuh", "Tak Tersentuh", "Menang tanpa terkena pukulan"),
        Ach("raja_counter", "Raja Counter", "20 tangkisan sempurna dalam satu pertandingan"),
        Ach("penjelajah", "Penjelajah Nusantara", "Menamatkan semua 6 bab"),
        Ach("juara", "Juara Nusantara", "Mengalahkan Sang Garuda Bertopeng"),
        Ach("kolektor", "Kolektor", "Membuka semua petarung"),
        Ach("tahan_banting", "Tahan Banting", "Menang dengan HP di bawah 5%"),
        Ach("sahabat_mabar", "Sahabat Mabar", "Bermain 10 kali lewat Bluetooth"),
        Ach("legenda", "Legenda", "Menang 100 pertandingan")
    )

    /** Progres angka untuk pencapaian yang bisa dihitung, atau null. */
    fun progress(id: String): Pair<Int, Int>? = when (id) {
        "tangan_besi" -> Pair(Save.koWins.coerceAtMost(10), 10)
        "sahabat_mabar" -> Pair(Save.btMatches.coerceAtMost(10), 10)
        "legenda" -> Pair(Save.totalWins.coerceAtMost(100), 100)
        "penjelajah" -> Pair(Save.chaptersDone, 6)
        "kolektor" -> Pair(Save.unlockedFighters().size, Roster.all.size)
        else -> null
    }

    private fun grant(id: String, out: MutableList<Ach>) {
        if (Save.hasAchievement(id)) return
        Save.addAchievement(id)
        list.firstOrNull { it.id == id }?.let { out.add(it) }
    }

    fun check(m: MatchSummary): List<Ach> {
        val out = ArrayList<Ach>()
        if (m.won) {
            grant("pertama", out)
            if (m.hitsTaken == 0) grant("tak_tersentuh", out)
            if (m.hpFraction < 0.05f) grant("tahan_banting", out)
            if (m.beatGarudaFinal) grant("juara", out)
        }
        if (m.perfects >= 20) grant("raja_counter", out)
        if (Save.koWins >= 10) grant("tangan_besi", out)
        if (Save.chaptersDone >= 6) grant("penjelajah", out)
        if (Save.unlockedFighters().size >= Roster.all.size) grant("kolektor", out)
        if (Save.btMatches >= 10) grant("sahabat_mabar", out)
        if (Save.totalWins >= 100) grant("legenda", out)
        return out
    }
}
