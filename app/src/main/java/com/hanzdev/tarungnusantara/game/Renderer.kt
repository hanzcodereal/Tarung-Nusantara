package com.hanzdev.tarungnusantara.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

object Fonts {
    lateinit var title: Typeface
    lateinit var ui: Typeface
    lateinit var bold: Typeface
    private var ready = false

    fun init(ctx: Context) {
        if (ready) return
        val am = ctx.applicationContext.assets
        title = Typeface.createFromAsset(am, "fonts/Lora-Bold.ttf")
        ui = Typeface.createFromAsset(am, "fonts/Poppins-Medium.ttf")
        bold = Typeface.createFromAsset(am, "fonts/Poppins-Bold.ttf")
        ready = true
    }
}

object Pal {
    const val SOGAN = 0xFF231710.toInt()
    const val NILA = 0xFF1D2A4F.toInt()
    const val KUNYIT = 0xFFE3A72F.toInt()
    const val CABAI = 0xFFB8281F.toInt()
    const val KREM = 0xFFF3E6CB.toInt()
    const val NILA_TERANG = 0xFF8FA8E8.toInt()
}

fun loadAssetBitmap(ctx: Context, path: String, sample: Int = 1): Bitmap? {
    return try {
        ctx.assets.open(path).use { s ->
            val o = BitmapFactory.Options()
            o.inSampleSize = sample
            BitmapFactory.decodeStream(s, null, o)
        }
    } catch (e: Exception) {
        null
    }
}

class Pose {
    var lean = 0f
    var crouch = 0f
    var hx = 0f
    var hy = 0f
    var fgx = 0f
    var fgy = 0f
    var rgx = 0f
    var rgy = 0f
    var ffx = 0f
    var ffy = 0f
    var rfx = 0f
    var rfy = 0f
    var rot = 0f
    var glow = 0f
    var face = 0

    fun set(a: FloatArray) {
        lean = a[0]; crouch = a[1]; hx = a[2]
        fgx = a[3]; fgy = a[4]; rgx = a[5]; rgy = a[6]
        ffx = a[7]; rfx = a[8]
        ffy = 0f; rfy = 0f; hy = 0f; rot = 0f; glow = 0f; face = 0
    }

    fun mix(a: FloatArray, b: FloatArray, t: Float) {
        val u = t.coerceIn(0f, 1f)
        lean = a[0] + (b[0] - a[0]) * u
        crouch = a[1] + (b[1] - a[1]) * u
        hx = a[2] + (b[2] - a[2]) * u
        fgx = a[3] + (b[3] - a[3]) * u
        fgy = a[4] + (b[4] - a[4]) * u
        rgx = a[5] + (b[5] - a[5]) * u
        rgy = a[6] + (b[6] - a[6]) * u
        ffx = a[7] + (b[7] - a[7]) * u
        rfx = a[8] + (b[8] - a[8]) * u
        ffy = 0f; rfy = 0f; hy = 0f; rot = 0f; glow = 0f; face = 0
    }
}

object Poses {
    //             lean crouch hx   fgx  fgy   rgx  rgy   ffx  rfx
    val GUARD = floatArrayOf(4f, 0f, 4f, 66f, -226f, 40f, -236f, 42f, -36f)
    val JAB_W = floatArrayOf(2f, 4f, 0f, 28f, -218f, 40f, -234f, 42f, -36f)
    val JAB_S = floatArrayOf(12f, 6f, 10f, 142f, -232f, 44f, -232f, 52f, -40f)
    val HOOK_W = floatArrayOf(-4f, 6f, -4f, 60f, -232f, -16f, -198f, 40f, -38f)
    val HOOK_S = floatArrayOf(16f, 10f, 14f, 60f, -232f, 122f, -248f, 58f, -34f)
    val UP_W = floatArrayOf(10f, 48f, 6f, 34f, -120f, 40f, -232f, 46f, -40f)
    val UP_S = floatArrayOf(-6f, -4f, 4f, 98f, -308f, 46f, -232f, 52f, -34f)
    val SP_A = floatArrayOf(14f, 8f, 12f, 150f, -232f, 44f, -236f, 54f, -40f)
    val SP_B = floatArrayOf(14f, 8f, 12f, 62f, -228f, 140f, -240f, 54f, -40f)
    val BLOCK = floatArrayOf(8f, 8f, 0f, 58f, -250f, 46f, -232f, 40f, -38f)
    val DUCK = floatArrayOf(16f, 62f, 14f, 70f, -170f, 48f, -176f, 52f, -44f)
    val HURT = floatArrayOf(-18f, 6f, -16f, 30f, -196f, 10f, -190f, 36f, -40f)
    val CROSS = floatArrayOf(6f, 14f, 2f, 40f, -200f, 36f, -206f, 44f, -38f)
}

class FighterRenderer {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rect = RectF()
    private val pose = Pose()
    private val flashFilter = PorterDuffColorFilter(0xB0FFFFFF.toInt(), PorterDuff.Mode.SRC_ATOP)
    private val ol = Pal.SOGAN
    var tintFilter: android.graphics.ColorFilter? = null
    private var kx = 0f
    private var ky = 0f
    private var ex = 0f
    private var ey = 0f

