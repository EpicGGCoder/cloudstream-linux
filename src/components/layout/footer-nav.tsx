"use client";

import * as React from "react";
import { Home, Search, Library, Settings } from "lucide-react";
import { cn } from "@/lib/utils";

export type TabId = "home" | "search" | "library" | "settings";

interface FooterNavProps {
  active: TabId;
  onChange: (tab: TabId) => void;
}

const TABS: { id: TabId; label: string; icon: React.ComponentType<{ className?: string }> }[] = [
  { id: "home", label: "Home", icon: Home },
  { id: "search", label: "Search", icon: Search },
  { id: "library", label: "Library", icon: Library },
  { id: "settings", label: "Settings", icon: Settings },
];

/**
 * Sticky bottom navigation bar — CloudStream-style 4-tab footer.
 *
 * Critical: this MUST stick to the bottom of the viewport when content is
 * short, and be pushed down naturally when content overflows. The parent
 * shell uses `min-h-screen flex flex-col` so this component uses `mt-auto`
 * to achieve the sticky-to-bottom behavior.
 */
export function FooterNav({ active, onChange }: FooterNavProps) {
  return (
    <footer
      className={cn(
        "sticky bottom-0 z-30 mt-auto",
        "border-t border-border/60 bg-background/80 backdrop-blur-md",
        "supports-[backdrop-filter]:bg-background/60",
      )}
    >
      <nav
        className="mx-auto flex w-full max-w-screen-2xl items-stretch justify-around px-1 sm:px-2"
        role="tablist"
        aria-label="Main navigation"
      >
        {TABS.map(({ id, label, icon: Icon }) => {
          const isActive = active === id;
          return (
            <button
              key={id}
              type="button"
              role="tab"
              aria-selected={isActive}
              aria-controls={`panel-${id}`}
              onClick={() => onChange(id)}
              className={cn(
                "group relative flex min-h-[56px] flex-1 flex-col items-center justify-center gap-1 px-2 py-2 transition-colors duration-200",
                "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/60 focus-visible:ring-inset",
                isActive ? "text-primary" : "text-muted-foreground hover:text-foreground",
              )}
            >
              <span
                className={cn(
                  "flex h-7 w-7 items-center justify-center transition-transform duration-200",
                  isActive
                    ? "scale-110 drop-shadow-[0_0_8px_var(--primary)]"
                    : "group-hover:scale-105",
                )}
              >
                <Icon className="size-5" />
              </span>
              <span className="text-[11px] font-medium leading-none tracking-tight">
                {label}
              </span>
              {/* Active indicator bar */}
              <span
                className={cn(
                  "absolute top-0 h-[2px] w-10 rounded-b-full bg-primary transition-opacity duration-200",
                  isActive ? "opacity-100" : "opacity-0",
                )}
                aria-hidden="true"
              />
            </button>
          );
        })}
      </nav>
    </footer>
  );
}
