package com.premierpicks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.premierpicks.ui.PremierPicksApp
import com.premierpicks.ui.PremierPicksTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PremierPicksTheme {
                PremierPicksApp()
            }
        }
    }
}
