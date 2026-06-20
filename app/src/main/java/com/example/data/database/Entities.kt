package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "frozen_apps")
data class FrozenApp(
    @PrimaryKey val packageName: String,
    val label: String,
    val isFrozen: Boolean = false,
    val isHidden: Boolean = false,
    val addedTimestamp: Long = System.currentTimeMillis(),
    val lastStateChangeTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "vault_files")
data class VaultFile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fileName: String,
    val encryptedFilePath: String,
    val originalSize: Long,
    val mimeType: String,
    val addedTimestamp: Long = System.currentTimeMillis(),
    val vectorBytes: ByteArray // Initialization vector for AES-GCM decryption
) {
    // Boilerplate structural override due to ByteArray
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as VaultFile
        if (id != other.id) return false
        if (fileName != other.fileName) return false
        if (encryptedFilePath != other.encryptedFilePath) return false
        if (originalSize != other.originalSize) return false
        if (mimeType != other.mimeType) return false
        if (addedTimestamp != other.addedTimestamp) return false
        if (!vectorBytes.contentEquals(other.vectorBytes)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + fileName.hashCode()
        result = 31 * result + encryptedFilePath.hashCode()
        result = 31 * result + originalSize.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + addedTimestamp.hashCode()
        result = 31 * result + vectorBytes.contentHashCode()
        return result
    }
}

@Entity(tableName = "app_usage")
data class AppUsageLog(
    @PrimaryKey val packageName: String,
    val label: String,
    val launchCount: Int = 0,
    val lastLaunchTime: Long = System.currentTimeMillis(),
    val totalTimeInForegroundMs: Long = 0
)

@Entity(tableName = "enclave_settings")
data class EnclaveSetting(
    @PrimaryKey val configKey: String,
    val configValue: String
)
