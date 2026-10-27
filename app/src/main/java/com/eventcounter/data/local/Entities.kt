package com.eventcounter.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "groups",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentGroupId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("parentGroupId")],
)
data class GroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val parentGroupId: Long? = null,
    val name: String,
    val iconKey: String,
    val sortOrder: Int,
)

@Entity(
    tableName = "events",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("groupId")],
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long?,
    val title: String,
    val shortDescription: String,
    val fullDescription: String,
    val tag: String,
    val iconKey: String,
    val occurredAtEpochMillis: Long,
    val isIndefinite: Boolean,
    val yellowAmount: Long?,
    val yellowUnit: String?,
    val redAmount: Long?,
    val redUnit: String?,
    val counterDirection: String,
    val counterUnit: String,
    val sortOrder: Int,
)

@Entity(
    tableName = "event_images",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("eventId")],
)
data class EventImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: Long,
    val path: String,
    val sortOrder: Int,
)
