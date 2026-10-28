# Simple Home Screen (シンプルホーム)

A small, original Android home-screen app. No ads, no logins, no internet permission.

- App grid with search (type, then Enter opens the first match)
- Dock: long-press any icon → Add to dock (up to 5)
- 大きいアイコン / Big tiles, 大きい文字 / Large text, 日本語 / English switch
- Muted dusty rose and sage
- ⚙ opens Android's "Default home app" setting

## Install
Download the APK from https://gutsmcd-cmd.github.io/free-apps/apk/simple-home.apk, open it, allow
"install unknown apps", then press the Home button and choose シンプルホーム (or ⚙ → Default home app).
To go back, pick your old launcher in Settings → Apps → Default apps → Home app.

## Build
`./gradlew assembleRelease` (Android SDK 37, JDK 17+). Signing reads `keystore.properties` (not committed).
