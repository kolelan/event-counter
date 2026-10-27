package com.eventcounter.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.eventcounter.data.EventRepository
import com.eventcounter.data.local.EventDatabase
import com.eventcounter.data.local.ImageStore
import com.eventcounter.data.prefs.SettingsRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val imageStore: ImageStore = ImageStore(appContext)
    val repository: EventRepository = EventRepository(
        database = EventDatabase.get(appContext),
        imageStore = imageStore,
    )
    val settingsRepository: SettingsRepository = SettingsRepository(appContext)
}

fun <VM : ViewModel> viewModelFactory(create: () -> VM): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            @Suppress("UNCHECKED_CAST")
            return create() as T
        }
    }
}
