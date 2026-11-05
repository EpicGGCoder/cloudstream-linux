"use client";

import * as React from "react";
import { AnimatePresence, motion } from "framer-motion";
import { Play, Info, Star } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { formatScore } from "@/lib/score";
import { tvTypeLabel } from "./poster-card";
import type { SearchResponse } from "@/lib/types/shared";

interface HeroCarouselProps {
  items: SearchResponse[];
  onPlay?: (item: SearchResponse) => void;
  onMoreInfo?: (item: SearchResponse) => void;
}

const AUTO_ROTATE_MS = 8000;

/**
 * Hero carousel — full-width, ~50vh tall, with backdrop image,
 * gradient overlay (bottom → black), title, year + type badge,
 * plot (2 lines, truncated), Play + More info buttons.
 *
 * Auto-rotates every 8 seconds. Slides animate via framer-motion.
 */
export function HeroCarousel({ items, onPlay, onMoreInfo }: HeroCarouselProps) {
  const [index, setIndex] = React.useState(0);
  const [paused, setPaused] = React.useState(false);
  const slides = items.slice(0, 5);

  // Reset to first slide when items change.
  React.useEffect(() => {
    setIndex(0);
  }, [items]);

  // Auto-rotate.
  React.useEffect(() => {
    if (paused || slides.length <= 1) return;
    const t = setInterval(() => {
      setIndex((i) => (i + 1) % slides.length);
    }, AUTO_ROTATE_MS);
    return () => clearInterval(t);
  }, [paused, slides.length]);

  if (slides.length === 0) return null;

  const current = slides[index]!;
  const scoreStr = formatScore(current.score);
  const typeLabel = tvTypeLabel(current.type);
  const year = "year" in current ? (current as { year?: number }).year : undefined;
  const posterUrl = current.posterUrl;

  return (
    <section
      className="relative w-full overflow-hidden"
      style={{ height: "min(56vh, 480px)" }}
      aria-roledescription="carousel"
      aria-label="Featured titles"
      onMouseEnter={() => setPaused(true)}
      onMouseLeave={() => setPaused(false)}
    >
      <AnimatePresence mode="wait">
        <motion.div
          key={current.url}
          initial={{ opacity: 0, scale: 1.04 }}
          animate={{ opacity: 1, scale: 1 }}
          exit={{ opacity: 0, scale: 1 }}
          transition={{ duration: 0.6, ease: "easeOut" }}
          className="absolute inset-0"
        >
          {/* Backdrop image */}
          {posterUrl ? (
            <img
              src={posterUrl}
              alt=""
              className="size-full object-cover"
              referrerPolicy="no-referrer"
            />
          ) : (
            <div className="size-full bg-gradient-to-br from-primary/30 via-background to-background" />
          )}

          {/* Gradient overlays */}
          <div className="absolute inset-0 bg-gradient-to-t from-background via-background/70 to-transparent" />
          <div className="absolute inset-0 bg-gradient-to-r from-background/80 via-background/30 to-transparent" />

          {/* Content */}
          <div className="absolute inset-x-0 bottom-0 flex flex-col gap-3 px-4 pb-8 sm:px-8 lg:px-12">
            {/* Badges */}
            <div className="flex flex-wrap items-center gap-2">
              {typeLabel && (
                <Badge variant="default" className="text-xs uppercase tracking-wide">
                  {typeLabel}
                </Badge>
              )}
              {year != null && (
                <Badge variant="secondary" className="text-xs">
                  {year}
                </Badge>
              )}
              {scoreStr && (
                <Badge
                  variant="outline"
                  className="gap-1 border-white/30 bg-black/40 text-white backdrop-blur-sm"
                >
                  <Star className="size-3 fill-current text-primary" />
                  <span className="font-semibold">{scoreStr}</span>
                </Badge>
              )}
            </div>

            {/* Title */}
            <h2 className="max-w-3xl text-3xl font-bold leading-tight tracking-tight text-foreground drop-shadow-[0_2px_8px_rgba(0,0,0,0.6)] sm:text-4xl lg:text-5xl">
              {current.name}
            </h2>

            {/* Plot — extracted from provider name only; CS3 home rows don't
                include plots. Show a short call-to-action instead. */}
            <p className="max-w-2xl text-sm leading-relaxed text-muted-foreground line-clamp-2 sm:text-base">
              {current.apiName
                ? `Discover "${current.name}" on ${current.apiName}. Tap "More info" to see details, episodes, and sources.`
                : `Tap "More info" to see details, episodes, and sources for "${current.name}".`}
            </p>

            {/* Action buttons */}
            <div className="mt-1 flex flex-wrap items-center gap-3">
              <Button
                size="lg"
                onClick={() => onPlay?.(current)}
                className="h-11 min-w-[7rem] gap-2 rounded-full px-6 shadow-[0_0_24px_-6px_var(--primary)]"
              >
                <Play className="size-4 fill-current" />
                Play
              </Button>
              <Button
                size="lg"
                variant="secondary"
                onClick={() => onMoreInfo?.(current)}
                className="h-11 min-w-[7rem] gap-2 rounded-full px-6"
              >
                <Info className="size-4" />
                More info
              </Button>
            </div>

            {/* Slide indicators */}
            {slides.length > 1 && (
              <div
                className="mt-3 flex items-center gap-1.5"
                role="tablist"
                aria-label="Slide indicators"
              >
                {slides.map((s, i) => {
                  const isActive = i === index;
                  return (
                    <button
                      key={s.url}
                      type="button"
                      role="tab"
                      aria-selected={isActive}
                      aria-label={`Go to slide ${i + 1}`}
                      onClick={() => setIndex(i)}
                      className={
                        "h-1.5 rounded-full transition-all duration-300 " +
                        (isActive
                          ? "w-7 bg-primary"
                          : "w-3 bg-foreground/30 hover:bg-foreground/50")
                      }
                    />
                  );
                })}
              </div>
            )}
          </div>
        </motion.div>
      </AnimatePresence>
    </section>
  );
}

/** Skeleton placeholder for the hero carousel. */
export function HeroCarouselSkeleton() {
  return (
    <section
      className="relative w-full overflow-hidden"
      style={{ height: "min(56vh, 480px)" }}
      aria-hidden="true"
    >
      <div className="absolute inset-0 animate-pulse bg-gradient-to-br from-muted/60 via-muted/40 to-background" />
      <div className="absolute inset-x-0 bottom-0 flex flex-col gap-3 px-4 pb-8 sm:px-8 lg:px-12">
        <div className="h-5 w-32 animate-pulse rounded bg-foreground/20" />
        <div className="h-10 w-2/3 max-w-2xl animate-pulse rounded bg-foreground/20" />
        <div className="h-4 w-1/2 max-w-xl animate-pulse rounded bg-foreground/10" />
        <div className="mt-2 flex gap-3">
          <div className="h-11 w-28 animate-pulse rounded-full bg-foreground/20" />
          <div className="h-11 w-28 animate-pulse rounded-full bg-foreground/10" />
        </div>
      </div>
    </section>
  );
}
