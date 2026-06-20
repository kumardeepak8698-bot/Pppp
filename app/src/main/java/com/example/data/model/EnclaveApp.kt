package com.example.data.model

data class EnclaveApp(
    val packageName: String,
    val label: String,
    val isFrozen: Boolean = false,
    val isHidden: Boolean = false,
    val isSystem: Boolean = false,
    val launchCount: Int = 0,
    val lastLaunchTime: Long = 0L,
    val totalTimeInForegroundMs: Long = 0L
)
