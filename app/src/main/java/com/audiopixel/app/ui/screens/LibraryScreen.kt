package com.audiopixel.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.audiopixel.app.AudioPixelApp
import com.audiopixel.app.audio.AudioPlayer
import com.audiopixel.app.data.Recording
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * شاشة المكتبة — تعرض كل التسجيلات السابقة في قائمة عمودية قابلة للتمرير.
 * كل عنصر يعرض: صورة مصغّرة (الصورة المشفّرة)، الاسم، التاريخ، المدة، الحجم،
 * وأزرار: تشغيل الصوت، مشاركة الصورة، حذف.
 */
@Composable
fun LibraryScreen(
    onBack: () -> Unit,
    onGoToDecode: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as AudioPixelApp
    val player = remember { AudioPlayer() }
    val scope = rememberCoroutineScope()

    var recordings by remember { mutableStateOf(app.repository.list()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مكتبة التسجيلات", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = onGoToDecode) {
                        Icon(Icons.Filled.Image, contentDescription = "فك تشفير صورة")
                    }
                }
            )
        }
    ) { padding ->
        if (recordings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Image,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "لا توجد تسجيلات بعد",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "ابدأ بتسجيل صوتك من شاشة التسجيل الرئيسية",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recordings, key = { it.id }) { rec ->
                    RecordingCard(
                        recording = rec,
                        onPlay = { player.playWavFile(rec.audioPath) },
                        onShare = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/png"
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    File(rec.imagePath)
                                )
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "مشاركة الصورة عبر…"))
                        },
                        onDelete = {
                            app.repository.delete(rec)
                            recordings = app.repository.list()
                            Toast.makeText(context, "حُذفت التسجيلة", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordingCard(
    recording: Recording,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    val dateStr = remember(recording.createdAt) {
        SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
            .format(Date(recording.createdAt))
    }
    val sizeStr = remember(recording.sizeBytes) {
        val kb = recording.sizeBytes / 1024.0
        if (kb > 1024) String.format("%.2f MB", kb / 1024) else String.format("%.0f KB", kb)
    }
    val durStr = remember(recording.durationMs) {
        val s = recording.durationMs / 1000
        val m = s / 60
        val sec = s % 60
        String.format("%02d:%02d", m, sec)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // صورة مصغّرة للصورة المشفّرة
            AsyncImage(
                model = File(recording.imagePath),
                contentDescription = "الصورة المشفّرة",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
            )

            Spacer(Modifier.width(12.dp))

            // معلومات نصية
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    recording.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "$dateStr  •  $durStr  •  $sizeStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            // أزرار
            Row {
                IconButton(onClick = onPlay) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "تشغيل",
                        tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Filled.Share, contentDescription = "مشاركة",
                        tint = MaterialTheme.colorScheme.secondary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "حذف",
                        tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
