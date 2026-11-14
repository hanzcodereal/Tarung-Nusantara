package com.hanzdev.tarungnusantara.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.hanzdev.tarungnusantara.data.Teks
import com.hanzdev.tarungnusantara.game.Fighter
import com.hanzdev.tarungnusantara.game.Fonts
import com.hanzdev.tarungnusantara.game.GameSession
import com.hanzdev.tarungnusantara.game.Pal
import com.hanzdev.tarungnusantara.game.Ph
import kotlin.math.min

/** HUD: bar HP dan stamina, ronde, pewaktu, meteran jurus, banner, komentar. */
class Hud {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val r = RectF()
    private val lag = floatArrayOf(1f, 1f)

    fun draw(c: Canvas, w: Int, h: Int, s: GameSession, rtt: Int) {
        val u = h / 360f
        val m = s.match
        drawFighterBlock(c, s.p1, 0, 16f * u, w / 2f - 52f * u, u, false)
        drawFighterBlock(c, s.p2, 1, w / 2f + 52f * u, w - 16f * u, u, true)
        drawCenter(c, w, u, s)
        drawMeter(c, w, h, u, s)
        if (s.bannerFrames > 0) drawBanner(c, w, h, u, s)
        if (s.commentFrames > 0) drawComment(c, w, h, u, s.commentText)
        if (s.toastFrames > 0) drawToast(c, w, u, s.toastText)
        if (s.isNet) drawNet(c, w, u, rtt, s.reconnecting)
        if (s.isTraining) drawTraining(c, w, h, u, s)
        if (m.phase == Ph.BREAK) drawBreak(c, w, h, u, (m.cfg.breakFrames - m.phaseFrame) / 60 + 1)
        if (m.phase == Ph.INTRO) drawIntro(c, w, h, u, s)
    }

    private fun text(c: Canvas, t: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean, align: Paint.Align, title: Boolean = false) {
        p.style = Paint.Style.FILL
        p.typeface = if (title) Fonts.title else if (bold) Fonts.bold else Fonts.ui
        p.textSize = size
        p.textAlign = align
        p.color = color
        c.drawText(t, x, y, p)
    }

    private fun outlined(c: Canvas, t: String, x: Float, y: Float, size: Float, fill: Int, u: Float) {
        p.typeface = Fonts.title
        p.textSize = size
        p.textAlign = Paint.Align.CENTER
        p.style = Paint.Style.STROKE
        p.strokeWidth = 5f * u
        p.strokeJoin = Paint.Join.ROUND
        p.color = Pal.SOGAN
        c.drawText(t, x, y, p)
        p.style = Paint.Style.FILL
        p.color = fill
        c.drawText(t, x, y, p)
    }

    private fun bar(c: Canvas, x0: Float, x1: Float, y: Float, hh: Float, frac: Float, lagFrac: Float, fill: Int, rtl: Boolean, u: Float) {
        r.set(x0, y, x1, y + hh)
        p.style = Paint.Style.FILL
        p.color = 0xCC231710.toInt()
        c.drawRoundRect(r, hh / 2f, hh / 2f, p)
        val wd = x1 - x0
        if (lagFrac > frac) {
            p.color = 0xFFF3E6CB.toInt()
            if (rtl) r.set(x1 - wd * lagFrac, y, x1, y + hh) else r.set(x0, y, x0 + wd * lagFrac, y + hh)
            c.drawRoundRect(r, hh / 2f, hh / 2f, p)
        }
        p.color = fill
        if (rtl) r.set(x1 - wd * frac, y, x1, y + hh) else r.set(x0, y, x0 + wd * frac, y + hh)
        if (frac > 0.005f) c.drawRoundRect(r, hh / 2f, hh / 2f, p)
        r.set(x0, y, x1, y + hh)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f * u
        p.color = 0xFFE3A72F.toInt()
        c.drawRoundRect(r, hh / 2f, hh / 2f, p)
    }

    private fun drawFighterBlock(c: Canvas, f: Fighter, idx: Int, x0: Float, x1: Float, u: Float, rtl: Boolean) {
        val frac = (f.hp / f.maxHp).coerceIn(0f, 1f)
        if (lag[idx] > frac) lag[idx] = maxOf(frac, lag[idx] - 0.006f) else lag[idx] = frac
        val align = if (rtl) Paint.Align.RIGHT else Paint.Align.LEFT
        val tx = if (rtl) x1 else x0
        text(c, f.def.nama, tx, 22f * u, 15f * u, Pal.KREM, false, align, true)
        val low = frac < 0.25f
        val hpColor = if (low) Pal.CABAI else Pal.KUNYIT
        bar(c, x0, x1, 28f * u, 15f * u, frac, lag[idx], hpColor, rtl, u)
        text(c, "HP ${f.hp.toInt()}", if (rtl) x0 + 6f * u else x1 - 6f * u, 40f * u, 10.5f * u, Pal.KREM, true, if (rtl) Paint.Align.LEFT else Paint.Align.RIGHT)
        val sf = (f.stam / f.maxStam).coerceIn(0f, 1f)
        bar(c, x0, x1, 48f * u, 8f * u, sf, sf, Pal.NILA_TERANG, rtl, u)
        text(c, "STAMINA", tx, 66f * u, 9f * u, Pal.KREM, false, align)
    }

