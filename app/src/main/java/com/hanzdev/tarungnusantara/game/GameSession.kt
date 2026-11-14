package com.hanzdev.tarungnusantara.game

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.hanzdev.tarungnusantara.audio.Commentator
import com.hanzdev.tarungnusantara.audio.Sound
import com.hanzdev.tarungnusantara.bluetooth.BtLink
import com.hanzdev.tarungnusantara.bluetooth.BtSession
import com.hanzdev.tarungnusantara.bluetooth.Msg
import com.hanzdev.tarungnusantara.bluetooth.NetEvents
import com.hanzdev.tarungnusantara.bluetooth.Packet
import com.hanzdev.tarungnusantara.bluetooth.SyncProtocol
import com.hanzdev.tarungnusantara.data.Save
import com.hanzdev.tarungnusantara.data.Shop
import com.hanzdev.tarungnusantara.data.Teks
import com.hanzdev.tarungnusantara.ui.Hud
import java.util.Random
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

object Mode {
    const val CAREER = 0
    const val ARCADE = 1
    const val QUICK = 2
    const val TRAINING = 3
    const val BT_HOST = 4
    const val BT_CLIENT = 5
}

class SessionConfig(
    val mode: Int,
    val p1: String,
    val p2: String,
    val arenaId: String,
    val level: Int,
    val rounds: Int,
    val kunyit: Boolean = false,
    val kencur: Boolean = false,
    val temulawak: Boolean = false,
    val chapter: Int = 0,
    val matchIdx: Int = 0,
    val arcadeWins: Int = 0,
    val btGloves: Array<String> = arrayOf("", ""),
    val btHeads: Array<String> = arrayOf("", ""),
    val btCostumes: Array<String> = arrayOf("", "")
) {
    fun withJamu(k: Boolean, e: Boolean, t: Boolean) = SessionConfig(
        mode, p1, p2, arenaId, level, rounds, k, e, t, chapter, matchIdx, arcadeWins, btGloves, btHeads, btCostumes
    )

    fun toIntent(ctx: Context): Intent {
        val i = Intent(ctx, GameActivity::class.java)
        i.putExtra("mode", mode).putExtra("p1", p1).putExtra("p2", p2).putExtra("arena", arenaId)
            .putExtra("level", level).putExtra("rounds", rounds)
            .putExtra("kunyit", kunyit).putExtra("kencur", kencur).putExtra("temulawak", temulawak)
            .putExtra("chapter", chapter).putExtra("matchIdx", matchIdx).putExtra("arcadeWins", arcadeWins)
            .putExtra("bg0", btGloves[0]).putExtra("bg1", btGloves[1])
            .putExtra("bh0", btHeads[0]).putExtra("bh1", btHeads[1])
            .putExtra("bc0", btCostumes[0]).putExtra("bc1", btCostumes[1])
        return i
    }

    companion object {
        fun from(i: Intent): SessionConfig = SessionConfig(
            i.getIntExtra("mode", Mode.QUICK), i.getStringExtra("p1") ?: "bimo", i.getStringExtra("p2") ?: "bonar",
            i.getStringExtra("arena") ?: "keraton", i.getIntExtra("level", 1), i.getIntExtra("rounds", 3),
            i.getBooleanExtra("kunyit", false), i.getBooleanExtra("kencur", false), i.getBooleanExtra("temulawak", false),
            i.getIntExtra("chapter", 0), i.getIntExtra("matchIdx", 0), i.getIntExtra("arcadeWins", 0),
            arrayOf(i.getStringExtra("bg0") ?: "", i.getStringExtra("bg1") ?: ""),
            arrayOf(i.getStringExtra("bh0") ?: "", i.getStringExtra("bh1") ?: ""),
            arrayOf(i.getStringExtra("bc0") ?: "", i.getStringExtra("bc1") ?: "")
        )
    }
}

