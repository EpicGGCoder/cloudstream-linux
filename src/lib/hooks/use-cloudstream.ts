/**
 * React Query hooks for the CloudStream API.
 *
 * Thin wrappers around `fetch` + `useQuery` / `useMutation` from
 * `@tanstack/react-query`. All endpoints return JSON.
 *
 * Endpoints consumed:
 *   GET  /api/home
 *   GET  /api/search?q=<query>&page=<n>
 *   GET  /api/detail?apiName=<n>&url=<u>
 *   POST /api/links          body { apiName, data }
 *   GET  /api/providers
 */

"use client";

import { useMutation, useQuery } from "@tanstack/react-query";
import type {
  ExtractorLink,
  LoadResponse,
  ProviderInfo,
  SearchResponse,
  SubtitleFile,
} from "@/lib/types/shared";

// ---------------------------------------------------------------------------
// Response shapes (mirror the API routes)
// ---------------------------------------------------------------------------

export interface HomeRow {
  id: string;
  name: string;
  provider: string;
  items: SearchResponse[];
}

export interface HomeResponse {
  rows: HomeRow[];
}

export interface ProviderSearchResult {
  apiName: string;
  items: SearchResponse[];
  hasNext: boolean;
  error?: string;
}

export interface SearchResponsePayload {
  query: string;
  page: number;
  results: ProviderSearchResult[];
}

export interface DetailResponse {
  apiName: string;
  url: string;
  loadResponse: LoadResponse | null;
  error?: string;
}

export interface LinksResponse {
  apiName: string;
  data: string;
  links: ExtractorLink[];
  subtitles: SubtitleFile[];
  cached: boolean;
  error?: string;
}

export interface ProvidersResponse {
  providers: ProviderInfo[];
}

// ---------------------------------------------------------------------------
// Fetch helpers
// ---------------------------------------------------------------------------

async function fetchJson<T>(url: string): Promise<T> {
  const r = await fetch(url, { headers: { Accept: "application/json" } });
  if (!r.ok) {
    const text = await r.text().catch(() => r.statusText);
    throw new Error(`${r.status} ${r.statusText}: ${text}`);
  }
  return (await r.json()) as T;
}

async function postJson<T>(url: string, body: unknown): Promise<T> {
  const r = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "application/json" },
    body: JSON.stringify(body),
  });
  if (!r.ok) {
    const text = await r.text().catch(() => r.statusText);
    throw new Error(`${r.status} ${r.statusText}: ${text}`);
  }
  return (await r.json()) as T;
}

// ---------------------------------------------------------------------------
// Query hooks
// ---------------------------------------------------------------------------

/** GET /api/home — aggregated home feed across all enabled providers. */
export function useHomeFeed() {
  return useQuery<HomeResponse>({
    queryKey: ["home"],
    queryFn: () => fetchJson<HomeResponse>("/api/home"),
  });
}

/** GET /api/search?q=<query>&page=<n> — cross-provider search. */
export function useSearch(query: string, page = 1, enabled = true) {
  return useQuery<SearchResponsePayload>({
    queryKey: ["search", query, page],
    queryFn: () =>
      fetchJson<SearchResponsePayload>(
        `/api/search?q=${encodeURIComponent(query)}&page=${page}`,
      ),
    enabled: enabled && query.trim().length > 0,
  });
}

/** GET /api/detail?apiName=<n>&url=<u> — load a title's detail page. */
export function useDetail(
  apiName: string | null,
  url: string | null,
  enabled = true,
) {
  return useQuery<DetailResponse>({
    queryKey: ["detail", apiName, url],
    queryFn: () =>
      fetchJson<DetailResponse>(
        `/api/detail?apiName=${encodeURIComponent(apiName ?? "")}&url=${encodeURIComponent(url ?? "")}`,
      ),
    enabled: enabled && !!apiName && !!url,
  });
}

/** GET /api/providers — list of all registered providers. */
export function useProviders() {
  return useQuery<ProvidersResponse>({
    queryKey: ["providers"],
    queryFn: () => fetchJson<ProvidersResponse>("/api/providers"),
  });
}

// ---------------------------------------------------------------------------
// Mutations
// ---------------------------------------------------------------------------

/** POST /api/links — resolve playable links + subtitles for a movie/episode. */
export function useLoadLinks() {
  return useMutation<
    LinksResponse,
    Error,
    { apiName: string; data: string }
  >({
    mutationFn: ({ apiName, data }) =>
      postJson<LinksResponse>("/api/links", { apiName, data }),
  });
}
