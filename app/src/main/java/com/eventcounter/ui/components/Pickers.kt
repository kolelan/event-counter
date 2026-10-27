package com.eventcounter.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eventcounter.di.LocalAppContainer
import com.eventcounter.domain.GroupChoice
import com.eventcounter.ui.icons.AppIcons
import com.eventcounter.ui.icons.IconifySearch

@Composable
fun IconPicker(
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconify = LocalAppContainer.current.iconify
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        IconifySearch(repository = iconify, selectedKey = selectedKey, onSelect = onSelect)
        Text(
            "На устройстве",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(AppIcons.all, key = { it.key }) { icon ->
                val selected = icon.key == selectedKey
                Surface(
                    onClick = { onSelect(icon.key) },
                    shape = CircleShape,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ) {
                    Icon(
                        imageVector = icon.image,
                        contentDescription = icon.label,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun ParentGroupField(
    selectedId: Long?,
    choices: List<GroupChoice>,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    val selected = choices.firstOrNull { it.id == selectedId }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Группа", style = MaterialTheme.typography.titleSmall)
        OutlinedButton(
            onClick = { open = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(selected?.name ?: "Корень")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("Корень") },
                onClick = {
                    onSelect(null)
                    open = false
                },
            )
            choices.forEach { choice ->
                DropdownMenuItem(
                    text = {
                        Row {
                            Spacer(Modifier.width((choice.depth * 16).dp))
                            Text(choice.name)
                        }
                    },
                    onClick = {
                        onSelect(choice.id)
                        open = false
                    },
                )
            }
        }
    }
}
