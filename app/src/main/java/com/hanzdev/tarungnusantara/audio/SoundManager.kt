package com.hanzdev.tarungnusantara.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.speech.tts.TextToSpeech
import com.hanzdev.tarungnusantara.data.Save
import java.util.Locale

object Sound {
    private val names = listOf(
        "jab", "hook", "upper", "special", "block", "perfect", "whoosh", "dodge", "ko", "bell",
        "cheer", "dizzy", "down", "click", "coin", "unlock", "tick"
    )
    private var pool: SoundPool? = null
    private val ids = HashMap<String, Int>()
    private var loaded = 0
    private var appCtx: Context? = null
    private var music: MediaPlayer? = null
    private var currentTrack = ""

    @Volatile var ready = false
        private set
    @Volatile var failed = false
        private set

    fun init(ctx: Context) {
        if (pool != null) return
        appCtx = ctx.applicationContext
        try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val sp = SoundPool.Builder().setMaxStreams(8).setAudioAttributes(attrs).build()
            sp.setOnLoadCompleteListener { _, _, status ->
                if (status != 0) failed = true
                loaded++
                if (loaded >= names.size) ready = true
            }
            for (n in names) {
                val afd = ctx.applicationContext.assets.openFd("audio/x_$n.ogg")
                ids[n] = sp.load(afd, 1)
                afd.close()
            }
            pool = sp
        } catch (e: Exception) {
            failed = true
            ready = true
        }
    }

    private fun sfxVolume(): Float = if (Save.sfxOn) (Save.sfxVol / 100f).let { it * it } else 0f

    fun play(name: String, vol: Float = 1f, rate: Float = 1f) {
        val v = sfxVolume() * vol
        if (v <= 0f) return
        val id = ids[name] ?: return
        pool?.play(id, v, v, 1, 0, rate.coerceIn(0.5f, 2f))
    }

    fun playMusic(file: String, loop: Boolean = true) {
        val ctx = appCtx ?: return
        if (currentTrack == file && music != null) {
            applyMusicVolume()
            return
        }
        stopMusic()
        try {
            val afd = ctx.assets.openFd("audio/$file")
            val mp = MediaPlayer()
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            mp.isLooping = loop
            mp.prepare()
            music = mp
            currentTrack = file
            applyMusicVolume()
            if (Save.musicOn) mp.start()
        } catch (e: Exception) {
            music = null
            currentTrack = ""
        }
    }

    fun applyMusicVolume() {
        val mp = music ?: return
        val v = if (Save.musicOn) (Save.musicVol / 100f).let { it * it } * 0.8f else 0f
        try {
            mp.setVolume(v, v)
            if (Save.musicOn && !mp.isPlaying) mp.start()
            if (!Save.musicOn && mp.isPlaying) mp.pause()
        } catch (e: IllegalStateException) {
            music = null
        }
    }

    fun pauseMusic() {
        try { music?.let { if (it.isPlaying) it.pause() } } catch (e: IllegalStateException) { music = null }
    }

    fun resumeMusic() {
        if (!Save.musicOn) return
        try { music?.let { if (!it.isPlaying) it.start() } } catch (e: IllegalStateException) { music = null }
    }

    fun stopMusic() {
        try { music?.stop() } catch (e: IllegalStateException) { }
        music?.release()
        music = null
        currentTrack = ""
    }
}

class Commentator(ctx: Context) {
    private var tts: TextToSpeech? = null
    private var ok = false
    private var lastAt = 0L

    init {
        tts = TextToSpeech(ctx.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val r = tts?.setLanguage(Locale("id", "ID")) ?: TextToSpeech.LANG_NOT_SUPPORTED
                ok = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
                if (ok) tts?.setSpeechRate(1.1f)
            }
        }
    }

    fun say(text: String) {
        if (!ok || !Save.commentaryVoice || !Save.sfxOn) return
        val now = System.currentTimeMillis()
        if (now - lastAt < 2200) return
        lastAt = now
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "komentar")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
