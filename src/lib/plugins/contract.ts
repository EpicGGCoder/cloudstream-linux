/**
 * MainAPI abstract class — TypeScript port of CS3's MainAPI.
 *
 * Plugins extend this class and implement the abstract methods.
 * See ARCHITECTURE.md §1 for the full contract reference.
 *
 * To write a plugin:
 *
 *   import { MainAPI, TvType } from "@/lib/plugins/contract";
 *   import { newMovieSearchResponse, newMovieLoadResponse, newExtractorLink } from "@/lib/plugins/types";
 *
 *   export default class MyProvider extends MainAPI {
 *     override name = "MyProvider";
 *     override mainUrl = "https://example.com";
 *     override supportedTypes = [TvType.Movie];
 *
 *     override async search(query: string, page: number) {
 *       const res = await app.get(`${this.mainUrl}/search?q=${encodeURIComponent(query)}&page=${page}`);
 *       return newSearchResponseList([...]);
 *     }
 *
 *     override async load(url: string) { ... }
 *     override async loadLinks(data, isCasting, subCb, linkCb) { ... }
 *   }
 */

import type {
  ExtractorLink,
  HomePageResponse,
  LoadResponse,
  MainPageData,
  MainPageRequest,
  ProviderInfo,
  ProviderType,
  SearchResponse,
  SearchResponseList,
  SubtitleFile,
  SyncIdName,
  TvType,
  VPNStatus,
} from "./types";

export abstract class MainAPI {
  // --- Identity ---
  name = "NONE";
  mainUrl = "NONE";
  lang = "en";

  // --- Capabilities ---
  hasMainPage = false;
  hasQuickSearch = false;
  hasChromecastSupport = true;
  hasDownloadSupport = true;
  usesWebView = false;

  // --- Classification ---
  supportedTypes: TvType[] = [];
  providerType: ProviderType = "DirectProvider" as ProviderType;
  vpnStatus: VPNStatus = "None" as VPNStatus;

  // --- Home page config ---
  mainPage: MainPageData[] = [];
  sequentialMainPage = false;
  sequentialMainPageDelay = 0;
  sequentialMainPageScrollDelay = 0;

  // --- Timeouts (ms; null = use default) ---
  loadLinksTimeoutMs?: number;
  getMainPageTimeoutMs?: number;
  searchTimeoutMs?: number;
  quickSearchTimeoutMs?: number;
  loadTimeoutMs?: number;

  // --- Sync support ---
  supportedSyncNames: Set<SyncIdName> = new Set();

  // --- Plugin source tracking ---
  sourcePlugin?: string;

  // ========================================================================
  // Abstract methods — every provider MUST implement these
  // ========================================================================

  abstract search(query: string, page: number): Promise<SearchResponseList | null>;
  abstract load(url: string): Promise<LoadResponse | null>;
  abstract loadLinks(
    data: string,
    isCasting: boolean,
    subtitleCallback: (s: SubtitleFile) => void,
    callback: (l: ExtractorLink) => void,
  ): Promise<boolean>;

  // ========================================================================
  // Optional methods — providers override as needed
  // ========================================================================

  async getMainPage(
    page: number,
    request: MainPageRequest,
  ): Promise<HomePageResponse | null> {
    return null;
  }

  async quickSearch(query: string): Promise<SearchResponse[] | null> {
    return null;
  }

  async extractorVerifierJob(extractorData: string | null): Promise<void> {}

  async getLoadUrl(name: SyncIdName, id: string): Promise<string | null> {
    return null;
  }

  // ========================================================================
  // Metadata — for the registry / settings UI
  // ========================================================================

  info(): ProviderInfo {
    return {
      name: this.name,
      mainUrl: this.mainUrl,
      lang: this.lang,
      supportedTypes: this.supportedTypes,
      hasMainPage: this.hasMainPage,
      hasQuickSearch: this.hasQuickSearch,
      hasChromecastSupport: this.hasChromecastSupport,
      hasDownloadSupport: this.hasDownloadSupport,
      usesWebView: this.usesWebView,
      providerType: this.providerType,
      tier: "typescript",
      enabled: true,
    };
  }
}
