# AutoRotateApp — Screen Rotation / چرخش صفحه

A lightweight Android utility for controlling device orientation, with Persian and English UI, light/dark themes, per-app orientation rules, a floating quick panel, and restoration of the original rotation settings when global control is disabled.

## Features
- Persian and English UI; language choice is saved.
- Light and dark themes; selected theme persists across restarts.
- Global modes: auto sensor, portrait, reverse portrait, landscape, reverse landscape, reading, and video.
- Per-app rules for launcher apps.
- Floating quick panel to cycle common orientations and drag the panel by its title.
- Reapply saved global orientation after boot or app replacement when enabled and permissions allow.
- Save the original device rotation settings on first enable and restore them when global control is disabled.

## Requirements and permissions
- Android 5.0 (API 21) or newer.
- Modify system settings (WRITE_SETTINGS): required to change device rotation. Grant it through the app's settings-permission button.
- Display over other apps (SYSTEM_ALERT_WINDOW): only needed for the floating panel.
- Accessibility service: only needed for per-app rules. It listens for foreground-window changes and applies the saved package orientation; it does not request window-content access.
- Receive boot completed: allows the saved global orientation to be restored after reboot.

Per-app rules may be limited by Android versions, manufacturer customizations, games, video apps, or apps that manage orientation themselves. Battery restrictions can also delay background behavior. Some apps may ignore system rotation settings.

## Advertising
- TapsellPlus standard and native banner placements are integrated in the main screen.
- Adivery interstitial ads are limited to natural actions and a minimum cooldown; app-open ads are only considered after returning from the background.
- The optional rewarded ad grants 24 hours without full-screen ads; banners remain available.
- Ad SDK failures are logged and caught so they should not prevent core rotation controls from working. Live SDK behavior still requires a device test.

## Build
JDK 17 and Android SDK Platform 34 are expected. The Gradle Wrapper is included and pins Gradle 8.5 with distribution checksum verification.

Linux/macOS: ./gradlew assembleDebug

Windows: gradlew.bat assembleDebug

No CI workflow is enabled by default.

## Developer
Ali Android Studio / استودیو اندروید علی
