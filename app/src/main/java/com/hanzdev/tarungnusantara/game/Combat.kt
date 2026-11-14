package com.hanzdev.tarungnusantara.game

import java.util.Random
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object St {
    const val IDLE = 0
    const val WALK = 1
    const val JAB = 2
    const val HOOK = 3
    const val UPPER = 4
    const val BLOCK = 5
    const val DODGE = 6
    const val HIT = 7
    const val DIZZY = 8
    const val DOWN = 9
    const val GETUP = 10
    const val SPECIAL = 11
    const val WIN = 12
    const val KO = 13
    const val STAGGER = 14
    const val DUCK = 15
}

object Ph {
    const val INTRO = 0
    const val READY = 1
    const val FIGHT = 2
    const val ROUND_END = 3
    const val BREAK = 4
    const val MATCH_END = 5
}

object Ev {
    const val BELL = 1
    const val HIT_LIGHT = 2
    const val HIT_HEAVY = 3
    const val HIT_SPECIAL = 4
    const val BLOCK = 5
    const val PERFECT = 6
    const val WHIFF = 7
    const val DODGE = 8
    const val DIZZY = 9
    const val DOWN = 10
    const val KO = 11
    const val METER_FULL = 12
    const val STAM_OUT = 13
    const val SPECIAL_START = 14
    const val GETUP = 15
    const val ROUND_START = 16
    const val MATCH_END = 17
    const val TICK = 18
    const val COM_ROUND = 30
    const val COM_BIGHIT = 31
    const val COM_PERFECT = 32
    const val COM_KO = 33
    const val COM_DECISION = 34
    const val COM_DIZZY = 35
    const val COM_AMAZING = 36
}

object Bits {
    const val JAB = 1
    const val HOOK = 2
    const val UPPER = 4
    const val SPECIAL = 8
    const val DODGE_L = 16
    const val DODGE_R = 32
    const val BLOCK_DOWN = 64
}

object Moves {
    val startup = intArrayOf(4, 9, 15)
    val active = intArrayOf(3, 4, 4)
    val recover = intArrayOf(6, 12, 18)
    val damage = floatArrayOf(5f, 10f, 15f)
    val stamina = floatArrayOf(4f, 8f, 12f)
    val reach = floatArrayOf(150f, 160f, 140f)
    val stun = intArrayOf(10, 16, 24)
    val push = floatArrayOf(10f, 20f, 30f)
    val lunge = floatArrayOf(22f, 30f, 18f)
}

const val FPS = 60
const val RING_HALF = 470f
const val MIN_DIST = 78f

class FighterInput {
    @Volatile var moveX = 0f
    @Volatile var duck = false
    @Volatile var block = false
    private var edges = 0

    @Synchronized fun press(bit: Int) { edges = edges or bit }
    @Synchronized fun takeEdges(): Int { val e = edges; edges = 0; return e }
    @Synchronized fun peekEdges(): Int = edges

    fun clear() {
        moveX = 0f; duck = false; block = false
        takeEdges()
    }
}

class Upgrades(val hp: Int = 0, val stamina: Int = 0, val power: Int = 0, val speed: Int = 0)

class Fighter(val def: FighterDef, val slot: Int) {
    var look: Look = def.look
    var gloveStyle = 0
    var x = 0f
    var facing = 1
    var maxHp = 100f
    var hp = 100f
    var maxStam = 100f
    var stam = 100f
    var meter = 0f

    var state = St.IDLE
    var frame = 0
    var stateLen = 0
    var attackKind = 0
    var startupF = 0
    var recoverF = 0
    var hitDone = false

    var specFrame = 0
    var counterFired = false
    var swapped = false

    var blockAge = 999
    var dodgeDir = 0
    var pushV = 0f
    var downTimer = 0
    var downNeed = 180
    var downCount = 0
    var heavyChain = 0
    var heavyTimer = 0
    var lastStandUsed = false
    var invul = 0
    var flash = 0
    var duck = false
    var bufferAct = 0
    var bufferAge = 99
    var stamWarn = 0

    var speedFactor = 1f
    var powerMul = 1f
    var defMul = 1f
    var walkSpeed = 250f
    var counterWin = 12
    var regen = 10f

