package com.audiopixel.app.codec

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.sqrt

/**
 * المحوّل بين الصوت (بايتات WAV/PCM) والصورة (Bitmap) — تحويل غير فاقد للبيانات (Lossless).
 *
 * الفكرة الأساسية:
 *  - كل بكسل في الصورة يحمل 3 بايت (R, G, B).
 *  - نأخذ بايتات الصوت الخام، ونوزّعها على البكسلات ثلاثياً ثلاثياً.
 *  - أول 16 بايت = ترويسة (header) تحوي: (1) البصمة السحرية 0xAUD1PIXEL، (2) طول البيانات الأصلية.
 *  - عند فك التشفير: نقرأ الترويسة، ثم نستخرج البكسلات حتى نبلغ الطول المعلن،
 *    ونقطع أي بايت زائد.
 *
 * ملاحظة: تنسيق PNG يستخدم ضغط lossless، لذا يمكن استرجاع البكسلات بدقة 100%.
 */
class AudioImageCodec {

    /** بصمة سحرية لتمييز الصور المولدة من قبل هذا التطبيق. */
    private val magic: ByteArray = byteArrayOf(
        0x41, 0x75, 0x64, 0x69, 0x6F, 0x50, 0x69, 0x78 // "AudioPix"
    )

    /**
     * يحوّل مصفوفة بايتات (الصوت الخام) إلى Bitmap بصيغة ARGB_8888.
     * @param audioData بايتات الصوت (يفضّل أن تكون PCM خام أو WAV كامل).
     * @return Bitmap يحمل البيانات بصرياً + معلومات الترويسة.
     */
    fun encode(audioData: ByteArray): Bitmap {
        require(audioData.isNotEmpty()) { "بيانات الصوت فارغة" }

        // نبني الحزمة النهائية = ترويسة + طول (8 بايت) + بيانات الصوت
        val payload = ByteArrayOutputStream().apply {
            write(magic)
            write(longToBytes(audioData.size.toLong()))
            write(audioData)
        }.toByteArray()

        // كل بكسل = 3 بايت، لذا نحتاج ceil(payload.size / 3) بكسل.
        val totalPixels = (payload.size + 2) / 3
        val width = computeSquareWidth(totalPixels)
        val height = ((totalPixels + width - 1) / width).coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        var idx = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (idx >= payload.size) {
                    // بكسل تعبئة شفّاف (للجمال البصري فقط)
                    bitmap.setPixel(x, y, Color.argb(255, 10, 10, 10))
                    continue
                }
                val r = payload[idx].toInt() and 0xFF
                val g = if (idx + 1 < payload.size) payload[idx + 1].toInt() and 0xFF else 0
                val b = if (idx + 2 < payload.size) payload[idx + 2].toInt() and 0xFF else 0
                bitmap.setPixel(x, y, Color.argb(255, r, g, b))
                idx += 3
            }
        }
        return bitmap
    }

    /**
     * يحوّل Bitmap (تم توليده عبر encode) إلى بايتات الصوت الأصلية.
     * @throws IllegalArgumentException إذا كانت الصورة لا تحمل البصمة السحرية.
     */
    fun decode(bitmap: Bitmap): ByteArray {
        val totalPixels = bitmap.width * bitmap.height
        require(totalPixels >= 16) { "الصورة صغيرة جداً لتكون صوتاً مشفّراً" }

        // اقرأ كل البكسلات كـ RGB
        val raw = ByteArray(totalPixels * 3)
        var pos = 0
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val c = bitmap.getPixel(x, y)
                raw[pos++] = Color.red(c).toByte()
                raw[pos++] = Color.green(c).toByte()
                raw[pos++] = Color.blue(c).toByte()
            }
        }

        // تحقق من البصمة السحرية
        for (i in magic.indices) {
            if (raw[i] != magic[i]) {
                throw IllegalArgumentException("الصورة لا تحمل بصمة AudioPixel — على الأرجح ليست صورة مشفّرة بهذا التطبيق.")
            }
        }

        // اقرأ طول البيانات (8 بايت بعد البصمة)
        val dataLen = bytesToLong(raw, magic.size, 8).toInt()
        require(dataLen in 0..(totalPixels * 3 - 16)) { "طول البيانات غير منطقي: $dataLen" }

        // استخرج بايتات الصوت
        val audio = ByteArray(dataLen)
        System.arraycopy(raw, magic.size + 8, audio, 0, dataLen)
        return audio
    }

    /**
     * يحفظ Bitmap في InputStream بتنسيق PNG (lossless).
     */
    fun bitmapToPngStream(bitmap: Bitmap): ByteArray {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        return out.toByteArray()
    }

    /**
     * يحوّل بايتات PNG إلى Bitmap.
     */
    fun pngStreamToBitmap(png: ByteArray): Bitmap {
        val opts = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inMutable = false
        }
        return BitmapFactory.decodeByteArray(png, 0, png.size, opts)
            ?: throw IllegalArgumentException("تعذّر قراءة الصورة كـ PNG")
    }

    /**
     * يحوّل ملف PNG مباشرة إلى بايتات صوت.
     */
    fun decodeFromPng(pngBytes: ByteArray): ByteArray {
        val bmp = pngStreamToBitmap(pngBytes)
        return decode(bmp)
    }

    /**
     * يحوّل بايتات صوت مباشرة إلى PNG جاهز للحفظ.
     */
    fun encodeToPng(audioData: ByteArray): ByteArray {
        val bmp = encode(audioData)
        return bitmapToPngStream(bmp)
    }

    // ---- أدوات مساعدة ----

    /** يحسب عرضاً قريباً من المربع لعدد البكسلات المعطى. */
    private fun computeSquareWidth(totalPixels: Int): Int {
        if (totalPixels <= 0) return 1
        val side = sqrt(totalPixels.toDouble()).toInt().coerceAtLeast(1)
        return side.coerceAtMost(MAX_DIM).coerceAtLeast(1)
    }

    private fun longToBytes(v: Long): ByteArray {
        val b = ByteArray(8)
        for (i in 0 until 8) b[i] = ((v shr (8 * (7 - i))).toByte())
        return b
    }

    private fun bytesToLong(b: ByteArray, offset: Int, length: Int): Long {
        var v = 0L
        for (i in 0 until length) {
            v = (v shl 8) or (b[offset + i].toLong() and 0xFF)
        }
        return v
    }

    companion object {
        /** أقصى بُعد للصورة المولّدة. 2048 × 2048 = ~4.2 مليون بكسل = ~12.6 MB صوت خام. */
        const val MAX_DIM = 2048
    }
}
