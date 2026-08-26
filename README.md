# GPT-WORK — Lua Browser

A fast, editable Android browser architecture with Lua-driven browser logic, Google/DuckDuckGo search selection, themes, tabs, bookmarks, history, settings, extension hooks, and GitHub Actions APK builds.

## Architecture

- **Lua**: browser state, settings, theme model, search providers, tabs, bookmarks, history, extension registry.
- **Android/WebView host**: native WebView bridge and APK packaging.
- **GitHub Actions**: reproducible debug APK build.

## Goals

1. Fast startup and navigation.
2. Small, modular files that are easy to edit.
3. Search provider can be changed without touching browser UI.
4. Theme values are centralized in Lua.
5. Extensions use a controlled hook API instead of unrestricted native access.

## Search engines

- Google: `https://www.google.com/search?q=`
- DuckDuckGo: `https://duckduckgo.com/?q=`

## Build

The repository includes a GitHub Actions workflow under `.github/workflows/build-apk.yml`.

> Note: this first foundation keeps the Lua layer self-contained and documents the Android host contract. The native Android WebView bridge can be implemented independently without changing the Lua APIs.
