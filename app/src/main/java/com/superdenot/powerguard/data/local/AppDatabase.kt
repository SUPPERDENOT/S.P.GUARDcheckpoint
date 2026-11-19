package com.superdenot.powerguard.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.superdenot.powerguard.data.local.dao.BatteryDao
import com.superdenot.powerguard.data.local.entity.BatteryHealthEntity
import com.superdenot.powerguard.data.local.entity.BatterySampleEntity
import com.superdenot.powerguard.data.local.entity.ChargingSessionEntity
import com.superdenot.powerguard.data.local.entity.DailyRollupEntity

@Database(
    entities = [
        ChargingSessionEntity::class,
        BatterySampleEntity::class,
        BatteryHealthEntity::class,
        DailyRollupEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun batteryDao(): BatteryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "powerguard_db"
                ).addMigrations(DatabaseMigrations.MIGRATION_1_2).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
