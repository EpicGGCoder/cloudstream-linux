# CloudStream Linux

<p align="center">
  <strong>A native Linux desktop port of CloudStream — the extension-based media player.</strong>
  <br>
  Runs real <code>.cs3</code> plugins unmodified. Produces a portable <code>.AppImage</code>.
</p>

<p align="center">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.4.0-7F52FF">
  <img alt="Compose Multiplatform" src="https://img.shields.io/badge/Compose%20MP-1.12.0-4285F4">
  <img alt="License" src="https://img.shields.io/badge/License-GPL%20v3-blue">
  <img alt="Platform" src="https://img.shields.io/badge/Platform-Linux%20x64-FCC624">
</p>

---
## ⚠️ Development in Progress

This project is under active development. While the core functionality works (plugin loading, playback, UI, persistence), it is **not yet a stable release**. Expect breaking changes, missing features, and bugs.

### What works
- ✅ Real `.cs3` plugin loading (97% compatibility across 106 tested plugins)
- ✅ Video playback via vlcj (HLS, MP4, MKV)
- ✅ Compose Desktop UI (Home, Search, Detail, Player, Library, Plugins, Settings)
- ✅ Bookmarks, favorites, resume watching, history
- ✅ Cloudflare bypass via Chrome DevTools Protocol
- ✅ OpenSubtitles online subtitle search
- ✅ AniSkip skip intro/outro API
- ✅ Download manager (HLS + direct files)
- ✅ Backup/restore

### What's in progress
- 🚧 AniSkip integration into player UI (API works, needs "Skip Intro" button)
- 🚧 Download manager UI (backend works, needs download buttons on detail page)
- 🚧 Backup/restore UI (backend works, needs export/import buttons in settings)
- 🚧 `:shared` module (vendored but not compiled — compose.resources issues)
- 🚧 Sync providers (MAL/AniList/Trakt) — OAuth2 flows not built yet

### What's not planned
- ❌ Chromecast (Google-only, replaced with Fcast concept)
- ❌ TV 10-foot UI (desktop/laptop only)
- ❌ PiP (Android-specific)

### Build from source
No prebuilt binaries yet. Build from source:
```bash
git clone https://github.com/EpicGGCoder/cloudstream-linux.git
cd cloudstream-linux
./scripts/fetch-dex-tools.sh
JAVA_HOME=/path/to/jdk21 ./gradlew :app:packageDistributionForCurrentOS
```

See [BUILD.md](BUILD.md) for detailed instructions.


---

## What is this?

CloudStream is an Android app that lets you stream movies, TV shows, anime, and more from community-maintained plugin repositories. **This project ports it to Linux desktop** — not as a website, but as a **real native application** that runs the exact same `.cs3` plugins as the Android version, without modification.

### How does it work?

```
.cs3 plugin (Dalvik DEX bytecode)
  ↓ extract classes.dex
  ↓ dex2jar (DEX → JVM bytecode conversion)
  ↓ URLClassLoader
  ↓ reflection → Plugin.load(Context)
  ↓ registerMainAPI(MyProvider())
  ↓
Provider is live — search, load, play
```

The Android compatibility layer (28 stub files covering `Context`, `SharedPreferences`, `AppCompatActivity`, `WebView`, `Handler`, `Log`, `Base64`, etc.) lets plugin bytecode link and run on the JVM exactly as it does on Android.

### Key technologies

