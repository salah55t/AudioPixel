package com.audiopixel.app

import android.app.Application
import com.audiopixel.app.data.RecordingRepository

/**
 * نقطة الدخول للتطبيق — تُهيّئ مستودع التسجيلات المشترك على مستوى التطبيق.
 */
class AudioPixelApp : Application() {
    lateinit var repository: RecordingRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        repository = RecordingRepository(this)
    }

    companion object {
        lateinit var instance: AudioPixelApp
            private set
    }
}
