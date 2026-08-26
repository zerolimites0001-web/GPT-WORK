# GPT-WORK Browser

A fast Android browser with a Lua logic layer and native WebView host.

## Included now
- Real Android Gradle project.
- WebView navigation, address/search bar, back/forward/reload/home.
- Google and DuckDuckGo search selection.
- Tabs foundation, bookmarks, downloads, history/cache clearing.
- Dark/light theme switching.
- Lua runtime (`LuaJ`) used for search URL logic.
- Safe Browsing, HTTPS-first navigation policy, hardware rendering.
- Extension architecture remains exposed through `lua/extensions.lua` for future script hooks.
- GitHub Actions builds a debug APK on every push/PR and manual dispatch.

## Build locally
Install Android Studio + SDK 35 + JDK 17, then run `./gradlew :app:assembleDebug`.

## APK
Use the Actions artifact named `GPT-WORK-debug` after a successful workflow run.
