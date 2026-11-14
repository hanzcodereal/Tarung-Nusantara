package com.hanzdev.tarungnusantara.data

import com.hanzdev.tarungnusantara.game.Mode
import com.hanzdev.tarungnusantara.game.Roster
import com.hanzdev.tarungnusantara.game.SessionConfig
import java.util.Random

object Career {
    fun chapter(n: Int): Chapter = Teks.chapters[(n - 1).coerceIn(0, Teks.chapters.size - 1)]

    fun opponentFor(chapter: Int, matchIdx: Int, playerId: String): String {
        val ch = chapter(chapter)
        if (matchIdx >= 3) return ch.bossId
        val pool = Roster.starters.map { it.id }.filter { it != playerId && it != ch.bossId }
        return pool[(chapter * 3 + matchIdx) % pool.size]
    }

    fun levelFor(chapter: Int, matchIdx: Int): Int {
        val base = chapter(chapter).levelAwal + (if (matchIdx >= 3) 1 else 0)
        return base.coerceIn(0, 3)
    }

    fun config(
        chapter: Int, matchIdx: Int, playerId: String,
        kunyit: Boolean = false, kencur: Boolean = false, temulawak: Boolean = false
    ): SessionConfig {
        val ch = chapter(chapter)
        val rounds = if (matchIdx >= 3 && chapter == 6) 5 else 3
        return SessionConfig(
            Mode.CAREER, playerId, opponentFor(chapter, matchIdx, playerId), ch.arenaId,
            levelFor(chapter, matchIdx), rounds, kunyit, kencur, temulawak, chapter, matchIdx
        )
    }

    fun unlockedAfter(chapter: Int): String? = when (chapter) {
        2 -> "Bang Jaelani"
        4 -> "Yance Wenda"
        5 -> "Cut Zahra"
        6 -> "Sang Garuda Bertopeng"
        else -> null
    }
}

object Arcade {
    private val rng = Random()

    fun next(playerId: String, wins: Int): SessionConfig {
        val ids = Roster.all.map { it.id }
        val opp = ids[rng.nextInt(ids.size)]
        val arena = Roster.arenas[rng.nextInt(Roster.arenas.size)].id
        val level = (wins / 5).coerceIn(0, 3)
        return SessionConfig(Mode.ARCADE, playerId, opp, arena, level, 1, arcadeWins = wins)
    }
}
