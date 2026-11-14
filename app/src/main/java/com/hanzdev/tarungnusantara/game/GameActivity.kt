package com.hanzdev.tarungnusantara.game

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import com.hanzdev.tarungnusantara.audio.Commentator
import com.hanzdev.tarungnusantara.audio.Sound
import com.hanzdev.tarungnusantara.bluetooth.BtSession
import com.hanzdev.tarungnusantara.bluetooth.Msg
import com.hanzdev.tarungnusantara.bluetooth.Packet
import com.hanzdev.tarungnusantara.data.Achievements
import com.hanzdev.tarungnusantara.data.Arcade
import com.hanzdev.tarungnusantara.data.Career
import com.hanzdev.tarungnusantara.data.MatchSummary
import com.hanzdev.tarungnusantara.data.Save
import com.hanzdev.tarungnusantara.data.Teks
import com.hanzdev.tarungnusantara.ui.Kind
import com.hanzdev.tarungnusantara.ui.Ui

class GameActivity : Activity() {
    private lateinit var gameView: GameView
    private lateinit var overlay: FrameLayout
    private var session: GameSession? = null
    private var commentator: Commentator? = null
    private lateinit var cfg: SessionConfig

    private var resultShown = false
    private var pauseShown = false
    private var rewardApplied = false
    private var opponentWantsRematch = false
    private var rematchSent = false
    private var opponentLeft = false
    private var lostConnection = false
    private var resultBuilder: (() -> Unit)? = null

