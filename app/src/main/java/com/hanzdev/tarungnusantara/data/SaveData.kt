package com.hanzdev.tarungnusantara.data

import android.content.Context
import android.content.SharedPreferences

object Save {
    const val VERSION = 1
    private lateinit var sp: SharedPreferences

    fun init(ctx: Context) {
        if (::sp.isInitialized) return
        sp = ctx.applicationContext.getSharedPreferences("tarung_nusantara", Context.MODE_PRIVATE)
        migrate()
    }

    private fun migrate() {
        val v = sp.getInt("version", 0)
        if (v < 1) sp.edit().putInt("version", VERSION).apply()
    }

    private fun int(k: String, d: Int) = sp.getInt(k, d)
    private fun put(k: String, v: Int) = sp.edit().putInt(k, v).apply()
    private fun bool(k: String, d: Boolean) = sp.getBoolean(k, d)
    private fun put(k: String, v: Boolean) = sp.edit().putBoolean(k, v).apply()

    var coins: Int
        get() = int("coins", 0)
        set(v) = put("coins", v.coerceAtLeast(0))

    var sfxOn: Boolean
        get() = bool("sfxOn", true)
        set(v) = put("sfxOn", v)
    var sfxVol: Int
        get() = int("sfxVol", 80)
        set(v) = put("sfxVol", v)
    var musicOn: Boolean
        get() = bool("musicOn", true)
        set(v) = put("musicOn", v)
    var musicVol: Int
        get() = int("musicVol", 70)
        set(v) = put("musicVol", v)
    var haptic: Boolean
        get() = bool("haptic", true)
        set(v) = put("haptic", v)
    var aiDefault: Int
        get() = int("aiDefault", 1)
        set(v) = put("aiDefault", v)
    var buttonsLeft: Boolean
        get() = bool("buttonsLeft", false)
        set(v) = put("buttonsLeft", v)
    var reduceMotion: Boolean
        get() = bool("reduceMotion", false)
        set(v) = put("reduceMotion", v)
    var commentaryVoice: Boolean
        get() = bool("commentaryVoice", true)
        set(v) = put("commentaryVoice", v)

    var lastFighter: String
        get() = sp.getString("lastFighter", "bimo") ?: "bimo"
        set(v) = sp.edit().putString("lastFighter", v).apply()

    var chaptersDone: Int
        get() = int("chaptersDone", 0)
        set(v) = put("chaptersDone", v.coerceIn(0, 6))
    var careerMatch: Int
        get() = int("careerMatch", 0)
        set(v) = put("careerMatch", v.coerceIn(0, 3))
    var bestArcade: Int
        get() = int("bestArcade", 0)
        set(v) = put("bestArcade", v)

    var totalWins: Int
        get() = int("totalWins", 0)
        set(v) = put("totalWins", v)
    var koWins: Int
        get() = int("koWins", 0)
        set(v) = put("koWins", v)
    var btMatches: Int
        get() = int("btMatches", 0)
        set(v) = put("btMatches", v)

    var lastDailyDay: Long
        get() = sp.getLong("lastDailyDay", -1L)
        set(v) = sp.edit().putLong("lastDailyDay", v).apply()

    fun upgrade(stat: String): Int = int("up_$stat", 0)
    fun setUpgrade(stat: String, level: Int) = put("up_$stat", level.coerceIn(0, 5))

    private fun strSet(k: String): MutableSet<String> = HashSet(sp.getStringSet(k, emptySet()) ?: emptySet())
    private fun putSet(k: String, v: Set<String>) = sp.edit().putStringSet(k, v).apply()

    fun owns(itemId: String): Boolean = itemId == "glove_merah" || strSet("owned").contains(itemId)
    fun addOwned(itemId: String) { val s = strSet("owned"); s.add(itemId); putSet("owned", s) }

    fun equipped(fighterId: String, slot: String): String = sp.getString("eq_${fighterId}_$slot", "") ?: ""
    fun equip(fighterId: String, slot: String, itemId: String) {
        sp.edit().putString("eq_${fighterId}_$slot", itemId).apply()
    }

    fun jamu(id: String): Int = int("jamu_$id", 0)
    fun setJamu(id: String, n: Int) = put("jamu_$id", n.coerceAtLeast(0))

    fun hasAchievement(id: String): Boolean = strSet("ach").contains(id)
    fun addAchievement(id: String) { val s = strSet("ach"); s.add(id); putSet("ach", s) }
    fun achievementCount(): Int = strSet("ach").size

    fun lastriClaimed(chapter: Int): Boolean = strSet("lastri").contains(chapter.toString())
    fun setLastriClaimed(chapter: Int) { val s = strSet("lastri"); s.add(chapter.toString()); putSet("lastri", s) }

    fun unlockedFighters(): Set<String> {
        val s = hashSetOf("bimo", "bonar", "rani", "tamang", "sari", "malin")
        val d = chaptersDone
        if (d >= 2) s.add("jaelani")
        if (d >= 4) s.add("yance")
        if (d >= 5) s.add("zahra")
        if (d >= 6) s.add("garuda")
        return s
    }

    fun isUnlocked(fighterId: String) = unlockedFighters().contains(fighterId)

    fun resetAll() {
        sp.edit().clear().putInt("version", VERSION).apply()
    }
}
