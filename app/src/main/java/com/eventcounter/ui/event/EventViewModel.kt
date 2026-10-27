package com.eventcounter.ui.event

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventcounter.data.EventRepository
import com.eventcounter.data.local.ImageStore
import com.eventcounter.di.AppContainer
import com.eventcounter.di.viewModelFactory
import com.eventcounter.domain.CounterDirection
import com.eventcounter.domain.CounterUnit
import com.eventcounter.domain.Event
import com.eventcounter.domain.EventDetails
import com.eventcounter.domain.Group
import com.eventcounter.domain.IconKeys
import com.eventcounter.domain.StatusCalculator
import com.eventcounter.domain.ThresholdUnit
import com.eventcounter.domain.parseThreshold
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

data class ImageDraft(
    val path: String,
    val persisted: Boolean,
)

data class EventFormState(
    val loading: Boolean = true,
    val notFound: Boolean = false,
    val busy: Boolean = false,
    val importing: Boolean = false,
    val error: String? = null,
    val id: Long = 0,
    val title: String = "",
    val shortDescription: String = "",
    val fullDescription: String = "",
    val tag: String = "",
    val iconKey: String = IconKeys.EVENT,
    val occurredAt: Instant = Instant.EPOCH,
    val isIndefinite: Boolean = false,
    val yellowEnabled: Boolean = true,
    val yellowAmount: String = "60",
    val yellowUnit: ThresholdUnit = ThresholdUnit.DAYS,
    val redEnabled: Boolean = true,
    val redAmount: String = "90",
    val redUnit: ThresholdUnit = ThresholdUnit.DAYS,
    val counterDirection: CounterDirection = CounterDirection.ELAPSED,
    val counterUnit: CounterUnit = CounterUnit.DAYS,
    val groupId: Long? = null,
    val sortOrder: Int = 0,
    val images: List<ImageDraft> = emptyList(),
    val groups: List<Group> = emptyList(),
) {
    fun toPreviewEvent(): Event = Event(
        id = id,
        groupId = groupId,
        title = title.ifBlank { "Новое событие" },
        shortDescription = shortDescription,
        fullDescription = fullDescription,
        tag = tag,
        iconKey = iconKey,
        occurredAt = occurredAt,
        isIndefinite = isIndefinite,
        yellow = if (!isIndefinite && yellowEnabled) parseThreshold(yellowAmount, yellowUnit) else null,
        red = if (!isIndefinite && redEnabled) parseThreshold(redAmount, redUnit) else null,
        counterDirection = counterDirection,
        counterUnit = counterUnit,
        sortOrder = sortOrder,
    )
}

