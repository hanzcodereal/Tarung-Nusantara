package com.hanzdev.tarungnusantara.bluetooth

import com.hanzdev.tarungnusantara.game.FighterInput
import com.hanzdev.tarungnusantara.game.Match
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

object Msg {
    const val HELLO = 1
    const val CONFIG = 2
    const val READY = 3
    const val START = 4
    const val INPUT = 5
    const val SNAP = 6
    const val REMATCH_REQ = 7
    const val REMATCH_OK = 8
    const val LEAVE = 9
    const val PING = 10
    const val PONG = 11
}

class Packet(val type: Int, val data: ByteArray)

/** Penampung kejadian pertandingan yang dikirim bersama snapshot ke client. */
class NetEvents {
    private val types = IntArray(32)
    private val whos = IntArray(32)
    private val xs = FloatArray(32)
    var count = 0
        private set

    @Synchronized fun add(type: Int, who: Int, x: Float) {
        if (count >= 32) return
        types[count] = type; whos[count] = who; xs[count] = x
        count++
    }

    @Synchronized fun clear() { count = 0 }
    fun type(i: Int) = types[i]
    fun who(i: Int) = whos[i]
    fun x(i: Int) = xs[i]
}

object SyncProtocol {
    private val methods = arrayOf("", "KO", "TKO", "Poin")

    fun writeInput(inp: FighterInput): ByteArray {
        val b = ByteArray(3)
        b[0] = inp.takeEdges().toByte()
        b[1] = (inp.moveX * 100f).toInt().coerceIn(-100, 100).toByte()
        b[2] = ((if (inp.duck) 1 else 0) or (if (inp.block) 2 else 0)).toByte()
        return b
    }

    fun readInput(d: ByteArray, inp: FighterInput) {
        if (d.size < 3) return
        val edges = d[0].toInt() and 0xFF
        inp.moveX = d[1].toInt() / 100f
        inp.duck = (d[2].toInt() and 1) != 0
        inp.block = (d[2].toInt() and 2) != 0
        var bit = 1
        while (bit <= 64) {
            if (edges and bit != 0) inp.press(bit)
            bit = bit shl 1
        }
    }

    fun writeSnapshot(m: Match, ev: NetEvents): ByteArray {
        val bo = ByteArrayOutputStream(128)
        val o = DataOutputStream(bo)
        o.writeByte(m.phase)
        o.writeByte(m.round)
        o.writeShort(m.timer)
        o.writeByte(m.winner)
        o.writeByte((if (m.matchOver) 1 else 0) or (if (m.drawResult) 2 else 0))
        o.writeByte(methods.indexOf(m.method).coerceAtLeast(0))
        o.writeByte(m.roundWins[0])
        o.writeByte(m.roundWins[1])
        o.writeShort(m.phaseFrame)
        for (f in m.fighters) {
            o.writeShort((f.x * 16f).toInt())
            o.writeByte(f.facing)
            o.writeShort((f.hp * 10f).toInt())
            o.writeShort((f.maxHp * 10f).toInt())
            o.writeShort((f.stam * 10f).toInt())
            o.writeShort((f.maxStam * 10f).toInt())
            o.writeByte(f.meter.toInt())
            o.writeByte(f.state)
            o.writeShort(f.frame)
            o.writeByte(f.attackKind)
            o.writeByte(f.startupF)
            o.writeByte(f.recoverF)
            o.writeShort(f.specFrame)
            o.writeByte((if (f.counterFired) 1 else 0) or (if (f.swapped) 2 else 0))
            o.writeByte(f.dodgeDir)
            o.writeByte(f.flash)
            o.writeShort((f.dmgDealt * 10f).toInt().coerceAtMost(32000))
            o.writeShort(f.hitsTaken)
            o.writeShort(f.perfects)
            o.writeShort(f.thrown)
            o.writeShort(f.landed)
            o.writeByte(f.downCount)
            o.writeByte(f.knockdowns)
        }
        val n = ev.count
        o.writeByte(n)
        for (i in 0 until n) {
            o.writeByte(ev.type(i))
            o.writeByte(ev.who(i))
            o.writeShort((ev.x(i) * 16f).toInt())
        }
        return bo.toByteArray()
    }

    fun readSnapshot(data: ByteArray, m: Match, sink: (Int, Int, Float) -> Unit) {
        try {
            val i = DataInputStream(ByteArrayInputStream(data))
            m.phase = i.readByte().toInt()
            m.round = i.readByte().toInt()
            m.timer = i.readShort().toInt()
            m.winner = i.readByte().toInt()
            val fl = i.readByte().toInt()
            m.matchOver = (fl and 1) != 0
            m.drawResult = (fl and 2) != 0
            m.method = methods[i.readByte().toInt().coerceIn(0, 3)]
            m.roundWins[0] = i.readByte().toInt()
            m.roundWins[1] = i.readByte().toInt()
            m.phaseFrame = i.readShort().toInt()
            for (f in m.fighters) {
                f.x = i.readShort() / 16f
                f.facing = i.readByte().toInt()
                f.hp = i.readShort() / 10f
                f.maxHp = i.readShort() / 10f
                f.stam = i.readShort() / 10f
                f.maxStam = i.readShort() / 10f
                f.meter = i.readByte().toInt().toFloat()
                f.state = i.readByte().toInt()
                f.frame = i.readShort().toInt()
                f.attackKind = i.readByte().toInt()
                f.startupF = i.readByte().toInt()
                f.recoverF = i.readByte().toInt()
                f.specFrame = i.readShort().toInt()
                val sf = i.readByte().toInt()
                f.counterFired = (sf and 1) != 0
                f.swapped = (sf and 2) != 0
                f.dodgeDir = i.readByte().toInt()
                f.flash = i.readByte().toInt()
                f.dmgDealt = i.readShort() / 10f
                f.hitsTaken = i.readShort().toInt()
                f.perfects = i.readShort().toInt()
                f.thrown = i.readShort().toInt()
                f.landed = i.readShort().toInt()
                f.downCount = i.readByte().toInt()
                f.knockdowns = i.readByte().toInt()
            }
            val n = i.readByte().toInt()
            for (k in 0 until n) {
                val t = i.readByte().toInt()
                val w = i.readByte().toInt()
                val x = i.readShort() / 16f
                sink(t, w, x)
            }
        } catch (e: java.io.IOException) {
            return
        }
    }

    fun writeStrings(vararg s: String): ByteArray {
        val bo = ByteArrayOutputStream()
        val o = DataOutputStream(bo)
        for (x in s) o.writeUTF(x)
        return bo.toByteArray()
    }

    fun readStrings(d: ByteArray, count: Int): List<String> {
        val out = ArrayList<String>()
        try {
            val i = DataInputStream(ByteArrayInputStream(d))
            for (k in 0 until count) out.add(i.readUTF())
        } catch (e: java.io.IOException) {
            while (out.size < count) out.add("")
        }
        return out
    }
}