class GameSession(
    private val ctx: Context,
    val cfg: SessionConfig,
    private val commentator: Commentator?
) : MatchListener {
    private val link: BtLink? get() = if (cfg.mode == Mode.BT_HOST || cfg.mode == Mode.BT_CLIENT) BtSession.link else null
    val localSlot = if (cfg.mode == Mode.BT_CLIENT) 1 else 0
    val isClient = cfg.mode == Mode.BT_CLIENT
    val isHost = cfg.mode == Mode.BT_HOST
    val isNet = isClient || isHost
    val isTraining = cfg.mode == Mode.TRAINING

    val p1: Fighter
    val p2: Fighter
    val match: Match
    val input0 = FighterInput()
    val input1 = FighterInput()
    val touch = TouchControls(input0)
    private val rng = Random()
    private var ai: AIController? = null
    private val effects = Effects()
    private val fighterRenderer = FighterRenderer()
    val arena: ArenaRenderer = ArenaRenderer(ctx, Roster.arenaById(cfg.arenaId))
    private val hud = Hud()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val netEvents = NetEvents()

    var anim = 0f
    var paused = false
    var hitStop = 0
    var bannerText = ""
    var bannerFrames = 0
    var commentText = ""
    var commentFrames = 0
    var toastText = ""
    var toastFrames = 0
    var cheer = 0f
    var reconnecting = false
    var connectionLost = false
    var onControlPacket: ((Packet) -> Unit)? = null
    var onMatchEnded: (() -> Unit)? = null
    var onConnectionLost: (() -> Unit)? = null
    private var lostNotified = false
    private var endNotified = false
    private var snapCounter = 0
    private var inputCounter = 0

    var tutorialStep = 0
    var trainerMode = 0
    private var trainerTimer = 0
    private var travel = 0f
    private var lastX = 0f
    private var hookUsed = 0
    private var upperUsed = 0
    private var dodgeUsed = 0
    private var specialUsed = 0
    private var lastP1State = St.IDLE
    private var stepBase = 0
    var tutorialDone = false

    private val vibrator: Vibrator? = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    init {
        val d1 = Roster.byId(cfg.p1)
        val d2 = Roster.byId(cfg.p2)
        p1 = Fighter(d1, 0)
        p2 = Fighter(d2, 1)
        if (isNet) {
            val l1 = Shop.lookWith(d1, cfg.btGloves[0], cfg.btHeads[0], cfg.btCostumes[0])
            val l2 = Shop.lookWith(d2, cfg.btGloves[1], cfg.btHeads[1], cfg.btCostumes[1])
            p1.look = l1.first; p1.gloveStyle = l1.second
            p2.look = l2.first; p2.gloveStyle = l2.second
            p1.applyStats(Upgrades()); p2.applyStats(Upgrades())
        } else {
            val mine = Shop.lookFor(d1)
            p1.look = mine.first
            p1.gloveStyle = mine.second
            p1.applyStats(Shop.playerUpgrades())
            p2.applyStats(Upgrades())
            if (d1.id == d2.id) p2.look = p2.look.copy(glove = 0xFF1F5FA8.toInt())
            if (!isTraining) ai = AIController(cfg.level, rng)
        }
        val mc = MatchConfig(
            rounds = cfg.rounds, training = isTraining,
            jamuKunyit = cfg.kunyit, jamuKencur = cfg.kencur, jamuTemulawak = cfg.temulawak
        )
        match = Match(mc, p1, p2, rng)
        match.mirrorMode = isClient
        match.listener = this
        touch.buttonsLeft = Save.buttonsLeft
        if (isTraining) {
            trainerMode = 0
            p2.look = p2.look.copy(skin = 0xFF7A6A5A.toInt())
        }
        lastX = p1.x
    }

    fun layout(w: Int, h: Int) = touch.layout(w, h)

    fun banner(text: String, frames: Int = 90) { bannerText = text; bannerFrames = frames }
    fun toast(text: String) { toastText = text; toastFrames = 110 }

    @Suppress("DEPRECATION")
    private fun vibrate(ms: Long) {
        if (!Save.haptic) return
        val v = vibrator ?: return
        try {
            if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            else v.vibrate(ms)
        } catch (e: Exception) {
            return
        }
    }

    override fun onEvent(type: Int, who: Int, x: Float) {
        if (isHost) netEvents.add(type, who, x)
        handleEvent(type, who, x)
    }

    private val shakeScale: Float get() = if (Save.reduceMotion) 0.3f else 1f

    fun handleEvent(type: Int, who: Int, x: Float) {
        val y = ArenaRenderer.GROUND - 190f
        when (type) {
            Ev.HIT_LIGHT -> {
                Sound.play("jab", 1f, 0.95f + rng.nextFloat() * 0.1f)
                effects.spark(x, y, 38f, Pal.KREM)
                effects.addTrauma(0.12f, shakeScale)
                if (who == localSlot) vibrate(18)
            }
            Ev.HIT_HEAVY -> {
                val attacker = if (who == 0) p2 else p1
                Sound.play(if (attacker.attackKind == 2) "upper" else "hook", 1f, 0.95f + rng.nextFloat() * 0.1f)
                effects.spark(x, y, 58f, Pal.KUNYIT)
                effects.addTrauma(0.35f, shakeScale)
                if (!isClient) hitStop = max(hitStop, 3)
                if (who == localSlot) vibrate(35)
            }
            Ev.HIT_SPECIAL -> {
                Sound.play("special")
                effects.spark(x, y, 84f, Pal.CABAI)
                effects.ring(x, y, 90f, Pal.KUNYIT)
                effects.addTrauma(0.7f, shakeScale)
                if (!isClient) hitStop = max(hitStop, 6)
                if (who == localSlot) vibrate(60)
            }
            Ev.BLOCK -> {
                Sound.play("block")
                effects.spark(x, y, 30f, Pal.NILA_TERANG)
                effects.addTrauma(0.08f, shakeScale)
            }
            Ev.PERFECT -> {
                Sound.play("perfect")
                effects.ring(x, y, 80f, Pal.KUNYIT)
                effects.addTrauma(0.15f, shakeScale)
                if (who == localSlot) vibrate(25)
            }
            Ev.WHIFF -> Sound.play("whoosh", 0.45f)
            Ev.DODGE -> Sound.play("dodge", 0.7f)
            Ev.DIZZY -> Sound.play("dizzy")
            Ev.DOWN -> {
                Sound.play("down")
                effects.addTrauma(0.5f, shakeScale)
                if (!isClient) hitStop = max(hitStop, 8)
                banner("JATUH!", 60)
            }
            Ev.KO -> {
                Sound.play("ko")
                effects.addTrauma(1f, shakeScale)
                effects.flashAlpha = if (Save.reduceMotion) 0f else 0.5f
                banner("KO!", 150)
            }
            Ev.METER_FULL -> if (who == localSlot) { toast(Teks.JURUS_PENUH); Sound.play("tick", 0.8f, 1.4f) }
            Ev.STAM_OUT -> if (who == localSlot) toast(Teks.STAMINA_HABIS)
            Ev.SPECIAL_START -> {
                effects.ring(x, y, 100f, Pal.KREM)
                if (who == localSlot) vibrate(40)
            }
            Ev.BELL -> Sound.play("bell")
            Ev.ROUND_START -> {
                banner(if (match.cfg.rounds == 1) "BERSIAP" else "RONDE $who", 80)
                Sound.play("cheer", 0.6f)
            }
            Ev.TICK -> if (x > 0f) Sound.play("tick", 0.7f)
            Ev.MATCH_END -> {
                Sound.play("cheer")
                cheer = 1f
                val w = match.winner
                banner(if (w < 0) "SERI" else (if (w == localSlot) "MENANG!" else "KALAH"), 200)
            }
            Ev.COM_ROUND, Ev.COM_BIGHIT, Ev.COM_PERFECT, Ev.COM_KO, Ev.COM_DECISION, Ev.COM_DIZZY, Ev.COM_AMAZING -> {
                val t = Teks.komentar[type] ?: return
                if (type == Ev.COM_BIGHIT && commentFrames > 60) return
                commentText = t
                commentFrames = 120
                commentator?.say(t)
            }
        }
    }

    fun pauseGame(v: Boolean) {
        paused = v
        touch.reset()
        input0.clear()
    }

    private fun pollLink() {
        val l = link ?: return
        while (true) {
            val pk = l.poll() ?: break
            when (pk.type) {
                Msg.INPUT -> if (isHost) SyncProtocol.readInput(pk.data, input1)
                Msg.SNAP -> if (isClient) SyncProtocol.readSnapshot(pk.data, match) { t, w, x -> handleEvent(t, w, x) }
                else -> onControlPacket?.invoke(pk)
            }
        }
    }

    fun update() {
        if (paused) return
        anim += 1f / FPS
        if (cheer > 0f) cheer = max(0f, cheer - 0.004f)
        if (bannerFrames > 0) bannerFrames--
        if (commentFrames > 0) commentFrames--
        if (toastFrames > 0) toastFrames--
        effects.update()

        if (isNet) {
            val l = link
            if (l != null && !l.alive && !reconnecting && !connectionLost && match.phase != Ph.MATCH_END) {
                reconnecting = true
                toast(Teks.KONEKSI_PUTUS)
                Thread {
                    if (!BtSession.reconnect(5000)) connectionLost = true
                }.start()
            }
            if (reconnecting) {
                if (connectionLost) {
                    reconnecting = false
                    if (!lostNotified) { lostNotified = true; onConnectionLost?.invoke() }
                } else if (l != null && l.alive) {
                    reconnecting = false
                    toastFrames = 0
                }
                return
            }
            pollLink()
        }

        if (isClient) {
            match.visualStep()
            sendInput()
            notifyEnd()
            return
        }

        if (hitStop > 0) { hitStop--; return }

        val fighting = match.phase == Ph.FIGHT
        if (!fighting) {
            input1.clear()
            if (!isNet) input0.takeEdges()
        } else if (!isNet || isHost) {
            if (!isNet) {
                if (isTraining) trainerStep() else ai?.update(p2, p1, input1)
            }
        }
        if (isTraining) trackTutorial()
        match.tick(input0, input1)

        if (isHost) {
            snapCounter++
            if (snapCounter >= 3) {
                snapCounter = 0
                link?.send(Msg.SNAP, SyncProtocol.writeSnapshot(match, netEvents))
                netEvents.clear()
            }
        }
        notifyEnd()
    }

    private fun sendInput() {
        inputCounter++
        if (inputCounter < 3) return
        inputCounter = 0
        link?.send(Msg.INPUT, SyncProtocol.writeInput(input0))
    }

    private fun notifyEnd() {
        if (!endNotified && match.phase == Ph.MATCH_END && match.phaseFrame > 120) {
            endNotified = true
            onMatchEnded?.invoke()
        }
    }

    private fun trainerStep() {
        input1.clear()
        if (match.phase != Ph.FIGHT) return
        if (trainerMode == 1 && p2.isActionable) {
            trainerTimer++
            val dist = abs(p1.x - p2.x)
            if (dist > 150f) input1.moveX = if (p1.x < p2.x) -0.8f else 0.8f
            if (trainerTimer >= 110 && dist <= 155f) {
                trainerTimer = 0
                input1.press(Bits.HOOK)
            }
        }
    }

    private fun trackTutorial() {
        travel += abs(p1.x - lastX)
        lastX = p1.x
        if (p1.state != lastP1State) {
            when (p1.state) {
                St.HOOK -> hookUsed++
                St.UPPER -> upperUsed++
                St.DODGE -> dodgeUsed++
                St.SPECIAL -> specialUsed++
            }
            lastP1State = p1.state
        }
        if (tutorialDone) return
        var next = false
        when (tutorialStep) {
            0 -> next = travel >= 260f
            1 -> next = p1.thrown - stepBase >= 3
            2 -> next = hookUsed >= 1 && upperUsed >= 1
            3 -> { trainerMode = 1; next = p1.perfects >= 1 }
            4 -> { trainerMode = 0; next = dodgeUsed >= 1 }
            5 -> {
                if (p1.meter < 100f && specialUsed == 0) p1.meter = 100f
                next = specialUsed >= 1
            }
        }
        if (next) {
            Sound.play("coin", 0.7f)
            tutorialStep++
            stepBase = p1.thrown
            if (tutorialStep >= Teks.tutorial.size) {
                tutorialDone = true
                trainerMode = 0
                toast("Latihan selesai. Berlatihlah sebebasnya.")
            }
        }
    }

    fun render(c: Canvas, w: Int, h: Int) {
        val s = h / 720f
        val halfW = w / (2f * s)
        c.drawColor(Pal.SOGAN)
        val tr = effects.trauma
        val sh = tr * tr * 16f * s
        val ox = sin(anim * 63f) * sh
        val oy = sin(anim * 51f + 1f) * sh
        c.save()
        c.translate(w / 2f + ox, oy)
        c.scale(s, s)
        arena.drawBackground(c, halfW)
        arena.drawCrowd(c, anim, halfW, cheer)
        arena.drawRing(c, anim)
        val first = if (p1.isAttacking || p1.state == St.HIT) p2 else p1
        val second = if (first === p1) p2 else p1
        fighterRenderer.draw(c, first, first.x, ArenaRenderer.GROUND, anim)
        fighterRenderer.draw(c, second, second.x, ArenaRenderer.GROUND, anim)
        effects.draw(c)
        arena.drawForegroundCrowd(c, anim, halfW, cheer)
        c.restore()
        if (effects.flashAlpha > 0f) {
            paint.style = Paint.Style.FILL
            paint.color = Color.argb((255 * effects.flashAlpha).toInt(), 255, 255, 255)
            c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        }
        hud.draw(c, w, h, this, link?.rttMs ?: -1)
        if (match.phase != Ph.MATCH_END && !paused) touch.draw(c, fighterOf(localSlot).meter, anim)
    }

    fun fighterOf(slot: Int): Fighter = if (slot == 0) p1 else p2

    fun release() {
        arena.release()
    }
}
