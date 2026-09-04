package com.example.nutrition

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.nutrition.ui.MainScreen
import com.example.nutrition.ui.theme.NutritionTheme

/**
 * 应用唯一 Activity
 *
 * 采用单 Activity + Compose 多 Screen 架构
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NutritionTheme {
                MainScreen()
            }
        }
    }
}
