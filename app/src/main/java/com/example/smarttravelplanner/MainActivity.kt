package com.example.smarttravelplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.smarttravelplanner.navigation.AppNavigation
import com.example.smarttravelplanner.ui.theme.SmartTravelPlannerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SmartTravelPlannerTheme {
                AppNavigation()
            }
        }
    }
}