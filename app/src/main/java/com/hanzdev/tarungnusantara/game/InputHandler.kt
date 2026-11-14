package com.hanzdev.tarungnusantara.game

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/** Kontrol sentuh: joystick di satu sisi, empat tombol aksi dan tombol jurus di sisi lain. */
class TouchControls(private val input: FighterInput) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    var buttonsLeft = false
    var onPause: (() -> Unit)? = null
    var enabled = true

    private var u = 1f
    private var w = 0
    private var h = 0

    private val labels = arrayOf("JAB", "HOOK", "UPPER", "TANGKIS", "JURUS")
    private val bits = intArrayOf(Bits.JAB, Bits.HOOK, Bits.UPPER, 0, Bits.SPECIAL)
    private val bx = FloatArray(5)
    private val by = FloatArray(5)
    private val br = FloatArray(5)
    private val bPointer = IntArray(5) { -1 }

    private var joyCx = 0f
    private var joyCy = 0f
    private var joyR = 0f
    private var joyPointer = -1
    private var knobX = 0f
    private var knobY = 0f
    private var prevAxis = 0f
    private var lastFlickDir = 0
    private var lastFlickAt = 0L

    private var pauseX = 0f
    private var pauseY = 0f
    private var pauseR = 0f
    private var pausePointer = -1

    fun layout(width: Int, height: Int) {
        w = width; h = height
        u = height / 360f
        val ax = 22f * u
        val ay = 22f * u
        val offs = arrayOf(
            floatArrayOf(-64f, -64f, 40f), floatArrayOf(-152f, -42f, 38f), floatArrayOf(-60f, -158f, 38f),
            floatArrayOf(-160f, -132f, 40f), floatArrayOf(-246f, -96f, 44f)
        )
        for (i in 0 until 5) {
            val ox = offs[i][0] * u
            val oy = offs[i][1] * u
            bx[i] = if (buttonsLeft) ax - ox else w - ax + ox
            by[i] = h - ay + oy
            br[i] = offs[i][2] * u
        }
        joyR = 70f * u
        joyCy = h - 100f * u
        joyCx = if (buttonsLeft) w - 100f * u else 100f * u
        pauseX = w / 2f
        pauseY = 66f * u
        pauseR = 22f * u
        knobX = joyCx; knobY = joyCy
    }

    private fun inButton(i: Int, x: Float, y: Float): Boolean = hypot(x - bx[i], y - by[i]) <= br[i] * 1.12f

    fun reset() {
        for (i in 0 until 5) bPointer[i] = -1
        joyPointer = -1
        pausePointer = -1
        knobX = joyCx; knobY = joyCy
        input.moveX = 0f; input.duck = false; input.block = false
    }

    fun onTouch(ev: MotionEvent): Boolean {
        if (!enabled) return true
        val action = ev.actionMasked
        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val idx = ev.actionIndex
                down(ev.getPointerId(idx), ev.getX(idx), ev.getY(idx))
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until ev.pointerCount) {
                    if (ev.getPointerId(i) == joyPointer) joyMove(ev.getX(i), ev.getY(i))
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                val idx = ev.actionIndex
                val id = ev.getPointerId(idx)
                if (action == MotionEvent.ACTION_CANCEL) reset() else up(id, ev.getX(idx), ev.getY(idx))
            }
        }
        return true
    }

    private fun down(id: Int, x: Float, y: Float) {
        if (hypot(x - pauseX, y - pauseY) <= pauseR * 1.2f) {
            pausePointer = id
            return
        }
        for (i in 0 until 5) {
            if (bPointer[i] == -1 && inButton(i, x, y)) {
                bPointer[i] = id
                if (i == 3) {
                    input.block = true
                    input.press(Bits.BLOCK_DOWN)
                } else {
                    input.press(bits[i])
                }
                return
            }
        }
        val onJoySide = if (buttonsLeft) x > w * 0.55f else x < w * 0.45f
        if (joyPointer == -1 && onJoySide) {
            joyPointer = id
            joyMove(x, y)
        }
    }

    private fun up(id: Int, x: Float, y: Float) {
        if (id == pausePointer) {
            pausePointer = -1
            if (hypot(x - pauseX, y - pauseY) <= pauseR * 1.4f) onPause?.invoke()
            return
        }
        for (i in 0 until 5) {
            if (bPointer[i] == id) {
                bPointer[i] = -1
                if (i == 3) input.block = false
            }
        }
        if (id == joyPointer) {
            joyPointer = -1
            knobX = joyCx; knobY = joyCy
            input.moveX = 0f
            input.duck = false
            prevAxis = 0f
        }
    }

    private fun joyMove(x: Float, y: Float) {
        var dx = x - joyCx
        var dy = y - joyCy
        val d = hypot(dx, dy)
        if (d > joyR) { dx = dx / d * joyR; dy = dy / d * joyR }
        knobX = joyCx + dx
        knobY = joyCy + dy
        val axis = (dx / (joyR * 0.75f)).coerceIn(-1f, 1f)
        input.moveX = axis
        input.duck = dy > joyR * 0.5f && abs(dx) < joyR * 0.6f
        val now = System.currentTimeMillis()
        if (abs(prevAxis) < 0.45f && abs(axis) > 0.8f) {
            val dir = if (axis > 0f) 1 else -1
            if (dir == lastFlickDir && now - lastFlickAt <= 320L) {
                input.press(if (dir < 0) Bits.DODGE_L else Bits.DODGE_R)
                lastFlickDir = 0
            } else {
                lastFlickDir = dir
                lastFlickAt = now
            }
        }
        prevAxis = axis
    }

    fun draw(c: Canvas, meter: Float, anim: Float) {
        if (!enabled) return
        p.style = Paint.Style.FILL
        p.color = 0x66231710
        c.drawCircle(joyCx, joyCy, joyR, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 3f * u
        p.color = 0xAAE3A72F.toInt()
        c.drawCircle(joyCx, joyCy, joyR, p)
        p.strokeWidth = 1.5f * u
        p.color = 0x55F3E6CB
        c.drawLine(joyCx - joyR * 0.8f, joyCy, joyCx + joyR * 0.8f, joyCy, p)
        p.style = Paint.Style.FILL
        p.color = 0xE0F3E6CB.toInt()
        c.drawCircle(knobX, knobY, 30f * u, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 3f * u
        p.color = 0xFF231710.toInt()
        c.drawCircle(knobX, knobY, 30f * u, p)

        p.style = Paint.Style.FILL
        p.color = 0x99231710.toInt()
        c.drawCircle(pauseX, pauseY, pauseR, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2.5f * u
        p.color = 0xFFE3A72F.toInt()
        c.drawCircle(pauseX, pauseY, pauseR, p)
        p.style = Paint.Style.FILL
        p.color = 0xFFF3E6CB.toInt()
        rect.set(pauseX - 7f * u, pauseY - 8f * u, pauseX - 2.5f * u, pauseY + 8f * u)
        c.drawRoundRect(rect, 1.5f * u, 1.5f * u, p)
        rect.set(pauseX + 2.5f * u, pauseY - 8f * u, pauseX + 7f * u, pauseY + 8f * u)
        c.drawRoundRect(rect, 1.5f * u, 1.5f * u, p)

        val full = meter >= 100f
        for (i in 0 until 5) {
            val held = bPointer[i] != -1
            val cx = bx[i]
            val cy = by[i]
            val r = br[i] * (if (held) 0.94f else 1f)
            p.style = Paint.Style.FILL
            p.color = when {
                i == 4 && full -> 0xFFB8281F.toInt()
                i == 4 -> 0xAA231710.toInt()
                held -> 0xFFE3A72F.toInt()
                else -> 0xC41D2A4F.toInt()
            }
            c.drawCircle(cx, cy, r, p)
            p.style = Paint.Style.STROKE
            p.strokeWidth = 3.5f * u
            p.color = if (i == 4 && !full) 0x66E3A72F else 0xFFE3A72F.toInt()
            c.drawCircle(cx, cy, r, p)
            if (i == 4) {
                rect.set(cx - r - 4f * u, cy - r - 4f * u, cx + r + 4f * u, cy + r + 4f * u)
                p.strokeWidth = 4f * u
                p.color = if (full) 0xFFF3E6CB.toInt() else 0xFFB8281F.toInt()
                c.drawArc(rect, -90f, 360f * min(1f, meter / 100f), false, p)
                if (full) {
                    p.color = (0x30 + (50 * abs(sin(anim * 8f))).toInt() shl 24) or 0xF3E6CB
                    c.drawCircle(cx, cy, r + 10f * u, p)
                }
            }
            p.style = Paint.Style.FILL
            p.color = if (held && i != 4) 0xFF231710.toInt() else 0xFFF3E6CB.toInt()
            p.typeface = Fonts.bold
            p.textAlign = Paint.Align.CENTER
            p.textSize = (if (labels[i].length > 5) 11f else 13.5f) * u
            c.drawText(labels[i], cx, cy + p.textSize * 0.35f, p)
        }
        p.typeface = null
        p.textAlign = Paint.Align.LEFT
    }
}
