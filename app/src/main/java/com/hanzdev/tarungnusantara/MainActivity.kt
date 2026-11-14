package com.hanzdev.tarungnusantara

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.FrameLayout
import android.widget.Toast
import com.hanzdev.tarungnusantara.audio.Sound
import com.hanzdev.tarungnusantara.data.Save
import com.hanzdev.tarungnusantara.game.Fonts
import com.hanzdev.tarungnusantara.game.SessionConfig
import com.hanzdev.tarungnusantara.ui.Nav
import com.hanzdev.tarungnusantara.ui.Screen
import com.hanzdev.tarungnusantara.ui.SplashScreen
import com.hanzdev.tarungnusantara.ui.Ui

class MainActivity : Activity(), Nav {
    override val context: Context get() = this
    private lateinit var host: FrameLayout
    private val stack = ArrayList<Screen>()
    private var btCallback: ((Boolean) -> Unit)? = null
    private var enableCallback: ((Boolean) -> Unit)? = null
    private var needRefresh = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Save.init(this)
        Fonts.init(this)
        Ui.init(this)
        Sound.init(this)
        host = FrameLayout(this)
        setContentView(host)
        hideSystemUi()
        push(SplashScreen())
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

    private fun show(s: Screen, animate: Boolean = true) {
        host.removeAllViews()
        val v = s.build(this)
        host.addView(v, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        if (animate && !Save.reduceMotion) {
            v.alpha = 0f
            v.animate().alpha(1f).setDuration(140).start()
        }
        s.onShow()
        Sound.playMusic(s.music, s.musicLoop)
    }

    override fun push(screen: Screen) {
        stack.lastOrNull()?.onHide()
        screen.nav = this
        stack.add(screen)
        show(screen)
    }

    override fun pop() {
        if (stack.size <= 1) { finish(); return }
        stack.removeAt(stack.lastIndex).onHide()
        show(stack.last())
    }

    override fun replace(screen: Screen) {
        if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex).onHide()
        screen.nav = this
        stack.add(screen)
        show(screen)
    }

    override fun popTo(count: Int) {
        while (stack.size > count && stack.size > 1) stack.removeAt(stack.lastIndex).onHide()
        show(stack.last())
    }

    override fun startGame(cfg: SessionConfig) {
        needRefresh = true
        startActivity(cfg.toIntent(this))
    }

    override fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            toast("Tidak ada aplikasi untuk membuka tautan ini.")
        }
    }

    override fun sendEmail(address: String) {
        val i = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$address"))
        i.putExtra(Intent.EXTRA_SUBJECT, "Tarung Nusantara 1.0.0")
        try {
            startActivity(i)
        } catch (e: ActivityNotFoundException) {
            toast("Tidak ada aplikasi email. Alamatnya: $address")
        }
    }

    override fun music(file: String) = Sound.playMusic(file)

    override fun requestBluetooth(onResult: (Boolean) -> Unit) {
        val perms = if (Build.VERSION.SDK_INT >= 31) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        val missing = perms.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) { onResult(true); return }
        btCallback = onResult
        requestPermissions(missing.toTypedArray(), 77)
    }

    @Suppress("DEPRECATION")
    override fun requestEnableBluetooth(onResult: (Boolean) -> Unit) {
        enableCallback = onResult
        startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 78)
    }

    override fun requestDiscoverable() {
        val i = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120)
        try {
            startActivity(i)
        } catch (e: ActivityNotFoundException) {
            toast("Tidak bisa membuat perangkat terlihat. Atur lewat pengaturan Bluetooth.")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 77) {
            val ok = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            btCallback?.invoke(ok)
            btCallback = null
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 78) {
            enableCallback?.invoke(resultCode == RESULT_OK)
            enableCallback = null
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        val top = stack.lastOrNull()
        if (top != null && top.onBack()) return
        pop()
    }

    override fun onResume() {
        super.onResume()
        hideSystemUi()
        val top = stack.lastOrNull() ?: return
        if (needRefresh) {
            needRefresh = false
            top.nav = this
            show(top, false)
        } else {
            Sound.playMusic(top.music)
        }
    }

    override fun onPause() {
        super.onPause()
        Sound.pauseMusic()
    }
}
