package com.eventcounter.ui.group

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eventcounter.di.LocalAppContainer
import com.eventcounter.domain.GroupTree
import com.eventcounter.ui.components.IconPicker
import com.eventcounter.ui.components.ParentGroupField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupEditorScreen(
    groupId: Long?,
    parentId: Long?,
    onDone: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: GroupViewModel = viewModel(
        factory = GroupViewModel.factory(container, groupId, parentId),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    val excluded = if (state.id == 0L) {
        emptySet()
    } else {
        GroupTree.descendantIds(state.groups, state.id) + state.id
    }
    val choices = GroupTree.choices(state.groups, excluded)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (groupId == null) "Новая группа" else "Группа") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.save(onDone) },
                        enabled = !state.loading && !state.busy && !state.notFound,
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
                    Text("Группа не найдена")
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = { value -> viewModel.edit { it.copy(name = value) } },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Название") },
                        singleLine = true,
                    )
                    Text("Иконка", style = MaterialTheme.typography.titleSmall)
                    IconPicker(
                        selectedKey = state.iconKey,
                        onSelect = { key -> viewModel.edit { it.copy(iconKey = key) } },
                    )
                    ParentGroupField(
                        selectedId = state.parentGroupId,
                        choices = choices,
                        onSelect = { id -> viewModel.edit { it.copy(parentGroupId = id) } },
                    )
                    state.error?.let { message ->
                        Text(message, color = MaterialTheme.colorScheme.error)
                    }
                    if (state.id != 0L) {
                        TextButton(onClick = { confirmDelete = true }) {
                            Text("Удалить группу")
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Удалить группу?") },
            text = { Text("Вложенные группы и события переместятся на уровень выше.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(onDone) }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Отмена") }
            },
        )
    }
}