    var dmgDealt = 0f
    var dmgTaken = 0f
    var roundDmg = 0f
    var hitsTaken = 0
    var perfects = 0
    var thrown = 0
    var landed = 0
    var knockdowns = 0
    var wins = 0

    fun applyStats(up: Upgrades) {
        val s = def.stats
        val power = s.power + up.power
        val speed = s.speed + up.speed
        speedFactor = 1.25f - 0.05f * speed
        powerMul = 0.7f + 0.06f * power
        defMul = 1.15f - 0.03f * s.defense
        walkSpeed = 200f + 12f * speed
        counterWin = 7 + s.counter
        regen = 6f + 0.8f * s.stamina
        maxHp = 100f + 5f * up.hp
        maxStam = 100f + 5f * up.stamina
        hp = maxHp
        stam = maxStam
    }

    fun setState(s: Int, len: Int = 0) {
        state = s
        frame = 0
        stateLen = len
    }

    val isActionable: Boolean
        get() = state == St.IDLE || state == St.WALK || state == St.DUCK || state == St.BLOCK

    val isAttacking: Boolean
        get() = state == St.JAB || state == St.HOOK || state == St.UPPER || state == St.SPECIAL

    val isDownish: Boolean
        get() = state == St.DOWN || state == St.KO || state == St.GETUP
}

class MatchConfig(
    val rounds: Int = 3,
    val roundSeconds: Int = 60,
    val training: Boolean = false,
    val introFrames: Int = 150,
    val breakFrames: Int = 600,
    val jamuKunyit: Boolean = false,
    val jamuKencur: Boolean = false,
    val jamuTemulawak: Boolean = false
)

interface MatchListener {
    fun onEvent(type: Int, who: Int, x: Float)
}

class Match(val cfg: MatchConfig, val a: Fighter, val b: Fighter, private val rng: Random) {
    val fighters = arrayOf(a, b)
    var listener: MatchListener? = null

    var phase = Ph.INTRO
    var phaseFrame = 0
    var round = 0
    var timer = cfg.roundSeconds * FPS
    var tickCount = 0
    var winner = -1
    var method = ""
    var matchOver = false
    var drawResult = false
    val roundWins = IntArray(2)
    var mirrorMode = false

    init {
        a.x = -190f; b.x = 190f
        a.facing = 1; b.facing = -1
        if (cfg.jamuKencur) a.regen *= 1.5f
        if (cfg.jamuTemulawak) a.meter = 30f
        if (cfg.training) {
            phase = Ph.FIGHT
            round = 1
        }
    }

    private fun emit(type: Int, who: Int, x: Float) {
        listener?.onEvent(type, who, x)
    }

    val timeLeftSeconds: Int get() = (timer + FPS - 1) / FPS

    fun tick(in0: FighterInput, in1: FighterInput) {
        tickCount++
        phaseFrame++
        when (phase) {
            Ph.INTRO -> if (phaseFrame >= cfg.introFrames) startRound()
            Ph.READY -> {
                idleStep(a); idleStep(b)
                if (phaseFrame == 30) emit(Ev.BELL, 0, 0f)
                if (phaseFrame >= 80) { phase = Ph.FIGHT; phaseFrame = 0 }
            }
            Ph.FIGHT -> fightStep(in0, in1)
            Ph.ROUND_END -> {
                idleStep(a); idleStep(b)
                if (phaseFrame >= 150) afterRoundEnd()
            }
            Ph.BREAK -> {
                idleStep(a); idleStep(b)
                if (phaseFrame >= cfg.breakFrames) startRound()
            }
            Ph.MATCH_END -> { idleStep(a); idleStep(b) }
        }
    }

    fun visualStep() {
        phaseFrame++
        for (f in fighters) {
            f.frame++
            if (f.flash > 0) f.flash--
        }
    }

    fun skipBreak() {
        if (phase == Ph.BREAK) phaseFrame = cfg.breakFrames
    }

    private fun idleStep(f: Fighter) {
        f.frame++
        if (f.flash > 0) f.flash--
        if (f.state == St.HIT || f.state == St.STAGGER) {
            if (f.frame >= f.stateLen) f.setState(St.IDLE)
        }
        if (f.state == St.DOWN || f.state == St.KO || f.state == St.WIN) return
        if (f.state != St.IDLE) f.setState(St.IDLE)
    }

