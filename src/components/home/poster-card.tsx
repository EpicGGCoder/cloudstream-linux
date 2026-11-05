"use client";

import * as React from "react";
import { Star, Info } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { cn } from "@/lib/utils";
import { formatScore } from "@/lib/score";
import type { SearchResponse, TvType } from "@/lib/types/shared";

interface PosterCardProps {
  item: SearchResponse;
  /** Optional width override (default 150px). */
  width?: number;
  /** Click handler — opens the detail modal. */
  onClick?: (item: SearchResponse) => void;
  /** Optional extra className applied to the root. */
  className?: string;
  /** Whether to render the "More info" tooltip on hover. */
  showInfoTooltip?: boolean;
}

const TV_TYPE_LABELS: Partial<Record<TvType, string>> = {
  Movie: "Movie",
  AnimeMovie: "Anime Movie",
  TvSeries: "TV",
  Cartoon: "Cartoon",
  Anime: "Anime",
  OVA: "OVA",
  Documentary: "Doc",
  AsianDrama: "Drama",
  Live: "Live",
  Torrent: "Torrent",
  Others: "Other",
  Music: "Music",
  AudioBook: "Audiobook",
  CustomMedia: "Custom",
  Audio: "Audio",
  Podcast: "Podcast",
  Video: "Video",
};

/** Map a TvType to a short human label. */
export function tvTypeLabel(t?: TvType): string | null {
  if (!t) return null;
  return TV_TYPE_LABELS[t] ?? t;
}

/**
 * Reusable poster card. Used in home rows, search grid, and recommendations.
 *
 * Aspect ratio 2:3, ~150px wide by default. Hover: scale 1.05 + show
 * "More info" tooltip. Clicking opens the detail modal.
 */
export function PosterCard({
  item,
  width = 150,
  onClick,
  className,
  showInfoTooltip = true,
}: PosterCardProps) {
  const scoreStr = formatScore(item.score);
  const typeLabel = tvTypeLabel(item.type);
  const year = "year" in item ? (item as { year?: number }).year : undefined;

  const card = (
    <button
      type="button"
      onClick={() => onClick?.(item)}
      className={cn(
        "group relative flex flex-col gap-2 text-left",
        "transition-transform duration-200 ease-out",
        "hover:scale-105 focus-visible:scale-105",
        "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/60 focus-visible:ring-offset-2 focus-visible:ring-offset-background rounded-md",
        className,
      )}
      style={{ width }}
      aria-label={`${item.name}${year ? ` (${year})` : ""}`}
    >
      {/* Poster image */}
      <div className="relative aspect-[2/3] w-full overflow-hidden rounded-md border border-border/60 bg-muted/40 shadow-sm transition-all duration-200 group-hover:border-primary/60 group-hover:shadow-[0_0_20px_-4px_var(--primary)]">
        {item.posterUrl ? (
          <img
            src={item.posterUrl}
            alt={item.name}
            loading="lazy"
            className="size-full object-cover transition-opacity duration-300"
            referrerPolicy="no-referrer"
            onError={(e) => {
              (e.currentTarget as HTMLImageElement).style.opacity = "0";
            }}
          />
        ) : (
          <div className="flex size-full items-center justify-center p-2 text-center text-[11px] font-medium text-muted-foreground/70">
            {item.name}
          </div>
        )}

        {/* Top-left: type badge */}
        {typeLabel && (
          <div className="absolute left-1.5 top-1.5">
            <Badge
              variant="secondary"
              className="h-5 px-1.5 text-[10px] font-medium uppercase tracking-wide opacity-90 backdrop-blur-sm"
            >
              {typeLabel}
            </Badge>
          </div>
        )}

        {/* Top-right: score */}
        {scoreStr && (
          <div className="absolute right-1.5 top-1.5">
            <Badge
              variant="default"
              className="h-5 gap-0.5 px-1.5 text-[10px] font-semibold opacity-95 backdrop-blur-sm"
            >
              <Star className="size-2.5 fill-current" />
              {scoreStr}
            </Badge>
          </div>
        )}

        {/* Bottom gradient + hover info */}
        <div className="pointer-events-none absolute inset-x-0 bottom-0 h-12 bg-gradient-to-t from-black/70 to-transparent opacity-0 transition-opacity duration-200 group-hover:opacity-100" />
        <div className="pointer-events-none absolute inset-x-0 bottom-0 flex items-center justify-center pb-1.5 opacity-0 transition-opacity duration-200 group-hover:opacity-100">
          <span className="flex items-center gap-1 text-[10px] font-medium text-white/90">
            <Info className="size-3" />
            More info
          </span>
        </div>
      </div>

      {/* Title + year */}
      <div className="flex flex-col gap-0.5 px-0.5">
        <span
          className="line-clamp-1 text-xs font-medium leading-snug tracking-tight text-foreground"
          title={item.name}
        >
          {item.name}
        </span>
        {year != null && (
          <span className="text-[10px] text-muted-foreground">{year}</span>
        )}
      </div>
    </button>
  );

  if (!showInfoTooltip) return card;

  return (
    <Tooltip>
      <TooltipTrigger asChild>{card}</TooltipTrigger>
      <TooltipContent side="top" className="max-w-[220px] text-center">
        {item.name}
        {year != null ? ` (${year})` : ""}
      </TooltipContent>
    </Tooltip>
  );
}

/** A skeleton poster card used as loading placeholder. */
export function PosterCardSkeleton({ width = 150 }: { width?: number }) {
  return (
    <div className="flex flex-col gap-2" style={{ width }} aria-hidden="true">
      <div className="aspect-[2/3] w-full animate-pulse rounded-md bg-muted/60" />
      <div className="h-3 w-3/4 animate-pulse rounded bg-muted/60" />
      <div className="h-2.5 w-1/3 animate-pulse rounded bg-muted/60" />
    </div>
  );
}
