package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.database.VaultFile
import com.example.data.model.EnclaveApp
import com.example.ui.viewmodel.EnclaveViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: EnclaveViewModel,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val isProfileOwner by viewModel.isProfileOwner.collectAsState()
    val isDeviceOwner by viewModel.isDeviceOwner.collectAsState()
    val uiError by viewModel.uiError.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    
    var currentTab by remember { mutableStateOf(0) }

    LaunchedEffect(uiError) {
        uiError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    MyCustomTheme(isDark = isDarkMode) {
        Scaffold(
            topBar = {
                LargeTopAppBar(
                    title = {
                        Column {
                            Text(
                                "ENCLAVE DPC",
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp
                            )
                            Text(
                                "Advanced Workspace Sandbox",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.primary
                    ),
                    actions = {
                        IconButton(onClick = { viewModel.toggleDarkMode() }) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Theme"
                            )
                        }
                        IconButton(onClick = onLogout) {
                            Icon(Icons.Default.Lock, contentDescription = "Lock Dashboard")
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = { Icon(Icons.Default.Widgets, contentDescription = "Sandbox") },
                        label = { Text("Sandbox") }
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = { Icon(Icons.Default.Security, contentDescription = "Vault") },
                        label = { Text("Vault") }
                    )
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        icon = { Icon(Icons.Default.Equalizer, contentDescription = "Usage") },
                        label = { Text("Usage") }
                    )
                    NavigationBarItem(
                        selected = currentTab == 3,
                        onClick = { currentTab = 3 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (currentTab) {
                    0 -> SandboxTab(viewModel, isProfileOwner || isDeviceOwner)
                    1 -> VaultTab(viewModel)
                    2 -> UsageTab(viewModel)
                    3 -> SettingsTab(viewModel, isProfileOwner, isDeviceOwner)
                }
            }
        }
    }
}

@Composable
fun SandboxTab(viewModel: EnclaveViewModel, isEnrolled: Boolean) {
    val filteredApps by viewModel.filteredApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var expandedPackageName by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        if (!isEnrolled) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "DPC Mode Offline",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Limited functionality. Fully set up the profile or enrollment owner via Settings parameters.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            label = { Text("Search system packages...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("app_search_field"),
            singleLine = true,
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            }
        )

        if (filteredApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Apps,
                        contentDescription = "Empty",
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No applications matches your lookup", color = MaterialTheme.colorScheme.secondary)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    val isExpanded = expandedPackageName == app.packageName
                    AppCard(
                        app = app,
                        isExpanded = isExpanded,
                        onExpandToggle = {
                            expandedPackageName = if (isExpanded) null else app.packageName
                        },
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun AppCard(
    app: EnclaveApp,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    viewModel: EnclaveViewModel
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpandToggle() }
            .testTag("app_card_${app.packageName}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (app.isFrozen) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (app.isFrozen) MaterialTheme.colorScheme.outline
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (app.isFrozen) Icons.Default.AcUnit 
                                      else if (app.isSystem) Icons.Default.SystemUpdateAlt 
                                      else Icons.Default.HomeMini,
                        contentDescription = null,
                        tint = if (app.isFrozen) MaterialTheme.colorScheme.onSurfaceVariant 
                               else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.label,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (app.isFrozen) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (app.isFrozen) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("FROZEN", fontSize = 10.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        )
                    }
                    if (app.isHidden) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("HIDDEN", fontSize = 10.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Launch Track", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Total launches: ${app.launchCount} times", style = MaterialTheme.typography.bodyMedium)
                            if (app.lastLaunchTime > 0L) {
                                val date = Date(app.lastLaunchTime)
                                val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                Text("Active: ${format.format(date)}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Button(
                            onClick = { viewModel.launchApplication(context, app.packageName, app.label) },
                            modifier = Modifier.testTag("launch_btn_${app.packageName}")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Launch")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isProfileRegistered by viewModel.isProfileOwner.collectAsState()
                        
                        // Frozen Control Buttons
                        OutlinedButton(
                            onClick = {
                                if (app.isFrozen) {
                                    viewModel.unfreezeApplication(app.packageName, app.label)
                                } else {
                                    viewModel.freezeApplication(app.packageName, app.label)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (app.isFrozen) Icons.Default.WbSunny else Icons.Default.AcUnit,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (app.isFrozen) "Thaw" else "Freeze")
                        }

                        // Hide/Unhide Buttons
                        OutlinedButton(
                            onClick = {
                                if (app.isHidden) {
                                    viewModel.unhideApplication(app.packageName, app.label)
                                } else {
                                    viewModel.hideApplication(app.packageName, app.label)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (app.isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (app.isHidden) "Unhide" else "Hide")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Clone inside Sandbox Container button
                    Button(
                        onClick = { viewModel.clonePackage(app.packageName) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clone App into Work Profile Enclave")
                    }
                }
            }
        }
    }
}

@Composable
fun VaultTab(viewModel: EnclaveViewModel) {
    val context = LocalContext.current
    val vaultFiles by viewModel.vaultFiles.collectAsState()

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { selectedUri ->
            val resolver = context.contentResolver
            var fileName = "unknown"
            var fileSize = 0L
            try {
                resolver.query(selectedUri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        fileName = cursor.getString(nameIdx)
                        fileSize = cursor.getLong(sizeIdx)
                    }
                }
            } catch (e: Exception) {
                // Ignore query failure, use random default
                fileName = "vault_secured_file_" + System.currentTimeMillis()
            }
            val mimeType = resolver.getType(selectedUri) ?: "application/octet-stream"
            viewModel.encryptFileToVault(context, selectedUri, fileName, fileSize, mimeType)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.EnhancedEncryption,
                        contentDescription = "Shield",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Military-Grade Vault",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "All files are encrypted inside the application private SQLite and block structures using secure hardware AES-GCM tags.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { filePicker.launch("*/*") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("vault_upload_button")
        ) {
            Icon(Icons.Default.CloudUpload, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Select & Encrypt File to Vault")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (vaultFiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.FolderZip,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Your Vault is currently empty.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(vaultFiles, key = { it.id }) { file ->
                    VaultFileCard(
                        file = file,
                        onExportClick = {
                            decryptAndOpen(file, context, viewModel)
                        },
                        onDeleteClick = {
                            viewModel.removeFileFromVault(file)
                        }
                    )
                }
            }
        }
    }
}

fun decryptAndOpen(file: VaultFile, context: Context, viewModel: EnclaveViewModel) {
    try {
        val cacheFile = File(context.cacheDir, "decrypted_${file.fileName}")
        val outputStream = FileOutputStream(cacheFile)
        viewModel.decryptAndExportFile(file, outputStream) {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, cacheFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, file.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Open Encrypted file: ${file.fileName}"))
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun VaultFileCard(
    file: VaultFile,
    onExportClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    file.fileName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${file.originalSize / 1024} KB | ${file.mimeType}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Row {
                IconButton(onClick = onExportClick) {
                    Icon(
                        Icons.Default.Launch,
                        contentDescription = "Decrypt & View",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove Permanently",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun UsageTab(viewModel: EnclaveViewModel) {
    val logs by viewModel.usageLogs.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Sandbox Logs & Metrics",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Visual analytics generated dynamically inside the secure partition container.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No app telemetry logged yet", color = MaterialTheme.colorScheme.secondary)
            }
        } else {
            // Draw a pristine, professional canvas based analytic chart
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                val chartPrimaryColor = MaterialTheme.colorScheme.primary
                val chartSecondaryColor = MaterialTheme.colorScheme.onSurfaceVariant
                
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    val maxVal = logs.maxOfOrNull { it.launchCount.toFloat() } ?: 1f
                    val count = logs.size
                    val spacing = size.width / (count + 1)

                    // Draw grid helper lines
                    for (i in 1..4) {
                        val y = size.height * i / 5
                        drawLine(
                            color = chartSecondaryColor.copy(alpha = 0.1f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 2f
                        )
                    }

                    logs.take(5).forEachIndexed { index, appUsageLog ->
                        val x = spacing * (index + 1)
                        val frac = appUsageLog.launchCount.toFloat() / maxVal
                        val barHeight = size.height * frac * 0.75f
                        val y = size.height - barHeight

                        // Dynamic bar shading gradients
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(chartPrimaryColor, chartPrimaryColor.copy(alpha = 0.4f))
                            ),
                            topLeft = Offset(x - 20f, y),
                            size = androidx.compose.ui.geometry.Size(40f, barHeight)
                        )
                        
                        // Text Labels underneath Canvas bars
                        drawContext.canvas.nativeCanvas.drawText(
                            if (appUsageLog.label.length > 5) appUsageLog.label.take(5) + ".." else appUsageLog.label,
                            x - 20f,
                            size.height - 4f,
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.GRAY
                                textSize = 24f
                                isAntiAlias = true
                            }
                        )
                    }
                }
            }

            // Simple details list
            Text(
                "Telemetry Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { log ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(log.label, fontWeight = FontWeight.Bold)
                            Text(log.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        }
                        Text(
                            "${log.launchCount} Launches",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun SettingsTab(
    viewModel: EnclaveViewModel,
    isProfileOwner: Boolean,
    isDeviceOwner: Boolean
) {
    val activity = LocalContext.current as Activity
    var pinValue by remember { mutableStateOf("") }
    
    // Check if PIN lock is enabled active
    val isLocked by viewModel.isLocked.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Enrollment & Administration",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Configure this partition boundary using strict Knox & Android Enterprise protocols.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        // Provisioning Controls Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Enterprise Provisioning",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Generate a dedicated Android Knox system Work Profile to segment operations seamlessly.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.triggerWorkProfileProvisioning(activity) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isProfileOwner) MaterialTheme.colorScheme.outline
                            else MaterialTheme.colorScheme.primary
                        ),
                        enabled = !isProfileOwner
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isProfileOwner) "Work Profile Active" else "Setup Work Profile Sandbox")
                    }
                }
            }
        }

        // ADB Commands Instructions Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "ADB Enrollment Activation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "To grant full hardware Freeze/Thaw and App Hiding flags locally across profile partitions, execute the following command in ADB terminal:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SelectionContainer {
                        Text(
                            "adb shell dpm set-profile-owner com.aistudio.enclave.qyznpx/com.example.EnclaveDeviceAdminReceiver 0",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Security locks PIN Cards
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Privacy Dashboard Passcode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Secure the dashboard sandbox behind an AES encrypted local PIN code. Leave blank to disable lock.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = pinValue,
                        onValueChange = { if (it.length <= 6) pinValue = it },
                        label = { Text("6-digit security PIN") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            viewModel.setupPinLock(pinValue)
                            Toast.makeText(activity, "Enclave configuration updated", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Update Security Lock Code")
                    }
                }
            }
        }
    }
}

