package com.eko.vision

import android.app.Application
import android.util.Log
import com.cactus.CactusContextInitializer
import com.cactus.services.CactusTelemetry

class EkoVisionApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 1. Initialize Cactus native engine context
        try {
            CactusContextInitializer.initialize(this)
            // Local-first privacy: telemetry disabled by default
            CactusTelemetry.isTelemetryEnabled = false
            Log.i("EkoVisionApp", "Cactus runtime initialized successfully.")
        } catch (e: Throwable) {
            Log.e("EkoVisionApp", "Failed to initialize Cactus runtime", e)
        }
    }
}