| Component | Technology | Purpose |
|---|---|---|
| UI framework | Compose Multiplatform | Native desktop UI (same as CS3's upstream migration) |
| Plugin loading | dex2jar + URLClassLoader | Real `.cs3` plugin compatibility |
| Video playback | vlcj (libvlc) | HLS, MP4, MKV, WebVTT subtitles, multi-audio |
| Cloudflare bypass | Chrome DevTools Protocol | Drives system Chrome/Chromium headlessly to solve JS challenges |
| Image loading | Coil 3 | Poster/backdrop rendering |
| HTTP client | OkHttp + NiceHttp | Same as CS3 Android |
| HTML parsing | Jsoup + Ksoup | Scraper support |
| Persistence | JSON files (`~/.cloudstream/`) | Settings, library, watch progress |

---

## Features

### Plugin System
- **Real `.cs3` compatibility** — loads the exact same plugins as the Android app, unmodified
- **dex2jar pipeline** — DEX bytecode → JVM bytecode at runtime
- **Repository manager** — add any CS3 repo URL, browse plugins, install with SHA-256 verification
- **10 prebuilt repos** — CNCVerse, recloudstream/extensions, phisher98, Luna712, and more
- **Auto-load on startup** — previously installed plugins load automatically
- **Verified compatibility** — 103/106 plugins load successfully across 16 different repos

### Video Player
- **vlcj (libvlc)** — same engine as VLC
- **HLS (.m3u8)** — adaptive streaming with quality selection
- **Direct video** — MP4, MKV, WebM, AVI
- **Subtitles** — provider-supplied + OpenSubtitles online search
- **Source picker** — switch between available streams
- **Subtitle picker** — multiple languages
- **Watch progress** — auto-saves resume position
- **Skip timestamps** — AniSkip API for anime intro/outro

### UI
- **Home feed** — hero carousel + horizontal poster rows from all providers
- **Search** — debounced, cross-provider, grouped by provider
- **Detail page** — backdrop, logo, plot, cast, episodes (grouped by season), recommendations
- **Library** — Continue Watching, Favorites, Bookmarks, Recently Viewed
- **Settings** — General, Player, UI, Providers, Updates
- **Plugins** — repo management, install/uninstall, plugin details

### Cloudflare Bypass
- Uses Chrome DevTools Protocol (CDP) to drive the system's Chrome/Chromium
- No bundled browser — uses whatever is already installed
- Caches solved cookies for 30 minutes
- Transparent to plugins — OkHttp interceptor handles it automatically

### Persistence
- **Bookmarks** — save titles for later
- **Favorites** — mark favorites
- **Resume watching** — auto-tracks playback position
- **History** — recently viewed titles
- **Settings** — theme, preferred media types, player config, etc.
- **Backup/restore** — export/import all data to JSON

### Download Manager
- **HLS download** — fetches all `.ts` segments and merges into a single `.ts` file
- **Direct file download** — for MP4/MKV
- **Progress tracking** — real-time download progress

---

## Build & Install

### Prerequisites

```bash
# JDK 17+ (developed on 21)
sudo apt install openjdk-21-jdk

# VLC (for libvlc — the player engine)
sudo apt install vlc libvlc-dev

# Chromium (for Cloudflare bypass — optional but recommended)
sudo apt install chromium

# appimagetool (for final AppImage packaging)
wget https://github.com/AppImage/AppImageKit/releases/download/continuous/appimagetool-x86_64.AppImage \
  -O ~/.local/bin/appimagetool && chmod +x ~/.local/bin/appimagetool
```

### Build

```bash
git clone https://github.com/EpicGGCoder/cloudstream-linux.git
cd cloudstream-linux

# 1. Bundle dex-tools (one-time)
./scripts/fetch-dex-tools.sh

# 2. Build the native distribution
JAVA_HOME=/path/to/jdk21 ./gradlew :app:packageDistributionForCurrentOS

# 3. Wrap into portable AppImage
JAVA_HOME=/path/to/jdk21 ./gradlew :app:wrapAppImage

# Result: build/compose/binaries/CloudStream-x86_64.AppImage
```

### Run from source (no packaging)

```bash
JAVA_HOME=/path/to/jdk21 ./gradlew :app:run
```

### Run tests

```bash
# Test a single plugin end-to-end
JAVA_HOME=/path/to/jdk21 ./gradlew :app:pluginTest

# Bulk test all CNCVerse plugins (41)
JAVA_HOME=/path/to/jdk21 ./gradlew :app:bulkPluginTest --args="test-plugins"

# Bulk test plugins from 15 repos (65)
JAVA_HOME=/path/to/jdk21 ./gradlew :app:bulkPluginTest --args="test-plugins-multi"
```

### Install

```bash
chmod +x build/compose/binaries/CloudStream-x86_64.AppImage
./build/compose/binaries/CloudStream-x86_64.AppImage

# Or system-wide:
sudo install -m 0755 build/compose/binaries/CloudStream-x86_64.AppImage /opt/cloudstream/
```

---

## Usage

1. **Install plugins** — go to the **Plugins** tab, click "Use" on a prebuilt repo, then "Fetch" and "Install" individual plugins
2. **Browse** — the Home tab shows content from all loaded providers
3. **Search** — type in the search bar, results appear from all providers in parallel
4. **Watch** — click any title → detail page → Play → vlcj plays the stream
5. **Bookmark/Favorite** — save titles from the detail page, they appear in Library
6. **Continue Watching** — playback position auto-saves, resume from Library

### Optional: OpenSubtitles API key

```bash
export OS_API_KEY="your-key-from-opensubtitles.com/consumers"
```

---

## Verified Plugin Compatibility

Tested **106 plugins** from **16 different repositories**:

| Metric | Result |
|---|---|
| Plugins loaded (dex2jar + URLClassLoader) | **103/106 (97%)** |
| Providers registered | **101/106 (95%)** |
| getMainPage() returns real data | **70/106 (66%)** |
| search() returns real data | **82/106 (77%)** |

The remaining ~5% failures are:
- Cloudflare-protected sites needing CDP (fixed if Chrome is installed)
- Network errors (site down/timeout)
- A few plugins needing specific Android UI classes

See [PLUGIN_TESTS.md](PLUGIN_TESTS.md) for full test output.

---

## Project Structure

```
cloudstream-linux/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/libs.versions.toml          # Dependency versions
├── library/                           # Vendored CS3 :library (157 Kotlin files)
│   └── src/
│       ├── commonMain/                # MainAPI, ExtractorApi, all data models
│       ├── jvmMain/                   # JVM stubs (28 Android compat files)
│       └── jvmCommonMain/            # Shared JVM code
├── shared/                            # Vendored CS3 :shared (59 Kotlin files)
│   └── src/commonMain/                # Theme, Mihon settings widgets, MVI
├── app/                               # Desktop application
│   ├── build.gradle.kts              # Compose Desktop + jpackage + AppImage
│   ├── src/jvmMain/kotlin/com/lagradost/cloudstream4/
│   │   ├── Main.kt                   # App entry point, theme, navigation
│   │   ├── data/
│   │   │   ├── AppSettings.kt        # Settings persistence
│   │   │   ├── DataStoreHelper.kt    # Bookmarks/favorites/resume/history
│   │   │   └── BackupManager.kt      # Export/import
│   │   ├── network/
│   │   │   ├── CdpCloudflareSolver.kt # Cloudflare bypass via Chrome CDP
│   │   │   ├── WebViewResolverJvm.kt  # WebView resolver bridge
│   │   │   └── AniSkipApi.kt          # Skip intro/outro timestamps
│   │   ├── player/
│   │   │   └── VlcjPlayer.kt          # libvlc bindings
│   │   ├── plugin/
│   │   │   ├── PluginLoader.kt        # .cs3 → dex2jar → URLClassLoader → load()
│   │   │   └── PluginManager.kt       # Install/uninstall/persistence
│   │   ├── repo/
│   │   │   └── RepositoryManager.kt   # Repo fetch + plugin download
│   │   ├── subtitles/
│   │   │   └── OpenSubtitlesApi.kt    # Online subtitle search
│   │   ├── download/
│   │   │   └── DownloadManager.kt     # HLS + direct file downloads
│   │   ├── test/
│   │   │   ├── PluginLoaderTest.kt    # Single-plugin E2E test
│   │   │   └── BulkPluginTest.kt      # Bulk compatibility matrix
│   │   └── ui/
│   │       ├── CommonUi.kt            # PosterCard, skeletons, shared widgets
│   │       ├── HomeScreen.kt          # Hero carousel + provider rows
│   │       ├── SearchScreen.kt        # Debounced cross-provider search
│   │       ├── DetailScreen.kt        # Hero + episodes + cast + bookmark
│   │       ├── PlayerScreen.kt        # vlcj + controls + source/sub pickers
│   │       ├── LibraryScreen.kt       # Continue watching + favorites + bookmarks
│   │       ├── PluginsScreen.kt       # Repo management + install/uninstall
│   │       └── SettingsScreen.kt      # Editable settings
│   └── resources/dex-tools/           # Bundled dex2jar (fetched by script)
├── scripts/
│   ├── fetch-dex-tools.sh             # Download dex2jar
│   ├── bulk-download-cncverse.sh      # Download all CNCVerse plugins
│   └── bulk-download-multi-repo.py    # Download from 15 repos
├── icons/
│   └── icon-linux.png
├── BUILD.md                           # Detailed build instructions
├── FEATURE_MAP.md                     # 1,812-line feature inventory
└── PLUGIN_TESTS.md                    # Verified plugin test results
```

---

## How Plugin Loading Works

### The Problem

CloudStream plugins (`.cs3` files) are renamed APKs containing Dalvik DEX bytecode — the bytecode format used by Android. The JVM cannot execute DEX bytecode directly.

### The Solution

```
┌─────────────────────────────────────────────────────────────────┐
│  .cs3 file (ZIP containing classes.dex + manifest.json)        │
│                         ↓                                       │
│  1. Extract classes.dex                                         │
│  2. dex2jar converts DEX → JVM bytecode (.jar)                  │
│  3. URLClassLoader loads the .jar                               │
│  4. Reflection: loadClass(manifest.pluginClassName)             │
│  5. Verify: extends BasePlugin                                  │
│  6. Instantiate: getDeclaredConstructor().newInstance()        │
│  7. Set filename = absolute path of .cs3                        │
│  8. Invoke load(Context) reflectively                           │
│  9. Plugin calls registerMainAPI(MyProvider())                  │
│ 10. Provider is now in APIHolder.allProviders                   │
│                         ↓                                       │
│  UI calls provider.search() / load() / loadLinks() / etc.       │
└─────────────────────────────────────────────────────────────────┘
```

### Android Compatibility Layer

Plugins compiled against Android's `:app` module expect Android APIs to be available. We provide 28 JVM stub files in the same packages:

| Stub | Package | Used by |
|---|---|---|
| `Context` | `android.content` | SharedPreferences, resources, file dirs |
| `Intent` | `android.content` | Activity launching |
| `SharedPreferences` | `android.content` | Key-value persistence |
| `AppCompatActivity` | `androidx.appcompat.app` | Activity base class |
| `FragmentManager` | `androidx.fragment.app` | Fragment management |
| `Uri` | `android.net` | URL parsing |
| `WebView` | `android.webkit` | Cloudflare solving (CDP-backed) |
| `Handler` | `android.os` | Thread posting |
| `Log` | `android.util` | Logging (delegates to SLF4J) |
| `Base64` | `android.util` | Encoding |
| `CommonActivity` | `com.lagradost.cloudstream3` | CS3 app-level activity |
| `CloudflareKiller` | `com.lagradost.cloudstream3.network` | CF bypass |
| ... | ... | 16 more stubs |

---

## Cloudflare Bypass

Many streaming sites use Cloudflare protection (JS challenges). The CDP solver:

1. Detects 403/503 responses with `server: cloudflare` header
2. Launches Chrome/Chromium headless with `--remote-debugging-port`
3. Connects via WebSocket to Chrome's DevTools Protocol
4. Navigates to the protected URL
5. Waits for the `cf_clearance` cookie (up to 30s)
6. Extracts all cookies via `Network.getAllCookies`
7. Retries the original request with the solved cookies
8. Caches cookies for 30 minutes

No bundled browser — uses whatever Chrome/Chromium is installed on the system.

---

## Credits

- **[CloudStream](https://github.com/recloudstream/cloudstream)** — the original Android app by reCloudStream. This project vendors the `:library` and `:shared` modules under GPL v3.
- **[dex2jar](https://github.com/pxb1988/dex2jar)** — DEX to JVM bytecode converter
- **[vlcj](https://github.com/caprica/vlcj)** — Java bindings for VLC
- **[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform)** — UI framework by JetBrains

---

## License

GNU General Public License v3.0 — same as upstream CloudStream.

This project is for educational purposes. The plugin system is open — what you put in it is your responsibility. The maintainers do not endorse or distribute copyrighted content.
