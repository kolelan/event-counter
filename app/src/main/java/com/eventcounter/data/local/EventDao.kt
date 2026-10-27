package com.eventcounter.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM groups WHERE parentGroupId IS NULL ORDER BY sortOrder ASC, name ASC")
    fun observeRootGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE parentGroupId = :parentId ORDER BY sortOrder ASC, name ASC")
    fun observeChildGroups(parentId: Long): Flow<List<GroupEntity>>

    @Query(
        "SELECT * FROM events WHERE groupId IS NULL AND archivedAtEpochMillis IS NULL " +
            "ORDER BY sortOrder ASC, title ASC",
    )
    fun observeRootEvents(): Flow<List<EventEntity>>

    @Query(
        "SELECT * FROM events WHERE groupId = :groupId AND archivedAtEpochMillis IS NULL " +
            "ORDER BY sortOrder ASC, title ASC",
    )
    fun observeChildEvents(groupId: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE archivedAtEpochMillis IS NOT NULL ORDER BY occurredAtEpochMillis DESC")
    fun observeArchivedEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM groups ORDER BY sortOrder ASC, name ASC")
    fun observeAllGroups(): Flow<List<GroupEntity>>

    @Query("UPDATE events SET archivedAtEpochMillis = :archivedAt WHERE id = :id")
    suspend fun setArchivedAt(id: Long, archivedAt: Long?)

    @Query("SELECT * FROM groups WHERE id = :id")
    fun observeGroup(id: Long): Flow<GroupEntity?>

    @Query("SELECT * FROM groups ORDER BY sortOrder ASC, name ASC")
    suspend fun getAllGroups(): List<GroupEntity>

    @Query("SELECT * FROM groups WHERE id = :id")
    suspend fun getGroup(id: Long): GroupEntity?

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getEvent(id: Long): EventEntity?

    @Query("SELECT * FROM events WHERE id IN (:ids)")
    suspend fun getEventsByIds(ids: List<Long>): List<EventEntity>

    @Query(
        "SELECT * FROM events WHERE archivedAtEpochMillis IS NULL " +
            "ORDER BY title COLLATE NOCASE ASC, occurredAtEpochMillis DESC",
    )
    suspend fun getActiveEvents(): List<EventEntity>

    @Query("SELECT * FROM event_images WHERE eventId = :eventId ORDER BY sortOrder ASC")
    suspend fun getImages(eventId: Long): List<EventImageEntity>

    @Insert
    suspend fun insertGroup(group: GroupEntity): Long

    @Update
    suspend fun updateGroup(group: GroupEntity)

    @Insert
    suspend fun insertEvent(event: EventEntity): Long

    @Update
    suspend fun updateEvent(event: EventEntity)

    @Insert
    suspend fun insertImages(images: List<EventImageEntity>)

    @Query("DELETE FROM event_images WHERE eventId = :eventId")
    suspend fun deleteImagesFor(eventId: Long)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteEventById(id: Long)

    @Query("DELETE FROM groups WHERE id = :id")
    suspend fun deleteGroupById(id: Long)

    @Query("UPDATE groups SET parentGroupId = :newParentId WHERE parentGroupId = :oldParentId")
    suspend fun reparentGroups(oldParentId: Long, newParentId: Long?)

    @Query("UPDATE events SET groupId = :newParentId WHERE groupId = :oldParentId")
    suspend fun reparentEvents(oldParentId: Long, newParentId: Long?)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM events WHERE groupId IS NULL")
    suspend fun nextRootEventSort(): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM events WHERE groupId = :groupId")
    suspend fun nextChildEventSort(groupId: Long): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM groups WHERE parentGroupId IS NULL")
    suspend fun nextRootGroupSort(): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM groups WHERE parentGroupId = :parentId")
    suspend fun nextChildGroupSort(parentId: Long): Int
}
