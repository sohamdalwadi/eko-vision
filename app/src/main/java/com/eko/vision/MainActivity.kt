package com.eko.vision

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.eko.vision.presentation.camera.CameraScreen
import com.eko.vision.presentation.theme.EkoVisionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EkoVisionTheme {
                CameraScreen()
            }
        }
    }
}