    private fun rad(d: Float) = d * (PI.toFloat() / 180f)
    private fun ease(t: Float): Float { val u = t.coerceIn(0f, 1f); return u * u * (3f - 2f * u) }
    private fun shade(c: Int, m: Float): Int =
        Color.rgb((Color.red(c) * m).toInt(), (Color.green(c) * m).toInt(), (Color.blue(c) * m).toInt())

    fun computePose(f: Fighter, anim: Float, po: Pose) {
        val bob = sin(anim * 5.5f + f.slot * 1.7f)
        val g = Poses.GUARD
        when (f.state) {
            St.IDLE -> {
                po.set(g); po.crouch = 3f + bob * 2.5f; po.fgy += bob * 2f; po.rgy -= bob * 2f
            }
            St.WALK -> {
                po.set(g)
                val ph = anim * 12f
                po.ffx = 42f + sin(ph) * 28f; po.rfx = -36f - sin(ph) * 28f
                po.ffy = -max(0f, cos(ph)) * 12f; po.rfy = -max(0f, -cos(ph)) * 12f
                po.crouch = 3f + abs(sin(ph)) * 3f
            }
            St.DUCK -> po.set(Poses.DUCK)
            St.BLOCK -> { po.set(Poses.BLOCK); po.crouch += bob }
            St.DODGE -> {
                po.set(g)
                val t = f.frame / 16f
                val s = sin(PI.toFloat() * t.coerceIn(0f, 1f))
                val local = f.dodgeDir * f.facing
                po.lean = if (local < 0) -22f * s else 14f * s
                po.crouch = 20f * s
                po.ffx += 20f * local * s; po.rfx += 20f * local * s
                po.hx = -10f * s * (if (local < 0) 1f else -0.4f)
            }
            St.HIT, St.STAGGER -> {
                val k = max(0f, 1f - f.frame / 14f)
                po.mix(g, Poses.HURT, k)
                po.hy = 4f * k
                po.face = 1
            }
            St.DIZZY -> {
                val w = sin(anim * 6f)
                po.set(Poses.CROSS)
                po.lean = w * 14f - 4f; po.hx = w * 10f; po.crouch = 10f
                po.face = 2
            }
            St.DOWN -> {
                val t = ease(f.frame / 12f)
                po.set(g); po.rot = 82f * t; po.fgx = 60f; po.fgy = -190f; po.rgx = 20f; po.rgy = -170f
                po.face = 3
            }
            St.GETUP -> {
                val t = f.frame / 26f
                po.set(g); po.rot = 82f * (1f - ease(t)); po.crouch = 30f * (1f - t)
                po.face = 3
            }
            St.KO -> {
                val t = ease(f.frame / 12f)
                po.set(g); po.rot = 82f * t; po.fgx = 60f; po.fgy = -190f; po.rgx = 20f; po.rgy = -170f
                po.face = 3
            }
            St.WIN -> {
                po.set(g)
                val b = abs(sin(anim * 7f))
                po.fgx = 60f; po.fgy = -330f - b * 10f; po.rgx = -14f; po.rgy = -325f - b * 10f
                po.lean = -6f; po.crouch = b * 6f; po.face = 4
            }
            St.JAB, St.HOOK, St.UPPER -> attackPose(f, po)
            St.SPECIAL -> specialPose(f, anim, po)
            else -> po.set(g)
        }
    }

    private fun attackPose(f: Fighter, po: Pose) {
        val k = f.attackKind
        val w = when (k) { 0 -> Poses.JAB_W; 1 -> Poses.HOOK_W; else -> Poses.UP_W }
        val s = when (k) { 0 -> Poses.JAB_S; 1 -> Poses.HOOK_S; else -> Poses.UP_S }
        val act = Moves.active[k]
        when {
            f.frame <= f.startupF -> po.mix(Poses.GUARD, w, ease(f.frame / f.startupF.toFloat()))
            f.frame <= f.startupF + act -> po.mix(w, s, 1f)
            else -> po.mix(s, Poses.GUARD, ease((f.frame - f.startupF - act) / f.recoverF.toFloat()))
        }
    }

    private fun specialPose(f: Fighter, anim: Float, po: Pose) {
        val sp = f.def.special
        val sf = f.specFrame
        po.set(Poses.GUARD)
        po.glow = 1f
        if (sp.counterStance) {
            if (f.counterFired && sf >= 38) {
                po.mix(Poses.HOOK_W, Poses.HOOK_S, if (sf >= 42) 1f else (sf - 38) / 4f)
            } else {
                po.set(Poses.BLOCK); po.lean = -6f
            }
            return
        }
        if (sp.invulStart >= 0 && sf in sp.invulStart..sp.invulEnd) {
            if (sp.swapAt >= 0) {
                po.set(Poses.GUARD); po.lean = -20f; po.crouch = 22f; po.ffx += 24f; po.rfx += 24f
            } else {
                po.set(Poses.CROSS); po.crouch = 14f + sin(anim * 20f) * 3f
            }
            return
        }
        for (i in sp.strikes.indices) {
            val df = abs(sf - sp.strikes[i].frame)
            if (df <= 5) {
                val u = 1f - df / 6f
                po.mix(Poses.GUARD, if (i % 2 == 0) Poses.SP_A else Poses.SP_B, ease(u))
                po.glow = 1f
                return
            }
        }
        po.lean = 8f
    }