    private fun startRound() {
        round++
        phase = Ph.READY
        phaseFrame = 0
        timer = cfg.roundSeconds * FPS
        for (f in fighters) {
            f.setState(St.IDLE)
            f.stam = f.maxStam
            f.downCount = 0
            f.heavyChain = 0
            f.lastStandUsed = false
            f.roundDmg = 0f
            f.invul = 0
            f.bufferAct = 0
            f.duck = false
        }
        a.x = -190f; b.x = 190f
        a.facing = 1; b.facing = -1
        if (cfg.jamuKunyit) a.hp = min(a.maxHp, a.hp + a.maxHp * 0.1f)
        emit(Ev.ROUND_START, round, 0f)
        emit(Ev.COM_ROUND, round, 0f)
    }

    private fun afterRoundEnd() {
        if (matchOver) {
            finishMatch()
            return
        }
        if (round >= cfg.rounds) {
            decideByPoints()
            finishMatch()
            return
        }
        for (f in fighters) if (f.state != St.KO) f.hp = min(f.maxHp, f.hp + f.maxHp * 0.15f)
        phase = Ph.BREAK
        phaseFrame = 0
    }

    private fun decideByPoints() {
        method = "Poin"
        emit(Ev.COM_DECISION, 0, 0f)
        winner = when {
            abs(a.dmgDealt - b.dmgDealt) > 0.05f -> if (a.dmgDealt > b.dmgDealt) 0 else 1
            abs(a.hp / a.maxHp - b.hp / b.maxHp) > 0.001f -> if (a.hp / a.maxHp > b.hp / b.maxHp) 0 else 1
            else -> -1
        }
        drawResult = winner == -1
        matchOver = true
    }

    private fun finishMatch() {
        phase = Ph.MATCH_END
        phaseFrame = 0
        matchOver = true
        if (winner >= 0) {
            val w = fighters[winner]
            w.wins++
            if (w.state != St.KO && w.state != St.DOWN) w.setState(St.WIN)
        }
        emit(Ev.MATCH_END, winner, 0f)
    }

    private fun endRound(koWinner: Int, how: String) {
        phase = Ph.ROUND_END
        phaseFrame = 0
        if (koWinner >= 0) {
            winner = koWinner
            method = how
            matchOver = true
            roundWins[koWinner]++
        } else {
            val ra = a.roundDmg
            val rb = b.roundDmg
            if (ra > rb) roundWins[0]++ else if (rb > ra) roundWins[1]++
        }
    }

    private fun fightStep(in0: FighterInput, in1: FighterInput) {
        if (!cfg.training) {
            timer--
            if (timer % FPS == 0 && timer in 1..(5 * FPS)) emit(Ev.TICK, 0, (timer / FPS).toFloat())
        }
        stepFighter(a, b, in0)
        stepFighter(b, a, in1)
        separate()
        if (cfg.training) {
            if (b.hp <= 0f || b.state == St.KO) { b.hp = b.maxHp; b.setState(St.IDLE) }
            if (a.hp <= 0f) { a.hp = a.maxHp; a.setState(St.IDLE) }
            return
        }
        if (matchOver) return
        for (f in fighters) {
            val o = fighters[1 - f.slot]
            if (f.state == St.KO) {
                emit(Ev.KO, f.slot, f.x)
                emit(Ev.COM_KO, f.slot, 0f)
                endRound(o.slot, "KO")
                return
            }
            if (f.downCount >= 3) {
                emit(Ev.COM_KO, f.slot, 0f)
                endRound(o.slot, "TKO")
                return
            }
        }
        if (timer <= 0) {
            emit(Ev.BELL, 0, 0f)
            endRound(-1, "")
        }
    }

    private fun separate() {
        if (a.isDownish || b.isDownish || a.state == St.DODGE || b.state == St.DODGE) {
            a.x = a.x.coerceIn(-RING_HALF, RING_HALF)
            b.x = b.x.coerceIn(-RING_HALF, RING_HALF)
            return
        }
        val dx = b.x - a.x
        val dist = abs(dx)
        if (dist < MIN_DIST) {
            val push = (MIN_DIST - dist) / 2f
            val dir = if (dx >= 0f) 1f else -1f
            a.x -= dir * push
            b.x += dir * push
        }
        a.x = a.x.coerceIn(-RING_HALF, RING_HALF)
        b.x = b.x.coerceIn(-RING_HALF, RING_HALF)
    }

