package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FrozenApp::class,
        VaultFile::class,
        AppUsageLog::class,
        EnclaveSetting::class
    ],
    version = 1,
    exportSchema = false
)
abstract class EnclaveDatabase : RoomDatabase() {

    abstract fun frozenAppDao(): FrozenAppDao
    abstract fun vaultFileDao(): VaultFileDao
    abstract fun appUsageLogDao(): AppUsageLogDao
    abstract fun enclaveSettingDao(): EnclaveSettingDao

    companion object {
        @Volatile
        private var INSTANCE: EnclaveDatabase? = null

        fun getDatabase(context: Context): EnclaveDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EnclaveDatabase::class.java,
                    "enclave_enterprise_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
