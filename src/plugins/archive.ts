/**
 * Internet Archive provider — public-domain movies & video.
 *
 * Source: https://archive.org/ (legal, public-domain / CC content)
 * API docs: https://archive.org/advancedsearch.php
 *
 * This provider returns real, playable video URLs — making it the only
 * built-in provider that lets the user actually watch something end-to-end.
 * It demonstrates the full plugin lifecycle: home → search → detail → play.
 */

import { MainAPI } from "@/lib/plugins/contract";
import { app } from "@/lib/http/app";
import {
  ExtractorLink,
  ExtractorLinkType,
  HomePageResponse,
  LoadResponse,
  Qualities,
  Score,
  SearchResponse,
  SearchResponseList,
  SubtitleFile,
  TvType,
  type MainPageRequest,
  newEpisode,
  newExtractorLink,
  newHomePageResponse,
  newMovieLoadResponse,
  newMovieSearchResponse,
  newSearchResponseList,
  newTvSeriesLoadResponse,
} from "@/lib/plugins/types";

const PROVIDER_NAME = "Internet Archive";
const PROVIDER_URL = "https://archive.org";

interface ArchiveSearchResponse {
  response: {
    numFound: number;
    start: number;
    docs: ArchiveDoc[];
  };
}

interface ArchiveDoc {
  identifier: string;
  title?: string;
  description?: string | string[];
  year?: number;
  publicdate?: string;
  median_runtime?: string;
  downloads?: number;
}

interface ArchiveMetadataResponse {
  server: string;
  dir: string;
  metadata: {
    identifier: string;
    title?: string;
    description?: string;
    year?: string;
    runtime?: string;
    collection?: string | string[];
  };
  files: ArchiveFile[];
}

interface ArchiveFile {
  name: string;
  format: string;
  size?: string;
  length?: string;
  mtime?: string;
}

class ArchiveProvider extends MainAPI {
  override name = PROVIDER_NAME;
  override mainUrl = PROVIDER_URL;
  override lang = "en";
  override hasMainPage = true;
  override supportedTypes = [TvType.Movie, TvType.Documentary, TvType.Others];
  override mainPage = [
    { name: "Classic Feature Films", data: "collection:feature_films", horizontalImages: false },
    { name: "Classic Comedy", data: "collection:comedy", horizontalImages: false },
    { name: "Sci-Fi Classics", data: "collection:SciFi_Horror", horizontalImages: false },
    { name: "Animation", data: "collection:animation", horizontalImages: false },
    { name: "Documentaries", data: "collection:documentaryfilms", horizontalImages: false },
    { name: "Newsreels", data: "collection:universal_newsreels", horizontalImages: false },
  ];

  private url(id: string) { return `archive://${id}`; }
  private parseUrl(url: string): string | null {
    const m = url.match(/^archive:\/\/(.+)$/);
    return m ? m[1] : null;
  }

  // ------------------------------------------------------------------------
  // Home page — one row per mainPage entry
  // ------------------------------------------------------------------------
  override async getMainPage(page: number, request: MainPageRequest): Promise<HomePageResponse | null> {
    if (page > 1) return null;
    try {
      const items = await this.searchCollection(request.data, 24);
      return newHomePageResponse(request.name, items);
    } catch (e) {
      console.error("[archive] getMainPage error:", e);
      return null;
    }
  }

  // ------------------------------------------------------------------------
  // Search
  // ------------------------------------------------------------------------
  override async search(query: string, page: number): Promise<SearchResponseList | null> {
    try {
      const start = (page - 1) * 24;
      const url =
        `https://archive.org/advancedsearch.php?q=${encodeURIComponent(
          `(${query}) AND mediatype:movies`,
        )}&fl[]=identifier&fl[]=title&fl[]=year&fl[]=description&fl[]=median_runtime&fl[]=downloads&sort[]=downloads%20desc&rows=24&start=${start}&output=json`;
      const r = await app.get(url);
      const data = await r.parsed<ArchiveSearchResponse>();
      const items = data.response.docs.map((d) => this.docToSearchResponse(d));
      return newSearchResponseList(items, data.response.start + items.length < data.response.numFound);
    } catch (e) {
      console.error("[archive] search error:", e);
      return null;
    }
  }

