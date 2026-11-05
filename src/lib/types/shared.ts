/**
 * Client-side type re-exports.
 *
 * Re-exports the plugin data shapes needed by the UI without dragging in
 * the server-only Score class implementation details. The client always
 * receives Score as a plain `{ data: number }` object after JSON round-trip.
 *
 * Use `import type { ... } from "@/lib/types/shared"` from client components.
 */

export type {
  TvType,
  ProviderType,
  VPNStatus,
  ShowStatus,
  DubStatus,
  SearchQuality,
  ExtractorLinkType,
  Qualities,
  ActorRole,
  SyncIdName,
} from "@/lib/plugins/types";

export type {
  Episode,
  NextAiring,
  SeasonData,
  Actor,
  ActorData,
  AudioFile,
  SubtitleFile,
  TrailerData,
  Tracker,
  SearchResponse,
  MovieSearchResponse,
  TvSeriesSearchResponse,
  AnimeSearchResponse,
  LiveSearchResponse,
  TorrentSearchResponse,
  LoadResponse,
  MovieLoadResponse,
  TvSeriesLoadResponse,
  AnimeLoadResponse,
  LiveStreamLoadResponse,
  TorrentLoadResponse,
  MainPageRequest,
  MainPageData,
  HomePageList,
  HomePageResponse,
  SearchResponseList,
  ExtractorLink,
  DrmExtractorLink,
  ProviderInfo,
} from "@/lib/plugins/types";
