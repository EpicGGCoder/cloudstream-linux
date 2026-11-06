# CloudStream Linux Port — Architecture

> Companion to `PLAN.md`. This document maps each CloudStream subsystem to its port implementation, with exact API contract references.

## CS3 Reference (cloned)

Source: `/reference/cloudstream/` (shallow clone of `recloudstream/cloudstream`).
- App version: `4.8.0` (versionCode 68)
- Kotlin: `2.4.0`, Compose Multiplatform: `1.12.0`, JVM target: 11, JDK toolchain: 17.
- Modules: `:app` (Android), `:shared` (KMP compose UI), `:library` (KMP plugin primitives), `:desktopApp` (JVM stub), `:docs`.
- `:desktopApp` is a 62-line Hello World — upstream desktop port has not started.

---

## 1. Plugin / Provider Contract

### CS3 contract (`library/src/commonMain/kotlin/com/lagradost/cloudstream3/MainAPI.kt`, 2860 lines)

`abstract class MainAPI` — abstract base class every provider extends.

**Constructor properties**:
```kotlin
open var name: String = "NONE"
open var mainUrl: String = "NONE"
open var lang: String = "en"                          // BCP-47 IETF
open var canBeOverridden: Boolean = true
open var sequentialMainPage: Boolean = false
open var sequentialMainPageDelay: Long = 0L
open var sequentialMainPageScrollDelay: Long = 0L
open val instantLinkLoading: Boolean = false
open val hasChromecastSupport: Boolean = true
open val hasDownloadSupport: Boolean = true
open val usesWebView: Boolean = false
open val hasMainPage: Boolean = false
open val hasQuickSearch: Boolean = false
open val loadLinksTimeoutMs: Long? = null
open val getMainPageTimeoutMs: Long? = null
open val searchTimeoutMs: Long? = null
open val quickSearchTimeoutMs: Long? = null
open val loadTimeoutMs: Long? = null
open val supportedTypes: Set<TvType> = setOf(Movie, TvSeries, Cartoon, Anime, OVA)
open val providerType: ProviderType = DirectProvider
open val mainPage: List<MainPageData> = listOf(MainPageData("", "", false))
```

**Abstract / open methods**:
```kotlin
open suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse?
open suspend fun search(query: String, page: Int): SearchResponseList?
open suspend fun search(query: String): List<SearchResponse>?              // deprecated
open suspend fun quickSearch(query: String): List<SearchResponse>?
open suspend fun load(url: String): LoadResponse?
open suspend fun extractorVerifierJob(extractorData: String?)
open suspend fun loadLinks(
    data: String,
    isCasting: Boolean,
    subtitleCallback: (SubtitleFile) -> Unit,
    callback: (ExtractorLink) -> Unit,
): Boolean
open fun getVideoInterceptor(extractorLink: ExtractorLink): Interceptor?
open suspend fun getLoadUrl(name: SyncIdName, id: String): String?
```

**TvType enum**: `Movie, AnimeMovie, TvSeries, Cartoon, Anime, OVA, Torrent, Documentary, AsianDrama, Live, NSFW, Others, Music, AudioBook, CustomMedia, Audio, Podcast, Video`.

### Port implementation (`src/lib/plugins/`)

TypeScript mirror of `MainAPI`. Identical method names, identical data shapes, identical builder functions. See `2-b` step deliverable for full source.

```typescript
// src/lib/plugins/types.ts
export abstract class MainAPI {
  name = "NONE";
  mainUrl = "NONE";
  lang = "en";
  hasMainPage = false;
  hasQuickSearch = false;
  supportedTypes: TvType[] = [TvType.Movie, TvType.TvSeries];
  providerType: ProviderType = ProviderType.DirectProvider;
  abstract search(query: string, page: number): Promise<SearchResponseList | null>;
  abstract load(url: string): Promise<LoadResponse | null>;
  abstract loadLinks(
    data: string,
    isCasting: boolean,
    subtitleCallback: (s: SubtitleFile) => void,
    callback: (l: ExtractorLink) => void,
  ): Promise<boolean>;
  async getMainPage(page: number, request: MainPageRequest): Promise<HomePageResponse | null> { return null; }
  async quickSearch(query: string): Promise<SearchResponse[] | null> { return null; }
}
```