// Compact typography/themes definitions
@Composable
fun MyCustomTheme(
    isDark: Boolean,
    content: @Composable () -> Unit
) {
    val myColors = if (isDark) {
        darkColorScheme(
            primary = Color(0xFFD0E4FF),
            onPrimary = Color(0xFF00315C),
            primaryContainer = Color(0xFF3E4759),
            onPrimaryContainer = Color(0xFFD6E3FF),
            secondary = Color(0xFF8E9199),
            onSecondary = Color(0xFF1E2125),
            tertiary = Color(0xFFB4E39B),
            onTertiary = Color(0xFF153800),
            background = Color(0xFF1A1C1E),
            onBackground = Color(0xFFE2E2E6),
            surface = Color(0xFF212327),
            onSurface = Color(0xFFE2E2E6),
            surfaceVariant = Color(0xFF212327),
            onSurfaceVariant = Color(0xFFC4C6CF),
            outline = Color(0xFF43474E)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF005FAF),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFD6E3FF),
            onPrimaryContainer = Color(0xFF001B3E),
            secondary = Color(0xFF535F70),
            onSecondary = Color.White,
            tertiary = Color(0xFF3B692A),
            onTertiary = Color.White,
            background = Color(0xFFF9F9FC),
            onBackground = Color(0xFF1A1C1E),
            surface = Color(0xFFECEFF5),
            onSurface = Color(0xFF1A1C1E),
            surfaceVariant = Color(0xFFDFE2EB),
            onSurfaceVariant = Color(0xFF43474E),
            outline = Color(0xFF73777F)
        )
    }

    MaterialTheme(
        colorScheme = myColors,
        content = content
    )
}
