package com.eventcounter.ui.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eventcounter.EventCounterApp
import com.eventcounter.di.LocalAppContainer
import com.eventcounter.di.viewModelFactory
import com.eventcounter.domain.Event
import com.eventcounter.domain.WIDGET_EMPTY_TITLE
import com.eventcounter.domain.eventsForSlot
import com.eventcounter.domain.uniqueSlotIds
import com.eventcounter.ui.theme.EventCounterTheme
import com.eventcounter.widget.WidgetRefresher
import com.eventcounter.widget.WidgetStore
import com.eventcounter.widget.configureTitle
import com.eventcounter.widget.slotCountOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.compose.runtime.CompositionLocalProvider

class WidgetConfigureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        setResult(RESULT_CANCELED, result)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val provider = AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId)?.provider?.className
        val slotCount = slotCountOf(provider)
        val title = configureTitle(provider, slotCount)
        val container = (application as EventCounterApp).container
        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                EventCounterTheme {
                    WidgetConfigureScreen(
                        widgetId = widgetId,
                        slotCount = slotCount,
                        title = title,
                        onDone = {
                            setResult(RESULT_OK, result)
                            finish()
                        },
                    )
                }
            }
        }
    }
}

private data class ConfigureState(
    val loading: Boolean = true,
    val events: List<Event> = emptyList(),
    val selected: List<Long?> = emptyList(),
    val pickingSlot: Int? = null,
)

private class WidgetConfigureViewModel(
    private val repository: com.eventcounter.data.EventRepository,
    private val store: WidgetStore,
    private val refresher: WidgetRefresher,
    private val widgetId: Int,
    slotCount: Int,
) : ViewModel() {
    private val _state = MutableStateFlow(ConfigureState(selected = List(slotCount) { null }))
    val state: StateFlow<ConfigureState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val events = repository.getActiveEvents()
            val stored = store.read(widgetId).eventIds
            val activeIds = events.map { it.id }.toSet()
            val selected = List(_state.value.selected.size) { index ->
                stored.getOrNull(index)?.takeIf { it in activeIds }
            }
            _state.update { it.copy(loading = false, events = events, selected = selected) }
        }
    }

    fun openSlot(index: Int) {
        _state.update { it.copy(pickingSlot = index) }
    }

    fun closeSlot() {
        _state.update { it.copy(pickingSlot = null) }
    }

    fun choose(index: Int, eventId: Long?) {
        _state.update { state ->
            val next = state.selected.toMutableList()
            if (index in next.indices) next[index] = eventId
            state.copy(selected = uniqueSlotIds(next), pickingSlot = null)
        }
    }

    fun save(onDone: () -> Unit) {
        val snapshot = _state.value
        store.write(widgetId, snapshot.selected.size, uniqueSlotIds(snapshot.selected))
        viewModelScope.launch {
            runCatching { refresher.refresh() }
            onDone()
        }
    }

    companion object {
        fun factory(
            container: com.eventcounter.di.AppContainer,
            widgetId: Int,
            slotCount: Int,
        ) = viewModelFactory {
            WidgetConfigureViewModel(
                repository = container.repository,
                store = container.widgetStore,
                refresher = container.widgetRefresher,
                widgetId = widgetId,
                slotCount = slotCount,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetConfigureScreen(
    widgetId: Int,
    slotCount: Int,
    title: String,
    onDone: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: WidgetConfigureViewModel = viewModel(
        factory = WidgetConfigureViewModel.factory(container, widgetId, slotCount),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                actions = {
                    TextButton(
                        onClick = { viewModel.save(onDone) },
                        enabled = !state.loading,
                    ) {
                        Text("Готово")
                    }
                },
            )
        },
    ) { padding ->
        if (state.loading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text(
                        text = "Для каждого места выберите активное событие. Пустое место потом можно заполнить с виджета.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(state.selected.size) { index ->
                    val selectedId = state.selected[index]
                    val label = state.events.firstOrNull { it.id == selectedId }?.title ?: WIDGET_EMPTY_TITLE
                    TextButton(
                        onClick = { viewModel.openSlot(index) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "${index + 1}. $label",
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }

    val picking = state.pickingSlot
    if (picking != null) {
        val choices = eventsForSlot(state.events, state.selected, picking)
        AlertDialog(
            onDismissRequest = viewModel::closeSlot,
            title = { Text("Событие ${picking + 1}") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    item {
                        ChoiceRow(
                            title = "Очистить",
                            selected = state.selected.getOrNull(picking) == null,
                            onClick = { viewModel.choose(picking, null) },
                        )
                    }
                    items(choices, key = { it.id }) { event ->
                        ChoiceRow(
                            title = event.title,
                            selected = state.selected.getOrNull(picking) == event.id,
                            onClick = { viewModel.choose(picking, event.id) },
                        )
                    }
                    if (choices.isEmpty()) {
                        item {
                            Text(
                                text = "Нет других активных событий.",
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::closeSlot) { Text("Закрыть") }
            },
        )
    }
}

@Composable
private fun ChoiceRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
