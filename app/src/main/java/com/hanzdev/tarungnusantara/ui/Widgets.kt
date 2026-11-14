package com.hanzdev.tarungnusantara.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.hanzdev.tarungnusantara.audio.Sound
import com.hanzdev.tarungnusantara.data.Save
import com.hanzdev.tarungnusantara.game.Fighter
import com.hanzdev.tarungnusantara.game.FighterDef
import com.hanzdev.tarungnusantara.game.FighterRenderer
import com.hanzdev.tarungnusantara.game.Look
import com.hanzdev.tarungnusantara.game.Pal
import com.hanzdev.tarungnusantara.game.Upgrades

/** Menggambar petarung diam (seluruh badan atau setengah badan) memakai pembuat gambar yang sama dengan pertandingan. */
class FighterView(ctx: Context) : View(ctx) {
    private val renderer = FighterRenderer()
    private var fighter: Fighter? = null
    private val dim = PorterDuffColorFilter(0xFF4A423C.toInt(), PorterDuff.Mode.MULTIPLY)
    var bust = false
    var dimmed = false

    fun setFighter(def: FighterDef, look: Look, gloveStyle: Int) {
        val f = Fighter(def, 0)
        f.look = look
        f.gloveStyle = gloveStyle
        f.applyStats(Upgrades())
        fighter = f
        invalidate()
    }

    override fun onDraw(c: Canvas) {
        val f = fighter ?: return
        val t = if (Save.reduceMotion) 0f else SystemClock.uptimeMillis() / 1000f
        renderer.tintFilter = if (dimmed) dim else null
        c.save()
        if (bust) {
            val s = minOf(width / 170f, height / 190f)
            c.translate(width / 2f, height + 150f * s)
            c.scale(s, s)
        } else {
            val s = minOf(width / 230f, height / 350f)
            c.translate(width / 2f, height - 8f * s)
            c.scale(s, s)
        }
        renderer.draw(c, f, 0f, 0f, t)
        c.restore()
        if (!Save.reduceMotion) postInvalidateOnAnimation()
    }
}

object Widgets {
    fun styleChip(t: TextView, selected: Boolean) {
        t.setTextColor(if (selected) Pal.SOGAN else Pal.KREM)
        t.background = Ui.box(if (selected) Pal.KUNYIT else 0xFF1D2A4F.toInt(), Pal.KUNYIT, 2, 22)
    }

    /** Pilihan tunggal: baris chip yang menyorot pilihan aktif tanpa membangun ulang layar. */
    fun choice(ctx: Context, options: List<String>, selected: Int, onSelect: (Int) -> Unit): View {
        val row = LinearLayout(ctx)
        row.orientation = LinearLayout.HORIZONTAL
        val chips = ArrayList<TextView>()
        var current = selected
        for ((i, label) in options.withIndex()) {
            val chip = Ui.chip(ctx, label, i == current) {}
            chips.add(chip)
            chip.setOnClickListener {
                Sound.play("click")
                current = i
                for ((k, c) in chips.withIndex()) {
                    styleChip(c, k == current)
                    c.contentDescription = if (k == current) "${options[k]}, dipilih" else options[k]
                }
                onSelect(i)
            }
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.rightMargin = Ui.dp(8)
            row.addView(chip, lp)
        }
        val sc = HorizontalScrollView(ctx)
        sc.isHorizontalScrollBarEnabled = false
        sc.addView(row)
        return sc
    }

    fun toggle(ctx: Context, label: String, initial: Boolean, onChange: (Boolean) -> Unit): TextView {
        var on = initial
        val t = Ui.text(ctx, if (on) "Hidup" else "Mati", 13f, Pal.KREM, 1)
        t.gravity = Gravity.CENTER
        t.minWidth = Ui.dp(84)
        t.minHeight = Ui.dp(44)
        t.setPadding(Ui.dp(14), Ui.dp(10), Ui.dp(14), Ui.dp(10))
        t.isClickable = true
        t.isFocusable = true
        fun apply() {
            t.text = if (on) "Hidup" else "Mati"
            styleChip(t, on)
            t.contentDescription = "$label: ${if (on) "hidup" else "mati"}"
        }
        apply()
        t.setOnClickListener {
            Sound.play("click")
            on = !on
            apply()
            onChange(on)
        }
        return t
    }
}

/** Empat batang kualitas sambungan. */
class SignalView(ctx: Context, private val level: Int) : View(ctx) {
    private val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(c: Canvas) {
        val gap = width / 12f
        val bw = (width - gap * 3) / 4f
        for (i in 0 until 4) {
            val h = height * (0.35f + 0.65f * (i + 1) / 4f)
            p.color = if (i < level) Pal.KUNYIT else 0x55F3E6CB
            c.drawRect(i * (bw + gap), height - h, i * (bw + gap) + bw, height.toFloat(), p)
        }
    }
}
