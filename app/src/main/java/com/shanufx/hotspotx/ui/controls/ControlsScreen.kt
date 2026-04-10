package com.shanufx.hotspotx.ui.controls

import android.net.wifi.SoftApConfiguration
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shanufx.hotspotx.data.db.entity.ScheduleEntity
import com.shanufx.hotspotx.ui.components.*
import com.shanufx.hotspotx.ui.theme.*
import com.shanufx.hotspotx.util.FormatUtils

@Composable
fun ControlsScreen(vm: ControlsViewModel = hiltViewModel()) {
    val ui by vm.uiState.collectAsStateWithLifecycle()
    val settings = ui.settings

    if (ui.showAddSchedule) {
        ScheduleDialog(
            existing = ui.editingSchedule,
            onSave = vm::saveSchedule,
            onDismiss = vm::dismissScheduleDialog
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Controls & Schedule", style = MaterialTheme.typography.titleLarge)

        // ── Hotspot Configuration ─────────────────────────────────
        SectionHeader("Hotspot Configuration")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            // SSID
            var ssid by remember(settings.hotspotSsid) { mutableStateOf(settings.hotspotSsid) }
            OutlinedTextField(
                value = ssid,
                onValueChange = { ssid = it },
                label = { Text("SSID (Network Name)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { vm.updateSsid(ssid) }) {
                        Icon(Icons.Rounded.Check, null, tint = CyanPrimary)
                    }
                }
            )
            Spacer(Modifier.height(12.dp))

            // Password
            var pass by remember(settings.hotspotPassword) { mutableStateOf(settings.hotspotPassword) }
            var passVisible by remember { mutableStateOf(false) }
            val strength = FormatUtils.passwordStrength(pass)
            OutlinedTextField(
                value = pass,
                onValueChange = { pass = it },
                label = { Text("Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passVisible)
                    androidx.compose.ui.text.input.VisualTransformation.None
                else
                    androidx.compose.ui.text.input.PasswordVisualTransformation(),
                trailingIcon = {
                    Row {
                        IconButton(onClick = { passVisible = !passVisible }) {
                            Icon(
                                if (passVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                null, tint = TextSecondary
                            )
                        }
                        IconButton(onClick = { vm.updatePassword(pass) }) {
                            Icon(Icons.Rounded.Check, null, tint = CyanPrimary)
                        }
                    }
                }
            )
            Spacer(Modifier.height(4.dp))
            PasswordStrengthMeter(strength, FormatUtils.passwordStrengthLabel(strength))
            Spacer(Modifier.height(12.dp))

            // Band selector
            Text("Frequency Band", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            val bands = listOf("Auto", "2.4 GHz", "5 GHz")
            val bandValues = listOf(
                SoftApConfiguration.BAND_2GHZ or SoftApConfiguration.BAND_5GHZ,
                SoftApConfiguration.BAND_2GHZ,
                SoftApConfiguration.BAND_5GHZ
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                bands.forEachIndexed { i, label ->
                    FilterChip(
                        selected = settings.hotspotBand == bandValues[i],
                        onClick = { vm.updateBand(bandValues[i]) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = CyanPrimary
                        )
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // Max clients
            Text("Max Clients: ${settings.maxClients}", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = settings.maxClients.toFloat(),
                onValueChange = { vm.updateMaxClients(it.toInt()) },
                valueRange = 1f..10f,
                steps = 8,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(thumbColor = CyanPrimary, activeTrackColor = CyanPrimary)
            )
            Spacer(Modifier.height(4.dp))

            // Hidden network + auto-off
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Hidden Network", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = settings.hiddenNetwork, onCheckedChange = vm::updateHidden,
                    colors = SwitchDefaults.colors(checkedThumbColor = CyanPrimary, checkedTrackColor = CyanPrimary.copy(alpha = 0.4f)))
            }
            Spacer(Modifier.height(4.dp))
            val autoOffLabels = mapOf(0 to "Never", 5 to "5 min", 10 to "10 min", 30 to "30 min", 60 to "1 hour")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Auto-off if idle", style = MaterialTheme.typography.bodyMedium)
                Text(autoOffLabels[settings.autoOffMinutes] ?: "${settings.autoOffMinutes} min",
                    style = MaterialTheme.typography.bodyMedium, color = CyanPrimary)
            }
            Slider(
                value = settings.autoOffMinutes.toFloat(),
                onValueChange = { vm.updateAutoOff(it.toInt()) },
                valueRange = 0f..60f,
                steps = 4,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(thumbColor = CyanPrimary, activeTrackColor = CyanPrimary)
            )
        }

        // ── Schedule ──────────────────────────────────────────────
        SectionHeader("Schedules")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            if (ui.schedules.isEmpty()) {
                Text("No schedules configured.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            } else {
                ui.schedules.forEach { schedule ->
                    ScheduleRow(
                        schedule = schedule,
                        onToggle = { vm.toggleSchedule(schedule.id, it) },
                        onEdit = { vm.editSchedule(schedule) },
                        onDelete = { vm.deleteSchedule(schedule) }
                    )
                    HorizontalDivider(color = GlassBorder, modifier = Modifier.padding(vertical = 6.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = vm::showAddSchedule,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary.copy(alpha = 0.2f),
                    contentColor = CyanPrimary)
            ) {
                Icon(Icons.Rounded.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("Add Schedule")
            }
        }

        // ── Data Limit ────────────────────────────────────────────
        SectionHeader("Monthly Data Cap")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            val capGb = settings.monthlyDataCapBytes / (1024f * 1024 * 1024)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Monthly Cap", style = MaterialTheme.typography.bodyMedium)
                Text(if (settings.monthlyDataCapBytes == 0L) "No limit" else "%.1f GB".format(capGb),
                    style = MaterialTheme.typography.bodyMedium, color = CyanPrimary)
            }
            Slider(
                value = capGb.coerceIn(0f, 100f),
                onValueChange = { vm.updateDataCap((it * 1024 * 1024 * 1024).toLong()) },
                valueRange = 0f..100f,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(thumbColor = CyanPrimary, activeTrackColor = CyanPrimary)
            )
            Spacer(Modifier.height(8.dp))
            Text("Warning at: ${settings.dataCapWarningPercent}%", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = settings.dataCapWarningPercent.toFloat(),
                onValueChange = { vm.updateCapWarning(it.toInt()) },
                valueRange = 50f..95f,
                steps = 8,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(thumbColor = StatusIdle, activeTrackColor = StatusIdle)
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = CyanPrimary)
}

@Composable
private fun ScheduleRow(
    schedule: ScheduleEntity,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                FormatUtils.dayMaskToShortLabels(schedule.daysOfWeekMask),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "${FormatUtils.minutesToTimeString(schedule.startMinutes)} – ${FormatUtils.minutesToTimeString(schedule.endMinutes)}",
                style = MaterialTheme.typography.bodySmall, color = TextSecondary
            )
        }
        Switch(checked = schedule.isEnabled, onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = CyanPrimary, checkedTrackColor = CyanPrimary.copy(alpha = 0.4f)))
        IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, null, tint = TextSecondary) }
        IconButton(onClick = onDelete) { Icon(Icons.Rounded.DeleteOutline, null, tint = StatusBlocked) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleDialog(
    existing: ScheduleEntity?,
    onSave: (ScheduleEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var dayMask by remember { mutableIntStateOf(existing?.daysOfWeekMask ?: 0b1111111) }
    var startMin by remember { mutableIntStateOf(existing?.startMinutes ?: 480) }
    var endMin by remember { mutableIntStateOf(existing?.endMinutes ?: 1320) }
    var label by remember { mutableStateOf(existing?.label ?: "") }
    val dayLabels = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Schedule" else "Edit Schedule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Day selector
                Text("Days", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    dayLabels.forEachIndexed { i, d ->
                        val bit = 1 shl i
                        FilterChip(
                            selected = dayMask and bit != 0,
                            onClick = { dayMask = dayMask xor bit },
                            label = { Text(d) }
                        )
                    }
                }
                // Time pickers (simple text fields for now)
                OutlinedTextField(
                    value = FormatUtils.minutesToTimeString(startMin),
                    onValueChange = { startMin = FormatUtils.timeStringToMinutes(it) },
                    label = { Text("Start Time (HH:MM)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = FormatUtils.minutesToTimeString(endMin),
                    onValueChange = { endMin = FormatUtils.timeStringToMinutes(it) },
                    label = { Text("Stop Time (HH:MM)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(ScheduleEntity(
                    id = existing?.id ?: 0L,
                    daysOfWeekMask = dayMask,
                    startMinutes = startMin,
                    endMinutes = endMin,
                    isEnabled = existing?.isEnabled ?: true,
                    label = label
                ))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = SurfaceDark
    )
}
