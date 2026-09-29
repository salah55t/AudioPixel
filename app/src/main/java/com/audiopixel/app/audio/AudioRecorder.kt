package com.audiopixel.app.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * مسجّل صوت يعمل بـ PCM مباشرة (بدل MediaRecorder الذي يضغط بصيغة مضيعة للجودة).
 *
 * المخرج: قائمة ByteArray صغيرة (chunks) تُجمّع لاحقاً في بايت واحد، أو تكتب مباشرة لـ WAV.
 *
 * الإعداد الافتراضي:
 *  - Sample rate: 44100 Hz
 *  - Mono channel
 *  - 16-bit PCM
 */
class AudioRecorder(
    private val sampleRate: Int = DEFAULT_SAMPLE_RATE,
    private val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
    private val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT,
) {

    enum class State { IDLE, RECORDING, STOPPED, ERROR }

    private val _state = MutableStateFlow(State.IDLE)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _amplitude = MutableStateFlow(0)
    /** مستوى الصوت الحالي (0..100) — يفيد لعرض مؤشّر بصري أثناء التسجيل. */
    val amplitude: StateFlow<Int> = _amplitude.asStateFlow()

    private var recorder: AudioRecord? = null
    private var recordThread: Thread? = null
    @Volatile private var isRecording = false

    private val collectedChunks = ArrayList<ByteArray>()
    private val chunkSize: Int by lazy {
        val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (minBuf <= 0) DEFAULT_CHUNK else minBuf * 2
    }

    /** يبدأ التسجيل. إذا كانت هناك تسجيلات سابقة فسيتم مسحها. */
    fun start() {
        if (_state.value == State.RECORDING) return
        try {
            val recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                chunkSize * 4
            )
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                _state.value = State.ERROR
                Log.e(TAG, "AudioRecord لم يُهيّأ — ربما الإذن مرفوض.")
                return
            }
            collectedChunks.clear()
            recorder.startRecording()
            isRecording = true
            this.recorder = recorder
            _state.value = State.RECORDING

            recordThread = Thread {
                val buf = ByteArray(chunkSize)
                while (isRecording) {
                    val read = recorder.read(buf, 0, buf.size)
                    if (read > 0) {
                        val chunk = buf.copyOf(read)
                        synchronized(collectedChunks) { collectedChunks.add(chunk) }
                        _amplitude.value = computeAmplitude(chunk)
                    }
                }
                try { recorder.stop() } catch (_: Exception) {}
                recorder.release()
                this.recorder = null
                _state.value = State.STOPPED
            }.also { it.start() }
        } catch (sec: SecurityException) {
            _state.value = State.ERROR
            Log.e(TAG, "إذن RECORD_AUDIO غير ممنوح", sec)
        } catch (e: Exception) {
            _state.value = State.ERROR
            Log.e(TAG, "خطأ أثناء بدء التسجيل", e)
        }
    }

    /** يوقف التسجيل ويُرجع البيانات الصوتية الخام (PCM 16-bit). */
    fun stop(): ByteArray {
        if (_state.value != State.RECORDING) return ByteArray(0)
        isRecording = false
        recordThread?.join(2000)
        recordThread = null

        val pcm = synchronized(collectedChunks) {
            val total = collectedChunks.sumOf { it.size }
            val out = ByteArray(total)
            var off = 0
            for (c in collectedChunks) {
                System.arraycopy(c, 0, out, off, c.size)
                off += c.size
            }
            collectedChunks.clear()
            out
        }
        _state.value = State.IDLE
        _amplitude.value = 0
        return pcm
    }

    /** يُلغي التسجيل الجاري دون إرجاع أي بيانات. */
    fun cancel() {
        isRecording = false
        try { recordThread?.join(1000) } catch (_: Exception) {}
        recordThread = null
        try { recorder?.stop() } catch (_: Exception) {}
        recorder?.release()
        recorder = null
        synchronized(collectedChunks) { collectedChunks.clear() }
        _state.value = State.IDLE
        _amplitude.value = 0
    }

    /** يحوّل PCM الحالي إلى ملف WAV بصيغة كاملة بترتيب الـ RIFF القياسي. */
    fun pcmToWav(pcm: ByteArray): ByteArray {
        val numChannels = if (channelConfig == AudioFormat.CHANNEL_IN_MONO) 1 else 2
        val bitsPerSample = if (audioFormat == AudioFormat.ENCODING_PCM_16BIT) 16 else 8
        val byteRate = sampleRate * numChannels * bitsPerSample / 8
        val blockAlign = numChannels * bitsPerSample / 8
        val dataSize = pcm.size
        val chunkSize = 36 + dataSize
        val out = java.io.ByteArrayOutputStream(dataSize + 44)
        out.apply {
            write("RIFF".toByteArray(Charsets.US_ASCII))
            write(intToLittleEndian(chunkSize))
            write("WAVE".toByteArray(Charsets.US_ASCII))
            write("fmt ".toByteArray(Charsets.US_ASCII))
            write(intToLittleEndian(16))
            write(shortToLittleEndian(1)) // PCM = 1
            write(shortToLittleEndian(numChannels))
            write(intToLittleEndian(sampleRate))
            write(intToLittleEndian(byteRate))
            write(shortToLittleEndian(blockAlign))
            write(shortToLittleEndian(bitsPerSample))
            write("data".toByteArray(Charsets.US_ASCII))
            write(intToLittleEndian(dataSize))
            write(pcm)
        }
        return out.toByteArray()
    }

    private fun computeAmplitude(chunk: ByteArray): Int {
        if (chunk.size < 2) return 0
        var max = 0
        var i = 0
        while (i + 1 < chunk.size) {
            val lo = chunk[i].toInt() and 0xFF
            val hi = chunk[i + 1].toInt()
            val sample = (hi shl 8) or lo
            val abs = if (sample < 0) -sample else sample
            if (abs > max) max = abs
            i += 2
        }
        return (max.toFloat() / Short.MAX_VALUE * 100f).toInt().coerceIn(0, 100)
    }

    private fun intToLittleEndian(v: Int): ByteArray =
        byteArrayOf((v and 0xFF).toByte(), ((v shr 8) and 0xFF).toByte(),
            ((v shr 16) and 0xFF).toByte(), ((v shr 24) and 0xFF).toByte())

    private fun shortToLittleEndian(v: Int): ByteArray =
        byteArrayOf((v and 0xFF).toByte(), ((v shr 8) and 0xFF).toByte())

    companion object {
        private const val TAG = "AudioRecorder"
        const val DEFAULT_SAMPLE_RATE = 44_100
        const val DEFAULT_CHUNK = 1024
    }
}
