# Simple Home (シンプルホーム)

A small, original Android home-screen app. No ads, no logins, no internet permission.

- App grid with search (type, then Enter opens the first match)
- Dock: long-press any icon → Add to dock (up to 5)
- 大きいアイコン / Big tiles, 大きい文字 / Large text, 日本語 / English switch
- Muted dusty rose and sage
- ⚙ opens Android's "Default home app" setting
- 無料アプリ / Free Apps and ユニバース74 / Universe 74 buttons open those sites in the browser
- Runs on Android 5.1 and newer (minSdk 22), including Amazon Fire tablets on Fire OS 5

## Fire tablets
Fire OS has no "Default home app" setting, so on a Fire tablet Simple Home is an ordinary app:
open it from your apps. The Home button still goes to Amazon's home screen, and ⚙ explains this.

## Install
Download the APK from https://gutsmcd-cmd.github.io/free-apps/apk/simple-home.apk, open it, allow
"install unknown apps", then press the Home button and choose シンプルホーム (or ⚙ → Default home app).
To go back, pick your old launcher in Settings → Apps → Default apps → Home app.

## Build
`./gradlew assembleRelease` (Android SDK 37, JDK 17+). Signing reads `keystore.properties` (not committed).
