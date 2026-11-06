"use client";

import * as React from "react";
import { AlertCircle, Film } from "lucide-react";
import { HeroCarousel, HeroCarouselSkeleton } from "./hero-carousel";
import { PosterRow } from "./poster-row";
import { useHomeFeed } from "@/lib/hooks/use-cloudstream";
import type { SearchResponse } from "@/lib/types/shared";

interface HomeViewProps {
  onCardClick?: (item: SearchResponse) => void;
  onPlayFromHero?: (item: SearchResponse) => void;
}

/**
 * Home tab — hero carousel (first row's first 5 items) + horizontal
 * poster rows for each home row returned by `/api/home`.
 */
export function HomeView({ onCardClick, onPlayFromHero }: HomeViewProps) {
  const { data, isLoading, isError, error } = useHomeFeed();

  if (isError) {
    return (
      <div className="flex flex-1 flex-col items-center justify-center gap-3 px-6 py-16 text-center">
        <AlertCircle className="size-10 text-destructive" />
        <h3 className="text-lg font-semibold tracking-tight">
          Could not load the home feed
        </h3>
        <p className="max-w-md text-sm text-muted-foreground">
          The backend returned an error. {String(error?.message ?? "").slice(0, 200)}
        </p>
      </div>
    );
  }

  const rows = data?.rows ?? [];

  // Empty state.
  if (!isLoading && rows.length === 0) {
    return (
      <div className="flex flex-1 flex-col items-center justify-center gap-3 px-6 py-16 text-center">
        <Film className="size-10 text-muted-foreground" />
        <h3 className="text-lg font-semibold tracking-tight">No home rows yet</h3>
        <p className="max-w-md text-sm text-muted-foreground">
          No providers with a main page are currently enabled. Visit Settings
          to enable a provider.
        </p>
      </div>
    );
  }

  // Hero: take the first row's first 5 items.
  const heroItems = rows[0]?.items?.slice(0, 5) ?? [];

  return (
    <div className="flex flex-1 flex-col gap-6 pb-6">
      {/* Hero */}
      {isLoading ? (
        <HeroCarouselSkeleton />
      ) : (
        <HeroCarousel
          items={heroItems}
          onPlay={onPlayFromHero}
          onMoreInfo={onCardClick}
        />
      )}

      {/* Rows */}
      <div className="flex flex-col gap-4">
        {isLoading
          ? Array.from({ length: 3 }).map((_, i) => (
              <PosterRow
                key={`skeleton-${i}`}
                title={
                  ["Loading...", "Loading...", "Loading..."][i] ?? "Loading..."
                }
                items={[]}
                loading
              />
            ))
          : rows.map((row) => (
              <PosterRow
                key={row.id}
                title={row.name}
                subtitle={row.provider}
                items={row.items}
                onCardClick={onCardClick}
              />
            ))}
      </div>
    </div>
  );
}
