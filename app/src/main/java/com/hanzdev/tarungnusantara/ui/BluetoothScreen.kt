package com.hanzdev.tarungnusantara.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import com.hanzdev.tarungnusantara.audio.Sound
import com.hanzdev.tarungnusantara.bluetooth.BtLink
import com.hanzdev.tarungnusantara.bluetooth.BtSession
import com.hanzdev.tarungnusantara.bluetooth.Msg
import com.hanzdev.tarungnusantara.bluetooth.Packet
import com.hanzdev.tarungnusantara.bluetooth.SyncProtocol
import com.hanzdev.tarungnusantara.data.Save
import com.hanzdev.tarungnusantara.data.Shop
import com.hanzdev.tarungnusantara.data.Teks
import com.hanzdev.tarungnusantara.game.Mode
import com.hanzdev.tarungnusantara.game.Pal
import com.hanzdev.tarungnusantara.game.Roster
import com.hanzdev.tarungnusantara.game.SessionConfig
import java.io.IOException

private class Found(val device: BluetoothDevice, var rssi: Int?)

class BluetoothScreen : Screen() {
    private enum class Stage { CHOOSE, HOSTING, SCANNING, CONNECTING, LOBBY }

    private var stage = Stage.CHOOSE
    private var message: String? = null
    private val main = Handler(Looper.getMainLooper())
    private var container: FrameLayout? = null
    private var ctxRef: Context? = null
    private var adapter: BluetoothAdapter? = null
    private val found = LinkedHashMap<String, Found>()
    private var scanning = false
    private var receiver: BroadcastReceiver? = null
    private var server: BluetoothServerSocket? = null

    private var myFighter = currentPlayer().id
    private var remoteFighter = ""
    private var myReady = false
    private var remoteReady = false
    private var arenaId = "keraton"
    private var rounds = 3
    private var polling = false

    override fun build(ctx: Context): View {
        ctxRef = ctx
        adapter = BtSession.adapter(ctx)
        val box = FrameLayout(ctx)
        container = box
        render()
        return Ui.frame(ctx, "Mabar Bluetooth", { if (!onBack()) nav.pop() }, false, box)
    }

    override fun onShow() {
        val l = BtSession.link
        if (stage == Stage.LOBBY) {
            if (l == null || !l.alive) {
                message = "Sesi mabar berakhir."
                stage = Stage.CHOOSE
                resetLobby()
                render()
            } else {
                myReady = false
                remoteReady = false
                startPolling()
                l.send(Msg.READY, "0".toByteArray())
                render()
            }
        }
    }

    override fun onHide() {
        polling = false
        stopScan()
    }

    override fun onBack(): Boolean {
        when (stage) {
            Stage.LOBBY -> {
                BtSession.link?.send(Msg.LEAVE, ByteArray(0))
                BtSession.reset()
                polling = false
                resetLobby()
                stage = Stage.CHOOSE
                message = null
                render()
                return true
            }
            Stage.HOSTING -> { cancelHost(); return true }
            Stage.SCANNING, Stage.CONNECTING -> { stopScan(); stage = Stage.CHOOSE; render(); return true }
            else -> return false
        }
    }

    private fun resetLobby() {
        remoteFighter = ""
        myReady = false
        remoteReady = false
    }

