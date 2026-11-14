package com.hanzdev.tarungnusantara.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import java.io.IOException
import java.util.UUID

/** Keadaan sesi Bluetooth yang dipakai bersama oleh layar lobi dan layar pertandingan. */
object BtSession {
    val SERVICE_UUID: UUID = UUID.fromString("7a5c3e10-6d2f-4b8a-9c41-0e5f3a1d2b77")
    const val SERVICE_NAME = "TarungNusantara"

    @Volatile var link: BtLink? = null
    var isHost = false
    var remoteDevice: BluetoothDevice? = null
    var scoreHost = 0
    var scoreClient = 0

    var remoteFighter = "bimo"
    var remoteGlove = ""
    var remoteHead = ""
    var remoteCostume = ""
    var arenaId = "keraton"
    var rounds = 3

    fun reset() {
        link?.close()
        link = null
        remoteDevice = null
        scoreHost = 0
        scoreClient = 0
    }

    fun adapter(ctx: Context): BluetoothAdapter? {
        val mgr = ctx.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
        return mgr?.adapter
    }

    @SuppressLint("MissingPermission")
    fun reconnect(timeoutMs: Int): Boolean {
        val ad = BluetoothAdapter.getDefaultAdapter() ?: return false
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            try {
                if (isHost) {
                    val server = ad.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SERVICE_UUID)
                    val left = (end - System.currentTimeMillis()).toInt().coerceAtLeast(500)
                    val sock: BluetoothSocket = try { server.accept(left) } finally { server.close() }
                    link = BtLink(sock)
                    return true
                } else {
                    val dev = remoteDevice ?: return false
                    ad.cancelDiscovery()
                    val sock = dev.createRfcommSocketToServiceRecord(SERVICE_UUID)
                    sock.connect()
                    link = BtLink(sock)
                    return true
                }
            } catch (e: IOException) {
                try { Thread.sleep(600) } catch (ie: InterruptedException) { return false }
            }
        }
        return false
    }
}
