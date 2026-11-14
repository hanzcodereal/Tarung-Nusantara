package com.hanzdev.tarungnusantara.game

import java.util.Random
import kotlin.math.abs

object Level {
    const val MUDAH = 0
    const val SEDANG = 1
    const val SULIT = 2
    const val LEGENDA = 3
    val names = arrayOf("Mudah", "Sedang", "Sulit", "Legenda")
    val hints = arrayOf(
        "Menyerang acak, jarang menangkis",
        "Menangkis pola dasar, membalas serangan",
        "Membaca kebiasaanmu, memakai counter dan jurus",
        "Sulit ditebak dan sangat cepat, tetapi tetap bisa dikalahkan"
    )
}

/** Menghasilkan input untuk satu petarung memakai antarmuka yang sama dengan sentuhan pemain. */
class AIController(private val level: Int, private val rng: Random) {
    private val reactFrames = intArrayOf(26, 16, 9, 4)
    private val defendProb = floatArrayOf(0.12f, 0.45f, 0.75f, 0.92f)
    private val perfectSkill = floatArrayOf(0.02f, 0.25f, 0.5f, 0.75f)
    private val attackRate = floatArrayOf(0.9f, 1.4f, 2.0f, 2.8f)
    private val specialProb = floatArrayOf(0.25f, 0.55f, 0.9f, 1.0f)
    private val dodgeProb = floatArrayOf(0f, 0.1f, 0.3f, 0.45f)

    private val memory = IntArray(3) { -1 }
    private var memIdx = 0
    private var lastOppState = St.IDLE

    private var decideTimer = 30
    private var pendingDefenseIn = -1
    private var blockDelay = -1
    private var blockHold = 0
    private var duckHold = 0
    private var dodgePending = 0
    private var retreatFrames = 0

    private fun remember(kind: Int) {
        memory[memIdx] = kind
        memIdx = (memIdx + 1) % memory.size
    }

    private fun favoriteKind(): Int {
        val counts = IntArray(4)
        for (m in memory) if (m in 0..3) counts[m]++
        var best = -1
        var bestC = 1
        for (i in 0..3) if (counts[i] > bestC) { best = i; bestC = counts[i] }
        return best
    }

    private fun oppKind(op: Fighter): Int = when (op.state) {
        St.JAB -> 0
        St.HOOK -> 1
        St.UPPER -> 2
        St.SPECIAL -> 3
        else -> -1
    }

    fun update(me: Fighter, op: Fighter, inp: FighterInput) {
        inp.moveX = 0f
        inp.duck = false
        inp.block = false

        val kind = oppKind(op)
        val newAttack = kind >= 0 && (lastOppState != op.state) && op.frame <= 2
        lastOppState = op.state
        if (newAttack) {
            remember(kind)
            if (rng.nextFloat() < defendProb[level]) pendingDefenseIn = reactFrames[level] + rng.nextInt(4)
        }
        if (pendingDefenseIn >= 0) {
            pendingDefenseIn--
            if (pendingDefenseIn < 0) planDefense(me, op)
        }

        if (blockDelay > 0) blockDelay--
        if (blockDelay == 0) {
            if (me.isActionable) inp.block = true
            blockHold--
            if (blockHold <= 0) blockDelay = -1
        }
        if (duckHold > 0) { duckHold--; inp.duck = true }
        if (dodgePending != 0 && me.isActionable) {
            inp.press(if (dodgePending < 0) Bits.DODGE_L else Bits.DODGE_R)
            dodgePending = 0
        }
        if (!me.isActionable) return
        if (blockDelay >= 0 || duckHold > 0) {
            if (blockDelay > 0) inp.block = false
            return
        }

        val dist = abs(me.x - op.x)
        val toward = if (op.x >= me.x) 1f else -1f

        if (me.stam < 22f && retreatFrames <= 0) retreatFrames = 40 + rng.nextInt(30)
        if (retreatFrames > 0) {
            retreatFrames--
            inp.moveX = -toward * 0.8f
            if (dist < 130f && op.isAttacking) inp.block = true
            return
        }

        if (me.meter >= 100f && dist <= 170f && rng.nextFloat() < specialProb[level] * 0.08f) {
            inp.press(Bits.SPECIAL)
            return
        }

        decideTimer--
        val vulnerable = op.state == St.DIZZY || op.state == St.STAGGER || op.state == St.GETUP
        if (decideTimer <= 0) {
            decideTimer = (60f / attackRate[level] * (0.6f + rng.nextFloat() * 0.8f)).toInt()
            if (vulnerable) decideTimer /= 2
            if (dist <= 160f) chooseAttack(me, op, inp, vulnerable)
        }

        if (dist > 175f) {
            inp.moveX = toward * 0.9f
        } else if (dist < 95f && !op.isAttacking) {
            if (rng.nextFloat() < 0.5f) inp.moveX = -toward * 0.6f
        } else if (level >= 2 && dist in 110f..150f && rng.nextFloat() < 0.02f) {
            inp.moveX = toward * 0.5f
        }
    }

    private fun chooseAttack(me: Fighter, op: Fighter, inp: FighterInput, vulnerable: Boolean) {
        if (me.meter >= 100f && rng.nextFloat() < specialProb[level]) {
            inp.press(Bits.SPECIAL)
            return
        }
        val r = rng.nextFloat()
        val idleChance = when (level) { 0 -> 0.3f; 1 -> 0.15f; 2 -> 0.05f; else -> 0f }
        if (r < idleChance) return
        val heavyBias = if (vulnerable) 0.5f else when (level) { 0 -> 0.25f; 1 -> 0.3f; 2 -> 0.35f; else -> 0.4f }
        val roll = rng.nextFloat()
        var bit = when {
            roll < 1f - heavyBias - 0.2f -> Bits.JAB
            roll < 1f - 0.2f -> Bits.HOOK
            else -> Bits.UPPER
        }
        if (level >= 2 && op.state == St.BLOCK) bit = Bits.UPPER.takeIf { me.stam >= 14f } ?: Bits.JAB
        if (op.state == St.DUCK) bit = Bits.UPPER
        inp.press(bit)
    }

    private fun planDefense(me: Fighter, op: Fighter) {
        if (!me.isActionable) return
        val dist = abs(me.x - op.x)
        val kind = oppKind(op)
        if (kind < 0) return
        val reach = if (kind == 3) 200f else Moves.reach[kind] + 30f
        if (dist > reach) return

        val fav = favoriteKind()
        val canDuck = level >= 2 && (kind == 0 || kind == 1 || fav == 0 || fav == 1) && kind != 2 && kind != 3
        val untilHit = when (kind) {
            0, 1, 2 -> (op.startupF - op.frame).coerceAtLeast(0)
            else -> 6
        }
        val roll = rng.nextFloat()
        if (roll < dodgeProb[level] && me.stam >= 6f) {
            dodgePending = if (op.x >= me.x) -1 else 1
            return
        }
        if (canDuck && rng.nextFloat() < 0.4f) {
            duckHold = untilHit + 18
            return
        }
        val perfect = rng.nextFloat() < perfectSkill[level]
        val delay = if (perfect) (untilHit - 2 - rng.nextInt(3)).coerceAtLeast(1) else 1
        blockDelay = delay
        blockHold = untilHit + 14 - delay + 6
    }
}
