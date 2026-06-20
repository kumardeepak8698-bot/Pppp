package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.data.repository.EnclaveRepository
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LockScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.EnclaveViewModel

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: EnclaveViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = EnclaveRepository(this)
        viewModel = EnclaveViewModel(repository)

        setContent {
            val isDarkTheme by viewModel.isDarkMode.collectAsState()
            val isLocked by viewModel.isLocked.collectAsState()

            MyApplicationTheme(darkTheme = isDarkTheme) {
                if (isLocked) {
                    LockScreen(
                        onUnlockSuccess = { viewModel.unlockApp() },
                        onVerifyPin = { pin -> viewModel.checkUnlockAttempts(pin) }
                    )
                } else {
                    DashboardScreen(
                        viewModel = viewModel,
                        onLogout = { viewModel.setupPinLock("1234") }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) {
            viewModel.checkOwnership()
            viewModel.refreshApps()
        }
    }
}