    private fun ik(sx: Float, sy: Float, tx: Float, ty: Float, l1: Float, l2: Float, forward: Boolean) {
        var dx = tx - sx
        var dy = ty - sy
        var d = sqrt(dx * dx + dy * dy)
        val maxD = l1 + l2 - 0.5f
        if (d > maxD) { val k = maxD / d; dx *= k; dy *= k; d = maxD }
        if (d < 8f) d = 8f
        ex = sx + dx
        ey = sy + dy
        val a = (l1 * l1 - l2 * l2 + d * d) / (2f * d)
        val h = sqrt(max(0f, l1 * l1 - a * a))
        val mx = sx + dx * a / d
        val my = sy + dy * a / d
        val px = -dy / d
        val py = dx / d
        val c1x = mx + px * h
        val c1y = my + py * h
        val c2x = mx - px * h
        val c2y = my - py * h
        val pickFirst = if (forward) c1x >= c2x else c1y >= c2y
        kx = if (pickFirst) c1x else c2x
        ky = if (pickFirst) c1y else c2y
    }

    private fun seg(c: Canvas, ax: Float, ay: Float, bx: Float, by: Float, w: Float, col: Int) {
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = w + 7f
        p.color = ol
        c.drawLine(ax, ay, bx, by, p)
        p.strokeWidth = w
        p.color = col
        c.drawLine(ax, ay, bx, by, p)
    }

