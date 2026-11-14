package com.hanzdev.tarungnusantara.ui

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import com.hanzdev.tarungnusantara.audio.Sound
import com.hanzdev.tarungnusantara.data.Achievements
import com.hanzdev.tarungnusantara.data.Save
import com.hanzdev.tarungnusantara.data.Teks
import com.hanzdev.tarungnusantara.game.Level
import com.hanzdev.tarungnusantara.game.Pal
import com.hanzdev.tarungnusantara.game.loadAssetBitmap
import java.util.TimeZone

private fun logoView(ctx: Context): ImageView {
    val iv = ImageView(ctx)
    val bmp = loadAssetBitmap(ctx, "ui/logo.webp")
    if (bmp != null) iv.setImageDrawable(BitmapDrawable(ctx.resources, bmp))
    iv.scaleType = ImageView.ScaleType.FIT_CENTER
    iv.contentDescription = "Logo Tarung Nusantara"
    return iv
}

class SplashScreen : Screen() {
    override val music = "s_splash.ogg"
    override val musicLoop = false
    private val handler = Handler(Looper.getMainLooper())
    private var started = 0L
    private var done = false
    private lateinit var status: TextView

    override fun build(ctx: Context): View {
        val root = FrameLayout(ctx)
        root.setBackgroundColor(Pal.SOGAN)
        val col = LinearLayout(ctx)
        col.orientation = LinearLayout.VERTICAL
        col.gravity = Gravity.CENTER
        col.addView(logoView(ctx), Ui.lp(Ui.dp(360), Ui.dp(190)))
        col.addView(Ui.text(ctx, Teks.TAGLINE, 14f, Pal.KREM, 0).also { it.gravity = Gravity.CENTER })
        col.addView(Ui.spacer(ctx, 1, 14))
        col.addView(Ui.text(ctx, Teks.tips.random(), 12.5f, Pal.KREM, 0).also { it.gravity = Gravity.CENTER })
        col.addView(Ui.spacer(ctx, 1, 14))
        status = Ui.text(ctx, "Memuat suara...", 12f, Pal.KUNYIT, 1)
        status.gravity = Gravity.CENTER
        col.addView(status)
        col.addView(Ui.spacer(ctx, 1, 18))
        col.addView(Ui.text(ctx, "${Teks.DEVELOPER}  \u2022  Versi ${Teks.VERSI}", 11f, Pal.KREM, 0))
        root.addView(col, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER))
        root.setOnClickListener { if (Sound.ready) goNext() }
        return root
    }

    override fun onShow() {
        started = System.currentTimeMillis()
        done = false
        handler.post(object : Runnable {
            override fun run() {
                if (done) return
                val elapsed = System.currentTimeMillis() - started
                if (Sound.ready) {
                    status.text = if (Sound.failed) "Suara tidak bisa dimuat. Game tetap bisa dimainkan." else "Ketuk untuk mulai"
                    if (elapsed > 2600) { goNext(); return }
                } else if (elapsed > 6000) {
                    status.text = "Suara lambat dimuat. Melanjutkan tanpa menunggu."
                    goNext()
                    return
                }
                handler.postDelayed(this, 150)
            }
        })
    }

    private fun goNext() {
        if (done) return
        done = true
        nav.replace(MenuScreen())
    }

    override fun onHide() {
        done = true
        handler.removeCallbacksAndMessages(null)
    }
}

class MenuScreen : Screen() {
    private fun today(): Long {
        val now = System.currentTimeMillis()
        return (now + TimeZone.getDefault().getOffset(now)) / 86_400_000L
    }

