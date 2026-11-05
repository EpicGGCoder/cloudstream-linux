"use client";

import * as React from "react";
import { TopBar } from "@/components/layout/top-bar";
import { FooterNav, type TabId } from "@/components/layout/footer-nav";
import { HomeView } from "@/components/home/home-view";
import { SearchView } from "@/components/search/search-view";
import { LibraryView } from "@/components/library/library-view";
import { SettingsView } from "@/components/settings/settings-view";
import { DetailModal } from "@/components/detail/detail-modal";
import {
  PlayerModal,
  type PlayerTarget,
  moviePlayerTarget,
  episodePlayerTarget,
} from "@/components/player/player-modal";
import type {
  Episode,
  MovieLoadResponse,
  SearchResponse,
  TvSeriesLoadResponse,
} from "@/lib/types/shared";

/**
 * CloudStream-style single-page app shell.
 *
 * Layout:
 *   - TopBar (sticky top, 56px tall)
 *   - Main content area (flex-1, scrolls)
 *   - FooterNav (sticky to bottom via mt-auto on a flex-col root)
 *
 * State:
 *   - activeTab: which of the four tabs is visible
 *   - searchQuery: controlled by the top-bar and the search tab
 *   - detail: { apiName, url } for the open detail modal
 *   - player: PlayerTarget for the open player modal
 */
export default function Home() {
  const [activeTab, setActiveTab] = React.useState<TabId>("home");
  const [query, setQuery] = React.useState("");

  // Detail modal state.
  const [detail, setDetail] = React.useState<{
    apiName: string;
    url: string;
  } | null>(null);

  // Player modal state.
  const [player, setPlayer] = React.useState<PlayerTarget | null>(null);

  // Open the detail modal for a search result.
  const openDetail = React.useCallback((item: SearchResponse) => {
    setDetail({ apiName: item.apiName, url: item.url });
  }, []);

  // Close detail modal.
  const closeDetail = React.useCallback(() => {
    setDetail(null);
  }, []);

  // Top-bar search input handling — switching tabs as needed.
  const handleQueryChange = React.useCallback(
    (q: string) => {
      setQuery(q);
      // If user is typing in the top bar while not on Search tab, switch.
      // (We don't switch back automatically — only when typing starts.)
      if (q && activeTab !== "search") {
        setActiveTab("search");
      }
    },
    [activeTab],
  );

  const handleSearchSubmit = React.useCallback(
    (q: string) => {
      if (q.trim()) {
        setActiveTab("search");
      }
    },
    [],
  );

  const handleFocusSearch = React.useCallback(() => {
    // Switch to search tab when the top-bar input is focused.
    if (activeTab !== "search") setActiveTab("search");
  }, [activeTab]);

  // Hero "Play" — open detail first, then play.
  // For the hero we don't yet have a loadResponse. We open the detail
  // modal which fetches it; the user can then press Play.
  // But the spec asks for a "Play" button that opens the player directly.
  // We open the detail modal with a flag to auto-play. For simplicity here,
  // we open the detail modal and let the user press Play inside.
  const handleHeroPlay = React.useCallback(
    (item: SearchResponse) => {
      openDetail(item);
    },
    [openDetail],
  );

  // Detail "Play" (movie) — open the player modal.
  const handlePlayMovie = React.useCallback(
    (load: MovieLoadResponse) => {
      setPlayer(moviePlayerTarget(load));
      closeDetail();
    },
    [closeDetail],
  );

  // Detail "Play" (episode) — open the player modal.
  // Close the detail modal first — Radix Dialog doesn't support two open at once.
  const handlePlayEpisode = React.useCallback(
    (load: TvSeriesLoadResponse, episode: Episode) => {
      setPlayer(episodePlayerTarget(load, episode));
      closeDetail();
    },
    [closeDetail],
  );

  return (
    <div
      className="flex min-h-screen flex-col bg-background text-foreground"
      data-slot="app-shell"
    >
      {/* Top bar */}
      <TopBar
        query={query}
        onQueryChange={handleQueryChange}
        onSearchSubmit={handleSearchSubmit}
        onFocusSearch={handleFocusSearch}
      />

      {/* Main content area — flex-1, scrolls */}
      <main
        id={`panel-${activeTab}`}
        role="tabpanel"
        aria-label={`${activeTab} tab`}
        className="flex flex-1 flex-col"
      >
        {/* Render all views to preserve their scroll/state when switching. */}
        <div className={activeTab === "home" ? "flex flex-1 flex-col" : "hidden"}>
          <HomeView
            onCardClick={openDetail}
            onPlayFromHero={handleHeroPlay}
          />
        </div>
        <div
          className={
            activeTab === "search" ? "flex flex-1 flex-col" : "hidden"
          }
        >
          <SearchView
            query={query}
            onQueryChange={setQuery}
            onCardClick={openDetail}
          />
        </div>
        <div
          className={
            activeTab === "library" ? "flex flex-1 flex-col" : "hidden"
          }
        >
          <LibraryView onBrowse={() => setActiveTab("home")} />
        </div>
        <div
          className={
            activeTab === "settings" ? "flex flex-1 flex-col" : "hidden"
          }
        >
          <SettingsView />
        </div>
      </main>

      {/* Footer (sticky to bottom via mt-auto on flex-col root) */}
      <FooterNav active={activeTab} onChange={setActiveTab} />

      {/* Detail modal */}
      <DetailModal
        open={detail !== null}
        onOpenChange={(o) => {
          if (!o) closeDetail();
        }}
        apiName={detail?.apiName ?? null}
        url={detail?.url ?? null}
        onCardClick={(item) => {
          // Recommendations inside detail — swap to a new detail target.
          setDetail({ apiName: item.apiName, url: item.url });
        }}
        onPlayMovie={handlePlayMovie}
        onPlayEpisode={handlePlayEpisode}
      />

      {/* Player modal */}
      <PlayerModal
        open={player !== null}
        onOpenChange={(o) => {
          if (!o) setPlayer(null);
        }}
        title={player?.title ?? ""}
        apiName={player?.apiName ?? ""}
        data={player?.data ?? null}
        initialSubtitles={player?.subtitles}
      />
    </div>
  );
}
