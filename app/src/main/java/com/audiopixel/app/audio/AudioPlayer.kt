package com.audiopixel.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream

/**
 * مشغّل صوت بسيط:
 *  - يشغّل WAV من ملف.
 *  - يشغّل RAW PCM عند توفير sampleRate و channelConfig.
 *  - يكشف الحالة (Playing / Stopped) عبر StateFlow.
 */
class AudioPlayer {

    enum class State { IDLE, PLAYING, COMPLETED }

    private val _state = MutableStateFlow(State.IDLE)
    val state: StateFlow<State> = _state.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var pcmPlayerThread: Thread? = null

    /** يشغّل ملف WAV بصيغة RIFF. */
    fun playWavFile(wavBytes: ByteArray) {
        stop()
        val tmp = File.createTempFile("decoded_", ".wav").apply { deleteOnExit() }
        FileOutputStream(tmp).use { it.write(wavBytes) }
        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(tmp.absolutePath)
                setOnCompletionListener { _state.value = State.COMPLETED }
                prepare()
                start()
            }
            mediaPlayer = mp
            _state.value = State.PLAYING
        } catch (e: Exception) {
            Log.e(TAG, "فشل تشغيل WAV", e)
            _state.value = State.IDLE
        }
    }

    /** يشغّل ملف WAV من مسار محلي. */
    fun playWavFile(path: String) {
        stop()
        try {
            val mp = MediaPlayer().apply {
                setDataSource(path)
                setOnCompletionListener { _state.value = State.COMPLETED }
                prepare()
                start()
            }
            mediaPlayer = mp
            _state.value = State.PLAYING
        } catch (e: Exception) {
            Log.e(TAG, "فشل تشغيل الملف: $path", e)
            _state.value = State.IDLE
        }
    }

    /** يشغّل PCM خام باستخدام AudioTrack. */
    fun playPcm(pcm: ByteArray, sampleRate: Int = 44_100, isMono: Boolean = true) {
        stop()
        val channelConfig = if (isMono) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO
        pcmPlayerThread = Thread {
            try {
                val minBuf = android.media.AudioTrack.getMinBufferSize(
                    sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT)
                val track = android.media.AudioTrack(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build(),
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build(),
                    minBuf.coerceAtLeast(pcm.size),
                    android.media.AudioTrack.MODE_STATIC,
                    AudioManager.AUDIO_SESSION_ID_GENERATE
                )
                track.write(pcm, 0, pcm.size)
                track.play()
                _state.value = State.PLAYING
                // انتظر حتى ينتهي التشغيل تقريباً
                val durationMs = (pcm.size * 1000L) / (sampleRate * (if (isMono) 1 else 2) * 2)
                Thread.sleep(durationMs.coerceAtLeast(50))
                track.stop()
                track.release()
                _state.value = State.COMPLETED
            } catch (e: Exception) {
                Log.e(TAG, "فشل تشغيل PCM", e)
                _state.value = State.IDLE
            }
        }.also { it.start() }
    }

    fun stop() {
        try { mediaPlayer?.let { if (it.isPlaying) it.stop(); it.release() } } catch (_: Exception) {}
        mediaPlayer = null
        pcmPlayerThread?.interrupt()
        try { pcmPlayerThread?.join(500) } catch (_: Exception) {}
        pcmPlayerThread = null
        _state.value = State.IDLE
    }

    companion object { private const val TAG = "AudioPlayer" }
}
