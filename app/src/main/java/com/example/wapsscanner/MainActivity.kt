package com.example.wapsscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wapsscanner.ui.theme.WAPsScannerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WAPsScannerTheme {
                // Use AppViewModel for all scan and UI logic
                val wifiSurveyViewModel: AppViewModel = viewModel(factory = AppViewModelFactory(application))
                WifiAnalyzerScreen(wifiSurveyViewModel)
            }
        }

        // Hide system bars for immersive UI on Android 11+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.decorView.post {
                window.insetsController?.let { controller ->
                    controller.hide(android.view.WindowInsets.Type.statusBars())
                    controller.hide(android.view.WindowInsets.Type.navigationBars())
                }
            }
        }
    }
}
