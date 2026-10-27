package com.eventcounter.data

import androidx.room.withTransaction
import com.eventcounter.data.local.EventDatabase
import com.eventcounter.data.local.EventImageEntity
import com.eventcounter.data.local.ImageStore
import com.eventcounter.data.local.toDomain
import com.eventcounter.data.local.toEntity
import com.eventcounter.domain.Event
import com.eventcounter.domain.EventDetails
import com.eventcounter.domain.FolderSnapshot
import com.eventcounter.domain.Group
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class EventRepository(
    private val database: EventDatabase,
    private val imageStore: ImageStore,
) {
    private val dao = database.eventDao()

    fun observeFolder(parentGroupId: Long?): Flow<FolderSnapshot> {
        val groups = if (parentGroupId == null) {
            dao.observeRootGroups()
        } else {
            dao.observeChildGroups(parentGroupId)
        }
        val events = if (parentGroupId == null) {
            dao.observeRootEvents()
        } else {
            dao.observeChildEvents(parentGroupId)
        }
        return combine(groups, events) { groupRows, eventRows ->
            FolderSnapshot(
                groups = groupRows.map { it.toDomain() },
                events = eventRows.map { it.toDomain() },
            )
        }
    }

    fun observeGroup(id: Long): Flow<Group?> = dao.observeGroup(id).map { it?.toDomain() }

    suspend fun getAllGroups(): List<Group> = dao.getAllGroups().map { it.toDomain() }

    suspend fun getEventDetails(id: Long): EventDetails? {
        val event = dao.getEvent(id)?.toDomain() ?: return null
        val images = dao.getImages(id).map { it.toDomain() }
        return EventDetails(event, images)
    }

    suspend fun saveEvent(event: Event, imagePaths: List<String>): Long = database.withTransaction {
        val existing = if (event.id == 0L) null else dao.getEvent(event.id)
        val sortOrder = if (existing == null || existing.groupId != event.groupId) {
            nextEventSort(event.groupId)
        } else {
            existing.sortOrder
        }
        val entity = event.copy(sortOrder = sortOrder).toEntity()
        val id = if (existing == null) {
            dao.insertEvent(entity)
        } else {
            dao.updateEvent(entity)
            entity.id
        }
        dao.deleteImagesFor(id)
        if (imagePaths.isNotEmpty()) {
            dao.insertImages(
                imagePaths.mapIndexed { index, path ->
                    EventImageEntity(eventId = id, path = path, sortOrder = index)
                },
            )
        }
        id
    }

    suspend fun deleteEvent(id: Long) {
        val images = dao.getImages(id)
        dao.deleteEventById(id)
        images.forEach { imageStore.delete(it.path) }
    }

    suspend fun saveGroup(group: Group): Long = database.withTransaction {
        val existing = if (group.id == 0L) null else dao.getGroup(group.id)
        val sortOrder = if (existing == null || existing.parentGroupId != group.parentGroupId) {
            nextGroupSort(group.parentGroupId)
        } else {
            existing.sortOrder
        }
        val entity = group.copy(sortOrder = sortOrder).toEntity()
        if (existing == null) {
            dao.insertGroup(entity)
        } else {
            dao.updateGroup(entity)
            entity.id
        }
    }

    suspend fun deleteGroup(id: Long) {
        database.withTransaction {
            val group = dao.getGroup(id) ?: return@withTransaction
            dao.reparentGroups(id, group.parentGroupId)
            dao.reparentEvents(id, group.parentGroupId)
            dao.deleteGroupById(id)
        }
    }

    private suspend fun nextEventSort(groupId: Long?): Int {
        return if (groupId == null) dao.nextRootEventSort() else dao.nextChildEventSort(groupId)
    }

    private suspend fun nextGroupSort(parentId: Long?): Int {
        return if (parentId == null) dao.nextRootGroupSort() else dao.nextChildGroupSort(parentId)
    }
}
