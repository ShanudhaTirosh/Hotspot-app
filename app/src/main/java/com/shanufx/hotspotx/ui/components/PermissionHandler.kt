package com.shanufx.hotspotx.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.shanufx.hotspotx.ui.theme.CyanPrimary
import com.shanufx.hotspotx.ui.theme.TextSecondary

data class PermissionInfo(
    val permission: String,
    val rationale: String
)

@Composable
fun PermissionGuard(
    permissions: List<PermissionInfo>,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    fun allGranted() = permissions.all {
        ContextCompat.checkSelfPermission(context, it.permission) == PackageManager.PERMISSION_GRANTED
    }

    var granted by remember { mutableStateOf(allGranted()) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        granted = results.values.all { it }
    }

    if (granted) {
        content()
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Rounded.VerifiedUser,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = CyanPrimary
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Permissions Required",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(8.dp))
            permissions.forEach { perm ->
                Text(
                    "• ${perm.rationale}",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { launcher.launch(permissions.map { it.permission }.toTypedArray()) }
            ) {
                Text("Grant Permissions")
            }
        }
    }
}

fun requiredPermissions(): List<PermissionInfo> = buildList {
    add(PermissionInfo(
        Manifest.permission.ACCESS_FINE_LOCATION,
        "Location — required to read Wi-Fi SSID on Android 10+"
    ))
    add(PermissionInfo(
        Manifest.permission.ACCESS_WIFI_STATE,
        "Wi-Fi state — needed to manage hotspot"
    ))
    add(PermissionInfo(
        Manifest.permission.READ_PHONE_STATE,
        "Phone state — required for network usage stats"
    ))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(PermissionInfo(
            Manifest.permission.POST_NOTIFICATIONS,
            "Notifications — show persistent hotspot status"
        ))
    }
}
