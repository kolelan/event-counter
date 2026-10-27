package com.eventcounter.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventcounter.di.AppContainer
import com.eventcounter.di.viewModelFactory
import com.eventcounter.domain.Event
import com.eventcounter.domain.Group
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

data class ArchiveRow(
    val id: Long,
    val title: String,
    val iconKey: String,
    val occurredAt: Instant,
    val groupName: String,
)

sealed interface ArchiveUiState {
    data object Loading : ArchiveUiState

    data class Ready(val items: List<ArchiveRow>) : ArchiveUiState
}

class ArchiveViewModel(
    private val repository: com.eventcounter.data.EventRepository,
) : ViewModel() {
    val uiState: StateFlow<ArchiveUiState> = combine(
        repository.observeArchived(),
        repository.observeAllGroups(),
    ) { events, groups ->
        ArchiveUiState.Ready(events.toRows(groups))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ArchiveUiState.Loading)

    fun restore(eventId: Long) {
        viewModelScope.launch {
            repository.setArchived(eventId, archived = false)
        }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            ArchiveViewModel(container.repository)
        }
    }
}

private fun List<Event>.toRows(groups: List<Group>): List<ArchiveRow> {
    val names = groups.associate { it.id to it.name }
    return sortedWith(compareBy<Event> { it.title.lowercase() }.thenByDescending { it.occurredAt })
        .map { event ->
            ArchiveRow(
                id = event.id,
                title = event.title,
                iconKey = event.iconKey,
                occurredAt = event.occurredAt,
                groupName = event.groupId?.let { names[it] }.orEmpty(),
            )
        }
}
