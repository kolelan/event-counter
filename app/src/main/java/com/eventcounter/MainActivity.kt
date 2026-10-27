package com.eventcounter

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eventcounter.di.LocalAppContainer
import com.eventcounter.ui.navigation.AppNavHost
import com.eventcounter.ui.theme.EventCounterTheme
import com.eventcounter.widget.WidgetIntents

class MainActivity : ComponentActivity() {
    private var openEventId by mutableStateOf<Long?>(null)
    private var openEventToken by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        acceptOpenEvent(intent)
        enableEdgeToEdge()
        val container = (application as EventCounterApp).container
        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                EventCounterTheme {
                    AppNavHost(
                        openEventId = openEventId,
                        openEventToken = openEventToken,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        acceptOpenEvent(intent)
    }

    private fun acceptOpenEvent(intent: Intent?) {
        val eventId = WidgetIntents.eventId(intent) ?: return
        openEventId = eventId
        openEventToken += 1
    }
}
