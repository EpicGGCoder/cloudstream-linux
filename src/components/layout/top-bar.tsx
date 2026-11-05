"use client";

import * as React from "react";
import { CloudIcon, Search } from "lucide-react";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";

interface TopBarProps {
  /** Current search query value (controlled). */
  query: string;
  /** Fired on every keystroke — parent decides whether to debounce. */
  onQueryChange: (q: string) => void;
  /** Fired on submit (Enter or click). */
  onSearchSubmit: (q: string) => void;
  /** Click the search bar to switch tabs. */
  onFocusSearch?: () => void;
}

/**
 * Sticky top bar — CloudStream logo + a search trigger.
 *
 * 56px tall on desktop, slightly taller on mobile to keep touch targets
 * ≥44px. The search input is the primary interactive element.
 */
export function TopBar({ query, onQueryChange, onSearchSubmit, onFocusSearch }: TopBarProps) {
  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      onSearchSubmit(query);
    }
  };

  return (
    <header
      className={cn(
        "sticky top-0 z-30 h-14 w-full border-b border-border/60",
        "bg-background/80 backdrop-blur-md supports-[backdrop-filter]:bg-background/60",
      )}
      data-slot="top-bar"
    >
      <div className="mx-auto flex h-full w-full max-w-screen-2xl items-center gap-3 px-3 sm:px-4">
        {/* Logo */}
        <div className="flex shrink-0 items-center gap-2">
          <span
            className="flex size-8 items-center justify-center rounded-md bg-primary text-primary-foreground shadow-[0_0_16px_-2px_var(--primary)]"
            aria-hidden="true"
          >
            <CloudIcon className="size-5" />
          </span>
          <span className="hidden text-base font-semibold tracking-tight sm:inline">
            CloudStream
          </span>
        </div>

        {/* Search */}
        <div className="relative flex-1">
          <Search
            className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground"
            aria-hidden="true"
          />
          <Input
            type="search"
            value={query}
            onChange={(e) => onQueryChange(e.target.value)}
            onKeyDown={handleKeyDown}
            onFocus={onFocusSearch}
            placeholder="Search movies, TV shows, anime..."
            aria-label="Search"
            className={cn(
              "h-10 w-full rounded-full border-border/60 bg-input/40 pl-9 pr-4",
              "placeholder:text-muted-foreground/70",
              "focus-visible:border-primary focus-visible:ring-primary/30",
            )}
          />
        </div>
      </div>
    </header>
  );
}