    private fun render() {
        val box = container ?: return
        val ctx = ctxRef ?: return
        box.removeAllViews()
        val v = when (stage) {
            Stage.CHOOSE -> chooseView(ctx)
            Stage.HOSTING -> hostingView(ctx)
            Stage.SCANNING -> scanView(ctx)
            Stage.CONNECTING -> waitView(ctx, "Menyambung...", "Menunggu perangkat lawan menerima sambungan.")
            Stage.LOBBY -> lobbyView(ctx)
        }
        box.addView(v, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    private fun scroll(ctx: Context, inner: View): View {
        val sv = ScrollView(ctx)
        sv.addView(inner)
        return sv
    }

    private fun chooseView(ctx: Context): View {
        val p = Ui.panel(ctx, 18)
        p.addView(Ui.text(ctx, "Main berdua lewat Bluetooth", 20f, Pal.KUNYIT, 2))
        p.addView(Ui.spacer(ctx, 1, 6))
        p.addView(Ui.text(ctx, "Dua perangkat Android berdekatan, tanpa internet. Satu pemain membuat ruangan, pemain lain mencarinya. Petarung dan kostum pilihanmu dipakai, tetapi upgrade statistik dimatikan supaya adil.", 13f, Pal.KREM, 0))
        p.addView(Ui.spacer(ctx, 1, 6))
        p.addView(Ui.text(ctx, "Skor dan koin tidak berubah di mode ini, hanya pencapaian dan hitungan main.", 12f, 0xFFC9BCA2.toInt(), 0))
        message?.let {
            p.addView(Ui.spacer(ctx, 1, 8))
            p.addView(Ui.text(ctx, it, 13f, Pal.KUNYIT, 1))
        }
        p.addView(Ui.spacer(ctx, 1, 12))
        val ok = adapter != null
        if (!ok) p.addView(Ui.text(ctx, "Perangkat ini tidak mendukung Bluetooth.", 13f, Pal.KUNYIT, 1))
        val row = LinearLayout(ctx)
        row.orientation = LinearLayout.HORIZONTAL
        row.addView(Ui.button(ctx, "Buat Ruangan", Kind.PRIMARY, ok) { ensureReady { startHost() } }, Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(Ui.spacer(ctx, 10, 1))
        row.addView(Ui.button(ctx, "Cari Ruangan", Kind.SECONDARY, ok) { ensureReady { startScan() } }, Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        p.addView(row)
        return scroll(ctx, p)
    }

    private fun waitView(ctx: Context, title: String, body: String): View {
        val p = Ui.panel(ctx, 18)
        p.addView(Ui.text(ctx, title, 20f, Pal.KUNYIT, 2))
        p.addView(Ui.spacer(ctx, 1, 6))
        p.addView(Ui.text(ctx, body, 13f, Pal.KREM, 0))
        return scroll(ctx, p)
    }

    private fun hostingView(ctx: Context): View {
        val p = Ui.panel(ctx, 18)
        p.addView(Ui.text(ctx, "Ruangan terbuka", 20f, Pal.KUNYIT, 2))
        p.addView(Ui.spacer(ctx, 1, 6))
        p.addView(Ui.text(ctx, "Menunggu pemain lain. Minta temanmu memilih Cari Ruangan di perangkatnya, lalu pilih perangkat ini dari daftar.", 13f, Pal.KREM, 0))
        p.addView(Ui.spacer(ctx, 1, 6))
        p.addView(Ui.text(ctx, "Perangkat ini terlihat selama 2 menit. Bila temanmu belum menemukannya, pasangkan dulu kedua perangkat lewat pengaturan Bluetooth Android.", 12f, 0xFFC9BCA2.toInt(), 0))
        p.addView(Ui.spacer(ctx, 1, 12))
        p.addView(Ui.button(ctx, "Tutup Ruangan", Kind.SECONDARY) { cancelHost() })
        return scroll(ctx, p)
    }

    private fun scanView(ctx: Context): View {
        val c = LinearLayout(ctx)
        c.orientation = LinearLayout.VERTICAL
        val head = LinearLayout(ctx)
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL
        head.addView(Ui.text(ctx, if (scanning) "Mencari ruangan..." else "Pencarian selesai", 15f, Pal.KUNYIT, 1), Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        head.addView(Ui.button(ctx, "Cari Lagi", Kind.SECONDARY, !scanning) { startScan() })
        c.addView(head)
        message?.let { c.addView(Ui.text(ctx, it, 12.5f, Pal.KUNYIT, 1)) }
        c.addView(Ui.spacer(ctx, 1, 8))
        val list = LinearLayout(ctx)
        list.orientation = LinearLayout.VERTICAL
        if (found.isEmpty()) {
            list.addView(Ui.text(ctx, if (scanning) "Belum ada perangkat ditemukan." else "Tidak ada perangkat ditemukan. Pastikan temanmu sudah memilih Buat Ruangan dan perangkatnya terlihat.", 13f, Pal.KREM, 0))
        }
        for (f in found.values) list.addView(deviceRow(ctx, f))
        val sv = ScrollView(ctx)
        sv.addView(list)
        c.addView(sv, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        return c
    }

    @SuppressLint("MissingPermission")
    private fun deviceRow(ctx: Context, f: Found): View {
        val card = Ui.panel(ctx, 10)
        val row = LinearLayout(ctx)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        val rawName = try { f.device.name } catch (e: SecurityException) { null }
        val name = rawName ?: "Perangkat tanpa nama"
        val info = LinearLayout(ctx)
        info.orientation = LinearLayout.VERTICAL
        info.addView(Ui.text(ctx, name, 15f, Pal.KREM, 1))
        info.addView(Ui.text(ctx, f.device.address.takeLast(8), 11f, 0xFFC9BCA2.toInt(), 0))
        row.addView(info, Ui.lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val r = f.rssi
        val level = when { r == null -> 0; r >= -60 -> 4; r >= -70 -> 3; r >= -80 -> 2; else -> 1 }
        val sig = SignalView(ctx, level)
        sig.contentDescription = if (r == null) "Sinyal tidak diketahui, perangkat sudah dipasangkan" else "Kekuatan sinyal $level dari 4"
        row.addView(sig, Ui.lp(Ui.dp(28), Ui.dp(20)))
        row.addView(Ui.spacer(ctx, 10, 1))
        row.addView(Ui.button(ctx, "Sambung", Kind.PRIMARY) { connectTo(f.device, name) })
        card.addView(row)
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.bottomMargin = Ui.dp(8)
        card.layoutParams = lp
        return card
    }

    private fun ensureReady(onOk: () -> Unit) {
        val ad = adapter
        if (ad == null) { message = "Perangkat ini tidak mendukung Bluetooth."; render(); return }
        nav.requestBluetooth { granted ->
            if (!granted) {
                message = "Izin Bluetooth ditolak. Izinkan lewat pengaturan aplikasi agar bisa mabar."
                render()
            } else if (!ad.isEnabled) {
                nav.requestEnableBluetooth { ok ->
                    if (ok) onOk() else { message = "Bluetooth masih mati."; render() }
                }
            } else {
                message = null
                onOk()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startHost() {
        val ad = adapter ?: return
        BtSession.reset()
        BtSession.isHost = true
        stage = Stage.HOSTING
        message = null
        render()
        nav.requestDiscoverable()
        Thread {
            try {
                val srv = ad.listenUsingRfcommWithServiceRecord(BtSession.SERVICE_NAME, BtSession.SERVICE_UUID)
                server = srv
                val sock = srv.accept()
                srv.close()
                server = null
                main.post { onConnected(sock) }
            } catch (e: IOException) {
                main.post {
                    if (stage == Stage.HOSTING) {
                        message = "Ruangan ditutup."
                        stage = Stage.CHOOSE
                        render()
                    }
                }
            }
        }.start()
    }

    private fun cancelHost() {
        stage = Stage.CHOOSE
        message = null
        try { server?.close() } catch (e: IOException) { server = null }
        server = null
        render()
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun startScan() {
        val ad = adapter ?: return
        val ctx = ctxRef ?: return
        BtSession.reset()
        BtSession.isHost = false
        stage = Stage.SCANNING
        message = null
        found.clear()
        for (d in ad.bondedDevices ?: emptySet()) found[d.address] = Found(d, null)
        stopScan()
        val rc = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                when (i.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val d: BluetoothDevice? = if (Build.VERSION.SDK_INT >= 33) i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        else i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        val rssi = i.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                        if (d != null) {
                            val e = found[d.address]
                            if (e != null) e.rssi = if (rssi == Short.MIN_VALUE) e.rssi else rssi.toInt()
                            else found[d.address] = Found(d, if (rssi == Short.MIN_VALUE) null else rssi.toInt())
                            if (stage == Stage.SCANNING) render()
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        scanning = false
                        if (stage == Stage.SCANNING) render()
                    }
                }
            }
        }
        receiver = rc
        val f = IntentFilter()
        f.addAction(BluetoothDevice.ACTION_FOUND)
        f.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        ctx.registerReceiver(rc, f)
        scanning = ad.startDiscovery()
        if (!scanning) message = "Pencarian tidak bisa dimulai. Coba matikan lalu nyalakan Bluetooth."
        render()
    }

    @SuppressLint("MissingPermission")
    private fun stopScan() {
        val ctx = ctxRef
        receiver?.let { try { ctx?.unregisterReceiver(it) } catch (e: IllegalArgumentException) { receiver = null } }
        receiver = null
        if (scanning) { try { adapter?.cancelDiscovery() } catch (e: SecurityException) { scanning = false } }
        scanning = false
    }

    @SuppressLint("MissingPermission")
    private fun connectTo(device: BluetoothDevice, name: String) {
        stopScan()
        stage = Stage.CONNECTING
        render()
        Thread {
            try {
                val sock = device.createRfcommSocketToServiceRecord(BtSession.SERVICE_UUID)
                sock.connect()
                main.post { onConnected(sock) }
            } catch (e: IOException) {
                main.post {
                    message = "Gagal tersambung ke $name. Pastikan temanmu sudah memilih Buat Ruangan."
                    stage = Stage.SCANNING
                    render()
                }
            }
        }.start()
    }

    private fun onConnected(sock: BluetoothSocket) {
        stopScan()
        BtSession.link?.close()
        BtSession.link = BtLink(sock)
        BtSession.remoteDevice = sock.remoteDevice
        stage = Stage.LOBBY
        message = null
        resetLobby()
        Sound.play("unlock")
        sendHello()
        if (BtSession.isHost) sendConfig()
        startPolling()
        render()
    }

    private fun sendHello() {
        val ids = Shop.equipIds(myFighter)
        BtSession.link?.send(Msg.HELLO, SyncProtocol.writeStrings(myFighter, ids[0], ids[1], ids[2]))
    }

    private fun sendConfig() {
        BtSession.link?.send(Msg.CONFIG, SyncProtocol.writeStrings(arenaId, rounds.toString()))
    }

    private fun setReady(v: Boolean) {
        myReady = v
        BtSession.link?.send(Msg.READY, (if (v) "1" else "0").toByteArray())
        render()
        tryStart()
    }

    private fun configChanged() {
        if (myReady) { myReady = false; BtSession.link?.send(Msg.READY, "0".toByteArray()) }
    }

    private fun startPolling() {
        if (polling) return
        polling = true
        main.post(object : Runnable {
            override fun run() {
                if (!polling) return
                val l = BtSession.link
                if (stage == Stage.LOBBY) {
                    if (l == null || !l.alive) {
                        polling = false
                        message = "Koneksi terputus."
                        stage = Stage.CHOOSE
                        BtSession.reset()
                        resetLobby()
                        render()
                        return
                    }
                    var changed = false
                    while (true) {
                        val pk = l.poll() ?: break
                        if (handle(pk)) changed = true
                    }
                    if (changed) render()
                }
                main.postDelayed(this, 90)
            }
        })
    }

    private fun handle(pk: Packet): Boolean {
        when (pk.type) {
            Msg.HELLO -> {
                val s = SyncProtocol.readStrings(pk.data, 4)
                remoteFighter = s[0]
                BtSession.remoteFighter = s[0]
                BtSession.remoteGlove = s[1]
                BtSession.remoteHead = s[2]
                BtSession.remoteCostume = s[3]
                return true
            }
            Msg.CONFIG -> {
                val s = SyncProtocol.readStrings(pk.data, 2)
                arenaId = s[0]
                rounds = s[1].toIntOrNull() ?: 3
                myReady = false
                return true
            }
            Msg.READY -> {
                remoteReady = String(pk.data) == "1"
                tryStart()
                return true
            }
            Msg.START -> {
                if (!BtSession.isHost) launch()
                return false
            }
            Msg.LEAVE -> {
                message = "Lawan meninggalkan ruangan."
                polling = false
                BtSession.reset()
                resetLobby()
                stage = Stage.CHOOSE
                return true
            }
        }
        return false
    }

    private fun tryStart() {
        if (BtSession.isHost && myReady && remoteReady && remoteFighter.isNotEmpty()) {
            sendConfig()
            BtSession.link?.send(Msg.START, ByteArray(0))
            launch()
        }
    }

    private fun launch() {
        polling = false
        val host = BtSession.isHost
        val mine = Shop.equipIds(myFighter)
        val theirs = arrayOf(BtSession.remoteGlove, BtSession.remoteHead, BtSession.remoteCostume)
        val hostIds = if (host) mine else theirs
        val clientIds = if (host) theirs else mine
        val hostFighter = if (host) myFighter else remoteFighter
        val clientFighter = if (host) remoteFighter else myFighter
        val cfg = SessionConfig(
            if (host) Mode.BT_HOST else Mode.BT_CLIENT, hostFighter, clientFighter, arenaId, 0, rounds,
            btGloves = arrayOf(hostIds[0], clientIds[0]),
            btHeads = arrayOf(hostIds[1], clientIds[1]),
            btCostumes = arrayOf(hostIds[2], clientIds[2])
        )
        myReady = false
        remoteReady = false
        nav.startGame(cfg)
    }

    private fun lobbyView(ctx: Context): View {
        val root = LinearLayout(ctx)
        root.orientation = LinearLayout.HORIZONTAL
        val me = Roster.byId(myFighter)

        val mine = LinearLayout(ctx)
        mine.orientation = LinearLayout.VERTICAL
        mine.gravity = Gravity.CENTER_HORIZONTAL
        mine.addView(Ui.text(ctx, if (BtSession.isHost) "Kamu (tuan rumah)" else "Kamu (tamu)", 12f, Pal.KUNYIT, 1))
        mine.addView(fighterView(ctx, me, false, true), Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        mine.addView(Ui.text(ctx, me.nama, 15f, Pal.KREM, 2))
        mine.addView(Ui.button(ctx, "Ganti Petarung", Kind.SECONDARY) {
            nav.push(FighterPickScreen(false) { d ->
                Save.lastFighter = d.id
                myFighter = d.id
                configChanged()
                sendHello()
                nav.pop()
            })
        })
        root.addView(mine, Ui.lp(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        root.addView(Ui.spacer(ctx, 8, 1))

        val opp = LinearLayout(ctx)
        opp.orientation = LinearLayout.VERTICAL
        opp.gravity = Gravity.CENTER_HORIZONTAL
        opp.addView(Ui.text(ctx, if (BtSession.isHost) "Tamu" else "Tuan rumah", 12f, Pal.KUNYIT, 1))
        if (remoteFighter.isNotEmpty()) {
            val d = Roster.byId(remoteFighter)
            val ids = arrayOf(BtSession.remoteGlove, BtSession.remoteHead, BtSession.remoteCostume)
            val lk = Shop.lookWith(d, ids[0], ids[1], ids[2])
            val fv = FighterView(ctx)
            fv.setFighter(d, lk.first, lk.second)
            fv.scaleX = -1f
            fv.contentDescription = "Gambar ${d.nama}"
            opp.addView(fv, Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            opp.addView(Ui.text(ctx, d.nama, 15f, Pal.KREM, 2))
        } else {
            opp.addView(Ui.text(ctx, "Lawan belum memilih petarung.", 13f, Pal.KREM, 0), Ui.lp(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        }
        opp.addView(Ui.text(ctx, if (remoteReady) "Lawan siap" else "Lawan belum siap", 12.5f, if (remoteReady) Pal.KUNYIT else Pal.KREM, 1))
        root.addView(opp, Ui.lp(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        root.addView(Ui.spacer(ctx, 8, 1))

        val p = Ui.panel(ctx, 14)
        val l = BtSession.link
        val rtt = l?.rttMs ?: -1
        val q = when { rtt < 0 -> 0; rtt < 60 -> 4; rtt < 120 -> 3; rtt < 220 -> 2; else -> 1 }
        val qrow = LinearLayout(ctx)
        qrow.orientation = LinearLayout.HORIZONTAL
        qrow.gravity = Gravity.CENTER_VERTICAL
        val sig = SignalView(ctx, q)
        sig.contentDescription = "Kualitas sambungan $q dari 4"
        qrow.addView(sig, Ui.lp(Ui.dp(28), Ui.dp(20)))
        qrow.addView(Ui.spacer(ctx, 8, 1))
        qrow.addView(Ui.text(ctx, if (rtt >= 0) "Latensi $rtt ms" else "Mengukur latensi...", 12.5f, Pal.KREM, 1))
        p.addView(qrow)
        p.addView(Ui.spacer(ctx, 1, 8))
        if (BtSession.isHost) {
            p.addView(Ui.text(ctx, "Arena", 12f, 0xFFC9BCA2.toInt(), 0))
            p.addView(Ui.button(ctx, "${Roster.arenaById(arenaId).nama}  (ganti)", Kind.SECONDARY) {
                nav.push(ArenaPickScreen(arenaId) { a ->
                    arenaId = a.id
                    configChanged()
                    sendConfig()
                    nav.pop()
                })
            })
            p.addView(Ui.spacer(ctx, 1, 6))
            p.addView(Ui.text(ctx, "Jumlah ronde", 12f, 0xFFC9BCA2.toInt(), 0))
            val opts = listOf(1, 3, 5)
            p.addView(Widgets.choice(ctx, opts.map { "$it" }, opts.indexOf(rounds).coerceAtLeast(0)) {
                rounds = opts[it]
                configChanged()
                sendConfig()
            })
        } else {
            p.addView(Ui.text(ctx, "Arena dan ronde dipilih tuan rumah.", 12f, Pal.KREM, 0))
            p.addView(Ui.text(ctx, "${Roster.arenaById(arenaId).nama}, $rounds ronde", 14f, Pal.KUNYIT, 1))
        }
        p.addView(Ui.spacer(ctx, 1, 10))
        val label = if (myReady) "Batal Siap" else "Siap Bertanding"
        p.addView(Ui.button(ctx, label, if (myReady) Kind.SECONDARY else Kind.PRIMARY, remoteFighter.isNotEmpty()) { setReady(!myReady) })
        if (remoteFighter.isEmpty()) p.addView(Ui.text(ctx, "Menunggu lawan memilih petarung.", 11.5f, 0xFFC9BCA2.toInt(), 0))
        else if (remoteReady && !myReady) p.addView(Ui.text(ctx, Teks.LAWAN_SIAP, 12f, Pal.KUNYIT, 1))
        else if (myReady && !remoteReady) p.addView(Ui.text(ctx, "Menunggu lawan siap...", 11.5f, 0xFFC9BCA2.toInt(), 0))
        val sv = ScrollView(ctx)
        sv.addView(p)
        root.addView(sv, Ui.lp(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.2f))
        return root
    }
}
