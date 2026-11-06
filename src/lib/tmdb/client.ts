/**
 * TMDB API client — direct REST wrapper around The Movie Database.
 *
 * Uses the same public API key CS3 uses (hardcoded in TmdbProvider.kt:67):
 *   e6333b32409e02a4a6eba6fb7ff866bb
 *
 * See ARCHITECTURE.md §9.
 */

const TMDB_API_KEY = "e6333b32409e02a4a6eba6fb7ff866bb";
const TMDB_BASE = "https://api.themoviedb.org/3";
export const TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p";

// Image URL helpers — TMDB has fixed size presets.
export function tmdbPoster(path?: string | null, size: "w200" | "w342" | "w500" | "original" = "w342"): string | undefined {
  if (!path) return undefined;
  if (path.startsWith("http")) return path;
  return `${TMDB_IMAGE_BASE}/${size}${path}`;
}

export function tmdbBackdrop(path?: string | null, size: "w780" | "w1280" | "original" = "w1280"): string | undefined {
  if (!path) return undefined;
  if (path.startsWith("http")) return path;
  return `${TMDB_IMAGE_BASE}/${size}${path}`;
}

export function tmdbLogo(path?: string | null, size: "w92" | "w154" | "w185" | "w300" | "original" = "w185"): string | undefined {
  if (!path) return undefined;
  if (path.startsWith("http")) return path;
  return `${TMDB_IMAGE_BASE}/${size}${path}`;
}

// ---------------------------------------------------------------------------
// TMDB response types (only what we use)
// ---------------------------------------------------------------------------

export interface TmdbPageResult<T> {
  page: number;
  results: T[];
  total_pages: number;
  total_results: number;
}

export interface TmdbMovie {
  id: number;
  title: string;
  original_title?: string;
  poster_path?: string;
  backdrop_path?: string;
  overview?: string;
  release_date?: string;
  vote_average?: number;
  vote_count?: number;
  genre_ids?: number[];
  popularity?: number;
  adult?: boolean;
  original_language?: string;
}

export interface TmdbTv {
  id: number;
  name: string;
  original_name?: string;
  poster_path?: string;
  backdrop_path?: string;
  overview?: string;
  first_air_date?: string;
  vote_average?: number;
  vote_count?: number;
  genre_ids?: number[];
  popularity?: number;
  origin_country?: string[];
  original_language?: string;
}

export interface TmdbGenre {
  id: number;
  name: string;
}

export interface TmdbEpisode {
  id: number;
  name: string;
  overview?: string;
  episode_number: number;
  season_number: number;
  air_date?: string;
  still_path?: string;
  vote_average?: number;
  runtime?: number;
}

export interface TmdbSeason {
  id: number;
  name: string;
  season_number: number;
  air_date?: string;
  poster_path?: string;
  overview?: string;
  episodes: TmdbEpisode[];
}

export interface TmdbMovieDetail extends TmdbMovie {
  imdb_id?: string;
  runtime?: number;
  genres?: TmdbGenre[];
  tagline?: string;
  homepage?: string;
  budget?: number;
  revenue?: number;
  status?: string;
  production_companies?: { id: number; name: string; logo_path?: string }[];
  videos?: { results: TmdbVideo[] };
  recommendations?: TmdbPageResult<TmdbMovie>;
  credits?: { cast: TmdbCast[] };
  images?: { logos: TmdbImage[] };
}

export interface TmdbTvDetail extends TmdbTv {
  number_of_seasons?: number;
  number_of_episodes?: number;
  episode_run_time?: number[];
  genres?: TmdbGenre[];
  seasons?: TmdbSeason[];
  status?: string;
  tagline?: string;
  homepage?: string;
  in_production?: boolean;
  last_air_date?: string;
  networks?: { id: number; name: string; logo_path?: string }[];
  production_companies?: { id: number; name: string; logo_path?: string }[];
  videos?: { results: TmdbVideo[] };
  recommendations?: TmdbPageResult<TmdbTv>;
  credits?: { cast: TmdbCast[] };
  images?: { logos: TmdbImage[] };
  content_ratings?: { results: { iso_3166_1: string; rating: string }[] };
}

export interface TmdbVideo {
  id: string;
  key: string;
  name: string;
  site: string; // "YouTube"
  type: string; // "Trailer", "Teaser"
  official?: boolean;
  published_at?: string;
}

export interface TmdbCast {
  id: number;
  name: string;
  character?: string;
  profile_path?: string;
  order: number;
}

export interface TmdbImage {
  file_path: string;
  iso_639_1?: string;
  aspect_ratio: number;
  vote_average: number;
}

// ---------------------------------------------------------------------------
// API methods
// ---------------------------------------------------------------------------

