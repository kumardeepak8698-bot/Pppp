package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FrozenAppDao {
    @Query("SELECT * FROM frozen_apps ORDER BY label ASC")
    fun getAllFrozenAppsFlow(): Flow<List<FrozenApp>>

    @Query("SELECT * FROM frozen_apps ORDER BY label ASC")
    suspend fun getAllFrozenApps(): List<FrozenApp>

    @Query("SELECT * FROM frozen_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getAppByPackage(packageName: String): FrozenApp?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateApp(app: FrozenApp)

    @Delete
    suspend fun removeApp(app: FrozenApp)

    @Query("DELETE FROM frozen_apps WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)
}

@Dao
interface VaultFileDao {
    @Query("SELECT * FROM vault_files ORDER BY addedTimestamp DESC")
    fun getAllVaultFilesFlow(): Flow<List<VaultFile>>

    @Query("SELECT * FROM vault_files ORDER BY addedTimestamp DESC")
    suspend fun getAllVaultFiles(): List<VaultFile>

    @Query("SELECT * FROM vault_files WHERE id = :id LIMIT 1")
    suspend fun getFileById(id: Int): VaultFile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: VaultFile): Long

    @Delete
    suspend fun deleteFile(file: VaultFile)
}

@Dao
interface AppUsageLogDao {
    @Query("SELECT * FROM app_usage ORDER BY totalTimeInForegroundMs DESC, launchCount DESC")
    fun getAllUsageLogsFlow(): Flow<List<AppUsageLog>>

    @Query("SELECT * FROM app_usage ORDER BY totalTimeInForegroundMs DESC, launchCount DESC")
    suspend fun getAllUsageLogs(): List<AppUsageLog>

    @Query("SELECT * FROM app_usage WHERE packageName = :packageName LIMIT 1")
    suspend fun getUsageByPackage(packageName: String): AppUsageLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUsage(log: AppUsageLog)

    @Query("UPDATE app_usage SET launchCount = launchCount + 1, lastLaunchTime = :time WHERE packageName = :packageName")
    suspend fun incrementLaunchCount(packageName: String, time: Long)
}

@Dao
interface EnclaveSettingDao {
    @Query("SELECT * FROM enclave_settings")
    suspend fun getAllSettings(): List<EnclaveSetting>

    @Query("SELECT configValue FROM enclave_settings WHERE configKey = :key LIMIT 1")
    suspend fun getSettingByKey(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSetting(setting: EnclaveSetting)
}