    private fun bufferFrom(f: Fighter, edges: Int) {
        val act = when {
            edges and Bits.SPECIAL != 0 -> 4
            edges and Bits.UPPER != 0 -> 3
            edges and Bits.HOOK != 0 -> 2
            edges and Bits.JAB != 0 -> 1
            edges and Bits.DODGE_L != 0 -> 5
            edges and Bits.DODGE_R != 0 -> 6
            else -> 0
        }
        if (act != 0) { f.bufferAct = act; f.bufferAge = 0 }
    }

    private fun stepFighter(f: Fighter, o: Fighter, inp: FighterInput) {
        val dt = 1f / FPS
        val edges = inp.takeEdges()
        f.frame++
        if (f.flash > 0) f.flash--
        if (f.invul > 0) f.invul--
        if (f.stamWarn > 0) f.stamWarn--
        if (f.heavyTimer > 0) { f.heavyTimer--; if (f.heavyTimer == 0) f.heavyChain = 0 }
        if (f.bufferAge < 99) f.bufferAge++
        if (f.state != St.DOWN && f.state != St.KO && f.state != St.WIN) {
            f.facing = if (o.x >= f.x) 1 else -1
        }
        if (f.state != St.DOWN && f.state != St.KO) bufferFrom(f, edges)
        else if (f.state == St.DOWN && edges and (Bits.JAB or Bits.HOOK or Bits.UPPER) != 0) f.downTimer += 14

        val wasBlock = f.state == St.BLOCK
        if (f.pushV > 0.5f) {
            f.x -= f.facing * f.pushV
            f.pushV *= 0.8f
        }

        when (f.state) {
            St.IDLE, St.WALK, St.DUCK, St.BLOCK -> actionable(f, o, inp, wasBlock, dt)
            St.JAB, St.HOOK, St.UPPER -> attackStep(f, o)
            St.DODGE -> {
                if (f.frame <= 14) f.x += f.dodgeDir * 9f
                if (f.frame >= 16) f.setState(St.IDLE)
            }
            St.HIT, St.STAGGER -> {
                if (f.frame >= f.stateLen) f.setState(St.IDLE)
            }
            St.DIZZY -> {
                if (f.frame >= f.stateLen) { f.setState(St.IDLE); f.heavyChain = 0 }
            }
            St.DOWN -> {
                f.downTimer++
                if (f.downTimer >= f.downNeed) {
                    f.setState(St.GETUP, 26)
                    f.invul = 40
                    emit(Ev.GETUP, f.slot, f.x)
                }
            }
            St.GETUP -> if (f.frame >= f.stateLen) f.setState(St.IDLE)
            St.SPECIAL -> specialStep(f, o)
            else -> {}
        }
        if (f.meter >= 100f && f.state != St.KO) {
            if (f.meter > 100f) f.meter = 100f
        }
    }

    private fun regenStam(f: Fighter, mul: Float, dt: Float) {
        f.stam = min(f.maxStam, f.stam + f.regen * mul * dt)
    }

    private fun actionable(f: Fighter, o: Fighter, inp: FighterInput, wasBlock: Boolean, dt: Float) {
        if (f.bufferAge <= 8 && f.bufferAct != 0) {
            val act = f.bufferAct
            var consumed = true
            when (act) {
                1, 2, 3 -> {
                    val k = act - 1
                    if (f.stam >= Moves.stamina[k]) startAttack(f, k)
                    else {
                        if (f.stamWarn == 0) { emit(Ev.STAM_OUT, f.slot, f.x); f.stamWarn = 90 }
                    }
                }
                4 -> if (f.meter >= 100f) startSpecial(f) else consumed = true
                5, 6 -> {
                    if (f.stam >= 6f) startDodge(f, if (act == 5) -1 else 1) else consumed = true
                }
            }
            if (consumed) { f.bufferAct = 0; f.bufferAge = 99 }
            if (!f.isActionable) return
        }
        val moveX = inp.moveX
        when {
            inp.block -> {
                if (f.state != St.BLOCK) { f.blockAge = 0 }
                f.setStateKeep(St.BLOCK)
                f.blockAge++
                val slow = if (abs(moveX) > 0.3f) moveX * f.walkSpeed * 0.35f * dt else 0f
                f.x += slow
                regenStam(f, 0.5f, dt)
            }
            inp.duck -> {
                f.setStateKeep(St.DUCK)
                f.blockAge = 999
                regenStam(f, 1f, dt)
            }
            abs(moveX) > 0.2f -> {
                f.setStateKeep(St.WALK)
                f.blockAge = 999
                val toward = (moveX > 0f) == (f.facing > 0)
                f.x += moveX * f.walkSpeed * (if (toward) 1f else 0.82f) * dt
                regenStam(f, 1f, dt)
            }
            else -> {
                f.setStateKeep(St.IDLE)
                f.blockAge = 999
                regenStam(f, 1f, dt)
            }
        }
        f.duck = f.state == St.DUCK
    }

