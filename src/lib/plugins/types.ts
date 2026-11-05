/**
 * CloudStream plugin contract — TypeScript port of CS3's MainAPI.kt
 *
 * This file is a 1:1 mirror of the data models and enums defined in
 * library/src/commonMain/kotlin/com/lagradost/cloudstream3/MainAPI.kt
 * (CS3 v4.8.0). Every type name, every field, every enum value matches
 * the Kotlin source so that porting a CS3 plugin to TS is mechanical.
 *
 * See ARCHITECTURE.md §1 for the full contract reference.
 */

// ============================================================================
// Enums
// ============================================================================

/** CS3 MainAPI.kt:1120-1142 */
export enum TvType {
  Movie = "Movie",
  AnimeMovie = "AnimeMovie",
  TvSeries = "TvSeries",
  Cartoon = "Cartoon",
  Anime = "Anime",
  OVA = "OVA",
  Torrent = "Torrent",
  Documentary = "Documentary",
  AsianDrama = "AsianDrama",
  Live = "Live",
  NSFW = "NSFW",
  Others = "Others",
  Music = "Music",
  AudioBook = "AudioBook",
  CustomMedia = "CustomMedia",
  Audio = "Audio",
  Podcast = "Podcast",
  Video = "Video",
}

/** CS3 MainAPI.kt:887 */
export enum ProviderType {
  MetaProvider = "MetaProvider",
  DirectProvider = "DirectProvider",
}

/** CS3 MainAPI.kt:893 */
export enum VPNStatus {
  None = "None",
  MightBeNeeded = "MightBeNeeded",
  Torrent = "Torrent",
}

/** CS3 MainAPI.kt:900 */
export enum ShowStatus {
  Completed = "Completed",
  Ongoing = "Ongoing",
}

/** CS3 MainAPI.kt:905 — id matches Kotlin enum values */
export enum DubStatus {
  None = -1,
  Dubbed = 1,
  Subbed = 0,
}

/** CS3 MainAPI.kt:1303 */
export enum SearchQuality {
  Cam = "Cam",
  CamRip = "CamRip",
  HdCam = "HdCam",
  Telesync = "Telesync",
  WorkPrint = "WorkPrint",
  Telecine = "Telecine",
  HQ = "HQ",
  HD = "HD",
  HDR = "HDR",
  BlueRay = "BlueRay",
  DVD = "DVD",
  SD = "SD",
  FourK = "FourK",
  UHD = "UHD",
  SDR = "SDR",
  WebRip = "WebRip",
}

/** CS3 ExtractorApi.kt:417-444 */
export enum ExtractorLinkType {
  VIDEO = "VIDEO",
  M3U8 = "M3U8",
  DASH = "DASH",
  TORRENT = "TORRENT",
  MAGNET = "MAGNET",
}

/** CS3 ExtractorApi.kt:852-883 — quality height as int, default priority in parens */
export enum Qualities {
  Unknown = 400,
  P144 = 144,
  P240 = 240,
  P360 = 360,
  P480 = 480,
  P720 = 720,
  P1080 = 1080,
  P1440 = 1440,
  P2160 = 2160,
}

/** CS3 MainAPI.kt:2838 */
export enum TrackerType {
  MOVIE = "MOVIE",
  TV = "TV",
  TV_SHORT = "TV_SHORT",
  ONA = "ONA",
  OVA = "OVA",
  SPECIAL = "SPECIAL",
  MUSIC = "MUSIC",
}

/** CS3 MainAPI.kt:1537 */
export enum ActorRole {
  Main = "Main",
  Supporting = "Supporting",
  Background = "Background",
}

/** Sync ID namespaces (CS3 SyncIdName) */
export enum SyncIdName {
  MyAnimeList = "MyAnimeList",
  Anilist = "Anilist",
  Kitsu = "Kitsu",
  Trakt = "Trakt",
  Imdb = "Imdb",
  Simkl = "Simkl",
  LocalList = "LocalList",
}

// ============================================================================
// Score (CS3 MainAPI.kt:917-1117)
// ============================================================================
// CS3 stores scores as integers with MAX = 1_000_000_000 and converts to
// various display scales. We replicate the same semantics.

