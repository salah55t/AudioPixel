package com.audiopixel.app.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.audiopixel.app.AudioPixelApp
import com.audiopixel.app.audio.AudioPlayer
import java.io.File

/**
 * شاشة فك التشفير — يستورد المستخدم صورة PNG (من المعرض أو الملفات)،
 * ثم يحاول التطبيق قراءتها كـ PCM مشفّر، فإذا نجح فعّل زر "تشغيل".
 *
 * الأمور المهمة:
 *  - يتحقق أن الصورة تحمل البصمة السحرية (Magic).
 *  - يحفظ نسخة PCM في ملف WAV مؤقت قبل التشغيل.
 *  - يعرض رسالة خطأ واضحة إذا كانت الصورة ليست AudioPixel-encoded.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecodeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as AudioPixelApp
    val player = remember { AudioPlayer() }

    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var decodedPath by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) {
            Toast.makeText(context, "لم يتم اختيار صورة", Toast.LENGTH_SHORT).show()
        } else {
            pickedUri = uri
            decodedPath = null
            errorMsg = null
            info = "جارٍ تحليل الصورة…"
            // نفك التشفير في background بسيط عبر Thread
            Thread {
                try {
                    val pcm = app.repository.decodeFromImageUri(uri)
                    if (pcm == null || pcm.isEmpty()) {
                        errorMsg = "تعذّر قراءة الصورة — تأكد أنها صُنعت بواسطة AudioPixel."
                        info = null
                        return@Thread
                    }
                    // نكتب WAV مؤقتاً للتشغيل
                    val tmpWav = File(context.cacheDir, "decoded_${System.currentTimeMillis()}.wav")
                    val wavBytes = com.audiopixel.app.audio.AudioRecorder().pcmToWav(pcm)
                    tmpWav.outputStream().use { it.write(wavBytes) }
                    decodedPath = tmpWav.absolutePath
                    info = "تم فك التشفير بنجاح!\nحجم البيانات: ${pcm.size} بايت\nيمكنك ضغط تشغيل للاستماع."
                } catch (e: IllegalArgumentException) {
                    errorMsg = e.message ?: "الصورة غير صالحة لفك التشفير"
                    info = null
                } catch (e: Exception) {
                    errorMsg = "خطأ غير متوقع: ${e.message}"
                    info = null
                }
            }.start()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("فك تشفير صورة", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // أيقونة توضيحية
            Icon(
                Icons.Filled.GraphicEq,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "اختر صورة PNG تم توليدها بواسطة AudioPixel لإعادة تشغيلها كصوت",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))

            // زر اختيار صورة
            Button(
                onClick = { pickImageLauncher.launch("image/*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("اختر صورة", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(24.dp))

            // معاينة الصورة المختارة
            pickedUri?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = "الصورة المختارة",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                )
                Spacer(Modifier.height(16.dp))
            }

            // معلومات الحالة
            info?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        it,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            errorMsg?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        it,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // زر التشغيل — يظهر فقط عند نجاح فك التشفير
            decodedPath?.let {
                Button(
                    onClick = { player.playWavFile(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("▶ تشغيل الصوت المفكوك", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