class EventViewModel(
    private val repository: EventRepository,
    private val imageStore: ImageStore,
    private val eventId: Long?,
    private val parentId: Long?,
) : ViewModel() {
    private val zone = ZoneId.systemDefault()
    private val gate = Any()
    private val pendingNew = mutableListOf<String>()
    private val removedPersisted = mutableListOf<String>()
    private var saved = false
    private var cleared = false

    private val _state = MutableStateFlow(EventFormState())
    val state: StateFlow<EventFormState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val groups = repository.getAllGroups()
            if (eventId == null) {
                _state.value = EventFormState(
                    loading = false,
                    groupId = parentId,
                    groups = groups,
                    occurredAt = nowTruncated(),
                )
            } else {
                val details = repository.getEventDetails(eventId)
                _state.value = if (details == null) {
                    EventFormState(loading = false, notFound = true, groups = groups)
                } else {
                    details.toForm(groups)
                }
            }
        }
    }

    fun edit(transform: (EventFormState) -> EventFormState) {
        _state.update { transform(it).copy(error = null) }
    }

    fun setIndefinite(enabled: Boolean) {
        edit { state ->
            state.copy(
                isIndefinite = enabled,
                counterDirection = if (enabled && state.counterDirection == CounterDirection.UNTIL_RED) {
                    CounterDirection.ELAPSED
                } else {
                    state.counterDirection
                },
            )
        }
    }

    fun setRedEnabled(enabled: Boolean) {
        edit { state ->
            state.copy(
                redEnabled = enabled,
                counterDirection = if (!enabled && state.counterDirection == CounterDirection.UNTIL_RED) {
                    CounterDirection.ELAPSED
                } else {
                    state.counterDirection
                },
            )
        }
    }

    fun applyTag(tag: String) {
        edit { it.copy(tag = tag, iconKey = IconKeys.suggest(tag)) }
    }

    fun addImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(importing = true, error = null) }
            val paths = withContext(NonCancellable) {
                val imported = imageStore.import(uris)
                trackNew(imported)
                imported
            }
            if (!isActive) return@launch
            _state.update { state ->
                state.copy(
                    importing = false,
                    images = state.images + paths.map { ImageDraft(it, persisted = false) },
                    error = if (paths.isEmpty()) "Не удалось добавить изображения" else null,
                )
            }
        }
    }

    fun removeImage(path: String) {
        val image = _state.value.images.firstOrNull { it.path == path } ?: return
        edit { state -> state.copy(images = state.images.filterNot { it.path == path }) }
        if (image.persisted) {
            synchronized(gate) { removedPersisted += path }
        } else {
            synchronized(gate) { pendingNew.remove(path) }
            imageStore.delete(path)
        }
    }

    fun save(onSaved: () -> Unit) {
        val snapshot = _state.value
        if (snapshot.loading || snapshot.busy || snapshot.notFound || snapshot.importing) return
        val error = validate(snapshot)
        if (error != null) {
            _state.update { it.copy(error = error) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            try {
                val id = repository.saveEvent(snapshot.toEvent(), snapshot.images.map { it.path })
                val removed = synchronized(gate) { removedPersisted.toList() }
                removed.forEach(imageStore::delete)
                markSaved()
                _state.update { it.copy(busy = false, id = id) }
                onSaved()
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                _state.update { it.copy(busy = false, error = "Не удалось сохранить") }
            }
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val id = _state.value.id
        if (id == 0L || _state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            repository.deleteEvent(id)
            onDeleted()
        }
    }

    override fun onCleared() {
        val orphaned = mutableListOf<String>()
        synchronized(gate) {
            cleared = true
            if (!saved) {
                orphaned += pendingNew
                pendingNew.clear()
            }
        }
        orphaned.forEach(imageStore::delete)
    }

    private fun trackNew(paths: List<String>) {
        val orphaned = mutableListOf<String>()
        synchronized(gate) {
            if (cleared && !saved) orphaned += paths else pendingNew += paths
        }
        orphaned.forEach(imageStore::delete)
    }

    private fun markSaved() {
        synchronized(gate) { saved = true }
    }

    private fun validate(state: EventFormState): String? {
        if (state.title.trim().isEmpty()) return "Введите название"
        val yellow = if (!state.isIndefinite && state.yellowEnabled) {
            parseThreshold(state.yellowAmount, state.yellowUnit) ?: return "Укажите жёлтый порог"
        } else {
            null
        }
        val red = if (!state.isIndefinite && state.redEnabled) {
            parseThreshold(state.redAmount, state.redUnit) ?: return "Укажите красный порог"
        } else {
            null
        }
        if (yellow != null && red != null && !StatusCalculator.isYellowBeforeRed(state.occurredAt, yellow, red, zone)) {
            return "Красный порог должен наступать позже жёлтого"
        }
        return null
    }

    private fun EventFormState.toEvent(): Event = Event(
        id = id,
        groupId = groupId,
        title = title.trim(),
        shortDescription = shortDescription.trim(),
        fullDescription = fullDescription.trim(),
        tag = tag.trim(),
        iconKey = iconKey.ifBlank { IconKeys.EVENT },
        occurredAt = occurredAt,
        isIndefinite = isIndefinite,
        yellow = if (!isIndefinite && yellowEnabled) parseThreshold(yellowAmount, yellowUnit) else null,
        red = if (!isIndefinite && redEnabled) parseThreshold(redAmount, redUnit) else null,
        counterDirection = counterDirection,
        counterUnit = counterUnit,
        sortOrder = sortOrder,
    )

    private fun EventDetails.toForm(groups: List<Group>): EventFormState {
        return EventFormState(
            loading = false,
            id = event.id,
            title = event.title,
            shortDescription = event.shortDescription,
            fullDescription = event.fullDescription,
            tag = event.tag,
            iconKey = event.iconKey,
            occurredAt = event.occurredAt,
            isIndefinite = event.isIndefinite,
            yellowEnabled = event.yellow != null,
            yellowAmount = event.yellow?.amount?.toString() ?: "60",
            yellowUnit = event.yellow?.unit ?: ThresholdUnit.DAYS,
            redEnabled = event.red != null,
            redAmount = event.red?.amount?.toString() ?: "90",
            redUnit = event.red?.unit ?: ThresholdUnit.DAYS,
            counterDirection = event.counterDirection,
            counterUnit = event.counterUnit,
            groupId = event.groupId,
            sortOrder = event.sortOrder,
            images = images.map { ImageDraft(it.path, persisted = true) },
            groups = groups,
        )
    }

    private fun nowTruncated(): Instant {
        return Instant.now().atZone(zone).withSecond(0).withNano(0).toInstant()
    }

    companion object {
        fun factory(container: AppContainer, eventId: Long?, parentId: Long?) = viewModelFactory {
            EventViewModel(container.repository, container.imageStore, eventId, parentId)
        }
    }
}
