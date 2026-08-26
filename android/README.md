# Android host contract

The Android host should expose a small bridge to Lua:

- `WebView.loadUrl(url)`
- `WebView.goBack()`
- `WebView.goForward()`
- `WebView.reload()`
- `WebView.canGoBack()`
- `WebView.canGoForward()`
- `Browser.openExternal(url)`
- `Storage.get(key)` / `Storage.set(key, value)`

Keep the native layer thin. Browser behavior belongs in `lua/` so changes remain fast and editable.

Recommended WebView hardening:

- Safe Browsing enabled.
- HTTPS preferred.
- File/content access disabled unless explicitly needed.
- External intents restricted to allowlisted schemes.
- Downloads handled by Android DownloadManager.
