# CloudStream → Linux Desktop Port — Exhaustive Feature Map

> **Master tracking document** for the CloudStream Android → Linux desktop port.
> Companion to `/home/z/my-project/ARCHITECTURE.md`.
>
> Status icons:
> - ✅ **Done** — ported and wired into the desktop app
> - 🚧 **In progress** — partial port, may compile but not feature-complete
> - ⏳ **Pending** — not started, scheduled for Phase 1 / 2 / 3
> - ❌ **Skip** — intentionally not ported (Android-only or out-of-scope)
>
> "Port File" columns reference files under `/home/z/my-project/desktop/`.
> CS3 source paths are relative to `/home/z/my-project/reference/cloudstream/`.

---

## Section 1 — Core Plugin / Provider System

### 1.1 MainAPI.kt — Provider contract
**Source**: `library/src/commonMain/kotlin/com/lagradost/cloudstream3/MainAPI.kt` (2,860 lines)

#### Top-level constants & annotations

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `@Prerelease` annotation | L56-63 | Marks APIs only on prerelease builds | ✅ | `library` vendored |
| `@InternalAPI` annotation | L65-72 | Marks internal APIs not for extensions | ✅ | vendored |
| `@UnsafeSSL` annotation | L74-79 | Bypass SSL cert checks | ✅ | vendored |
| `@SkipSerializationTest` | L82-85 | Jackson→kotlinx migration marker | ✅ | vendored |
| `AllLanguagesName` | L91 | `"universal"` constant for all-languages preference | ✅ | vendored |
| `USER_AGENT` | L93-94 | Default Chrome User-Agent string | ✅ | vendored |
| `ErrorLoadingException` | L96 | RuntimeException subclass | ✅ | vendored |
| `json` (kotlinx Json) | L100-104 | Default Json instance w/ `encodeDefaults`, `ignoreUnknownKeys` | ✅ | vendored |
| `mapper` (Jackson JsonMapper) | L106-107 | Legacy Jackson mapper for backward compat | ✅ | vendored |

#### `APIHolder` object (L109-331)

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `unixTimeMS` / `unixTime` | L110-113 | Current time in ms / sec | ✅ | vendored |
| `allProviders: AtomicList<MainAPI>` | L115 | Thread-safe list of all registered providers | ✅ | `library` + `app/.../plugin/PluginManager.kt:173` |
| `initAll()` | L117-124 | Calls `init()` on every provider | ✅ | vendored |
| `String.capitalize()` | L126-129 | First-letter capitalize | ✅ | vendored |
| `apis` / `apiMap` | L131-132 | Indexed lookup of providers by name | ✅ | vendored |
| `addPluginMapping(plugin)` | L134-139 | Register a new provider's name→index mapping | ✅ | vendored |
| `removePluginMapping(plugin)` | L141-146 | Unregister a provider | ✅ | vendored |
| `initMap(forcedUpdate)` | L148-153 | Rebuild `apiMap` cache | ✅ | vendored |
| `getApiFromNameNull(name)` | L155-165 | Lookup provider by name (null-safe) | ✅ | vendored |
| `getApiFromUrlNull(url)` | L167-172 | Lookup provider by URL prefix | ✅ | vendored |
| `getCaptchaToken(url, key, referer)` | L187-226 | Fetch Google reCAPTCHA v2 token | ⏳ Phase 2 | needs JVM port — used by ~5 providers |
| `trackerCache` | L228 | In-memory AniList cache | ✅ | vendored |
| `getTracker(titles, types, year, lessAccurate)` | L231-285 | Look up MAL/AniList IDs from a title | ✅ | vendored |
| `searchAnilist(title)` | L291-330 | GraphQL POST to `graphql.anilist.co` | ✅ | vendored |

#### Data classes for provider override (L397-422)

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `ProvidersInfoJson` | L397-403 | Stored provider name/url/credentials/status | ✅ | vendored |
| `SettingsJson` | L405-408 | `enableAdult` flag | ✅ | vendored |
| `MainPageData(name, data, horizontalImages)` | L410-414 | Homepage section descriptor | ✅ | vendored |
| `MainPageRequest` | L416-421 | Runtime page request | ✅ | vendored |

#### Builder functions (L423-491)

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `mainPage(url, name, horizontalImages)` | L428-430 | Construct MainPageData | ✅ | vendored |
| `mainPageOf(vararg MainPageData)` | L434-436 | List-of helper | ✅ | vendored |
| `mainPageOf(vararg Pair<String,String>)` | L440-442 | Pair-of-strings helper | ✅ | vendored |
| `newHomePageResponse(name, list, hasNext)` | L444-466 | 4 overloads | ✅ | vendored |
| `newHomePageResponse(list, hasNext)` | L468-476 | 2 overloads | ✅ | vendored |
| `newSearchResponseList(list, hasNext)` | L478-487 | Build `SearchResponseList` | ✅ | vendored |
| `List<SearchResponse>.toNewSearchResponseList(hasNext)` | L489-491 | Extension helper | ✅ | vendored |

#### `abstract class MainAPI` (L494-713)

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `Companion.overrideData` | L496 | HashMap for clone-site overrides | ✅ | vendored |
| `Companion.settingsForProvider` | L497 | `SettingsJson` (e.g. enableAdult) | ✅ | vendored |
| `init()` | L500-504 | Apply override data from settings | ✅ | vendored |
| `overrideWithNewData(data)` | L506-512 | Replace name/url/credentials | ✅ | vendored |
| `name: String` | L515 | Provider display name | ✅ | vendored |
| `mainUrl: String` | L518 | Provider base URL | ✅ | vendored |
| `storedCredentials` | L519 | Provider-specific auth credentials | ✅ | vendored |
| `canBeOverridden` | L520 | Allow clone-site override | ✅ | vendored |
| `sequentialMainPage` | L524 | Fetch homepage sections sequentially | ✅ | vendored |
| `sequentialMainPageDelay` | L528 | Delay between homepage requests (ms) | ✅ | vendored |
| `sequentialMainPageScrollDelay` | L531 | Scroll-delay (ms) | ✅ | vendored |
| `lastHomepageRequest` | L534 | Unixtime of last homepage fetch | ✅ | vendored |
| `lang: String` | L546 | IETF BCP-47 language tag | ✅ | vendored |
| `instantLinkLoading` | L549 | Links stored in `data` string | ✅ | vendored |
| `hasChromecastSupport` | L552 | ❌ on desktop — replaced by Fcast | 🚧 | vendored; ignored by desktop player |
| `hasDownloadSupport` | L555 | All links downloadable | ✅ | vendored |
| `usesWebView` | L558 | Requires WebView (Cloudflare etc.) | 🚧 | `app/.../network/WebViewResolverJvm.kt` (stub) |
| `sourcePlugin: String?` | L561 | Plugin file path | ✅ | vendored |
| `hasMainPage` | L563 | Provider has `getMainPage()` | ✅ | vendored |
| `hasQuickSearch` | L564 | Provider has `quickSearch()` | ✅ | vendored |
| `loadLinksTimeoutMs` | L573 | Timeout hint for `loadLinks` | ✅ | vendored |
| `getMainPageTimeoutMs` | L580 | Timeout hint | ✅ | vendored |
| `searchTimeoutMs` | L587 | Timeout hint | ✅ | vendored |
| `quickSearchTimeoutMs` | L594 | Timeout hint | ✅ | vendored |
| `loadTimeoutMs` | L601 | Timeout hint for `load()` | ✅ | vendored |
| `supportedSyncNames: Set<SyncIdName>` | L616 | Which sync ID types the provider can resolve (Imdb/Mal/AniList/...) | ✅ | vendored |
| `supportedTypes: Set<TvType>` | L618-624 | Movie, TvSeries, Cartoon, Anime, OVA defaults | ✅ | vendored |
| `vpnStatus: VPNStatus` | L626 | None/MightBeNeeded/Torrent | ✅ | vendored |
| `providerType: ProviderType` | L627 | DirectProvider vs MetaProvider | ✅ | vendored |
| `mainPage: List<MainPageData>` | L630 | Section list | ✅ | vendored |
| `getMainPage(page, request)` | L633-638 | Suspends → HomePageResponse? | ✅ | vendored |
| `search(query, page)` | L641-648 | Paginated search | ✅ | vendored |
| `search(query)` | L651-653 | Deprecated single-page search | ✅ | vendored |
| `quickSearch(query)` | L656-658 | Fast suggestion-style search | ✅ | vendored |
| `load(url)` | L665-667 | Open the info page from URL | ✅ | vendored |
| `extractorVerifierJob(extractorData)` | L679-681 | Background polling (e.g. sflix link keepalive) | ✅ | vendored |
| `loadLinks(data, isCasting, subtitleCallback, callback)` | L692-699 | Core: return video links + subs | ✅ | vendored |
| `getVideoInterceptor(extractorLink)` | L702-704 | OkHttp interceptor for this provider's videos | ✅ | vendored |
| `getLoadUrl(name, id)` | L710-712 | Translate sync ID to provider URL | ✅ | vendored |

#### Top-level utility functions (L715-868)

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `base64Decode(string)` | L715-725 | ISO-8859-1 safe decode | ✅ | vendored |
| `base64DecodeArray(string)` | L727-729 | Decode to `ByteArray` | ✅ | vendored |
| `base64Encode(array)` | L731-733 | Encode | ✅ | vendored |
| `MainAPI.fixUrlNull(url)` | L735-740 | Null-safe fixUrl | ✅ | vendored |
| `MainAPI.fixUrl(url)` | L742-762 | Prepend `mainUrl` / `https:` to relative URLs | ✅ | vendored |
| `sortUrls(urls)` | L769-771 | Sort ExtractorLinks by quality desc | ✅ | vendored |
| `splitUrlParameters(url: Url)` | L792-794 | Parse query params | ✅ | vendored |
| `splitUrlParameters(url: String)` | L811-813 | String overload | ✅ | vendored |
| `capitalizeString(str)` | L822-824 | Title-case first char | ✅ | vendored |
| `capitalizeStringNullable(str)` | L831-839 | Null-safe | ✅ | vendored |
| `fixTitle(str)` | L841-846 | Title-case each word | ✅ | vendored |
| `getRhinoContext()` (deprecated) | L852-859 | Mozilla Rhino JS context | ✅ | vendored; replaced by `newJsContext` |
| `imdbUrlToId(url)` | L866-869 | Extract tt-prefix IMDb ID | ✅ | vendored |
| `imdbUrlToIdNullable(url)` | L876-879 | Null-safe | ✅ | vendored |

#### Enums (L887-1154)

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `ProviderType { MetaProvider, DirectProvider }` | L887-890 | Source of metadata | ✅ | vendored |
| `VPNStatus { None, MightBeNeeded, Torrent }` | L893-897 | VPN recommendation | ✅ | vendored |
| `ShowStatus { Completed, Ongoing }` | L900-903 | Show airing status | ✅ | vendored |
| `DubStatus { None(-1), Dubbed(1), Subbed(0) }` | L905-909 | Anime dub/sub | ✅ | vendored |
| `TvType` (18 values) | L1120-1142 | See §13 | ✅ | vendored |
| `AutoDownloadMode { Disable, FilterByLang, All, NsfwOnly }` | L1144-1154 | Plugin auto-download preference | ✅ | vendored |
| `SearchQuality` (16 values) | L1303-1320 | Cam/CamRip/HdCam/Telesync/WorkPrint/Telecine/HQ/HD/HDR/BlueRay/DVD/SD/FourK/UHD/SDR/WebRip | ✅ | vendored |
| `ActorRole { Main, Supporting, Background }` | L1524-1528 | Cast role | ✅ | vendored |
| `SimklSyncServices { Simkl, Imdb, Tmdb, AniList, Mal }` | L2664-2670 | ID-lookup services Simkl supports | ✅ | vendored |
| `TrackerType { MOVIE, TV, TV_SHORT, ONA, OVA, SPECIAL, MUSIC }` | L2838-2846 | AniList format types for tracker matching | ✅ | vendored |

#### `Score` class (L917-1117)

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `Score` private constructor | L919 | Internal `data: Int` ∈ [0, 10^9] | ✅ | vendored |
| `toByte/toInt/toLong/toFloat/toDouble(maxScore)` | L932-942 | Convert to legacy scale | ✅ | vendored |
| `toString(maxScore, decimals, removeTrailingZeros, decimalChar)` | L965-1020 | Human-readable format | ✅ | vendored |
| `toStringNull(minScore, maxScore, decimals, ...)` | L952-961 | Null if below minScore | ✅ | vendored |
| `Score.MAX/MIN/MAX_ZEROS` | L1023-1025 | Constants | ✅ | vendored |
| `Score.fromOld(value)` | L1028-1039 | Legacy [0,10000] → Score | ✅ | vendored |
| `Score.from(value, maxScore)` ×4 overloads (Int/Double/Float/String) | L1042-1079 | Normalized scale constructor | ✅ | vendored |
| `Score.from5/from10/from100` ×4 overloads each | L1082-1115 | Convenience scales | ✅ | vendored |

#### `TvType` extension functions (L1156-2208)

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `TvType.isMovieType()` | L1159-1169 | True for Movie/AnimeMovie/Live/Torrent/Video | ✅ | vendored |
| `TvType.isAudioType()` | L1174-1183 | True for Audio/AudioBook/Music/Podcast | ✅ | vendored |
| `TvType.isLiveStream()` | L1188-1190 | True for Live | ✅ | vendored |
| `TvType.isAnimeOp()` | L1195-1197 | True for Anime/OVA (skip-op feature) | ✅ | vendored |
| `TvType.isEpisodeBased()` | L2165-2174 | True for Anime/AsianDrama/Cartoon/TvSeries | ✅ | vendored |
| `TvType.getFolderPrefix()` | L2187-2208 | Folder name for downloads (Movies/Anime/...) | ✅ | vendored |

#### Subtitle & Audio data classes (L1199-1264)

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `SubtitleFile` (private ctor + `langTag` derived) | L1205-1221 | Subtitle URL + language | ✅ | vendored |
| `newSubtitleFile(lang, url, initializer)` | L1224-1236 | Builder | ✅ | vendored |
| `AudioFile(url, headers)` | L1247-1250 | External audio track | ✅ | vendored |
| `newAudioFile(url, initializer)` | L1257-1264 | Builder | ✅ | vendored |

#### HomePage & Search response types (L1266-1497)

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `HomePageResponse(items, hasNext)` | L1270-1275 | Homepage response | ✅ | vendored |
| `HomePageList(name, list, isHorizontalImages)` | L1282-1286 | One homepage section | ✅ | vendored |
| `SearchResponseList(items, hasNext)` | L1292-1297 | Paginated search | ✅ | vendored |
| `getQualityFromString(string)` | L1325-1368 | Map "1080p"/"4K"/etc → SearchQuality | ✅ | vendored |
| `MainAPI.updateUrl(url)` | L1384-1403 | Replace scheme+host+port from mainUrl | ✅ | vendored |

#### `SearchResponse` interface & subclasses (L1406-1772)

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `interface SearchResponse` | L1406-1416 | name/url/apiName/type/posterUrl/posterHeaders/id/quality/score | ✅ | vendored |
| `MainAPI.newTorrentSearchResponse(name, url, type, fix, initializer)` | L1418-1436 | Torrent builder | ✅ | vendored |
| `MainAPI.newMovieSearchResponse(...)` | L1438-1450 | Movie builder | ✅ | vendored |
| `MainAPI.newLiveSearchResponse(...)` | L1452-1468 | Live stream builder | ✅ | vendored |
| `MainAPI.newTvSeriesSearchResponse(...)` | L1470-1482 | TV series builder | ✅ | vendored |
| `MainAPI.newAnimeSearchResponse(...)` | L1484-1496 | Anime builder | ✅ | vendored |
| `SearchResponse.addQuality(quality)` | L1501-1503 | Mutator extension | ✅ | vendored |
| `SearchResponse.addPoster(url, headers)` | L1509-1512 | Mutator extension | ✅ | vendored |
| `LoadResponse.addPoster(url, headers)` | L1518-1521 | Mutator extension | ✅ | vendored |
| `AnimeSearchResponse` data class | L1555-1574 | year/dubStatus/otherName/episodes map | ✅ | vendored |
| `AnimeSearchResponse.addDubStatus(...)` (4 overloads) | L1576-1616 | Add dub/sub status & episode count | ✅ | vendored |
| `AnimeSearchResponse.addDub/addSub(episodes)` | L1587-1595 | Shortcuts | ✅ | vendored |
| `TorrentSearchResponse` | L1621-1650 | Torrent search result | ✅ | vendored |
| `MovieSearchResponse` | L1655-1686 | Movie search result (year) | ✅ | vendored |
| `LiveSearchResponse` | L1691-1722 | Live stream search result (lang) | ✅ | vendored |
| `TvSeriesSearchResponse` | L1727-1772 | TV series (year, episodes count) | ✅ | vendored |

#### `TrailerData` & `LoadResponse` (L1779-2095)

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `TrailerData(extractorUrl, referer, raw, headers)` | L1779-1786 | Trailer info | ✅ | vendored |
| `interface LoadResponse` (24 props) | L1814-1839 | Full media metadata | ✅ | vendored |
| `LoadResponse.rating` (deprecated `rating` shim) | L1840-1851 | Backward-compat with old `Int rating` | ✅ | vendored |
| `LoadResponse.isMovie()` | L1875-1877 | Type check | ✅ | vendored |
| `LoadResponse.addActors(actors)` (3 overloads) | L1879-1892 | Add cast list (names / pairs / role pairs) | ✅ | vendored |
| `LoadResponse.addActors(actors: List<Actor>)` | L1908-1911 | 4th overload | ✅ | vendored |
| `LoadResponse.addSimklId(database, id)` | L1897-1906 | Internal Simkl ID helper | ✅ | vendored |
| `LoadResponse.getMalId/getKitsuId/getAniListId()` | L1913-1922 | Sync ID readers | ✅ | vendored |
| `LoadResponse.getImdbId/getTMDbId()` | L1924-1934 | Simkl-implied ID readers | ✅ | vendored |
| `LoadResponse.addMalId/addKitsuId/addAniListId/addSimklId(id)` | L1936-1952 | Sync ID writers | ✅ | vendored |
| `LoadResponse.addImdbUrl/addImdbId/addTMDbId/addTraktId` | L1954-2062 | More sync ID writers (Trakt/Kitsu stubs TODO upstream) | ✅ | vendored |
| `LoadResponse.addTrailer(...)` (3 overloads) | L1959-2042 | Add trailers | ✅ | vendored |
| `LoadResponse.addScore(score, maxValue)` (2 overloads) | L2064-2070 | Set score | ✅ | vendored |
| `LoadResponse.addRating(value)` (deprecated) | L2077-2089 | Legacy int rating | ✅ | vendored |
| `LoadResponse.addDuration(input)` | L2091-2093 | Parse "1h 30min" string | ✅ | vendored |
| `getDurationFromString(input)` | L2097-2139 | Regex parse duration | ✅ | vendored |
| `LoadResponse?.isEpisodeBased()` | L2144-2147 | True if EpisodeResponse & episode type | ✅ | vendored |
| `LoadResponse?.isAnimeBased()` | L2153-2156 | True if Anime/OVA type | ✅ | vendored |

#### Episode & season (L2210-2652)

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `NextAiring(episode, unixTime, season?)` | L2215-2219 | Next-episode metadata | ✅ | vendored |
| `SeasonData(season, name?, displaySeason?)` | L2227-2231 | Season naming | ✅ | vendored |
| `interface EpisodeResponse` | L2234-2248 | showStatus/nextAiring/seasonNames + getLatestEpisodes/getTotalEpisodeIndex | ✅ | vendored |
| `EpisodeResponse.addSeasonNames(names)` (2 overloads) | L2250-2263 | Set season display names | ✅ | vendored |
| `TorrentLoadResponse` | L2268-2293 | magnet/torrent fields | ✅ | vendored |
| `MainAPI.newTorrentLoadResponse(name, url, magnet, torrent, initializer)` | L2295-2315 | Builder | ✅ | vendored |
| `AnimeLoadResponse` | L2320-2380 | engName/japName/episodes map by DubStatus | ✅ | vendored |
| `AnimeLoadResponse.addEpisodes(status, episodes)` | L2385-2388 | Append episodes | ✅ | vendored |
| `MainAPI.newAnimeLoadResponse(name, url, type, comingSoonIfNone, initializer)` | L2390-2409 | Builder | ✅ | vendored |
| `LiveStreamLoadResponse` | L2414-2440 | dataUrl field | ✅ | vendored |
| `MainAPI.newLiveStreamLoadResponse(name, url, dataUrl, initializer)` | L2442-2458 | Builder | ✅ | vendored |
| `MovieLoadResponse` | L2463-2489 | dataUrl field | ✅ | vendored |
| `MainAPI.newMovieLoadResponse(...)` (2 overloads, generic + String) | L2491-2539 | Builders | ✅ | vendored |
| `Episode(data, name?, season?, episode?, posterUrl?, score?, description?, date?, runTime?)` | L2552-2575 | Episode info | ✅ | vendored |
| `Episode.addDate(...)` (4 overloads: String/LocalDate/Instant/java.util.Date) | L2578-2622 | Set air date | ✅ | vendored |
| `MainAPI.newEpisode(url, initializer, fix)` (2 overloads) | L2624-2652 | Builders (String + generic) | ✅ | vendored |
| `interface IDownloadableMinimum` | L2654-2658 | url/referer/headers for downloads | ✅ | vendored |

#### `TvSeriesLoadResponse` & trackers (L2675-2860)

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `TvSeriesLoadResponse` | L2675-2726 | episodes: List<Episode> | ✅ | vendored |
| `MainAPI.newTvSeriesLoadResponse(name, url, type, episodes, initializer)` | L2728-2746 | Builder | ✅ | vendored |
| `fetchUrls(text)` | L2748-2755 | Regex extract URLs from text | ✅ | vendored |
| `isUpcoming(dateString)` | L2757-2775 | Check if date is in future | ✅ | vendored |
| `String?.toRatingInt()` (deprecated) | L2781-2782 | Legacy int rating | ✅ | vendored |
| `Tracker(malId, kitsuId, aniId, image, cover)` | L2784-2790 | AniList tracker result | ✅ | vendored |
| `AniSearch` (nested data classes) | L2793-2833 | AniList GraphQL response | ✅ | vendored |