All CS3 data classes (`SearchResponse`, `LoadResponse`, `Episode`, `HomePageList`, `ExtractorLink`, `SubtitleFile`, `MainPageRequest`, `MainPageData`) are ported 1:1.

---

## 2. Plugin Loading

### CS3 (`app/src/main/java/com/lagradost/cloudstream3/plugins/PluginManager.kt`)
- `.cs3` files = APK/JAR with DEX bytecode + `manifest.json`.
- `PathClassLoader` (Android-only) loads the DEX.
- `BasePlugin` registers `MainAPI` providers via `registerMainAPI()`.

### Port (`src/lib/plugins/registry.ts`)
- **TypeScript plugins**: dynamic `import()` of `.ts`/`.js` files from `plugins/` dir.
- **Kotlin plugins** (Phase 3): JSON-RPC calls to JVM sidecar.
- Registry abstracts both behind a unified `Provider` interface — UI doesn't care which tier.

```typescript
// registry pseudocode
interface Provider {
  meta: ProviderMeta;          // name, mainUrl, lang, supportedTypes, hasMainPage, tier
  search(q: string, page: number): Promise<SearchResponseList>;
  load(url: string): Promise<LoadResponse>;
  loadLinks(data: string, isCasting: boolean): Promise<{ links: ExtractorLink[]; subtitles: SubtitleFile[] }>;
  getMainPage?(page: number, request: MainPageRequest): Promise<HomePageResponse>;
}
```

---

## 3. Repository Mechanism

### CS3 (`app/.../plugins/RepositoryManager.kt`)
- Repo URL → fetch JSON → list of `SitePlugin` (url, status, version, name, iconUrl, fileHash, tvTypes, language).
- `downloadPluginToFile` → SHA-256 verify → store at `filesDir/Extensions/`.
- Stored repos in `REPOSITORIES_KEY` SharedPreferences.

### Port (`src/lib/plugins/repos.ts` + Prisma `PluginRepo` model)
- Identical schema: a repo URL → JSON manifest → plugin descriptors.
- Plugin download → SHA-256 verify → store in `~/.cloudstream/plugins/` (or platform-appropriate dir).
- Repos persisted in SQLite (`PluginRepo` table).
- **TS plugin repos**: GitHub raw URLs to `.ts` plugin source.
- **Kotlin plugin repos** (Phase 3): standard CS3 repo URLs — fetched same way, downloaded `.cs3` files passed to JVM sidecar.

---

## 4. Data Persistence

### CS3 (`app/.../utils/DataStoreHelper.kt`)
SharedPreferences (JSON-encoded values), keyed:
- `video_pos_dur` — resume position `{url: {position, duration}}`
- `result_watch_state` — `WatchType` per id (0=none, 1=watching, 2=completed, 3=dropped)
- `result_watch_state_data` — `BookmarkedData[]`
- `result_subscribed_state_data` — `SubscribedData[]`
- `result_favorites_state_data` — `FavoritesData[]`
- `result_resume_watching_2` — `ResumeWatchingResult[]`
- `PLUGINS_KEY` / `PLUGINS_KEY_LOCAL` / `REPOSITORIES_KEY`
- Account prefix: `"${currentAccount}/"` on every key

### Port (Prisma schema, see `2-a`)
SQLite tables mirror the same logical entities with the same field names. Key constants preserved for backup-format compatibility:
- `Media` (id, apiName, url, type, name, posterUrl, year, plot, score, tags, ...)
- `Episode` (id, mediaId, season, episode, name, data, posterUrl, ...)
- `LibraryItem` (mediaId, kind: bookmarked|favorite|subscribed|watching|completed|dropped, addedAt, ...)
- `WatchProgress` (mediaId, episodeId, position, duration, updatedAt)
- `Plugin` (internalName, url, version, filePath, status, isOnline)
- `PluginRepo` (url, name, description, addedAt)
- `Account` (keyIndex, name, customImage, defaultImageIndex, lockPin)
- `SearchHistory` (query, searchedAt)