const SCORE_MAX = 1_000_000_000;

export class Score {
  private constructor(private readonly data: number) {}

  toInt(maxScore = 10): number {
    return Math.round((this.data / SCORE_MAX) * maxScore);
  }
  toLong(maxScore = 10): number {
    return this.toInt(maxScore);
  }
  toFloat(maxScore = 10): number {
    return (this.data / SCORE_MAX) * maxScore;
  }
  toDouble(maxScore = 10): number {
    return this.toFloat(maxScore);
  }
  toString(maxScore = 10, decimals = 1): string {
    return this.toFloat(maxScore).toFixed(decimals);
  }

  static from(value: number | null | undefined, maxScore: number): Score | null {
    if (value == null || Number.isNaN(value)) return null;
    return new Score(Math.round((value / maxScore) * SCORE_MAX));
  }
  static from5(value: number | null | undefined): Score | null {
    return Score.from(value, 5);
  }
  static from10(value: number | null | undefined): Score | null {
    return Score.from(value, 10);
  }
  static from100(value: number | null | undefined): Score | null {
    return Score.from(value, 100);
  }
}

// ============================================================================
// Common data classes
// ============================================================================

/** CS3 MainAPI.kt:2552-2563 */
export interface Episode {
  data: string; // opaque string passed back to loadLinks
  name?: string;
  season?: number;
  episode?: number;
  posterUrl?: string;
  score?: Score | null;
  description?: string;
  date?: number; // unixtime ms
  runTime?: number; // seconds
}

/** CS3 MainAPI.kt:2215-2219 */
export interface NextAiring {
  episode: number;
  unixTime: number;
  season?: number;
}

/** CS3 MainAPI.kt:2227-2231 */
export interface SeasonData {
  season: number;
  name?: string;
  displaySeason?: number;
}

/** CS3 MainAPI.kt:1534-1550 */
export interface Actor {
  name: string;
  image?: string;
}
export interface ActorData {
  actor: Actor;
  role?: ActorRole;
  roleString?: string;
  voiceActor?: Actor;
}

/** CS3 MainAPI.kt:1247-1250 */
export interface AudioFile {
  url: string;
  headers?: Record<string, string>;
}

/** CS3 MainAPI.kt:1205-1221 */
export interface SubtitleFile {
  lang: string; // IETF tag or language code
  url: string;
  headers?: Record<string, string>;
}

/** CS3 MainAPI.kt:1779-1786 */
export interface TrailerData {
  extractorUrl: string;
  referer?: string;
  raw: boolean;
  headers?: Record<string, string>;
}

/** CS3 MainAPI.kt:2784-2790 */
export interface Tracker {
  malId?: number;
  kitsuId?: string;
  aniId?: string;
  image?: string;
  cover?: string;
}

// ============================================================================
// Search responses (CS3 MainAPI.kt:1406-1750)
// ============================================================================

/** CS3 MainAPI.kt:1406-1416 — base interface */
export interface SearchResponse {
  name: string;
  url: string;
  apiName: string;
  type?: TvType;
  posterUrl?: string;
  posterHeaders?: Record<string, string>;
  id?: number;
  quality?: SearchQuality;
  score?: Score | null;
}

export interface MovieSearchResponse extends SearchResponse {
  type: TvType.Movie | TvType.AnimeMovie | TvType.Torrent;
  year?: number;
}

export interface TvSeriesSearchResponse extends SearchResponse {
  type: TvType.TvSeries | TvType.Cartoon | TvType.AsianDrama | TvType.Documentary;
  year?: number;
  episodes?: number;
}

export interface AnimeSearchResponse extends SearchResponse {
  type: TvType.Anime | TvType.OVA;
  year?: number;
  dubStatus: DubStatus[]; // which dubs are available
  otherName?: string;
  episodes?: Record<DubStatus, number | undefined>;
}

export interface LiveSearchResponse extends SearchResponse {
  type: TvType.Live;
  lang?: string;
}

export interface TorrentSearchResponse extends SearchResponse {
  type: TvType.Torrent;
  seeders?: number;
  leechers?: number;
  size?: string;
  magnet?: string;
  torrent?: string;
}

