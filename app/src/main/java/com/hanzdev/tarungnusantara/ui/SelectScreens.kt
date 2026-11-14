package com.hanzdev.tarungnusantara.ui

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import com.hanzdev.tarungnusantara.audio.Sound
import com.hanzdev.tarungnusantara.data.Career
import com.hanzdev.tarungnusantara.data.Arcade
import com.hanzdev.tarungnusantara.data.Save
import com.hanzdev.tarungnusantara.data.Shop
import com.hanzdev.tarungnusantara.data.Teks
import com.hanzdev.tarungnusantara.game.ArenaDef
import com.hanzdev.tarungnusantara.game.FighterDef
import com.hanzdev.tarungnusantara.game.Level
import com.hanzdev.tarungnusantara.game.Mode
import com.hanzdev.tarungnusantara.game.Pal
import com.hanzdev.tarungnusantara.game.Roster
import com.hanzdev.tarungnusantara.game.SessionConfig
import com.hanzdev.tarungnusantara.game.loadAssetBitmap

internal fun fighterView(ctx: Context, def: FighterDef, bust: Boolean, own: Boolean): FighterView {
    val v = FighterView(ctx)
    v.bust = bust
    val lk = if (own) Shop.lookFor(def) else Pair(def.look, 0)
    v.setFighter(def, lk.first, lk.second)
    v.contentDescription = "Gambar ${def.nama}"
    return v
}

internal fun currentPlayer(): FighterDef {
    val d = Roster.byId(Save.lastFighter)
    return if (Save.isUnlocked(d.id)) d else Roster.bimo
}

private fun vbox(ctx: Context): LinearLayout {
    val l = LinearLayout(ctx)
    l.orientation = LinearLayout.VERTICAL
    return l
}

private fun hbox(ctx: Context): LinearLayout {
    val l = LinearLayout(ctx)
    l.orientation = LinearLayout.HORIZONTAL
    return l
}

class FighterPickScreen(private val allowLocked: Boolean, private val onPick: (FighterDef) -> Unit) : Screen() {
    private var selected: FighterDef = currentPlayer()
    private val cards = ArrayList<Pair<FighterDef, FrameLayout>>()
    private lateinit var detail: LinearLayout

