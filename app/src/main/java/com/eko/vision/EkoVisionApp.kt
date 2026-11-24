package com.eko.vision

import android.app.Application
import android.util.Log
import com.cactus.CactusContextInitializer

class EkoVisionApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Cactus native engine context
        try {
            CactusContextInitializer.initialize(this)
            Log.i("EkoVisionApp", "Cactus runtime initialized successfully.")
        } catch (e: Throwable) {
            Log.e("EkoVisionApp", "Failed to initialize Cactus runtime", e)
        }
    }
}
