package com.superdenot.powerguard.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Additive migration that preserves all existing session and sample rows. */
object DatabaseMigrations {
    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE charging_sessions ADD COLUMN hasCurrentData INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE charging_sessions ADD COLUMN hasTemperatureData INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE battery_samples ADD COLUMN isLevelAvailable INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE battery_samples ADD COLUMN isCurrentAvailable INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE battery_samples ADD COLUMN isVoltageAvailable INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE battery_samples ADD COLUMN isTemperatureAvailable INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE battery_samples ADD COLUMN isPowerAvailable INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE battery_samples ADD COLUMN isChargingStatusAvailable INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE battery_samples ADD COLUMN isCharging INTEGER NOT NULL DEFAULT 0")
        }
    }
}