async function tmdbGet<T>(path: string, params: Record<string, string | number> = {}): Promise<T> {
  const url = new URL(`${TMDB_BASE}${path}`);
  url.searchParams.set("api_key", TMDB_API_KEY);
  url.searchParams.set("language", "en-US");
  for (const [k, v] of Object.entries(params)) url.searchParams.set(k, String(v));
  const r = await fetch(url.toString(), { next: { revalidate: 3600 } });
  if (!r.ok) throw new Error(`TMDB ${r.status}: ${path}`);
  return (await r.json()) as T;
}

export const tmdb = {
  trending: (window: "day" | "week" = "week", page = 1) =>
    tmdbGet<TmdbPageResult<TmdbMovie | TmdbTv>>(`/trending/all/${window}`, { page }),

  trendingMovies: (window: "day" | "week" = "week", page = 1) =>
    tmdbGet<TmdbPageResult<TmdbMovie>>(`/trending/movie/${window}`, { page }),

  trendingTv: (window: "day" | "week" = "week", page = 1) =>
    tmdbGet<TmdbPageResult<TmdbTv>>(`/trending/tv/${window}`, { page }),

  popularMovies: (page = 1) =>
    tmdbGet<TmdbPageResult<TmdbMovie>>(`/movie/popular`, { page }),

  popularTv: (page = 1) =>
    tmdbGet<TmdbPageResult<TmdbTv>>(`/tv/popular`, { page }),

  topRatedMovies: (page = 1) =>
    tmdbGet<TmdbPageResult<TmdbMovie>>(`/movie/top_rated`, { page }),

  topRatedTv: (page = 1) =>
    tmdbGet<TmdbPageResult<TmdbTv>>(`/tv/top_rated`, { page }),

  upcomingMovies: (page = 1) =>
    tmdbGet<TmdbPageResult<TmdbMovie>>(`/movie/upcoming`, { page }),

  nowPlayingMovies: (page = 1) =>
    tmdbGet<TmdbPageResult<TmdbMovie>>(`/movie/now_playing`, { page }),

  airingTodayTv: (page = 1) =>
    tmdbGet<TmdbPageResult<TmdbTv>>(`/tv/airing_today`, { page }),

  onTheAirTv: (page = 1) =>
    tmdbGet<TmdbPageResult<TmdbTv>>(`/tv/on_the_air`, { page }),

  searchMulti: (query: string, page = 1) =>
    tmdbGet<TmdbPageResult<TmdbMovie | TmdbTv>>(`/search/multi`, { query, page, include_adult: "false" }),

  searchMovies: (query: string, page = 1) =>
    tmdbGet<TmdbPageResult<TmdbMovie>>(`/search/movie`, { query, page, include_adult: "false" }),

  searchTv: (query: string, page = 1) =>
    tmdbGet<TmdbPageResult<TmdbTv>>(`/search/tv`, { query, page, include_adult: "false" }),

  movieDetail: (id: number) =>
    tmdbGet<TmdbMovieDetail>(`/movie/${id}`, {
      append_to_response: "videos,recommendations,credits,images",
      include_image_language: "en,null",
    }),

  tvDetail: (id: number) =>
    tmdbGet<TmdbTvDetail>(`/tv/${id}`, {
      append_to_response: "videos,recommendations,credits,images,content_ratings",
      include_image_language: "en,null",
    }),

  seasonDetail: (tvId: number, seasonNumber: number) =>
    tmdbGet<TmdbSeason>(`/tv/${tvId}/season/${seasonNumber}`),

  genres: (movieOrTv: "movie" | "tv") =>
    tmdbGet<{ genres: TmdbGenre[] }>(`/genre/${movieOrTv}/list`),

  discoverMovies: (params: { with_genres?: string; page?: number; sort_by?: string; year?: number }) =>
    tmdbGet<TmdbPageResult<TmdbMovie>>(`/discover/movie`, {
      page: params.page ?? 1,
      ...(params.with_genres ? { with_genres: params.with_genres } : {}),
      ...(params.sort_by ? { sort_by: params.sort_by } : {}),
      ...(params.year ? { primary_release_year: params.year } : {}),
    }),

  discoverTv: (params: { with_genres?: string; page?: number; sort_by?: string; first_air_date_year?: number }) =>
    tmdbGet<TmdbPageResult<TmdbTv>>(`/discover/tv`, {
      page: params.page ?? 1,
      ...(params.with_genres ? { with_genres: params.with_genres } : {}),
      ...(params.sort_by ? { sort_by: params.sort_by } : {}),
      ...(params.first_air_date_year ? { first_air_date_year: params.first_air_date_year } : {}),
    }),
};
