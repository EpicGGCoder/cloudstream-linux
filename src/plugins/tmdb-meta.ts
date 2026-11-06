/**
 * TMDB meta-provider — TypeScript port of CS3's TmdbProvider.
 *
 * Uses the same public API key as CS3 (see ARCHITECTURE.md §9).
 * This is a MetaProvider: it returns metadata, not playable links.
 * Real streaming providers (Phase 3, via JVM sidecar) consume the TMDB id
 * and resolve to their own site URLs in their `load()` method.
 */

import { MainAPI } from "@/lib/plugins/contract";
import {
  DubStatus,
  Episode,
  ExtractorLink,
  HomePageResponse,
  LoadResponse,
  ProviderType,
  Score,
  SearchResponse,
  SearchResponseList,
  SubtitleFile,
  TvType,
  type MainPageRequest,
  newAnimeSearchResponse,
  newEpisode,
  newHomePageResponse,
  newMovieLoadResponse,
  newMovieSearchResponse,
  newSearchResponseList,
  newTvSeriesLoadResponse,
  newTvSeriesSearchResponse,
} from "@/lib/plugins/types";
import {
  tmdb,
  tmdbBackdrop,
  tmdbLogo,
  tmdbPoster,
  type TmdbMovie,
  type TmdbMovieDetail,
  type TmdbTv,
  type TmdbTvDetail,
} from "@/lib/tmdb/client";

const PROVIDER_NAME = "TMDB";
const PROVIDER_URL = "https://www.themoviedb.org";

class TmdbMetaProvider extends MainAPI {
  override name = PROVIDER_NAME;
  override mainUrl = PROVIDER_URL;
  override lang = "en";
  override hasMainPage = true;
  override hasQuickSearch = false;
  override supportedTypes = [
    TvType.Movie,
    TvType.TvSeries,
    TvType.Anime,
    TvType.Documentary,
  ];
  override providerType = ProviderType.MetaProvider;
  override mainPage = [
    { name: "Trending Today", data: "trending/day", horizontalImages: false },
    { name: "Trending This Week", data: "trending/week", horizontalImages: false },
    { name: "Popular Movies", data: "movies/popular", horizontalImages: false },
    { name: "Popular TV Shows", data: "tv/popular", horizontalImages: false },
    { name: "Top Rated Movies", data: "movies/top_rated", horizontalImages: false },
    { name: "Top Rated TV Shows", data: "tv/top_rated", horizontalImages: false },
    { name: "Now Playing (Cinema)", data: "movies/now_playing", horizontalImages: false },
    { name: "On The Air (TV)", data: "tv/on_the_air", horizontalImages: false },
    { name: "Upcoming Movies", data: "movies/upcoming", horizontalImages: false },
  ];

  // URL scheme: tmdb://movie/{id} or tmdb://tv/{id}
  private movieUrl(id: number) { return `tmdb://movie/${id}`; }
  private tvUrl(id: number) { return `tmdb://tv/${id}`; }
  private parseUrl(url: string): { kind: "movie" | "tv"; id: number } | null {
    const m = url.match(/^tmdb:\/\/(movie|tv)\/(\d+)$/);
    if (!m) return null;
    return { kind: m[1] as "movie" | "tv", id: Number(m[2]) };
  }

  // ------------------------------------------------------------------------
  // Home page
  // ------------------------------------------------------------------------
  override async getMainPage(page: number, request: MainPageRequest): Promise<HomePageResponse | null> {
    if (page > 1) return null; // TMDB lists are paginated but we only show page 1 per row for simplicity
    try {
      const data = request.data;
      let results: (TmdbMovie | TmdbTv)[] = [];

      if (data === "trending/day") results = (await tmdb.trending("day")).results;
      else if (data === "trending/week") results = (await tmdb.trending("week")).results;
      else if (data === "movies/popular") results = (await tmdb.popularMovies()).results;
      else if (data === "movies/top_rated") results = (await tmdb.topRatedMovies()).results;
      else if (data === "movies/upcoming") results = (await tmdb.upcomingMovies()).results;
      else if (data === "movies/now_playing") results = (await tmdb.nowPlayingMovies()).results;
      else if (data === "tv/popular") results = (await tmdb.popularTv()).results;
      else if (data === "tv/top_rated") results = (await tmdb.topRatedTv()).results;
      else if (data === "tv/on_the_air") results = (await tmdb.onTheAirTv()).results;
      else if (data === "tv/airing_today") results = (await tmdb.airingTodayTv()).results;
      else return null;

      const items = results
        .map((r) => this.toSearchResponse(r))
        .filter((r): r is SearchResponse => r !== null);

      return newHomePageResponse(request.name, items);
    } catch (e) {
      console.error("[tmdb] getMainPage error:", e);
      return null;
    }
  }

