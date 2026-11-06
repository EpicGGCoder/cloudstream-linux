"use client";

import * as React from "react";
import { Bookmark, Clock, Library as LibraryIcon } from "lucide-react";
import { Button } from "@/components/ui/button";

interface LibraryViewProps {
  onBrowse?: () => void;
}

/**
 * Library tab — placeholder for favorites / watch history.
 *
 * Will be expanded in a later phase to actually persist favorites and
 * progress. For now, shows an empty-state.
 */
export function LibraryView({ onBrowse }: LibraryViewProps) {
  return (
    <div className="flex flex-1 flex-col gap-8 px-3 pb-6 pt-4 sm:px-6">
      <div className="flex items-center gap-3">
        <LibraryIcon className="size-6 text-primary" />
        <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">
          Your Library
        </h1>
      </div>

      {/* Bookmarks (empty) */}
      <section className="flex flex-col gap-3">
        <h2 className="flex items-center gap-2 text-sm font-semibold tracking-tight text-muted-foreground">
          <Bookmark className="size-4" />
          Bookmarks
        </h2>
        <div className="flex flex-col items-center justify-center gap-3 rounded-xl border border-dashed border-border/60 bg-card/30 px-6 py-12 text-center">
          <Bookmark className="size-10 text-muted-foreground/50" />
          <p className="max-w-sm text-sm text-muted-foreground">
            Your library is empty. Browse and bookmark titles to see them here.
          </p>
          {onBrowse && (
            <Button variant="default" size="sm" className="mt-2" onClick={onBrowse}>
              Browse titles
            </Button>
          )}
        </div>
      </section>

      {/* Continue watching (empty) */}
      <section className="flex flex-col gap-3">
        <h2 className="flex items-center gap-2 text-sm font-semibold tracking-tight text-muted-foreground">
          <Clock className="size-4" />
          Continue watching
        </h2>
        <div className="flex flex-col items-center justify-center gap-2 rounded-xl border border-dashed border-border/60 bg-card/30 px-6 py-12 text-center">
          <Clock className="size-10 text-muted-foreground/50" />
          <p className="max-w-sm text-sm text-muted-foreground">
            Your watch history will appear here.
          </p>
        </div>
      </section>
    </div>
  );
}