    override fun build(ctx: Context): View {
        cards.clear()
        val root = hbox(ctx)
        val grid = vbox(ctx)
        val perRow = 5
        var row = hbox(ctx)
        for ((i, def) in Roster.all.withIndex()) {
            if (i % perRow == 0) {
                row = hbox(ctx)
                grid.addView(row)
            }
            val locked = !allowLocked && !Save.isUnlocked(def.id)
            val card = FrameLayout(ctx)
            val fv = fighterView(ctx, def, true, false)
            fv.dimmed = locked
            card.addView(fv, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(70), Gravity.TOP))
            val name = Ui.text(ctx, Roster.shortName(def), 10.5f, Pal.KREM, 1)
            name.gravity = Gravity.CENTER
            card.addView(name, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM).also { it.bottomMargin = Ui.dp(6) })
            if (locked) {
                val lock = Ui.text(ctx, "Terkunci", 9.5f, Pal.KUNYIT, 1)
                lock.gravity = Gravity.CENTER
                card.addView(lock, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
            }
            card.isClickable = true
            card.isFocusable = true
            card.contentDescription = "${def.nama}, ${def.daerah}${if (locked) ", terkunci" else ""}"
            card.setOnClickListener {
                Sound.play("click")
                selected = def
                refresh(ctx)
            }
            val lp = LinearLayout.LayoutParams(Ui.dp(80), Ui.dp(100))
            lp.setMargins(Ui.dp(3), Ui.dp(3), Ui.dp(3), Ui.dp(3))
            row.addView(card, lp)
            cards.add(Pair(def, card))
        }
        val gridScroll = ScrollView(ctx)
        gridScroll.addView(grid)
        root.addView(gridScroll, Ui.lp(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        root.addView(Ui.spacer(ctx, 10, 1))
        detail = vbox(ctx)
        val ds = ScrollView(ctx)
        ds.addView(detail)
        root.addView(ds, Ui.lp(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.15f))
        refresh(ctx)
        return Ui.frame(ctx, "Pilih Petarung", { nav.pop() }, false, root)
    }

    private fun refresh(ctx: Context) {
        for ((def, card) in cards) {
            card.background = Ui.box(0xFF1D2A4F.toInt(), if (def.id == selected.id) Pal.KUNYIT else 0x55E3A72F, if (def.id == selected.id) 3 else 1, 10)
        }
        val d = selected
        val locked = !allowLocked && !Save.isUnlocked(d.id)
        detail.removeAllViews()
        val p = Ui.panel(ctx, 12)
        p.addView(Ui.text(ctx, d.nama, 22f, Pal.KUNYIT, 2))
        p.addView(Ui.text(ctx, "${d.daerah}  \u2022  ${d.gaya}", 12.5f, Pal.KREM, 1))
        p.addView(Ui.spacer(ctx, 1, 6))
        val mid = hbox(ctx)
        val fv = fighterView(ctx, d, false, !locked)
        fv.dimmed = locked
        mid.addView(fv, Ui.lp(Ui.dp(110), Ui.dp(150)))
        mid.addView(Ui.spacer(ctx, 8, 1))
        val stats = vbox(ctx)
        stats.addView(Ui.statBar(ctx, "Kekuatan", d.stats.power))
        stats.addView(Ui.statBar(ctx, "Kecepatan", d.stats.speed))
        stats.addView(Ui.statBar(ctx, "Pertahanan", d.stats.defense))
        stats.addView(Ui.statBar(ctx, "Stamina", d.stats.stamina))
        stats.addView(Ui.statBar(ctx, "Counter", d.stats.counter))
        mid.addView(stats, Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        p.addView(mid)
        p.addView(Ui.spacer(ctx, 1, 6))
        p.addView(Ui.text(ctx, "Jurus: ${d.special.nama}", 14f, Pal.KUNYIT, 1))
        p.addView(Ui.text(ctx, d.special.deskripsi, 12f, Pal.KREM, 0))
        p.addView(Ui.spacer(ctx, 1, 6))
        p.addView(Ui.text(ctx, "\u201C${d.dialogBuka}\u201D", 12f, 0xFFC9BCA2.toInt(), 0))
        p.addView(Ui.spacer(ctx, 1, 8))
        if (locked) {
            p.addView(Ui.text(ctx, "Cara membuka: ${d.caraBuka}", 12.5f, Pal.KUNYIT, 1))
            p.addView(Ui.spacer(ctx, 1, 6))
        }
        p.addView(Ui.button(ctx, if (locked) "Terkunci" else "Pilih ${Roster.shortName(d)}", Kind.PRIMARY, !locked) { onPick(d) })
        detail.addView(p)
    }
}

class ArenaPickScreen(private val current: String, private val onPick: (ArenaDef) -> Unit) : Screen() {
    private var selected = Roster.arenaById(current)
    private val cards = ArrayList<Pair<ArenaDef, LinearLayout>>()

    override fun build(ctx: Context): View {
        cards.clear()
        val c = vbox(ctx)
        val row = hbox(ctx)
        for (a in Roster.arenas) {
            val card = vbox(ctx)
            card.setPadding(Ui.dp(6), Ui.dp(6), Ui.dp(6), Ui.dp(8))
            val iv = ImageView(ctx)
            val bmp = loadAssetBitmap(ctx, "arena/${a.id}.webp", 4)
            if (bmp != null) iv.setImageDrawable(BitmapDrawable(ctx.resources, bmp))
            iv.scaleType = ImageView.ScaleType.CENTER_CROP
            iv.contentDescription = "Gambar arena ${a.nama}"
            card.addView(iv, Ui.lp(Ui.dp(200), Ui.dp(92)))
            card.addView(Ui.spacer(ctx, 1, 6))
            card.addView(Ui.text(ctx, a.nama, 14.5f, Pal.KUNYIT, 2))
            card.addView(Ui.text(ctx, a.daerah, 11.5f, Pal.KREM, 1))
            card.addView(Ui.text(ctx, a.suasana, 11f, Pal.KREM, 0))
            card.addView(Ui.text(ctx, a.ciri, 11f, 0xFFC9BCA2.toInt(), 0))
            card.isClickable = true
            card.isFocusable = true
            card.contentDescription = "${a.nama}, ${a.daerah}. ${a.suasana}"
            card.setOnClickListener { Sound.play("click"); selected = a; refresh() }
            val lp = LinearLayout.LayoutParams(Ui.dp(212), ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.rightMargin = Ui.dp(10)
            row.addView(card, lp)
            cards.add(Pair(a, card))
        }
        refresh()
        val hs = HorizontalScrollView(ctx)
        hs.addView(row)
        c.addView(hs, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        c.addView(Ui.button(ctx, "Pilih Arena", Kind.PRIMARY) { onPick(selected) })
        return Ui.frame(ctx, "Pilih Arena", { nav.pop() }, false, c)
    }

    private fun refresh() {
        for ((a, card) in cards) {
            card.background = Ui.box(0xE61D2A4F.toInt(), if (a.id == selected.id) Pal.KUNYIT else 0x55E3A72F, if (a.id == selected.id) 3 else 1, 12)
        }
    }
}

class QuickSetupScreen(
    private val playerId: String,
    private var oppId: String? = null,
    private var arenaId: String = "keraton",
    private var level: Int = Save.aiDefault,
    private var rounds: Int = 3
) : Screen() {
    private fun again(): QuickSetupScreen = QuickSetupScreen(playerId, oppId, arenaId, level, rounds)

    override fun build(ctx: Context): View {
        val opp = Roster.byId(oppId ?: Roster.starters.first { it.id != playerId }.id)
        oppId = opp.id
        val me = Roster.byId(playerId)
        val root = hbox(ctx)
        val vs = hbox(ctx)
        vs.gravity = Gravity.CENTER
        vs.addView(fighterView(ctx, me, false, true), Ui.lp(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        vs.addView(Ui.text(ctx, "VS", 22f, Pal.CABAI, 2).also { it.gravity = Gravity.CENTER })
        vs.addView(fighterView(ctx, opp, false, false).also { it.scaleX = -1f }, Ui.lp(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        root.addView(vs, Ui.lp(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        root.addView(Ui.spacer(ctx, 10, 1))

        val p = Ui.panel(ctx, 14)
        p.addView(Ui.text(ctx, "Lawan", 12f, 0xFFC9BCA2.toInt(), 0))
        p.addView(Ui.button(ctx, "${opp.nama}  (ganti)", Kind.SECONDARY) {
            nav.push(FighterPickScreen(true) { d -> oppId = d.id; nav.pop(); nav.replace(again()) })
        })
        p.addView(Ui.spacer(ctx, 1, 8))
        p.addView(Ui.text(ctx, "Arena", 12f, 0xFFC9BCA2.toInt(), 0))
        p.addView(Ui.button(ctx, "${Roster.arenaById(arenaId).nama}  (ganti)", Kind.SECONDARY) {
            nav.push(ArenaPickScreen(arenaId) { a -> arenaId = a.id; nav.pop(); nav.replace(again()) })
        })
        p.addView(Ui.spacer(ctx, 1, 8))
        p.addView(Ui.text(ctx, "Tingkat kesulitan", 12f, 0xFFC9BCA2.toInt(), 0))
        val hint = Ui.text(ctx, Level.hints[level], 11.5f, Pal.KREM, 0)
        p.addView(Widgets.choice(ctx, Level.names.toList(), level) { level = it; hint.text = Level.hints[it] })
        p.addView(hint)
        p.addView(Ui.spacer(ctx, 1, 8))
        p.addView(Ui.text(ctx, "Jumlah ronde", 12f, 0xFFC9BCA2.toInt(), 0))
        val opts = listOf(1, 3, 5)
        p.addView(Widgets.choice(ctx, opts.map { "$it ronde" }, opts.indexOf(rounds).coerceAtLeast(0)) { rounds = opts[it] })
        p.addView(Ui.spacer(ctx, 1, 12))
        p.addView(Ui.button(ctx, "Lanjut", Kind.PRIMARY) {
            val cfg = SessionConfig(Mode.QUICK, playerId, opp.id, arenaId, level, rounds)
            nav.push(PrepScreen(cfg, false))
        })
        val sv = ScrollView(ctx)
        sv.addView(p)
        root.addView(sv, Ui.lp(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.2f))
        return Ui.frame(ctx, "Duel Cepat", { nav.pop() }, false, root)
    }
}

class PrepScreen(private val cfg: SessionConfig, private val career: Boolean) : Screen() {
    private val use = BooleanArray(3)

    override fun build(ctx: Context): View {
        val me = Roster.byId(cfg.p1)
        val opp = Roster.byId(cfg.p2)
        val arena = Roster.arenaById(cfg.arenaId)
        val root = hbox(ctx)
        val left = vbox(ctx)
        val vs = hbox(ctx)
        vs.gravity = Gravity.CENTER
        vs.addView(fighterView(ctx, me, true, true), Ui.lp(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        vs.addView(Ui.text(ctx, "VS", 22f, Pal.CABAI, 2).also { it.gravity = Gravity.CENTER })
        vs.addView(fighterView(ctx, opp, true, false).also { it.scaleX = -1f }, Ui.lp(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        left.addView(vs, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        left.addView(Ui.text(ctx, "${me.nama} melawan ${opp.nama}", 14f, Pal.KREM, 1).also { it.gravity = Gravity.CENTER })
        left.addView(Ui.text(ctx, "${arena.nama}  \u2022  ${Level.names[cfg.level]}  \u2022  ${cfg.rounds} ronde", 12f, Pal.KUNYIT, 1).also { it.gravity = Gravity.CENTER })
        root.addView(left, Ui.lp(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        root.addView(Ui.spacer(ctx, 10, 1))

        val p = Ui.panel(ctx, 14)
        p.addView(Ui.text(ctx, "Jamu sebelum bertanding", 16f, Pal.KUNYIT, 2))
        p.addView(Ui.text(ctx, "Satu jamu terpakai tiap kali dipilih dan pertandingan dimulai.", 11.5f, Pal.KREM, 0))
        p.addView(Ui.spacer(ctx, 1, 6))
        val ids = arrayOf("kunyit", "kencur", "temulawak")
        for ((i, j) in Shop.jamu.withIndex()) {
            val n = Save.jamu(j.id)
            val label = if (n > 0) "${j.nama} (punya $n)" else "${j.nama} (habis)"
            val chip = Ui.chip(ctx, label, false) {}
            if (n <= 0) {
                chip.alpha = 0.55f
                chip.setOnClickListener { nav.toast("Beli jamu di Toko.") }
            } else {
                chip.setOnClickListener {
                    Sound.play("click")
                    use[i] = !use[i]
                    Widgets.styleChip(chip, use[i])
                    chip.contentDescription = "$label${if (use[i]) ", dipilih" else ""}"
                }
            }
            p.addView(chip)
            p.addView(Ui.text(ctx, j.efek, 11f, 0xFFC9BCA2.toInt(), 0).also { it.setPadding(Ui.dp(8), Ui.dp(1), 0, Ui.dp(6)) })
        }
        if (career && !Save.lastriClaimed(cfg.chapter)) {
            p.addView(Ui.spacer(ctx, 1, 4))
            p.addView(Ui.text(ctx, "Ibu Lastri, penjual jamu di arena ini, menawarkan satu jamu kunyit gratis.", 12f, Pal.KREM, 0))
            p.addView(Ui.button(ctx, "Terima Jamu dari Ibu Lastri", Kind.SECONDARY) {
                Save.setJamu("kunyit", Save.jamu("kunyit") + 1)
                Save.setLastriClaimed(cfg.chapter)
                Sound.play("coin")
                nav.replace(PrepScreen(cfg, career))
            })
        }
        p.addView(Ui.spacer(ctx, 1, 10))
        p.addView(Ui.button(ctx, "Mulai Tarung", Kind.PRIMARY) {
            for (i in 0 until 3) if (use[i]) Save.setJamu(ids[i], Save.jamu(ids[i]) - 1)
            nav.startGame(cfg.withJamu(use[0], use[1], use[2]))
        })
        val sv = ScrollView(ctx)
        sv.addView(p)
        root.addView(sv, Ui.lp(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        return Ui.frame(ctx, "Siap Bertanding", { nav.pop() }, false, root)
    }
}

class StoryScreen(
    private val title: String,
    private val body: String,
    private val quote: String?,
    private val label: String,
    private val onContinue: () -> Unit
) : Screen() {
    override fun build(ctx: Context): View {
        val p = Ui.panel(ctx, 20)
        p.addView(Ui.text(ctx, body, 15f, Pal.KREM, 0))
        if (quote != null) {
            p.addView(Ui.spacer(ctx, 1, 12))
            p.addView(Ui.text(ctx, "\u201C$quote\u201D", 15f, Pal.KUNYIT, 2))
        }
        p.addView(Ui.spacer(ctx, 1, 16))
        p.addView(Ui.button(ctx, label, Kind.PRIMARY) { onContinue() })
        val sv = ScrollView(ctx)
        sv.addView(p)
        return Ui.frame(ctx, title, { nav.pop() }, false, sv)
    }
}

private fun playerPanel(ctx: Context, nav: Nav, onChanged: () -> Unit): LinearLayout {
    val def = currentPlayer()
    val l = vbox(ctx)
    l.gravity = Gravity.CENTER_HORIZONTAL
    l.addView(fighterView(ctx, def, false, true), Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
    l.addView(Ui.text(ctx, def.nama, 17f, Pal.KUNYIT, 2).also { it.gravity = Gravity.CENTER })
    l.addView(Ui.text(ctx, "${def.daerah}  \u2022  ${def.gaya}", 11.5f, Pal.KREM, 0).also { it.gravity = Gravity.CENTER })
    l.addView(Ui.spacer(ctx, 1, 6))
    l.addView(Ui.button(ctx, "Ganti Petarung", Kind.SECONDARY) {
        nav.push(FighterPickScreen(false) { d -> Save.lastFighter = d.id; nav.pop(); onChanged() })
    })
    return l
}

class CareerScreen : Screen() {
    override fun build(ctx: Context): View {
        val root = hbox(ctx)
        root.addView(playerPanel(ctx, nav) { nav.replace(CareerScreen()) }, Ui.lp(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.8f))
        root.addView(Ui.spacer(ctx, 10, 1))
        val list = vbox(ctx)
        val done = Save.chaptersDone
        val player = currentPlayer()
        for (n in 1..6) {
            val ch = Career.chapter(n)
            val card = Ui.panel(ctx, 12)
            val status = when { n <= done -> "Selesai"; n == done + 1 -> "Sedang berjalan"; else -> "Terkunci" }
            val head = hbox(ctx)
            head.gravity = Gravity.CENTER_VERTICAL
            head.addView(Ui.text(ctx, "Bab $n: ${ch.daerah}", 17f, Pal.KUNYIT, 2), Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            head.addView(Ui.text(ctx, status, 11.5f, if (n <= done) Pal.KUNYIT else Pal.KREM, 1))
            card.addView(head)
            card.addView(Ui.text(ctx, "${ch.arena}  \u2022  Laga penentu: ${ch.boss}", 11.5f, Pal.KREM, 1))
            card.addView(Ui.spacer(ctx, 1, 4))
            card.addView(Ui.text(ctx, ch.cerita, 12f, 0xFFD9CDB4.toInt(), 0))
            card.addView(Ui.spacer(ctx, 1, 8))
            val frontier = n == done + 1
            val label = when {
                n > done + 1 -> "Terkunci: selesaikan Bab ${n - 1}"
                frontier && Save.careerMatch == 0 && n == 1 && done == 0 -> "Mulai Karier"
                frontier -> "Lanjut: laga ${Save.careerMatch + 1} dari 4"
                else -> "Main Ulang Bab $n"
            }
            card.addView(Ui.button(ctx, label, if (frontier) Kind.PRIMARY else Kind.SECONDARY, n <= done + 1) {
                val idx = if (frontier) Save.careerMatch else 0
                val cfg = Career.config(n, idx, player.id)
                val toPrep = { nav.push(PrepScreen(cfg, true)) }
                if (n == 1 && done == 0 && idx == 0) {
                    nav.push(StoryScreen("Prolog", Teks.PROLOG, Teks.PROLOG_KUTIPAN, "Lanjut") {
                        nav.push(StoryScreen("Bab 1: ${ch.daerah}", ch.cerita, null, "Siapkan Laga") { toPrep() })
                    })
                } else if (idx == 0) {
                    nav.push(StoryScreen("Bab $n: ${ch.daerah}", ch.cerita, null, "Siapkan Laga") { toPrep() })
                } else {
                    toPrep()
                }
            })
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = Ui.dp(8)
            list.addView(card, lp)
        }
        val sv = ScrollView(ctx)
        sv.addView(list)
        root.addView(sv, Ui.lp(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.6f))
        return Ui.frame(ctx, "Karier", { nav.pop() }, true, root)
    }
}

class ArcadeScreen : Screen() {
    override fun build(ctx: Context): View {
        val root = hbox(ctx)
        root.addView(playerPanel(ctx, nav) { nav.replace(ArcadeScreen()) }, Ui.lp(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.8f))
        root.addView(Ui.spacer(ctx, 10, 1))
        val p = Ui.panel(ctx, 16)
        p.addView(Ui.text(ctx, "Arcade", 22f, Pal.KUNYIT, 2))
        p.addView(Ui.spacer(ctx, 1, 6))
        p.addView(Ui.text(ctx, "Lawan datang berturut-turut di arena acak. Kalah sekali, rangkaian berakhir.", 13.5f, Pal.KREM, 0))
        p.addView(Ui.text(ctx, "Setiap 5 kemenangan, lawan naik satu tingkat dan kamu mendapat 150 koin.", 13.5f, Pal.KREM, 0))
        p.addView(Ui.spacer(ctx, 1, 10))
        p.addView(Ui.text(ctx, "Skor terbaik: ${Save.bestArcade}", 16f, Pal.KUNYIT, 1))
        p.addView(Ui.text(ctx, if (Save.bestArcade == 0) "Belum ada skor. Skor dihitung 100 poin per kemenangan." else "Skor dihitung 100 poin per kemenangan.", 11.5f, 0xFFC9BCA2.toInt(), 0))
        p.addView(Ui.spacer(ctx, 1, 14))
        p.addView(Ui.button(ctx, "Mulai Arcade", Kind.PRIMARY) { nav.startGame(Arcade.next(currentPlayer().id, 0)) })
        val sv = ScrollView(ctx)
        sv.addView(p)
        root.addView(sv, Ui.lp(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.4f))
        return Ui.frame(ctx, "Arcade", { nav.pop() }, true, root)
    }
}

class TrainingScreen : Screen() {
    override fun build(ctx: Context): View {
        val root = hbox(ctx)
        root.addView(playerPanel(ctx, nav) { nav.replace(TrainingScreen()) }, Ui.lp(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.8f))
        root.addView(Ui.spacer(ctx, 10, 1))
        val p = Ui.panel(ctx, 16)
        p.addView(Ui.text(ctx, "Latihan bersama Pak Guru Wiryo", 18f, Pal.KUNYIT, 2))
        p.addView(Ui.spacer(ctx, 1, 6))
        p.addView(Ui.text(ctx, "Enam langkah singkat: gerak, jab, hook dan uppercut, tangkisan sempurna, hindar, lalu jurus spesial. Setelah itu kamu bebas berlatih melawan kantong tinju.", 13.5f, Pal.KREM, 0))
        p.addView(Ui.spacer(ctx, 1, 6))
        p.addView(Ui.text(ctx, "Latihan tidak memberi koin dan tidak mengubah kemajuan karier.", 12f, 0xFFC9BCA2.toInt(), 0))
        p.addView(Ui.spacer(ctx, 1, 14))
        p.addView(Ui.button(ctx, "Mulai Latihan", Kind.PRIMARY) {
            nav.startGame(SessionConfig(Mode.TRAINING, currentPlayer().id, "bonar", "keraton", 0, 1))
        })
        val sv = ScrollView(ctx)
        sv.addView(p)
        root.addView(sv, Ui.lp(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.4f))
        return Ui.frame(ctx, "Latihan", { nav.pop() }, false, root)
    }
}