export type AnySearchResponse =
  | MovieSearchResponse
  | TvSeriesSearchResponse
  | AnimeSearchResponse
  | LiveSearchResponse
  | TorrentSearchResponse;

// ============================================================================
// Load responses (CS3 MainAPI.kt:1814-2700)
// ============================================================================

/** CS3 MainAPI.kt:1814-1838 — base interface */
export interface LoadResponse {
  name: string;
  url: string;
  apiName: string;
  type: TvType;
  posterUrl?: string;
  year?: number;
  plot?: string;
  score?: Score | null;
  tags?: string[];
  duration?: number;
  trailers: TrailerData[];
  recommendations?: SearchResponse[];
  actors?: ActorData[];
  comingSoon: boolean;
  syncData: Record<string, string>;
  posterHeaders?: Record<string, string>;
  backgroundPosterUrl?: string;
  logoUrl?: string;
  contentRating?: string;
  uniqueUrl: string;
}

export interface MovieLoadResponse extends LoadResponse {
  type: TvType.Movie | TvType.AnimeMovie | TvType.Torrent | TvType.CustomMedia;
  dataUrl: string; // opaque — passed to loadLinks
}

export interface TvSeriesLoadResponse extends LoadResponse {
  type: TvType.TvSeries | TvType.Cartoon | TvType.AsianDrama | TvType.Documentary;
  showStatus?: ShowStatus;
  nextAiring?: NextAiring;
  seasonNames?: SeasonData[];
  episodes: Episode[];
}

export interface AnimeLoadResponse extends LoadResponse {
  type: TvType.Anime | TvType.OVA;
  episodes: Record<DubStatus, Episode[]>;
  engName?: string;
  japName?: string;
  synonyms?: string[];
  showStatus?: ShowStatus;
  nextAiring?: NextAiring;
  seasonNames?: SeasonData[];
}

export interface LiveStreamLoadResponse extends LoadResponse {
  type: TvType.Live;
  dataUrl: string;
}

export interface TorrentLoadResponse extends LoadResponse {
  type: TvType.Torrent;
  magnet?: string;
  torrent?: string;
}

export type AnyLoadResponse =
  | MovieLoadResponse
  | TvSeriesLoadResponse
  | AnimeLoadResponse
  | LiveStreamLoadResponse
  | TorrentLoadResponse;

// ============================================================================
// Home page (CS3 MainAPI.kt:1270-1297)
// ============================================================================

/** CS3 MainAPI.kt:416-421 */
export interface MainPageRequest {
  name: string;
  data: string;
  horizontalImages: boolean;
}

/** CS3 MainAPI.kt:410-414 */
export interface MainPageData {
  name: string;
  data: string;
  horizontalImages?: boolean;
}

/** CS3 MainAPI.kt:1282-1286 */
export interface HomePageList {
  name: string;
  list: SearchResponse[];
  isHorizontalImages?: boolean;
}

/** CS3 MainAPI.kt:1270-1275 */
export interface HomePageResponse {
  items: HomePageList[];
  hasNext?: boolean;
}

/** CS3 MainAPI.kt:1292-1297 */
export interface SearchResponseList {
  items: SearchResponse[];
  hasNext?: boolean;
}

// ============================================================================
// Extractor links (CS3 ExtractorApi.kt:703-717 + DRM variant)
// ============================================================================

export interface ExtractorLink {
  source: string; // provider/extractor name
  name: string;
  url: string;
  referer: string;
  quality: number; // height as int (e.g. 720) or Qualities.Unknown.value
  headers?: Record<string, string>;
  extractorData?: string; // opaque token for extractorVerifierJob
  type: ExtractorLinkType;
  audioTracks?: AudioFile[];
  isM3u8?: boolean;
  isDash?: boolean;
}

export interface DrmExtractorLink extends ExtractorLink {
  uuid: string; // CLEARKEY / WIDEVINE / PLAYREADY UUID
  kid: string;
  key: string;
  licenseUrl?: string;
}

// ============================================================================
// Provider metadata (manifest.json equivalent)
// ============================================================================

