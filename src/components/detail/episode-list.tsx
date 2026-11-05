"use client";

import * as React from "react";
import { Play, Calendar } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { formatScore } from "@/lib/score";
import { cn } from "@/lib/utils";
import type { Episode } from "@/lib/types/shared";

interface EpisodeListProps {
  episodes: Episode[];
  onPlay?: (episode: Episode) => void;
}

interface SeasonGroup {
  season: number;
  name?: string;
  episodes: Episode[];
}

/** Group episodes by their `season` field. Treats undefined season as 1. */
export function groupEpisodesBySeason(episodes: Episode[]): SeasonGroup[] {
  const map = new Map<number, SeasonGroup>();
  const order: number[] = [];
  for (const ep of episodes) {
    const s = ep.season ?? 1;
    if (!map.has(s)) {
      map.set(s, { season: s, episodes: [] });
      order.push(s);
    }
    map.get(s)!.episodes.push(ep);
  }
  return order
    .slice()
    .sort((a, b) => a - b)
    .map((s) => {
      const g = map.get(s)!;
      g.episodes.sort((a, b) => (a.episode ?? 0) - (b.episode ?? 0));
      return g;
    });
}

function formatDate(unixMs?: number): string | null {
  if (!unixMs || !Number.isFinite(unixMs)) return null;
  const d = new Date(unixMs);
  if (Number.isNaN(d.getTime())) return null;
  return d.toLocaleDateString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}

function formatRuntime(seconds?: number): string | null {
  if (!seconds || !Number.isFinite(seconds) || seconds <= 0) return null;
  const m = Math.round(seconds / 60);
  if (m < 60) return `${m}m`;
  const h = Math.floor(m / 60);
  const rem = m % 60;
  return rem === 0 ? `${h}h` : `${h}h ${rem}m`;
}

/**
 * Episode list grouped by season.
 *
 * Each season is collapsible (default open for the first season). Each
 * episode row shows: episode number, name, description (truncated), air
 * date. Clicking opens the player.
 */
