# AL3RTED

A loud, hard-to-miss alarm and alert app for Android, built with Kotlin and Jetpack
Compose. Designed to actually wake you up — with boosted volume, strobe flashlight,
and full-screen alerts that fire even over the lock screen.

**Package:** `com.al3rted.app`

## Features

### Alerts
- **Multiple named alerts** — create, edit, and delete any number of alerts, each with
  its own name.
- **Per-alert scheduling** — set the time and choose which days of the week it repeats
  (Sun–Sat, multi-select).
- **12h / 24h time entry** — large up/down time spinners with an AM/PM toggle in 12-hour
  mode, or a 0–23 hour picker in 24-hour mode.
- **Enable / disable toggle** — turn an alert on or off without deleting it.
- **Survives reboot** — alerts are automatically rescheduled after the device restarts.

### How alerts fire
- **Full-screen alert** — a full-screen alarm screen launches even over the lock screen,
  backed by a foreground service and a high-priority alarm notification.
- **Dismiss action** — dismiss directly from the notification or the alert screen.

### Sound
- **Custom alert sound** — pick any ringtone/alarm tone via the system sound picker
  (defaults to the system alarm sound).
- **Volume 0–150%** — boost past the system maximum using Android's `LoudnessEnhancer`
  (anything above 100% is amplified).
- **Forces volume through** — raises alarm/ring/music/notification streams and grabs
  audio focus so the alert isn't missed, then restores your original volumes afterward.
- **Fade-in** — optionally ramp the sound up from silent over 30 seconds.

### Alert type & vibration
- **Alert types** — Sound & Vibration, Sound Only, Vibration Only, or Screen Only.
- **Vibration patterns** — Pulse, Rapid, Escalating, SOS, and Long Wave.

### Extras
- **Snooze** — configurable snooze duration (1, 5, 10, 15, 20, or 30 minutes).
- **Strobe flashlight** — optionally strobe the camera LED during an alert.
- **24-hour time** — global toggle for 12h vs 24h time display.
- **Custom theming** — in-app app theme / accent picker.

## Building

Standard Gradle Android project:

```bash
./gradlew assembleDebug      # build a debug APK
./gradlew installDebug       # build and install on a connected device/emulator
```

Requires Android Studio (or the Android SDK + JDK 17). `local.properties` is generated
locally and is not committed.

## Permissions

AL3RTED uses exact alarms, a foreground service, full-screen intents, vibration, and
(optionally) camera/flashlight access. These are required for reliable, on-time alarms.
