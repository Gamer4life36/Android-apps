# ScreenSlayer

A live-wallpaper and wallpaper-rotation app for Android, built with Kotlin and Jetpack
Compose. Build a library of wallpapers and have them rotate automatically on your lock
screen — on unlock, on a shake, or at random.

**Package:** `com.mj.screenslayer`

## Features

### Wallpaper library
- **Slot grid** — manage your wallpapers in a grid of slots (default 15, configurable
  from 1 up to 1000, with quick presets: 15, 25, 50, 100, 250, 500, 1000).
- **Add images** — tap a slot to pick one image, or multi-select several at once.
- **Animated GIF support** — GIFs are first-class. When ScreenSlayer is set as your
  live wallpaper, GIFs play **animated** on the lock screen (via `AnimatedImageDrawable`
  on Android 9+), with scale/dim/grayscale still applied. As a plain system wallpaper,
  the raw GIF is streamed so OEMs that support it (e.g. Samsung One UI) animate it,
  while others show the first frame.
- **Import a folder** — bulk-import every image in a chosen folder into empty slots.
- **Manage slots** — clear an individual slot or clear all slots at once (your original
  image files are never touched).
- **Persistent access** — retains read permission to the images you add.

### Live wallpaper & rotation
- **Live wallpaper engine** — set ScreenSlayer as your live wallpaper; the home screen
  detects when it isn't active yet and shows a one-tap setup banner.
- **Change on unlock** — apply the next wallpaper each time you unlock the device.
- **Shake to change** — shake the phone to switch wallpapers, with Low / Medium / High
  sensitivity presets.
- **Shuffle or sequential** — rotate images in random order or in sequence.
- **Random now** — a shuffle button applies a random wallpaper instantly.
- **Background rotation** — wallpaper changes are applied via a WorkManager worker.
- **Target selection** — wallpapers can be applied to the lock screen, home screen, or
  both.

### Appearance
- **Scale mode** — Fill, Fit, or Stretch.
- **Dim** — darken the wallpaper from 0–70%.
- **Grayscale** — optional black-and-white rendering.
- **App theme** — cycle between System, Light, and Dark themes.

### Lock screen controls
- **Tap to wake** — toggle tap-to-wake (where supported).
- **Screen timeout** — pick a timeout from 15 seconds up to "Never".
- **System shortcuts** — quick deep links to the system clock style, lock-screen
  notifications, shortcuts, screen-lock type, and biometrics settings.

## Building

Standard Gradle Android project:

```bash
./gradlew assembleDebug      # build a debug APK
./gradlew installDebug       # build and install on a connected device/emulator
```

Requires Android Studio (or the Android SDK + JDK 17). `local.properties` is generated
locally and is not committed.

### Release signing

Signing credentials are **not** stored in `build.gradle.kts`. Instead, the release
`signingConfig` reads them from a `keystore.properties` file at the project root, which
is **gitignored** and must never be committed.

To produce a signed release build, create `keystore.properties` next to
`settings.gradle.kts` (copy `keystore.properties.example` and fill in your values):

```properties
storeFile=../screenslayer-release.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=YOUR_KEY_ALIAS
keyPassword=YOUR_KEY_PASSWORD
```

If `keystore.properties` is absent (e.g. a fresh clone), debug builds still work —
only release signing is skipped. Keep the `.jks` keystore and its passwords backed up
securely and out of version control.

## Permissions

ScreenSlayer needs access to the images you select, the live-wallpaper service, and —
for the screen-timeout and tap-to-wake controls — the "Modify system settings"
permission (requested in-app when needed).