export interface ProviderInfo {
  name: string;
  mainUrl: string;
  lang: string;
  supportedTypes: TvType[];
  hasMainPage: boolean;
  hasQuickSearch: boolean;
  hasChromecastSupport: boolean;
  hasDownloadSupport: boolean;
  usesWebView: boolean;
  providerType: ProviderType;
  /** "typescript" (built-in) or "kotlin" (via JVM sidecar) */
  tier: "typescript" | "kotlin";
  enabled: boolean;
}

// ============================================================================
// Builder functions (mirror CS3's new* helpers)
// ============================================================================

export function newMovieSearchResponse(
  name: string,
  url: string,
  init: Partial<MovieSearchResponse> = {},
): MovieSearchResponse {
  return {
    name,
    url,
    apiName: init.apiName ?? "",
    type: init.type ?? TvType.Movie,
    posterUrl: init.posterUrl,
    posterHeaders: init.posterHeaders,
    id: init.id,
    quality: init.quality,
    score: init.score,
    year: init.year,
  };
}

export function newTvSeriesSearchResponse(
  name: string,
  url: string,
  init: Partial<TvSeriesSearchResponse> = {},
): TvSeriesSearchResponse {
  return {
    name,
    url,
    apiName: init.apiName ?? "",
    type: init.type ?? TvType.TvSeries,
    posterUrl: init.posterUrl,
    posterHeaders: init.posterHeaders,
    id: init.id,
    quality: init.quality,
    score: init.score,
    year: init.year,
    episodes: init.episodes,
  };
}

export function newAnimeSearchResponse(
  name: string,
  url: string,
  init: Partial<AnimeSearchResponse> = {},
): AnimeSearchResponse {
  return {
    name,
    url,
    apiName: init.apiName ?? "",
    type: init.type ?? TvType.Anime,
    posterUrl: init.posterUrl,
    posterHeaders: init.posterHeaders,
    id: init.id,
    quality: init.quality,
    score: init.score,
    year: init.year,
    dubStatus: init.dubStatus ?? [],
    otherName: init.otherName,
    episodes: init.episodes,
  };
}

export function newLiveSearchResponse(
  name: string,
  url: string,
  init: Partial<LiveSearchResponse> = {},
): LiveSearchResponse {
  return {
    name,
    url,
    apiName: init.apiName ?? "",
    type: TvType.Live,
    posterUrl: init.posterUrl,
    posterHeaders: init.posterHeaders,
    id: init.id,
    quality: init.quality,
    score: init.score,
    lang: init.lang,
  };
}

export function newTorrentSearchResponse(
  name: string,
  url: string,
  init: Partial<TorrentSearchResponse> = {},
): TorrentSearchResponse {
  return {
    name,
    url,
    apiName: init.apiName ?? "",
    type: TvType.Torrent,
    posterUrl: init.posterUrl,
    posterHeaders: init.posterHeaders,
    id: init.id,
    quality: init.quality,
    score: init.score,
    seeders: init.seeders,
    leechers: init.leechers,
    size: init.size,
    magnet: init.magnet,
    torrent: init.torrent,
  };
}

export function newMovieLoadResponse(
  name: string,
  url: string,
  apiName: string,
  dataUrl: string,
  init: Partial<MovieLoadResponse> = {},
): MovieLoadResponse {
  return {
    name,
    url,
    apiName,
    type: init.type ?? TvType.Movie,
    posterUrl: init.posterUrl,
    year: init.year,
    plot: init.plot,
    score: init.score,
    tags: init.tags,
    duration: init.duration,
    trailers: init.trailers ?? [],
    recommendations: init.recommendations,
    actors: init.actors,
    comingSoon: init.comingSoon ?? false,
    syncData: init.syncData ?? {},
    posterHeaders: init.posterHeaders,
    backgroundPosterUrl: init.backgroundPosterUrl,
    logoUrl: init.logoUrl,
    contentRating: init.contentRating,
    uniqueUrl: init.uniqueUrl ?? url,
    dataUrl,
  };
}

