package com.hanzdev.tarungnusantara.bluetooth

import android.bluetooth.BluetoothSocket
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentLinkedQueue

/** Satu sambungan RFCOMM dua arah dengan bingkai [tipe][panjang][data] dan pengukur latensi. */
class BtLink(private val socket: BluetoothSocket) {
    private val input = DataInputStream(socket.inputStream)
    private val output = DataOutputStream(BufferedOutputStream(socket.outputStream))
    private val inbox = ConcurrentLinkedQueue<Packet>()
    private val writeLock = Any()

    @Volatile var alive = true
        private set
    @Volatile var rttMs = -1
        private set
    @Volatile private var lastRx = System.currentTimeMillis()

    init {
        Thread({ readLoop() }, "bt-read").start()
        Thread({ pingLoop() }, "bt-ping").start()
    }

    private fun readLoop() {
        try {
            while (alive) {
                val type = input.readUnsignedByte()
                val len = input.readUnsignedShort()
                val data = ByteArray(len)
                input.readFully(data)
                lastRx = System.currentTimeMillis()
                when (type) {
                    Msg.PING -> send(Msg.PONG, data)
                    Msg.PONG -> {
                        if (data.size >= 8) {
                            val sent = java.nio.ByteBuffer.wrap(data).long
                            rttMs = (System.currentTimeMillis() - sent).toInt().coerceAtLeast(0)
                        }
                    }
                    else -> inbox.add(Packet(type, data))
                }
            }
        } catch (e: IOException) {
            alive = false
        }
    }

    private fun pingLoop() {
        while (alive) {
            val now = System.currentTimeMillis()
            if (now - lastRx > 5000L) { alive = false; break }
            send(Msg.PING, java.nio.ByteBuffer.allocate(8).putLong(now).array())
            try { Thread.sleep(1000) } catch (e: InterruptedException) { return }
        }
    }

    fun send(type: Int, data: ByteArray) {
        if (!alive) return
        synchronized(writeLock) {
            try {
                output.writeByte(type)
                output.writeShort(data.size)
                output.write(data)
                output.flush()
            } catch (e: IOException) {
                alive = false
            }
        }
    }

    fun poll(): Packet? = inbox.poll()

    fun close() {
        alive = false
        try { socket.close() } catch (e: IOException) { return }
    }
}