  // ------------------------------------------------------------------------
  // Search
  // ------------------------------------------------------------------------
  override async search(query: string, page: number): Promise<SearchResponseList | null> {
    try {
      const res = await tmdb.searchMulti(query, page);
      const items = res.results
        .filter((r) => "title" in r || "name" in r)
        .map((r) => this.toSearchResponse(r))
        .filter((r): r is SearchResponse => r !== null);
      return newSearchResponseList(items, res.page < res.total_pages);
    } catch (e) {
      console.error("[tmdb] search error:", e);
      return null;
    }
  }

  // ------------------------------------------------------------------------
  // Load (detail page)
  // ------------------------------------------------------------------------
  override async load(url: string): Promise<LoadResponse | null> {
    const parsed = this.parseUrl(url);
    if (!parsed) return null;

    try {
      if (parsed.kind === "movie") {
        const m = await tmdb.movieDetail(parsed.id);
        return this.toMovieLoadResponse(m);
      } else {
        const t = await tmdb.tvDetail(parsed.id);
        return await this.toTvSeriesLoadResponse(t);
      }
    } catch (e) {
      console.error("[tmdb] load error:", e);
      return null;
    }
  }

  // ------------------------------------------------------------------------
  // loadLinks — meta-provider returns no links.
  // Real providers consume the TMDB id in their own load() to find streams.
  // ------------------------------------------------------------------------
  override async loadLinks(
    _data: string,
    _isCasting: boolean,
    _subtitleCallback: (s: SubtitleFile) => void,
    _callback: (l: ExtractorLink) => void,
  ): Promise<boolean> {
    // MetaProvider — no direct streams. The UI will prompt the user to
    // install a streaming provider plugin to get playable sources.
    return false;
  }

  // ------------------------------------------------------------------------
  // Helpers — convert TMDB DTOs to CS3 data shapes
  // ------------------------------------------------------------------------
  private isMovie(r: TmdbMovie | TmdbTv): r is TmdbMovie {
    return "title" in r || !("name" in r);
  }

  private toSearchResponse(r: TmdbMovie | TmdbTv): SearchResponse | null {
    // Determine media type — TMDB multi-search returns a `media_type` field,
    // but standalone movie/tv endpoints don't, so we infer from fields.
    const isMovie = this.isMovie(r);
    const id = r.id;
    const url = isMovie ? this.movieUrl(id) : this.tvUrl(id);
    const name = (isMovie ? (r as TmdbMovie).title : (r as TmdbTv).name) ?? "Untitled";
    const yearStr = isMovie ? (r as TmdbMovie).release_date : (r as TmdbTv).first_air_date;
    const year = yearStr ? Number(yearStr.substring(0, 4)) : undefined;
    const poster = tmdbPoster(r.poster_path);

    if (isMovie) {
      return newMovieSearchResponse(name, url, {
        apiName: PROVIDER_NAME,
        posterUrl: poster,
        year,
        score: Score.from10(r.vote_average),
      });
    } else {
      return newTvSeriesSearchResponse(name, url, {
        apiName: PROVIDER_NAME,
        posterUrl: poster,
        year,
        score: Score.from10(r.vote_average),
      });
    }
  }

  private pickOfficialTrailer(videos?: { results: { key: string; site: string; type: string; official?: boolean }[] }): { url: string; name: string } | null {
    if (!videos?.results?.length) return null;
    const yt = videos.results.filter((v) => v.site === "YouTube" && v.type === "Trailer");
    if (!yt.length) return null;
    const official = yt.find((v) => v.official) ?? yt[0];
    return { url: `https://www.youtube.com/watch?v=${official.key}`, name: official.key };
  }

