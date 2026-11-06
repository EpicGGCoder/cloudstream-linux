"use client";

import * as React from "react";
import { Search as SearchIcon, History, X, AlertCircle } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { PosterCard, PosterCardSkeleton } from "@/components/home/poster-card";
import { useSearch } from "@/lib/hooks/use-cloudstream";
import { cn } from "@/lib/utils";
import type { SearchResponse } from "@/lib/types/shared";

interface SearchViewProps {
  /** Controlled query (synced with the global top-bar search input). */
  query: string;
  onQueryChange: (q: string) => void;
  onCardClick?: (item: SearchResponse) => void;
}

const HISTORY_KEY = "cs:search-history";
const HISTORY_MAX = 10;

function loadHistory(): string[] {
  if (typeof window === "undefined") return [];
  try {
    const raw = window.localStorage.getItem(HISTORY_KEY);
    if (!raw) return [];
    const arr = JSON.parse(raw);
    if (!Array.isArray(arr)) return [];
    return arr.filter((s) => typeof s === "string" && s.trim()).slice(0, HISTORY_MAX);
  } catch {
    return [];
  }
}

function saveHistory(items: string[]): void {
  if (typeof window === "undefined") return;
  try {
    window.localStorage.setItem(HISTORY_KEY, JSON.stringify(items.slice(0, HISTORY_MAX)));
  } catch {
    // ignore
  }
}

/**
 * Search tab — large search input (sticky within the tab) + cross-provider
 * results grid grouped by provider. Shows search-history chips when the
 * input is empty.
 */
