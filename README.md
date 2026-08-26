# GPT-WORK Browser v2

Fast, editable Android browser with Lua-driven configuration and native WebView.

## V2

- Larger bottom action for **new tab**.
- Visual tab chooser with new-tab action.
- Loading progress bar and page status.
- Google / DuckDuckGo search switching.
- Persistent bookmarks.
- **Bookmarklet manager**: create and execute JavaScript bookmarklets.
- **Native DevTools panel**: console capture, JavaScript evaluator, HTML dump, WebView/runtime info.
- Fast WebView defaults: hardware rendering, cache, DOM storage, Safe Browsing, image loading and offscreen preraster where supported.
- Settings: homepage, JavaScript, desktop user-agent, popup preference, Safe Browsing and DNS/DoH profile.
- Downloads through Android DownloadManager.

### DNS note

The DNS setting stores and tests a custom DNS/DoH endpoint, but Android WebView does not expose a simple per-WebView DNS setter. A true per-app DNS transport requires a network/VPN/HTTP stack layer. V2 deliberately does not pretend the setting changes WebView DNS when it does not.

## Build

GitHub Actions builds the debug APK automatically from `main`.
