package com.eventcounter.ui.archive

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eventcounter.di.LocalAppContainer
import com.eventcounter.ui.components.AppIconGlyph
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    onBack: () -> Unit,
    onOpenEvent: (Long) -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: ArchiveViewModel = viewModel(factory = ArchiveViewModel.factory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val formatter = remember {
        DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale.forLanguageTag("ru"))
    }
    val zone = remember { ZoneId.systemDefault() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Архив") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        when (val ui = state) {
            ArchiveUiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is ArchiveUiState.Ready -> {
                if (ui.items.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Архив пуст. Уберите событие с главного экрана, и здесь останется дата, когда оно произошло.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 12.dp,
                            bottom = 24.dp,
                        ),
                        modifier = Modifier.padding(padding),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(ui.items, key = { it.id }) { item ->
                            ArchiveRowItem(
                                item = item,
                                whenLabel = item.occurredAt.atZone(zone).format(formatter),
                                onOpen = { onOpenEvent(item.id) },
                                onRestore = { viewModel.restore(item.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchiveRowItem(
    item: ArchiveRow,
    whenLabel: String,
    onOpen: () -> Unit,
    onRestore: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppIconGlyph(item.iconKey, modifier = Modifier.size(28.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = whenLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (item.groupName.isNotBlank()) {
                Text(
                    text = item.groupName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onRestore) {
            Icon(Icons.Outlined.Unarchive, contentDescription = "Вернуть из архива")
        }
    }
}
