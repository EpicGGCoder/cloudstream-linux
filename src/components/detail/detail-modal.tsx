"use client";

import * as React from "react";
import {
  Dialog,
  DialogContent,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Avatar, AvatarImage, AvatarFallback } from "@/components/ui/avatar";
import { Play, Star, Clock, AlertCircle, Film, Tv } from "lucide-react";
import { PosterCard } from "@/components/home/poster-card";
import { EpisodeList, EpisodeListSkeleton } from "./episode-list";
import { tvTypeLabel } from "@/components/home/poster-card";
import { useDetail } from "@/lib/hooks/use-cloudstream";
import { formatScore } from "@/lib/score";
import type {
  Episode,
  LoadResponse,
  MovieLoadResponse,
  SearchResponse,
  TvSeriesLoadResponse,
} from "@/lib/types/shared";
import { cn } from "@/lib/utils";

interface DetailModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  apiName: string | null;
  url: string | null;
  onCardClick?: (item: SearchResponse) => void;
  onPlayMovie?: (loadResponse: MovieLoadResponse) => void;
  onPlayEpisode?: (
    loadResponse: TvSeriesLoadResponse,
    episode: Episode,
  ) => void;
}

function isMovieLoad(r: LoadResponse): r is MovieLoadResponse {
  return "dataUrl" in r && typeof (r as MovieLoadResponse).dataUrl === "string";
}

function isTvSeriesLoad(r: LoadResponse): r is TvSeriesLoadResponse {
  return "episodes" in r && Array.isArray((r as TvSeriesLoadResponse).episodes);
}

function formatDuration(seconds?: number): string | null {
  if (!seconds || !Number.isFinite(seconds) || seconds <= 0) return null;
  const m = Math.round(seconds / 60);
  if (m < 60) return `${m}m`;
  const h = Math.floor(m / 60);
  const rem = m % 60;
  return rem === 0 ? `${h}h` : `${h}h ${rem}m`;
}

/**
 * Detail modal — near-fullscreen Dialog (max-w-5xl, h-[90vh]).
 *
 * Top: backdrop image + gradient + title + badges + Play / Continue watching.
 * Below: plot, actors strip, recommendations row.
 * For TV series: episode list grouped by season.
 */