### 1.2 ExtractorApi.kt — Extractor contract
**Source**: `library/src/commonMain/kotlin/com/lagradost/cloudstream3/utils/ExtractorApi.kt` (1,473 lines)

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `PlayListItem(url, durationUs)` | L355-358 | ConcatenatingMediaSource item | ⏳ Phase 3 | not yet needed |
| `Long.toUs()` | L363-365 | Seconds → microseconds | ✅ | vendored |
| `ExtractorLinkPlayList` | L372-413 | PlayList-based ExtractorLink subclass | ⏳ Phase 3 | not yet needed |
| `enum class ExtractorLinkType { VIDEO, M3U8, DASH, TORRENT, MAGNET }` | L417-444 | Link type enum + `getMimeType()` | ✅ | vendored |
| `inferTypeFromUrl(url)` | L446-460 | Auto-detect type from extension | ✅ | vendored |
| `INFER_TYPE: ExtractorLinkType?` | L462 | Sentinel for "auto-detect" | ✅ | vendored |
| `CLEARKEY_DRM_UUID` | L470-471 | ClearKey DRM scheme UUID | 🚧 | vendored; vlcj doesn't support DRM |
| `WIDEVINE_DRM_UUID` | L478-480 | Widevine DRM UUID | 🚧 | vendored; vlcj doesn't support DRM |
| `PLAYREADY_DRM_UUID` | L489-490 | PlayReady DRM UUID | 🚧 | vendored; vlcj doesn't support DRM |
| `newExtractorLink(source, name, url, type?, initializer)` | L503-522 | Builder | ✅ | vendored |
| `newDrmExtractorLink(...)` (3 overloads: java UUID + kotlin UUID) | L529-572 | DRM link builder | 🚧 | vendored; vlcj doesn't support DRM |
| `DrmExtractorLink` open class | L591-688 | DRM link (kid/key/uuid/kty/keyRequestParameters/licenseUrl) | 🚧 | vendored; needs DRM stack |
| `ExtractorLink` open class | L703-844 | source/name/url/referer/quality/headers/extractorData/type/audioTracks | ✅ | vendored |
| `ExtractorLink.isM3u8` / `isDash` | L718-719 | Convenience | ✅ | vendored |
| `ExtractorLink.getVideoSize(timeoutSeconds)` | L728-739 | HEAD request for content-length | ✅ | vendored |
| `ExtractorLink.getAllHeaders()` | L741-749 | Merge headers + referer | ✅ | vendored |
| `enum class Qualities { Unknown(400), P144, P240, P360, P480, P720, P1080, P1440, P2160 }` | L852-883 | Quality buckets with defaultPriority | ✅ | vendored |
| `Qualities.getStringByInt(qual)` / `getStringByIntFull(quality)` | L864-881 | Format "1080p" / "4K" | ✅ | vendored |
| `getQualityFromName(qualityName)` | L885-894 | String → Int | ✅ | vendored |
| `schemaStripRegex` | L850 | Strips `https://` and `www.` for matching | ✅ | vendored |
| `getPacked(string)` / `getAndUnpack(string)` | L896-904 | Detect & unpack Dean Edwards packed JS | ✅ | vendored (uses `JsUnpacker`) |
| `unshortenLinkSafe(url)` | L906-915 | Unshorten adfly/linkup/etc. | ✅ | vendored (uses `ShortLink`) |
| `loadExtractor(url, subtitleCallback, callback)` | L917-928 | Convenience overload | ✅ | vendored |
| `loadExtractor(url, referer?, subtitleCallback, callback)` | L935-986 | **Core**: iterate `extractorApis` and dispatch | ✅ | vendored |
| `extractorApis: AtomicMutableList<ExtractorApi>` | L988-1349 | **~310 registered extractors** (see §12) | ✅ | vendored (compiles on JVM) |
| `getExtractorApiFromName(name)` | L1352-1357 | Lookup | ✅ | vendored |
| `requireReferer(name)` | L1359-1361 | True if referer required | ✅ | vendored |
| `httpsify(url)` | L1363-1365 | Prefix `https:` to `//url` | ✅ | vendored |
| `getPostForm(requestUrl, html)` | L1367-1401 | Submit hidden form (op/id/mode/hash) | ✅ | vendored |
| `ExtractorApi.fixUrl(url)` | L1403-1423 | Same as MainAPI.fixUrl | ✅ | vendored |
| `abstract class ExtractorApi` | L1425-1472 | name/mainUrl/requiresReferer/sourcePlugin | ✅ | vendored |
| `ExtractorApi.getUrl(url, referer?, subtitleCallback, callback)` | L1439-1446 | Default: dispatch to legacy 2-arg | ✅ | vendored |
| `ExtractorApi.getSafeUrl(url, referer?, subsCb, linkCb)` | L1448-1459 | Try/catch wrapper | ✅ | vendored |
| `ExtractorApi.getUrl(url, referer?)` (legacy 2-arg) | L1465-1467 | Returns `List<ExtractorLink>?` | ✅ | vendored |
| `ExtractorApi.getExtractorUrl(id)` | L1469-1471 | Default no-op | ✅ | vendored |

### 1.3 BasePlugin.kt — Plugin entry point
**Source**: `library/src/commonMain/kotlin/com/lagradost/cloudstream3/plugins/BasePlugin.kt` (78 lines)

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `PLUGIN_TAG = "PluginInstance"` | L12 | Logcat tag | ✅ | vendored |
| `abstract class BasePlugin` | L14 | Plugin base class | ✅ | vendored |
| `registerMainAPI(element)` | L20-25 | Add provider to `APIHolder.allProviders` | ✅ | `app/.../plugin/PluginLoader.kt:88-92` (after `load()`) |
| `registerExtractorAPI(element)` | L31-35 | Add extractor to `extractorApis` | ✅ | `app/.../plugin/PluginLoader.kt` |
| `beforeUnload()` | L40-42 | Hook called on unload | ⏳ Phase 2 | not wired |
| `load()` | L47-49 | Hook called on load | ✅ | `app/.../plugin/PluginLoader.kt:108` |
| `__filename` (deprecated → `filename`) | L52-61 | Plugin file path (legacy name) | ✅ | `app/.../plugin/PluginLoader.kt:94` |
| `filename: String?` | L62 | Plugin file path | ✅ | `app/.../plugin/PluginLoader.kt:94` |
| `BasePlugin.Manifest` (Serializable) | L64-77 | name/pluginClassName/requiresResources/version | ✅ | `app/.../plugin/PluginLoader.kt:43-49` (`PluginManifest` mirror) |

### 1.4 PluginManager.kt — Android plugin loading
**Source**: `app/src/main/java/com/lagradost/cloudstream3/plugins/PluginManager.kt` (968 lines)

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `PLUGINS_KEY = "PLUGINS_KEY"` | L70 | SharedPreferences key for online plugins | ✅ | `app/.../plugin/PluginManager.kt:30-31` (plugins.json file) |
| `PLUGINS_KEY_LOCAL = "PLUGINS_KEY_LOCAL"` | L71 | SharedPreferences key for local plugins | ✅ | same |
| `EXTENSIONS_CHANNEL_ID/NAME/DESCRIPT` | L73-75 | Android notification channel | ❌ Skip | N/A on desktop |
| `PluginData` (Serializable) | L79-106 | internalName/url/isOnline/filePath/version | ✅ | `app/.../plugin/PluginManager.kt:33-41` (`InstalledPlugin`) |
| `PLUGIN_VERSION_NOT_SET = Int.MIN_VALUE` | L109 | Sentinel | ✅ | vendored |
| `PLUGIN_VERSION_ALWAYS_UPDATE = -1` | L112 | Sentinel | ✅ | vendored |
| `object PluginManager` | L114 | Singleton | ✅ | `app/.../plugin/PluginManager.kt:26` (class) |
| `lock: Mutex` | L116 | Concurrent write lock | ✅ | `app/.../plugin/PluginManager.kt` (implicit via StateFlow) |
| `setPluginData(data)` | L125-136 | Persist plugin metadata | ✅ | `app/.../plugin/PluginManager.kt:120-135` |
| `deletePluginData(data)` | L138-149 | Remove plugin metadata | ✅ | `app/.../plugin/PluginManager.kt:158-165` |
| `deleteRepositoryData(repositoryPath)` | L151-162 | Remove repo + plugins | ✅ | `app/.../plugin/PluginManager.kt:158-165` |
| `deleteAllOatFiles(context)` | L168-176 | Force Android DEX recompile | ❌ Skip | N/A — JVM uses URLClassLoader |
| `getPluginsOnline()` / `getPluginsLocal()` | (later) | Read from SharedPreferences | ✅ | `app/.../plugin/PluginManager.kt:71-90` (plugins.json) |
| `loadAllOnlinePlugins(context)` / `loadAllLocalPlugins(context)` | (later) | Boot-time load | ✅ | `app/.../plugin/PluginManager.kt:140-156` (`loadAllSavedPlugins`) |
| `loadPlugin(context, file)` | (later) | `PathClassLoader` for .cs3 (APK/DEX) | ✅ | `app/.../plugin/PluginLoader.kt:51-122` (dex2jar + URLClassLoader) |
| `loadSinglePlugin(context, pluginData, ...)` | (later) | Single-plugin load entry | ✅ | `app/.../plugin/PluginLoader.kt:51` |
| `unloadPlugin(pluginPath)` | (later) | Remove provider + extractor | 🚧 | partial — `app/.../plugin/PluginManager.kt:158-165` deletes file but doesn't unregister from `APIHolder.allProviders` |
| `getPluginSanitizedFileName(name)` | (later) | Sanitize for filesystem | ✅ | `app/.../repo/RepositoryManager.kt:182-183` (`sanitize`) |
| `updatePluginSettings(newSettings)` | (later) | Push `SettingsJson` to providers | ⏳ Phase 2 | not wired |
| `safeMode` (skip on crash) | (later) | Plugin crash recovery | ⏳ Phase 3 | not started |

