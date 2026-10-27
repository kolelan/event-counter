package com.eventcounter.ui.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.eventcounter.data.iconify.IconifyGlyph
import com.eventcounter.data.iconify.IconifyRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun IconifySearch(
    repository: IconifyRepository,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var hits by remember { mutableStateOf<List<IconifyGlyph>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(query) {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            hits = emptyList()
            loading = false
            message = null
            return@LaunchedEffect
        }
        delay(400)
        loading = true
        message = null
        val result = runCatching { repository.search(trimmed) }
        loading = false
        result.fold(
            onSuccess = { found ->
                hits = found
                message = if (found.isEmpty()) "Ничего не найдено" else null
            },
            onFailure = {
                hits = emptyList()
                message = "Не удалось загрузить значки"
            },
        )
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Поиск значка") },
            placeholder = { Text("car, water, oil") },
            singleLine = true,
            supportingText = message?.let { text -> { Text(text) } },
        )
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }
        if (hits.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(hits, key = { it.key }) { glyph ->
                    val bitmap = remember(glyph.svg) { glyph.svg.toIconBitmap(96) }
                    val selected = glyph.key == selectedKey
                    Surface(
                        onClick = {
                            scope.launch {
                                val stored = runCatching { repository.store(glyph) }.isSuccess
                                if (stored) onSelect(glyph.key)
                            }
                        },
                        shape = CircleShape,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    ) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = glyph.key,
                                modifier = Modifier.padding(12.dp).size(24.dp),
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                            )
                        } else {
                            Text(
                                "…",
                                modifier = Modifier.padding(12.dp).size(24.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
