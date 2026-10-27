package com.eventcounter.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [GroupEntity::class, EventEntity::class, EventImageEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class EventDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao

    companion object {
        @Volatile
        private var instance: EventDatabase? = null

        fun get(context: Context): EventDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    EventDatabase::class.java,
                    "event-counter.db",
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
        }
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE events ADD COLUMN archivedAtEpochMillis INTEGER")
    }
}
