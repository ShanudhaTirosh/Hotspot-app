package com.shanufx.hotspotx.ui.dashboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shanufx.hotspotx.ui.components.*
import com.shanufx.hotspotx.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(vm: DashboardViewModel = hiltViewModel()) {
    val ui by vm.uiState.collectAsStateWithLifecycle()
    val qrBitmap by vm.qrBitmap.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // QR bottom sheet
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showQrSheet by remember { mutableStateOf(false) }

    LaunchedEffect(qrBitmap) {
        if (qrBitmap != null) showQrSheet = true
    }

    // Error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(ui.errorMessage) {
        ui.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.dismissError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Header ────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("HotspotX", style = MaterialTheme.typography.titleLarge, color = CyanPrimary)
                Spacer(Modifier.weight(1f))
                Text("ShanuFx", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }

            // ── Main Control Card ─────────────────────────────────
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        StatusBadge(
                            label = if (ui.isHotspotActive) "ACTIVE" else "INACTIVE",
                            color = if (ui.isHotspotActive) StatusActive else TextSecondary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Wi-Fi Hotspot", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (ui.isHotspotActive) "${ui.connectedDevices} device(s) connected" else "Tap to enable",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    HotspotToggle(
                        isActive = ui.isHotspotActive,
                        isLoading = ui.isLoading,
                        onClick = vm::toggleHotspot
                    )
                }

                if (ui.isHotspotActive || ui.ssid.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = GlassBorder)
                    Spacer(Modifier.height(12.dp))

                    // SSID row
                    StatRow(label = "SSID", value = ui.ssid.ifEmpty { "—" })
                    Spacer(Modifier.height(6.dp))

                    // Password row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Password", style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (ui.passwordVisible) ui.password else "••••••••",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = CyanPrimary
                            )
                            Spacer(Modifier.width(4.dp))
                            IconButton(onClick = vm::togglePasswordVisibility, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    if (ui.passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // ── Speed Card ────────────────────────────────────────
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Real-time Speed", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    SpeedBlock("↑ Upload", ui.uploadSpeedText, ChartUpload, Modifier.weight(1f))
                    Spacer(Modifier.width(12.dp))
                    SpeedBlock("↓ Download", ui.downloadSpeedText, ChartDownload, Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                SparkLineChart(
                    uploadPoints = ui.uploadHistory,
                    downloadPoints = ui.downloadHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    LegendDot("Upload", ChartUpload)
                    Spacer(Modifier.width(16.dp))
                    LegendDot("Download", ChartDownload)
                    Spacer(Modifier.weight(1f))
                    Text("Today: ${ui.totalTodayText}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }

            // ── Quick Actions ─────────────────────────────────────
            Text("Quick Actions", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Rounded.QrCode2,
                    label = "QR Code",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        vm.generateQrCode()
                    }
                )
                QuickActionButton(
                    icon = Icons.Rounded.ContentCopy,
                    label = "Copy Pass",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("Wi-Fi Password", ui.password))
                    }
                )
                QuickActionButton(
                    icon = Icons.Rounded.Usb,
                    label = "USB Tether",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    }
                )
                QuickActionButton(
                    icon = Icons.Rounded.Refresh,
                    label = "Refresh",
                    modifier = Modifier.weight(1f),
                    onClick = vm::refreshDevices
                )
            }

            // ── USB Tethering Info ────────────────────────────────
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Rounded.Info,
                        contentDescription = null,
                        tint = StatusIdle,
                        modifier = Modifier.size(18.dp).padding(top = 2.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "USB Tethering must be enabled manually in Android Settings > Network & Internet > Hotspot. Tap \"USB Tether\" above.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    // ── QR Code Bottom Sheet ──────────────────────────────────────
    if (showQrSheet && qrBitmap != null) {
        ModalBottomSheet(
            onDismissRequest = { showQrSheet = false; vm.dismissQr() },
            sheetState = sheetState,
            containerColor = SurfaceDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Scan to Connect", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(ui.ssid, style = MaterialTheme.typography.bodyMedium, color = CyanPrimary)
                Spacer(Modifier.height(20.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = androidx.compose.ui.graphics.Color.White) {
                    Image(
                        bitmap = qrBitmap!!.asImageBitmap(),
                        contentDescription = "Wi-Fi QR Code",
                        modifier = Modifier.size(240.dp).padding(12.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Open Camera and point at the QR code",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SpeedBlock(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    GlassCard(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LegendDot(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(8.dp)) { drawCircle(color = color) }
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier.clickable(onClick = onClick), cornerRadius = 14.dp) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(icon, contentDescription = label, tint = CyanPrimary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}