export function DetailModal({
  open,
  onOpenChange,
  apiName,
  url,
  onCardClick,
  onPlayMovie,
  onPlayEpisode,
}: DetailModalProps) {
  const { data, isLoading, isError, error } = useDetail(apiName, url, open);

  const load = data?.loadResponse ?? null;

  // Reset scroll on open.
  const scrollRef = React.useRef<HTMLDivElement>(null);
  React.useEffect(() => {
    if (open && scrollRef.current) scrollRef.current.scrollTop = 0;
  }, [open, apiName, url]);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent
        showCloseButton
        className={cn(
          "max-w-5xl gap-0 overflow-hidden p-0",
          "h-[90vh] w-[calc(100%-1rem)] sm:w-[calc(100%-2rem)]",
          "bg-background",
        )}
      >
        <DialogTitle className="sr-only">
          {load?.name ?? "Title details"}
        </DialogTitle>
        <DialogDescription className="sr-only">
          Detailed information about this title.
        </DialogDescription>

        <div
          ref={scrollRef}
          className="scrollbar-hide h-full overflow-y-auto"
        >
          {isLoading && <DetailSkeleton />}

          {!isLoading && isError && (
            <div className="flex h-full flex-col items-center justify-center gap-3 p-8 text-center">
              <AlertCircle className="size-10 text-destructive" />
              <h3 className="text-lg font-semibold tracking-tight">
                Failed to load details
              </h3>
              <p className="max-w-md text-sm text-muted-foreground">
                {String(error?.message ?? "").slice(0, 200)}
              </p>
            </div>
          )}

          {!isLoading && !isError && !load && (
            <div className="flex h-full flex-col items-center justify-center gap-3 p-8 text-center">
              <Film className="size-10 text-muted-foreground" />
              <h3 className="text-lg font-semibold tracking-tight">
                No details available
              </h3>
              <p className="max-w-md text-sm text-muted-foreground">
                The provider returned no data for this title.
              </p>
            </div>
          )}

          {!isLoading && !isError && load && (
            <DetailBody
              load={load}
              onCardClick={onCardClick}
              onPlayMovie={onPlayMovie}
              onPlayEpisode={onPlayEpisode}
            />
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}

// ---------------------------------------------------------------------------
// Detail body (loaded)
// ---------------------------------------------------------------------------

interface DetailBodyProps {
  load: LoadResponse;
  onCardClick?: (item: SearchResponse) => void;
  onPlayMovie?: (load: MovieLoadResponse) => void;
  onPlayEpisode?: (load: TvSeriesLoadResponse, episode: Episode) => void;
}

function DetailBody({
  load,
  onCardClick,
  onPlayMovie,
  onPlayEpisode,
}: DetailBodyProps) {
  const scoreStr = formatScore(load.score);
  const typeLabel = tvTypeLabel(load.type);
  const durationStr = formatDuration(load.duration);
  const backdrop = load.backgroundPosterUrl ?? load.posterUrl;
  const isMovie = isMovieLoad(load);
  const isSeries = isTvSeriesLoad(load);

  const onPrimaryClick = () => {
    if (isMovie) {
      onPlayMovie?.(load as MovieLoadResponse);
    }
    // For series, the episode list handles play.
  };

  return (
    <div className="flex flex-col">
      {/* Hero section */}
      <div className="relative h-[36vh] min-h-[260px] w-full overflow-hidden sm:h-[44vh] sm:min-h-[320px]">
        {backdrop ? (
          <img
            src={backdrop}
            alt=""
            className="size-full object-cover"
            referrerPolicy="no-referrer"
          />
        ) : (
          <div className="size-full bg-gradient-to-br from-primary/30 via-background to-background" />
        )}
        <div className="absolute inset-0 bg-gradient-to-t from-background via-background/60 to-transparent" />
        <div className="absolute inset-0 bg-gradient-to-r from-background/60 via-transparent to-transparent" />

        {/* Logo or title */}
        <div className="absolute inset-x-0 bottom-0 flex flex-col gap-3 p-5 sm:p-8">
          {load.logoUrl && (
            <div className="mb-1 max-h-20 max-w-[60%]">
              <img
                src={load.logoUrl}
                alt={load.name}
                className="h-auto max-h-20 w-auto object-contain drop-shadow-[0_2px_8px_rgba(0,0,0,0.6)]"
                referrerPolicy="no-referrer"
              />
            </div>
          )}
          {!load.logoUrl && (
            <h2 className="max-w-2xl text-2xl font-bold leading-tight tracking-tight drop-shadow-[0_2px_8px_rgba(0,0,0,0.6)] sm:text-3xl lg:text-4xl">
              {load.name}
            </h2>
          )}

          {/* Badges */}
          <div className="flex flex-wrap items-center gap-2">
            {typeLabel && (
              <Badge variant="default" className="text-[11px] uppercase tracking-wide">
                {typeLabel}
              </Badge>
            )}
            {load.year != null && (
              <Badge variant="secondary" className="text-[11px]">
                {load.year}
              </Badge>
            )}
            {scoreStr && (
              <Badge
                variant="outline"
                className="gap-1 border-white/30 bg-black/40 text-white text-[11px] backdrop-blur-sm"
              >
                <Star className="size-3 fill-current text-primary" />
                <span className="font-semibold">{scoreStr}</span>
              </Badge>
            )}
            {durationStr && (
              <Badge
                variant="outline"
                className="gap-1 border-white/30 bg-black/40 text-white text-[11px] backdrop-blur-sm"
              >
                <Clock className="size-3" />
                {durationStr}
              </Badge>
            )}
            {isSeries &&
              (load as TvSeriesLoadResponse).showStatus && (
                <Badge variant="secondary" className="text-[11px]">
                  {(load as TvSeriesLoadResponse).showStatus}
                </Badge>
              )}
          </div>

          {/* Tags */}
          {load.tags && load.tags.length > 0 && (
            <div className="flex flex-wrap gap-1.5">
              {load.tags.slice(0, 6).map((t) => (
                <span
                  key={t}
                  className="rounded-full border border-white/15 bg-black/30 px-2 py-0.5 text-[10px] text-white/80 backdrop-blur-sm"
                >
                  {t}
                </span>
              ))}
            </div>
          )}

          {/* Primary action */}
          {isMovie && (
            <div className="mt-1">
              <Button
                size="lg"
                onClick={onPrimaryClick}
                className="h-11 gap-2 rounded-full px-6 shadow-[0_0_24px_-6px_var(--primary)]"
              >
                <Play className="size-4 fill-current" />
                Play
              </Button>
            </div>
          )}
          {isSeries && (
            <div className="mt-1">
              <Button
                size="lg"
                variant="secondary"
                onClick={() => {
                  // Scroll to episodes section.
                  const el = document.getElementById(
                    "detail-episodes-section",
                  );
                  el?.scrollIntoView({ behavior: "smooth", block: "start" });
                }}
                className="h-11 gap-2 rounded-full px-6"
              >
                <Tv className="size-4" />
                Continue watching
              </Button>
            </div>
          )}
        </div>
      </div>

      {/* Body */}
      <div className="flex flex-col gap-8 p-5 sm:p-8">
        {/* Plot */}
        {load.plot && (
          <section className="flex flex-col gap-2">
            <h3 className="text-sm font-semibold tracking-tight text-muted-foreground">
              Overview
            </h3>
            <p className="max-w-3xl text-sm leading-relaxed text-foreground/90">
              {load.plot}
            </p>
          </section>
        )}

        {/* Actors */}
        {load.actors && load.actors.length > 0 && (
          <section className="flex flex-col gap-3">
            <h3 className="text-sm font-semibold tracking-tight text-muted-foreground">
              Cast
            </h3>
            <div className="scrollbar-hide flex gap-4 overflow-x-auto pb-1">
              {load.actors.slice(0, 20).map((a, i) => (
                <div
                  key={`${a.actor.name}-${i}`}
                  className="flex w-20 shrink-0 flex-col items-center gap-1.5 text-center"
                >
                  <Avatar className="size-16 border border-border/60">
                    {a.actor.image ? (
                      <AvatarImage
                        src={a.actor.image}
                        alt={a.actor.name}
                        referrerPolicy="no-referrer"
                      />
                    ) : null}
                    <AvatarFallback>
                      {a.actor.name
                        ?.split(" ")
                        .slice(0, 2)
                        .map((s) => s[0])
                        .join("")
                        .toUpperCase()}
                    </AvatarFallback>
                  </Avatar>
                  <div className="flex flex-col gap-0.5">
                    <span
                      className="line-clamp-2 text-[11px] font-medium leading-tight"
                      title={a.actor.name}
                    >
                      {a.actor.name}
                    </span>
                    {a.roleString && (
                      <span className="line-clamp-1 text-[10px] text-muted-foreground">
                        {a.roleString}
                      </span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </section>
        )}

        {/* Episodes (TV series) */}
        {isSeries && (
          <section
            id="detail-episodes-section"
            className="flex flex-col gap-3 scroll-mt-4"
          >
            <h3 className="text-sm font-semibold tracking-tight text-muted-foreground">
              Episodes
            </h3>
            <EpisodeList
              episodes={(load as TvSeriesLoadResponse).episodes}
              onPlay={(ep) =>
                onPlayEpisode?.(load as TvSeriesLoadResponse, ep)
              }
            />
          </section>
        )}

        {/* Recommendations */}
        {load.recommendations && load.recommendations.length > 0 && (
          <section className="flex flex-col gap-3">
            <h3 className="text-sm font-semibold tracking-tight text-muted-foreground">
              More like this
            </h3>
            <div className="scrollbar-hide -mx-1 flex gap-3 overflow-x-auto px-1 pb-2">
              {load.recommendations.slice(0, 12).map((item, i) => (
                <PosterCard
                  key={`${item.url}-${i}`}
                  item={item}
                  onClick={onCardClick}
                  width={130}
                />
              ))}
            </div>
          </section>
        )}
      </div>
    </div>
  );
}

// ---------------------------------------------------------------------------
// Skeleton
// ---------------------------------------------------------------------------

function DetailSkeleton() {
  return (
    <div className="flex flex-col">
      <div className="relative h-[36vh] min-h-[260px] w-full sm:h-[44vh] sm:min-h-[320px]">
        <Skeleton className="absolute inset-0 rounded-none" />
        <div className="absolute inset-x-0 bottom-0 flex flex-col gap-3 p-5 sm:p-8">
          <Skeleton className="h-10 w-2/3 max-w-2xl" />
          <div className="flex gap-2">
            <Skeleton className="h-5 w-16" />
            <Skeleton className="h-5 w-12" />
            <Skeleton className="h-5 w-14" />
          </div>
          <Skeleton className="h-11 w-28 rounded-full" />
        </div>
      </div>
      <div className="flex flex-col gap-8 p-5 sm:p-8">
        <div className="flex flex-col gap-2">
          <Skeleton className="h-4 w-20" />
          <Skeleton className="h-4 w-full" />
          <Skeleton className="h-4 w-5/6" />
          <Skeleton className="h-4 w-3/4" />
        </div>
        <div className="flex flex-col gap-3">
          <Skeleton className="h-4 w-12" />
          <div className="flex gap-4">
            {Array.from({ length: 6 }).map((_, i) => (
              <Skeleton key={i} className="size-16 rounded-full" />
            ))}
          </div>
        </div>
        <EpisodeListSkeleton />
      </div>
    </div>
  );
}
