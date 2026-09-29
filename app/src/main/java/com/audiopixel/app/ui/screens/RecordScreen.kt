package com.audiopixel.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audiopixel.app.AudioPixelApp
import com.audiopixel.app.audio.AudioPlayer
import com.audiopixel.app.audio.AudioRecorder
import kotlinx.coroutines.launch

/**
 * الشاشة الرئيسية للتسجيل والتشفير.
 *
 * تدفّق المستخدم:
 *  1) يضغط زر "تسجيل" فيبدأ AudioRecorder بجمع PCM.
 *  2) يظهر مؤشّر النبض البصري أثناء التسجيل.
 *  3) يضغط "إيقاف" فيُحفظ التسجيل كـ WAV + صورة PNG في مستودع التسجيلات.
 *  4) تظهر معاينة الصورة المولّدة وأزرار للتشغيل أو فتح المكتبة.
 */
@Composable
fun RecordScreen(
    onGoToLibrary: () -> Unit,
    onGoToDecode: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as AudioPixelApp
    val recorder = remember { AudioRecorder() }
    val player = remember { AudioPlayer() }
    val scope = rememberCoroutineScope()

    val recState by recorder.state.collectAsState()
    val amp by recorder.amplitude.collectAsState()

    var lastImagePath by remember { mutableStateOf<String?>(null) }
    var lastAudioPath by remember { mutableStateOf<String?>(null) }
    var savedName by remember { mutableStateOf<String?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }

    // عرض توست محلي
    LaunchedEffect(toast) {
        toast?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            toast = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // الشعار + العنوان
        Text(
            text = "AudioPixel",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = "حوّل صوتك إلى صورة مشفّرة",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(32.dp))

        // المؤشّر البصري — دائرة نابضة أثناء التسجيل + موجات صوتية
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            // حلقات النبض
            val transition = rememberInfiniteTransition(label = "pulse")
            val scale by transition.animateFloat(
                initialValue = 0.85f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    tween(800, easing = LinearEasing),
                    RepeatMode.Reverse
                ),
                label = "scale"
            )
            // نلتقط اللون هنا (خارج Canvas) لأن DrawScope ليس @Composable
            val primaryColor = MaterialTheme.colorScheme.primary
            val pulseColor = primaryColor.copy(alpha = 0.18f)
            val barColor = primaryColor
            if (recState == AudioRecorder.State.RECORDING) {
                Canvas(modifier = Modifier.fillMaxSize(scale)) {
                    drawCircle(
                        color = pulseColor,
                        radius = size.minDimension / 2f
                    )
                }
            }
            // موجات الموجة الصوتية
            Canvas(modifier = Modifier.size(200.dp, 80.dp)) {
                val bars = 24
                val w = size.width / bars
                val baseColor = barColor
                for (i in 0 until bars) {
                    val h = if (recState == AudioRecorder.State.RECORDING) {
                        // ارتفاع يعتمد على السعة الحالية + عامل عشوائي طفيف لكل عمود
                        val ampNorm = amp.coerceIn(0, 100) / 100f
                        val wave = (0.15f + ampNorm * 0.85f)
                        val seed = (i % 7) * 0.07f
                        (wave * (1f - seed) + seed) * size.height
                    } else 6f
                    drawRect(
                        color = baseColor.copy(alpha = 0.9f),
                        topLeft = Offset(i * w + 2f, (size.height - h) / 2),
                        size = androidx.compose.ui.geometry.Size(w - 4f, h)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // نص الحالة
        val statusText = when (recState) {
            AudioRecorder.State.RECORDING -> "● يسجّل الآن…"
            AudioRecorder.State.STOPPED -> "تم الإيقاف"
            AudioRecorder.State.ERROR -> "تعذّر التسجيل — تحقّق من إذن الميكروفون"
            AudioRecorder.State.IDLE -> savedName?.let { "حفظ باسم: $it" } ?: "جاهز للتسجيل"
        }
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = when (recState) {
                AudioRecorder.State.RECORDING -> MaterialTheme.colorScheme.tertiary
                AudioRecorder.State.ERROR -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(32.dp))

        // زر التسجيل/الإيقاف
        Button(
            onClick = {
                scope.launch {
                    if (recState == AudioRecorder.State.RECORDING) {
                        // إيقاف وحفظ
                        val pcm = recorder.stop()
                        if (pcm.isEmpty()) {
                            toast = "التسجيل فارغ"
                            return@launch
                        }
                        val r = app.repository.saveRecording(pcm)
                        lastImagePath = r.imagePath
                        lastAudioPath = r.audioPath
                        savedName = r.name
                        toast = "تم حفظ الصورة والصوت: ${r.name}"
                    } else {
                        // بدء تسجيل جديد
                        savedName = null
                        recorder.start()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (recState == AudioRecorder.State.RECORDING)
                    MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary,
                contentColor = if (recState == AudioRecorder.State.RECORDING)
                    Color.White
                else MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                imageVector = if (recState == AudioRecorder.State.RECORDING) Icons.Filled.Stop else Icons.Filled.Mic,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = if (recState == AudioRecorder.State.RECORDING) "إيقاف وحفظ" else "بدء التسجيل",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
            )
        }

        Spacer(Modifier.height(16.dp))

        // أزرار تنقّل سريعة
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onGoToLibrary,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("المكتبة", fontSize = 14.sp)
            }
            OutlinedButton(
                onClick = onGoToDecode,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("فك تشفير", fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(24.dp))

        // تشغيل آخر تسجيلة محفوظة
        lastAudioPath?.let { path ->
            ElevatedButton(
                onClick = { player.playWavFile(path) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("▶ تشغيل آخر تسجيلة", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
