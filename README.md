# ShanuFx HotspotX

A full-featured Wi-Fi Hotspot & USB Tethering manager for Android — no root required.

**Package:** `com.shanufx.hotspotx`  
**Min SDK:** Android 8.0 (API 26) | **Target SDK:** Android 14 (API 34)  
**Developer:** ShanuFx (Shanudha Tirosh)

---

## Features

| Feature | Details |
|---|---|
| **Hotspot Control** | Toggle Wi-Fi hotspot with animated ON/OFF button |
| **API-level Handling** | TetheringManager (API 30+) with LocalOnlyHotspot fallback (API 26–29) |
| **Device Discovery** | ARP table scan via `/proc/net/arp` — no root needed, refreshes every 3s |
| **Real-time Speed** | Upload ↑ / Download ↓ via `TrafficStats`, 1-second polling |
| **Usage History** | `NetworkStatsManager` + Room DB snapshots, hourly/daily/monthly charts |
| **QR Code Share** | ZXing Wi-Fi QR (WPA format), scannable by Android/iOS Camera |
| **Schedules** | WorkManager + AlarmManager, per-day of week, start/stop times |
| **Data Cap** | Monthly limit, warning threshold, auto-disable at cap |
| **Device Management** | Block/Allow, rename, pin — persisted in Room |
| **USB Tethering** | Deep link to `Settings.ACTION_WIRELESS_SETTINGS` with explanation |
| **Foreground Service** | Persistent notification with live stats + Stop/Dashboard actions |
| **Boot Auto-start** | `BOOT_COMPLETED` BroadcastReceiver |
| **Dark Glassmorphism** | Cyan/Violet accent, Material3 Dark theme |
| **Export** | Usage CSV + Settings JSON via FileProvider |

---

## Architecture

```
┌─────────────────────────────────────────────────┐
│                   UI Layer                      │
│  DashboardScreen  DevicesScreen  UsageScreen    │
│  ControlsScreen   SettingsScreen                │
│  (Jetpack Compose + Material3)                  │
├─────────────────────────────────────────────────┤
│               ViewModel Layer                   │
│  DashboardVM  DevicesVM  UsageVM                │
│  ControlsVM   SettingsVM  (Hilt + StateFlow)    │
├─────────────────────────────────────────────────┤
│             Repository Layer                    │
│  TetheringRepository  StatsRepository           │
│  DeviceRepository     SettingsRepository        │
├──────────────┬──────────────────────────────────┤
│  Local DB    │  Android APIs                    │
│  Room        │  WifiManager / TetheringManager  │
│  DataStore   │  NetworkStatsManager             │
│  (Entities,  │  TrafficStats / ConnectivityMgr  │
│   DAOs)      │  AlarmManager / WorkManager      │
└──────────────┴──────────────────────────────────┘
```

## Tech Stack

- **Language:** Kotlin (100%)
- **UI:** Jetpack Compose + Material3
- **DI:** Hilt
- **DB:** Room (entities: Device, UsageSnapshot, Schedule, Session)
- **Async:** Kotlin Coroutines + Flow
- **Background:** ForegroundService + WorkManager + AlarmManager
- **Charts:** MPAndroidChart (line, bar — via AndroidView)
- **QR Code:** ZXing Android Embedded
- **Prefs:** DataStore Preferences
- **Navigation:** Jetpack Navigation Component (NavGraph)
- **Build:** Gradle KTS + Version Catalog (libs.versions.toml)

---

## Project Structure