    private fun Fighter.setStateKeep(s: Int) {
        if (state != s) { state = s; frame = 0 }
    }

    private fun startAttack(f: Fighter, kind: Int) {
        f.attackKind = kind
        f.startupF = max(2, (Moves.startup[kind] * f.speedFactor).roundToInt())
        f.recoverF = max(3, (Moves.recover[kind] * f.speedFactor).roundToInt())
        f.hitDone = false
        f.stam -= Moves.stamina[kind]
        f.thrown++
        f.setState(St.JAB + kind)
        f.duck = false
    }

    private fun attackStep(f: Fighter, o: Fighter) {
        val k = f.attackKind
        val act = Moves.active[k]
        if (f.frame <= f.startupF) {
            f.x += f.facing * (Moves.lunge[k] / f.startupF)
        }
        if (f.frame == f.startupF + 1) emit(Ev.WHIFF, f.slot, f.x)
        if (f.frame > f.startupF && f.frame <= f.startupF + act && !f.hitDone) {
            val dmg = Moves.damage[k]
            val connected = strike(f, o, dmg, Moves.reach[k], false, k != 0, false, Moves.stun[k], Moves.push[k], k)
            if (connected) f.hitDone = true
        }
        if (f.frame >= f.startupF + act + f.recoverF) f.setState(St.IDLE)
    }

    private fun startDodge(f: Fighter, dir: Int) {
        f.stam -= 6f
        f.dodgeDir = dir
        f.setState(St.DODGE, 16)
        f.invul = 0
        emit(Ev.DODGE, f.slot, f.x)
    }

    private fun startSpecial(f: Fighter) {
        f.meter = 0f
        f.specFrame = 0
        f.counterFired = false
        f.swapped = false
        f.setState(St.SPECIAL)
        emit(Ev.SPECIAL_START, f.slot, f.x)
        emit(Ev.COM_AMAZING, f.slot, 0f)
    }

    private fun specialInvul(f: Fighter): Boolean {
        val sp = f.def.special
        return f.state == St.SPECIAL && sp.invulStart >= 0 && f.specFrame in sp.invulStart..sp.invulEnd
    }

    private fun specialStep(f: Fighter, o: Fighter) {
        val sp = f.def.special
        if (sp.counterStance && !f.counterFired && f.specFrame == 40) f.specFrame = 46
        if (sp.swapAt == f.specFrame && !f.swapped) {
            f.swapped = true
            val side = if (f.x <= o.x) 1f else -1f
            f.x = (o.x + side * (MIN_DIST + 12f)).coerceIn(-RING_HALF, RING_HALF)
            f.facing = if (o.x >= f.x) 1 else -1
        }
        for (s in sp.strikes) {
            if (s.frame != f.specFrame) continue
            if (sp.counterStance && !f.counterFired) continue
            f.x += f.facing * s.lunge
            var dmg = s.damage
            if (sp.randomize) dmg *= 0.7f + rng.nextFloat() * 0.6f
            strike(f, o, dmg, s.reach, s.pierce, true, s.knockdown, 18, 24f, 3)
        }
        f.specFrame++
        if (f.specFrame >= sp.total) f.setState(St.IDLE)
    }