  // ------------------------------------------------------------------------
  // Load (detail page)
  // ------------------------------------------------------------------------
  override async load(url: string): Promise<LoadResponse | null> {
    const id = this.parseUrl(url);
    if (!id) return null;

    try {
      const r = await app.get(`https://archive.org/metadata/${id}`);
      const meta = await r.parsed<ArchiveMetadataResponse>();
      const title = meta.metadata.title ?? id;
      const year = meta.metadata.year ? Number(meta.metadata.year) : undefined;
      const runtime = meta.metadata.runtime ? Number(meta.metadata.runtime.split(":")[0]) : undefined;
      const description = typeof meta.metadata.description === "string" ? meta.metadata.description : undefined;

      // Find the best playable mp4 (preferred) or other video format
      const mp4 = meta.files.find((f) => f.format === "h.264" || (f.name.endsWith(".mp4") && f.format !== "Metadata"));
      const ogg = meta.files.find((f) => f.format === "512Kb MPEG4" || f.name.endsWith(".ogv"));
      const playableFile = mp4 ?? ogg;

      const dataUrl = playableFile
        ? `https://archive.org/download/${id}/${encodeURIComponent(playableFile.name)}`
        : "";

      // If there are multiple MPEG4 files (e.g. multi-part documentary), treat as TvSeries
      const mp4Files = meta.files.filter((f) =>
        f.name.endsWith(".mp4") &&
        f.format !== "Metadata" &&
        f.format !== "h.264 IA" &&
        !f.name.toLowerCase().includes("_meta"),
      );

      if (mp4Files.length > 1) {
        // Multi-part — model as a TV series with each file as an episode
        const episodes = mp4Files.map((f, i) =>
          newEpisode(`https://archive.org/download/${id}/${encodeURIComponent(f.name)}`, {
            name: f.name.replace(/\.mp4$/i, "").replace(/_/g, " "),
            episode: i + 1,
            season: 1,
            runTime: f.length ? parseRuntime(f.length) : undefined,
          }),
        );
        return newTvSeriesLoadResponse(title, this.url(id), PROVIDER_NAME, episodes, {
          apiName: PROVIDER_NAME,
          type: TvType.TvSeries,
          posterUrl: `https://archive.org/services/img/${id}`,
          year,
          plot: description,
          duration: runtime ? runtime * 60 : undefined,
        });
      }

      return newMovieLoadResponse(title, this.url(id), PROVIDER_NAME, dataUrl, {
        apiName: PROVIDER_NAME,
        type: TvType.Movie,
        posterUrl: `https://archive.org/services/img/${id}`,
        year,
        plot: description,
        score: Score.from10(meta.metadata.collection ? 7 : 5),
      });
    } catch (e) {
      console.error("[archive] load error:", e);
      return null;
    }
  }

  // ------------------------------------------------------------------------
  // loadLinks — return the direct playable URL
  // ------------------------------------------------------------------------
  override async loadLinks(
    data: string,
    _isCasting: boolean,
    _subtitleCallback: (s: SubtitleFile) => void,
    callback: (l: ExtractorLink) => void,
  ): Promise<boolean> {
    if (!data.startsWith("http")) return false;
    callback(
      newExtractorLink(PROVIDER_NAME, PROVIDER_NAME, data, {
        referer: PROVIDER_URL,
        quality: Qualities.Unknown,
        type: ExtractorLinkType.VIDEO,
      }),
    );
    return true;
  }

  // ------------------------------------------------------------------------
  // Helpers
  // ------------------------------------------------------------------------
  private async searchCollection(query: string, rows: number): Promise<SearchResponse[]> {
    const url =
      `https://archive.org/advancedsearch.php?q=${encodeURIComponent(
        `${query} AND mediatype:movies`,
      )}&fl[]=identifier&fl[]=title&fl[]=year&fl[]=description&fl[]=median_runtime&fl[]=downloads&sort[]=downloads%20desc&rows=${rows}&page=1&output=json`;
    const r = await app.get(url);
    const data = await r.parsed<ArchiveSearchResponse>();
    return data.response.docs.map((d) => this.docToSearchResponse(d));
  }

  private docToSearchResponse(d: ArchiveDoc): SearchResponse {
    const name = d.title ?? d.identifier;
    return newMovieSearchResponse(name, this.url(d.identifier), {
      apiName: PROVIDER_NAME,
      posterUrl: `https://archive.org/services/img/${d.identifier}`,
      year: d.year,
      score: Score.from10(d.downloads ? Math.min(10, Math.log10(d.downloads + 1) * 2) : 5),
    });
  }
}

function parseRuntime(lengthStr: string): number | undefined {
  // Format: "HH:MM:SS" or "MM:SS" or "SS"
  const parts = lengthStr.split(":").map((p) => Number(p));
  if (parts.some((p) => Number.isNaN(p))) return undefined;
  let s = 0;
  for (const p of parts) s = s * 60 + p;
  return s;
}

export const archiveProvider = new ArchiveProvider();