```
app/src/main/java/com/shanufx/hotspotx/
├── HotspotXApp.kt                     # @HiltAndroidApp + WorkManager config
├── MainActivity.kt                    # Single-activity entry point
├── di/
│   └── Modules.kt                     # DatabaseModule + AppModule
├── data/
│   ├── db/
│   │   ├── HotspotDatabase.kt
│   │   ├── entity/Entities.kt         # Device, UsageSnapshot, Schedule, Session
│   │   └── dao/
│   │       ├── DeviceDao.kt
│   │       ├── UsageSnapshotDao.kt    # Hourly, daily, per-device queries
│   │       └── ScheduleAndSessionDao.kt
│   └── repository/
│       ├── TetheringRepository.kt     # Hotspot start/stop, API 26–34 handling
│       ├── StatsRepository.kt         # TrafficStats + NetworkStatsManager
│       ├── DeviceRepository.kt        # ARP scan + Room sync
│       └── SettingsRepository.kt      # DataStore preferences
├── service/
│   └── HotspotService.kt              # ForegroundService: ARP poll + speed + sessions
├── receiver/
│   ├── BootReceiver.kt                # Auto-start on boot
│   └── ScheduleReceiver.kt            # AlarmManager schedule trigger
├── worker/
│   └── ScheduleWorker.kt              # WorkManager: programs AlarmManager
├── util/
│   ├── ArpScanner.kt                  # /proc/net/arp parser + hostname resolver
│   ├── FormatUtils.kt                 # Bytes, speed, duration formatters
│   ├── NotificationHelper.kt          # Channels + notification builders
│   └── QrCodeGenerator.kt             # ZXing Wi-Fi QR bitmap
└── ui/
    ├── theme/
    │   ├── Color.kt                   # Cyan/Violet glassmorphism palette
    │   ├── Theme.kt                   # Material3 dark/light color schemes
    │   └── Type.kt                    # Typography scale
    ├── navigation/NavGraph.kt         # Bottom nav + NavHost
    ├── components/
    │   ├── UiComponents.kt            # GlassCard, HotspotToggle, SparkLine, badges
    │   └── PermissionHandler.kt       # Permission guard composable
    ├── dashboard/
    │   ├── DashboardViewModel.kt
    │   └── DashboardScreen.kt         # Toggle, speed, QR sheet, quick actions
    ├── devices/
    │   ├── DevicesViewModel.kt
    │   └── DevicesScreen.kt           # Device cards, block/rename/pin
    ├── usage/
    │   ├── UsageViewModel.kt
    │   └── UsageScreen.kt             # MPAndroidChart line + bar charts
    ├── controls/
    │   ├── ControlsViewModel.kt
    │   └── ControlsScreen.kt          # Config, schedule CRUD, data cap
    └── settings/
        ├── SettingsViewModel.kt
        └── SettingsScreen.kt          # Theme, notifications, export/import
```

---

## Build & Run

```bash
# Clone and open in Android Studio Iguana or newer
git clone <repo>
cd HotspotX

# Build debug APK
./gradlew assembleDebug

# Install to device
./gradlew installDebug
```

### Requirements
- Android Studio Iguana+ (2023.2.1+)
- JDK 17
- Android device or emulator with API 26+

---

## Permissions Explained

| Permission | Why |
|---|---|
| `ACCESS_FINE_LOCATION` | Required to read SSID on Android 10+ |
| `READ_PHONE_STATE` | NetworkStatsManager requires it |
| `FOREGROUND_SERVICE_CONNECTED_DEVICE` | Android 14 requires typed foreground services |
| `POST_NOTIFICATIONS` | Android 13+ requires explicit permission for foreground notification |
| `TETHER_PRIVILEGED` | Needed for TetheringManager — falls back to LocalOnlyHotspot if denied |

---

## Notes on Hotspot API

- **API 30+:** Attempts `TetheringManager.startTethering()` via reflection. On non-privileged apps this throws `SecurityException` and the app gracefully falls back to `WifiManager.startLocalOnlyHotspot()`.
- **API 26–29:** Uses `WifiManager.startLocalOnlyHotspot()` directly. The SSID/password are assigned by Android, not configurable without root.
- **USB Tethering:** Cannot be toggled programmatically without root on any API level. The app deep-links to `Settings.ACTION_WIRELESS_SETTINGS` with a clear explanation in the UI.
- **MAC Blocking:** Full packet-level blocking requires root (iptables). The app maintains a local blocklist in Room and excludes blocked devices when the hotspot restarts. A warning is shown to the user.

---

## License

MIT © ShanuFx
