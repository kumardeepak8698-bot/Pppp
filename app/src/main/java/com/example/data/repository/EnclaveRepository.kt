package com.example.data.repository

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import com.example.EnclaveDeviceAdminReceiver
import com.example.data.database.*
import com.example.data.security.CryptoEngine
import com.example.data.model.EnclaveApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class EnclaveRepository(private val context: Context) {

    private val db = EnclaveDatabase.getDatabase(context)
    private val frozenAppDao = db.frozenAppDao()
    private val vaultFileDao = db.vaultFileDao()
    private val appUsageLogDao = db.appUsageLogDao()
    private val enclaveSettingDao = db.enclaveSettingDao()

    private val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val adminComponent = ComponentName(context, EnclaveDeviceAdminReceiver::class.java)

    fun isProfileOwner(): Boolean {
        try {
            return dpm.isProfileOwnerApp(context.packageName)
        } catch (e: Exception) {
            return false
        }
    }

    fun isDeviceOwner(): Boolean {
        try {
            return dpm.isDeviceOwnerApp(context.packageName)
        } catch (e: Exception) {
            return false
        }
    }

    suspend fun getAppsList(): List<EnclaveApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val dbApps = frozenAppDao.getAllFrozenApps().associateBy { it.packageName }
        val dbUsage = appUsageLogDao.getAllUsageLogs().associateBy { it.packageName }

        val systemPackageName = context.packageName

        apps.filter { it.packageName != systemPackageName }.map { appInfo ->
            val pName = appInfo.packageName
            val label = appInfo.loadLabel(pm).toString()
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            
            val savedApp = dbApps[pName]
            val savedUsage = dbUsage[pName]

            var isFrozenLive = savedApp?.isFrozen ?: false
            var isHiddenLive = savedApp?.isHidden ?: false
            
            if (isProfileOwner() || isDeviceOwner()) {
                try {
                    isHiddenLive = dpm.isApplicationHidden(adminComponent, pName)
                } catch (e: Exception) {
                    Log.e("EnclaveRepository", "Error checking application hidden status: ${e.message}")
                }
            }

            EnclaveApp(
                packageName = pName,
                label = label,
                isFrozen = isFrozenLive,
                isHidden = isHiddenLive,
                isSystem = isSystem,
                launchCount = savedUsage?.launchCount ?: 0,
                lastLaunchTime = savedUsage?.lastLaunchTime ?: 0L,
                totalTimeInForegroundMs = savedUsage?.totalTimeInForegroundMs ?: 0L
            )
        }.sortedBy { it.label }
    }

    fun getFrozenAppsFlow(): Flow<List<FrozenApp>> = frozenAppDao.getAllFrozenAppsFlow()
    fun getVaultFilesFlow(): Flow<List<VaultFile>> = vaultFileDao.getAllVaultFilesFlow()
    fun getUsageLogsFlow(): Flow<List<AppUsageLog>> = appUsageLogDao.getAllUsageLogsFlow()

    suspend fun freezeApp(packageName: String, label: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            var apiExecuted = false
            if (isProfileOwner() || isDeviceOwner()) {
                val array = arrayOf(packageName)
                val failed = dpm.setPackagesSuspended(adminComponent, array, true)
                apiExecuted = failed.isEmpty()
            }
            
            frozenAppDao.insertOrUpdateApp(
                FrozenApp(
                    packageName = packageName,
                    label = label,
                    isFrozen = true,
                    lastStateChangeTimestamp = System.currentTimeMillis()
                )
            )

            if (apiExecuted || !isProfileOwner()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to suspend/freeze app via DevicePolicyManager"))
            }
        } catch (e: SecurityException) {
            frozenAppDao.insertOrUpdateApp(FrozenApp(packageName, label, isFrozen = true))
            Result.failure(SecurityException("Requires Profile Owner or Device Owner permissions. Run ADB enrollment to unlock fully: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unfreezeApp(packageName: String, label: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            var apiExecuted = false
            if (isProfileOwner() || isDeviceOwner()) {
                val array = arrayOf(packageName)
                val failed = dpm.setPackagesSuspended(adminComponent, array, false)
                apiExecuted = failed.isEmpty()
            }

            frozenAppDao.insertOrUpdateApp(
                FrozenApp(
                    packageName = packageName,
                    label = label,
                    isFrozen = false,
                    lastStateChangeTimestamp = System.currentTimeMillis()
                )
            )

            if (apiExecuted || !isProfileOwner()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to unsuspend/unfreeze app via DevicePolicyManager"))
            }
        } catch (e: SecurityException) {
            frozenAppDao.insertOrUpdateApp(FrozenApp(packageName, label, isFrozen = false))
            Result.failure(SecurityException("Requires Profile Owner or Device Owner permissions. Run ADB enrollment to unlock fully: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun hideApp(packageName: String, label: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (isProfileOwner() || isDeviceOwner()) {
                dpm.setApplicationHidden(adminComponent, packageName, true)
            }
            frozenAppDao.insertOrUpdateApp(
                FrozenApp(
                    packageName = packageName,
                    label = label,
                    isHidden = true,
                    lastStateChangeTimestamp = System.currentTimeMillis()
                )
            )
            Result.success(Unit)
        } catch (e: SecurityException) {
            frozenAppDao.insertOrUpdateApp(FrozenApp(packageName, label, isHidden = true))
            Result.failure(SecurityException("Hiding/disabling packages requires Device/Profile Owner privileges: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unhideApp(packageName: String, label: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (isProfileOwner() || isDeviceOwner()) {
                dpm.setApplicationHidden(adminComponent, packageName, false)
            }
            frozenAppDao.insertOrUpdateApp(
                FrozenApp(
                    packageName = packageName,
                    label = label,
                    isHidden = false,
                    lastStateChangeTimestamp = System.currentTimeMillis()
                )
            )
            Result.success(Unit)
        } catch (e: SecurityException) {
            frozenAppDao.insertOrUpdateApp(FrozenApp(packageName, label, isHidden = false))
            Result.failure(SecurityException("Unhiding packages requires Device/Profile Owner privileges: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cloneAppToWorkProfile(packageName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (isProfileOwner() || isDeviceOwner()) {
                dpm.enableSystemApp(adminComponent, packageName)
                Result.success(Unit)
            } else {
                Result.failure(SecurityException("Enabling an app in the Work Profile container is restricted to the Work Profile Owner."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addFileToVault(fileName: String, size: Long, mimeType: String, inputStream: InputStream): Result<VaultFile> = withContext(Dispatchers.IO) {
        try {
            val privateDir = File(context.filesDir, "vault_storage")
            if (!privateDir.exists()) {
                privateDir.mkdirs()
            }
            val targetFile = File(privateDir, "vault_${System.currentTimeMillis()}_${fileName.hashCode()}.enc")
            val outputStream = FileOutputStream(targetFile)

            val ivBytes = CryptoEngine.encrypt(inputStream, outputStream)

            val vaultFile = VaultFile(
                fileName = fileName,
                encryptedFilePath = targetFile.absolutePath,
                originalSize = size,
                mimeType = mimeType,
                vectorBytes = ivBytes
            )

            val insertId = vaultFileDao.insertFile(vaultFile)
            Result.success(vaultFile.copy(id = insertId.toInt()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun decryptVaultFile(vaultFile: VaultFile, outputStream: OutputStream): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val targetFile = File(vaultFile.encryptedFilePath)
            if (!targetFile.exists()) {
                return@withContext Result.failure(Exception("Encrypted Vault file not found!"))
            }
            FileInputStream(targetFile).use { inputStream ->
                CryptoEngine.decrypt(inputStream, outputStream, vaultFile.vectorBytes)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeVaultFile(vaultFile: VaultFile): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val targetFile = File(vaultFile.encryptedFilePath)
            if (targetFile.exists()) {
                targetFile.delete()
            }
            vaultFileDao.deleteFile(vaultFile)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logAppLaunch(packageName: String, label: String) = withContext(Dispatchers.IO) {
        val existing = appUsageLogDao.getUsageByPackage(packageName)
        if (existing == null) {
            appUsageLogDao.insertOrUpdateUsage(
                AppUsageLog(
                    packageName = packageName,
                    label = label,
                    launchCount = 1,
                    lastLaunchTime = System.currentTimeMillis(),
                    totalTimeInForegroundMs = 5000L
                )
            )
        } else {
            appUsageLogDao.incrementLaunchCount(packageName, System.currentTimeMillis())
        }
    }

    suspend fun getSetting(key: String, defaultValue: String): String {
        return enclaveSettingDao.getSettingByKey(key) ?: defaultValue
    }

    suspend fun saveSetting(key: String, value: String) {
        enclaveSettingDao.saveSetting(EnclaveSetting(key, value))
    }
}