    private fun fillShape(c: Canvas, fill: Int) {
        p.style = Paint.Style.FILL
        p.color = fill
        c.drawPath(path, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 3.5f
        p.strokeJoin = Paint.Join.ROUND
        p.color = ol
        c.drawPath(path, p)
    }

    private fun disc(c: Canvas, x: Float, y: Float, r: Float, fill: Int, outline: Boolean = true) {
        p.style = Paint.Style.FILL
        p.color = fill
        c.drawCircle(x, y, r, p)
        if (outline) {
            p.style = Paint.Style.STROKE
            p.strokeWidth = 3.5f
            p.color = ol
            c.drawCircle(x, y, r, p)
        }
    }

    fun draw(c: Canvas, f: Fighter, wx: Float, gy: Float, anim: Float) {
        computePose(f, anim, pose)
        c.save()
        c.translate(wx, gy)
        p.colorFilter = null
        p.style = Paint.Style.FILL
        p.color = 0x55000000
        rect.set(-62f, -8f, 62f, 8f)
        c.drawOval(rect, p)
        if (pose.glow > 0f) {
            p.color = (0x30 + (20 * abs(sin(anim * 9f))).toInt()) shl 24 or 0xE3A72F
            rect.set(-96f, -310f, 96f, 6f)
            c.drawOval(rect, p)
        }
        if (f.facing < 0) c.scale(-1f, 1f)
        if (pose.rot != 0f) {
            c.translate(0f, -sin(rad(pose.rot)) * 26f)
            c.rotate(-pose.rot)
        }
        p.colorFilter = if (f.flash > 0) flashFilter else tintFilter
        drawBody(c, f, pose)
        p.colorFilter = null
        c.restore()
        if (f.state == St.DIZZY) drawStars(c, wx, gy - 330f, anim)
    }

    private fun drawStars(c: Canvas, x: Float, y: Float, anim: Float) {
        p.style = Paint.Style.FILL
        p.color = Pal.KUNYIT
        for (i in 0 until 3) {
            val a = anim * 5f + i * 2.094f
            c.drawCircle(x + cos(a) * 34f, y + sin(a) * 8f, 6f, p)
        }
    }

    private fun drawBody(c: Canvas, f: Fighter, po: Pose) {
        val look = f.look
        val b = look.bulk
        val skin = look.skin
        val rearSkin = shade(skin, 0.84f)
        val hipX = 0f
        val hipY = -(112f - po.crouch)
        val lr = rad(po.lean)
        val shX = hipX + sin(lr) * 92f
        val shY = hipY - cos(lr) * 92f
        val hdX = shX + sin(lr) * 36f + po.hx
        val hdY = shY - cos(lr) * 36f + po.hy

        val pantFrac = when (look.outfit) {
            Outfit.SARUNG -> 0.95f
            Outfit.GALEMBONG -> 0.85f
            Outfit.ACEH -> 1f
            Outfit.BETAWI -> 0.8f
            else -> 0.55f
        }
        val cloth1 = look.cloth1
        val cloth2 = look.cloth2

        ik(hipX - 6f, hipY, po.rfx, po.rfy, 58f, 57f, true)
        drawLeg(c, hipX - 6f, hipY, kx, ky, ex, ey, rearSkin, shade(cloth1, 0.85f), cloth2, pantFrac, look.outfit, b, true)

        val rsx = shX - 8f
        val rsy = shY + 8f
        ik(rsx, rsy, hipX + po.rgx, po.rgy + po.crouch * 0.9f, 50f, 48f, false)
        drawArm(c, rsx, rsy, kx, ky, ex, ey, if (look.sleeves) shade(cloth1, 0.85f) else rearSkin, look, b, f.gloveStyle, true)

        seg(c, hipX, hipY, shX, shY, 62f * b, skin)
        if (look.female || look.outfit == Outfit.ULOS || look.outfit == Outfit.GARUDA) {
            val tx = hipX + (shX - hipX) * 0.28f
            val ty = hipY + (shY - hipY) * 0.28f
            val top = when (look.outfit) {
                Outfit.SARUNG -> cloth2
                else -> cloth1
            }
            seg(c, tx, ty, shX, shY, 64f * b, top)
            if (look.outfit == Outfit.ULOS) {
                for (i in 1..3) {
                    val u = 0.3f + i * 0.2f
                    val sx = hipX + (shX - hipX) * u
                    val sy = hipY + (shY - hipY) * u
                    p.style = Paint.Style.STROKE
                    p.strokeWidth = 5f
                    p.color = cloth2
                    c.drawLine(sx - 28f * b, sy, sx + 28f * b, sy, p)
                }
            }
            if (look.outfit == Outfit.GARUDA) disc(c, (hipX + shX) / 2f, (hipY + shY) / 2f, 12f, cloth2)
        } else if (look.outfit == Outfit.PAPUA) {
            p.style = Paint.Style.STROKE
            p.strokeWidth = 5f
            p.color = Pal.KREM
            for (i in 1..3) {
                val u = 0.3f + i * 0.17f
                val sx = hipX + (shX - hipX) * u
                val sy = hipY + (shY - hipY) * u
                c.drawLine(sx - 22f, sy, sx + 22f, sy, p)
            }
        } else if (look.outfit == Outfit.BETANG) {
            for (i in -3..3) disc(c, shX + i * 8f, shY + 14f + abs(i) * -2f + 10f, 4f, if (i % 2 == 0) cloth2 else Pal.KREM, false)
        }
        disc(c, hipX, hipY + 6f, 30f * b, cloth1)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 6f
        p.color = cloth2
        c.drawLine(hipX - 26f * b, hipY - 14f, hipX + 26f * b, hipY - 14f, p)

        headgearBack(c, look, hdX, hdY)
        drawHead(c, f, hdX, hdY, po)
        headgearFront(c, look, hdX, hdY)

        ik(hipX + 8f, hipY, po.ffx, po.ffy, 58f, 57f, true)
        drawLeg(c, hipX + 8f, hipY, kx, ky, ex, ey, skin, cloth1, cloth2, pantFrac, look.outfit, b, false)

        val fsx = shX + 12f
        val fsy = shY + 6f
        ik(fsx, fsy, hipX + po.fgx, po.fgy + po.crouch * 0.9f, 50f, 48f, false)
        drawArm(c, fsx, fsy, kx, ky, ex, ey, if (look.sleeves) cloth1 else skin, look, b, f.gloveStyle, false)
    }

    private fun drawLeg(
        c: Canvas, hx: Float, hy: Float, kx: Float, ky: Float, fx: Float, fy: Float,
        skin: Int, cloth: Int, cloth2: Int, frac: Float, outfit: Outfit, b: Float, rear: Boolean
    ) {
        val full = outfit == Outfit.ACEH
        seg(c, kx, ky, fx, fy, 28f * b, if (full) cloth else skin)
        seg(c, hx, hy, kx, ky, 38f * b, skin)
        val px = hx + (kx - hx) * frac
        val py = hy + (ky - hy) * frac
        seg(c, hx, hy, px, py, 40f * b, cloth)
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.color = cloth2
        p.strokeWidth = 4f
        when (outfit) {
            Outfit.BATIK, Outfit.BETANG, Outfit.PAPUA -> for (i in 1..2) {
                val t = i / 3f
                disc(c, hx + (px - hx) * t, hy + (py - hy) * t, 4f, cloth2, false)
            }
            Outfit.SARUNG, Outfit.BETAWI -> for (i in 1..3) {
                val t = i / 4f
                val cx = hx + (px - hx) * t
                val cy = hy + (py - hy) * t
                c.drawLine(cx - 12f, cy - 6f, cx + 12f, cy + 6f, p)
            }
            else -> {}
        }
        p.strokeWidth = 5f
        c.drawLine(px - 14f, py + 8f, px + 14f, py - 8f, p)
        val toe = fx + 24f
        rect.set(fx - 14f, fy - 14f, toe, fy + 2f)
        p.style = Paint.Style.FILL
        p.color = if (rear) 0xFF140C08.toInt() else ol
        c.drawRoundRect(rect, 7f, 7f, p)
    }

    private fun drawArm(
        c: Canvas, sx: Float, sy: Float, ekx: Float, eky: Float, gx: Float, gy: Float,
        skin: Int, look: Look, b: Float, gloveStyle: Int, rear: Boolean
    ) {
        seg(c, sx, sy, ekx, eky, 25f * b, skin)
        seg(c, ekx, eky, gx, gy, 23f * b, skin)
        val dx = ekx - gx
        val dy = eky - gy
        val d = max(1f, sqrt(dx * dx + dy * dy))
        val cx = gx + dx / d * 18f
        val cy = gy + dy / d * 18f
        seg(c, gx, gy, cx, cy, 31f * b, Pal.KUNYIT)
        val r = 25f * sqrt(b)
        val col = if (rear) shade(look.glove, 0.88f) else look.glove
        disc(c, gx, gy, r, col)
        if (gloveStyle == 2) {
            p.style = Paint.Style.STROKE
            p.strokeWidth = 3.5f
            p.color = Pal.KUNYIT
            c.save()
            path.reset()
            path.addCircle(gx, gy, r - 1f, Path.Direction.CW)
            c.clipPath(path)
            for (i in -2..2) {
                path.reset()
                path.moveTo(gx - r, gy + i * 9f + 10f)
                path.quadTo(gx, gy + i * 9f - 8f, gx + r, gy + i * 9f + 10f)
                c.drawPath(path, p)
            }
            c.restore()
        } else if (gloveStyle == 1) {
            p.style = Paint.Style.FILL
            p.color = 0x66FFFFFF
            rect.set(gx - r * 0.55f, gy - r * 0.75f, gx + r * 0.2f, gy - r * 0.2f)
            c.drawOval(rect, p)
        }
        disc(c, gx + 6f, gy - r * 0.55f, 9f, shade(col, 0.92f))
    }

    private fun drawHead(c: Canvas, f: Fighter, x: Float, y: Float, po: Pose) {
        val look = f.look
        val b = sqrt(look.bulk)
        disc(c, x - 12f, y + 4f, 8f, shade(look.skin, 0.9f))
        val faceR = if (look.head == Headgear.KERUDUNG) 27f else 30f
        disc(c, x, y, faceR * b, look.skin)
        path.reset()
        path.moveTo(x + 28f * b, y - 2f)
        path.lineTo(x + 37f * b, y + 6f)
        path.lineTo(x + 28f * b, y + 10f)
        path.close()
        fillShape(c, shade(look.skin, 0.95f))
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = 3.5f
        p.color = ol
        when (po.face) {
            1 -> {
                c.drawLine(x + 8f, y - 8f, x + 20f, y - 3f, p)
                c.drawLine(x + 8f, y + 0f, x + 20f, y - 5f, p)
                c.drawCircle(x + 14f, y + 14f, 4f, p)
            }
            2 -> {
                c.drawCircle(x + 14f, y - 5f, 6f, p)
                c.drawCircle(x + 14f, y - 5f, 2.5f, p)
                c.drawLine(x + 6f, y + 15f, x + 22f, y + 12f, p)
            }
            3 -> {
                c.drawLine(x + 8f, y - 10f, x + 18f, y - 1f, p)
                c.drawLine(x + 18f, y - 10f, x + 8f, y - 1f, p)
                c.drawLine(x + 8f, y + 14f, x + 20f, y + 14f, p)
            }
            4 -> {
                c.drawArc(x + 7f, y - 10f, x + 21f, y + 2f, 200f, 140f, false, p)
                c.drawArc(x + 5f, y + 4f, x + 24f, y + 20f, 10f, 160f, false, p)
            }
            else -> {
                p.style = Paint.Style.FILL
                p.color = Color.WHITE
                c.drawCircle(x + 14f, y - 4f, 6.5f, p)
                p.color = ol
                c.drawCircle(x + 16f, y - 4f, 3.4f, p)
                p.style = Paint.Style.STROKE
                c.drawLine(x + 6f, y - 13f, x + 21f, y - 10f, p)
                c.drawLine(x + 8f, y + 14f, x + 20f, y + 14f, p)
            }
        }
    }

    private fun headgearBack(c: Canvas, look: Look, x: Float, y: Float) {
        when (look.head) {
            Headgear.KERUDUNG -> {
                rect.set(x - 38f, y - 40f, x + 36f, y + 42f)
                p.style = Paint.Style.FILL
                p.color = look.headC1
                c.drawOval(rect, p)
                p.style = Paint.Style.STROKE
                p.strokeWidth = 3.5f
                p.color = ol
                c.drawOval(rect, p)
            }
            Headgear.BUN -> {
                disc(c, x - 26f, y - 22f, 17f, look.hair)
                rect.set(x - 32f, y - 36f, x + 32f, y + 20f)
                p.style = Paint.Style.FILL
                p.color = look.hair
                c.drawArc(rect, 180f, 180f, true, p)
            }
            else -> {}
        }
    }

    private fun headgearFront(c: Canvas, look: Look, x: Float, y: Float) {
        val c1 = look.headC1
        val c2 = look.headC2
        when (look.head) {
            Headgear.NONE -> {
                rect.set(x - 31f, y - 33f, x + 31f, y + 27f)
                p.style = Paint.Style.FILL
                p.color = look.hair
                c.drawArc(rect, 190f, 170f, true, p)
            }
            Headgear.UDENG -> {
                rect.set(x - 33f, y - 32f, x + 33f, y - 8f)
                path.reset(); path.addRoundRect(rect, 10f, 10f, Path.Direction.CW)
                fillShape(c, c1)
                p.style = Paint.Style.STROKE; p.strokeWidth = 4f; p.color = c2
                c.drawLine(x - 30f, y - 20f, x + 30f, y - 20f, p)
                path.reset()
                path.moveTo(x + 4f, y - 31f); path.lineTo(x + 28f, y - 56f); path.lineTo(x + 34f, y - 26f); path.close()
                fillShape(c, c1)
            }
            Headgear.BLANGKON -> {
                rect.set(x - 31f, y - 48f, x + 31f, y + 2f)
                path.reset(); path.addOval(rect, Path.Direction.CW)
                fillShape(c, c1)
                p.style = Paint.Style.STROKE; p.strokeWidth = 4f; p.color = c2
                c.drawLine(x - 30f, y - 16f, x + 30f, y - 16f, p)
                disc(c, x - 34f, y - 14f, 9f, c1)
            }
            Headgear.DAYAK -> {
                rect.set(x - 33f, y - 30f, x + 33f, y - 10f)
                path.reset(); path.addRoundRect(rect, 8f, 8f, Path.Direction.CW)
                fillShape(c, c1)
                for (i in -1..1) disc(c, x + i * 18f, y - 20f, 4f, c2, false)
                for (i in 0..1) {
                    path.reset()
                    path.moveTo(x - 14f + i * 10f, y - 28f)
                    path.quadTo(x - 28f + i * 12f, y - 70f, x - 16f + i * 14f, y - 78f)
                    path.quadTo(x - 6f + i * 10f, y - 60f, x - 6f + i * 10f, y - 28f)
                    path.close()
                    fillShape(c, if (i == 0) Pal.KREM else c2)
                }
            }
            Headgear.SIGER -> {
                path.reset()
                path.moveTo(x - 31f, y - 20f)
                for (i in 0..4) {
                    path.lineTo(x - 31f + i * 12.4f + 6.2f, y - 56f)
                    path.lineTo(x - 31f + (i + 1) * 12.4f, y - 26f)
                }
                path.lineTo(x + 31f, y - 12f); path.lineTo(x - 31f, y - 12f); path.close()
                fillShape(c, c1)
                disc(c, x, y - 22f, 4.5f, c2, false)
            }
            Headgear.PECI -> {
                rect.set(x - 29f, y - 48f, x + 31f, y - 14f)
                path.reset(); path.addRoundRect(rect, 8f, 8f, Path.Direction.CW)
                fillShape(c, 0xFF1A1210.toInt())
            }
            Headgear.DETA -> {
                rect.set(x - 33f, y - 42f, x + 33f, y - 6f)
                path.reset(); path.addOval(rect, Path.Direction.CW)
                fillShape(c, c1)
                p.style = Paint.Style.STROKE; p.strokeWidth = 5f; p.color = c2
                c.drawLine(x - 24f, y - 32f, x + 22f, y - 12f, p)
                path.reset()
                path.moveTo(x - 30f, y - 24f); path.lineTo(x - 54f, y - 2f); path.lineTo(x - 24f, y - 4f); path.close()
                fillShape(c, c1)
            }
            Headgear.FEATHER -> {
                rect.set(x - 32f, y - 30f, x + 32f, y - 12f)
                path.reset(); path.addRoundRect(rect, 8f, 8f, Path.Direction.CW)
                fillShape(c, c1)
                val cols = intArrayOf(c2, Pal.CABAI, Pal.KUNYIT, Pal.CABAI, c2)
                for (i in 0..4) {
                    val a = rad(-120f + i * 30f)
                    val tx = x + cos(a) * 62f
                    val ty = y - 24f + sin(a) * 62f
                    path.reset()
                    path.moveTo(x - 4f, y - 26f); path.lineTo(tx - 6f, ty); path.lineTo(tx + 6f, ty + 4f); path.lineTo(x + 4f, y - 26f); path.close()
                    fillShape(c, cols[i])
                }
            }
            Headgear.TOPENG -> {
                path.reset()
                path.moveTo(x - 29f, y - 22f); path.lineTo(x + 31f, y - 22f); path.lineTo(x + 29f, y + 4f)
                path.lineTo(x + 8f, y + 11f); path.lineTo(x - 29f, y + 4f); path.close()
                fillShape(c, c2)
                p.style = Paint.Style.FILL; p.color = ol
                c.drawRect(x + 6f, y - 10f, x + 22f, y - 3f, p)
                for (i in 0..2) {
                    path.reset()
                    path.moveTo(x - 10f + i * 14f, y - 22f); path.lineTo(x - 18f + i * 16f, y - 62f); path.lineTo(x + 2f + i * 14f, y - 22f); path.close()
                    fillShape(c, if (i == 1) Pal.CABAI else c1)
                }
            }
            Headgear.KERUDUNG -> {
                path.reset()
                path.moveTo(x - 30f, y - 6f); path.quadTo(x + 4f, y - 46f, x + 30f, y - 10f)
                path.lineTo(x + 30f, y - 22f); path.quadTo(x + 4f, y - 54f, x - 32f, y - 22f); path.close()
                fillShape(c, c1)
                p.style = Paint.Style.STROKE; p.strokeWidth = 4f; p.color = c2
                c.drawLine(x - 28f, y - 14f, x + 28f, y - 14f, p)
            }
            Headgear.BUN -> {
                for (i in 0..4) {
                    val a = rad(i * 72f)
                    disc(c, x - 6f + cos(a) * 8f, y - 38f + sin(a) * 8f, 6.5f, Pal.KREM)
                }
                disc(c, x - 6f, y - 38f, 3.5f, Pal.KUNYIT, false)
            }
        }
    }
}

class Effects {
    private val maxSparks = 24
    private val sx = FloatArray(maxSparks)
    private val sy = FloatArray(maxSparks)
    private val sLife = IntArray(maxSparks)
    private val sMax = IntArray(maxSparks)
    private val sCol = IntArray(maxSparks)
    private val sSize = FloatArray(maxSparks)
    private val sRing = BooleanArray(maxSparks)
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    var trauma = 0f
    var flashAlpha = 0f

