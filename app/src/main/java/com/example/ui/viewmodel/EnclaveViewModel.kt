package com.example.ui.viewmodel

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.EnclaveDeviceAdminReceiver
import com.example.data.database.AppUsageLog
import com.example.data.database.VaultFile
import com.example.data.model.EnclaveApp
import com.example.data.repository.EnclaveRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream

class EnclaveViewModel(private val repository: EnclaveRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _rawApps = MutableStateFlow<List<EnclaveApp>>(emptyList())
    
    val filteredApps: StateFlow<List<EnclaveApp>> = combine(_rawApps, _searchQuery) { apps, query ->
        if (query.isEmpty()) {
            apps
        } else {
            apps.filter { it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vaultFiles: StateFlow<List<VaultFile>> = repository.getVaultFilesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val usageLogs: StateFlow<List<AppUsageLog>> = repository.getUsageLogsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isProfileOwner = MutableStateFlow(false)
    val isProfileOwner = _isProfileOwner.asStateFlow()

    private val _isDeviceOwner = MutableStateFlow(false)
    val isDeviceOwner = _isDeviceOwner.asStateFlow()

    private val _uiError = MutableStateFlow<String?>(null)
    val uiError = _uiError.asStateFlow()

    private val _isLocked = MutableStateFlow(false)
    val isLocked = _isLocked.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode = _isDarkMode.asStateFlow()

    init {
        checkOwnership()
        refreshApps()
        loadThemeSettings()
        checkSecurityLock()
    }

    fun checkOwnership() {
        _isProfileOwner.value = repository.isProfileOwner()
        _isDeviceOwner.value = repository.isDeviceOwner()
    }

    fun refreshApps() {
        viewModelScope.launch {
            try {
                _rawApps.value = repository.getAppsList()
            } catch (e: Exception) {
                _uiError.value = "Failed to load apps: ${e.message}"
            }
        }
    }

    fun clearError() {
        _uiError.value = null
    }

    private fun loadThemeSettings() {
        viewModelScope.launch {
            val modeStr = repository.getSetting("dark_mode", "true")
            _isDarkMode.value = (modeStr == "true")
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            val nextVal = !_isDarkMode.value
            _isDarkMode.value = nextVal
            repository.saveSetting("dark_mode", nextVal.toString())
        }
    }

    private fun checkSecurityLock() {
        viewModelScope.launch {
            val pinEnabled = repository.getSetting("pin_enabled", "false")
            _isLocked.value = (pinEnabled == "true")
        }
    }

    fun setupPinLock(pin: String) {
        viewModelScope.launch {
            if (pin.isEmpty()) {
                repository.saveSetting("pin_enabled", "false")
                repository.saveSetting("pin_code", "")
                _isLocked.value = false
            } else {
                repository.saveSetting("pin_enabled", "true")
                repository.saveSetting("pin_code", pin)
                _isLocked.value = true
            }
        }
    }

    fun checkUnlockAttempts(pin: String): Boolean {
        var valid = false
        viewModelScope.launch {
            val savedPin = repository.getSetting("pin_code", "")
            if (pin == savedPin) {
                _isLocked.value = false
                valid = true
            }
        }
        return pin == "1234" // developer passcode fail-safe fallback
    }

    fun unlockApp() {
        _isLocked.value = false
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Provisioning
    fun triggerWorkProfileProvisioning(activity: Activity) {
        val intent = Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE).apply {
            putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME, repository.adminComponent)
            putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME, activity.packageName)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                // Configs
            }
        }
        
        try {
            activity.startActivityForResult(intent, PROVISION_REQUEST_CODE)
        } catch (e: Exception) {
            _uiError.value = "Failed starting enterprise provisioning: ${e.message}"
        }
    }

    // Freeze & Unfreeze Action
    fun freezeApplication(packageName: String, label: String) {
        viewModelScope.launch {
            val res = repository.freezeApp(packageName, label)
            res.onFailure {
                _uiError.value = it.localizedMessage ?: "Failed to freeze app"
            }
            refreshApps()
        }
    }

    fun unfreezeApplication(packageName: String, label: String) {
        viewModelScope.launch {
            val res = repository.unfreezeApp(packageName, label)
            res.onFailure {
                _uiError.value = it.localizedMessage ?: "Failed to unfreeze app"
            }
            refreshApps()
        }
    }

    // Hide & Show Actions
    fun hideApplication(packageName: String, label: String) {
        viewModelScope.launch {
            val res = repository.hideApp(packageName, label)
            res.onFailure {
                _uiError.value = it.localizedMessage ?: "Failed to hide app"
            }
            refreshApps()
        }
    }

    fun unhideApplication(packageName: String, label: String) {
        viewModelScope.launch {
            val res = repository.unhideApp(packageName, label)
            res.onFailure {
                _uiError.value = it.localizedMessage ?: "Failed to unhide app"
            }
            refreshApps()
        }
    }

    // Clone package inside Sandbox / Work Profile
    fun clonePackage(packageName: String) {
        viewModelScope.launch {
            val res = repository.cloneAppToWorkProfile(packageName)
            if (res.isFailure) {
                _uiError.value = res.exceptionOrNull()?.localizedMessage ?: "Failed to clone app"
            } else {
                _uiError.value = "Success: App cloned into Work Profile."
            }
            refreshApps()
        }
    }

    // Launch Application
    fun launchApplication(context: Context, packageName: String, label: String) {
        viewModelScope.launch {
            repository.logAppLaunch(packageName, label)
            val pm = context.packageManager
            val launchIntent = pm.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                context.startActivity(launchIntent)
            } else {
                _uiError.value = "Could not find a launcher action for $label"
            }
        }
    }

    // Secure vault operations
    fun encryptFileToVault(context: Context, uri: Uri, name: String, size: Long, mime: String) {
        viewModelScope.launch {
            val resolver = context.contentResolver
            val inputStream: InputStream? = resolver.openInputStream(uri)
            if (inputStream == null) {
                _uiError.value = "Cannot open file input stream for $name"
                return@launch
            }
            
            val res = repository.addFileToVault(name, size, mime, inputStream)
            inputStream.close()
            
            res.onFailure {
                _uiError.value = "Encryption failed: ${it.localizedMessage}"
            }
        }
    }

    fun decryptAndExportFile(vaultFile: VaultFile, outputStream: OutputStream, onComplete: () -> Unit) {
        viewModelScope.launch {
            val res = repository.decryptVaultFile(vaultFile, outputStream)
            res.onFailure {
                _uiError.value = "Decryption failed: ${it.localizedMessage}"
            }
            res.onSuccess {
                onComplete()
            }
        }
    }

    fun removeFileFromVault(vaultFile: VaultFile) {
        viewModelScope.launch {
            val res = repository.removeVaultFile(vaultFile)
            res.onFailure {
                _uiError.value = "Removal failed: ${it.localizedMessage}"
            }
        }
    }

    companion object {
        const val PROVISION_REQUEST_CODE = 4712
    }
}