    private val isNet: Boolean get() = cfg.mode == Mode.BT_HOST || cfg.mode == Mode.BT_CLIENT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Save.init(this)
        Fonts.init(this)
        Ui.init(this)
        Sound.init(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        cfg = SessionConfig.from(intent)
        commentator = Commentator(this)
        gameView = GameView(this)
        overlay = FrameLayout(this)
        val root = FrameLayout(this)
        root.addView(gameView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        root.addView(overlay, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        setContentView(root)
        hideSystemUi()
        startSession(cfg)
    }

    @Suppress("DEPRECATION")
    private fun hideSystemUi() {
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUi()
    }

    private fun startSession(c: SessionConfig) {
        cfg = c
        overlay.removeAllViews()
        resultShown = false
        pauseShown = false
        rewardApplied = false
        opponentWantsRematch = false
        rematchSent = false
        val s = GameSession(this, c, commentator)
        s.onMatchEnded = { runOnUiThread { showResult(s) } }
        s.onControlPacket = { pk -> runOnUiThread { onControl(pk) } }
        s.onConnectionLost = { runOnUiThread { showConnectionLost() } }
        s.touch.onPause = { runOnUiThread { showPause() } }
        session?.release()
        session = s
        gameView.session = s
        Sound.playMusic(Roster.arenaById(c.arenaId).musik)
    }

    private fun showPanel(content: View, widthDp: Int = 420) {
        overlay.removeAllViews()
        val dim = FrameLayout(this)
        dim.setBackgroundColor(0xB3231710.toInt())
        dim.isClickable = true
        val scroll = ScrollView(this)
        scroll.isFillViewport = false
        val panel = Ui.panel(this, 18)
        panel.addView(content)
        scroll.addView(panel)
        val lp = FrameLayout.LayoutParams(Ui.dp(widthDp), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        lp.topMargin = Ui.dp(12)
        lp.bottomMargin = Ui.dp(12)
        dim.addView(scroll, lp)
        overlay.addView(dim, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    private fun col(): LinearLayout {
        val l = LinearLayout(this)
        l.orientation = LinearLayout.VERTICAL
        return l
    }

    private fun gap(h: Int): View = Ui.spacer(this, 1, h)

    private fun showPause() {
        val s = session ?: return
        if (resultShown || pauseShown || lostConnection) return
        pauseShown = true
        if (!isNet) s.pauseGame(true)
        val c = col()
        c.addView(Ui.text(this, "Jeda", 26f, Pal.KUNYIT, 2))
        if (isNet) c.addView(Ui.text(this, "Pertandingan Bluetooth tetap berjalan saat jendela ini terbuka.", 12f))
        c.addView(gap(14))
        c.addView(Ui.button(this, if (isNet) "Kembali ke Pertandingan" else "Lanjutkan", Kind.PRIMARY) {
            overlay.removeAllViews()
            pauseShown = false
            if (!isNet) s.pauseGame(false)
        })
        c.addView(gap(8))
        c.addView(Ui.button(this, if (isNet) "Tinggalkan Pertandingan" else "Keluar ke Menu") { leave() })
        showPanel(c, 340)
    }

    private fun leave() {
        if (isNet) {
            BtSession.link?.send(Msg.LEAVE, ByteArray(0))
            BtSession.reset()
        }
        finish()
    }

    private fun showConnectionLost() {
        if (lostConnection) return
        lostConnection = true
        val c = col()
        c.addView(Ui.text(this, "Koneksi Terputus", 26f, Pal.KUNYIT, 2))
        c.addView(gap(6))
        c.addView(Ui.text(this, "Sambungan Bluetooth tidak bisa dipulihkan dalam 5 detik. Pertandingan dibatalkan dan tidak dihitung.", 13f))
        c.addView(gap(14))
        c.addView(Ui.button(this, "Kembali ke Menu", Kind.PRIMARY) { leave() })
        showPanel(c, 380)
    }

    private fun onControl(pk: Packet) {
        when (pk.type) {
            Msg.REMATCH_REQ -> { opponentWantsRematch = true; resultBuilder?.invoke() }
            Msg.REMATCH_OK -> if (cfg.mode == Mode.BT_HOST && rematchSent) hostStartRematch()
            Msg.START -> if (cfg.mode == Mode.BT_CLIENT) startSession(cfg)
            Msg.LEAVE -> { opponentLeft = true; if (resultShown) resultBuilder?.invoke() else showConnectionLost() }
        }
    }

    private fun hostStartRematch() {
        BtSession.link?.send(Msg.START, ByteArray(0))
        startSession(cfg)
    }

    private fun onRematchPressed() {
        val link = BtSession.link ?: return
        if (opponentWantsRematch) {
            if (cfg.mode == Mode.BT_HOST) hostStartRematch() else link.send(Msg.REMATCH_OK, ByteArray(0))
        } else {
            rematchSent = true
            link.send(Msg.REMATCH_REQ, ByteArray(0))
            resultBuilder?.invoke()
        }
    }

    private fun showResult(s: GameSession) {
        if (resultShown || isFinishing || lostConnection) return
        resultShown = true
        pauseShown = false
        val m = s.match
        val me = s.fighterOf(s.localSlot)
        val won = m.winner == s.localSlot
        val draw = m.winner < 0
        val byKo = m.method == "KO" || m.method == "TKO"

        var coins = 0
        val lines = ArrayList<String>()
        var unlock: String? = null
        var cleared = false

        if (!rewardApplied) {
            rewardApplied = true
            if (won) {
                Save.totalWins = Save.totalWins + 1
                if (byKo) Save.koWins = Save.koWins + 1
            }
            if (isNet) {
                Save.btMatches = Save.btMatches + 1
                if (!draw) {
                    val hostWon = m.winner == 0
                    if (hostWon) BtSession.scoreHost++ else BtSession.scoreClient++
                }
            }
            if (cfg.mode == Mode.CAREER && won) {
                val boss = cfg.matchIdx >= 3
                val base = if (boss) 300 else 100
                coins += base
                lines.add(if (boss) "Laga penentu: +$base" else "Menang: +$base")
                if (byKo) { coins += 50; lines.add("Menang KO: +50") }
                if (me.hitsTaken == 0) { coins += 100; lines.add("Tanpa terkena pukulan: +100") }
                val frontier = cfg.chapter == Save.chaptersDone + 1
                val nextIdx = cfg.matchIdx + 1
                cleared = nextIdx >= 4
                if (frontier) {
                    if (cleared) { Save.chaptersDone = cfg.chapter; Save.careerMatch = 0 } else Save.careerMatch = nextIdx
                }
                if (cleared && frontier) unlock = Career.unlockedAfter(cfg.chapter)
            }
            if (cfg.mode == Mode.ARCADE && won) {
                val wins = cfg.arcadeWins + 1
                lines.add("Rangkaian menang: $wins")
                if (wins % 5 == 0) { coins += 150; lines.add("Setiap 5 kemenangan: +150") }
                if (wins * 100 > Save.bestArcade) Save.bestArcade = wins * 100
            }
            if (coins > 0) Save.coins = Save.coins + coins
        }
        val hpFraction = if (me.maxHp > 0f) me.hp / me.maxHp else 0f
        val beatFinal = cfg.mode == Mode.CAREER && won && cfg.chapter == 6 && cfg.matchIdx >= 3
        val newAch = Achievements.check(MatchSummary(won, byKo, me.hitsTaken, me.perfects, hpFraction, isNet, beatFinal))
        if (newAch.isNotEmpty() || unlock != null) Sound.play("unlock")

        val winnerDef = if (draw) null else (if (m.winner == 0) s.p1.def else s.p2.def)
        val dealt = me.dmgDealt.toInt()
        val perfects = me.perfects
        val thrown = me.thrown
        val landed = me.landed

        resultBuilder = {
            val c = col()
            val title = when { draw -> "SERI"; won -> "MENANG"; else -> "KALAH" }
            c.addView(Ui.text(this, title, 32f, if (won) Pal.KUNYIT else Pal.KREM, 2))
            val how = when (m.method) {
                "KO" -> "KO di ronde ${m.round}"
                "TKO" -> "TKO: tiga kali jatuh dalam satu ronde"
                else -> if (draw) "Poin sama kuat" else "Keputusan poin"
            }
            c.addView(Ui.text(this, how, 14f, Pal.KREM, 1))
            if (winnerDef != null) {
                c.addView(gap(6))
                c.addView(Ui.text(this, "${winnerDef.nama}: \u201C${winnerDef.dialogMenang}\u201D", 12.5f, Pal.KREM, 0))
            }
            c.addView(gap(10))
            c.addView(Ui.text(this, "Damage ke lawan: $dealt   Pukulan: $thrown, kena $landed   Tangkisan sempurna: $perfects", 12.5f, Pal.KREM, 0))
            for (l in lines) c.addView(Ui.text(this, l, 13f, Pal.KUNYIT, 1))
            if (coins > 0) c.addView(Ui.text(this, "Total koin: +$coins", 14f, Pal.KUNYIT, 1))
            if (cfg.mode == Mode.CAREER && won && cleared) {
                c.addView(gap(6))
                c.addView(Ui.text(this, "Bab ${cfg.chapter} selesai.", 14f, Pal.KUNYIT, 1))
                if (cfg.chapter == 6) c.addView(Ui.text(this, Teks.EPILOG, 12.5f, Pal.KREM, 0))
            }
            if (unlock != null) c.addView(Ui.text(this, "Petarung baru terbuka: $unlock", 13.5f, Pal.KUNYIT, 1))
            for (a in newAch) c.addView(Ui.text(this, "Pencapaian baru: ${a.nama}", 13f, Pal.KUNYIT, 1))
            if (cfg.mode == Mode.ARCADE && !won) {
                c.addView(gap(6))
                c.addView(Ui.text(this, "Rangkaian berakhir di ${cfg.arcadeWins} kemenangan. Skor terbaik: ${Save.bestArcade}", 13f, Pal.KREM, 1))
            }
            if (isNet) {
                c.addView(gap(6))
                c.addView(Ui.text(this, "Skor sesi (Tuan rumah : Tamu)  ${BtSession.scoreHost} : ${BtSession.scoreClient}", 13f, Pal.KREM, 1))
                if (opponentLeft) c.addView(Ui.text(this, "Lawan meninggalkan pertandingan.", 13f, Pal.KUNYIT, 1))
                else if (opponentWantsRematch) c.addView(Ui.text(this, "Lawan mengajak rematch.", 13f, Pal.KUNYIT, 1))
                else if (rematchSent) c.addView(Ui.text(this, "Menunggu jawaban lawan...", 13f, Pal.KREM, 0))
            }
            c.addView(gap(14))
            addResultButtons(c, won, cleared)
            showPanel(c, 460)
        }
        resultBuilder?.invoke()
    }

    private fun addResultButtons(c: LinearLayout, won: Boolean, cleared: Boolean) {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        fun add(label: String, kind: Kind, enabled: Boolean = true, onClick: () -> Unit) {
            if (row.childCount > 0) row.addView(Ui.spacer(this, 10, 1))
            row.addView(Ui.button(this, label, kind, enabled, onClick), Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        when (cfg.mode) {
            Mode.CAREER -> {
                if (won && !cleared) {
                    add("Laga Berikutnya", Kind.PRIMARY) { startSession(Career.config(cfg.chapter, cfg.matchIdx + 1, cfg.p1)) }
                    add("Menu", Kind.SECONDARY) { finish() }
                } else if (won) {
                    add("Selesai", Kind.PRIMARY) { finish() }
                } else {
                    add("Coba Lagi", Kind.PRIMARY) { startSession(Career.config(cfg.chapter, cfg.matchIdx, cfg.p1)) }
                    add("Menu", Kind.SECONDARY) { finish() }
                }
            }
            Mode.ARCADE -> {
                if (won) {
                    add("Lawan Berikutnya", Kind.PRIMARY) { startSession(Arcade.next(cfg.p1, cfg.arcadeWins + 1)) }
                    add("Berhenti", Kind.SECONDARY) { finish() }
                } else {
                    add("Main Lagi", Kind.PRIMARY) { startSession(Arcade.next(cfg.p1, 0)) }
                    add("Menu", Kind.SECONDARY) { finish() }
                }
            }
            Mode.QUICK -> {
                add("Main Lagi", Kind.PRIMARY) { startSession(cfg) }
                add("Menu", Kind.SECONDARY) { finish() }
            }
            else -> {
                add(if (opponentWantsRematch) "Terima Rematch" else "Ajak Rematch", Kind.PRIMARY, !opponentLeft && !rematchSent) { onRematchPressed() }
                add("Keluar", Kind.SECONDARY) { leave() }
            }
        }
        c.addView(row)
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (resultShown || lostConnection) {
            if (isNet) leave() else finish()
        } else if (pauseShown) {
            overlay.removeAllViews()
            pauseShown = false
            if (!isNet) session?.pauseGame(false)
        } else {
            showPause()
        }
    }

    override fun onPause() {
        super.onPause()
        Sound.pauseMusic()
        if (!isNet && !resultShown && !isFinishing) showPause()
    }

    override fun onResume() {
        super.onResume()
        Sound.resumeMusic()
        hideSystemUi()
    }

    override fun onDestroy() {
        super.onDestroy()
        commentator?.shutdown()
        session?.release()
        session = null
    }
}
