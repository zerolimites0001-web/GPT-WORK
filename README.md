# ⚠️ GPT-WORK Browser — DESCONTINUADO

> **Status: Descontinuado / Discontinued — 26/08/2026**
> Este repositório não receberá mais atualizações, correções ou suporte. O código permanece público sob licença MIT para estudo, fork ou arquivamento.
> *This repository is discontinued and will no longer be maintained.*

---

# GPT-WORK Browser v2.5

<p align="center">
  <img src="app/src/main/res/drawable/ic_launcher_foreground.xml" width="96" alt="GPT-WORK"/>
  <br/>
  <b>Browser Android rápido, editável e com Lua — WebView turbo</b>
  <br/>
  <a href="https://github.com/zerolimites0001-web/GPT-WORK/actions"><img src="https://github.com/zerolimites0001-web/GPT-WORK/workflows/Build%20GPT-WORK%20APK/badge.svg" alt="Build"/></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-blue.svg" alt="MIT"/></a>
  <img src="https://img.shields.io/badge/minSdk-23-green.svg" alt="minSdk"/>
  <img src="https://img.shields.io/badge/targetSdk-35-orange.svg" alt="targetSdk"/>
</p>

## 📑 Índice
- [O que é](#o-que-é)
- [Features v2.5](#features-v25)
- [Arquitetura](#arquitetura)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Tela inicial própria](#tela-inicial-própria)
- [Abas reais multi-WebView](#abas-reais-multi-webview)
- [Armazenamento: IndexDB / LocalStorage / Cookies](#armazenamento-indexdb--localstorage--cookies)
- [Redirect & OAuth](#redirect--oauth)
- [Segurança e privacidade](#segurança-e-privacidade)
- [Downloads](#downloads)
- [Background / Spotify mode](#background--spotify-mode)
- [Lua Engine](#lua-engine)
- [Build (GitHub Actions)](#build-github-actions)
- [Instalação](#instalação)
- [Uso](#uso)
- [Configurações (engrenagem)](#configurações-engrenagem)
- [Ícones SVG](#ícones-svg)
- [Performance](#performance)
- [Permissões](#permissões)
- [API e storage](#api-e-storage)
- [Troubleshooting](#troubleshooting)
- [Descontinuação](#descontinuação)
- [Licença](#licença)

## O que é
GPT-WORK Browser é um browser Android nativo escrito em Kotlin + WebView, com camada Lua editável (`lua/`), com abas reais, DevTools nativo, bookmarklets, e gestão completa de storage/cookies/OAuth.

- **Package:** `com.gptwork.browser`
- **Linguagem:** Kotlin 2.0.21 + Gradle 8.10.2 + AGP 8.7.3
- **SDK:** `compileSdk 35`, `minSdk 23` (Android 6.0+), `targetSdk 35`, Java 17
- **WebView:** `androidx.webkit:webkit:1.12.1`, hardware rendering, cache, DOM storage
- **Lua:** `org.luaj:luaj-jse:3.0.1` para `searchUrl(provider, query)`

## Features v2.5
| Categoria | Detalhe |
|-----------|---------|
| **Tela inicial** | `assets/home.html` com grid 8 tiles, busca, chips, badges IndexDB/LocalStorage/Redirect/DoH |
| **Abas reais** | `FrameLayout` + `WebHolder` (`GTab`) com `applicationContext` WebView, `visibility` switch, música continua ao trocar |
| **Background** | `BackgroundAudioService` foreground (`mediaPlayback`) + `WebHolder` nunca destrói, `moveTaskToBack` no back, notificação "Tocando em segundo plano" |
| **Armazenamento** | Toggles `domStorage`, `databaseEnabled` (IndexDB), `CookieManager` + third-party, `WebStorage.deleteAllData()` |
| **Redirect/OAuth** | `shouldOverrideUrlLoading` permite `accounts.google.com`, `github.com`, `login.microsoftonline.com`, `appleid.apple.com` quando `oauth=true`; `setSupportMultipleWindows` + `onCreateWindow` |
| **Segurança** | `safeBrowsing` toggle, `httpsOnly`, `blockMixed` (`MIXED_CONTENT_NEVER_ALLOW`), `onReceivedSslError` com dialog |
| **Privacidade** | `DNT` header/JS, `incognito`, `dnsProfile` (DoH) salvo, teste via `HttpURLConnection HEAD` |
| **Downloads** | `DownloadManager` em `/sdcard/Download` + cópia em `filesDir/downloads`, dialog "Arquivo pode ser nocivo", `DownloadsActivity` com tempo, pausa (aviso), delete, FileProvider |
| **UI** | 16 VectorDrawable SVG (`ic_home`, `ic_search`, `ic_tabs`, `ic_settings` gear, `ic_download`, etc), `Material3` DayNight |
| **Perf** | `hardwareAccelerated`, `largeHeap`, `RENDERER_PRIORITY_IMPORTANT`, `offscreenPreRaster`, `setEnableSmoothTransition`, DNS prefetch HEAD, `app:about:blank` warm |

## Arquitetura
```
App (BrowserApplication)
 └─ MainActivity (FrameLayout webContainer + top bar + bottom nav)
     ├─ WebHolder (object singleton, GTab list)
     ├─ BackgroundAudioService (foreground, START_STICKY)
     ├─ SettingsActivity (gear)
     └─ DownloadsActivity (FileProvider)
LuaEngine (JsePlatform) -> searchUrl()
assets/home.html -> file:///android_asset/home.html
```

- **Single Activity + multiple WebViews:** cada `newTab()` cria `WebView(applicationContext)` configurado, adiciona no `webContainer`, `switchTo()` só troca `visibility` e `onResume/resumeTimers`.
- **Holder:** `data class GTab(val webView: WebView, var url, var title)` em `object WebHolder` sobrevive a `onDestroy`.
- **Service:** `START_STICKY`, `foregroundServiceType="mediaPlayback"`, canal `gptwork_playback`.

## Estrutura do projeto
```
GPT-WORK/
├── app/
│   ├── build.gradle.kts
│   ├── src/main/
│   │   ├── AndroidManifest.xml (MainActivity, SettingsActivity, DownloadsActivity, BackgroundAudioService, FileProvider)
│   │   ├── assets/home.html
│   │   ├── java/com/gptwork/browser/
│   │   │   ├── MainActivity.kt (real tabs, download confirm)
│   │   │   ├── BackgroundAudioService.kt
│   │   │   ├── SettingsActivity.kt
│   │   │   ├── DownloadsActivity.kt
│   │   │   ├── BrowserApplication.kt (warm-up)
│   │   │   └── LuaEngine.kt
│   │   ├── res/
│   │   │   ├── drawable/*.xml (15 SVG)
│   │   │   ├── xml/file_paths.xml
│   │   │   └── values/{colors,strings,themes}.xml
│   └── proguard-rules.pro
├── lua/ (contrato host: loadUrl, goBack, Storage.get/set)
├── android/ (host contract README)
├── .github/workflows/build-apk.yml
├── build.gradle.kts / settings.gradle.kts / gradle.properties
└── LICENSE (MIT)
```

## Tela inicial própria
- Arquivo: `app/src/main/assets/home.html`
- Carregada como `homepage = file:///android_asset/home.html` (padrão, editável em Settings)
- Contém: logo gradiente, `.searchBox` (input + botão Ir), `.grid` 4x2 tiles, `.chips`, `.badge` status
- JS: `go()` redireciona para `https://` ou busca Google

## Abas reais multi-WebView
- **Antes (v2.0):** `pages = MutableList<String>` + 1 WebView -> nova aba perdia anterior.
- **Agora:** `WebHolder.tabs: MutableList<GTab>` + `webContainer: FrameLayout`
- `createWebView()` configura cada WebView com mesmo settings; `newTab(url)` cria, `switchTo(i)` hide/show; `showTabs()` dialog lista `title` e permite fechar.
- Audio continua pois `onPause()` não pausa timers e `onDestroy()` não destrói se tem tabs.

## Armazenamento: IndexDB / LocalStorage / Cookies
- `WebSettings.domStorageEnabled = prefs domStorage`
- `WebSettings.databaseEnabled = prefs indexdb`
- `CookieManager.setAcceptCookie` + `setAcceptThirdPartyCookies`
- Settings toggles + `clearAll()` via `WebStorage.deleteAllData()` + `removeAllCookies` + `prefs.clear()`
- DownloadsActivity mostra `CookieManager.getCookie` e permite limpar.

## Redirect & OAuth
- `shouldOverrideUrlLoading`:
  - Lista allow: `accounts.google.com`, `github.com`, `login.microsoftonline.com`, `appleid.apple.com`, `auth0.com`, `okta.com`
  - Se `oauth && url.contains(allowed)` -> `return false` (deixa carregar)
  - `redirect` false -> bloqueia `request.isRedirect`
  - `httpsOnly` -> `http://` -> `https://`
- `setSupportMultipleWindows(oauth)` + `onCreateWindow` cria popup WebView com dialog.

## Segurança e privacidade
- **Segurança:** `safeBrowsing` toggle, `httpsOnly`, `blockMixed`, `onReceivedSslError` dialog Proceed/Cancel, `allowFileAccess=false`, `allowUniversalAccessFromFileURLs=false`.
- **Privacidade:** `DNT` via header/JS `navigator.doNotTrack='1'`, `incognito`, `dnsProfile` (salvo, testado HEAD), `saveFormData=false`, `setGeolocationEnabled(false)`.

## Downloads
- **Listener:** `setDownloadListener` com `URLUtil.guessFileName`, dialog "Deseja continuar? Arquivo pode ser nocivo"
- **Botão:** `ic_download` na bottom nav -> `showDownloads()` -> `DownloadsActivity`
- **Salva em dois lugares:** `DownloadManager` (`/sdcard/Download/fileName`) + `executor` copia via `HttpURLConnection` para `filesDir/downloads/fileName`
- **Permissões:** `WRITE_EXTERNAL_STORAGE` (maxSdk 32), `READ_EXTERNAL_STORAGE` (maxSdk 32), `READ_MEDIA_*`; só pede se `SDK <33` e necessário (não idiota).
- **DownloadsActivity:** `ScrollView` + `LinearLayout`, seções `app/files/downloads` e `DownloadManager` (`/sdcard/Download`), cada card com `ProgressBar`, `⏱ tempo`, `pausar` (aviso que DM não suporta), `deletar` (`dm.remove(id)` ou `file.delete()`), `abrir` via `FileProvider` ou `DownloadManager.getUriForDownloadedFile`.

## Background / Spotify mode
- `BackgroundAudioService : Service` com `NotificationChannel gptwork_playback`, `START_STICKY`, `FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK` (Android 14+)
- Notificação ongoing: "GPT-WORK • Tocando em segundo plano • Toque para voltar" + ação Parar
- `MainActivity`:
  - `WebView(applicationContext)` para sobreviver
  - `onPause() -> startBgService()`
  - `onBackPressed() -> moveTaskToBack(true) + startBgService()` (não finish)
  - `onDestroy() -> não destrói holder, só removeView + startBgService()`
- YouTube música continua com app em background/notificação.

## Lua Engine
```kotlin
class LuaEngine {
  fun eval(script: String): String
  fun searchUrl(provider:String, query:String): String // google vs duckduckgo
}
```
Usa `JsePlatform.standardGlobals().load(script).call()`.

## Build (GitHub Actions)
- Workflow: `.github/workflows/build-apk.yml`
  - `on: push main, pull_request, workflow_dispatch`
  - `runs-on: ubuntu-latest`
  - Steps: `checkout@v4`, `setup-java@v4 (temurin 17)`, `gradle/actions/setup-gradle@v4 (8.10.2)`, `android-actions/setup-android@v3`, `sdkmanager "platforms;android-35" "build-tools;35.0.0"`, `gradle :app:assembleDebug`, `upload-artifact@v4 (GPT-WORK-debug, app/build/outputs/apk/debug/*.apk)`
- **Compila apenas via GitHub Actions**.

## Instalação
1. Baixe o APK em `Actions` -> último `Build GPT-WORK APK` -> `GPT-WORK-debug` ou em `/sdcard/Download/GPT-WORK-v2.5-BG-FIX.apk` (se via device)
2. Permita "Instalar apps desconhecidos"
3. Instale:
   ```bash
   adb install app-debug.apk
   # ou
   pm install /sdcard/Download/GPT-WORK-v2.5-BG-FIX.apk
   ```

## Uso
- **Endereço/busca:** digite URL ou termo -> `lua.searchUrl` decide Google vs DuckDuckGo
- **Abas:** `+` nova, `▣` lista, `1/3` indicador; fechar via dialog "✕ Fechar aba"
- **Home:** `⌂` volta para `file:///android_asset/home.html`
- **Favoritos:** `★` salva `title<TAB>url` em `SharedPreferences`
- **Downloads:** `⬇️` abre DownloadsActivity; download via long-press link ou `DownloadListener`
- **DevTools:** menu `☰` -> `DevTools` (console, JS, HTML, Info)
- **Config:** `⚙️` abre `SettingsActivity`

## Configurações (engrenagem)
Acessível via `⚙️` bottom nav:
- **Geral:** homepage, desktop UA, JavaScript, buscador
- **Armazenamento:** LocalStorage, IndexDB, Cookies (+ third-party), ver/limpar
- **Redirect & OAuth:** permitir redirects, OAuth, pop-ups, lista domínios
- **Segurança:** Safe Browsing, HTTPS apenas, bloquear misto
- **Privacidade:** DNT, incognito, DNS/DoH + Salvar/Testar
- **Sobre:** versão, WebView 35, badges

## Ícones SVG
16 VectorDrawable em `res/drawable`: `ic_home`, `ic_search`, `ic_tabs`, `ic_bookmark`, `ic_settings` (gear), `ic_shield`, `ic_cookie`, `ic_storage`, `ic_back`, `ic_forward`, `ic_refresh`, `ic_add`, `ic_incognito`, `ic_devtools`, `ic_oauth`, `ic_download`, `ic_music_note`.

## Performance
- `hardwareAccelerated=true`, `largeHeap=true`, `RENDERER_PRIORITY_IMPORTANT`, `offscreenPreRaster`, `setEnableSmoothTransition`, `WebView(applicationContext)` warm-up em `BrowserApplication` (thread `MIN_PRIORITY`), DNS prefetch `HEAD` em `executor`, `cacheMode=LOAD_DEFAULT`.

## Permissões
```xml
INTERNET, ACCESS_NETWORK_STATE, POST_NOTIFICATIONS, ACCESS_WIFI_STATE,
FOREGROUND_SERVICE, FOREGROUND_SERVICE_MEDIA_PLAYBACK,
WRITE_EXTERNAL_STORAGE (max 32), READ_EXTERNAL_STORAGE (max 32),
READ_MEDIA_IMAGES/VIDEO/AUDIO
```
Provider: `androidx.core.content.FileProvider` com `xml/file_paths.xml` (`files-path downloads/`, `external-path Download/`).

## API e storage
- `SharedPreferences gptwork`: `provider`, `dark`, `homepage`, `dns`, `popups`, `desktop`, `domStorage`, `indexdb`, `cookies`, `redirect`, `oauth`, `safeBrowsing`, `httpsOnly`, `blockMixed`, `dnt`, `incognito`, `bookmarks` (Set), `bookmarklets`
- Files: `filesDir/downloads/` + `/sdcard/Download/` (via DownloadManager)
- WebView: `CookieManager`, `WebStorage`

## Troubleshooting
- **Build falha `setAppCacheEnabled`:** removido (deprecated SDK 35)
- **TextView(this) em card:** usar `act` (context da Activity) não `this` (LinearLayout)
- **onTaskRemoved em Activity:** só Service tem; removido
- **flags shadowing em Service:** `Intent(...).apply { flags = ... }` sombreia param `flags` -> usar `addFlags()`
- **DownloadManager pause:** não suporta pausa nativa, avisa usuário
- **Background para:** usar `applicationContext` WebView + holder + `moveTaskToBack`

## Descontinuação
Este projeto foi descontinuado em 26/08/2026. Não haverá novos builds. Último APK estável: `GPT-WORK-v2.5-BG-FIX.apk` (6.0 MB). Para continuar, faça fork.

## Licença
MIT - veja [LICENSE](LICENSE). Copyright (c) 2026 zerolimites0001-web
