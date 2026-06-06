package com.carldong.fifa.worldcup2026

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.carldong.fifa.worldcup2026.theme.WC2026Theme
import com.carldong.fifa.worldcup2026.ui.AppScaffold

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WC2026Theme {
                AppScaffold()
            }
        }
    }
}

