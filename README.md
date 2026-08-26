# GPT-WORK Browser

A real, buildable Android browser project using a native Android WebView and a Lua logic layer through LuaJ. LuaJ has an Android example and is distributed from Maven Central. citeturn0search0turn0search2

## Working features
- Android Gradle project.
- Web navigation, address bar, back/forward/reload/home.
- Google and DuckDuckGo search selection.
- Multiple-tab foundation.
- Bookmarks persisted in app preferences.
- DownloadManager integration.
- History/cache clearing.
- Dark/light theme switching.
- Lua-driven search URL generation.
- WebView Safe Browsing where supported, HTTPS/HTTP navigation only, hardware rendering.
- GitHub Actions debug APK build.

## Build

GitHub Actions installs Gradle 8.10.2 and Android SDK 35, then runs `gradle :app:assembleDebug`. The APK is uploaded as the `GPT-WORK-debug` artifact.

This is intentionally modular: the native host is kept small while browser behavior can be moved into `lua/` modules as features grow.
