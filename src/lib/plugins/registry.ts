/**
 * Plugin registry — manages all registered providers.
 *
 * Mirrors CS3's APIHolder.allProviders + apis + apiMap.
 * Supports two tiers of provider:
 *   - "typescript": a MainAPI subclass loaded via dynamic import (built-in TS plugin)
 *   - "kotlin": a real .cs3 plugin loaded via the JVM sidecar (Phase 3)
 *
 * Both tiers are exposed through the same Provider interface so the UI doesn't
 * care which tier a provider is on.
 */

import { MainAPI } from "./contract";
import type {
  ExtractorLink,
  HomePageResponse,
  LoadResponse,
  MainPageRequest,
  ProviderInfo,
  SearchResponseList,
  SubtitleFile,
} from "./types";

/** Unified provider interface — what the UI/API layer talks to. */
export interface Provider {
  info: ProviderInfo;
  search(query: string, page: number): Promise<SearchResponseList | null>;
  quickSearch(query: string): Promise<SearchResponseList | null>;
  load(url: string): Promise<LoadResponse | null>;
  loadLinks(
    data: string,
    isCasting: boolean,
  ): Promise<{ links: ExtractorLink[]; subtitles: SubtitleFile[] }>;
  getMainPage?(page: number, request: MainPageRequest): Promise<HomePageResponse | null>;
}

/** Wraps a MainAPI instance as a Provider. */
class MainAPIProviderAdapter implements Provider {
  constructor(private readonly api: MainAPI) {}

  get info(): ProviderInfo {
    return this.api.info();
  }

  async search(query: string, page: number) {
    return this.api.search(query, page);
  }

  async quickSearch(query: string) {
    const r = await this.api.quickSearch(query);
    return r ? { items: r, hasNext: false } : null;
  }

  async load(url: string) {
    return this.api.load(url);
  }

  async loadLinks(data: string, isCasting: boolean) {
    const links: ExtractorLink[] = [];
    const subtitles: SubtitleFile[] = [];
    await this.api.loadLinks(
      data,
      isCasting,
      (s) => subtitles.push(s),
      (l) => links.push(l),
    );
    return { links, subtitles };
  }

  async getMainPage(page: number, request: MainPageRequest) {
    return this.api.getMainPage(page, request);
  }
}

class PluginRegistry {
  private providers = new Map<string, Provider>();

  register(api: MainAPI): void {
    const name = api.name;
    if (this.providers.has(name)) {
      console.warn(`[plugins] overwriting provider "${name}"`);
    }
    this.providers.set(name, new MainAPIProviderAdapter(api));
  }

  unregister(name: string): void {
    this.providers.delete(name);
  }

  get(name: string): Provider | undefined {
    return this.providers.get(name);
  }

  list(): Provider[] {
    return Array.from(this.providers.values());
  }

  listEnabled(): Provider[] {
    return this.list().filter((p) => p.info.enabled);
  }

  listByType(type: string): Provider[] {
    return this.listEnabled().filter((p) => p.info.supportedTypes.includes(type as never));
  }

  clear(): void {
    this.providers.clear();
  }
}

/** Global singleton — equivalent to CS3's APIHolder.allProviders. */
export const registry = new PluginRegistry();
