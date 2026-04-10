package com.shanufx.hotspotx.ui.devices

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shanufx.hotspotx.data.repository.ConnectedDevice
import com.shanufx.hotspotx.data.repository.DeviceStatus
import com.shanufx.hotspotx.ui.components.*
import com.shanufx.hotspotx.ui.theme.*
import com.shanufx.hotspotx.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(vm: DevicesViewModel = hiltViewModel()) {
    val ui by vm.uiState.collectAsStateWithLifecycle()
    val pullState = rememberPullToRefreshState()

    if (pullState.isRefreshing) {
        LaunchedEffect(Unit) {
            vm.refresh()
            pullState.endRefresh()
        }
    }

    // Rename dialog
    ui.renameDialogMac?.let { mac ->
        var nameInput by remember { mutableStateOf(ui.renameDialogCurrentName) }
        AlertDialog(
            onDismissRequest = vm::dismissDialogs,
            title = { Text("Rename Device") },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Nickname") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.confirmRename(mac, nameInput) }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = vm::dismissDialogs) { Text("Cancel") }
            }
        )
    }

    // Block confirmation dialog
    ui.blockWarningMac?.let { mac ->
        AlertDialog(
            onDismissRequest = vm::dismissDialogs,
            icon = { Icon(Icons.Rounded.Block, null, tint = StatusBlocked) },
            title = { Text("Block Device?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("The device will be added to the blocklist.")
                    GlassCard {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Rounded.Warning, null, tint = StatusIdle, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Full packet-level blocking requires root. The app will exclude this device when restarting the hotspot.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { vm.confirmBlock(mac) },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusBlocked)
                ) { Text("Block") }
            },
            dismissButton = {
                TextButton(onClick = vm::dismissDialogs) { Text("Cancel") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = ui.isRefreshing,
            onRefresh = vm::refresh,
            modifier = Modifier.fillMaxSize()
        ) {
            if (ui.connectedDevices.isEmpty()) {
                EmptyDevicesPlaceholder()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Connected Devices", style = MaterialTheme.typography.titleLarge)
                            StatusBadge(
                                label = "${ui.connectedDevices.size}",
                                color = CyanPrimary
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    items(ui.connectedDevices, key = { it.mac }) { device ->
                        DeviceCard(
                            device = device,
                            onBlock = { vm.blockDevice(device.mac) },
                            onAllow = { vm.allowDevice(device.mac) },
                            onRename = { vm.showRenameDialog(device.mac, device.nickname ?: device.hostname ?: "") },
                            onPin = { vm.pinDevice(device.mac, !device.isPinned) }
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun EmptyDevicesPlaceholder() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Rounded.WifiOff, null, modifier = Modifier.size(64.dp), tint = TextSecondary)
            Text("No Devices Connected", style = MaterialTheme.typography.titleMedium)
            Text("Pull down to refresh", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DeviceCard(
    device: ConnectedDevice,
    onBlock: () -> Unit,
    onAllow: () -> Unit,
    onRename: () -> Unit,
    onPin: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = deviceIcon(device.deviceType),
                contentDescription = null,
                tint = CyanPrimary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.nickname ?: device.hostname ?: "Unknown Device",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${device.ip}  •  ${device.mac}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            StatusBadge(
                label = device.status.name,
                color = when (device.status) {
                    DeviceStatus.ACTIVE  -> StatusActive
                    DeviceStatus.IDLE    -> StatusIdle
                    DeviceStatus.BLOCKED -> StatusBlocked
                }
            )
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(28.dp)) {
                Icon(
                    if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Expanded details
        if (expanded) {
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = GlassBorder)
            Spacer(Modifier.height(10.dp))
            StatRow("Session Duration", FormatUtils.formatDuration(System.currentTimeMillis() - device.firstSeen))
            Spacer(Modifier.height(4.dp))
            StatRow("Device Type", device.deviceType.replaceFirstChar { it.uppercase() })
            Spacer(Modifier.height(12.dp))
            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Block / Allow
                if (device.status == DeviceStatus.BLOCKED) {
                    OutlinedButton(onClick = onAllow, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Allow", style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    OutlinedButton(
                        onClick = onBlock,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusBlocked)
                    ) {
                        Icon(Icons.Rounded.Block, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Block", style = MaterialTheme.typography.labelLarge)
                    }
                }
                // Rename
                OutlinedButton(onClick = onRename, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Rename", style = MaterialTheme.typography.labelLarge)
                }
                // Pin
                IconButton(onClick = onPin) {
                    Icon(
                        if (device.isPinned) Icons.Rounded.PushPin else Icons.Rounded.PushPin,
                        contentDescription = "Pin",
                        tint = if (device.isPinned) CyanPrimary else TextSecondary
                    )
                }
            }
        }
    }
}

private fun deviceIcon(type: String): ImageVector = when (type.lowercase()) {
    "phone"   -> Icons.Rounded.PhoneAndroid
    "laptop"  -> Icons.Rounded.Laptop
    "tablet"  -> Icons.Rounded.TabletAndroid
    "tv"      -> Icons.Rounded.Tv
    else      -> Icons.Rounded.DeviceUnknown
}
