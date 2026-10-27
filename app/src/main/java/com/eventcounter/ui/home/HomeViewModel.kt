package com.eventcounter.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventcounter.di.AppContainer
import com.eventcounter.di.viewModelFactory
import com.eventcounter.domain.CounterValue
import com.eventcounter.domain.EventStatus
import com.eventcounter.domain.FolderSnapshot
import com.eventcounter.domain.Group
import com.eventcounter.domain.StatusCalculator
import com.eventcounter.domain.ViewMode
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

sealed interface BrowserItem {
    val key: String

    data class GroupRow(
        val id: Long,
        val name: String,
        val iconKey: String,
    ) : BrowserItem {
        override val key: String = "g$id"
    }

    data class EventRow(
        val id: Long,
        val title: String,
        val iconKey: String,
        val status: EventStatus,
        val counter: CounterValue,
        val subtitle: String,
    ) : BrowserItem {
        override val key: String = "e$id"
    }
}

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Ready(
        val title: String,
        val isRoot: Boolean,
        val viewMode: ViewMode,
        val items: List<BrowserItem>,
    ) : HomeUiState
}

class HomeViewModel(
    private val repository: com.eventcounter.data.EventRepository,
    private val settingsRepository: com.eventcounter.data.prefs.SettingsRepository,
    private val parentId: Long?,
) : ViewModel() {
    private val zoneId = ZoneId.systemDefault()

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeFolder(parentId),
        parentFlow(),
        settingsRepository.settings,
        ticker(),
    ) { folder, parent, settings, now ->
        HomeUiState.Ready(
            title = parent?.name ?: "Счётчик событий",
            isRoot = parentId == null,
            viewMode = settings.viewMode,
            items = buildItems(folder, now),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    fun toggleViewMode() {
        val current = (uiState.value as? HomeUiState.Ready)?.viewMode ?: return
        viewModelScope.launch {
            settingsRepository.setViewMode(
                if (current == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST,
            )
        }
    }

    private fun parentFlow(): Flow<Group?> {
        val id = parentId ?: return flow {
            emit(null)
            awaitCancellation()
        }
        return repository.observeGroup(id)
    }

    private fun ticker(): Flow<Instant> = flow {
        while (true) {
            emit(Instant.now())
            delay(1_000)
        }
    }

    private fun buildItems(folder: FolderSnapshot, now: Instant): List<BrowserItem> {
        val groups = folder.groups.map { group ->
            BrowserItem.GroupRow(group.id, group.name, group.iconKey)
        }
        val events = folder.events.map { event ->
            BrowserItem.EventRow(
                id = event.id,
                title = event.title,
                iconKey = event.iconKey,
                status = StatusCalculator.status(event, now, zoneId),
                counter = StatusCalculator.counter(event, now, zoneId),
                subtitle = event.shortDescription.ifBlank { event.tag },
            )
        }
        return groups + events
    }

    companion object {
        fun factory(container: AppContainer, parentId: Long?) = viewModelFactory {
            HomeViewModel(container.repository, container.settingsRepository, parentId)
        }
    }
}
