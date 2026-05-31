# Android apps

A collection of Android applications (Kotlin / Jetpack Compose), each in its own folder.

## Apps

### AL3RTED
A loud, hard-to-miss alarm and alert app. Create scheduled alerts that fire full-screen
over the lock screen, with boosted volume, custom vibration patterns, fade-in, snooze,
and a strobe flashlight.

- **Package:** `com.al3rted.app`
- **Highlights:** multiple named alerts with day-of-week repeat, 12h/24h time entry,
  custom sound, volume up to 150% (boosted), Sound/Vibration/Screen alert types, five
  vibration patterns, snooze, fade-in, and strobe flashlight.
- **Full feature list:** [`AL3RTED/Al3rted/README.md`](AL3RTED/Al3rted/README.md)
- **Location:** [`AL3RTED/Al3rted`](AL3RTED/Al3rted)

### ScreenSlayer
A live-wallpaper and wallpaper-rotation app. Build a library of wallpapers and have them
rotate automatically on your lock screen — on unlock, on a shake, or at random.

- **Package:** `com.mj.screenslayer`
- **Highlights:** wallpaper slot grid (1–1000 slots), single/multi/folder import, live
  wallpaper engine, change-on-unlock and shake-to-change triggers, shuffle/sequential
  order, scale/dim/grayscale appearance, and lock-screen timeout & tap-to-wake controls.
- **Full feature list:** [`ScreenSlayer/README.md`](ScreenSlayer/README.md)
- **Location:** [`ScreenSlayer`](ScreenSlayer)

## Building

Each app is a standard Gradle Android project. From inside an app folder:

```bash
./gradlew assembleDebug      # build a debug APK
./gradlew installDebug       # build and install on a connected device/emulator
```

You'll need Android Studio (or the Android SDK + JDK 17) installed. Each project's
`local.properties` (pointing at your local SDK) is intentionally not committed —
Android Studio will generate it on first open.

## Note on secrets

Release signing keystores (`*.jks`), keystore credentials (`PASS.txt`,
`KEYSTORE_CREDENTIALS.txt`), and build artifacts (`*.apk`, `*.aab`) are excluded from
this repository via `.gitignore` and are kept locally only.
