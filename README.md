# 0.3MP Lo-Fi Cam (Android)

Camera app with an early-2000s ~0.3MP CMOS look. Planning docs are in `replica/`.

## Get the APK
Push to GitHub, open **Actions**, open the latest "Build APK" run, download the `0.3mp-camera-apk` artifact (a zip). Inside: `app-release.apk` (use this) and `app-debug.apk`. Install on your phone and allow "install unknown apps".

The release APK is signed with a key generated fresh on every build. To update the app, uninstall the old version first.

## Notes
- Android 8+. Live filtered preview needs Android 13+; older phones show the normal preview but saved photos are still filtered.
- Video is recorded unfiltered at low resolution, without audio.
- Paywall UI exists but is off (`Config.PAYWALL_ENABLED`). Play Billing is not wired up.
- Font is the system monospace. To use Share Tech Mono, add the .ttf to `app/src/main/res/font/`.