    private fun strike(
        att: Fighter, def: Fighter, baseDmg: Float, reach: Float, pierce: Boolean,
        heavy: Boolean, knockdown: Boolean, stun: Int, push: Float, kind: Int
    ): Boolean {
        val dx = (def.x - att.x) * att.facing
        if (abs(def.x - att.x) > reach || dx < -24f) return false
        if (def.state == St.KO || def.state == St.DOWN || def.state == St.GETUP || def.state == St.WIN) return false
        if (def.invul > 0 || specialInvul(def) || (def.state == St.DODGE && def.frame in 3..13)) {
            if (def.state == St.DODGE) emit(Ev.DODGE, def.slot, def.x)
            return false
        }
        if (def.state == St.DUCK && kind in 0..1) return false

        val dsp = def.def.special
        if (def.state == St.SPECIAL && dsp.counterStance && !def.counterFired && def.specFrame in 4..36) {
            def.counterFired = true
            def.specFrame = 38
            def.perfects++
            def.meter = min(100f, def.meter + 15f)
            att.setState(St.STAGGER, 40)
            emit(Ev.PERFECT, def.slot, def.x)
            emit(Ev.COM_PERFECT, def.slot, 0f)
            return true
        }

        if (def.state == St.BLOCK && !pierce) {
            if (def.blockAge <= def.counterWin) {
                def.perfects++
                def.meter = min(100f, def.meter + 15f)
                att.setState(St.STAGGER, 36)
                att.pushV = 8f
                emit(Ev.PERFECT, def.slot, def.x)
                emit(Ev.COM_PERFECT, def.slot, 0f)
                return true
            }
            val enough = def.stam >= 3f
            def.stam = max(0f, def.stam - 3f)
            val dmg = baseDmg * att.powerMul * def.defMul * (if (enough) 0.3f else 0.65f)
            applyDamage(att, def, dmg)
            att.meter = min(100f, att.meter + 3f)
            def.pushV = push * 0.5f
            def.flash = 3
            emit(Ev.BLOCK, def.slot, def.x)
            checkKo(def)
            return true
        }

        var dmg = baseDmg * att.powerMul * def.defMul
        val wasDizzy = def.state == St.DIZZY
        if (wasDizzy) dmg *= 1.25f
        applyDamage(att, def, dmg)
        att.meter = min(100f, att.meter + 8f)
        def.meter = min(100f, def.meter + 5f)
        att.landed++
        def.hitsTaken++
        def.flash = 6
        emit(if (kind == 3) Ev.HIT_SPECIAL else if (heavy) Ev.HIT_HEAVY else Ev.HIT_LIGHT, def.slot, def.x)
        if (heavy) emit(Ev.COM_BIGHIT, def.slot, 0f)
        if (att.meter >= 100f && att.meter - 8f < 100f) emit(Ev.METER_FULL, att.slot, 0f)

        if (def.hp <= 0f) {
            def.hp = 0f
            def.setState(St.KO)
            return true
        }
        val armored = def.state == St.SPECIAL && dsp.invulStart >= 0
        if (heavy) {
            def.heavyChain++
            def.heavyTimer = 150
        }
        val lowHpKnock = heavy && !def.lastStandUsed && def.hp < def.maxHp * 0.2f
        if (!armored) {
            if (knockdown || (wasDizzy && heavy) || lowHpKnock) {
                if (lowHpKnock) def.lastStandUsed = true
                def.setState(St.DOWN)
                def.downTimer = 0
                def.downNeed = 180
                def.downCount++
                def.knockdowns++
                def.heavyChain = 0
                def.pushV = push
                emit(Ev.DOWN, def.slot, def.x)
            } else if (heavy && def.heavyChain >= 3) {
                def.heavyChain = 0
                def.setState(St.DIZZY, 90)
                emit(Ev.DIZZY, def.slot, def.x)
                emit(Ev.COM_DIZZY, def.slot, 0f)
            } else {
                def.setState(St.HIT, stun)
                def.pushV = push
                def.bufferAct = 0
            }
        }
        return true
    }

    private fun applyDamage(att: Fighter, def: Fighter, dmg: Float) {
        val real = min(dmg, def.hp)
        def.hp -= dmg
        def.dmgTaken += real
        att.dmgDealt += real
        att.roundDmg += real
    }

    private fun checkKo(def: Fighter) {
        if (def.hp <= 0f) { def.hp = 0f; def.setState(St.KO) }
    }
}
