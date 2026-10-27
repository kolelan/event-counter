package com.eventcounter.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventcounter.di.AppContainer
import com.eventcounter.di.viewModelFactory
import com.eventcounter.domain.AppSettings
import com.eventcounter.domain.ViewMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: com.eventcounter.data.prefs.SettingsRepository,
) : ViewModel() {
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { stored ->
                if (!_loaded.value) {
                    _settings.value = stored
                    _loaded.value = true
                } else {
                    _settings.update { it.copy(viewMode = stored.viewMode) }
                }
            }
        }
    }

    fun setViewMode(mode: ViewMode) {
        _settings.update { it.copy(viewMode = mode) }
        viewModelScope.launch { settingsRepository.setViewMode(mode) }
    }

    fun setAiApiKey(value: String) {
        _settings.update { it.copy(aiApiKey = value) }
        viewModelScope.launch { settingsRepository.setAiApiKey(value) }
    }

    fun setAiBaseUrl(value: String) {
        _settings.update { it.copy(aiBaseUrl = value) }
        viewModelScope.launch { settingsRepository.setAiBaseUrl(value) }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            SettingsViewModel(container.settingsRepository)
        }
    }
}