    private fun drawCenter(c: Canvas, w: Int, u: Float, s: GameSession) {
        val m = s.match
        val cx = w / 2f
        r.set(cx - 42f * u, 8f * u, cx + 42f * u, 56f * u)
        p.style = Paint.Style.FILL
        p.color = 0xE61D2A4F.toInt()
        c.drawRoundRect(r, 10f * u, 10f * u, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2.5f * u
        p.color = 0xFFE3A72F.toInt()
        c.drawRoundRect(r, 10f * u, 10f * u, p)
        val round = if (s.isTraining) "LATIHAN" else "RONDE ${m.round.coerceAtLeast(1)}/${m.cfg.rounds}"
        text(c, round, cx, 21f * u, 9.5f * u, Pal.KUNYIT, true, Paint.Align.CENTER)
        val clock = if (s.isTraining) "--" else m.timeLeftSeconds.coerceAtLeast(0).toString()
        text(c, clock, cx, 48f * u, 26f * u, Pal.KREM, false, Paint.Align.CENTER, true)
        for (i in 0 until 2) {
            val wins = m.roundWins[i]
            for (k in 0 until (m.cfg.rounds / 2 + 1)) {
                val dx = (if (i == 0) -1 else 1) * (58f + k * 11f) * u
                p.style = Paint.Style.FILL
                p.color = if (k < wins) Pal.KUNYIT else 0x55F3E6CB
                if (!s.isTraining && m.cfg.rounds > 1) c.drawCircle(cx + dx, 14f * u, 4f * u, p)
            }
        }
    }

    private fun drawMeter(c: Canvas, w: Int, h: Int, u: Float, s: GameSession) {
        val f = s.fighterOf(s.localSlot)
        val cx = w / 2f
        val y = h - 22f * u
        val wd = 150f * u
        val frac = (f.meter / 100f).coerceIn(0f, 1f)
        bar(c, cx - wd / 2f, cx + wd / 2f, y, 11f * u, frac, frac, Pal.CABAI, false, u)
        val label = if (f.meter >= 100f) "JURUS SIAP" else "JURUS ${f.meter.toInt()}%"
        text(c, label, cx, y - 4f * u, 10f * u, if (f.meter >= 100f) Pal.KUNYIT else Pal.KREM, true, Paint.Align.CENTER)
    }

    private fun drawBanner(c: Canvas, w: Int, h: Int, u: Float, s: GameSession) {
        val a = min(1f, s.bannerFrames / 15f)
        outlined(c, s.bannerText, w / 2f, h * 0.36f, 46f * u * (0.9f + 0.1f * a), Pal.KREM, u)
    }

    private fun drawComment(c: Canvas, w: Int, h: Int, u: Float, t: String) {
        p.typeface = Fonts.ui
        p.textSize = 12f * u
        val tw = p.measureText(t)
        val cx = w / 2f
        val y = h - 44f * u
        r.set(cx - tw / 2f - 12f * u, y - 16f * u, cx + tw / 2f + 12f * u, y + 6f * u)
        p.style = Paint.Style.FILL
        p.color = 0xD91D2A4F.toInt()
        c.drawRoundRect(r, 8f * u, 8f * u, p)
        text(c, t, cx, y, 12f * u, Pal.KREM, false, Paint.Align.CENTER)
    }

    private fun drawToast(c: Canvas, w: Int, u: Float, t: String) {
        p.typeface = Fonts.bold
        p.textSize = 12f * u
        val tw = p.measureText(t)
        val cx = w / 2f
        r.set(cx - tw / 2f - 12f * u, 92f * u, cx + tw / 2f + 12f * u, 116f * u)
        p.style = Paint.Style.FILL
        p.color = 0xE6B8281F.toInt()
        c.drawRoundRect(r, 8f * u, 8f * u, p)
        text(c, t, cx, 108f * u, 12f * u, Pal.KREM, true, Paint.Align.CENTER)
    }

    private fun drawNet(c: Canvas, w: Int, u: Float, rtt: Int, reconnecting: Boolean) {
        val bars = when {
            reconnecting || rtt < 0 -> 0
            rtt < 60 -> 4
            rtt < 120 -> 3
            rtt < 220 -> 2
            else -> 1
        }
        val x0 = w / 2f - 22f * u
        for (i in 0 until 4) {
            p.style = Paint.Style.FILL
            p.color = if (i < bars) Pal.KUNYIT else 0x55F3E6CB
            val bh = (5f + i * 3f) * u
            c.drawRect(x0 + i * 7f * u, 92f * u - bh + 12f * u, x0 + i * 7f * u + 5f * u, 104f * u, p)
        }
        val label = if (rtt >= 0) "$rtt ms" else "-- ms"
        text(c, label, x0 + 34f * u, 104f * u, 10f * u, Pal.KREM, false, Paint.Align.LEFT)
    }

    private fun drawTraining(c: Canvas, w: Int, h: Int, u: Float, s: GameSession) {
        val x = 16f * u
        val y = 84f * u
        val step = s.tutorialStep.coerceAtMost(Teks.tutorial.size - 1)
        val line = if (s.tutorialDone) "Latihan bebas. Coba semua gerakan." else Teks.tutorial[step]
        val maxW = w * 0.42f
        p.typeface = Fonts.ui
        p.textSize = 11.5f * u
        val words = line.split(" ")
        val lines = ArrayList<String>()
        var cur = ""
        for (wd in words) {
            val t = if (cur.isEmpty()) wd else "$cur $wd"
            if (p.measureText(t) > maxW - 20f * u) { lines.add(cur); cur = wd } else cur = t
        }
        if (cur.isNotEmpty()) lines.add(cur)
        val bh = (30f + lines.size * 15f) * u
        r.set(x, y, x + maxW, y + bh)
        p.style = Paint.Style.FILL
        p.color = 0xE61D2A4F.toInt()
        c.drawRoundRect(r, 10f * u, 10f * u, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f * u
        p.color = 0xFFE3A72F.toInt()
        c.drawRoundRect(r, 10f * u, 10f * u, p)
        text(c, "Pak Guru Wiryo  (${if (s.tutorialDone) "selesai" else "${step + 1}/${Teks.tutorial.size}"})", x + 10f * u, y + 16f * u, 10f * u, Pal.KUNYIT, true, Paint.Align.LEFT)
        for ((i, l) in lines.withIndex()) text(c, l, x + 10f * u, y + 32f * u + i * 15f * u, 11.5f * u, Pal.KREM, false, Paint.Align.LEFT)
        val thrown = s.p1.thrown
        val acc = if (thrown > 0) (s.p1.landed * 100 / thrown) else 0
        text(c, "Pukulan $thrown, kena ${s.p1.landed}, akurasi $acc%", x, y + bh + 16f * u, 10.5f * u, Pal.KREM, false, Paint.Align.LEFT)
    }

    private fun drawBreak(c: Canvas, w: Int, h: Int, u: Float, secs: Int) {
        outlined(c, "ISTIRAHAT", w / 2f, h * 0.32f, 34f * u, Pal.KREM, u)
        text(c, "Ronde berikutnya dalam $secs detik", w / 2f, h * 0.32f + 26f * u, 13f * u, Pal.KREM, false, Paint.Align.CENTER)
    }

    private fun drawIntro(c: Canvas, w: Int, h: Int, u: Float, s: GameSession) {
        val a = s.p1.def
        val b = s.p2.def
        p.style = Paint.Style.FILL
        p.color = 0x99231710.toInt()
        c.drawRect(0f, h * 0.2f, w.toFloat(), h * 0.5f, p)
        text(c, a.nama, w * 0.25f, h * 0.31f, 22f * u, Pal.KUNYIT, false, Paint.Align.CENTER, true)
        text(c, a.daerah, w * 0.25f, h * 0.31f + 16f * u, 11f * u, Pal.KREM, false, Paint.Align.CENTER)
        text(c, "\u201C${a.dialogBuka}\u201D", w * 0.25f, h * 0.31f + 40f * u, 11f * u, Pal.KREM, false, Paint.Align.CENTER)
        text(c, b.nama, w * 0.75f, h * 0.31f, 22f * u, Pal.KUNYIT, false, Paint.Align.CENTER, true)
        text(c, b.daerah, w * 0.75f, h * 0.31f + 16f * u, 11f * u, Pal.KREM, false, Paint.Align.CENTER)
        text(c, "\u201C${b.dialogBuka}\u201D", w * 0.75f, h * 0.31f + 40f * u, 11f * u, Pal.KREM, false, Paint.Align.CENTER)
        outlined(c, "VS", w / 2f, h * 0.38f, 36f * u, Pal.CABAI, u)
    }
}
