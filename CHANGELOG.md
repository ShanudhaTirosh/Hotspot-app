# Changelog

All notable changes to ShanuFx HotspotX are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

---

## [Unreleased]

### Added
- Initial release

---

## [1.0.0] — 2025-04-10

### Added
- Wi-Fi Hotspot toggle with animated ON/OFF button
- LocalOnlyHotspot fallback for API 26–29
- TetheringManager support for API 30+
- ARP table scanning (`/proc/net/arp`) — no root required
- Real-time upload/download speed via TrafficStats (1s polling)
- Connected device cards with block/allow/rename/pin actions
- QR code share sheet (ZXing, Wi-Fi WPA format)
- USB Tethering deep-link to Android Settings with explanation
- Hotspot scheduling (WorkManager + AlarmManager, per-day-of-week)
- Monthly data cap with warning threshold and auto-disable
- Auto-off after N minutes idle (AutoOffManager)
- Foreground service with persistent notification + Stop action
- Boot auto-start BroadcastReceiver
- Usage charts: real-time line, hourly bar, daily stacked bar (MPAndroidChart)
- Session history stored in Room DB
- Usage CSV export via FileProvider
- Settings JSON export/import
- Dark glassmorphism UI with cyan/violet accent (Material3)
- Hilt DI, MVVM, StateFlow, Jetpack Compose
- GitHub Actions CI/CD: debug, release, GitHub Release, lint, Telegram/Slack alerts