    override fun build(ctx: Context): View {
        val root = LinearLayout(ctx)
        root.orientation = LinearLayout.HORIZONTAL
        root.setBackgroundColor(Pal.SOGAN)
        root.setPadding(Ui.dp(20), Ui.dp(14), Ui.dp(20), Ui.dp(10))

        val left = LinearLayout(ctx)
        left.orientation = LinearLayout.VERTICAL
        left.gravity = Gravity.CENTER_HORIZONTAL
        left.addView(logoView(ctx), Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        left.addView(Ui.text(ctx, Teks.TAGLINE, 12f, Pal.KREM, 0).also { it.gravity = Gravity.CENTER })
        left.addView(Ui.spacer(ctx, 1, 6))
        left.addView(Ui.text(ctx, "${Teks.DEVELOPER}  \u2022  Versi ${Teks.VERSI}", 11f, Pal.KUNYIT, 1))
        left.addView(Ui.spacer(ctx, 1, 6))
        left.addView(Ui.button(ctx, "Dukung Developer", Kind.GHOST) { nav.openUrl(Teks.DUKUNG_URL) })
        root.addView(left, Ui.lp(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        root.addView(Ui.spacer(ctx, 16, 1))

        val right = LinearLayout(ctx)
        right.orientation = LinearLayout.VERTICAL
        val top = LinearLayout(ctx)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL or Gravity.END
        val claimable = Save.lastDailyDay != today()
        val daily = Ui.button(ctx, if (claimable) "Hadiah Harian +50" else "Hadiah harian sudah diambil", if (claimable) Kind.PRIMARY else Kind.GHOST, claimable) {
            Save.coins = Save.coins + 50
            Save.lastDailyDay = today()
            Sound.play("coin")
            nav.toast("+50 koin. Datang lagi besok.")
            nav.replace(MenuScreen())
        }
        top.addView(daily)
        top.addView(Ui.spacer(ctx, 10, 1))
        top.addView(Ui.coinChip(ctx))
        right.addView(top, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        right.addView(Ui.spacer(ctx, 1, 10))

        val grid = GridLayout(ctx)
        grid.columnCount = 2
        val items = listOf(
            Triple("Karier", Kind.PRIMARY) { nav.push(CareerScreen()) },
            Triple("Arcade", Kind.SECONDARY) { nav.push(ArcadeScreen()) },
            Triple("Duel Cepat", Kind.SECONDARY) { nav.push(FighterPickScreen(false) { def -> Save.lastFighter = def.id; nav.push(QuickSetupScreen(def.id)) }) },
            Triple("Mabar Bluetooth", Kind.SECONDARY) { nav.push(BluetoothScreen()) },
            Triple("Latihan", Kind.SECONDARY) { nav.push(TrainingScreen()) },
            Triple("Toko", Kind.SECONDARY) { nav.push(ShopScreen()) },
            Triple("Pencapaian", Kind.SECONDARY) { nav.push(AchievementsScreen()) },
            Triple("Pengaturan", Kind.SECONDARY) { nav.push(SettingsScreen()) }
        )
        for ((i, entry) in items.withIndex()) {
            val b = Ui.button(ctx, entry.first, entry.second) { entry.third() }
            b.minHeight = Ui.dp(56)
            val lp = GridLayout.LayoutParams(GridLayout.spec(i / 2), GridLayout.spec(i % 2, 1f))
            lp.width = 0
            lp.setMargins(Ui.dp(5), Ui.dp(5), Ui.dp(5), Ui.dp(5))
            grid.addView(b, lp)
        }
        val sv = ScrollView(ctx)
        sv.isVerticalScrollBarEnabled = false
        sv.addView(grid)
        right.addView(sv, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(right, Ui.lp(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.15f))
        return root
    }

    override fun onBack(): Boolean = false
}

class AchievementsScreen : Screen() {
    override fun build(ctx: Context): View {
        val list = LinearLayout(ctx)
        list.orientation = LinearLayout.VERTICAL
        val unlocked = Achievements.list.count { Save.hasAchievement(it.id) }
        list.addView(Ui.text(ctx, "$unlocked dari ${Achievements.list.size} pencapaian terbuka", 13f, Pal.KREM, 1))
        list.addView(Ui.spacer(ctx, 1, 8))
        for (a in Achievements.list) {
            val has = Save.hasAchievement(a.id)
            val card = Ui.panel(ctx, 12)
            val row = LinearLayout(ctx)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            val badge = Ui.text(ctx, if (has) "Terbuka" else "Terkunci", 11f, if (has) Pal.SOGAN else Pal.KREM, 1)
            badge.gravity = Gravity.CENTER
            badge.setPadding(Ui.dp(10), Ui.dp(6), Ui.dp(10), Ui.dp(6))
            badge.background = Ui.box(if (has) Pal.KUNYIT else 0xFF3A3430.toInt(), Pal.KUNYIT, 1, 12)
            row.addView(badge, Ui.lp(Ui.dp(78), ViewGroup.LayoutParams.WRAP_CONTENT))
            row.addView(Ui.spacer(ctx, 12, 1))
            val info = LinearLayout(ctx)
            info.orientation = LinearLayout.VERTICAL
            info.addView(Ui.text(ctx, a.nama, 16f, Pal.KUNYIT, 2))
            info.addView(Ui.text(ctx, a.syarat, 12.5f, Pal.KREM, 0))
            val pr = Achievements.progress(a.id)
            if (pr != null && !has) info.addView(Ui.text(ctx, "Progres ${pr.first} dari ${pr.second}", 11.5f, Pal.NILA_TERANG, 1))
            row.addView(info, Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            card.addView(row)
            card.contentDescription = "${a.nama}. ${a.syarat}. ${if (has) "Terbuka" else "Terkunci"}"
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = Ui.dp(8)
            list.addView(card, lp)
        }
        val sv = ScrollView(ctx)
        sv.addView(list)
        return Ui.frame(ctx, "Pencapaian", { nav.pop() }, true, sv)
    }
}

class SettingsScreen : Screen() {
    private fun row(ctx: Context, label: String, hint: String?, control: View, controlWidthDp: Int = 0): View {
        val r = LinearLayout(ctx)
        r.orientation = LinearLayout.HORIZONTAL
        r.gravity = Gravity.CENTER_VERTICAL
        r.setPadding(0, Ui.dp(6), 0, Ui.dp(6))
        val t = LinearLayout(ctx)
        t.orientation = LinearLayout.VERTICAL
        t.addView(Ui.text(ctx, label, 14.5f, Pal.KREM, 1))
        if (hint != null) t.addView(Ui.text(ctx, hint, 11.5f, 0xFFC9BCA2.toInt(), 0))
        r.addView(t, Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        if (controlWidthDp > 0) r.addView(control, LinearLayout.LayoutParams(Ui.dp(controlWidthDp), ViewGroup.LayoutParams.WRAP_CONTENT))
        else r.addView(control)
        return r
    }

    private fun slider(ctx: Context, value: Int, onChange: (Int) -> Unit): SeekBar {
        val sb = SeekBar(ctx)
        sb.max = 100
        sb.progress = value
        sb.progressTintList = ColorStateList.valueOf(Pal.KUNYIT)
        sb.thumbTintList = ColorStateList.valueOf(Pal.KREM)
        sb.minimumHeight = Ui.dp(44)
        sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) { if (fromUser) onChange(p) }
            override fun onStartTrackingTouch(s: SeekBar) {}
            override fun onStopTrackingTouch(s: SeekBar) { Sound.play("click") }
        })
        return sb
    }

    override fun build(ctx: Context): View {
        val p = Ui.panel(ctx, 14)
        p.addView(row(ctx, "Suara efek", null, Widgets.toggle(ctx, "Suara efek", Save.sfxOn) { Save.sfxOn = it }))
        p.addView(row(ctx, "Volume efek", null, slider(ctx, Save.sfxVol) { Save.sfxVol = it }, 200))
        p.addView(row(ctx, "Musik", null, Widgets.toggle(ctx, "Musik", Save.musicOn) { Save.musicOn = it; Sound.applyMusicVolume() }))
        p.addView(row(ctx, "Volume musik", null, slider(ctx, Save.musicVol) { Save.musicVol = it; Sound.applyMusicVolume() }, 200))
        p.addView(row(ctx, "Suara komentator", "Membaca komentar singkat dengan suara bahasa Indonesia bila tersedia di perangkat", Widgets.toggle(ctx, "Suara komentator", Save.commentaryVoice) { Save.commentaryVoice = it }))
        p.addView(row(ctx, "Getar", "Getaran saat terkena pukulan", Widgets.toggle(ctx, "Getar", Save.haptic) { Save.haptic = it }))
        p.addView(row(ctx, "Kurangi gerak", "Meredam guncangan layar, kilatan, dan animasi menu", Widgets.toggle(ctx, "Kurangi gerak", Save.reduceMotion) { Save.reduceMotion = it }))
        p.addView(row(ctx, "Tingkat kesulitan bawaan", "Dipakai di Duel Cepat", Widgets.choice(ctx, Level.names.toList(), Save.aiDefault) { Save.aiDefault = it }))
        p.addView(row(ctx, "Posisi tombol", "Sisi tombol serangan di layar", Widgets.choice(ctx, listOf("Kanan", "Kiri"), if (Save.buttonsLeft) 1 else 0) { Save.buttonsLeft = it == 1 }))
        p.addView(row(ctx, "Bahasa", "Bahasa Inggris belum tersedia", Ui.text(ctx, "Indonesia", 14f, Pal.KUNYIT, 1)))
        p.addView(Ui.spacer(ctx, 1, 10))
        p.addView(Ui.button(ctx, "Tentang dan Dukungan", Kind.SECONDARY) { nav.push(AboutScreen()) })
        val sv = ScrollView(ctx)
        sv.addView(p)
        return Ui.frame(ctx, "Pengaturan", { nav.pop() }, false, sv)
    }
}

class AboutScreen : Screen() {
    override fun build(ctx: Context): View {
        val c = LinearLayout(ctx)
        c.orientation = LinearLayout.VERTICAL
        val p = Ui.panel(ctx, 16)
        p.addView(Ui.text(ctx, "Tarung Nusantara", 22f, Pal.KUNYIT, 2))
        p.addView(Ui.text(ctx, "Versi ${Teks.VERSI}", 13f, Pal.KREM, 1))
        p.addView(Ui.spacer(ctx, 1, 10))
        p.addView(Ui.text(ctx, "Developer", 12f, 0xFFC9BCA2.toInt(), 0))
        p.addView(Ui.text(ctx, Teks.DEVELOPER, 16f, Pal.KREM, 1))
        p.addView(Ui.spacer(ctx, 1, 8))
        p.addView(Ui.text(ctx, "Kontak", 12f, 0xFFC9BCA2.toInt(), 0))
        p.addView(Ui.text(ctx, Teks.EMAIL, 15f, Pal.KREM, 1))
        p.addView(Ui.spacer(ctx, 1, 6))
        p.addView(Ui.button(ctx, "Kirim Email", Kind.SECONDARY) { nav.sendEmail(Teks.EMAIL) })
        p.addView(Ui.spacer(ctx, 1, 8))
        p.addView(Ui.text(ctx, "Dukung", 12f, 0xFFC9BCA2.toInt(), 0))
        p.addView(Ui.text(ctx, Teks.DUKUNG_URL, 13f, Pal.KREM, 1))
        p.addView(Ui.spacer(ctx, 1, 6))
        p.addView(Ui.button(ctx, "Dukung lewat Saweria", Kind.PRIMARY) { nav.openUrl(Teks.DUKUNG_URL) })
        p.addView(Ui.spacer(ctx, 1, 12))
        p.addView(Ui.text(ctx, "Game ini gratis dan tanpa iklan. Dukungan bersifat sukarela dan tidak membuka apa pun di dalam game.", 12.5f, Pal.KREM, 0))
        c.addView(p)
        c.addView(Ui.spacer(ctx, 1, 10))
        val q = Ui.panel(ctx, 16)
        q.addView(Ui.text(ctx, "Lisensi dan aset", 15f, Pal.KUNYIT, 2))
        q.addView(Ui.spacer(ctx, 1, 4))
        q.addView(Ui.text(ctx, "Gambar, musik, dan efek suara dibuat khusus untuk game ini dengan skrip. Huruf Lora dan Poppins dipakai di bawah lisensi SIL Open Font License 1.1.", 12.5f, Pal.KREM, 0))
        c.addView(q)
        c.addView(Ui.spacer(ctx, 1, 10))
        val r = Ui.panel(ctx, 16)
        r.addView(Ui.text(ctx, "Data permainan", 15f, Pal.KUNYIT, 2))
        r.addView(Ui.spacer(ctx, 1, 4))
        r.addView(Ui.text(ctx, "Semua kemajuan disimpan di perangkat ini saja. Menghapusnya tidak bisa dibatalkan.", 12.5f, Pal.KREM, 0))
        r.addView(Ui.spacer(ctx, 1, 6))
        r.addView(Ui.button(ctx, "Hapus Semua Data", Kind.SECONDARY) { confirmReset(ctx) })
        c.addView(r)
        val sv = ScrollView(ctx)
        sv.addView(c)
        return Ui.frame(ctx, "Tentang", { nav.pop() }, false, sv)
    }

    private fun confirmReset(ctx: Context) {
        AlertDialog.Builder(ctx)
            .setTitle("Hapus semua data?")
            .setMessage("Koin, kemajuan karier, item, dan pencapaian akan hilang dan tidak bisa dikembalikan.")
            .setPositiveButton("Hapus") { _, _ ->
                Save.resetAll()
                nav.toast("Semua data dihapus.")
                nav.popTo(1)
                nav.replace(MenuScreen())
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
