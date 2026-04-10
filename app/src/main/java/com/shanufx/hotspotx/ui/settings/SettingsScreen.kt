package com.shanufx.hotspotx.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shanufx.hotspotx.ui.components.GlassCard
import com.shanufx.hotspotx.ui.components.StatRow
import com.shanufx.hotspotx.ui.theme.*
import com.shanufx.hotspotx.BuildConfig

@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val ui by vm.uiState.collectAsStateWithLifecycle()
    val settings = ui.settings
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(ui.snackMessage) {
        ui.snackMessage?.let { snackbarHostState.showSnackbar(it); vm.clearSnack() }
    }
    LaunchedEffect(ui.exportedUri) {
        ui.exportedUri?.let { uri ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export Settings"))
            vm.clearExportUri()
        }
    }

    // Reset confirmation
    if (ui.showResetConfirm) {
        AlertDialog(
            onDismissRequest = vm::dismissResetConfirm,
            title = { Text("Reset Usage Stats") },
            text = { Text("All recorded usage data will be permanently deleted. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = vm::resetUsageStats,
                    colors = ButtonDefaults.buttonColors(containerColor = StatusBlocked)
                ) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = vm::dismissResetConfirm) { Text("Cancel") } }
        )
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }, containerColor = BackgroundDark) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.titleLarge)

            // ── Theme ─────────────────────────────────────────────
            SettingSection("Appearance") {
                Text("Theme", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("dark", "light", "system").forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { vm.updateTheme(mode) },
                            label = { Text(mode.replaceFirstChar { it.uppercase() }) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = CyanPrimary
                            )
                        )
                    }
                }
            }

            // ── Notifications ─────────────────────────────────────
            SettingSection("Notifications") {
                ToggleRow("Foreground Service Notification", settings.foregroundServiceEnabled, vm::updateFgService)
                Spacer(Modifier.height(4.dp))
                ToggleRow("Notify on Device Connect", settings.notifyOnDeviceConnect, vm::updateNotifyConnect)
                Spacer(Modifier.height(4.dp))
                ToggleRow("Notify on Data Limit", settings.notifyOnDataLimit, vm::updateNotifyData)
            }

            // ── Startup ───────────────────────────────────────────
            SettingSection("Startup") {
                ToggleRow("Auto-start Hotspot on Boot", settings.autoStartOnBoot, vm::updateAutoBoot)
            }

            // ── Data Management ───────────────────────────────────
            SettingSection("Data Management") {
                Button(
                    onClick = vm::exportSettings,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanPrimary.copy(alpha = 0.2f),
                        contentColor = CyanPrimary
                    )
                ) {
                    Icon(Icons.Rounded.FileUpload, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Export Settings (JSON)")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = vm::showResetConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusBlocked)
                ) {
                    Icon(Icons.Rounded.DeleteForever, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Reset Usage Statistics")
                }
            }

            // ── About ─────────────────────────────────────────────
            SettingSection("About") {
                StatRow("App", "ShanuFx HotspotX")
                Spacer(Modifier.height(4.dp))
                StatRow("Version", BuildConfig.VERSION_NAME)
                Spacer(Modifier.height(4.dp))
                StatRow("Developer", "ShanuFx")
                Spacer(Modifier.height(4.dp))
                StatRow("Package", BuildConfig.APPLICATION_ID)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Built with Kotlin + Jetpack Compose | Material You | MVVM + Hilt | Room",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = CyanPrimary)
        Spacer(Modifier.height(8.dp))
        GlassCard(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CyanPrimary,
                checkedTrackColor = CyanPrimary.copy(alpha = 0.4f)
            )
        )
    }
}