`BackupUtils.BackupFile` JSON format is portable — backup files from Android CS3 can be imported.

---

## 5. UI Screens

### CS3 → Port mapping

| CS3 Screen | CS3 Source | Port Route | Port Source |
|---|---|---|---|
| Home | `ui/home/HomeFragment.kt` | `/` (root) | `src/app/page.tsx` |
| Search | `ui/search/SearchFragment.kt` | `/` (modal overlay) | `src/components/search/` |
| Result (detail) | `ui/result/ResultFragmentPhone.kt` | `/` (modal overlay) | `src/components/detail/` |
| Player | `ui/player/FullScreenPlayer.kt` | `/` (full-screen overlay) | `src/components/player/` |
| Library | `ui/library/LibraryFragment.kt` | `/` (tab in home) | `src/components/library/` |
| Downloads | `ui/download/DownloadFragment.kt` | (Phase 2) | — |
| Extensions | `ui/settings/extensions/ExtensionsFragment2.kt` | `/` (settings modal) | `src/components/settings/extensions/` |
| Settings | `ui/settings/SettingsFragment2.kt` | `/` (settings modal) | `src/components/settings/` |
| Setup | `ui/setup/*` | `/` (first-run wizard) | `src/components/setup/` |

Note: Next.js App Router — the only user-visible route is `/`. All "screens" are panels/modals/tabs within `/`. This is intentional for the single-window desktop feel.

---

## 6. Player Pipeline

### CS3 flow
1. User taps episode → `ResultViewModel2.load(url, apiName, ...)` calls `APIRepository(api).load(url)` → `LoadResponse`.
2. Episode list shown. Tap → `RepoLinkGenerator` calls `APIRepository(api).loadLinks(data, isCasting, subCb, linkCb)`.
3. `ExtractorLink[]` collected, deduplicated, sorted by `Qualities`.
4. `CS3IPlayer` wraps `ExoPlayer`, builds `MediaItem` with DRM config + subtitle configs.

