package com.audiopixel.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.audiopixel.app.ui.screens.DecodeScreen
import com.audiopixel.app.ui.screens.LibraryScreen
import com.audiopixel.app.ui.screens.RecordScreen
import com.audiopixel.app.ui.theme.AudioPixelTheme

/**
 * نقطة الدخول الوحيدة للتطبيق — تستضيف NavHost الذي يربط الشاشات الثلاث:
 *  - "record": شاشة التسجيل والتشفير.
 *  - "library": مكتبة التسجيلات المحفوظة.
 *  - "decode": شاش فك تشفير صورة مستوردة.
 */
class MainActivity : ComponentActivity() {

    private val requiredPermissions: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            // إن رفض المستخدم إذن المايك فسيظهر ذلك في شاشة التسجيل كحالة Error.
            (application as AudioPixelApp)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ensurePermissions()

        setContent {
            AudioPixelTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = "record"
                    ) {
                        composable("record") {
                            RecordScreen(
                                onGoToLibrary = { navController.navigate("library") },
                                onGoToDecode = { navController.navigate("decode") },
                            )
                        }
                        composable("library") {
                            LibraryScreen(
                                onBack = { navController.popBackStack() },
                                onGoToDecode = { navController.navigate("decode") }
                            )
                        }
                        composable("decode") {
                            DecodeScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun ensurePermissions() {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }
}