    fun spark(x: Float, y: Float, size: Float, color: Int) = add(x, y, size, color, false, 12)
    fun ring(x: Float, y: Float, size: Float, color: Int) = add(x, y, size, color, true, 20)

    private fun add(x: Float, y: Float, size: Float, color: Int, ring: Boolean, life: Int) {
        var idx = 0
        var oldest = Int.MAX_VALUE
        for (i in 0 until maxSparks) {
            if (sLife[i] <= 0) { idx = i; break }
            if (sLife[i] < oldest) { oldest = sLife[i]; idx = i }
        }
        sx[idx] = x; sy[idx] = y; sLife[idx] = life; sMax[idx] = life
        sCol[idx] = color; sSize[idx] = size; sRing[idx] = ring
    }

    fun addTrauma(t: Float, scale: Float) { trauma = min(1f, trauma + t * scale) }

    fun update() {
        for (i in 0 until maxSparks) if (sLife[i] > 0) sLife[i]--
        trauma = max(0f, trauma - 0.025f)
        flashAlpha = max(0f, flashAlpha - 0.04f)
    }

    fun draw(c: Canvas) {
        for (i in 0 until maxSparks) {
            if (sLife[i] <= 0) continue
            val t = 1f - sLife[i] / sMax[i].toFloat()
            p.color = sCol[i]
            p.alpha = (255 * (1f - t)).toInt()
            if (sRing[i]) {
                p.style = Paint.Style.STROKE
                p.strokeWidth = 6f * (1f - t) + 1f
                c.drawCircle(sx[i], sy[i], sSize[i] * (0.4f + t), p)
            } else {
                p.style = Paint.Style.STROKE
                p.strokeCap = Paint.Cap.ROUND
                p.strokeWidth = 6f * (1f - t) + 2f
                val r0 = sSize[i] * (0.25f + t * 0.5f)
                val r1 = sSize[i] * (0.55f + t * 0.8f)
                for (k in 0 until 8) {
                    val a = k * (PI.toFloat() / 4f) + 0.2f
                    c.drawLine(sx[i] + cos(a) * r0, sy[i] + sin(a) * r0, sx[i] + cos(a) * r1, sy[i] + sin(a) * r1, p)
                }
            }
        }
        p.alpha = 255
    }
}

class ArenaRenderer(private val ctx: Context, val arena: ArenaDef) {
    private val bg: Bitmap? = loadAssetBitmap(ctx, "arena/${arena.id}.webp")
    private val crowd: Bitmap? = loadAssetBitmap(ctx, "ui/crowd.webp")
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rect = RectF()
    private val src = android.graphics.Rect()
    private val dst = RectF()
    private val dark = PorterDuffColorFilter(0xFF3A3430.toInt(), PorterDuff.Mode.MULTIPLY)

