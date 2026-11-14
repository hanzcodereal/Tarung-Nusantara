package com.hanzdev.tarungnusantara.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.hanzdev.tarungnusantara.audio.Sound
import com.hanzdev.tarungnusantara.data.Save
import com.hanzdev.tarungnusantara.game.Fonts
import com.hanzdev.tarungnusantara.game.Pal

enum class Kind { PRIMARY, SECONDARY, GHOST }

object Ui {
    private var density = 1f

    fun init(ctx: Context) {
        density = ctx.resources.displayMetrics.density
    }

    fun dp(v: Int): Int = (v * density + 0.5f).toInt()
    fun dpf(v: Float): Float = v * density

    fun lp(w: Int, h: Int, weight: Float = 0f): LinearLayout.LayoutParams {
        val p = LinearLayout.LayoutParams(w, h)
        p.weight = weight
        return p
    }

    fun text(ctx: Context, s: String, sp: Float = 14f, color: Int = Pal.KREM, style: Int = 0): TextView {
        val t = TextView(ctx)
        t.text = s
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        t.setTextColor(color)
        t.typeface = when (style) { 2 -> Fonts.title; 1 -> Fonts.bold; else -> Fonts.ui }
        t.includeFontPadding = false
        t.setLineSpacing(0f, 1.18f)
        return t
    }

