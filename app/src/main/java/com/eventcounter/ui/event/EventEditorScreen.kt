package com.eventcounter.ui.event

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.eventcounter.di.LocalAppContainer
import com.eventcounter.domain.CounterDirection
import com.eventcounter.domain.CounterUnit
import com.eventcounter.domain.GroupTree
import com.eventcounter.domain.StatusCalculator
import com.eventcounter.domain.ThresholdUnit
import com.eventcounter.ui.components.AppIconGlyph
import com.eventcounter.ui.components.CounterLabel
import com.eventcounter.ui.components.IconPicker
import com.eventcounter.ui.components.ParentGroupField
import com.eventcounter.ui.components.eventContainerColor
import com.eventcounter.ui.components.label
import com.eventcounter.ui.components.shortLabel
import com.eventcounter.ui.components.statusColor
import kotlinx.coroutines.delay
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val tagPresets = listOf("здоровье", "гигиена", "авто", "вода", "дом")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditorScreen(
    eventId: Long?,
    parentId: Long?,
    onDone: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: EventViewModel = viewModel(
        factory = EventViewModel.factory(container, eventId, parentId),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(8),
    ) { uris -> viewModel.addImages(uris) }
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (eventId == null) "Новое событие" else "Событие") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.save(onDone) },
                        enabled = !state.loading && !state.busy && !state.importing && !state.notFound,
                    ) {
                        Text("Сохранить")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            state.notFound -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("Событие не найдено")
                }
            }

            else -> {
                EventForm(
                    state = state,
                    modifier = Modifier.padding(padding),
                    onEdit = viewModel::edit,
                    onIndefinite = viewModel::setIndefinite,
                    onRedEnabled = viewModel::setRedEnabled,
                    onApplyTag = viewModel::applyTag,
                    onPickImages = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onRemoveImage = viewModel::removeImage,
                    onAskDelete = { confirmDelete = true },
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Удалить событие?") },
            text = { Text("Изображения события тоже будут удалены.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(onDone) }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Отмена") }
            },
        )
    }
}

