"use client";

import * as React from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { PosterCard, PosterCardSkeleton } from "./poster-card";
import type { SearchResponse } from "@/lib/types/shared";
import { cn } from "@/lib/utils";

interface PosterRowProps {
  title: string;
  /** Optional secondary label (e.g. the provider name). */
  subtitle?: string;
  items: SearchResponse[];
  loading?: boolean;
  onCardClick?: (item: SearchResponse) => void;
}

/**
 * Horizontal scrollable row of poster cards.
 *
 * Uses the `scrollbar-hide` utility (already declared in globals.css)
 * to hide the scrollbar. Two edge buttons appear on hover (desktop only)
 * for keyboard/trackpad users who prefer clicking.
 */
export function PosterRow({
  title,
  subtitle,
  items,
  loading = false,
  onCardClick,
}: PosterRowProps) {
  const scrollRef = React.useRef<HTMLDivElement>(null);

  const scrollByAmount = (dir: "left" | "right") => {
    const el = scrollRef.current;
    if (!el) return;
    const amount = el.clientWidth * 0.85 * (dir === "left" ? -1 : 1);
    el.scrollBy({ left: amount, behavior: "smooth" });
  };

  return (
    <section className="group/row relative flex flex-col gap-2 py-2">
      {/* Heading */}
      <div className="flex items-end justify-between gap-3 px-3 sm:px-6">
        <div className="flex min-w-0 flex-col">
          <h2 className="line-clamp-1 text-base font-semibold tracking-tight text-foreground sm:text-lg">
            {title}
          </h2>
          {subtitle && (
            <span className="truncate text-xs text-muted-foreground">
              {subtitle}
            </span>
          )}
        </div>
      </div>

      {/* Scroll container */}
      <div className="relative">
        {/* Edge fade overlays */}
        <div
          className="pointer-events-none absolute inset-y-0 left-0 z-10 w-8 bg-gradient-to-r from-background to-transparent"
          aria-hidden="true"
        />
        <div
          className="pointer-events-none absolute inset-y-0 right-0 z-10 w-8 bg-gradient-to-l from-background to-transparent"
          aria-hidden="true"
        />

        {/* Left arrow */}
        <button
          type="button"
          aria-label="Scroll left"
          onClick={() => scrollByAmount("left")}
          className={cn(
            "absolute left-0 top-1/2 z-20 hidden h-10 w-8 -translate-y-1/2 items-center justify-center",
            "rounded-r-md bg-background/80 text-foreground/70 backdrop-blur-sm",
            "opacity-0 transition-opacity duration-200 hover:bg-background hover:text-foreground",
            "group-hover/row:opacity-100 md:flex",
          )}
        >
          <ChevronLeft className="size-5" />
        </button>

        {/* Right arrow */}
        <button
          type="button"
          aria-label="Scroll right"
          onClick={() => scrollByAmount("right")}
          className={cn(
            "absolute right-0 top-1/2 z-20 hidden h-10 w-8 -translate-y-1/2 items-center justify-center",
            "rounded-l-md bg-background/80 text-foreground/70 backdrop-blur-sm",
            "opacity-0 transition-opacity duration-200 hover:bg-background hover:text-foreground",
            "group-hover/row:opacity-100 md:flex",
          )}
        >
          <ChevronRight className="size-5" />
        </button>

        {/* Cards strip */}
        <div
          ref={scrollRef}
          className={cn(
            "scrollbar-hide flex gap-3 overflow-x-auto scroll-smooth",
            "snap-x snap-mandatory",
            "px-3 pb-2 sm:px-6",
          )}
        >
          {loading
            ? Array.from({ length: 8 }).map((_, i) => (
                <div key={i} className="snap-start">
                  <PosterCardSkeleton />
                </div>
              ))
            : items.map((item, i) => (
                <div key={`${item.url}-${i}`} className="snap-start">
                  <PosterCard item={item} onClick={onCardClick} />
                </div>
              ))}
        </div>
      </div>
    </section>
  );
}
