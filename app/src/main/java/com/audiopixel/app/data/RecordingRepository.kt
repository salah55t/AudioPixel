package com.audiopixel.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.audiopixel.app.codec.AudioImageCodec
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * نموذج تسجيلة واحدة — مرتبط بزوج (صورة + WAV).
 */
data class Recording(
    val id: Long,
    val name: String,
    val imagePath: String,
    val audioPath: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val createdAt: Long,
)

/**
 * مستودع التسجيلات — يحفظ الصور المولّدة + ملفات WAV في تخزين داخلي للتطبيق (Context.filesDir).
 * يوفّر عمليات CRUD بسيطة على نموذج Recording.
 */
class RecordingRepository(private val context: Context) {

    private val codec = AudioImageCodec()
    private val imagesDir = File(context.filesDir, "images").apply { if (!exists()) mkdirs() }
    private val audioDir = File(context.filesDir, "audio").apply { if (!exists()) mkdirs() }

    private val dbFile = File(context.filesDir, "recordings.tsv")

    init { if (!dbFile.exists()) dbFile.createNewFile() }

    /**
     * يحفظ بايتات PCM كـ WAV + يولّد منها صورة PNG ثم يضيف سجل للتسجيلة.
     * @param pcm بايتات PCM 16-bit
     * @param sampleRate معدل العيّنات المستخدم في التسجيل
     * @return كائن Recording الجديد.
     */
    fun saveRecording(pcm: ByteArray, sampleRate: Int = 44_100): Recording {
        val timestamp = System.currentTimeMillis()
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(timestamp))
        val baseName = "rec_$dateStr"

        // 1) حفظ WAV — نبني ملف WAV كامل من PCM الخام
        val wavBytes = buildWav(pcm, sampleRate, mono = true)
        val audioFile = File(audioDir, "$baseName.wav")
        FileOutputStream(audioFile).use { it.write(wavBytes) }

        // 2) توليد الصورة (encode PCM الأصلي وليس WAV حتى لا تُحفظ الترويسة مرتين)
        val pngBytes = codec.encodeToPng(pcm)
        val imageFile = File(imagesDir, "$baseName.png")
        FileOutputStream(imageFile).use { it.write(pngBytes) }

        // 3) حساب المدة بالمللي ثانية: PCM 16-bit mono => bytes / (sampleRate * 2) * 1000
        val durationMs = (pcm.size.toLong() * 1000L) / (sampleRate.toLong() * 2L)

        val recording = Recording(
            id = timestamp,
            name = baseName,
            imagePath = imageFile.absolutePath,
            audioPath = audioFile.absolutePath,
            durationMs = durationMs,
            sizeBytes = audioFile.length() + imageFile.length(),
            createdAt = timestamp
        )

        appendToDb(recording)
        Log.i(TAG, "حفظ تسجيلة جديدة: ${recording.name} (pcm=${pcm.size}B, dur=${durationMs}ms)")
        return recording
    }

    fun list(): List<Recording> {
        if (!dbFile.exists()) return emptyList()
        val results = ArrayList<Recording>()
        dbFile.useLines { lines ->
            for (line in lines) {
                val parts = line.split("\t")
                if (parts.size != 7) continue
                try {
                    results += Recording(
                        id = parts[0].toLong(),
                        name = parts[1],
                        imagePath = parts[2],
                        audioPath = parts[3],
                        durationMs = parts[4].toLong(),
                        sizeBytes = parts[5].toLong(),
                        createdAt = parts[6].toLong()
                    )
                } catch (_: NumberFormatException) {}
            }
        }
        return results.sortedByDescending { it.createdAt }
    }

    fun delete(recording: Recording): Boolean {
        val img = File(recording.imagePath)
        val wav = File(recording.audioPath)
        img.delete()
        wav.delete()
        val all = list().filterNot { it.id == recording.id }
        rewriteDb(all)
        return true
    }

    /**
     * يفك تشفير صورة (من Uri معطاة) إلى PCM، يحفظها كـ WAV، ثم يشغّلها.
     * @return PCM المفكوك أو null عند الفشل.
     */
    fun decodeFromImageUri(uri: Uri): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return null
            val pngBytes = inputStream.use { it.readBytes() }
            codec.decodeFromPng(pngBytes)
        } catch (e: Exception) {
            Log.e(TAG, "فشل فك تشفير الصورة", e)
            null
        }
    }

    private fun appendToDb(r: Recording) {
        dbFile.appendText(
            "${r.id}\t${r.name}\t${r.imagePath}\t${r.audioPath}\t${r.durationMs}\t${r.sizeBytes}\t${r.createdAt}\n"
        )
    }

    private fun rewriteDb(items: List<Recording>) {
        dbFile.printWriter().use { w ->
            items.forEach {
                w.println("${it.id}\t${it.name}\t${it.imagePath}\t${it.audioPath}\t${it.durationMs}\t${it.sizeBytes}\t${it.createdAt}")
            }
        }
    }

    /**
     * يبني ملف WAV كامل من PCM.
     */
    private fun buildWav(pcm: ByteArray, sampleRate: Int, mono: Boolean): ByteArray {
        val numChannels = if (mono) 1 else 2
        val bitsPerSample = 16
        val byteRate = sampleRate * numChannels * bitsPerSample / 8
        val blockAlign = numChannels * bitsPerSample / 8
        val dataSize = pcm.size
        val chunkSize = 36 + dataSize
        val out = java.io.ByteArrayOutputStream(dataSize + 44)
        out.apply {
            write("RIFF".toByteArray(Charsets.US_ASCII))
            write(intLE(chunkSize))
            write("WAVE".toByteArray(Charsets.US_ASCII))
            write("fmt ".toByteArray(Charsets.US_ASCII))
            write(intLE(16))
            write(shortLE(1))
            write(shortLE(numChannels))
            write(intLE(sampleRate))
            write(intLE(byteRate))
            write(shortLE(blockAlign))
            write(shortLE(bitsPerSample))
            write("data".toByteArray(Charsets.US_ASCII))
            write(intLE(dataSize))
            write(pcm)
        }
        return out.toByteArray()
    }

    private fun intLE(v: Int): ByteArray =
        byteArrayOf((v and 0xFF).toByte(), ((v shr 8) and 0xFF).toByte(),
            ((v shr 16) and 0xFF).toByte(), ((v shr 24) and 0xFF).toByte())

    private fun shortLE(v: Int): ByteArray =
        byteArrayOf((v and 0xFF).toByte(), ((v shr 8) and 0xFF).toByte())

    companion object { private const val TAG = "RecordingRepository" }
}