### Port flow
1. User taps episode → `/api/detail/load` (POST `{apiName, url}`) → calls registered provider's `load(url)` → `LoadResponse`.
2. Episode list rendered.
3. Tap episode → `/api/player/links` (POST `{apiName, data}`) → calls `loadLinks` → `{ links: ExtractorLink[], subtitles: SubtitleFile[] }`.
4. 20-minute cache (`apiName:id` key) — same as CS3's `RepoLinkGenerator.cache`.
5. Player UI uses **`hls.js`** for `.m3u8`, native `<video>` for `.mp4`/`.webm`.
6. Subtitles: WebVTT trackside rendering (browser native + custom styling).
7. Quality picker / source picker / subtitle picker dialogs.
8. DRM (Widevine): not feasible in browser without EME-licensed CDM; skip for now (CS3's DRM support is also limited on JVM).

---

## 7. Subtitles

### CS3 sources
1. Provider-supplied (`subtitleCallback` in `loadLinks`)
2. Online APIs: OpenSubtitles, Addic7ed, SubDL, SubSource — searched by query.
3. Embedded in container (MKV).

### Port
1. Provider-supplied: surfaced via `loadLinks` API response → `<track kind="subtitles">`.
2. Online APIs: `/api/subtitles/search?q=...` proxying to OpenSubtitles (free API). Caching 120min.
3. Embedded: not extractable in browser; rely on provider-supplied .vtt.
4. `SubtitleHelper` language code tables ported to `src/lib/subtitles/languages.ts`.

---

## 8. Sync Providers (MAL, AniList, Kitsu, Simkl, Trakt)

### CS3 (`library/.../syncproviders/`)
- `AuthAPI` → `SyncAPI` → concrete providers. OAuth2/PIN/in-app login.
- `LocalList` provider backs the local library tab.

### Port
- Reimplemented in TypeScript (Phase 2). All HTTP/GraphQL clients.
- OAuth2 flows handled via popup window + redirect URI.
- `LocalList` provider backed by Prisma `LibraryItem` table.
- Tokens stored in `Account` table (encrypted at rest with OS keyring in Phase 4 / Tauri).

---

## 9. Meta-providers

### CS3 (`library/.../metaproviders/`)
- `TmdbProvider` — uses hardcoded API key `e6333b32409e02a4a6eba6fb7ff866bb` (we'll use the same key — it's public).
- `TraktProvider` — `BuildConfig.TRAKT_CLIENT_ID`.
- `MyDramaListAPI` — `BuildConfig.MDL_API_KEY`.
- `CrossTmdbProvider` — aggregator that searches all matching providers in parallel.
- `SyncRedirector` — translates MAL/AniList URLs to provider URLs.

### Port (Phase 1)
- TMDB client (`src/lib/tmdb/`) — direct REST, same API key.
- `CrossTmdbProvider` aggregator — Phase 2 (requires plugin system to be populated).
- Trakt/MDL — Phase 2.

---

## 10. Network / Scraping Utilities

### CS3 (`library/.../utils/`)
- `app.get/post` — wraps `okhttp3` with Conscrypt, pluggable DNS, 50MB cache.
- `Jsoup`/`Ksoup` — HTML parsing.
- `M3u8Helper` / `HlsPlaylistParser` — HLS parsing.
- `JsUnpacker` / `JsInterpreter` — JS eval for obfuscated extractors.
- `UnshortenUrl` — link unshortener.

### Port
- `src/lib/http/app.ts` — `app.get/post` wrapper around `fetch` with same API surface (`.text`, `.document` via `cheerio`, `.parsed<T>()`, `.url`, `referer`, `headers`, `cacheTime`).
- `cheerio` for HTML parsing (Ksoup equivalent).
- `m3u8-parser` for HLS.
- `js-beautify` + `quickjs-emscripten` for JS eval.
- `unshorten` regex port.

---

## 11. Cast / Chromecast

### CS3
`com.google.android.gms.cast` — Android/Google-only.

### Port
- **Replaced with Fcast** (open-source casting protocol, supports Linux receivers).
- Already exists in CS3 (`actions/temp/fcast/`).
- Phase 3.

---

## 12. Build / Packaging

### CS3 desktopApp/build.gradle.kts
- `compose.desktop.application` plugin.
- `targetFormats(Dmg, Msi, Deb)` — **no AppImage**.
- `mainClass = "com.lagradost.cloudstream4.MainKt"`.

### Port final packaging (Phase 4)
1. Next.js static export → `out/` directory.
2. Tauri config:
   ```rust
   // src-tauri/tauri.conf.json
   {
     "build": { "frontendDist": "../out" },
     "bundle": {
       "active": true,
       "targets": ["appimage", "deb"],
       "icon": ["icons/icon-linux.png"]
     }
   }
   ```
3. Bundled JVM sidecar: `src-tauri/resources/jvm-sidecar/` (jlink-customized JRE + sidecar.jar).
4. AppImage: `tauri build` produces `CloudStream_x.y.z_amd64.AppImage`.

---

## TL;DR — what's being built HERE vs LATER

| What | Where | When |
|---|---|---|
| Web UI (home/search/detail/player/library/settings) | Here (Next.js) | Phase 1 (current) |
| TypeScript plugin system | Here | Phase 1 |
| TMDB meta-provider | Here | Phase 1 |
| Prisma persistence | Here | Phase 1 |
| Scraper utilities (HTTP, HTML, HLS, unshorten) | Here | Phase 1 |
| Demo provider (Internet Archive) | Here | Phase 1 |
| Tauri wrapper + AppImage skeleton | Your laptop | Phase 2 |
| JVM sidecar for real `.cs3` plugins | Your laptop | Phase 3 |
| Final `.AppImage` build | Your laptop | Phase 4 |

This is the honest, realistic path. Let's build.