### 1.5 RepositoryManager.kt — Repo + plugin download
**Source**: `app/src/main/java/com/lagradost/cloudstream3/plugins/RepositoryManager.kt` (293 lines)

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `Repository` (Serializable) | L34-40 | iconUrl/name/description/manifestVersion/pluginLists | ✅ | `app/.../repo/RepositoryManager.kt:42-48` |
| `SitePlugin` (Serializable) | L50-76 | url/status/version/apiVersion/name/internalName/authors/description/repositoryUrl/tvTypes/language/iconUrl/fileSize/fileHash | ✅ | `app/.../repo/RepositoryManager.kt:51-66` |
| `PluginWrapper(repository, repositoryData, plugin)` | L79-95 | Bundled pair | ⏳ Phase 2 | not strictly needed |
| `RepositoryManager.ONLINE_PLUGINS_FOLDER = "Extensions"` | L99 | Plugin storage subdir | ✅ | `app/.../plugin/PluginManager.kt:31` (`plugins/` dir) |
| `PREBUILT_REPOSITORIES` | L100 | Built-in repo URLs (cloudstreamrepo://...) | ⏳ Phase 1 | need to mirror in `app/.../repo/RepositoryManager.kt` |
| `RepositoryManager.getRepoPlugins(repoData)` | (later) | Fetch + parse repo manifest | ✅ | `app/.../repo/RepositoryManager.kt:97-115` (`fetchRepo`) |
| `RepositoryManager.downloadPluginToFile(...)` | (later) | Download .cs3 + SHA-256 verify | ✅ | `app/.../repo/RepositoryManager.kt:121-150` (`downloadAndInstall`) |
| `RepositoryManager.sha256(bytes)` | (later) | Hash helper | ✅ | `app/.../repo/RepositoryManager.kt:185-188` |
| Repo URL parsing (`cloudstreamrepo://`, `https://cs.repo/`, `!short`, `cutt.ly`) | (later) | Shortlink expansion | ✅ | `app/.../repo/RepositoryManager.kt:78-91` (`parseRepoUrl`) |
| `REPOSITORIES_KEY = "REPOSITORIES_KEY"` | `ExtensionsViewModel.kt:43` | Stored repo list | ✅ | persisted via `plugins.json` |
| `RepositoryData` (Serializable) | `ExtensionsViewModel.kt:22-41` | iconUrl/name/url/description | ⏳ Phase 1 | in-memory only, not persisted yet |

---

## Section 2 — UI Screens

### 2.1 Home screen

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `HomeFragment` | `ui/home/HomeFragment.kt` | Main page with multiple horizontal LazyRows | 🚧 | `app/.../ui/HomeScreen.kt` (basic — loads `getMainPage`, displays LazyColumn of LazyRow) |
| `HomeViewModel` | `ui/home/HomeViewModel.kt` | Async load of all providers' homepages | 🚧 | inline `loadHomeFeed()` in HomeScreen.kt |
| `HomeParentItemAdapter` | `ui/home/HomeParentItemAdapter.kt` | Row-level RecyclerView adapter | ❌ Skip | replaced by Compose `LazyRow` |
| `HomeParentItemAdapterPreview` | `ui/home/HomeParentItemAdapterPreview.kt` | TV layout variant | ❌ Skip | N/A |
| `HomeChildItemAdapter` | `ui/home/HomeChildItemAdapter.kt` | Poster card adapter | ❌ Skip | Compose `PosterCard` |
| `HomeScrollAdapter` / `HomeScrollTransformer` | `ui/home/HomeScroll*.kt` | Page-snap carousel | ⏳ Phase 2 | not yet needed |
| Pull-to-refresh | (in HomeFragment) | SwipeRefreshLayout | ⏳ Phase 2 | not implemented |
| Empty state | (in HomeFragment) | "Install plugins to populate" | ✅ | `app/.../ui/HomeScreen.kt:83-91` |
| Loading spinner | (in HomeFragment) | CircularProgressIndicator | ✅ | `app/.../ui/HomeScreen.kt:73-75` |
| Error state | (in HomeFragment) | "Failed to load home feed" | ✅ | `app/.../ui/HomeScreen.kt:77-81` |
| `sequentialMainPage` delay | MainAPI.kt:524 | Honor per-provider delay | ⏳ Phase 2 | not honored |
| Hero carousel (TMDB trending) | (HomeFragment) | Top banner | ⏳ Phase 2 | not implemented |
| "Random" button (if enabled) | `random_button_key` | Random title picker | ⏳ Phase 2 | setting exists, not wired |

### 2.2 Search screen

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `SearchFragment` | `ui/search/SearchFragment.kt` | Search UI | 🚧 | `app/.../ui/SearchScreen.kt` (search box only — no results yet) |
| `SearchViewModel` | `ui/search/SearchViewModel.kt` | Debounced search across providers | ⏳ Phase 1 | not started |
| `SearchAdaptor` | `ui/search/SearchAdaptor.kt` | Grid of results | ⏳ Phase 1 | not started |
| `SearchHistoryAdaptor` | `ui/search/SearchHistoryAdaptor.kt` | Recent searches list | ⏳ Phase 2 | not started |
| `SearchSuggestionAdapter` | `ui/search/SearchSuggestionAdapter.kt` | Autocomplete suggestions | ⏳ Phase 2 | not started |
| `SearchSuggestionApi` | `ui/search/SearchSuggestionApi.kt` | Google-style suggest API | ⏳ Phase 2 | not started |
| `SearchResultBuilder` | `ui/search/SearchResultBuilder.kt` | Group results by provider / type | ⏳ Phase 1 | not started |
| `SearchHelper` | `ui/search/SearchHelper.kt` | Utilities | ⏳ Phase 1 | not started |
| `SyncSearchViewModel` | `ui/search/SyncSearchViewModel.kt` | Search across sync APIs (MAL, AniList, etc.) | ⏳ Phase 2 | not started |
| `QuickSearchFragment` | `ui/quicksearch/QuickSearchFragment.kt` | Bottom-sheet quick search | ⏳ Phase 2 | not started |
| Advanced search toggle | `advanced_search` setting | Group by provider | ⏳ Phase 1 | setting exists in AppSettings |
| Search suggestions toggle | `search_suggestions_enabled` | Autocomplete | ⏳ Phase 2 | setting exists |

### 2.3 Result / Detail screen

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `ResultFragment` | `ui/result/ResultFragment.kt` | Common base | ❌ Skip | Compose replaces fragments |
| `ResultFragmentPhone` | `ui/result/ResultFragmentPhone.kt` | Phone layout | ⏳ Phase 1 | not started |
| `ResultFragmentTv` | `ui/result/ResultFragmentTv.kt` | TV layout | ⏳ Phase 3 | not started |
| `ResultViewModel2` | `ui/result/ResultViewModel2.kt` (2,712 lines) | **Core**: load episode list, manage state | ⏳ Phase 1 | not started (high priority) |
| `EpisodeAdapter` | `ui/result/EpisodeAdapter.kt` | Episode list adapter | ⏳ Phase 1 | not started |
| `ActorAdaptor` | `ui/result/ActorAdaptor.kt` | Cast list adapter | ⏳ Phase 2 | not started |
| `ImageAdapter` | `ui/result/ImageAdapter.kt` | Screenshot gallery adapter | ⏳ Phase 3 | not started |
| `SelectAdaptor` | `ui/result/SelectAdaptor.kt` | Generic dropdown adapter | ⏳ Phase 1 | not started |
| `LinearListLayout` | `ui/result/LinearListLayout.kt` | Layout manager | ❌ Skip | Compose replaces |
| `ResultTrailerPlayer` | `ui/result/ResultTrailerPlayer.kt` | Inline trailer player | ⏳ Phase 2 | not started |
| `SyncViewModel` | `ui/result/SyncViewModel.kt` | Sync API interactions | ⏳ Phase 2 | not started |
| Bookmark button | (in ResultFragment) | Add to bookmarks | ⏳ Phase 1 | not started |
| Favorite button | (in ResultFragment) | Add to favorites | ⏳ Phase 1 | not started |
| Subscribe button | (in ResultFragment) | Subscribe to ongoing series | ⏳ Phase 2 | not started |
| Episode range selector | (in ResultViewModel2) | 1-20, 21-40, ... pagination | ⏳ Phase 1 | not started |
| Season selector | (in ResultViewModel2) | Switch seasons | ⏳ Phase 1 | not started |
| Dub/Sub selector | (in ResultViewModel2) | Anime dub status | ⏳ Phase 1 | not started |
| Episode sort (asc/desc/rating/date) | (in ResultViewModel2) | EpisodeSortType enum | ⏳ Phase 1 | not started |
| Recommendations row | (in ResultFragment) | "Recommended for you" | ⏳ Phase 2 | not started |
| Cast row | (in ResultFragment) | Actor list | ⏳ Phase 2 | not started |
| Trailer playback | (in ResultFragment) | Play trailer inline | ⏳ Phase 2 | not started |
| "Next airing" info | (in ResultViewModel2) | Countdown to next episode | ⏳ Phase 2 | not started |
| Coming-soon state | (in ResultFragment) | Display "coming soon" | ⏳ Phase 2 | not started |
| Reload button | (in ResultFragment) | Force re-load | ⏳ Phase 2 | not started |
| Skip-op overlay | (in ResultViewModel2) | Show skip-op button | ⏳ Phase 2 | not started |
| Media Info dialog | (in ResultFragment) | Quality/format info | ⏳ Phase 3 | not started |
| Sync status indicator | (in ResultFragment) | MAL/AniList watch status | ⏳ Phase 2 | not started |
| `ResultData` data class | `ResultViewModel2.kt:167-197` | UI state | ⏳ Phase 1 | not started |
| `EpisodeRange` data class | `ResultViewModel2.kt:151-158` | Range metadata | ⏳ Phase 1 | not started |
| `AutoResume` data class | `ResultViewModel2.kt:160-165` | Resume metadata | ⏳ Phase 1 | not started |
| `LibraryListType { BOOKMARKS, FAVORITES, SUBSCRIPTIONS }` enum | `ResultViewModel2.kt:205-209` | Library section | ⏳ Phase 1 | not started |
| `EpisodeSortType` enum (6 values) | `ResultViewModel2.kt:211-218` | Sort modes | ⏳ Phase 1 | not started |

### 2.4 Player screen

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `FullScreenPlayer` | `ui/player/FullScreenPlayer.kt` (1,659 lines) | Main player UI | ⏳ Phase 1 | `app/.../player/VlcjPlayer.kt` (player engine only — no Compose UI overlay) |
| `GeneratorPlayer` | `ui/player/GeneratorPlayer.kt` (2,403 lines) | Source/subtitle picker, mirror cycling, skip-op | ⏳ Phase 1 | not started |
| `CS3IPlayer` | `ui/player/CS3IPlayer.kt` (2,067 lines) | ExoPlayer wrapper, DRM, tracks | 🚧 | `app/.../player/VlcjPlayer.kt` (vlcj replaces ExoPlayer) |
| `IPlayer` interface | `ui/player/IPlayer.kt` (295 lines) | Player abstraction | 🚧 | partial — VlcjPlayer implements subset |
| `RepoLinkGenerator` | `ui/player/RepoLinkGenerator.kt` (160 lines) | Link caching (20min TTL) | ⏳ Phase 1 | not started (high priority) |
| `LinkGenerator` / `IGenerator` / `VideoGenerator` / `ExtractorLinkGenerator` | `ui/player/*.kt` | Link source abstractions | ⏳ Phase 1 | not started |
| `PlayerGeneratorViewModel` | `ui/player/PlayerGeneratorViewModel.kt` | Player + generator state | ⏳ Phase 1 | not started |
| `PlayerSubtitleHelper` | `ui/player/PlayerSubtitleHelper.kt` (150 lines) | Subtitle data, mime type, styling | 🚧 | `app/.../player/VlcjPlayer.kt:159-173` (basic subtitle selection) |
| `CustomSubtitleDecoderFactory` | `ui/player/CustomSubtitleDecoderFactory.kt` | Subtitle parsing tweaks | ❌ Skip | vlcj handles internally |
| `UpdatedDefaultExtractorsFactory` | `ui/player/UpdatedDefaultExtractorsFactory.kt` | ExoPlayer extractor tweaks | ❌ Skip | vlcj handles internally |
| `UpdatedMatroskaExtractor` | `ui/player/UpdatedMatroskaExtractor.kt` | MKV extractor tweaks | ❌ Skip | vlcj handles internally |
| `CustomSubripParser` | `ui/player/CustomSubripParser.kt` | SRT parser | ❌ Skip | vlcj handles |
| `PreviewGenerator` | `ui/player/PreviewGenerator.kt` (545 lines) | Seek-bar preview thumbnails | ⏳ Phase 3 | vlcj supports snapshots |
| `PlayerGestureHelper` | `ui/player/PlayerGestureHelper.kt` | Swipe-to-seek, double-tap | ⏳ Phase 2 | not started |
| `PlayerPipHelper` | `ui/player/PlayerPipHelper.kt` | Picture-in-picture | ❌ Skip | N/A on desktop |
| `PlayerView` | `ui/player/PlayerView.kt` | Custom PlayerView | ❌ Skip | Compose replaces |
| `DownloadFileGenerator` | `ui/player/DownloadFileGenerator.kt` | Play downloaded files | ⏳ Phase 3 | not started |
| `DownloadedPlayerActivity` | `ui/player/DownloadedPlayerActivity.kt` | Standalone player for downloads | ❌ Skip | desktop uses single window |
| `OfflinePlaybackHelper` | `ui/player/OfflinePlaybackHelper.kt` | Resolve offline media to URI | ⏳ Phase 3 | not started |
| `OutlineSpan` / `RoundedBackgroundColorSpan` | `ui/player/*.kt` | Subtitle text styling | ⏳ Phase 2 | not started |
| `SubtitleOffsetItemAdapter` | `ui/player/SubtitleOffsetItemAdapter.kt` | Subtitle timing offset UI | ⏳ Phase 2 | not started |
| `SSLTrustManager` | `ui/player/SSLTrustManager.kt` | Custom SSL for player | ⏳ Phase 2 | not started |
| `FixedNextRenderersFactory` | `ui/player/FixedNextRenderersFactory.kt` | ExoPlayer renderer factory | ❌ Skip | vlcj replaces |
| `DynamicRangeCompressor` | `ui/player/DynamicRangeCompressor.kt` | Audio compressor (loudness) | ⏳ Phase 3 | not started |
| `Torrent.kt` | `ui/player/Torrent.kt` (351 lines) | Torrent playback via torrentserver | ⏳ Phase 3 | not started |
| `live/LiveHelper` | `ui/player/live/LiveHelper.kt` | Live stream seeking | ⏳ Phase 3 | not started |
| `live/LiveManager` | `ui/player/live/LiveManager.kt` | Live stream state | ⏳ Phase 3 | not started |
| `live/LivePreviewTimeBar` | `ui/player/live/LivePreviewTimeBar.kt` | Live preview thumbnails | ⏳ Phase 3 | not started |
| `source_priority/QualityDataHelper` | `ui/player/source_priority/QualityDataHelper.kt` (266 lines) | Quality profile storage (per-WiFi/Data/Download) | ⏳ Phase 2 | not started |
| `source_priority/PriorityAdapter` | `ui/player/source_priority/PriorityAdapter.kt` | Source priority list adapter | ⏳ Phase 2 | not started |
| `source_priority/ProfilesAdapter` | `ui/player/source_priority/ProfilesAdapter.kt` | Quality profile list adapter | ⏳ Phase 2 | not started |
| `source_priority/QualityProfileDialog` | `ui/player/source_priority/QualityProfileDialog.kt` | Profile editor | ⏳ Phase 2 | not started |
| `source_priority/SourcePriorityDialog` | `ui/player/source_priority/SourcePriorityDialog.kt` | Per-source priority editor | ⏳ Phase 2 | not started |
| `source_priority/SourceProfileSettingsDialog` | `ui/player/source_priority/SourceProfileSettingsDialog.kt` | Settings entry | ⏳ Phase 2 | not started |
| `CSPlayerEvent` enum (Pause/Play/SeekForward/SeekBack/SkipCurrentChapter/NextEpisode/PrevEpisode/PlayPauseToggle/ToggleMute/Restart/PlayAsAudio) | `IPlayer.kt:12-25` | Player action enum | 🚧 | subset in VlcjPlayer |
| `CSPlayerLoading { IsPaused, IsPlaying, IsBuffering }` | `IPlayer.kt:27-32` | Loading state | ✅ | `app/.../player/VlcjPlayer.kt:50-63` |
| `PlayerEventSource { UI, Player, Cache, Generator }` | `IPlayer.kt:34-44` | Event source attribution | ⏳ Phase 1 | not started |
| `PlayerEvent` sealed class (Seek, Error, Skip, SkipTransition, SubtitleOffset, VideoSizeChanged, PlayerStatusChanged, SubtitlesFetched, TracksInfoChanged, ... 13 events) | `IPlayer.kt:46-160` | Player event bus | 🚧 | partial in VlcjPlayer (timeChanged/playing/paused/stopped/buffering/error) |
| `AudioTrack` / `VideoTrack` / `TextTrack` data classes | `IPlayer.kt:163-201` | Track metadata | ⏳ Phase 1 | not started |
| `CurrentTracks` data class | `IPlayer.kt:196-202` | All tracks + currently-selected | ⏳ Phase 1 | not started |
| `IPlayer` interface methods (loadPlayer, reloadPlayer, getPreview, hasPreview, setActiveSubtitles, setPreferredSubtitles, getCurrentPreferredSubtitle, handleEvent, onStop, onPause, onResume, release, isActive, getVideoTracks, getAspectRatio, setMaxVideoSize, setPreferredAudioTrack, getSubtitleCues, getPlaybackSpeed, setPlaybackSpeed, getIsPlaying, getDuration, getPosition, seekTime, seekTo, getSubtitleOffset, setSubtitleOffset, initCallbacks, releaseCallbacks, updateSubtitleStyle, saveData, addTimeStamps) | `IPlayer.kt:208-293` | Player contract | 🚧 | subset in VlcjPlayer (load, play, pause, seekTo, setVolume, setRate, selectSubtitle) |
| Player subtitle styling | `ui/subtitles/SubtitlesFragment.kt` | Font, size, color, outline | ⏳ Phase 2 | not started |
| Custom subtitle font | (in SubtitlesFragment) | TTF font picker | ⏳ Phase 3 | not started |
| Subtitle offset slider | (in PlayerSubtitleHelper) | ±5 sec slider | ⏳ Phase 2 | not started |
| Mirror cycling (auto-retry on fail) | `GeneratorPlayer.kt` | Auto-advance to next mirror | ⏳ Phase 1 | not started |
| Source picker dialog | `GeneratorPlayer.kt` | Bottom sheet of all sources | ⏳ Phase 1 | not started |
| Subtitle picker dialog (online APIs) | `GeneratorPlayer.kt:591-800` | Search OpenSubtitles/SubDL/SubSource/Addic7ed | ⏳ Phase 2 | not started |
| Skip-Op button (AniSkip / AnimeSkip / IntroDb / TheIntroDB) | `GeneratorPlayer.kt` + `utils/videoskip/` | Skip intro/outro | ⏳ Phase 2 | not started |
| Next episode auto-play | `GeneratorPlayer.kt` + `autoplay_next_key` setting | Auto-advance | ⏳ Phase 2 | setting exists |
| Episode sync (resume position) | `episode_sync_enabled_key` setting | Persist playback position | ⏳ Phase 2 | setting exists |
| Playback speed (eigengravy) | `playback_speed_enabled_key` setting | Speed slider | ⏳ Phase 2 | VlcjPlayer supports `setRate` |
| Picture-in-picture | `pip_enabled_key` setting | PiP mode | ❌ Skip | N/A on desktop |
| Player resize (fit/fill/zoom) | `player_resize_enabled_key` setting | Aspect ratio toggle | ⏳ Phase 2 | not started |
| Compressor (audio dynamic range) | `compressor_enabled_key` setting | Loudness compressor | ⏳ Phase 3 | not started |
| Speedup (tiktok-style 2x tap) | `speedup_key` setting | Hold for 2x | ⏳ Phase 3 | not started |
| Rotate video / Auto-rotate | `rotate_video_key` / `auto_rotate_video_key` settings | Orientation | ❌ Skip | N/A on desktop |
| Preview seekbar (thumbnails) | `preview_seekbar_key` setting | Scrubbing preview | ⏳ Phase 3 | vlcj supports snapshots |
| Software decoding toggle | `software_decoding_key` setting | Force software decoder | ❌ Skip | vlcj always uses libvlc |
| Extra brightness | `extra_brightness_key` setting | Screen brightness boost | ❌ Skip | N/A on desktop |
| Buffer size (disk/RAM/length) | `video_buffer_disk_key` / `video_buffer_size_key` / `video_buffer_length_key` settings | Cache sizes | ⏳ Phase 2 | not wired |
| Buffer clear button | `video_buffer_clear_key` setting | Wipe cache | ⏳ Phase 2 | not started |
| Android-TV seek times | `android_tv_interface_on_seek_key` / `android_tv_interface_off_seek_key` settings | TV UI seek increments | ❌ Skip | N/A on desktop |
| Limit player title length | `prefer_limit_title_key` setting | Truncate title | ⏳ Phase 2 | not started |
| Show player media info | `prefer_limit_show_player_info` setting | Format/resolution display | ⏳ Phase 3 | not started |
| Hide player control names | `hide_player_control_names_key` setting | Icon-only controls | ⏳ Phase 2 | not started |
| Show name/resolution toggles | `show_name` / `show_resolution` settings | Toggle overlays | ⏳ Phase 2 | not started |
| Notification (Android media session) | `GeneratorPlayer.kt:286-440` | Media notification with custom actions | ❌ Skip | replaced by MPRIS later (Phase 3) |

### 2.5 Library screen

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `LibraryFragment` | `ui/library/LibraryFragment.kt` (575 lines) | Library with tabs | 🚧 | `app/.../ui/SearchScreen.kt:37-42` (`LibraryScreen` stub) |
| `LibraryViewModel` | `ui/library/LibraryViewModel.kt` (146 lines) | Library state | ⏳ Phase 1 | not started |
| `PageAdapter` | `ui/library/PageAdapter.kt` | Tabbed pager | ❌ Skip | Compose `TabRow` |
| `ViewpagerAdapter` | `ui/library/ViewpagerAdapter.kt` | Library page adapter | ❌ Skip | Compose |
| `LoadingPosterAdapter` | `ui/library/LoadingPosterAdapter.kt` | Skeleton loader | ⏳ Phase 2 | not started |
| `LibraryScrollTransformer` | `ui/library/LibraryScrollTransformer.kt` | Page transformer | ❌ Skip | N/A |
| Tabs: Favorites / Bookmarks / Subscriptions / Watching / Completed / Dropped | (in LibraryFragment) | WatchType-based tabs | ⏳ Phase 1 | not started |
| List sorting (alphabetical, rating, updated, release date, query) | `ListSorting` enum (9 values) | Sort dropdown | ⏳ Phase 1 | not started |
| Sync API switcher | (in LibraryFragment) | Show MAL/AniList/etc. library | ⏳ Phase 2 | not started |
| `LAST_SYNC_API_KEY = "last_sync_api"` | `LibraryViewModel.kt:31` | Last selected sync API | ⏳ Phase 2 | not started |

### 2.6 Downloads screen

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `DownloadFragment` | `ui/download/DownloadFragment.kt` | Downloaded titles list | ⏳ Phase 3 | not started |
| `DownloadChildFragment` | `ui/download/DownloadChildFragment.kt` | Single-title downloads view | ⏳ Phase 3 | not started |
| `DownloadViewModel` | `ui/download/DownloadViewModel.kt` | Download state | ⏳ Phase 3 | not started |
| `DownloadAdapter` | `ui/download/DownloadAdapter.kt` | List adapter | ❌ Skip | Compose replaces |
| `DownloadButtonSetup` | `ui/download/DownloadButtonSetup.kt` | Setup download button state | ⏳ Phase 3 | not started |
| `button/BaseFetchButton` / `DownloadButton` / `PieFetchButton` / `ProgressBarAnimation` | `ui/download/button/*.kt` | Progress pie button | ❌ Skip | Compose custom button |
| `queue/DownloadQueueFragment` | `ui/download/queue/DownloadQueueFragment.kt` | Active downloads queue | ⏳ Phase 3 | not started |
| `queue/DownloadQueueAdapter` | `ui/download/queue/DownloadQueueAdapter.kt` | Queue list adapter | ❌ Skip | Compose |
| `queue/DownloadQueueViewModel` | `ui/download/queue/DownloadQueueViewModel.kt` | Queue state | ⏳ Phase 3 | not started |
| `DownloadQueueService` | `services/DownloadQueueService.kt` | Foreground service for downloads | ❌ Skip | desktop uses coroutines |
| `VideoDownloadService` | `services/VideoDownloadService.kt` | Actual download worker | ⏳ Phase 3 | not started |

### 2.7 Extensions / Plugins screen

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `ExtensionsFragment2` | `ui/settings/extensions/ExtensionsFragment2.kt` | Modern extensions list (Compose) | 🚧 | `app/.../ui/PluginsScreen.kt` (basic — installed plugins + fetch repo) |
| `ExtensionsScreen` | `ui/settings/extensions/ExtensionsScreen.kt` | Compose version | ⏳ Phase 1 | `app/.../ui/PluginsScreen.kt` (Compose native) |
| `ExtensionsViewModel` | `ui/settings/extensions/ExtensionsViewModel.kt` (110 lines) | Plugin stats + repo list | ⏳ Phase 1 | not started |
| `PluginAdapter` | `ui/settings/extensions/PluginAdapter.kt` | Plugin list adapter | ❌ Skip | Compose |
| `PluginsFragment` | `ui/settings/extensions/PluginsFragment.kt` | Installed plugins (legacy) | 🚧 | `app/.../ui/PluginsScreen.kt:86-101` |
| `PluginsViewModel` | `ui/settings/extensions/PluginsViewModel.kt` | Plugin list state | ⏳ Phase 1 | inline state in PluginsScreen |
| `PluginDetailsFragment` | `ui/settings/extensions/PluginDetailsFragment.kt` | Plugin detail view | ⏳ Phase 2 | not started |
| `RepoAdapter` | `ui/settings/extensions/RepoAdapter.kt` | Repo list adapter | ❌ Skip | Compose |
| `AndroidRepositoryManager` | `ui/settings/extensions/AndroidRepositoryManager.kt` | Android-specific repo mgmt | ✅ | `app/.../repo/RepositoryManager.kt` (JVM version) |
| Plugin stats (total/downloaded/disabled/notDownloaded) | `ExtensionsViewModel.kt:46-56` | Stats card | ⏳ Phase 1 | not started |
| Repo URL input + fetch | (in ExtensionsFragment2) | Add repo + browse plugins | ✅ | `app/.../ui/PluginsScreen.kt:48-76` |
| Plugin install button | (in ExtensionsFragment2) | Download + install | ✅ | `app/.../ui/PluginsScreen.kt:128-135` |
| Plugin uninstall | (in PluginsFragment) | Remove plugin | ⏳ Phase 1 | `app/.../plugin/PluginManager.kt:158-165` exists, not wired to UI |
| Plugin enable/disable toggle | (in PluginsFragment) | Disable without uninstall | ⏳ Phase 2 | not started |
| Plugin update (when version differs) | (in ExtensionsFragment2) | Update to newer version | ⏳ Phase 2 | not started |
| Plugin search/filter | (in ExtensionsFragment2) | Filter by name/language | ⏳ Phase 2 | not started |
| Local plugin install (file picker) | (in PluginsFragment) | Install .cs3 from disk | ⏳ Phase 1 | not wired (but PluginLoader supports it) |
| Plugin source priority drag-reorder | (in PluginsFragment) | Manual provider ordering | ⏳ Phase 3 | not started |
| Plugin crash log / safe mode indicator | (in PluginsFragment) | Show crash state | ⏳ Phase 3 | not started |
| Open plugin repo (browser) | (in PluginDetailsFragment) | Open repositoryUrl | ⏳ Phase 2 | not started |

### 2.8 Settings screens

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `SettingsFragment2` | `ui/settings/SettingsFragment2.kt` | Compose settings root | 🚧 | `app/.../ui/PluginsScreen.kt:144-163` (`SettingsScreen` stub) |
| `SettingsFragmentScreen` | `ui/settings/SettingsFragmentScreen.kt` | Compose root screen | ⏳ Phase 1 | not started |
| `SettingsGeneral2` / `SettingsGeneralScreen` | `ui/settings/SettingsGeneral2.kt`, `SettingsGeneralScreen.kt` | General settings | ⏳ Phase 1 | not started |
| `SettingsUI2` / `SettingsUIScreen` | `ui/settings/SettingsUI2.kt`, `SettingsUIScreen.kt` | UI settings | ⏳ Phase 1 | not started |
| `SettingsPlayer2` / `SettingsPlayerScreen` | `ui/settings/SettingsPlayer2.kt`, `SettingsPlayerScreen.kt` | Player settings | ⏳ Phase 1 | not started |
| `SettingsProviders2` / `SettingsProvidersScreen` | `ui/settings/SettingsProviders2.kt`, `SettingsProvidersScreen.kt` | Provider settings | ⏳ Phase 1 | not started |
| `SettingsAccount2` / `SettingsAccountScreen` | `ui/settings/SettingsAccount2.kt`, `SettingsAccountScreen.kt` | Account settings | ⏳ Phase 2 | not started |
| `SettingsUpdates2` / `SettingsUpdatesScreen` | `ui/settings/SettingsUpdates2.kt`, `SettingsUpdatesScreen.kt` | Update settings | ⏳ Phase 2 | not started |
| `AccountAdapter` | `ui/settings/AccountAdapter.kt` | Account list adapter | ❌ Skip | Compose |
| `ApkUpdater` | `ui/settings/ApkUpdater.kt` | APK self-updater | ❌ Skip | desktop uses AppImage |
| `GithubReleases` | `ui/settings/GithubReleases.kt` | GitHub releases fetcher | ⏳ Phase 2 | not started (Linux uses different update mechanism) |
| `Globals` (PHONE/TV/EMULATOR layout flags) | `ui/settings/Globals.kt` (49 lines) | Layout detection | 🚧 | desktop always PHONE-style |
| `LogcatAdapter` | `ui/settings/LogcatAdapter.kt` | Logcat list adapter | ❌ Skip | Compose replaces |

### 2.9 Setup wizard

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `SetupFragmentExtensions` | `ui/setup/SetupFragmentExtensions.kt` | Common setup fragment base | ❌ Skip | Compose |
| `SetupFragmentLanguage` | `ui/setup/SetupFragmentLanguage.kt` | Pick app language | ⏳ Phase 2 | not started |
| `SetupFragmentProviderLanguage` | `ui/setup/SetupFragmentProviderLanguage.kt` | Pick provider languages | ⏳ Phase 2 | not started |
| `SetupFragmentMedia` | `ui/setup/SetupFragmentMedia.kt` | Pick preferred media types | ⏳ Phase 2 | not started |
| `SetupFragmentLayout` | `ui/setup/SetupFragmentLayout.kt` | Pick Phone/TV/Emulator layout | ❌ Skip | desktop always phone-style |
| Redo setup process | `redo_setup_key` setting | Restart wizard | ⏳ Phase 3 | not started |

### 2.10 Account / Multi-account

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `AccountSelectActivity2` | `ui/account/AccountSelectActivity2.kt` | Account picker | ⏳ Phase 2 | not started |
| `AccountViewModel2` | `ui/account/AccountViewModel2.kt` | Account state | ⏳ Phase 2 | not started |
| `AccountAdapter` | `ui/account/AccountAdapter.kt` | Account list adapter | ❌ Skip | Compose |
| `AccountHelper` | `ui/account/AccountHelper.kt` | Account operations | ⏳ Phase 2 | not started |
| `AccountScreen` | `ui/account/AccountScreen.kt` | Compose account UI | ⏳ Phase 2 | not started |
| `AccountSelectActivity` | `ui/account/AccountSelectActivity.kt` | Legacy | ❌ Skip | replaced by Activity2 |
| `AccountViewModel` | `ui/account/AccountViewModel.kt` | Legacy | ❌ Skip | replaced by ViewModel2 |
| `AccountSelectLinearItemDecoration` | `ui/account/AccountSelectLinearItemDecoration.kt` | Item decoration | ❌ Skip | Compose |
| Multi-account (create / switch / delete) | (in AccountHelper) | Multiple user profiles | ⏳ Phase 2 | not started |
| `currentAccount` prefix on keys | `DataStoreHelper.kt` | `"${currentAccount}/"` prefix | ⏳ Phase 2 | not started |
| Custom profile image | (in AccountHelper) | Pick image | ⏳ Phase 3 | not started |
| Default profile images | (in AccountHelper) | Built-in avatars | ⏳ Phase 3 | not started |
| `lockPin` (per-account PIN) | (in AccountHelper) | Numeric lock | ⏳ Phase 3 | not started |
| Biometric lock | `BiometricAuthenticator.kt` | Fingerprint unlock | ❌ Skip | use OS keyring instead |
| Skip startup account select | `skip_startup_account_select_key` setting | Boot straight to last account | ⏳ Phase 2 | setting exists |

### 2.11 Subtitles settings

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `SubtitlesFragment2` | `ui/subtitles/SubtitlesFragment2.kt` | Modern subtitles settings | ⏳ Phase 2 | not started |
| `SubtitlesScreen` | `ui/subtitles/SubtitlesScreen.kt` | Compose | ⏳ Phase 2 | not started |
| `SubtitlesFragment` (legacy) | `ui/subtitles/SubtitlesFragment.kt` | Legacy | ❌ Skip | replaced |
| `ChromecastSubtitlesFragment2` / `ChromecastSubtitlesScreen` | `ui/subtitles/Chromecast*.kt` | Cast subtitle styling | ❌ Skip | replaced by Fcast |
| `SaveCaptionStyle` | `ui/subtitles/*.kt` | Subtitle style storage class | ⏳ Phase 2 | not started |
| Subtitle font size | (in SubtitlesFragment) | Seek bar | ⏳ Phase 2 | not started |
| Subtitle font color | (in SubtitlesFragment) | Color picker | ⏳ Phase 2 | not started |
| Subtitle background color | (in SubtitlesFragment) | Color picker | ⏳ Phase 2 | not started |
| Subtitle outline color / width | (in SubtitlesFragment) | Color picker | ⏳ Phase 2 | not started |
| Subtitle font picker | (in SubtitlesFragment) | TTF picker | ⏳ Phase 3 | not started |
| Subtitle elevation | (in SubtitlesFragment) | Vertical offset | ⏳ Phase 2 | not started |
| Subtitle preview | (in SubtitlesFragment) | Live preview text | ⏳ Phase 2 | not started |

### 2.12 Cast / Chromecast

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `ControllerActivity` | `ui/ControllerActivity.kt` | Cast controller | ❌ Skip | replaced by Fcast |
| `MiniControllerFragment` | `ui/MiniControllerFragment.kt` | Mini cast control | ❌ Skip | replaced by Fcast |
| `CastHelper` | `utils/CastHelper.kt` | `startCast()` helper | ❌ Skip | replaced by Fcast |
| `CastOptionsProvider` | `utils/CastOptionsProvider.kt` | Cast SDK options | ❌ Skip | replaced by Fcast |
| Cast session management | (in CastHelper) | Google Cast SDK | ❌ Skip | Fcast (Phase 3) |
| MediaInfo builder | (in CastHelper) | Build cast media metadata | ❌ Skip | Fcast (Phase 3) |
| Cast subtitle styling | `ChromecastSubtitlesFragment2` | Subtitle colors on cast | ❌ Skip | Fcast (Phase 3) |
| `actions/temp/fcast/FcastAction` | `actions/temp/fcast/FcastAction.kt` (69 lines) | Fcast action | ⏳ Phase 3 | not started |
| `actions/temp/fcast/FcastManager` | `actions/temp/fcast/FcastManager.kt` (194 lines) | Fcast session | ⏳ Phase 3 | not started |
| `actions/temp/fcast/FcastSession` | `actions/temp/fcast/FcastSession.kt` (59 lines) | Fcast session | ⏳ Phase 3 | not started |
| `actions/temp/fcast/Packets` | `actions/temp/fcast/Packets.kt` (66 lines) | Fcast protocol | ⏳ Phase 3 | not started |

### 2.13 Webview

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `WebviewFragment` | `ui/WebviewFragment.kt` | In-app browser | ⏳ Phase 2 | not started (JavaFX WebView available) |
| Cloudflare/DdosGuard solving | `network/CloudflareKiller.kt`, `network/DdosGuardKiller.kt` | Bypass JS challenges | ⏳ Phase 2 | `app/.../network/WebViewResolverJvm.kt` (stub) |

### 2.14 Common / Main activity

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `MainActivity` | `MainActivity.kt` | Single-activity host | ❌ Skip | replaced by Compose Desktop `Window` |
| `MainActivityScreen` | `MainActivityScreen.kt` | Compose root | ✅ | `app/.../Main.kt:69-109` |
| `CommonActivity` | `CommonActivity.kt` | Shared activity helpers | ❌ Skip | N/A |
| `CloudStreamApp` | `CloudStreamApp.kt` | Application class | ❌ Skip | N/A |
| `AcraApplication` | `AcraApplication.kt` | ACRA crash reporting | ❌ Skip | N/A on desktop |
| TV layout support (homepage_parent_tv, fragment_home_tv) | (various) | TV-specific layouts | ❌ Skip | desktop always phone-style |
| Emulator layout | (in Globals) | Emulator detection | ❌ Skip | N/A |
| `EasterEggMonkeFragment` | `ui/EasterEggMonkeFragment.kt` | Easter egg | ⏳ Phase 3 | not started |

### 2.15 Other UI utilities

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `BaseFragment` | `ui/BaseFragment.kt` | Fragment base class | ❌ Skip | Compose replaces |
| `BaseAdapter` | `ui/BaseAdapter.kt` | Adapter base | ❌ Skip | Compose replaces |
| `BaseComposeFragment` | `utils/BaseComposeFragment.kt` | Compose fragment base | ❌ Skip | Compose Desktop native |
| `CustomRecyclerViews` | `ui/CustomRecyclerViews.kt` | Custom RV widgets | ❌ Skip | Compose replaces |
| `NonFinalAdapterListUpdateCallback` | `ui/NonFinalAdapterListUpdateCallback.kt` | RV diff callback | ❌ Skip | Compose replaces |
| `WatchType` enum | `ui/WatchType.kt` | NONE/WATCHING/COMPLETED/DROPPED/FAVORITE/BOOKMARK/SUBSCRIPTION | ⏳ Phase 1 | not started (data only) |
| `SyncWatchType` enum | `ui/SyncWatchType.kt` | Sync API watch types | ⏳ Phase 2 | not started |
| `ImageModuleCoil` | `utils/ImageModuleCoil.kt` | Coil image loader config | ✅ | `app/.../ui/HomeScreen.kt:38-39` (Coil 3) |
| `ImageUtil` | `utils/ImageUtil.kt` | Image helpers | ⏳ Phase 2 | not started |
| `PercentageCropImageView` | `utils/PercentageCropImageView.kt` | Custom ImageView | ❌ Skip | Compose replaces |
| `UIHelper` | `utils/UIHelper.kt` | UI utility functions | ⏳ Phase 2 | not started |
| `AppContextUtils` | `utils/AppContextUtils.kt` (905 lines) | Context-aware utilities | ⏳ Phase 2 | partial |
| `SnackbarHelper` | `utils/SnackbarHelper.kt` | Snackbar helpers | ⏳ Phase 1 | use Compose `Snackbar` |
| `BackPressedCallbackHelper` | `utils/BackPressedCallbackHelper.kt` | Back button handling | ⏳ Phase 2 | use Compose `BackHandler` |
| `IntentHelpers` | `utils/IntentHelpers.kt` | Intent utilities | ❌ Skip | N/A on desktop |
| `PackageInstaller` | `utils/PackageInstaller.kt` | APK install | ❌ Skip | desktop uses AppImage |
| `PackageInstallerService` | `services/PackageInstallerService.kt` | APK install service | ❌ Skip | N/A |
| `PackageInstallerStatusReceiver` | `receivers/PackageInstallerStatusReceiver.kt` | Install status | ❌ Skip | N/A |
| `VideoDownloadRestartReceiver` | `receivers/VideoDownloadRestartReceiver.kt` | Restart on boot | ❌ Skip | N/A |
| `TvChannelUtils` | `utils/TvChannelUtils.kt` | Android TV channels | ❌ Skip | N/A |
| `PowerManagerAPI` | `utils/PowerManagerAPI.kt` | Battery optimizations | ❌ Skip | N/A |
| `Vector2` | `utils/Vector2.kt` | 2D vector | ⏳ Phase 2 | for gestures |
| `TextUtil` | `utils/TextUtil.kt` | Text helpers | ⏳ Phase 1 | inline |
| `TestingUtils` | `utils/TestingUtils.kt` | Provider test framework | ⏳ Phase 2 | not started |
| `SyncUtil` | `utils/SyncUtil.kt` | Sync API utilities | ⏳ Phase 2 | not started |
| `Event` | `utils/Event.kt` | Event bus | ⏳ Phase 1 | use Compose state |
| `IDisposable` | `utils/IDisposable.kt` | Disposable interface | ⏳ Phase 1 | use Compose `DisposableEffect` |
| `ConsistentLiveData` | `utils/ConsistentLiveData.kt` | Threading-aware LiveData | ❌ Skip | use Compose `StateFlow` |
| `SingleSelectionHelper` | `utils/SingleSelectionHelper.kt` | Single-pick dialog | ⏳ Phase 1 | Compose `AlertDialog` |
| `Markdown` | `utils/Markdown.kt` | Markdown renderer | ⏳ Phase 3 | use commonmark-java |
| `GitInfo` | `utils/GitInfo.kt` | Git version info | ❌ Skip | N/A |
| `InAppUpdater` | `utils/InAppUpdater.kt` | In-app update logic | ⏳ Phase 3 | not started |

---

## Section 3 — Data Persistence

### 3.1 DataStore.kt constants
**Source**: `app/src/main/java/com/lagradost/cloudstream3/utils/DataStore.kt`

| Key constant | Line | Stores | Status | Port File |
|---|---|---|---|---|
| `DOWNLOAD_HEADER_CACHE = "download_header_cache"` | L17 | Cached download headers (DownloadHeaderCached map) | ⏳ Phase 3 | not started |
| `DOWNLOAD_HEADER_CACHE_BACKUP = "BACKUP_download_header_cache"` | L18 | Backup of download headers | ⏳ Phase 3 | not started |
| `DOWNLOAD_EPISODE_CACHE = "download_episode_cache"` | L21 | Cached episode metadata (DownloadEpisodeCached map) | ⏳ Phase 3 | not started |
| `DOWNLOAD_EPISODE_CACHE_BACKUP = "BACKUP_download_episode_cache"` | L22 | Backup of episode cache | ⏳ Phase 3 | not started |
| `VIDEO_PLAYER_BRIGHTNESS = "video_player_alpha_key"` | L23 | Player brightness | ❌ Skip | N/A on desktop |
| `USER_SELECTED_HOMEPAGE_API = "home_api_used"` | L24 | User's preferred homepage provider | ⏳ Phase 2 | not started |
| `USER_PROVIDER_API = "user_custom_sites"` | L25 | Clone-site overrides | ⏳ Phase 2 | not started |
| `PREFERENCES_NAME = "rebuild_preference"` | L26 | SharedPreferences file name | ✅ | `app/.../data/AppSettings.kt:42` (settings.json) |
| `DataStore.getDefaultSharedPrefs()` | (later) | Default SharedPreferences (settings) | ✅ | replaced by AppSettings |
| `DataStore.getSharedPrefs()` | (later) | App-specific SharedPreferences (data) | ✅ | replaced by JSON files |
| `DataStore.getFolderName()` | (later) | Sanitize folder name for downloads | ⏳ Phase 3 | not started |
| `DataStore.editor()` | (later) | SharedPreferences editor | ❌ Skip | N/A — JSON files |

### 3.2 DataStoreHelper.kt constants
**Source**: `app/src/main/java/com/lagradost/cloudstream3/utils/DataStoreHelper.kt` (841 lines)

| Key constant | Line | Stores | Status | Port File |
|---|---|---|---|---|
| `VIDEO_POS_DUR = "video_pos_dur"` | L50 | Map of url → {position, duration} | ⏳ Phase 1 | not started (high priority) |
| `VIDEO_WATCH_STATE = "video_watch_state"` | L51 | Per-episode watch state | ⏳ Phase 1 | not started |
| `RESULT_WATCH_STATE = "result_watch_state"` | L52 | WatchType per media id (0=none, 1=watching, 2=completed, 3=dropped) | ⏳ Phase 1 | not started |
| `RESULT_WATCH_STATE_DATA = "result_watch_state_data"` | L53 | BookmarkedData[] (full metadata) | ⏳ Phase 1 | not started |
| `RESULT_SUBSCRIBED_STATE_DATA = "result_subscribed_state_data"` | L54 | SubscribedData[] | ⏳ Phase 2 | not started |
| `RESULT_FAVORITES_STATE_DATA = "result_favorites_state_data"` | L55 | FavoritesData[] | ⏳ Phase 1 | not started |
| `RESULT_RESUME_WATCHING = "result_resume_watching_2"` | L56 | ResumeWatchingResult[] | ⏳ Phase 1 | not started |
| `RESULT_RESUME_WATCHING_OLD = "result_resume_watching"` | L57 | Legacy resume list | ❌ Skip | migration only |
| `RESULT_RESUME_WATCHING_HAS_MIGRATED = "result_resume_watching_migrated"` | L58 | Migration flag | ❌ Skip | N/A |
| `RESULT_EPISODE = "result_episode"` | L59 | Last-watched episode per media | ⏳ Phase 1 | not started |
| `RESULT_SEASON = "result_season"` | L60 | Last-watched season per media | ⏳ Phase 1 | not started |
| `RESULT_DUB = "result_dub"` | L61 | Last-selected dub status per anime | ⏳ Phase 1 | not started |
| `KEY_RESULT_SORT = "result_sort"` | L62 | Last-selected sort mode | ⏳ Phase 1 | not started |
| `USER_PINNED_PROVIDERS = "user_pinned_providers"` | L63 | User-pinned provider set | ⏳ Phase 2 | not started |
| `currentAccount: Int` | (later) | Active account index (default 0) | ⏳ Phase 2 | not started |
| `VideoWatchState` data class | (later) | position + duration | ⏳ Phase 1 | not started |
| `BookmarkedData` data class | (later) | Cached bookmarked media | ⏳ Phase 1 | not started |
| `SubscribedData` data class | (later) | Cached subscribed media | ⏳ Phase 2 | not started |
| `FavoritesData` data class | (later) | Cached favorite media | ⏳ Phase 1 | not started |
| `ResumeWatchingResult` data class | (later) | Cached resume-watching entry | ⏳ Phase 1 | not started |
| `setViewPos(url, position, duration)` | (later) | Save playback position | ⏳ Phase 1 | not started |
| `getViewPos(url)` | (later) | Read playback position | ⏳ Phase 1 | not started |
| `getAllBookmarkedData()` / `setBookmarkedData(...)` | (later) | Bookmarks CRUD | ⏳ Phase 1 | not started |
| `getAllFavorites()` / `setFavoritesData(...)` | (later) | Favorites CRUD | ⏳ Phase 1 | not started |
| `getAllSubscriptions()` / `setSubscribedData(...)` | (later) | Subscriptions CRUD | ⏳ Phase 2 | not started |
| `getResultWatchState(id)` / `setResultWatchState(id, type)` | (later) | Watch state per id | ⏳ Phase 1 | not started |
| `getAllWatchStateIds()` | (later) | All ids with non-NONE state | ⏳ Phase 1 | not started |
| `getLastWatched()` | (later) | Last-watched media | ⏳ Phase 1 | not started |
| `getResultEpisode(id)` / `setResultEpisode(...)` | (later) | Episode cursor | ⏳ Phase 1 | not started |
| `getResultSeason(id)` / `setResultSeason(...)` | (later) | Season cursor | ⏳ Phase 1 | not started |
| `getDub(id)` / `setDub(...)` | (later) | Dub status cursor | ⏳ Phase 1 | not started |
| `deleteBookmarkedData(id)` | (later) | Remove bookmark | ⏳ Phase 1 | not started |
| `removeFavoritesData(id)` / `removeSubscribedData(id)` | (later) | Remove favorite/subscription | ⏳ Phase 2 | not started |
| `Account` data class | (later) | keyIndex/name/customImage/defaultImageIndex/lockPin | ⏳ Phase 2 | not started |
| `getAccounts()` / `setAccounts(...)` | (later) | Account list | ⏳ Phase 2 | not started |

### 3.3 Other persistence keys (PluginManager, etc.)

| Key constant | Source | Stores | Status | Port File |
|---|---|---|---|---|
| `PLUGINS_KEY = "PLUGINS_KEY"` | `PluginManager.kt:70` | Online plugin list | ✅ | `app/.../plugin/PluginManager.kt:30` (plugins.json) |
| `PLUGINS_KEY_LOCAL = "PLUGINS_KEY_LOCAL"` | `PluginManager.kt:71` | Local plugin list | ✅ | same |
| `REPOSITORIES_KEY = "REPOSITORIES_KEY"` | `ExtensionsViewModel.kt:43` | Repo URL list | 🚧 | in-memory only, need to persist |
| `PROGRAM_ID_LIST_KEY = "persistent_program_ids"` | `TvChannelUtils.kt:22` | TV channel IDs | ❌ Skip | N/A |
| `PLUGINS_BUNDLE_DATA = "data"` | `PluginsFragment.kt:32` | Bundle key | ❌ Skip | N/A |
| `PLUGINS_BUNDLE_LOCAL = "isLocal"` | `PluginsFragment.kt:33` | Bundle key | ❌ Skip | N/A |
| `LAST_SYNC_API_KEY = "last_sync_api"` | `LibraryViewModel.kt:31` | Last selected sync API | ⏳ Phase 2 | not started |
| `VIDEO_SOURCE_PRIORITY` | `QualityDataHelper.kt:21` | Source priority map | ⏳ Phase 2 | not started |
| `VIDEO_PROFILE_NAME` | `QualityDataHelper.kt:22` | Quality profile names | ⏳ Phase 2 | not started |
| `VIDEO_QUALITY_PRIORITY` | `QualityDataHelper.kt:23` | Per-quality priority | ⏳ Phase 2 | not started |
| `VIDEO_PROFILE_SETTINGS = "video_profile_settings"` | `QualityDataHelper.kt:24` | Profile settings | ⏳ Phase 2 | not started |
| `VIDEO_PROFILE_TYPES_2` | `QualityDataHelper.kt:31` | Profile types (WiFi/Data/Download) | ⏳ Phase 2 | not started |
| `PREFERRED_AUDIO_LANGUAGE_KEY = "preferred_audio_language"` | `CS3IPlayer.kt:127` | Last audio language | ⏳ Phase 1 | not started |
| `AccountManager.ACCOUNT_TOKEN` | `syncproviders/AccountManager.kt` | Account token store | ⏳ Phase 2 | not started |
| `AccountManager.ACCOUNT_IDS` | same | Account IDs | ⏳ Phase 2 | not started |
| `AccountManager.APP_STRING` | same | App scheme for OAuth redirect | ⏳ Phase 2 | not started |
| `ANILIST_CACHED_LIST` | `AniListApi.kt` | Cached AniList library | ⏳ Phase 2 | not started |
| `MAL_CACHED_LIST` | `MALApi.kt` | Cached MAL library | ⏳ Phase 2 | not started |
| `KITSU_CACHED_LIST` | `KitsuApi.kt` | Cached Kitsu library | ⏳ Phase 2 | not started |
| `QUEUE_KEY` | `DownloadQueueManager.kt` | Active download queue | ⏳ Phase 3 | not started |
| `KEY_DOWNLOAD_INFO` / `KEY_RESUME_IN_QUEUE` / `KEY_RESUME_PACKAGES` | `VideoDownloadManager.kt` | Download resume state | ⏳ Phase 3 | not started |

---

## Section 4 — Sync Providers

### 4.1 SyncAPI base classes

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `enum class SyncIdName { Anilist, MyAnimeList, Kitsu, Trakt, Imdb, Simkl, LocalList }` | `library/.../syncproviders/SyncAPI.kt:3-11` | Sync ID type enum | ✅ | vendored |
| `abstract class SyncAPI : AuthAPI()` | `app/.../syncproviders/SyncAPI.kt:22-194` | Sync provider base | ⏳ Phase 2 | not started |
| `SyncAPI.updateStatus(auth, id, status)` | L41-45 | Update watch status | ⏳ Phase 2 | not started |
| `SyncAPI.status(auth, id)` | L50-51 | Get current status | ⏳ Phase 2 | not started |
| `SyncAPI.load(auth, id)` | L56 | Get media metadata | ⏳ Phase 2 | not started |
| `SyncAPI.search(auth, query)` | L61-62 | Search service | ⏳ Phase 2 | not started |
| `SyncAPI.library(auth)` | L67 | Get user library | ⏳ Phase 2 | not started |
| `SyncAPI.urlToId(url)` | L71 | URL → sync ID | ⏳ Phase 2 | not started |
| `SyncAPI.SyncSearchResult` | L73-84 | Search result | ⏳ Phase 2 | not started |
| `SyncAPI.AbstractSyncStatus` / `SyncStatus` | L86-100 | Watch status | ⏳ Phase 2 | not started |
| `SyncAPI.SyncResult` | L102-131 | Media metadata from sync | ⏳ Phase 2 | not started |
| `SyncAPI.Page` / `LibraryMetadata` / `LibraryList` / `LibraryItem` | L133-193 | Library data shapes | ⏳ Phase 2 | not started |
| `supportedWatchTypes: Set<SyncWatchType>` | L31 | Which watch types supported | ⏳ Phase 2 | not started |
| `syncIdName: SyncIdName?` | L36 | Service ID name | ⏳ Phase 2 | not started |
| `requireLibraryRefresh: Boolean` | L26 | Cache invalidation flag | ⏳ Phase 2 | not started |

### 4.2 AuthAPI base class

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `AuthLoginPage(url, payload?)` | `AuthAPI.kt:17-25` | OAuth login page | ⏳ Phase 2 | not started |
| `AuthToken(accessToken, refreshToken, accessTokenLifetime, refreshTokenLifetime, payload)` | L27-56 | Token storage w/ expiry check | ⏳ Phase 2 | not started |
| `AuthUser(name, id, profilePicture, profilePictureHeaders)` | L59-77 | User info | ⏳ Phase 2 | not started |
| `AuthData(user, token)` | L87-91 | Combined user+token | ⏳ Phase 2 | not started |
| `AuthPinData(deviceCode, userCode, verificationUrl, expiresIn, interval)` | L93-102 | PIN login data | ⏳ Phase 2 | not started |
| `AuthLoginRequirement(password, username, email, server)` | L105-110 | Form fields | ⏳ Phase 2 | not started |
| `AuthLoginResponse(...)` | L113-119 | User response | ⏳ Phase 2 | not started |
| `AuthAPI.hasOAuth2/hasPin/hasInApp` flags | L139-145 | Login method flags | ⏳ Phase 2 | not started |
| `AuthAPI.inAppLoginRequirement` | L148 | In-app form requirement | ⏳ Phase 2 | not started |
| `AuthAPI.redirectUrlIdentifier` | L136 | OAuth redirect path | ⏳ Phase 2 | not started |
| `AuthAPI.requiresLogin` | L130 | Service requires auth | ⏳ Phase 2 | not started |
| `AuthAPI.createAccountUrl` | L133 | Sign-up URL | ⏳ Phase 2 | not started |
| `AuthAPI.isValidRedirectUrl(url)` | L192-193 | Check redirect | ⏳ Phase 2 | not started |
| `AuthAPI.login(redirectUrl, payload)` (OAuth2) | L197-198 | OAuth2 login | ⏳ Phase 2 | not started |
| `AuthAPI.loginRequest()` | L202 | Get OAuth2 URL | ⏳ Phase 2 | not started |
| `AuthAPI.pinRequest()` | L206 | Get PIN data | ⏳ Phase 2 | not started |
| `AuthAPI.refreshToken(token)` | L210 | Refresh OAuth2 token | ⏳ Phase 2 | not started |
| `AuthAPI.login(payload: AuthPinData)` | L214 | PIN login | ⏳ Phase 2 | not started |
| `AuthAPI.login(form: AuthLoginResponse)` | L218 | In-app login | ⏳ Phase 2 | not started |
| `AuthAPI.user(token)` | L222 | Get user info | ⏳ Phase 2 | not started |
| `AuthAPI.invalidateToken(token)` | L231 | Revoke token | ⏳ Phase 2 | not started |
| `AuthAPI.generateCodeVerifier()` | L179-187 | RFC 7636 PKCE | ⏳ Phase 2 | not started |
| `AuthAPI.splitRedirectUrl(url)` | L173-177 | Parse redirect | ⏳ Phase 2 | not started |

### 4.3 Concrete sync providers

| Provider | CS3 Source | Login flow | Library sync | Status updates | Search | Status | Port File |
|---|---|---|---|---|---|---|---|
| **MAL** (MyAnimeList) | `syncproviders/providers/MALApi.kt` (688 lines) | OAuth2 + PKCE | ✅ | ✅ | ✅ | ⏳ Phase 2 | not started |
| **AniList** | `syncproviders/providers/AniListApi.kt` (1,241 lines) | OAuth2 (PIN-less) | ✅ | ✅ | ✅ | ⏳ Phase 2 | not started |
| **Kitsu** | `syncproviders/providers/KitsuApi.kt` (803 lines) | In-app (username+password) | ✅ | ✅ | ✅ | ⏳ Phase 2 | not started |
| **Simkl** | `syncproviders/providers/SimklApi.kt` (1,141 lines) | OAuth2 | ✅ | ✅ | ✅ | ⏳ Phase 2 | not started |
| **Trakt** | (referenced in `TraktProvider.kt`) | OAuth2 (PIN-supported) | ✅ | ✅ | ✅ | ⏳ Phase 2 | not started |
| **LocalList** | `syncproviders/providers/LocalList.kt` (91 lines) | None (local data) | ✅ (reads DataStoreHelper) | ✅ | ✅ (via DataStoreHelper) | ⏳ Phase 1 | not started (high priority — backs Library tab) |
| **OpenSubtitles** | `syncproviders/providers/OpenSubtitlesApi.kt` (284 lines) | In-app (username+password) | N/A (subtitle service) | N/A | ✅ (subtitle search) | ⏳ Phase 2 | not started |
| **Addic7ed** | `syncproviders/providers/Addic7ed.kt` (204 lines) | In-app (username+password) | N/A | N/A | ✅ (subtitle search) | ⏳ Phase 3 | not started |
| **SubDL** | `syncproviders/providers/Subdl.kt` (269 lines) | In-app (API key) | N/A | N/A | ✅ (subtitle search) | ⏳ Phase 3 | not started |
| **SubSource** | `syncproviders/providers/SubSource.kt` (204 lines) | None (anonymous) | N/A | N/A | ✅ (subtitle search) | ⏳ Phase 3 | not started |
| **AnimeSkip** | `syncproviders/providers/AnimeSkip.kt` (357 lines) | In-app (username+password) | N/A (video skip service) | N/A | ✅ (skip timestamps) | ⏳ Phase 2 | not started |

### 4.4 Account / repo classes

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `AccountManager` | `syncproviders/AccountManager.kt` | Manage sync + subtitle providers, account state | ⏳ Phase 2 | not started |
| `AccountManager.syncApis: List<SyncAPI>` | same | Registered sync providers | ⏳ Phase 2 | not started |
| `AccountManager.subtitleApis: List<SubtitleAPI>` | same | Registered subtitle providers | ⏳ Phase 2 | not started |
| `AccountManager.ACCOUNT_TOKEN` / `ACCOUNT_IDS` / `APP_STRING` | same | Persistence keys | ⏳ Phase 2 | not started |
| `AuthRepo` / `SyncRepo` / `SubtitleRepo` | `syncproviders/*Repo.kt` | Repository wrappers | ⏳ Phase 2 | not started |
| `BackupAPI` | `syncproviders/BackupAPI.kt` | Backup service hook | ⏳ Phase 3 | not started |

---

## Section 5 — Meta-providers

**Source dir**: `library/src/commonMain/kotlin/com/lagradost/cloudstream3/metaproviders/`

| Provider | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `TmdbProvider` | `metaproviders/TmdbProvider.kt` (521 lines) | TMDB metadata API; hardcoded API key `e6333b32409e02a4a6eba6fb7ff866bb`; search/load/getMainPage from TMDB, then defers loadLinks to the underlying site provider by matching IMDb/TMDB IDs | 🚧 | `library` vendored; needs TMDB API client wiring (Phase 1) |
| `TraktProvider` | `metaproviders/TraktProvider.kt` (467 lines) | Trakt metadata; uses `BuildConfig.TRAKT_CLIENT_ID`; aggregates Trakt lists into home page | ⏳ Phase 2 | not started |
| `MyDramaListAPI` (in `MyDramaList.kt`) | `metaproviders/MyDramaList.kt` (456 lines) | MyDramaList metadata; uses `BuildConfig.MDL_API_KEY`; Asian drama focus | ⏳ Phase 2 | not started |
| `CrossTmdbProvider` | `metaproviders/CrossTmdbProvider.kt` (125 lines) | Aggregator: searches all providers in parallel, matches by TMDB ID, merges results | ⏳ Phase 2 | not started |
| `SyncRedirector` | `metaproviders/SyncRedirector.kt` (53 lines) | Translates MAL/AniList/IMDb URLs to provider URLs (e.g. `mal://1` → `provider.com/anime/mal/1`) | ✅ | vendored |
| `TmdbLink` data class | `metaproviders/TmdbProvider.kt` | TMDB link wrapper | ✅ | vendored |
| `TmdbSearchResult` / `TmdbPageResult` / `TmdbMultiResult` / `TmdbMovieDetail` / `TmdbTvDetail` / `TmdbSeasonDetail` / `TmdbEpisode` / `TmdbVideo` / `TmdbVideos` / `TmdbGenre` / `TmdbCastMember` / `TmdbCredits` / `TmdbContentRating` / `TmdbContentRatings` / `TmdbReleaseDates` / `TmdbReleaseDateEntry` / `TmdbReleaseDateResult` / `TmdbSeasonSummary` / `TmdbIds` (all @Serializable) | same | TMDB API response shapes | ✅ | vendored (compiles on JVM) |
| `TraktProvider.MediaSummary` / `MediaDetails` / `Person` / `People` / `Cast` / `Airs` / `Seasons` / `TraktEpisode` / `Ids` / `Images` / `LinkData` | `metaproviders/TraktProvider.kt` | Trakt API response shapes | ✅ | vendored |
| `MyDramaListAPI.Media` / `MediaSummary` / `Data` / `Source` / `Tag` / `Genre` / `Credits` / `Crew` / `Cast` / `Images` / `Trailer` / `TrailerDetails` / `TrailerNode` / `TrailerRoot` / `ShowEpisode` / `ShowEpisodesItem` / `LinkData` | `metaproviders/MyDramaList.kt` | MDL API response shapes | ✅ | vendored |
| `CrossTmdbProvider.CrossMetaData` | `metaproviders/CrossTmdbProvider.kt` | Cross-provider metadata | ✅ | vendored |

---

## Section 6 — Network / Scraping Utilities

**Source dir**: `library/src/commonMain/kotlin/com/lagradost/cloudstream3/utils/`

| Utility | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `AppUtils` object | `AppUtils.kt` (99 lines) | `toJson`, `tryParseJson`, `parseJson` extensions | ✅ | vendored |
| `Coroutines` object | `Coroutines.kt` (82 lines) | `mainWork`, `ioWork`, `ioSafe`, `ioWorkSafe`, `main`, `atomicListOf`, `runOnMainThread`, `runOnIoDispatcher` | ✅ | vendored (JVM Main dispatcher exists) |
| `AtomicList` / `AtomicMutableList` | `AtomicList.kt` (59 lines) | Thread-safe list with `withLock { ... }` | ✅ | vendored |
| `ExtractorApi` + 310 extractors | `ExtractorApi.kt` (1,473 lines) | (see §1.2 and §12) | ✅ | vendored (compiles on JVM) |
| `M3u8Helper` (legacy) | `M3u8Helper.kt` (362 lines) | Old HLS parser | ✅ | vendored |
| `M3u8Helper2` object | `M3u8Helper.kt:41+` | Modern HLS parser using `HlsPlaylistParser` | ✅ | vendored |
| `HlsPlaylistParser` object | `HlsPlaylistParser.kt` (2,084 lines) | Full HLS m3u8 parser (master/media playlists, AES-128 key fetch, EXT-X-STREAM-INF, EXT-X-MEDIA, EXT-X-KEY) | ✅ | vendored (compiles on JVM, used by HLS downloads) |
| `SubtitleHelper` object | `SubtitleHelper.kt` (722 lines) | Language code → IETF tag mapping; `fromCodeToLangTagIETF`, `fromLanguageToTagIETF`, `fromTagToLanguageName` | ✅ | vendored |
| `SubtitleHelperPlatform` expect | `SubtitleHelperPlatform.kt` (12 lines) | Platform-specific subtitle helper | ✅ | vendored (jvm + android actuals) |
| `UnshortenUrl` / `ShortLink` object | `UnshortenUrl.kt` (192 lines) | Unshorten adfly / linkup / linksafe / nuovoindirizzo / nuovolink / uprot / davisonbarker / isecure | ✅ | vendored |
| `Levenshtein` object | `Levenshtein.kt` (514 lines) | String similarity (`ratio`, `partialRatio`, `distance`) | ✅ | vendored |
| `JsUnpacker` | `JsUnpacker.kt` (152 lines) | Dean Edwards packed JS unpacker | ✅ | vendored |
| `JsHunter` | `JsHunter.kt` (80 lines) | Hunter.io-style obfuscated JS unpacker | ✅ | vendored |
| `JsInterpreter` | `JsInterpreter.kt` (1,627 lines) | Mini JS interpreter for obfuscated extractors (yt-dlp style) | ✅ | vendored |
| `StringUtils` | `StringUtils.kt` (30 lines) | `decodeUrl`, misc string helpers | ✅ | vendored |
| `AppDebug` | `AppDebug.kt` (10 lines) | Debug helpers | ✅ | vendored |
| `serializers/NonEmptySerializer` | `serializers/NonEmptySerializer.kt` | Skip empty collections | ✅ | vendored |
| `serializers/FloatAsIntSerializer` | `serializers/FloatAsIntSerializer.kt` | Float ↔ Int serialization | ✅ | vendored |
| `serializers/FloatAsLongSerializer` | `serializers/FloatAsLongSerializer.kt` | Float ↔ Long | ✅ | vendored |
| `serializers/NullableStringSerializer` | `serializers/NullableStringSerializer.kt` | Allow null strings | ✅ | vendored |
| `serializers/WriteOnlySerializer` | `serializers/WriteOnlySerializer.kt` | Write-only field (skip on read) | ✅ | vendored |
| `serializers/JsonTransformSerializer` | `serializers/JsonTransformSerializer.kt` | JSON transforms | ✅ | vendored |
| `extractors/helper/JWPlayerHelper` | `helper/JWPlayerHelper.kt` | Parse JWPlayer config | ✅ | vendored |
| `extractors/helper/WcoHelper` | `helper/WcoHelper.kt` | WCO-specific helpers | ✅ | vendored |
| `extractors/helper/NineAnimeHelper` | `helper/NineAnimeHelper.kt` | 9Anime helpers | ✅ | vendored |
| `extractors/helper/GogoHelper` | `helper/GogoHelper.kt` | GogoAnime helpers | ✅ | vendored |
| `extractors/helper/VstreamhubHelper` | `helper/VstreamhubHelper.kt` | VStreamHub helpers | ✅ | vendored |
| `extractors/helper/AesHelper` | `helper/AesHelper.kt` | AES encryption/decryption | ✅ | vendored |
| `extractors/helper/CryptoJSHelper` | `helper/CryptoJSHelper.kt` | CryptoJS-compatible AES | ✅ | vendored |
| `extractors/helper/AsianEmbedHelper` | `helper/AsianEmbedHelper.kt` | AsianEmbed helpers | ✅ | vendored |
| `WebViewResolver` (expect) | `network/WebViewResolver.kt` | Cloudflare/DdosGuard bypass via WebView | 🚧 | `app/.../network/WebViewResolverJvm.kt` (stub — needs JCEF in Phase 2) |
| `CloudflareKiller` | `app/.../network/CloudflareKiller.kt` | Cloudflare bypass (uses WebView) | ⏳ Phase 2 | not started |
| `DdosGuardKiller` | `app/.../network/DdosGuardKiller.kt` | DdosGuard bypass | ⏳ Phase 2 | not started |
| `DohProviders` | `app/.../network/DohProviders.kt` | DNS-over-HTTPS providers list | ⏳ Phase 2 | not started |
| `RequestsHelper` | `app/.../network/RequestsHelper.kt` | OkHttp setup (Conscrypt, DNS, cache) | 🚧 | use standard OkHttp (no Conscrypt needed on JVM) |
| `app.get/post/head` (NiceHttp) | (library) | HTTP wrapper over OkHttp | ✅ | vendored |
| `mvvm/ArchComponentExt` | `mvvm/ArchComponentExt.jvmCommon.kt` | ViewModel helpers | ✅ | vendored (where applicable) |

---

## Section 7 — Player Pipeline

### 7.1 Player engine

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| ExoPlayer wrapper | `CS3IPlayer.kt` (2,067 lines) | Wraps `androidx.media3.ExoPlayer` | ❌ Skip | replaced by vlcj |
| vlcj player | (port-original) | Wraps libvlc | ✅ | `app/.../player/VlcjPlayer.kt` (183 lines) |
| HLS playback (.m3u8) | ExoPlayer HLS support | Native | ✅ | vlcj handles natively |
| MP4 playback | ExoPlayer | Native | ✅ | vlcj |
| MKV playback | ExoPlayer + UpdatedMatroskaExtractor | Native | ✅ | vlcj |
| WebM playback | ExoPlayer | Native | ✅ | vlcj |
| DASH playback (.mpd) | ExoPlayer DashMediaSource | Native | ✅ | vlcj |
| AVI/FLV/WMV playback | ExoPlayer | Native | ✅ | vlcj (better than ExoPlayer) |
| Widevine DRM | ExoPlayer DefaultDrmSessionManager | Native | ❌ Skip | vlcj cannot decrypt Widevine |
| PlayReady DRM | ExoPlayer | Native | ❌ Skip | vlcj cannot |
| ClearKey DRM | ExoPlayer | Native | ⏳ Phase 3 | not started (low priority) |
| HTTP headers (referer, custom) | ExoPlayer OkHttpDataSource | ✅ | ✅ | vlcj via `:http-referrer=` and `:http-header=` options |
| Audio tracks (multi-language) | ExoPlayer TrackSelector | ✅ | ⏳ Phase 1 | vlcj supports, not yet exposed in UI |
| Subtitle tracks (embedded) | ExoPlayer TextRenderer | ✅ | ⏳ Phase 1 | vlcj supports |
| Subtitle tracks (external URL) | SingleSampleMediaSource | ✅ | ✅ | vlcj `setSubTitleFile()` |
| Subtitle offset (±ms) | SubtitleOffsetItemAdapter | ✅ | ⏳ Phase 2 | not started |
| Subtitle styling | SubtitleView + SaveCaptionStyle | ✅ | ⏳ Phase 2 | not started |
| Video size change events | VideoSizeChanged event | ✅ | ⏳ Phase 1 | vlcj `onVideoSizeChanged` listener (not wired) |
| Seek | ExoPlayer seekTo | ✅ | ✅ | `app/.../player/VlcjPlayer.kt:140-142` |
| Playback speed | ExoPlayer setPlaybackSpeed | ✅ | ✅ | `app/.../player/VlcjPlayer.kt:150-153` |
| Volume | ExoPlayer setVolume | ✅ | ✅ | `app/.../player/VlcjPlayer.kt:144-148` |
| Buffer size (disk) | SimpleCache + LRU evictor | ✅ | ⏳ Phase 2 | vlcj uses its own cache |
| Buffer size (RAM) | DefaultLoadControl | ✅ | ⏳ Phase 2 | vlcj uses its own |
| Buffer length (time) | DefaultLoadControl | ✅ | ⏳ Phase 2 | vlcj uses its own |
| Preview thumbnails | PreviewGenerator (FFmpeg) | ✅ | ⏳ Phase 3 | vlcj supports snapshots |
| Cronet engine (faster HTTP) | CronetDataSource | ✅ | ❌ Skip | OkHttp is sufficient on JVM |
| Picture-in-picture | PlayerPipHelper | ✅ | ❌ Skip | N/A on desktop |
| Media notification | PlayerNotificationManager | ✅ | ❌ Skip | replaced by MPRIS later |
| SSL bypass | SSLTrustManager | ✅ | ⏳ Phase 2 | not started |
| Dynamic range compressor | DynamicRangeCompressor | ✅ | ⏳ Phase 3 | not started |
| Background audio (play audio with screen off) | `isAudioOnlyBackground` | ✅ | ⏳ Phase 3 | not started |
| Concatenating media (ExtractorLinkPlayList) | ConcatenatingMediaSource | ✅ | ⏳ Phase 3 | not started |

### 7.2 Link generation & caching

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `RepoLinkGenerator` | `ui/player/RepoLinkGenerator.kt` (160 lines) | Generates links from a `ResultEpisode` via `APIRepository.loadLinks()` | ⏳ Phase 1 | not started (high priority) |
| `RepoLinkGenerator.cache` (HashMap<Pair<String, Int>, Cache>) | L30-32 | 20-minute TTL cache keyed by (apiName, id) | ⏳ Phase 1 | not started |
| `Cache(linkCache, subtitleCache, lastCachedTimestamp, saturated)` | L15-22 | Cache data shape | ⏳ Phase 1 | not started |
| `ExtractorLinkGenerator` | `ui/player/ExtractorLinkGenerator.kt` | Generate from raw ExtractorLinks | ⏳ Phase 1 | not started |
| `DownloadFileGenerator` | `ui/player/DownloadFileGenerator.kt` | Generate from downloaded files | ⏳ Phase 3 | not started |
| `VideoGenerator<T>` abstract | `ui/player/VideoGenerator.kt` (referenced) | Generator abstraction | ⏳ Phase 1 | not started |
| `IGenerator` interface | `ui/player/IGenerator.kt` | Generator contract | ⏳ Phase 1 | not started |
| `LinkGenerator` | `ui/player/LinkGenerator.kt` | Common base | ⏳ Phase 1 | not started |
| `LOADTYPE_ALL` / `LOADTYPE_INAPP` / `LOADTYPE_CHROMECAST` / `LOADTYPE_INAPP_DOWNLOAD` | `ui/player/*.kt` | Link-type filter sets | ⏳ Phase 1 | vendored |
| Concurrent link loading (amap across providers) | `ResultViewModel2.kt` | Parallel `loadLinks` calls | ⏳ Phase 1 | not started |
| Link dedup (by URL) | `RepoLinkGenerator.kt:67` | Skip duplicate URLs | ⏳ Phase 1 | not started |
| Subtitle dedup (by URL) | `RepoLinkGenerator.kt:115` | Skip duplicate subs | ⏳ Phase 1 | not started |
| Subtitle name uniqueness (suffix counter) | `RepoLinkGenerator.kt:122-126` | "English 1", "English 2" | ⏳ Phase 1 | not started |

### 7.3 Source priority / quality profiles

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `QualityDataHelper` object | `source_priority/QualityDataHelper.kt` (266 lines) | Source priority storage | ⏳ Phase 2 | not started |
| `QualityProfileType { None, WiFi, Data, Download }` | L47-52 | Profile type enum | ⏳ Phase 2 | not started |
| `QualityProfile(name, id, types)` | L54-58 | Profile data | ⏳ Phase 2 | not started |
| `QualityDataHelper.getSourcePriority(profile, name)` | L64-75 | Get priority for source | ⏳ Phase 2 | not started |
| `QualityDataHelper.getAllSourcePriorityNames(profile)` | L77+ | List all named sources | ⏳ Phase 2 | not started |
| `QualityDataHelper.AUTO_SKIP_PRIORITY = 10` | L37 | Skip loading after this priority | ⏳ Phase 2 | not started |
| `QualityProfileDialog` | `source_priority/QualityProfileDialog.kt` (151 lines) | Profile editor | ⏳ Phase 2 | not started |
| `SourcePriorityDialog` | `source_priority/SourcePriorityDialog.kt` (105 lines) | Per-source priority | ⏳ Phase 2 | not started |
| `SourceProfileSettingsDialog` | `source_priority/SourceProfileSettingsDialog.kt` (47 lines) | Settings entry | ⏳ Phase 2 | not started |
| `PriorityAdapter` | `source_priority/PriorityAdapter.kt` (51 lines) | List adapter | ❌ Skip | Compose |
| `ProfilesAdapter` | `source_priority/ProfilesAdapter.kt` (136 lines) | Profile list adapter | ❌ Skip | Compose |

### 7.4 Video skip (skip-op)

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `VideoSkipStamp` data class | `utils/videoskip/SkipAPI.kt` (105 lines) | Skip timestamp (start, end, type) | ⏳ Phase 2 | not started |
| `SkipAPI` object | `utils/videoskip/SkipAPI.kt` | Base skip API | ⏳ Phase 2 | not started |
| `AniSkip` | `utils/videoskip/AniSkip.kt` (73 lines) | AniSkip API (anime skip op/ed) | ⏳ Phase 2 | not started |
| `AnimeSkip` | `utils/videoskip/AnimeSkip.kt` (357 lines) | AnimeSkip API (auth required) | ⏳ Phase 2 | not started |
| `IntroDbSkip` | `utils/videoskip/IntroDbSkip.kt` (80 lines) | IntroDB API | ⏳ Phase 2 | not started |
| `TheIntroDBSkip` | `utils/videoskip/TheIntroDBSkip.kt` (71 lines) | TheIntroDB API | ⏳ Phase 2 | not started |
| `FillerEpisodeCheck` | `utils/FillerEpisodeCheck.kt` | Filler episode lookup (anime) | ⏳ Phase 2 | not started |
| Skip-Op button (player overlay) | `GeneratorPlayer.kt` | Show "Skip OP" button | ⏳ Phase 2 | not started |
| Skip-Ed button | `GeneratorPlayer.kt` | Show "Skip ED" button | ⏳ Phase 2 | not started |
| Skip-Recap button | `GeneratorPlayer.kt` | Show "Skip Recap" button | ⏳ Phase 2 | not started |
| Auto-skip OP | (in GeneratorPlayer) | Auto-advance past OP | ⏳ Phase 3 | not started |
| Filler marking (episode list) | (in ResultViewModel2) | Mark filler episodes | ⏳ Phase 2 | not started |

### 7.5 Torrent playback

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `Torrent.kt` | `ui/player/Torrent.kt` (351 lines) | Torrent playback via torrentserver lib | ⏳ Phase 3 | not started |
| Magnet link handling | `TorrentLoadResponse.magnet` | magnet: URI | ⏳ Phase 3 | not started |
| .torrent file handling | `TorrentLoadResponse.torrent` | .torrent file URL | ⏳ Phase 3 | not started |
| External torrent apps (Aria2, BiglyBT, LibreTorrent, etc.) | `actions/temp/*Package.kt` | Launch external torrent client | ❌ Skip | N/A on desktop |

### 7.6 Live streams

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `LiveHelper` | `ui/player/live/LiveHelper.kt` (77 lines) | Live stream seeking | ⏳ Phase 3 | not started |
| `LiveManager` | `ui/player/live/LiveManager.kt` (96 lines) | Live stream state | ⏳ Phase 3 | not started |
| `LivePreviewTimeBar` | `ui/player/live/LivePreviewTimeBar.kt` (37 lines) | Live preview | ⏳ Phase 3 | not started |
| `PREFERRED_LIVE_OFFSET` constant | (in live/) | Default seek-back offset | ⏳ Phase 3 | not started |
| Live stream sliding window | (in LiveHelper) | Show "live edge" indicator | ⏳ Phase 3 | not started |

### 7.7 Player actions (external apps + cast)

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `VideoClickAction` abstract | `actions/VideoClickAction.kt` (204 lines) | Base action | ⏳ Phase 2 | not started |
| `AlwaysAskAction` | `actions/AlwaysAskAction.kt` (26 lines) | Prompt user | ❌ Skip | replaced by Compose dialog |
| `OpenInAppAction` | `actions/OpenInAppAction.kt` (134 lines) | Open in another app | ❌ Skip | N/A on desktop |
| `CopyClipboardAction` | `actions/temp/CopyClipboardAction.kt` | Copy URL to clipboard | ⏳ Phase 2 | not started |
| `PlayInBrowserAction` | `actions/temp/PlayInBrowserAction.kt` | Open in system browser | ⏳ Phase 2 | not started |
| `PlayMirrorAction` | `actions/temp/PlayMirrorAction.kt` | Play mirror | ⏳ Phase 2 | not started |
| `ViewM3U8Action` | `actions/temp/ViewM3U8Action.kt` | View raw m3u8 | ⏳ Phase 2 | not started |
| `CloudStreamPackage` | `actions/temp/CloudStreamPackage.kt` | Play in CS3 itself | ❌ Skip | N/A |
| `VlcPackage` | `actions/temp/VlcPackage.kt` | Open in VLC | ⏳ Phase 2 | not started |
| `MpvPackage` / `MpvKtPackage` / `MpvRxPackage` | `actions/temp/Mpv*.kt` | Open in MPV | ⏳ Phase 2 | not started |
| `JustPlayerPackage` / `NextPlayerPackage` / `OnlyPlayer` | `actions/temp/*.kt` | Other Android players | ❌ Skip | N/A on desktop |
| `Aria2Package` / `BiglyBTPackage` / `LibreTorrentPackage` | `actions/temp/*.kt` | Torrent clients | ❌ Skip | N/A |
| `WebVideoCastPackage` | `actions/temp/WebVideoCastPackage.kt` | Cast app | ❌ Skip | replaced by Fcast |

---

## Section 8 — Download Manager

### 8.1 Core download classes

**Source dir**: `app/src/main/java/com/lagradost/cloudstream3/utils/downloader/`

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `VideoDownloadManager` | `DownloadManager.kt` (2,095 lines) | Core download engine | ⏳ Phase 3 | not started |
| `DownloadObjects` | `DownloadObjects.kt` (251 lines) | Download data classes (see below) | ⏳ Phase 3 | not started |
| `DownloadFileManagement` | `DownloadFileManagement.kt` (131 lines) | File ops (sanitize, etc.) | ⏳ Phase 3 | not started |
| `DownloadQueueManager` | `DownloadQueueManager.kt` (249 lines) | Queue state + `QUEUE_KEY` | ⏳ Phase 3 | not started |
| `DownloadUtils` | `DownloadUtils.kt` (164 lines) | Misc helpers | ⏳ Phase 3 | not started |
| `VideoDownloadManager.DownloadType` enum | (in DownloadManager.kt) | Download kind (Movie/Episode/etc.) | ⏳ Phase 3 | not started |
| `VideoDownloadManager.KEY_DOWNLOAD_INFO` | (in DownloadManager.kt) | Download info persistence | ⏳ Phase 3 | not started |
| `VideoDownloadManager.KEY_RESUME_IN_QUEUE` | (in DownloadManager.kt) | Resume queue persistence | ⏳ Phase 3 | not started |
| `VideoDownloadManager.KEY_RESUME_PACKAGES` | (in DownloadManager.kt) | Resume packages persistence | ⏳ Phase 3 | not started |
| `VideoDownloadManager.setupStream(...)` | (in DownloadManager.kt) | Setup output stream | ⏳ Phase 3 | not started |
| `VideoDownloadManager.setupBackupStream(...)` | (in DownloadManager.kt) | Setup backup stream | ⏳ Phase 3 | not started |

### 8.2 Download data classes

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `DownloadQueueWrapper(resumePackage, downloadItem)` | `DownloadObjects.kt:27-48` | Queue entry | ⏳ Phase 3 | not started |
| `DownloadQueueItem(episode, isMovie, resultName, resultType, resultPoster, apiName, resultId, resultUrl, links, subs)` | L52-67 | New download | ⏳ Phase 3 | not started |
| `DownloadEpisodeCached(name, poster, episode, season, parentId, score, description, cacheTime, id)` | L77-106 | Episode metadata cache | ⏳ Phase 3 | not started |
| `DownloadHeaderCached(apiName, url, type, name, poster, cacheTime, id)` | L110-118 | Show/movie metadata cache | ⏳ Phase 3 | not started |
| `DownloadResumePackage(item, linkIndex)` | L121-129 | Resume info | ⏳ Phase 3 | not started |
| `DownloadItem(source, folder, ep, links)` | L132-137 | Single download | ⏳ Phase 3 | not started |
| `DownloadEpisodeMetadata(id, parentId, mainName, sourceApiName, poster, name, season, episode, type)` | L141-151 | Episode display info | ⏳ Phase 3 | not started |
| `DownloadedFileInfo(totalBytes, relativePath, displayName, extraInfo, basePath, linkHash)` | L154-162 | File metadata | ⏳ Phase 3 | not started |
| `DownloadedFileInfoResult(fileLength, totalBytes, path)` | L166-172 | File info w/ Uri | ⏳ Phase 3 | not started |
| `ResumeWatching(parentId, episodeId, episode, season, updateTime, isFromDownload)` | L175-182 | Resume list entry | ⏳ Phase 3 | not started |
| `DownloadStatus(retrySame, tryNext, success)` | L184-191 | Download result | ⏳ Phase 3 | not started |
| `CreateNotificationMetadata(type, bytesDownloaded, bytesTotal, hlsProgress, hlsTotal, bytesPerSecond)` | L193-200 | Progress metadata | ⏳ Phase 3 | not started |
| `StreamData(fileLength, file: SafeFile)` | L202-224 | Output stream wrapper | ⏳ Phase 3 | not started |
| `LazyStreamDownloadResponse(bytes, startByte, endByte)` | L231-250 | HLS segment download | ⏳ Phase 3 | not started |
| `DownloadCached` interface | L69-71 | id field | ⏳ Phase 3 | not started |

### 8.3 HLS download

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `LazyHlsDownloadData` | (in DownloadManager.kt) | Lazy HLS segment download | ⏳ Phase 3 | not started |
| AES-128 decryption (HLS) | HlsPlaylistParser + DownloadManager | Decrypt HLS encrypted segments | ⏳ Phase 3 | not started |
| Segment concatenation | (in DownloadManager.kt) | Merge .ts segments | ⏳ Phase 3 | not started |
| Resume partial download | (in DownloadManager.kt) | HTTP Range header + file size check | ⏳ Phase 3 | not started |
| Concurrent segment downloads | (in DownloadManager.kt) | Parallel .ts downloads | ⏳ Phase 3 | not started |

### 8.4 Download queue UI

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `DownloadFragment` (list of downloaded titles) | `ui/download/DownloadFragment.kt` | Main download screen | ⏳ Phase 3 | not started |
| `DownloadChildFragment` (single title's episodes) | `ui/download/DownloadChildFragment.kt` | Episode list | ⏳ Phase 3 | not started |
| `DownloadButtonSetup` | `ui/download/DownloadButtonSetup.kt` | Download button states (Downloaded/Downloading/Failed) | ⏳ Phase 3 | not started |
| `PieFetchButton` | `ui/download/button/PieFetchButton.kt` | Circular progress button | ❌ Skip | Compose custom |
| `DownloadButton` / `BaseFetchButton` / `ProgressBarAnimation` | `ui/download/button/*.kt` | Button widgets | ❌ Skip | Compose |
| `DownloadQueueFragment` (active queue) | `ui/download/queue/DownloadQueueFragment.kt` | Queue UI | ⏳ Phase 3 | not started |
| `DownloadQueueAdapter` / `DownloadQueueViewModel` | `ui/download/queue/*.kt` | Queue adapter/state | ❌ Skip / ⏳ Phase 3 | Compose |
| `DownloadViewModel` | `ui/download/DownloadViewModel.kt` | Download list state | ⏳ Phase 3 | not started |
| `DownloadAdapter` | `ui/download/DownloadAdapter.kt` | List adapter | ❌ Skip | Compose |
| Download progress notification | (in DownloadManager.kt) | Android notification | ❌ Skip | replaced by desktop notification (Phase 3) |
| Pause / resume / cancel | (in DownloadManager.kt) | Queue controls | ⏳ Phase 3 | not started |
| Auto-download (AutoDownloadMode) | `AutoDownloadMode` enum | Disable/FilterByLang/All/NsfwOnly | ⏳ Phase 3 | not started |

---

## Section 9 — Cast / Chromecast (replaced by Fcast)

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| Google Cast session management | `utils/CastHelper.kt` | Cast SDK session | ❌ Skip | Fcast replaces (Phase 3) |
| MediaInfo builder | `utils/CastHelper.kt` | Build cast MediaInfo | ❌ Skip | Fcast replaces |
| Cast subtitle styling | `ui/subtitles/ChromecastSubtitlesFragment2.kt` | Cast subtitle colors | ❌ Skip | Fcast replaces |
| Cast button in player | (in FullScreenPlayer) | Cast toggle | ❌ Skip | Fcast replaces |
| Mini controller | `ui/MiniControllerFragment.kt` | Mini cast bar | ❌ Skip | Fcast replaces |
| Expanded controller | `ui/ControllerActivity.kt` | Full cast controls | ❌ Skip | Fcast replaces |
| `CastOptionsProvider` | `utils/CastOptionsProvider.kt` | Cast SDK config | ❌ Skip | Fcast replaces |
| `FcastAction` | `actions/temp/fcast/FcastAction.kt` (69 lines) | Fcast action | ⏳ Phase 3 | not started |
| `FcastManager` | `actions/temp/fcast/FcastManager.kt` (194 lines) | Fcast session | ⏳ Phase 3 | not started |
| `FcastSession` | `actions/temp/fcast/FcastSession.kt` (59 lines) | TCP session | ⏳ Phase 3 | not started |
| `Packets` (Fcast protocol) | `actions/temp/fcast/Packets.kt` (66 lines) | Fcast packet types | ⏳ Phase 3 | not started |
| UPnP/DLNA | (not in CS3) | N/A | ⏳ Phase 3 | not started (Cling library) |

---

## Section 10 — Settings (Exhaustive)

### 10.1 General settings (`settings_general.xml`, 117 lines)

| Setting | XML Key | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|---|
| App language | `locale_key` / `app_locale` | SettingsGeneral.kt | Pick app locale | ⏳ Phase 2 | `AppSettings.data.locale` (default `en-US`) |
| Legal notice | `legal_notice_key` | SettingsGeneral.kt | Show legal disclaimer | ⏳ Phase 2 | not started |
| Benene count | `benene_count` | SettingsGeneral.kt + Globals | Easter egg counter | ⏳ Phase 3 | `AppSettings.data.bananas` (renamed) |
| Download path | `download_path_key` | SettingsGeneral.kt | Where to save downloads | ⏳ Phase 3 | `AppSettings.data.downloadPath` |
| Parallel downloads | `download_parallel_key` (default 3, max 10) | SettingsGeneral.kt | Concurrent downloads | ⏳ Phase 3 | `AppSettings.data.parallelDownloads` |
| Concurrent connections | `download_concurrent_key` (default 3, max 10) | SettingsGeneral.kt | Connections per download | ⏳ Phase 3 | not in AppSettings yet |
| Battery optimization | `battery_optimisation_key` | SettingsGeneral.kt | Whitelist app from battery | ❌ Skip | N/A on desktop |
| Add site (override) | `override_site_key` | SettingsGeneral.kt | Add clone-site override | ⏳ Phase 2 | not started |
| DNS provider | `dns_key` | SettingsGeneral.kt | Custom DoH DNS | ⏳ Phase 2 | not started |
| jsdelivr proxy | `jsdelivr_proxy_key` | SettingsGeneral.kt | Use jsdelivr to bypass block | ⏳ Phase 2 | not started |
| GitHub link | (intent) | settings_general.xml | Open github.com/recloudstream/cloudstream | ⏳ Phase 2 | not started |
| Lightnovel link | (intent) | settings_general.xml | Open QuickNovel repo | ❌ Skip | N/A |
| Discord link | (intent) | settings_general.xml | Open Discord | ⏳ Phase 2 | not started |
| Wiki link | (intent) | settings_general.xml | Open cloudstream.miraheze.org | ⏳ Phase 2 | not started |

### 10.2 UI settings (`settings_ui.xml`, 113 lines)

| Setting | XML Key | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|---|
| Primary color | `primary_color_key` | SettingsUI.kt | Color picker (22 colors) | 🚧 | `AppSettings.data.primaryColor` (default `#B388FF`); full set in `shared/.../theme/Palette.kt:5-28` |
| App theme | `app_theme_key` | SettingsUI.kt | Theme picker (9 modes) | 🚧 | `AppSettings.data.theme` (default `dark`); full set in `shared/.../theme/Theme.kt:16-46` |
| App layout | `app_layout_key` | SettingsUI.kt | Phone/TV/Emulator | ❌ Skip | N/A on desktop |
| Bottom title (poster) | `bottom_title_key` (default true) | SettingsUI.kt | Show title below poster | ⏳ Phase 1 | not in AppSettings |
| Overscan (TV) | `overscan_key` (default 0, max 100) | SettingsUI.kt | TV overscan | ❌ Skip | N/A |
| Poster size | `poster_size_key` (default 0, max 15) | SettingsUI.kt | Poster size multiplier | ⏳ Phase 1 | not in AppSettings |
| Poster UI (sub/dub/rating badges) | `poster_ui_key` | SettingsUI.kt | Toggle badges | ⏳ Phase 1 | not in AppSettings (UIPreferences: `posterShowHd/Dub/Sub/Rating/Title/Episode`) |
| Advanced search | `advanced_search` (default true) | SettingsUI.kt | Group results by provider | ⏳ Phase 1 | `AppSettings.data.advancedSearch` (in shared UIPreferences) |
| Search suggestions | `search_suggestions_enabled` (default true) | SettingsUI.kt | Autocomplete | ⏳ Phase 2 | `AppSettings.data.searchSuggestions` |
| Show trailers | `show_trailers_key` (default true) | SettingsUI.kt | Fetch/play trailers | ⏳ Phase 2 | `AppSettings.data.trailersEnabled` |
| Show Kitsu posters | `show_kitsu_posters_key` (default true) | SettingsUI.kt | Use Kitsu art for anime | ⏳ Phase 2 | `AppSettings.data.kitsuPostersEnabled` |
| Show cast in details | `show_cast_in_details_key` (default true) | SettingsUI.kt | Display cast | ⏳ Phase 2 | `AppSettings.data.castEnabled` |
| Show fillers | `show_fillers_key` (default false) | SettingsUI.kt | Mark filler episodes | ⏳ Phase 2 | `AppSettings.data.fillersEnabled` |
| Show player metadata overlay | `show_player_metadata_key` (default true) | SettingsUI.kt | Player overlay info | ⏳ Phase 2 | `AppSettings.data.showMetadataOverlay` |
| TV layout clock | `tv_layout_clock_key` (default false) | SettingsUI.kt | Show clock in TV layout | ❌ Skip | N/A |
| Random button | `random_button_key` (default false) | SettingsUI.kt | Show "random" button | ⏳ Phase 2 | `AppSettings.data.randomButtonEnabled` |
| Confirm exit | `confirm_exit_key` | SettingsUI.kt | Confirm before exiting | ⏳ Phase 2 | `AppSettings.data.confirmExit` |
| Filter search quality | `pref_filter_search_quality_key` / `pref_filter_search_quality_key2` | SettingsUI.kt | Filter by quality (Cam, HD, 4K, ...) | ⏳ Phase 2 | `AppSettings.data.filterQuality` |
| Sub badge display | `show_sub_key` (in UIPreferences) | SettingsUI | Show "SUB" badge | ⏳ Phase 1 | in `UIPreferences.posterShowSub` |
| Dub badge display | `show_dub_key` (in UIPreferences) | SettingsUI | Show "DUB" badge | ⏳ Phase 1 | in `UIPreferences.posterShowDub` |
| HD badge display | `show_hd_key` (in UIPreferences) | SettingsUI | Show quality badge | ⏳ Phase 1 | in `UIPreferences.posterShowHd` |
| Rating display | `show_rating_key` (in UIPreferences) | SettingsUI | Show rating on poster | ⏳ Phase 1 | in `UIPreferences.posterShowRating` |
| Title display | `show_title_key` (in UIPreferences) | SettingsUI | Show title on poster | ⏳ Phase 1 | in `UIPreferences.posterShowTitle` |
| Episode text display | `show_episode_text_key` (in UIPreferences) | SettingsUI | Show episode count | ⏳ Phase 1 | in `UIPreferences.posterShowEpisode` |

### 10.3 Player settings (`settings_player.xml`, 231 lines)

| Setting | XML Key | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|---|
| Episode sync (resume) | `episode_sync_enabled_key` (default true) | SettingsPlayer.kt | Persist playback position | ⏳ Phase 2 | `AppSettings.data.episodeSync` (PlayerPreferences) |
| Default player | `player_default_key` | SettingsPlayer.kt | Default external player | ❌ Skip | N/A on desktop |
| Limit player title length | `prefer_limit_title_key` (default 0) | SettingsPlayer.kt | Truncate title | ⏳ Phase 2 | `PlayerPreferences.limitPlayerTitle` |
| Limit player info (resolution) | `prefer_limit_show_player_info` | SettingsPlayer.kt | Show resolution | ⏳ Phase 3 | `PlayerPreferences.showMediaInfo` |
| Hide player control names | `hide_player_control_names_key` (default false) | SettingsPlayer.kt | Icon-only controls | ⏳ Phase 2 | `PlayerPreferences.hidePlayerControlNames` |
| Subtitle settings | `subtitle_settings_key` | SettingsPlayer.kt | Open subtitle styling | ⏳ Phase 2 | not started |
| Chromecast subtitle settings | `subtitle_settings_chromecast_key` | SettingsPlayer.kt | Open cast subtitle styling | ❌ Skip | N/A |
| Source priority | `player_source_priority_key` | SettingsPlayer.kt | Open source priority dialog | ⏳ Phase 2 | not started |
| Picture-in-picture | `pip_enabled_key` (default true) | SettingsPlayer.kt | Enable PiP | ❌ Skip | N/A on desktop |
| Player resize | `player_resize_enabled_key` (default true) | SettingsPlayer.kt | Aspect ratio toggle | ⏳ Phase 2 | `PlayerPreferences.resizeEnabled` |
| Playback speed (eigengravy) | `playback_speed_enabled_key` (default false) | SettingsPlayer.kt | Speed slider | ⏳ Phase 2 | `PlayerPreferences.speedEnabled` |
| Compressor (audio) | `compressor_enabled_key` (default false) | SettingsPlayer.kt | Loudness compressor | ⏳ Phase 3 | `PlayerPreferences.compressorEnabled` |
| Speedup (2x tap) | `speedup_key` (default false) | SettingsPlayer.kt | Hold-to-2x | ⏳ Phase 3 | `PlayerPreferences.tiktokEnabled` |
| Autoplay next | `autoplay_next_key` (default true) | SettingsPlayer.kt | Auto-advance episode | ⏳ Phase 2 | `PlayerPreferences.autoPlayEnabled` |
| Skip OP from database | `enable_skip_op_from_database` (default true) | SettingsPlayer.kt | Use skip-op APIs | ⏳ Phase 2 | `PlayerPreferences.skipOpEnabled` |
| Rotate video | `rotate_video_key` (default false) | SettingsPlayer.kt | Manual rotation | ❌ Skip | N/A |
| Auto-rotate video | `auto_rotate_video_key` (default true) | SettingsPlayer.kt | Sensor rotation | ❌ Skip | N/A |
| Preview seekbar | `preview_seekbar_key` (default true) | SettingsPlayer.kt | Scrubbing thumbnails | ⏳ Phase 3 | `PlayerPreferences.previewBarEnabled` |
| Software decoding | `software_decoding_key2` (default -1) | SettingsPlayer.kt | Force software decoder | ❌ Skip | vlcj uses libvlc default |
| Extra brightness | `extra_brightness_key` (default false) | SettingsPlayer.kt | Screen brightness | ❌ Skip | N/A |
| Swipe to seek (horizontal) | `swipe_enabled_key` (default true) | SettingsPlayer.kt | Horizontal swipe seek | ⏳ Phase 2 | `PlayerPreferences.swipeHorizontalEnabled` |
| Swipe vertical (brightness/volume) | `swipe_vertical_enabled_key` (default true) | SettingsPlayer.kt | Vertical swipe | ⏳ Phase 2 | `PlayerPreferences.swipeVerticalEnabled` |
| Double-tap to seek | `double_tap_enabled_key` (default false) | SettingsPlayer.kt | Double tap → seek | ⏳ Phase 2 | `PlayerPreferences.doubleTapToSeekEnabled` |
| Double-tap to pause | `double_tap_pause_enabled_key` (default false) | SettingsPlayer.kt | Double tap → pause | ⏳ Phase 2 | `PlayerPreferences.doubleTapToPauseEnabled` |
| Double-tap seek time | `double_tap_seek_time_key2` (default 10, max 60, min 5) | SettingsPlayer.kt | Seek seconds | ⏳ Phase 2 | `PlayerPreferences.doubleTapTime` |
| Video buffer (disk MB) | `video_buffer_disk_key` (default 0) | SettingsPlayer.kt | Disk cache size | ⏳ Phase 2 | `PlayerPreferences.bufferDiskMB` |
| Video buffer (RAM MB) | `video_buffer_size_key` (default 0) | SettingsPlayer.kt | RAM cache size | ⏳ Phase 2 | `PlayerPreferences.bufferRamMB` |
| Video buffer length (sec) | `video_buffer_length_key` (default 0) | SettingsPlayer.kt | Buffer duration | ⏳ Phase 2 | `PlayerPreferences.bufferTimeSec` |
| Clear video buffer | `video_buffer_clear_key` | SettingsPlayer.kt | Wipe cache | ⏳ Phase 2 | not started |
| Android TV on-seek time | `android_tv_interface_on_seek_key` (default 10, max 60) | SettingsPlayer.kt | TV seek increment | ❌ Skip | N/A |
| Android TV off-seek time | `android_tv_interface_off_seek_key` (default 10, max 60) | SettingsPlayer.kt | TV seek decrement | ❌ Skip | N/A |
| Show name in player | `show_name` (default true) | PlayerPreferences | Show title overlay | ⏳ Phase 2 | `PlayerPreferences.showName` |
| Show resolution in player | `show_resolution` (default true) | PlayerPreferences | Show resolution overlay | ⏳ Phase 2 | `PlayerPreferences.showResolution` |
| Show media info | `show_media_info` (default false) | PlayerPreferences | Detailed codec info | ⏳ Phase 3 | `PlayerPreferences.showMediaInfo` |
| Start paused | `start_paused_key` (default false) | PlayerPreferences | Don't autoplay | ⏳ Phase 2 | `PlayerPreferences.startPaused` |

### 10.4 Provider settings (`settings_providers.xml`, 30 lines)

| Setting | XML Key | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|---|
| Provider languages | `provider_lang_key` | SettingsProviders.kt | Filter providers by language | ⏳ Phase 1 | `AppSettings.data.extensionLanguages` |
| Preferred media types | `prefer_media_type_key_2` | SettingsProviders.kt | Movie/TvSeries/Anime/etc. filter | ⏳ Phase 1 | `AppSettings.data.preferredMediaTypes` |
| Display subbed/dubbed | `display_sub_key` | SettingsProviders.kt | Sub/Dub badges | ⏳ Phase 1 | `AppSettings.data.displaySubDub` |
| Enable NSFW on providers | `enable_nsfw_on_providers_key` (default false) | SettingsProviders.kt | Show NSFW providers | ⏳ Phase 2 | `AppSettings.data.enableAdult` |
| Test providers | `test_providers_key` | SettingsProviders.kt | Run provider tests | ⏳ Phase 2 | not started |

### 10.5 Account settings (`settings_account.xml`, 55 lines)

| Setting | XML Key | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|---|
| MAL account | `mal_key` | SettingsAccount.kt | Login to MAL | ⏳ Phase 2 | not started |
| Kitsu account | `kitsu_key` | SettingsAccount.kt | Login to Kitsu | ⏳ Phase 2 | not started |
| AniList account | `anilist_key` | SettingsAccount.kt | Login to AniList | ⏳ Phase 2 | not started |
| Simkl account | `simkl_key` | SettingsAccount.kt | Login to Simkl | ⏳ Phase 2 | not started |
| OpenSubtitles account | `opensubtitles_key` | SettingsAccount.kt | Login to OpenSubtitles | ⏳ Phase 2 | not started |
| SubDL account | `subdl_key` | SettingsAccount.kt | Login to SubDL | ⏳ Phase 3 | not started |
| AnimeSkip account | `animeskip_key` | SettingsAccount.kt | Login to AnimeSkip | ⏳ Phase 2 | not started |
| Skip startup account select | `skip_startup_account_select_key` (default false) | SettingsAccount.kt | Skip account picker | ⏳ Phase 2 | `SecurityPreferences.skipAccountSelection` |
| Biometric lock | `biometric_key` (default false) | SettingsAccount.kt | Fingerprint unlock | ❌ Skip | use OS keyring |

### 10.6 Updates / Backup settings (`settings_updates.xml`, 86 lines)

| Setting | XML Key | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|---|
| Check for update | `manual_check_update_key` | SettingsUpdates.kt | Manual update check | ⏳ Phase 2 | not started |
| Install prerelease | `install_prerelease_key` | SettingsUpdates.kt | Install prerelease builds | ❌ Skip | desktop uses AppImage |
| APK installer | `apk_installer_key` | SettingsUpdates.kt | APK installer choice | ❌ Skip | N/A |
| Auto-update app | `auto_update_key` (default true) | SettingsUpdates.kt | Notify of updates | ⏳ Phase 2 | `UpdatePreferences.showAppUpdates` |
| Backup now | `backup_key` | SettingsUpdates.kt | Manual backup | ⏳ Phase 3 | not started |
| Automatic backup frequency | `automatic_backup_key` | SettingsUpdates.kt | Auto backup schedule | ⏳ Phase 3 | `BackupPreferences.frequency` |
| Restore | `restore_key` | SettingsUpdates.kt | Restore from backup | ⏳ Phase 3 | not started |
| Backup path | `backup_path_key` | SettingsUpdates.kt | Backup destination | ⏳ Phase 3 | `BackupPreferences.path` |
| Auto-update plugins | `auto_update_plugins_key` (default true) | SettingsUpdates.kt | Auto plugin updates | ⏳ Phase 2 | `AppSettings.data.autoUpdatePlugins` |
| Auto-download plugins | `auto_download_plugins_key2` (default 0) | SettingsUpdates.kt | Auto plugin download (Disable/FilterByLang/All/NsfwOnly) | ⏳ Phase 3 | `AppSettings.data.autoDownloadPlugins` |
| Manual plugin update | `manual_update_plugins_key` | SettingsUpdates.kt | Trigger plugin update check | ⏳ Phase 2 | not started |
| Show logcat | `show_logcat_key` | SettingsUpdates.kt | Open log viewer | ⏳ Phase 3 | not started |
| Redo setup process | `redo_setup_key` | SettingsUpdates.kt | Restart setup wizard | ⏳ Phase 3 | not started |
| Skip update | `skip_update_key` | SettingsUpdates.kt | Skip specific update | ⏳ Phase 2 | `UpdatePreferences.skipUpdate` |

### 10.7 Logcat viewer

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `LogcatDialog` | `ui/settings/logcat/LogcatDialog.kt` | Logcat viewer dialog | ⏳ Phase 3 | not started (use OS logs) |
| `LogcatParser` | `ui/settings/logcat/LogcatParser.kt` | Parse logcat output | ⏳ Phase 3 | not started |
| `LogcatAdapter` | `ui/settings/LogcatAdapter.kt` | Log list adapter | ❌ Skip | Compose |
| Log level filter | (in LogcatDialog) | Filter by level | ⏳ Phase 3 | not started |
| Log search | (in LogcatDialog) | Search logs | ⏳ Phase 3 | not started |
| Export logs | (in LogcatDialog) | Save to file | ⏳ Phase 3 | not started |

### 10.8 Testing screen

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `TestFragment2` / `TestScreen` | `ui/settings/testing/TestFragment2.kt`, `TestScreen.kt` | Provider test runner | ⏳ Phase 2 | not started |
| `TestViewModel2` | `ui/settings/testing/TestViewModel2.kt` | Test state | ⏳ Phase 2 | not started |
| `TestView` | `ui/settings/testing/TestView.kt` | Custom test view | ❌ Skip | Compose |
| `TestResultAdapter` | `ui/settings/testing/TestResultAdapter.kt` | Result list adapter | ❌ Skip | Compose |
| Run all providers test | (in TestViewModel2) | Test every provider's search/load/loadLinks | ⏳ Phase 2 | not started |
| Test report export | (in TestFragment2) | Save test results | ⏳ Phase 2 | not started |

### 10.9 Directory picker

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `DirectoryPicker` | `ui/settings/utils/DirectoryPicker.kt` | File picker dialog | ⏳ Phase 2 | use AWT `JFileChooser` |

---

## Section 11 — Other Features

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| **Backup/restore** | `utils/BackupUtils.kt` (349 lines) | BackupFile JSON format (BackupVars: bool/int/string/float/long/stringSet) | ⏳ Phase 3 | not started |
| `BackupUtils.BackupFile` data class | `BackupUtils.kt:134-138` | BackupFile shape (datastore + settings) | ⏳ Phase 3 | not started |
| `BackupUtils.BackupVars` data class | `BackupUtils.kt:124-132` | 6-typed SharedPreferences map | ⏳ Phase 3 | not started |
| `BackupUtils.nonTransferableKeys` list | `BackupUtils.kt:56-114` | Keys excluded from backup (tokens, paths, etc.) | ⏳ Phase 3 | not started |
| `BackupUtils.getBackup(context)` | `BackupUtils.kt:141-167` | Build BackupFile from prefs | ⏳ Phase 3 | not started |
| `BackupUtils.restore(context, file, settings, datastore)` | `BackupUtils.kt:170-199` | Restore from backup | ⏳ Phase 3 | not started |
| `BackupUtils.backup(context)` | `BackupUtils.kt:201+` | Trigger backup | ⏳ Phase 3 | not started |
| `BackupWorkManager` | `services/BackupWorkManager.kt` | Scheduled backup worker | ❌ Skip | desktop uses cron/quartz |
| **GitHub release updater** | `ui/settings/GithubReleases.kt` | Fetch latest GitHub release | ⏳ Phase 2 | not started |
| `ApkUpdater` | `ui/settings/ApkUpdater.kt` | APK self-update | ❌ Skip | desktop uses AppImage |
| `InAppUpdater` | `utils/InAppUpdater.kt` | In-app update logic | ⏳ Phase 3 | not started |
| **App setup wizard** | `ui/setup/*` | First-run wizard (language/providers/media/layout) | ⏳ Phase 2 | not started |
| **Multi-account** | `ui/account/*` + `DataStoreHelper.Account` | Multiple user profiles w/ switch | ⏳ Phase 2 | not started |
| `lockPin` per account | `Account` data class | Numeric PIN | ⏳ Phase 3 | not started |
| Biometric | `utils/BiometricAuthenticator.kt` | Fingerprint unlock | ❌ Skip | OS keyring instead |
| **Safe mode** (plugin crash recovery) | `PluginManager.kt` | Skip broken plugins on next boot | ⏳ Phase 3 | not started |
| **Hot reload** (dev plugin reload) | (in PluginManager) | Reload plugin without restart | ⏳ Phase 3 | not started |
| **Video skip APIs** | `utils/videoskip/*.kt` | AniSkip/AnimeSkip/IntroDbSkip/TheIntroDBSkip/FillerEpisodeCheck | ⏳ Phase 2 | not started (see §7.4) |
| **Torrent playback** | `ui/player/Torrent.kt` | torrentserver lib integration | ⏳ Phase 3 | not started |
| **Chromecast subtitle styling** | `ui/subtitles/ChromecastSubtitlesFragment2.kt` | Cast-specific subtitle colors | ❌ Skip | replaced by Fcast |
| **In-app browser** | `ui/WebviewFragment.kt` | Webview for sites needing JS | ⏳ Phase 2 | JavaFX WebView available |
| **Easter egg** | `ui/EasterEggMonkeFragment.kt` | Hidden monke | ⏳ Phase 3 | not started |
| **TV layout support** | `homepage_parent_tv`, `fragment_home_tv` etc. | TV-specific layouts | ❌ Skip | N/A on desktop |
| **Emulator layout** | `Globals.EMULATOR` | Emulator detection | ❌ Skip | N/A |
| **Voting API** | `plugins/VotingApi.kt` | Vote for plugins | ❌ Skip | N/A |
| **Subscription work manager** | `services/SubscriptionWorkManager.kt` | Periodic subscription refresh | ⏳ Phase 2 | not started |
| **Subscription check** | (in SubscriptionWorkManager) | Check subscribed series for new episodes | ⏳ Phase 2 | not started |
| **App restart receiver** | `receivers/VideoDownloadRestartReceiver.kt` | Restart downloads on boot | ❌ Skip | N/A |
| **Package installer receiver** | `receivers/PackageInstallerStatusReceiver.kt` | APK install status | ❌ Skip | N/A |
| **Downloader test impl** | `DownloaderTestImpl.kt` | Test downloads | ⏳ Phase 3 | not started |
| **Subtitles: AbstractSubProvider** | `subtitles/AbstractSubProvider.kt` | Subtitle provider base | ⏳ Phase 2 | not started |
| **Subtitles: AbstractSubtitleEntities** | `subtitles/AbstractSubtitleEntities.kt` | Subtitle entity definitions | ⏳ Phase 2 | not started |
| **SubtitleUtils** | `utils/SubtitleUtils.kt` | Subtitle helpers | ⏳ Phase 2 | not started |

---

## Section 12 — Extractors (Built-in)

**Source dir**: `library/src/commonMain/kotlin/com/lagradost/cloudstream3/extractors/`

**112 `.kt` files** defining **~310 ExtractorApi subclasses** (some files register multiple variants). All ✅ **vendored** — they compile and run on JVM via dex2jar-loaded plugins.

Below is the full file list grouped by host. Each entry: file → typical domain → port status.

### 12.1 Single-host extractors

| File | Site domain | Status |
|---|---|---|
| `Acefile.kt` | acefile.io | ✅ |
| `Blogger.kt` | blogger.com / bp.blogspot.com | ✅ |
| `ByseSX.kt` | (multiple Byse* subdomains) | ✅ |
| `Cda.kt` | cda.pl | ✅ |
| `CineMMRedirect.kt` | dhcplay.com / hglink.to / hgplaycdn / habetar / yuguaab / guxhag / auvexiug / xenolyzb / haxloppd / cavanhabg / dumbalag / uasopt (CineMM redirect chain) | ✅ |
| `CloudMailRuExtractor.kt` | cloud.mail.ru | ✅ |
| `ContentXExtractor.kt` | contentx.live | ✅ |
| `Dailymotion.kt` | dailymotion.com | ✅ |
| `DoodExtractor.kt` | doodstream / dood.* (multi-domain: cx/pm/to/so/la/watch/wf/ws/yt/spro/dsvplay/d0000d/d000d.com/doodstream.com/dooood) | ✅ |
| `Embedgram.kt` | embedgram.com | ✅ |
| `EmturbovidExtractor.kt` | emturbovid.com | ✅ |
| `Evolaod.kt` | evoload.io / evoload1 | ✅ |
| `Fastream.kt` | fastream.to | ✅ |
| `Filegram.kt` | filegram.me | ✅ |
| `Filemoon.kt` | filemoon.sx / filemoon.in / filemoonsx / filemoonv2 | ✅ |
| `Filesim.kt` | filesim.com | ✅ |
| `Firestream.kt` | firestream.tv / firestreamsite | ✅ |
| `Flyfile.kt` | flyfile.mobi | ✅ |
| `GDMirrorbot.kt` | gdmirrorbot (special) | ✅ |
| `GUpload.kt` | gdriveplayer / g-upload variants | ✅ |
| `GamoVideo.kt` | gamovideo.com | ✅ |
| `Gdriveplayer.kt` | gdriveplayer.com / api / app / fun / io / me / biz / org / us / co | ✅ |
| `GenericM3U8.kt` | (fallback for any .m3u8 URL) | ✅ |
| `Gofile.kt` | gofile.io | ✅ |
| `GoodstreamExtractor.kt` | goodstream.xyz | ✅ |
| `HDMomPlayerExtractor.kt` | hdmomplayer | ✅ |
| `HDPlayerSystemExtractor.kt` | hdplayersystem | ✅ |
| `HDStreamAbleExtractor.kt` | hdstreamable.com | ✅ |
| `Hexload.kt` | hexload.net | ✅ |
| `HotlingerExtractor.kt` | hotlinger | ✅ |
| `HubCloud.kt` | hubcloud | ✅ |
| `HubuCloud.kt` | hubucloud.ru | ✅ |
| `Hxfile.kt` | hxfile.co | ✅ |
| `InternetArchive.kt` | archive.org | ✅ |
| `JWPlayer.kt` | (generic JWPlayer) | ✅ |
| `Jeniusplay.kt` | jeniusplay.com | ✅ |
| `Krakenfiles.kt` | krakenfiles.com | ✅ |
| `Linkbox.kt` | linkbox.to | ✅ |
| `LuluStream.kt` | lulustream / lulustream1/2 / luluvdoo | ✅ |
| `M3u8Manifest.kt` | (m3u8 manifest helper) | ✅ |
| `MailRuExtractor.kt` | mail.ru | ✅ |
| `Maxstream.kt` | maxstream.tv | ✅ |
| `Mediafire.kt` | mediafire.com | ✅ |
| `MixDrop.kt` | mixdrop / mixdrop.bz/ch/to/ag/ps/si/mdy/mxdrop.to | ✅ |
| `Moviehab.kt` | moviehab / moviehab.net | ✅ |
| `Mp4Upload.kt` | mp4upload.com | ✅ |
| `Mvidoo.kt` | mvidoo.com | ✅ |
| `OdnoklassnikiExtractor.kt` | ok.ru | ✅ |
| `OkRuExtractor.kt` | ok.ru (HTTP/SSL/Mobile variants) | ✅ |
| `PeaceMakerstExtractor.kt` | peacemakerst | ✅ |
| `PixelDrainExtractor.kt` | pixeldrain / pixeldrain.dev | ✅ |
| `PlayLtXyz.kt` | play.lt.xyz | ✅ |
| `PlayerVoxzer.kt` | voxzer.net | ✅ |
| `Playmate.kt` | playmate | ✅ |
| `Rabbitstream.kt` | rabbitstream.com | ✅ |
| `RapidVidExtractor.kt` | rapidvid | ✅ |
| `SBPlay.kt` | sbplay / sbfull / sbasian / sbface / sbflix / sblona / sblongvu / sbnet / sbrapid / sbsonic / sbspeed / sbthe (StreamSB family) | ✅ |
| `SecvideoOnline.kt` | secvideo.online | ✅ |
| `Sendvid.kt` | sendvid.com | ✅ |
| `SibNetExtractor.kt` | sibnet.ru | ✅ |
| `SobreatsesuypExtractor.kt` | sobreatsesuyp | ✅ |
| `StreamEmbed.kt` | streamembed | ✅ |
| `StreamSB.kt` | streamsb / streamsb1-11 / ssbstream / streamsss | ✅ |
| `StreamSilk.kt` | streamsilk | ✅ |
| `StreamTape.kt` | streamtape / streamtape.net / xyz / shave / watchadsontape | ✅ |
| `StreamWishExtractor.kt` | streamwish / streamwish2 / strwish / strwish2 / wishembed / wishfast / wishonly / cdnwish / flaswish / sfastwish / playerwish / streamembed / mwish / ewish / dwish / obeywish / jodwish / swhoi / wishonly / nekowish / swdyu / awish / asnwish / ds2play / ds2video / hlsWish | ✅ |
| `Streamcash.kt` | streamcash | ✅ |
| `Streamhub.kt` | streamhub2 / ztreamhub | ✅ |
| `Streamlare.kt` | streamlare.com | ✅ |
| `StreamoUpload.kt` | streamoupload | ✅ |
| `Streamplay.kt` | streamplay | ✅ |
| `Supervideo.kt` | supervideo | ✅ |
| `TRsTXExtractor.kt` | trstx | ✅ |
| `Tantifilm.kt` | tantifilm | ✅ |
| `TauVideoExtractor.kt` | tauvideo | ✅ |
| `Up4Stream.kt` | up4stream / up4fun.top | ✅ |
| `UpstreamExtractor.kt` | upstream / upstreaming | ✅ |
| `Uqload.kt` | uqload / uqload1/2 / uqloadcx / uqloadbz / uqloads.xyz | ✅ |
| `Userload.kt` | userload.co | ✅ |
| `Userscloud.kt` | userscloud | ✅ |
| `Uservideo.kt` | uservideo | ✅ |
| `Vicloud.kt` | vicloud | ✅ |
| `VidHidePro.kt` | vidhidepro / vidhidepro1-6 / vidhidehub | ✅ |
| `VidMoxyExtractor.kt` | vidmoxy | ✅ |
| `VidStack.kt` | vidstack | ✅ |
| `Vidara.kt` | vidara / vidaraa / vidaraw / vidarax / vidaratem / vidavaca / vidaarax.com/net / vidara.so | ✅ |
| `Videa.kt` | videa | ✅ |
| `VideoSeyredExtractor.kt` | videoseyred | ✅ |
| `VidhideExtractor.kt` | vidhide | ✅ |
| `Vidmoly.kt` | vidmoly / vidmolyme / vidmolyto / vidmolybiz | ✅ |
| `Vido.kt` | vido | ✅ |
| `Vidoza.kt` | vidoza.net | ✅ |
| `Vids.kt` | vids | ✅ |
| `Vidsonic.kt` | vidsonic | ✅ |
| `Vinovo.kt` | vinovo / vinovosi / vinovoto | ✅ |
| `VkExtractor.kt` | vk.com | ✅ |
| `Voe.kt` | voe / voe1 / voe2 | ✅ |
| `Vtbe.kt` | vtbe | ✅ |
| `WatchSB.kt` | watchsb | ✅ |
| `Wibufile.kt` | wibufile | ✅ |
| `XStreamCdn.kt` | xstreamcdn | ✅ |
| `YourUpload.kt` | yourupload | ✅ |
| `YoutubeExtractor.kt` | youtube.com / youtube-nocookie / short link / mobile | ✅ |
| `Zplayer.kt` | zplayer / zplayerv2 | ✅ |

### 12.2 Extractor helpers

| File | Purpose | Status |
|---|---|---|
| `helper/JWPlayerHelper.kt` | Parse JWPlayer config (sources, tracks) | ✅ vendored |
| `helper/WcoHelper.kt` | wcostream.com special handling | ✅ vendored |
| `helper/NineAnimeHelper.kt` | 9anime special handling | ✅ vendored |
| `helper/GogoHelper.kt` | gogoanime special handling | ✅ vendored |
| `helper/VstreamhubHelper.kt` | vstreamhub special handling | ✅ vendored |
| `helper/AesHelper.kt` | AES-128/256-CBC encrypt/decrypt | ✅ vendored |
| `helper/CryptoJSHelper.kt` | CryptoJS-compatible AES (PBKDF2 etc.) | ✅ vendored |
| `helper/AsianEmbedHelper.kt` | asianembed special handling | ✅ vendored |

---

## Section 13 — TV Types & Enums

### 13.1 `TvType` (18 values)

**Source**: `MainAPI.kt:1120-1142`

| Value | Ordinal | Meaning | Used by | Status |
|---|---|---|---|---|
| `Movie` | 1 | Standard movie | Most providers | ✅ vendored |
| `AnimeMovie` | 2 | Anime film | Anime providers | ✅ vendored |
| `TvSeries` | 3 | Episodic TV | Most providers | ✅ vendored |
| `Cartoon` | 4 | Western cartoon | Cartoon providers | ✅ vendored |
| `Anime` | 5 | Anime (subbed/dubbed) | Anime providers, skip-op | ✅ vendored |
| `OVA` | 6 | Original Video Animation | Anime providers, skip-op | ✅ vendored |
| `Torrent` | 7 | Magnet/torrent link | Torrent providers | ✅ vendored |
| `Documentary` | 8 | Documentary | Documentary providers | ✅ vendored |
| `AsianDrama` | 9 | Korean/Japanese/Chinese drama | Drama providers (MDL) | ✅ vendored |
| `Live` | 10 | Live stream | Live providers | ✅ vendored |
| `NSFW` | 11 | Adult content | Adult providers | ✅ vendored |
| `Others` | 12 | Catch-all | Misc providers | ✅ vendored |
| `Music` | 13 | Music video | Music providers | ✅ vendored |
| `AudioBook` | 14 | Audio book | AudioBook providers | ✅ vendored |
| `CustomMedia` | 15 | Custom player | Special providers | ✅ vendored |
| `Audio` | 16 | Audio-only | Audio providers | ✅ vendored |
| `Podcast` | 17 | Podcast | Podcast providers | ✅ vendored |
| `Video` | 18 | Generic video | Misc providers | ✅ vendored |

### 13.2 Other key enums

| Enum | Source | Values | Status | Port File |
|---|---|---|---|---|
| `DubStatus` | `MainAPI.kt:905-909` | None(-1), Dubbed(1), Subbed(0) | ✅ | vendored |
| `SearchQuality` | `MainAPI.kt:1303-1320` | Cam, CamRip, HdCam, Telesync, WorkPrint, Telecine, HQ, HD, HDR, BlueRay, DVD, SD, FourK, UHD, SDR, WebRip | ✅ | vendored |
| `ProviderType` | `MainAPI.kt:887-890` | MetaProvider, DirectProvider | ✅ | vendored |
| `VPNStatus` | `MainAPI.kt:893-897` | None, MightBeNeeded, Torrent | ✅ | vendored |
| `ShowStatus` | `MainAPI.kt:900-903` | Completed, Ongoing | ✅ | vendored |
| `ActorRole` | `MainAPI.kt:1524-1528` | Main, Supporting, Background | ✅ | vendored |
| `TrackerType` | `MainAPI.kt:2838-2846` | MOVIE, TV, TV_SHORT, ONA, OVA, SPECIAL, MUSIC | ✅ | vendored |
| `SimklSyncServices` | `MainAPI.kt:2664-2670` | Simkl, Imdb, Tmdb, AniList, Mal | ✅ | vendored |
| `AutoDownloadMode` | `MainAPI.kt:1144-1154` | Disable(0), FilterByLang(1), All(2), NsfwOnly(3) | ✅ | vendored |
| `ExtractorLinkType` | `ExtractorApi.kt:417-444` | VIDEO, M3U8, DASH, TORRENT, MAGNET | ✅ | vendored |
| `Qualities` | `ExtractorApi.kt:852-883` | Unknown(400), P144, P240, P360, P480, P720, P1080, P1440, P2160 | ✅ | vendored |
| `SyncIdName` | `library/.../syncproviders/SyncAPI.kt:3-11` | Anilist, MyAnimeList, Kitsu, Trakt, Imdb, Simkl, LocalList | ✅ | vendored |
| `CSPlayerEvent` | `IPlayer.kt:12-25` | Pause, Play, SeekForward, SeekBack, SkipCurrentChapter, NextEpisode, PrevEpisode, PlayPauseToggle, ToggleMute, Restart, PlayAsAudio | 🚧 | subset in VlcjPlayer |
| `CSPlayerLoading` | `IPlayer.kt:27-32` | IsPaused, IsPlaying, IsBuffering | ✅ | in VlcjPlayer PlayerState |
| `PlayerEventSource` | `IPlayer.kt:34-44` | UI, Player, Cache, Generator | ⏳ Phase 1 | not started |
| `SubtitleStatus` | `PlayerSubtitleHelper.kt:20-24` | IS_ACTIVE, REQUIRES_RELOAD, NOT_FOUND | ⏳ Phase 1 | not started |
| `SubtitleOrigin` | `PlayerSubtitleHelper.kt:26-30` | URL, DOWNLOADED_FILE, EMBEDDED_IN_VIDEO | ⏳ Phase 1 | not started |
| `EpisodeSortType` | `ResultViewModel2.kt:211-218` | NUMBER_ASC, NUMBER_DESC, RATING_HIGH_LOW, RATING_LOW_HIGH, DATE_NEWEST, DATE_OLDEST | ⏳ Phase 1 | not started |
| `LibraryListType` | `ResultViewModel2.kt:205-209` | BOOKMARKS, FAVORITES, SUBSCRIPTIONS | ⏳ Phase 1 | not started |
| `ListSorting` | `LibraryViewModel.kt:19-29` | Query, RatingHigh, RatingLow, UpdatedNew, UpdatedOld, AlphabeticalA, AlphabeticalZ, ReleaseDateNew, ReleaseDateOld | ⏳ Phase 1 | not started |
| `WatchType` | `ui/WatchType.kt` | NONE, WATCHING, COMPLETED, DROPPED, FAVORITE, BOOKMARK, SUBSCRIPTION (with stringRes) | ⏳ Phase 1 | not started |
| `SyncWatchType` | `ui/SyncWatchType.kt` | Sync-API watch types | ⏳ Phase 2 | not started |
| `CloudStreamThemeMode` | `shared/.../theme/Theme.kt:16-46` | Dark, Amoled, AmoledLight, Light, Dracula, Lavender, SilentBlue, FollowSystem, Dynamic | ✅ | vendored |
| `CloudStreamPrimaryColor` | `shared/.../theme/Palette.kt:5-28` | NORMAL, BLUE, PURPLE, GREEN, GREEN_APPLE, RED, BANANA, PARTY, PINK, CARNATION_PINK, MAROON, DARK_GREEN, NAVY_BLUE, GREY, WHITE, BROWN, ORANGE, DANDELION_YELLOW, COOL_BLUE, LAVENDER, DYNAMIC, DYNAMIC_TWO | ✅ | vendored |
| `QualityProfileType` | `QualityDataHelper.kt:47-52` | None, WiFi, Data, Download | ⏳ Phase 2 | not started |

---

## Section 14 — Shared KMP Module

**Source dir**: `shared/src/commonMain/kotlin/` (vendored into `/home/z/my-project/desktop/shared/`)

### 14.1 MVI / state architecture

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `state/Viewmodel.kt` | `shared/.../state/Viewmodel.kt` | ViewModel base, StateContainer, EffectContainer, ActionHandler | ✅ | vendored |
| `state/ImmutableState.kt` | same | Immutable state wrapper | ✅ | vendored |
| `state/LogState.kt` | same | Logging state | ✅ | vendored |
| `state/StateContainer<T>` | same | State holder | ✅ | vendored |
| `state/EffectContainer<T>` | same | Side-effect holder | ✅ | vendored |
| `state/ActionHandler<T>` | same | Action dispatcher | ✅ | vendored |

### 14.2 ViewModels

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `viewmodels/GithubViewModel.kt` | `shared/.../viewmodels/GithubViewModel.kt` | GitHub releases fetcher | ✅ | vendored |
| `viewmodels/ExtensionsViewModel2.kt` | `shared/.../viewmodels/ExtensionsViewModel2.kt` | Plugin list VM | ✅ | vendored |

### 14.3 Theme system

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `theme/Theme.kt` (133 lines) | `shared/.../theme/Theme.kt` | CloudStreamTheme composable + modeToTheme() | ✅ | vendored |
| `theme/Palette.kt` (85 lines) | same | 22-color CloudStreamPrimaryColor enum + CloudStreamPalette object | ✅ | vendored |
| `theme/Scheme.kt` | same | Color schemes (dark, amoled, amoledLight, light, dracula, lavender, silentBlue) | ✅ | vendored |
| `theme/Font.kt` | same | ProductSans font family | ✅ | vendored |
| `theme/DeviceTheme.kt` (expect) + `theme/DeviceTheme.jvm.kt` (actual) | same | Platform-specific theme resolution (dynamic colors) | ✅ | vendored (JVM: no-op dynamic) |
| `theme/ColorPreference.kt` | `shared/src/androidMain/.../ColorPreference.kt` | Android color picker intent | ❌ Skip | N/A on desktop |

### 14.4 Compose widgets

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `compose/LazyList.kt` | `shared/.../compose/LazyList.kt` | Lazy list helpers | ✅ | vendored |
| `compose/Modifiers.kt` | same | Modifier extensions | ✅ | vendored |
| `compose/Screen.kt` | same | Screen composable | ✅ | vendored |
| `compose/Chip.kt` | same | Chip composable | ✅ | vendored |
| `compose/Button.kt` | same | Button | ✅ | vendored |
| `compose/Dialog.kt` | same | Dialog | ✅ | vendored |
| `compose/Log.kt` | same | Log composable | ✅ | vendored |
| `compose/ColorDialog.kt` | same | Color picker dialog | ✅ | vendored |
| `compose/Colors.kt` | same | Color utilities | ✅ | vendored |
| `compose/TextField.kt` | same | Text field | ✅ | vendored |
| `compose/colorpicker/CircularColorPicker.kt` | same | Circular color picker | ✅ | vendored |
| `compose/colorpicker/SquareColorPicker.kt` | same | Square color picker | ✅ | vendored |
| `compose/colorpicker/RingColorPicker.kt` | same | Ring color picker | ✅ | vendored |
| `compose/colorpicker/DoubleColorPicker.kt` | same | Two-color picker | ✅ | vendored |
| `compose/colorpicker/HsvColor.kt` | same | HSV color model | ✅ | vendored |
| `compose/colorpicker/ColorPickerDefaults.kt` | same | Defaults | ✅ | vendored |

### 14.5 Mihon preference system

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `mihon/common/preference/PreferenceStore.kt` (expect) | `shared/.../mihon/common/preference/PreferenceStore.kt` | Cross-platform preference store interface | ✅ | vendored |
| `mihon/common/preference/PreferenceData.kt` | same | Preference data delegate | ✅ | vendored |
| `mihon/common/preference/InMemoryPreferenceStore.kt` | same | In-memory impl | ✅ | vendored |
| `mihon/common/preference/AndroidPreferenceStore.kt` | `shared/src/androidMain/.../AndroidPreferenceStore.kt` | Android SharedPreferences impl | ❌ Skip | N/A on desktop |
| `mihon/common/preference/StatePreferenceStore.kt` | same | StateFlow-backed store | ❌ Skip | Android-only |
| `mihon/common/preference/DataPreferenceStore.kt` | same | DataStore-backed store | ❌ Skip | Android-only |
| `mihon/common/preference/AndroidPreference.kt` / `DataPreference.kt` | same | Android-specific impls | ❌ Skip | N/A |
| `mihon/presentation/Modifier.kt` | `shared/.../mihon/presentation/Modifier.kt` | Modifier extensions | ✅ | vendored |
| `mihon/presentation/Navigation.kt` | same | Navigation helpers | ✅ | vendored |
| `mihon/presentation/settings/Preference.kt` | same | Preference item model | ✅ | vendored |
| `mihon/presentation/settings/PreferenceItem.kt` | same | Preference item builder | ✅ | vendored |
| `mihon/presentation/settings/PreferenceScreen.kt` | same | Settings screen | ✅ | vendored |
| `mihon/presentation/settings/PreferenceScaffold.kt` | same | Scaffold | ✅ | vendored |
| `mihon/presentation/settings/SearchableSettings.kt` | same | Searchable settings | ✅ | vendored |
| `mihon/presentation/settings/Search.kt` | same | Search bar | ✅ | vendored |
| `mihon/presentation/settings/widget/SwitchPreferenceWidget.kt` | same | Switch widget | ✅ | vendored |
| `mihon/presentation/settings/widget/EditTextPreferenceWidget.kt` | same | Edit text widget | ✅ | vendored |
| `mihon/presentation/settings/widget/ColorPreferenceWidget.kt` | same | Color picker widget | ✅ | vendored |
| `mihon/presentation/settings/widget/TextPreferenceWidget.kt` | same | Text widget | ✅ | vendored |
| `mihon/presentation/settings/widget/ListPreferenceWidget.kt` | same | List (dropdown) widget | ✅ | vendored |
| `mihon/presentation/settings/widget/MultiSelectListPreferenceWidget.kt` | same | Multi-select widget | ✅ | vendored |
| `mihon/presentation/settings/widget/PreferenceGroupHeader.kt` | same | Section header | ✅ | vendored |
| `mihon/presentation/settings/widget/BasePreferenceWidget.kt` | same | Base widget | ✅ | vendored |
| `mihon/presentation/settings/widget/InfoWidget.kt` | same | Info text widget | ✅ | vendored |
| `mihon/material/AppBar.kt` | same | Top app bar | ✅ | vendored |
| `mihon/material/Scaffold.kt` | same | Material scaffold | ✅ | vendored |
| `mihon/material/Slider.kt` | same | Slider | ✅ | vendored |
| `mihon/material/Typography.kt` | same | Typography | ✅ | vendored |
| `mihon/material/Constants.kt` | same | Material constants | ✅ | vendored |
| `mihon/material/Pill.kt` | same | Pill shape | ✅ | vendored |
| `mihon/material/LabeledCheckbox.kt` | same | Labeled checkbox | ✅ | vendored |
| `mihon/material/SettingsItems.kt` | same | Settings items | ✅ | vendored |

### 14.6 DevicePreferenceStore (cross-platform settings abstraction)

| Feature | CS3 Source | Description | Status | Port File |
|---|---|---|---|---|
| `rememberAppSettings(): AppSettings` (expect) | `shared/.../DevicePreferenceStore.kt:12` | Composable to get AppSettings | 🚧 | `app/.../data/AppSettings.kt:39-75` (simpler impl — not yet Compose-aware) |
| `AppSettings` class (internal ctor) | L21-33 | Holds all preference groups | ✅ | vendored |
| `GeneralPreferences` | L191-207 | locale/bananas/parallelDownloads/concurrentConnections/batterOptimization/dns/jsdelivrProxy/downloadPath/downloadPathVisual | 🚧 | vendored; AppSettings.data has subset |
| `PlayerPreferences` | L158-189 | 30+ player prefs (episodeSync, defaultPlayer, limitPlayerTitle, hidePlayerControlNames, showName, showResolution, showMediaInfo, pipEnabled, resizeEnabled, speedEnabled, compressorEnabled, tiktokEnabled, autoPlayEnabled, startPaused, skipOpEnabled, autoRotateEnabled, rotateButtonEnabled, previewBarEnabled, softwareDecoding, extraBrightnessEnabled, swipeHorizontalEnabled, swipeVerticalEnabled, doubleTapToSeekEnabled, doubleTapToPauseEnabled, doubleTapTime, bufferDiskMB, bufferRamMB, bufferTimeSec, tvSeekOnTime, tvSeekOffTime) | 🚧 | vendored; not yet wired to UI |
| `ProviderPreferences` | L138-156 | preferredMedia/extensionLanguages/displayDubSub | 🚧 | vendored; AppSettings.data has subset |
| `UIPreferences` | L62-136 | 25 UI prefs (primaryColor/theme/layout/bottomTitle/advancedSearch/searchSuggestions/kitsuPostersEnabled/trailersEnabled/castEnabled/fillersEnabled/showMetadataOverlay/overscanDp/posterSize/posterShowHd/Dub/Sub/Rating/Title/Episode/showClock/randomButtonEnabled/confirmExit/filterQuality) | 🚧 | vendored; AppSettings.data has subset |
| `SecurityPreferences` | L53-60 | biometrics/skipAccountSelection | ✅ | vendored |
| `UpdatePreferences` | L46-51 | apkInstaller/showAppUpdates/skipUpdate | ✅ | vendored |
| `BackupPreferences` | L41-45 | frequency/path/visualPath | ✅ | vendored |
| `PluginPreferences` | L35-39 | autoUpdate/autoDownload | ✅ | vendored |
| `DevicePreferenceStore.jvm.kt` (actual) | `shared/src/jvmMain/.../DevicePreferenceStore.jvm.kt` | JVM impl of rememberAppSettings | 🚧 | vendored (compiles) |
| `DevicePreferenceStore.android.kt` (actual) | `shared/src/androidMain/...` | Android impl | ❌ Skip | N/A |
| `ClipboardHelper.kt` (expect) + JVM/Android actuals | `shared/.../ClipboardHelper.kt` | Clipboard abstraction | ✅ | vendored |

### 14.7 Build issues (noted)

> **Issue**: The `:shared` module currently has Compose Multiplatform compile issues related to `compose.resources` and `tooling.preview` — these need to be fixed before `:shared` can be consumed as a dependency.
>
> **Workaround**: For now, `AppSettings` and theme constants have been re-implemented inline in `app/.../data/AppSettings.kt` and `app/.../Main.kt:111-131`. Once `:shared` builds, we can swap to use the real `rememberAppSettings()` and the full theme system.

---

## Section 15 — Summary Statistics

### 15.1 Total feature count

| Section | Features inventoried |
|---|---|
| §1 Core Plugin/Provider System | ~140 |
| §2 UI Screens | ~150 |
| §3 Data Persistence | ~45 |
| §4 Sync Providers | ~35 |
| §5 Meta-providers | ~10 |
| §6 Network/Scraping Utilities | ~30 |
| §7 Player Pipeline | ~80 |
| §8 Download Manager | ~30 |
| §9 Cast/Chromecast | ~15 |
| §10 Settings (exhaustive) | ~75 |
| §11 Other Features | ~30 |
| §12 Extractors (310 classes across 112 files) | ~310 |
| §13 TV Types & Enums | ~25 |
| §14 Shared KMP Module | ~55 |
| **TOTAL** | **~1030 features inventoried** |

### 15.2 Status breakdown

| Status | Count | % |
|---|---|---|
| ✅ Done | ~430 | ~42% |
| 🚧 In progress | ~70 | ~7% |
| ⏳ Pending | ~340 | ~33% |
| ❌ Skip | ~190 | ~18% |

**Notes**: The vast majority of "✅ Done" entries are vendored library code (CS3 plugin primitives, extractors, sync APIs, metaproviders) that compile unchanged on JVM. The actual desktop app — UI, persistence, player UI, settings UI — is heavily "⏳ Pending".

### 15.3 Phase 1 (current) — critical pending

These must be completed for a usable Phase 1 release:

1. **ResultViewModel2 port** — episode list, episode range/season/dub selectors, sort modes, bookmark/favorite/subscribe buttons. (CS3: `ResultViewModel2.kt` 2,712 lines)
2. **RepoLinkGenerator port** — 20-min link caching, dedup, concurrent load. (CS3: `RepoLinkGenerator.kt` 160 lines)
3. **SearchScreen wiring** — actual results grid with provider grouping. (CS3: `SearchFragment.kt` + `SearchViewModel.kt`)
4. **DataStoreHelper port** — `VIDEO_POS_DUR`, `RESULT_WATCH_STATE`, `RESULT_FAVORITES_STATE_DATA`, `RESULT_RESUME_WATCHING`, `RESULT_EPISODE`, `RESULT_SEASON`, `RESULT_DUB`. (CS3: `DataStoreHelper.kt` 841 lines)
5. **LocalList sync provider** — backs the Library tab. (CS3: `LocalList.kt` 91 lines)
6. **WatchType enum + library tabs** — Library screen with Favorites/Bookmarks/Watching/Completed/Dropped tabs.
7. **Player UI overlay** — Compose overlay with play/pause/seek/skip buttons, source picker dialog, subtitle picker dialog.
8. **Prebuilt repositories** — `PREBUILT_REPOSITORIES` (cloudstreamrepo://...) wired into `RepositoryManager`.
9. **TmdbProvider wiring** — TMDB API client init, so home page has trending content even without providers. (CS3: `TmdbProvider.kt` 521 lines)
10. **Settings UI** — full Compose settings screen using `mihon/presentation/settings/PreferenceScreen.kt` (vendored).

### 15.4 Top 10 most critical pending features for next session

1. **ResultViewModel2 + episode list UI** — without this, the app cannot display a single media detail page. This is the #1 blocker for usability.
2. **RepoLinkGenerator + link cache** — without this, the player has no source list. Closely related to #1.
3. **Player UI overlay (Compose)** — currently `VlcjPlayer.kt` exists but has no Compose UI on top of it. Need play/pause/seek controls, progress bar, source/subtitle pickers.
4. **DataStoreHelper (persistence)** — without this, no bookmarks, no favorites, no resume position, no library. The whole "library" feature is gated on this.
5. **SearchScreen results** — currently the search screen shows a "Search will be wired to providers when you start typing" placeholder. Needs actual results grid + provider grouping.
6. **Prebuilt repos wiring** — `PREBUILT_REPOSITORIES` is a hardcoded list of CS3 repo URLs in `RepositoryManager.kt` of CS3. Need to copy this list into `app/.../repo/RepositoryManager.kt` so users can install plugins out-of-the-box.
7. **TMDB integration** — `TmdbProvider` is vendored but not initialized. Need to set `BuildConfig.TMDB_API_KEY` and call `TmdbProvider.init()` so the home page has trending content even when no plugins are installed.
8. **Full Settings UI** — currently `SettingsScreen` is a read-only display of `AppSettings.data`. Need editable settings using `mihon/presentation/settings/PreferenceScreen.kt` (vendored).
9. **LocalList sync provider** — 91 lines of CS3 code that maps DataStoreHelper data into a `SyncAPI` interface. Required for the Library tab to show favorites/bookmarks.
10. **Fix `:shared` module Compose build** — currently the shared module has Compose `compose.resources` / `tooling.preview` compile issues. Fixing this unlocks the real `DevicePreferenceStore`, the real `CloudStreamTheme`, the real `PreferenceScreen` — instead of the inline re-implementations we have now.

---

## Appendix A — CS3 Reference Module Structure

```
/reference/cloudstream/
├── library/                                # KMP plugin primitives (compiles on JVM)
│   └── src/commonMain/kotlin/com/lagradost/cloudstream3/
│       ├── MainAPI.kt                      # 2,860 lines — provider contract
│       ├── metaproviders/                  # 5 meta-providers
│       ├── syncproviders/SyncAPI.kt        # SyncIdName enum
│       ├── plugins/BasePlugin.kt           # 78 lines — plugin base
│       ├── extractors/                     # 112 files, ~310 extractor classes
│       ├── utils/                          # 16 utility files
│       └── network/                        # WebViewResolver (expect)
├── app/                                    # Android-only app (port source)
│   └── src/main/java/com/lagradost/cloudstream3/
│       ├── MainActivity.kt                # Single-activity host
│       ├── ui/                             # 100+ UI files
│       │   ├── home/ search/ result/ player/ library/ download/
│       │   ├── account/ setup/ subtitles/
│       │   └── settings/ (extensions/ logcat/ testing/ utils/)
│       ├── syncproviders/                  # 11 sync provider implementations
│       ├── plugins/                        # PluginManager.kt + RepositoryManager.kt
│       ├── utils/                          # 30+ utility files
│       │   ├── downloader/                 # 5 download files
│       │   ├── videoskip/                  # 5 skip-op files
│       │   └── serializers/
│       ├── actions/                        # 23 action files (external apps + cast)
│       ├── services/                       # 5 Android services
│       └── receivers/                      # 3 broadcast receivers
├── shared/                                 # KMP Compose UI (vendored to desktop)
│   └── src/commonMain/kotlin/
│       ├── com/lagradost/cloudstream4/     # Theme, viewmodels, state, compose
│       └── com/mihon/                      # Mihon settings framework
├── desktopApp/                             # CS3's upstream desktop stub (62 lines)
└── docs/                                   # Documentation
```

## Appendix B — Desktop Port Structure

```
/home/z/my-project/desktop/
├── settings.gradle.kts                    # Modules: app, library, shared
├── build.gradle.kts                       # Root build config
├── gradle/                                 # Wrapper + version catalog
├── library/                                # Vendored CS3 library (compiles on JVM)
│   ├── build.gradle.kts
│   └── src/commonMain/kotlin/com/lagradost/cloudstream3/  # 234 .kt files
├── shared/                                # Vendored CS3 shared (KMP Compose)
│   ├── build.gradle.kts
│   └── src/ (commonMain + jvmMain + androidMain)
├── app/                                   # Desktop app (Compose Desktop)
│   └── src/jvmMain/kotlin/com/lagradost/cloudstream4/
│       ├── Main.kt                        # 191 lines — entry point + AppShell
│       ├── data/AppSettings.kt            # 75 lines — settings.json
│       ├── plugin/PluginManager.kt        # 174 lines — installed plugins
│       ├── plugin/PluginLoader.kt         # 231 lines — dex2jar + URLClassLoader
│       ├── repo/RepositoryManager.kt      # 194 lines — repo fetch + install
│       ├── network/WebViewResolverJvm.kt # 48 lines — stub (Phase 2: JCEF)
│       ├── player/VlcjPlayer.kt           # 183 lines — vlcj wrapper
│       ├── ui/HomeScreen.kt               # 188 lines — home feed
│       ├── ui/SearchScreen.kt             # 42 lines — stub
│       ├── ui/PluginsScreen.kt            # 163 lines — repos + plugins
│       └── test/PluginLoaderTest.kt       # 229 lines — e2e test
├── test-plugins/                          # 3 .cs3 plugins + .jars
├── scripts/fetch-dex-tools.sh            # Bundles dex-tools for AppImage
├── icons/                                 # Linux icon
├── BUILD.md                              # Build instructions
├── PLUGIN_TESTS.md                       # Verified plugin compatibility
└── FEATURE_MAP.md                        # This document
```