export function SearchView({ query, onQueryChange, onCardClick }: SearchViewProps) {
  const [debounced, setDebounced] = React.useState(query);
  const [history, setHistory] = React.useState<string[]>([]);
  const [historyVersion, setHistoryVersion] = React.useState(0);

  // Load history once on mount.
  React.useEffect(() => {
    setHistory(loadHistory());
  }, [historyVersion]);

  // Debounce 400ms.
  React.useEffect(() => {
    const t = setTimeout(() => setDebounced(query), 400);
    return () => clearTimeout(t);
  }, [query]);

  const trimmed = debounced.trim();
  const { data, isLoading, isError, error, isFetching } = useSearch(trimmed, 1, trimmed.length > 0);

  const inputRef = React.useRef<HTMLInputElement>(null);

  // Push a query into history when a fresh search completes.
  const committedRef = React.useRef<string>("");
  React.useEffect(() => {
    if (!trimmed) {
      committedRef.current = "";
      return;
    }
    if (committedRef.current === trimmed) return;
    if (!data) return;
    // Only commit once the query actually changed AND we have results.
    committedRef.current = trimmed;
    setHistory((prev) => {
      const next = [trimmed, ...prev.filter((s) => s !== trimmed)].slice(0, HISTORY_MAX);
      saveHistory(next);
      return next;
    });
  }, [trimmed, data]);

  const onHistoryChipClick = (q: string) => {
    onQueryChange(q);
    inputRef.current?.focus();
  };

  const onHistoryClear = () => {
    saveHistory([]);
    setHistory([]);
  };

  const onHistoryRemove = (q: string, e: React.MouseEvent) => {
    e.stopPropagation();
    setHistory((prev) => {
      const next = prev.filter((s) => s !== q);
      saveHistory(next);
      return next;
    });
  };

  const results = data?.results ?? [];

  return (
    <div className="flex flex-1 flex-col gap-4 pb-6">
      {/* Sticky search header */}
      <div className="sticky top-14 z-20 -mx-3 border-b border-border/40 bg-background/85 px-3 py-3 backdrop-blur-md supports-[backdrop-filter]:bg-background/65 sm:top-14 sm:-mx-6 sm:px-6">
        <div className="relative">
          <SearchIcon
            className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
            aria-hidden="true"
          />
          <Input
            ref={inputRef}
            type="search"
            value={query}
            onChange={(e) => onQueryChange(e.target.value)}
            placeholder="Search movies, TV shows, anime..."
            aria-label="Search query"
            className="h-11 rounded-full border-border/60 bg-input/40 pl-9 pr-9"
            autoFocus
          />
          {query && (
            <button
              type="button"
              onClick={() => onQueryChange("")}
              aria-label="Clear search"
              className="absolute right-2 top-1/2 flex size-7 -translate-y-1/2 items-center justify-center rounded-full text-muted-foreground transition-colors hover:bg-accent hover:text-foreground"
            >
              <X className="size-4" />
            </button>
          )}
        </div>
      </div>

      {/* Body */}
      <div className="px-3 sm:px-6">
        {/* Empty query → show history chips */}
        {!trimmed && (
          <div className="flex flex-col gap-3 pt-4">
            <div className="flex items-center justify-between">
              <h3 className="flex items-center gap-2 text-sm font-semibold tracking-tight text-muted-foreground">
                <History className="size-4" />
                Recent searches
              </h3>
              {history.length > 0 && (
                <Button
                  variant="ghost"
                  size="sm"
                  className="h-7 text-xs"
                  onClick={onHistoryClear}
                >
                  Clear all
                </Button>
              )}
            </div>
            {history.length === 0 ? (
              <p className="text-sm text-muted-foreground">
                Search for movies, TV shows, anime...
              </p>
            ) : (
              <div className="flex flex-wrap gap-2">
                {history.map((q) => (
                  <button
                    key={q}
                    type="button"
                    onClick={() => onHistoryChipClick(q)}
                    className="group inline-flex items-center gap-1.5 rounded-full border border-border/60 bg-card/60 px-3 py-1 text-xs font-medium transition-colors hover:border-primary/60 hover:bg-accent"
                  >
                    <span className="max-w-[16ch] truncate">{q}</span>
                    <span
                      role="button"
                      tabIndex={0}
                      onClick={(e) => onHistoryRemove(q, e)}
                      onKeyDown={(e) => {
                        if (e.key === "Enter" || e.key === " ") {
                          e.preventDefault();
                          onHistoryRemove(q, e as unknown as React.MouseEvent);
                        }
                      }}
                      className="flex size-4 items-center justify-center rounded-full text-muted-foreground transition-colors hover:bg-foreground/10 hover:text-foreground"
                      aria-label={`Remove "${q}" from history`}
                    >
                      <X className="size-3" />
                    </span>
                  </button>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Loading state */}
        {trimmed && isLoading && (
          <div className="flex flex-col gap-6 pt-4">
            {Array.from({ length: 2 }).map((_, i) => (
              <section key={i} className="flex flex-col gap-3">
                <div className="h-5 w-40 animate-pulse rounded bg-muted/60" />
                <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 md:grid-cols-6 lg:grid-cols-6 xl:grid-cols-8">
                  {Array.from({ length: 8 }).map((_, j) => (
                    <PosterCardSkeleton key={j} />
                  ))}
                </div>
              </section>
            ))}
          </div>
        )}

        {/* Error state */}
        {trimmed && isError && !isLoading && (
          <div className="flex flex-col items-center justify-center gap-3 py-16 text-center">
            <AlertCircle className="size-10 text-destructive" />
            <h3 className="text-lg font-semibold tracking-tight">Search failed</h3>
            <p className="max-w-md text-sm text-muted-foreground">
              {String(error?.message ?? "").slice(0, 200)}
            </p>
          </div>
        )}

        {/* Results */}
        {trimmed && !isLoading && !isError && results.length === 0 && (
          <div className="flex flex-col items-center justify-center gap-3 py-16 text-center">
            <SearchIcon className="size-10 text-muted-foreground" />
            <h3 className="text-lg font-semibold tracking-tight">No results</h3>
            <p className="max-w-md text-sm text-muted-foreground">
              No providers returned results for "{trimmed}". Try a different
              query.
            </p>
          </div>
        )}

        {/* Group by provider */}
        {trimmed && !isError && results.length > 0 && (
          <div className="flex flex-col gap-8 pt-4">
            {results.map((res) => {
              const hasItems = res.items.length > 0;
              return (
                <section key={res.apiName} className="flex flex-col gap-3">
                  <div className="flex items-center justify-between gap-2">
                    <h3 className="flex items-center gap-2 text-base font-semibold tracking-tight">
                      <span className="truncate">{res.apiName}</span>
                      {res.error && (
                        <Badge variant="destructive" className="text-[10px]">
                          error
                        </Badge>
                      )}
                      <Badge variant="secondary" className="text-[10px]">
                        {res.items.length}
                      </Badge>
                    </h3>
                    {res.hasNext && (
                      <span className="text-[11px] text-muted-foreground">
                        more available
                      </span>
                    )}
                  </div>

                  {!hasItems ? (
                    <p className="text-xs text-muted-foreground">
                      No results from this provider.
                    </p>
                  ) : (
                    <div
                      className={cn(
                        "grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 xl:grid-cols-8",
                      )}
                    >
                      {res.items.map((item, i) => (
                        <PosterCard
                          key={`${item.url}-${i}`}
                          item={item}
                          onClick={onCardClick}
                        />
                      ))}
                    </div>
                  )}
                </section>
              );
            })}
            {isFetching && (
              <p className="text-center text-xs text-muted-foreground">
                Loading more...
              </p>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
