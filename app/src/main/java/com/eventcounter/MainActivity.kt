package com.eventcounter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.eventcounter.di.LocalAppContainer
import com.eventcounter.ui.navigation.AppNavHost
import com.eventcounter.ui.theme.EventCounterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as EventCounterApp).container
        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                EventCounterTheme {
                    AppNavHost()
                }
            }
        }
    }
}
