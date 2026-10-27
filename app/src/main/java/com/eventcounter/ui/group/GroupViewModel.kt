package com.eventcounter.ui.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventcounter.data.EventRepository
import com.eventcounter.di.AppContainer
import com.eventcounter.di.viewModelFactory
import com.eventcounter.domain.Group
import com.eventcounter.domain.GroupTree
import com.eventcounter.domain.IconKeys
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupFormState(
    val loading: Boolean = true,
    val notFound: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null,
    val id: Long = 0,
    val name: String = "",
    val iconKey: String = IconKeys.FOLDER,
    val parentGroupId: Long? = null,
    val groups: List<Group> = emptyList(),
)

class GroupViewModel(
    private val repository: EventRepository,
    private val groupId: Long?,
    private val parentId: Long?,
) : ViewModel() {
    private val _state = MutableStateFlow(GroupFormState())
    val state: StateFlow<GroupFormState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val groups = repository.getAllGroups()
            if (groupId == null) {
                _state.value = GroupFormState(
                    loading = false,
                    parentGroupId = parentId,
                    groups = groups,
                )
            } else {
                val group = groups.firstOrNull { it.id == groupId }
                _state.value = if (group == null) {
                    GroupFormState(loading = false, notFound = true, groups = groups)
                } else {
                    GroupFormState(
                        loading = false,
                        id = group.id,
                        name = group.name,
                        iconKey = group.iconKey,
                        parentGroupId = group.parentGroupId,
                        groups = groups,
                    )
                }
            }
        }
    }

    fun edit(transform: (GroupFormState) -> GroupFormState) {
        _state.update { transform(it).copy(error = null) }
    }

    fun save(onSaved: () -> Unit) {
        val snapshot = _state.value
        if (snapshot.loading || snapshot.busy || snapshot.notFound) return
        val name = snapshot.name.trim()
        if (name.isEmpty()) {
            _state.update { it.copy(error = "Введите название") }
            return
        }
        val excluded = if (snapshot.id == 0L) {
            emptySet()
        } else {
            GroupTree.descendantIds(snapshot.groups, snapshot.id) + snapshot.id
        }
        if (snapshot.parentGroupId != null && snapshot.parentGroupId in excluded) {
            _state.update { it.copy(error = "Нельзя вложить группу саму в себя") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            try {
                repository.saveGroup(
                    Group(
                        id = snapshot.id,
                        parentGroupId = snapshot.parentGroupId,
                        name = name,
                        iconKey = snapshot.iconKey.ifBlank { IconKeys.FOLDER },
                    ),
                )
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
            repository.deleteGroup(id)
            onDeleted()
        }
    }

    companion object {
        fun factory(container: AppContainer, groupId: Long?, parentId: Long?) = viewModelFactory {
            GroupViewModel(container.repository, groupId, parentId)
        }
    }
}