@Composable
private fun EventForm(
    state: EventFormState,
    modifier: Modifier = Modifier,
    onEdit: ((EventFormState) -> EventFormState) -> Unit,
    onIndefinite: (Boolean) -> Unit,
    onRedEnabled: (Boolean) -> Unit,
    onApplyTag: (String) -> Unit,
    onPickImages: () -> Unit,
    onRemoveImage: (String) -> Unit,
    onAskDelete: () -> Unit,
) {
    val zone = remember { ZoneId.systemDefault() }
    val now by produceState(initialValue = Instant.now()) {
        while (true) {
            value = Instant.now()
            delay(1_000)
        }
    }
    val preview = state.toPreviewEvent()
    val status = StatusCalculator.status(preview, now, zone)
    val counter = StatusCalculator.counter(preview, now, zone)
    val choices = GroupTree.choices(state.groups)
    val timerEnabled = !state.isIndefinite && state.redEnabled
    val context = LocalContext.current
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("ru"))
    }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val zoned = state.occurredAt.atZone(zone)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = eventContainerColor(status)) {
                AppIconGlyph(
                    iconKey = state.iconKey,
                    tint = statusColor(status),
                    modifier = Modifier.padding(12.dp).size(32.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(preview.title, style = MaterialTheme.typography.titleLarge)
                CounterLabel(counter = counter, status = status)
                Text(status.label(), color = statusColor(status), style = MaterialTheme.typography.bodySmall)
            }
        }

        OutlinedTextField(
            value = state.title,
            onValueChange = { value -> onEdit { it.copy(title = value) } },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Название") },
            singleLine = true,
        )
        OutlinedTextField(
            value = state.shortDescription,
            onValueChange = { value -> onEdit { it.copy(shortDescription = value) } },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Краткое описание") },
            singleLine = true,
        )
        OutlinedTextField(
            value = state.fullDescription,
            onValueChange = { value -> onEdit { it.copy(fullDescription = value) } },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Полное описание") },
            minLines = 3,
        )
        OutlinedTextField(
            value = state.tag,
            onValueChange = { value -> onEdit { it.copy(tag = value) } },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Тег") },
            singleLine = true,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tagPresets) { tag ->
                FilterChip(
                    selected = state.tag.equals(tag, ignoreCase = true),
                    onClick = { onApplyTag(tag) },
                    label = { Text(tag) },
                )
            }
        }
        Text("Иконка", style = MaterialTheme.typography.titleSmall)
        IconPicker(
            selectedKey = state.iconKey,
            onSelect = { key -> onEdit { it.copy(iconKey = key) } },
        )
        Text("Момент события", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                val current = state.occurredAt.atZone(zone)
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        onEdit {
                            val updated = current.withYear(year).withMonth(month + 1).withDayOfMonth(day)
                            it.copy(occurredAt = updated.toInstant())
                        }
                    },
                    current.year,
                    current.monthValue - 1,
                    current.dayOfMonth,
                ).show()
            }) { Text(zoned.format(dateFormatter)) }
            OutlinedButton(onClick = {
                val current = state.occurredAt.atZone(zone)
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        onEdit {
                            val updated = current.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
                            it.copy(occurredAt = updated.toInstant())
                        }
                    },
                    current.hour,
                    current.minute,
                    true,
                ).show()
            }) { Text(zoned.format(timeFormatter)) }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = state.isIndefinite, onCheckedChange = onIndefinite)
            Text("Без срока")
        }
        if (state.isIndefinite) {
            Text(
                "Пороги не применяются, цвет статуса не меняется.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                "Жёлтый — пора готовиться, красный — срок вышел.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ThresholdEditor(
                title = "Жёлтый через",
                enabled = state.yellowEnabled,
                amount = state.yellowAmount,
                unit = state.yellowUnit,
                onEnabled = { enabled -> onEdit { it.copy(yellowEnabled = enabled) } },
                onAmount = { amount -> onEdit { it.copy(yellowAmount = amount) } },
                onUnit = { unit -> onEdit { it.copy(yellowUnit = unit) } },
            )
            ThresholdEditor(
                title = "Красный через",
                enabled = state.redEnabled,
                amount = state.redAmount,
                unit = state.redUnit,
                onEnabled = onRedEnabled,
                onAmount = { amount -> onEdit { it.copy(redAmount = amount) } },
                onUnit = { unit -> onEdit { it.copy(redUnit = unit) } },
            )
        }

        Text("Счётчик", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.counterDirection == CounterDirection.ELAPSED,
                onClick = { onEdit { it.copy(counterDirection = CounterDirection.ELAPSED) } },
                label = { Text("Прошло") },
                leadingIcon = { Icon(Icons.Outlined.Timer, contentDescription = null) },
            )
            FilterChip(
                selected = state.counterDirection == CounterDirection.UNTIL_RED,
                onClick = {
                    if (timerEnabled) onEdit { it.copy(counterDirection = CounterDirection.UNTIL_RED) }
                },
                enabled = timerEnabled,
                label = { Text("До красного") },
                leadingIcon = { Icon(Icons.Outlined.HourglassBottom, contentDescription = null) },
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(CounterUnit.entries) { unit ->
                FilterChip(
                    selected = state.counterUnit == unit,
                    onClick = { onEdit { it.copy(counterUnit = unit) } },
                    label = { Text(unit.shortLabel()) },
                )
            }
        }

        ParentGroupField(
            selectedId = state.groupId,
            choices = choices,
            onSelect = { groupId -> onEdit { it.copy(groupId = groupId) } },
        )

        Text("Изображения", style = MaterialTheme.typography.titleSmall)
        if (state.images.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.images, key = { it.path }) { image ->
                    Box {
                        AsyncImage(
                            model = File(image.path),
                            contentDescription = null,
                            modifier = Modifier.size(88.dp).clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        IconButton(
                            onClick = { onRemoveImage(image.path) },
                            modifier = Modifier.align(Alignment.TopEnd),
                        ) {
                            Icon(Icons.Outlined.Close, contentDescription = "Убрать изображение")
                        }
                    }
                }
            }
        }
        OutlinedButton(onClick = onPickImages, enabled = !state.importing) {
            Text(if (state.importing) "Добавление…" else "Добавить изображения")
        }

        state.error?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
        }

        if (state.id != 0L) {
            TextButton(onClick = onAskDelete) { Text("Удалить событие") }
        }
    }
}

@Composable
private fun ThresholdEditor(
    title: String,
    enabled: Boolean,
    amount: String,
    unit: ThresholdUnit,
    onEnabled: (Boolean) -> Unit,
    onAmount: (String) -> Unit,
    onUnit: (ThresholdUnit) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = enabled, onCheckedChange = onEnabled)
        Text(title, modifier = Modifier.weight(1f))
    }
    if (enabled) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = amount,
                onValueChange = { value ->
                    if (value.length <= 4 && value.all(Char::isDigit)) onAmount(value)
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            Box {
                OutlinedButton(onClick = { menu = true }) { Text(unit.shortLabel()) }
                androidx.compose.material3.DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    ThresholdUnit.entries.forEach { option ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(option.shortLabel()) },
                            onClick = {
                                onUnit(option)
                                menu = false
                            },
                        )
                    }
                }
            }
        }
    }
}
