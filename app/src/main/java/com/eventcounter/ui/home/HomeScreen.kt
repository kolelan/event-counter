package com.eventcounter.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eventcounter.di.LocalAppContainer
import com.eventcounter.domain.ViewMode
import com.eventcounter.ui.components.AppIconGlyph
import com.eventcounter.ui.components.CounterLabel
import com.eventcounter.ui.components.eventContainerColor
import com.eventcounter.ui.components.statusColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    parentId: Long?,
    onBack: () -> Unit,
    onOpenGroup: (Long) -> Unit,
    onOpenEvent: (Long) -> Unit,
    onCreateEvent: () -> Unit,
    onCreateGroup: () -> Unit,
    onOpenArchive: () -> Unit,
    onSettings: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(container, parentId))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }
    val ready = state as? HomeUiState.Ready
    val title = ready?.title ?: if (parentId == null) "Счётчик событий" else ""
    val isRoot = ready?.isRoot ?: (parentId == null)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (!isRoot) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Назад")
                        }
                    }
                },
                actions = {
                    if (ready != null) {
                        val grid = ready.viewMode == ViewMode.GRID
                        IconButton(onClick = viewModel::toggleViewMode) {
                            Icon(
                                imageVector = if (grid) Icons.AutoMirrored.Outlined.ViewList else Icons.Outlined.GridView,
                                contentDescription = if (grid) "Список" else "Плитка",
                            )
                        }
                    }
                    IconButton(onClick = onOpenArchive) {
                        Icon(Icons.Outlined.Archive, contentDescription = "Архив")
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Настройки")
                    }
                },
            )
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Outlined.Add, contentDescription = "Добавить")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Событие") },
                        leadingIcon = { Icon(Icons.Outlined.Event, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onCreateEvent()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Группа") },
                        leadingIcon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onCreateGroup()
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        when {
            ready == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            ready.items.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Пока пусто. Добавьте событие или группу.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            ready.viewMode == ViewMode.GRID -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(148.dp),
                    contentPadding = contentPadding(innerPadding),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    gridItems(ready.items, key = { it.key }) { item ->
                        BrowserTile(item, onOpenGroup, onOpenEvent, viewModel::archive)
                    }
                }
            }

            else -> {
                LazyColumn(
                    contentPadding = contentPadding(innerPadding),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(ready.items, key = { it.key }) { item ->
                        BrowserRow(item, onOpenGroup, onOpenEvent, viewModel::archive)
                    }
                }
            }
        }
    }
}

@Composable
private fun contentPadding(innerPadding: PaddingValues): PaddingValues {
    return PaddingValues(
        start = 16.dp,
        end = 16.dp,
        top = innerPadding.calculateTopPadding() + 12.dp,
        bottom = innerPadding.calculateBottomPadding() + 96.dp,
    )
}

@Composable
private fun BrowserRow(
    item: BrowserItem,
    onOpenGroup: (Long) -> Unit,
    onOpenEvent: (Long) -> Unit,
    onArchive: (Long) -> Unit,
) {
    when (item) {
        is BrowserItem.GroupRow -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onOpenGroup(item.id) }
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AppIconGlyph(item.iconKey, modifier = Modifier.size(28.dp))
                Text(
                    text = item.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null)
            }
        }

        is BrowserItem.EventRow -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(eventContainerColor(item.status))
                    .clickable { onOpenEvent(item.id) }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AppIconGlyph(
                    iconKey = item.iconKey,
                    tint = statusColor(item.status),
                    modifier = Modifier.size(28.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (item.subtitle.isNotBlank()) {
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                CounterLabel(counter = item.counter, status = item.status)
                IconButton(onClick = { onArchive(item.id) }) {
                    Icon(Icons.Outlined.Archive, contentDescription = "В архив")
                }
            }
        }
    }
}

@Composable
private fun BrowserTile(
    item: BrowserItem,
    onOpenGroup: (Long) -> Unit,
    onOpenEvent: (Long) -> Unit,
    onArchive: (Long) -> Unit,
) {
    when (item) {
        is BrowserItem.GroupRow -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 132.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onOpenGroup(item.id) }
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppIconGlyph(item.iconKey, modifier = Modifier.size(32.dp))
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("Группа", style = MaterialTheme.typography.bodySmall)
            }
        }

        is BrowserItem.EventRow -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 132.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(eventContainerColor(item.status))
                    .clickable { onOpenEvent(item.id) },
            ) {
                Column(
                    modifier = Modifier.padding(12.dp).padding(end = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AppIconGlyph(
                        iconKey = item.iconKey,
                        tint = statusColor(item.status),
                        modifier = Modifier.size(32.dp),
                    )
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    CounterLabel(counter = item.counter, status = item.status)
                }
                IconButton(
                    onClick = { onArchive(item.id) },
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Icon(Icons.Outlined.Archive, contentDescription = "В архив")
                }
            }
        }
    }
}