    fun box(color: Int, stroke: Int, strokeDp: Int, radiusDp: Int): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(color)
        g.cornerRadius = dpf(radiusDp.toFloat())
        if (strokeDp > 0) g.setStroke(dp(strokeDp), stroke)
        return g
    }

    fun button(ctx: Context, label: String, kind: Kind = Kind.SECONDARY, enabled: Boolean = true, onClick: () -> Unit): Button {
        val b = Button(ctx)
        b.text = label
        b.setAllCaps(false)
        b.typeface = Fonts.bold
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        b.stateListAnimator = null
        b.minHeight = dp(48)
        b.minimumHeight = dp(48)
        b.setPadding(dp(18), dp(8), dp(18), dp(8))
        b.includeFontPadding = false
        val col = when (kind) {
            Kind.PRIMARY -> intArrayOf(0xFFB8281F.toInt(), 0xFF9C2018.toInt(), Pal.KREM, Pal.KUNYIT)
            Kind.SECONDARY -> intArrayOf(0xFF1D2A4F.toInt(), 0xFF2B3C6B.toInt(), Pal.KREM, Pal.KUNYIT)
            Kind.GHOST -> intArrayOf(0x00000000, 0x33F3E6CB, Pal.KREM, 0x66F3E6CB)
        }
        val base = col[0]
        val press = col[1]
        val text = col[2]
        val stroke = col[3]
        val sl = StateListDrawable()
        sl.addState(intArrayOf(-android.R.attr.state_enabled), box(0xFF3A3430.toInt(), 0xFF5A524C.toInt(), 2, 10))
        sl.addState(intArrayOf(android.R.attr.state_pressed), box(press, stroke, 2, 10))
        sl.addState(intArrayOf(android.R.attr.state_focused), box(press, Pal.KREM, 3, 10))
        sl.addState(intArrayOf(), box(base, stroke, 2, 10))
        b.background = sl
        b.setTextColor(text)
        b.isEnabled = enabled
        if (!enabled) b.setTextColor(0xFFB8AFA3.toInt())
        b.setOnClickListener {
            Sound.play("click")
            onClick()
        }
        return b
    }

    fun chip(ctx: Context, label: String, selected: Boolean, onClick: () -> Unit): TextView {
        val t = text(ctx, label, 13f, if (selected) Pal.SOGAN else Pal.KREM, 1)
        t.gravity = Gravity.CENTER
        t.minHeight = dp(44)
        t.setPadding(dp(16), dp(10), dp(16), dp(10))
        t.background = box(if (selected) Pal.KUNYIT else 0xFF1D2A4F.toInt(), Pal.KUNYIT, 2, 22)
        t.isClickable = true
        t.isFocusable = true
        t.contentDescription = if (selected) "$label, dipilih" else label
        t.setOnClickListener { Sound.play("click"); onClick() }
        return t
    }

    fun panel(ctx: Context, pad: Int = 14): LinearLayout {
        val l = LinearLayout(ctx)
        l.orientation = LinearLayout.VERTICAL
        l.background = box(0xE61D2A4F.toInt(), Pal.KUNYIT, 2, 14)
        l.setPadding(dp(pad), dp(pad), dp(pad), dp(pad))
        return l
    }

    fun spacer(ctx: Context, w: Int, h: Int): View {
        val v = View(ctx)
        v.layoutParams = LinearLayout.LayoutParams(dp(w), dp(h))
        return v
    }

    fun fill(): LinearLayout.LayoutParams = lp(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)

    /** Bingkai layar standar: tombol kembali, judul, koin, pita motif, isi. */
    fun frame(ctx: Context, title: String, onBack: (() -> Unit)?, showCoins: Boolean, content: View): View {
        val root = LinearLayout(ctx)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Pal.SOGAN)
        val bar = LinearLayout(ctx)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.gravity = Gravity.CENTER_VERTICAL
        bar.setPadding(dp(14), dp(6), dp(14), dp(2))
        if (onBack != null) {
            val b = button(ctx, "Kembali", Kind.GHOST) { onBack() }
            b.contentDescription = "Kembali ke layar sebelumnya"
            bar.addView(b, lp(ViewGroup.LayoutParams.WRAP_CONTENT, dp(48)))
            bar.addView(spacer(ctx, 12, 1))
        }
        val t = text(ctx, title, 24f, Pal.KUNYIT, 2)
        t.contentDescription = title
        bar.addView(t, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        if (showCoins) bar.addView(coinChip(ctx))
        root.addView(bar, lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(KawungBand(ctx), lp(ViewGroup.LayoutParams.MATCH_PARENT, dp(14)))
        val body = FrameLayout(ctx)
        body.setPadding(dp(14), dp(10), dp(14), dp(10))
        body.addView(content, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        root.addView(body, lp(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        return root
    }

    fun coinChip(ctx: Context): View {
        val l = LinearLayout(ctx)
        l.orientation = LinearLayout.HORIZONTAL
        l.gravity = Gravity.CENTER_VERTICAL
        l.background = box(0xFF1D2A4F.toInt(), Pal.KUNYIT, 2, 18)
        l.setPadding(dp(10), dp(6), dp(14), dp(6))
        l.addView(CoinView(ctx), lp(dp(22), dp(22)))
        l.addView(spacer(ctx, 6, 1))
        val t = text(ctx, formatNumber(Save.coins), 15f, Pal.KREM, 1)
        l.addView(t)
        l.contentDescription = "Koin: ${Save.coins}"
        return l
    }

    fun formatNumber(n: Int): String {
        val s = n.toString()
        val sb = StringBuilder()
        for ((i, ch) in s.withIndex()) {
            if (i > 0 && (s.length - i) % 3 == 0) sb.append('.')
            sb.append(ch)
        }
        return sb.toString()
    }

    fun statBar(ctx: Context, label: String, value: Int, max: Int = 10): View {
        val row = LinearLayout(ctx)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.addView(text(ctx, label, 12f, Pal.KREM, 0), lp(dp(84), ViewGroup.LayoutParams.WRAP_CONTENT))
        row.addView(SegBar(ctx, value, max), lp(0, dp(12), 1f))
        row.addView(text(ctx, value.toString(), 12f, Pal.KUNYIT, 1).also { it.gravity = Gravity.END },
            lp(dp(26), ViewGroup.LayoutParams.WRAP_CONTENT))
        row.contentDescription = "$label $value dari $max"
        return row
    }
}

class CoinView(ctx: Context) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(c: Canvas) {
        val r = minOf(width, height) / 2f
        val cx = width / 2f
        val cy = height / 2f
        p.style = Paint.Style.FILL
        p.color = Pal.KUNYIT
        c.drawCircle(cx, cy, r - 1f, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = r * 0.14f
        p.color = Pal.SOGAN
        c.drawCircle(cx, cy, r - 1f, p)
        p.strokeWidth = r * 0.12f
        c.drawCircle(cx, cy, r * 0.55f, p)
        p.style = Paint.Style.FILL
        c.drawRect(cx - r * 0.14f, cy - r * 0.14f, cx + r * 0.14f, cy + r * 0.14f, p)
    }
}

class SegBar(ctx: Context, private val value: Int, private val max: Int) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val r = RectF()
    override fun onDraw(c: Canvas) {
        val gap = Ui.dpf(3f)
        val w = (width - gap * (max - 1)) / max
        for (i in 0 until max) {
            r.set(i * (w + gap), 0f, i * (w + gap) + w, height.toFloat())
            p.style = Paint.Style.FILL
            p.color = if (i < value) Pal.KUNYIT else 0x55F3E6CB
            c.drawRoundRect(r, 3f, 3f, p)
        }
    }
}

/** Pita motif kawung: pengenal visual tunggal game ini. */
class KawungBand(ctx: Context) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val r = RectF()

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onDraw(c: Canvas) {
        p.style = Paint.Style.FILL
        p.color = 0xFF1D2A4F.toInt()
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), p)
        val s = height.toFloat()
        var x = 0f
        p.style = Paint.Style.STROKE
        p.strokeWidth = s * 0.07f
        p.color = Pal.KUNYIT
        while (x < width) {
            val cx = x + s / 2f
            val cy = s / 2f
            val a = s * 0.3f
            val b = s * 0.17f
            r.set(cx - a, cy - b, cx + a, cy + b)
            c.drawOval(r, p)
            r.set(cx - b, cy - a, cx + b, cy + a)
            c.drawOval(r, p)
            x += s
        }
        p.strokeWidth = s * 0.12f
        c.drawLine(0f, 0f, width.toFloat(), 0f, p)
        c.drawLine(0f, height.toFloat(), width.toFloat(), height.toFloat(), p)
    }
}
