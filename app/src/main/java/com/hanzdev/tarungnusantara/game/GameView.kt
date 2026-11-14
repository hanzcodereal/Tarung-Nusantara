package com.hanzdev.tarungnusantara.game

import android.content.Context
import android.os.Build
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

/** Permukaan gambar dengan loop langkah tetap 60 Hz di thread sendiri. */
class GameView(ctx: Context) : SurfaceView(ctx), SurfaceHolder.Callback, Runnable {
    @Volatile var session: GameSession? = null
        set(value) {
            field = value
            if (viewW > 0) value?.layout(viewW, viewH)
        }
    @Volatile private var running = false
    private var thread: Thread? = null
    private var viewW = 0
    private var viewH = 0

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(h: SurfaceHolder) {
        running = true
        thread = Thread(this, "game-loop").also { it.start() }
    }

    override fun surfaceChanged(h: SurfaceHolder, format: Int, w: Int, hh: Int) {
        viewW = w
        viewH = hh
        session?.layout(w, hh)
    }

    override fun surfaceDestroyed(h: SurfaceHolder) {
        running = false
        try { thread?.join(1000) } catch (e: InterruptedException) { Thread.currentThread().interrupt() }
        thread = null
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val s = session ?: return true
        return s.touch.onTouch(event)
    }

    override fun run() {
        val step = 1_000_000_000L / FPS
        var last = System.nanoTime()
        var acc = 0L
        while (running) {
            val now = System.nanoTime()
            acc += (now - last).coerceAtMost(250_000_000L)
            last = now
            val s = session
            if (s != null) {
                var guard = 0
                while (acc >= step && guard < 6) {
                    s.update()
                    acc -= step
                    guard++
                }
                if (guard >= 6) acc = 0L
                drawFrame(s)
            } else {
                acc = 0L
            }
            try { Thread.sleep(2) } catch (e: InterruptedException) { return }
        }
    }

    private fun drawFrame(s: GameSession) {
        val locked = try {
            if (Build.VERSION.SDK_INT >= 26) holder.lockHardwareCanvas() else holder.lockCanvas()
        } catch (e: Exception) {
            null
        }
        val c = locked ?: return
        try {
            s.render(c, c.width, c.height)
        } finally {
            holder.unlockCanvasAndPost(c)
        }
    }
}