export function newTvSeriesLoadResponse(
  name: string,
  url: string,
  apiName: string,
  episodes: Episode[],
  init: Partial<TvSeriesLoadResponse> = {},
): TvSeriesLoadResponse {
  return {
    name,
    url,
    apiName,
    type: init.type ?? TvType.TvSeries,
    posterUrl: init.posterUrl,
    year: init.year,
    plot: init.plot,
    score: init.score,
    tags: init.tags,
    duration: init.duration,
    trailers: init.trailers ?? [],
    recommendations: init.recommendations,
    actors: init.actors,
    comingSoon: init.comingSoon ?? false,
    syncData: init.syncData ?? {},
    posterHeaders: init.posterHeaders,
    backgroundPosterUrl: init.backgroundPosterUrl,
    logoUrl: init.logoUrl,
    contentRating: init.contentRating,
    uniqueUrl: init.uniqueUrl ?? url,
    showStatus: init.showStatus,
    nextAiring: init.nextAiring,
    seasonNames: init.seasonNames,
    episodes,
  };
}

export function newAnimeLoadResponse(
  name: string,
  url: string,
  apiName: string,
  episodes: Record<DubStatus, Episode[]>,
  init: Partial<AnimeLoadResponse> = {},
): AnimeLoadResponse {
  return {
    name,
    url,
    apiName,
    type: init.type ?? TvType.Anime,
    posterUrl: init.posterUrl,
    year: init.year,
    plot: init.plot,
    score: init.score,
    tags: init.tags,
    duration: init.duration,
    trailers: init.trailers ?? [],
    recommendations: init.recommendations,
    actors: init.actors,
    comingSoon: init.comingSoon ?? false,
    syncData: init.syncData ?? {},
    posterHeaders: init.posterHeaders,
    backgroundPosterUrl: init.backgroundPosterUrl,
    logoUrl: init.logoUrl,
    contentRating: init.contentRating,
    uniqueUrl: init.uniqueUrl ?? url,
    engName: init.engName,
    japName: init.japName,
    synonyms: init.synonyms,
    showStatus: init.showStatus,
    nextAiring: init.nextAiring,
    seasonNames: init.seasonNames,
    episodes,
  };
}

export function newEpisode(
  data: string,
  init: Partial<Episode> = {},
): Episode {
  return {
    data,
    name: init.name,
    season: init.season,
    episode: init.episode,
    posterUrl: init.posterUrl,
    score: init.score,
    description: init.description,
    date: init.date,
    runTime: init.runTime,
  };
}

export function newExtractorLink(
  source: string,
  name: string,
  url: string,
  init: Partial<ExtractorLink> = {},
): ExtractorLink {
  const type: ExtractorLinkType =
    init.type ??
    inferTypeFromUrl(url);
  return {
    source,
    name,
    url,
    referer: init.referer ?? "",
    quality: init.quality ?? Qualities.Unknown,
    headers: init.headers ?? {},
    extractorData: init.extractorData,
    type,
    audioTracks: init.audioTracks ?? [],
    isM3u8: type === ExtractorLinkType.M3U8,
    isDash: type === ExtractorLinkType.DASH,
  };
}

export function newSubtitleFile(
  lang: string,
  url: string,
  init: Partial<SubtitleFile> = {},
): SubtitleFile {
  return { lang, url, headers: init.headers };
}

export function newHomePageResponse(
  name: string,
  list: SearchResponse[],
  init: Partial<HomePageResponse> = {},
): HomePageResponse {
  return {
    items: [{ name, list }],
    hasNext: init.hasNext,
  };
}

export function mainPageOf(
  ...pages: MainPageData[]
): MainPageData[] {
  return pages;
}

export function newSearchResponseList(
  items: SearchResponse[],
  hasNext = false,
): SearchResponseList {
  return { items, hasNext };
}

// ============================================================================
// Helpers
// ============================================================================

export function inferTypeFromUrl(url: string): ExtractorLinkType {
  const lower = url.toLowerCase();
  if (lower.includes(".m3u8")) return ExtractorLinkType.M3U8;
  if (lower.includes(".mpd")) return ExtractorLinkType.DASH;
  if (lower.startsWith("magnet:")) return ExtractorLinkType.MAGNET;
  if (lower.includes(".torrent")) return ExtractorLinkType.TORRENT;
  return ExtractorLinkType.VIDEO;
}

/** CS3 USER_AGENT constant (MainAPI.kt:93) */
export const USER_AGENT =
  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36";

/** CS3 AllLanguagesName constant */
export const ALL_LANGUAGES_NAME = "universal";