export function EpisodeList({ episodes, onPlay }: EpisodeListProps) {
  const groups = React.useMemo(() => groupEpisodesBySeason(episodes), [episodes]);
  const [openSeasons, setOpenSeasons] = React.useState<Set<number>>(
    () => new Set(groups.length > 0 ? [groups[0]!.season] : []),
  );

  // Keep open set in sync when groups change.
  React.useEffect(() => {
    if (groups.length === 0) return;
    setOpenSeasons((prev) => {
      const next = new Set(prev);
      if (next.size === 0) next.add(groups[0]!.season);
      // Drop seasons that no longer exist.
      const validSeasons = new Set(groups.map((g) => g.season));
      for (const s of next) if (!validSeasons.has(s)) next.delete(s);
      return next;
    });
  }, [groups]);

  const toggleSeason = (s: number) => {
    setOpenSeasons((prev) => {
      const next = new Set(prev);
      if (next.has(s)) next.delete(s);
      else next.add(s);
      return next;
    });
  };

  if (groups.length === 0) {
    return (
      <p className="text-sm text-muted-foreground">
        No episodes available for this title.
      </p>
    );
  }

  return (
    <div className="flex flex-col gap-4">
      {groups.map((group) => {
        const isOpen = openSeasons.has(group.season);
        const seasonTitle =
          group.name && group.name.trim()
            ? group.name
            : `Season ${group.season}`;
        return (
          <div
            key={`season-${group.season}`}
            className="overflow-hidden rounded-lg border border-border/50 bg-card/40"
          >
            <button
              type="button"
              onClick={() => toggleSeason(group.season)}
              aria-expanded={isOpen}
              className="flex w-full items-center justify-between gap-3 px-4 py-3 text-left transition-colors hover:bg-accent/50"
            >
              <div className="flex items-center gap-3">
                <span className="text-base font-semibold tracking-tight">
                  {seasonTitle}
                </span>
                <Badge variant="secondary" className="text-[10px]">
                  {group.episodes.length}{" "}
                  {group.episodes.length === 1 ? "episode" : "episodes"}
                </Badge>
              </div>
              <span
                className={cn(
                  "text-muted-foreground transition-transform duration-200",
                  isOpen ? "rotate-180" : "rotate-0",
                )}
                aria-hidden="true"
              >
                ▾
              </span>
            </button>

            {isOpen && (
              <ul className="divide-y divide-border/40">
                {group.episodes.map((ep, i) => {
                  const scoreStr = formatScore(ep.score);
                  const dateStr = formatDate(ep.date);
                  const runtimeStr = formatRuntime(ep.runTime);
                  const epNum = ep.episode ?? i + 1;
                  const epTitle = ep.name || `Episode ${epNum}`;
                  return (
                    <li key={`ep-${group.season}-${epNum}-${i}`}>
                      <button
                        type="button"
                        onClick={() => onPlay?.(ep)}
                        className={cn(
                          "group flex w-full items-start gap-3 px-4 py-3 text-left transition-colors hover:bg-accent/40",
                          "focus-visible:bg-accent/60 focus-visible:outline-none",
                        )}
                      >
                        {/* Episode thumbnail (optional) */}
                        <div className="relative size-16 shrink-0 overflow-hidden rounded-md border border-border/50 bg-muted/40">
                          {ep.posterUrl ? (
                            <img
                              src={ep.posterUrl}
                              alt=""
                              loading="lazy"
                              className="size-full object-cover transition-opacity duration-300"
                              referrerPolicy="no-referrer"
                              onError={(e) => {
                                (e.currentTarget as HTMLImageElement).style.opacity = "0";
                              }}
                            />
                          ) : (
                            <div className="flex size-full items-center justify-center">
                              <span className="text-xs font-semibold text-muted-foreground/70">
                                {epNum}
                              </span>
                            </div>
                          )}
                          <div className="absolute inset-0 flex items-center justify-center bg-black/40 opacity-0 transition-opacity duration-200 group-hover:opacity-100">
                            <Play className="size-5 fill-current text-white" />
                          </div>
                        </div>

                        {/* Episode text */}
                        <div className="flex min-w-0 flex-1 flex-col gap-1">
                          <div className="flex items-baseline gap-2">
                            <span className="text-xs font-semibold text-muted-foreground">
                              E{String(epNum).padStart(2, "0")}
                            </span>
                            <span className="line-clamp-1 text-sm font-medium tracking-tight">
                              {epTitle}
                            </span>
                            {scoreStr && (
                              <Badge
                                variant="outline"
                                className="ml-auto shrink-0 text-[10px]"
                              >
                                {scoreStr}
                              </Badge>
                            )}
                          </div>
                          {ep.description && (
                            <p className="line-clamp-2 text-xs leading-relaxed text-muted-foreground">
                              {ep.description}
                            </p>
                          )}
                          <div className="flex flex-wrap items-center gap-2 text-[10px] text-muted-foreground/80">
                            {dateStr && (
                              <span className="inline-flex items-center gap-1">
                                <Calendar className="size-3" />
                                {dateStr}
                              </span>
                            )}
                            {runtimeStr && <span>{runtimeStr}</span>}
                          </div>
                        </div>
                      </button>
                    </li>
                  );
                })}
              </ul>
            )}
          </div>
        );
      })}
    </div>
  );
}

/** Skeleton for the episode list. */
export function EpisodeListSkeleton() {
  return (
    <div className="flex flex-col gap-4">
      {Array.from({ length: 2 }).map((_, i) => (
        <div
          key={i}
          className="overflow-hidden rounded-lg border border-border/50 bg-card/40"
        >
          <div className="flex items-center justify-between px-4 py-3">
            <Skeleton className="h-5 w-32" />
            <Skeleton className="h-4 w-4 rounded-full" />
          </div>
          <div className="flex flex-col divide-y divide-border/40">
            {Array.from({ length: 4 }).map((_, j) => (
              <div key={j} className="flex items-start gap-3 px-4 py-3">
                <Skeleton className="size-16 shrink-0 rounded-md" />
                <div className="flex flex-1 flex-col gap-1">
                  <Skeleton className="h-4 w-3/4" />
                  <Skeleton className="h-3 w-full" />
                  <Skeleton className="h-3 w-2/3" />
                </div>
              </div>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}