  private toMovieLoadResponse(m: TmdbMovieDetail): LoadResponse {
    const url = this.movieUrl(m.id);
    const trailer = this.pickOfficialTrailer(m.videos);

    return newMovieLoadResponse(m.title, url, PROVIDER_NAME, url, {
      apiName: PROVIDER_NAME,
      type: TvType.Movie,
      posterUrl: tmdbPoster(m.poster_path, "w500"),
      backgroundPosterUrl: tmdbBackdrop(m.backdrop_path),
      year: m.release_date ? Number(m.release_date.substring(0, 4)) : undefined,
      plot: m.overview,
      score: Score.from10(m.vote_average),
      tags: m.genres?.map((g) => g.name) ?? [],
      duration: m.runtime ? m.runtime * 60 : undefined,
      trailers: trailer
        ? [{ extractorUrl: trailer.url, raw: true }]
        : [],
      recommendations: m.recommendations?.results
        ?.slice(0, 12)
        .map((r) => this.toSearchResponse(r))
        .filter((r): r is SearchResponse => r !== null) ?? [],
      actors: m.credits?.cast?.slice(0, 15).map((c) => ({
        actor: { name: c.name, image: tmdbPoster(c.profile_path, "w185") },
        roleString: c.character,
      })),
      logoUrl: m.images?.logos?.find((l) => l.iso_639_1 === "en")?.file_path
        ? tmdbLogo(m.images.logos.find((l) => l.iso_639_1 === "en")!.file_path, "w300")
        : m.images?.logos?.[0]?.file_path
          ? tmdbLogo(m.images.logos[0].file_path, "w300")
          : undefined,
      syncData: {
        tmdb: String(m.id),
        ...(m.imdb_id ? { imdb: m.imdb_id } : {}),
      },
    });
  }

  private async toTvSeriesLoadResponse(t: TmdbTvDetail): Promise<LoadResponse> {
    const url = this.tvUrl(t.id);
    const trailer = this.pickOfficialTrailer(t.videos);

    // TMDB /tv/{id} returns only season metadata, not episodes.
    // Fetch each season in parallel to get the full episode list.
    const seasonsMeta = (t.seasons ?? []).filter((s) => s.season_number >= 0);
    const seasonDetails = await Promise.all(
      seasonsMeta.map((s) =>
        tmdb.seasonDetail(t.id, s.season_number).catch(() => null),
      ),
    );

    const episodes: Episode[] = [];
    for (const season of seasonDetails) {
      if (!season) continue;
      for (const ep of season.episodes ?? []) {
        episodes.push(
          newEpisode(`tmdb://tv/${t.id}/season/${ep.season_number}/episode/${ep.episode_number}`, {
            name: ep.name,
            season: ep.season_number,
            episode: ep.episode_number,
            description: ep.overview,
            posterUrl: tmdbPoster(ep.still_path, "w342"),
            score: Score.from10(ep.vote_average),
            runTime: ep.runtime ? ep.runtime * 60 : undefined,
            date: ep.air_date ? Date.parse(ep.air_date) : undefined,
          }),
        );
      }
    }

    return newTvSeriesLoadResponse(t.name, url, PROVIDER_NAME, episodes, {
      apiName: PROVIDER_NAME,
      type: TvType.TvSeries,
      posterUrl: tmdbPoster(t.poster_path, "w500"),
      backgroundPosterUrl: tmdbBackdrop(t.backdrop_path),
      year: t.first_air_date ? Number(t.first_air_date.substring(0, 4)) : undefined,
      plot: t.overview,
      score: Score.from10(t.vote_average),
      tags: t.genres?.map((g) => g.name) ?? [],
      trailers: trailer
        ? [{ extractorUrl: trailer.url, raw: true }]
        : [],
      seasonNames: seasons.map((s) => ({
        season: s.season_number,
        name: s.name,
      })),
      recommendations: t.recommendations?.results
        ?.slice(0, 12)
        .map((r) => this.toSearchResponse(r))
        .filter((r): r is SearchResponse => r !== null) ?? [],
      actors: t.credits?.cast?.slice(0, 15).map((c) => ({
        actor: { name: c.name, image: tmdbPoster(c.profile_path, "w185") },
        roleString: c.character,
      })),
      logoUrl: t.images?.logos?.find((l) => l.iso_639_1 === "en")?.file_path
        ? tmdbLogo(t.images.logos.find((l) => l.iso_639_1 === "en")!.file_path, "w300")
        : t.images?.logos?.[0]?.file_path
          ? tmdbLogo(t.images.logos[0].file_path, "w300")
          : undefined,
      syncData: {
        tmdb: String(t.id),
      },
    });
  }
}

export const tmdbMetaProvider = new TmdbMetaProvider();