    companion object {
        const val GROUND = 590f
        const val BACK_Y = 470f
        const val FRONT_Y = 650f
    }

    fun drawBackground(c: Canvas, visibleHalfWidth: Float) {
        val b = bg ?: run { c.drawColor(arena.warnaLantai); return }
        val s = max(1f, visibleHalfWidth * 2f / 1600f)
        dst.set(-800f * s, 360f - 360f * s, 800f * s, 360f + 360f * s)
        c.drawBitmap(b, null, dst, p)
    }

    fun drawRing(c: Canvas, anim: Float) {
        val hl = 640f
        val hb = 560f
        path.reset()
        path.moveTo(-hb, BACK_Y); path.lineTo(hb, BACK_Y); path.lineTo(hl, FRONT_Y); path.lineTo(-hl, FRONT_Y); path.close()
        p.style = Paint.Style.FILL
        p.color = lighten(arena.warnaLantai, 1.35f)
        c.drawPath(path, p)
        p.color = lighten(arena.warnaLantai, 1.12f)
        c.drawRect(-hl, FRONT_Y - 70f, hl, FRONT_Y, p)
        p.color = 0x22000000
        c.drawRect(-hb, BACK_Y, hb, BACK_Y + 14f, p)
        p.color = arena.warnaLantai
        c.drawRect(-hl - 20f, FRONT_Y, hl + 20f, FRONT_Y + 40f, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 5f
        p.color = Pal.KUNYIT
        c.drawLine(-hl - 20f, FRONT_Y + 3f, hl + 20f, FRONT_Y + 3f, p)

        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        for (i in 0 until 3) {
            val h = 70f + i * 44f
            p.strokeWidth = 7f
            p.color = Pal.SOGAN
            c.drawLine(-hb, BACK_Y - h, hb, BACK_Y - h, p)
            p.strokeWidth = 4f
            p.color = arena.warnaTali
            c.drawLine(-hb, BACK_Y - h, hb, BACK_Y - h, p)
            p.strokeWidth = 7f
            p.color = Pal.SOGAN
            c.drawLine(-hb, BACK_Y - h, -hl, FRONT_Y - h * 1.15f, p)
            c.drawLine(hb, BACK_Y - h, hl, FRONT_Y - h * 1.15f, p)
            p.strokeWidth = 4f
            p.color = arena.warnaTali
            c.drawLine(-hb, BACK_Y - h, -hl, FRONT_Y - h * 1.15f, p)
            c.drawLine(hb, BACK_Y - h, hl, FRONT_Y - h * 1.15f, p)
        }
        p.style = Paint.Style.FILL
        for (sx in floatArrayOf(-hb, hb)) {
            p.color = Pal.SOGAN
            c.drawRect(sx - 12f, BACK_Y - 170f, sx + 12f, BACK_Y + 6f, p)
            p.color = Pal.CABAI
            c.drawRect(sx - 9f, BACK_Y - 160f, sx + 9f, BACK_Y - 20f, p)
        }
        if (arena.id == "keraton") for (sx in floatArrayOf(-hb, hb)) flame(c, sx, BACK_Y - 190f, anim, 1f)
        if (arena.id == "betang") flame(c, 0f, BACK_Y + 6f, anim, 2.2f)
    }

    private fun flame(c: Canvas, x: Float, y: Float, anim: Float, s: Float) {
        val fl = 1f + 0.15f * sin(anim * 17f + x)
        p.style = Paint.Style.FILL
        p.color = 0xFFE0651F.toInt()
        rect.set(x - 14f * s, y - 40f * s * fl, x + 14f * s, y + 6f)
        c.drawOval(rect, p)
        p.color = 0xFFF7B733.toInt()
        rect.set(x - 8f * s, y - 28f * s * fl, x + 8f * s, y + 4f)
        c.drawOval(rect, p)
    }

    fun drawCrowd(c: Canvas, anim: Float, halfW: Float, cheer: Float) {
        val sheet = crowd ?: return
        if (arena.id == "garuda") return
        val cell = 96
        val n = 8
        p.colorFilter = dark
        val step = 70f
        var i = 0
        var x = -halfW
        while (x < halfW) {
            val idx = (i * 5 + i / 3) % n
            val bob = sin(anim * 4f + i * 0.9f) * (3f + 6f * cheer)
            src.set(idx * cell, 0, (idx + 1) * cell, cell)
            dst.set(x, BACK_Y - 128f + bob, x + 78f, BACK_Y - 50f + bob)
            c.drawBitmap(sheet, src, dst, p)
            x += step
            i++
        }
        p.colorFilter = null
    }

    fun drawForegroundCrowd(c: Canvas, anim: Float, halfW: Float, cheer: Float) {
        val sheet = crowd ?: return
        val cell = 96
        p.colorFilter = PorterDuffColorFilter(0xFF1A1210.toInt(), PorterDuff.Mode.MULTIPLY)
        var i = 0
        var x = -halfW
        while (x < halfW) {
            val idx = (i * 3 + 1) % 8
            val bob = sin(anim * 3.5f + i * 1.3f) * (2f + 6f * cheer)
            src.set(idx * cell, 0, (idx + 1) * cell, cell)
            dst.set(x, 690f + bob, x + 140f, 690f + 140f + bob)
            c.drawBitmap(sheet, src, dst, p)
            x += 120f
            i++
        }
        p.colorFilter = null
    }

    private fun lighten(col: Int, m: Float): Int =
        Color.rgb(min(255, (Color.red(col) * m + 12).toInt()), min(255, (Color.green(col) * m + 12).toInt()), min(255, (Color.blue(col) * m + 12).toInt()))

    fun release() {
        bg?.recycle()
        crowd?.recycle()
    }
}
