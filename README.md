# Android apps

A collection of Android applications (Kotlin / Jetpack Compose), each in its own folder.

## Apps

### AL3RTED
An alarm and alert manager app. Lets you create, edit, and schedule alerts that fire
via Android's alarm system, with the service restarting on device boot.

- **Package:** `com.al3rted.app`
- **Key parts:** alarm scheduling (`AlarmScheduler`, `AlarmReceiver`), boot persistence
  (`BootReceiver`), foreground alerting (`AlertService`), and a Compose UI for the
  alert list, editing, and settings.
- **Location:** [`AL3RTED/Al3rted`](AL3RTED/Al3rted)

### ScreenSlayer
A live wallpaper / wallpaper rotation app. Provides a live wallpaper engine plus
scheduled wallpaper rotation, with lock-screen and preview settings.

- **Package:** `com.mj.screenslayer`
- **Key parts:** live wallpaper service (`ScreenSlayerLiveWallpaper`), rotation worker
  (`WallpaperRotationWorker`), boot persistence (`BootReceiver`), and a Compose UI
  (home, preview, wallpaper/lock-screen settings).
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
