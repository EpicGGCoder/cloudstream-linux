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
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  ChevronDown,
  Loader2,
  X,
  AlertCircle,
  Subtitles,
  Settings2,
} from "lucide-react";
import { useLoadLinks } from "@/lib/hooks/use-cloudstream";
import type {
  Episode,
  ExtractorLink,
  MovieLoadResponse,
  SubtitleFile,
  TvSeriesLoadResponse,
} from "@/lib/types/shared";
import { cn } from "@/lib/utils";

interface PlayerModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** Title shown in the title bar. */
  title: string;
  /** Provider name (for /api/links body). */
  apiName: string;
  /** Either a MovieLoadResponse.dataUrl or an Episode.data string. */
  data: string | null;
  /** Optional subtitle list (already known — e.g. from a series episode). */
  initialSubtitles?: SubtitleFile[];
}

function isHlsUrl(url: string): boolean {
  const lower = url.toLowerCase().split("?")[0]!;
  return lower.endsWith(".m3u8") || lower.includes(".m3u8");
}

/** Pick the best initial source — highest quality non-torrent first. */
function pickInitialSource(links: ExtractorLink[]): ExtractorLink | null {
  if (links.length === 0) return null;
  const sorted = [...links].sort((a, b) => {
    // Prefer non-torrent.
    const ta = a.type === "TORRENT" || a.type === "MAGNET" ? 1 : 0;
    const tb = b.type === "TORRENT" || b.type === "MAGNET" ? 1 : 0;
    if (ta !== tb) return ta - tb;
    // Then by quality desc.
    return (b.quality ?? 0) - (a.quality ?? 0);
  });
  return sorted[0] ?? null;
}

function qualityLabel(q: number): string {
  if (!q || q < 100) return "?";
  if (q >= 2160) return "4K";
  if (q >= 1440) return "1440p";
  if (q >= 1080) return "1080p";
  if (q >= 720) return "720p";
  if (q >= 480) return "480p";
  if (q >= 360) return "360p";
  return `${q}p`;
}

function sourceShortName(link: ExtractorLink): string {
  return link.name || link.source || "Source";
}

/**
 * Player modal — full-screen Dialog, dark, no padding.
 *
 * - `<video>` element fills the modal.
 * - If the URL ends in `.m3u8`, uses hls.js to attach.
 * - Otherwise native `<video>`.
 * - Title bar (top, auto-hide after 3s): title + close button.
 * - Source picker (bottom-left): dropdown of available `ExtractorLink`s.
 * - Subtitle picker (bottom-right): dropdown of `SubtitleFile`s by language.
 * - Loading state: spinner overlay.
 * - Error state: friendly message + close button.
 */
export function PlayerModal({
  open,
  onOpenChange,
  title,
  apiName,
  data,
  initialSubtitles,
}: PlayerModalProps) {
  const videoRef = React.useRef<HTMLVideoElement>(null);
  const hlsRef = React.useRef<unknown>(null);

  const loadLinks = useLoadLinks();

  // Trigger load when opening with new data.
  React.useEffect(() => {
    if (!open) return;
    if (!apiName || !data) return;
    loadLinks.reset();
    loadLinks.mutate({ apiName, data });
  }, [open, apiName, data, loadLinks]);

  const links = loadLinks.data?.links ?? [];
  const subtitles = loadLinks.data?.subtitles ?? initialSubtitles ?? [];

  const [selectedSource, setSelectedSource] = React.useState<ExtractorLink | null>(null);
  const [selectedSubtitle, setSelectedSubtitle] = React.useState<SubtitleFile | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  const [loading, setLoading] = React.useState<boolean>(false);
  const [controlsVisible, setControlsVisible] = React.useState<boolean>(true);
  const hideTimerRef = React.useRef<ReturnType<typeof setTimeout> | null>(null);

  // Reset selected source when new links arrive.
  React.useEffect(() => {
    if (links.length > 0 && !selectedSource) {
      setSelectedSource(pickInitialSource(links));
    }
    if (links.length === 0) {
      setSelectedSource(null);
    }
  }, [links, selectedSource]);

  // Reset everything on close.
  React.useEffect(() => {
    if (!open) {
      setSelectedSource(null);
      setSelectedSubtitle(null);
      setError(null);
      setLoading(false);
      if (hlsRef.current) {
        try {
          (hlsRef.current as { destroy: () => void }).destroy();
        } catch {
          // ignore
        }
        hlsRef.current = null;
      }
      if (videoRef.current) {
        try {
          videoRef.current.pause();
          videoRef.current.removeAttribute("src");
          videoRef.current.load();
        } catch {
          // ignore
        }
      }
      if (hideTimerRef.current) {
        clearTimeout(hideTimerRef.current);
        hideTimerRef.current = null;
      }
    }
  }, [open]);

  // Attach media to <video> when source changes.
  React.useEffect(() => {
    if (!open) return;
    if (!selectedSource) return;
    const video = videoRef.current;
    if (!video) return;

    setError(null);
    setLoading(true);

    // Tear down previous hls.
    if (hlsRef.current) {
      try {
        (hlsRef.current as { destroy: () => void }).destroy();
      } catch {
        // ignore
      }
      hlsRef.current = null;
    }

    const url = selectedSource.url;
    const useHls = isHlsUrl(url) || selectedSource.isM3u8;

    let cancelled = false;

    const attachHls = async () => {
      try {
        const mod = await import("hls.js");
        const Hls = mod.default;
        if (cancelled) return;
        if (Hls.isSupported()) {
          const hls = new Hls({ enableWorker: true, lowLatencyMode: false });
          hlsRef.current = hls;
          hls.loadSource(url);
          hls.attachMedia(video);
          hls.on(Hls.Events.MANIFEST_PARSED, () => {
            setLoading(false);
            video.play().catch(() => {
              // autoplay may be blocked; the user can press play.
            });
          });
          hls.on(Hls.Events.ERROR, (_evt, data) => {
            if (data?.fatal) {
              setError(`Playback error: ${data.type ?? "unknown"}`);
              setLoading(false);
            }
          });
        } else if (video.canPlayType("application/vnd.apple.mpegurl")) {
          // Safari native HLS.
          video.src = url;
          setLoading(false);
          video.play().catch(() => {});
        } else {
          setError("HLS not supported in this browser.");
          setLoading(false);
        }
      } catch (e) {
        setError(`Failed to load player: ${String(e)}`);
        setLoading(false);
      }
    };

    if (useHls) {
      attachHls();
    } else {
      // Native video.
      video.src = url;
      setLoading(false);
      video.play().catch(() => {
        // autoplay may be blocked.
      });
    }

    return () => {
      cancelled = true;
    };
  }, [open, selectedSource]);

  // Auto-hide controls after 3s of no movement.
  const scheduleHide = React.useCallback(() => {
    if (hideTimerRef.current) clearTimeout(hideTimerRef.current);
    hideTimerRef.current = setTimeout(() => setControlsVisible(false), 3000);
  }, []);

  const revealControls = React.useCallback(() => {
    setControlsVisible(true);
    scheduleHide();
  }, [scheduleHide]);

  React.useEffect(() => {
    if (open) scheduleHide();
    return () => {
      if (hideTimerRef.current) {
        clearTimeout(hideTimerRef.current);
        hideTimerRef.current = null;
      }
    };
  }, [open, scheduleHide]);

  // Subtitle track management.
  React.useEffect(() => {
    const video = videoRef.current;
    if (!video) return;
    // Clear existing cues by removing all <track> children we added.
    const existing = video.querySelectorAll("track[data-cs-sub]");
    existing.forEach((t) => t.remove());
    if (!selectedSubtitle) return;

    const track = document.createElement("track");
    track.kind = "subtitles";
    track.label = selectedSubtitle.lang;
    track.srclang = selectedSubtitle.lang.slice(0, 2);
    track.src = selectedSubtitle.url;
    track.setAttribute("data-cs-sub", "1");
    track.default = true;
    track.addEventListener("error", () => {
      // CORS may block subtitle loading — not fatal.
    });
    video.appendChild(track);
    // Activate the new track.
    for (let i = 0; i < video.textTracks.length; i++) {
      const t = video.textTracks[i];
      if (t === video.textTracks[video.textTracks.length - 1]) {
        t.mode = "showing";
      } else {
        t.mode = "disabled";
      }
    }
  }, [selectedSubtitle]);

  const isLoading = loadLinks.isPending || loading;
  const isError = !!error || (!!loadLinks.error && !loadLinks.data);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent
        showCloseButton={false}
        className={cn(
          "max-w-none gap-0 overflow-hidden p-0",
          "h-[100vh] w-[100vw] sm:h-[95vh] sm:w-[95vw] sm:rounded-lg",
          "border-border/40 bg-black",
        )}
        onMouseMove={revealControls}
        onMouseLeave={() => setControlsVisible(false)}
      >
        <DialogTitle className="sr-only">Player — {title}</DialogTitle>
        <DialogDescription className="sr-only">
          Video player with source and subtitle pickers.
        </DialogDescription>

        {/* Video */}
        <div className="relative size-full bg-black">
          <video
            ref={videoRef}
            className="size-full object-contain"
            controls={false}
            playsInline
            preload="metadata"
            onClick={() => {
              const v = videoRef.current;
              if (!v) return;
              if (v.paused) v.play().catch(() => {});
              else v.pause();
            }}
            onWaiting={() => setLoading(true)}
            onPlaying={() => setLoading(false)}
            onCanPlay={() => setLoading(false)}
            onError={() => {
              setError("Video playback error.");
              setLoading(false);
            }}
          />

          {/* Loading spinner overlay */}
          {isLoading && !isError && (
            <div className="pointer-events-none absolute inset-0 flex items-center justify-center bg-black/40">
              <Loader2 className="size-12 animate-spin text-primary" />
            </div>
          )}

          {/* Error overlay */}
          {isError && (
            <div className="absolute inset-0 flex flex-col items-center justify-center gap-3 p-8 text-center">
              <AlertCircle className="size-12 text-destructive" />
              <h3 className="text-lg font-semibold tracking-tight">
                {error ? "Playback error" : "Failed to load sources"}
              </h3>
              <p className="max-w-md text-sm text-muted-foreground">
                {error ??
                  String(loadLinks.error?.message ?? "").slice(0, 200)}
              </p>
              <Button
                variant="secondary"
                onClick={() => onOpenChange(false)}
                className="mt-2"
              >
                Close
              </Button>
            </div>
          )}

          {/* No sources available */}
          {!isLoading && !isError && links.length === 0 && (
            <div className="absolute inset-0 flex flex-col items-center justify-center gap-3 p-8 text-center">
              <AlertCircle className="size-12 text-muted-foreground" />
              <h3 className="text-lg font-semibold tracking-tight">
                No playable sources
              </h3>
              <p className="max-w-md text-sm text-muted-foreground">
                This provider returned no direct video links. The title may be
                a meta-provider (TMDB) — install a streaming plugin to get
                playable sources.
              </p>
              <Button
                variant="secondary"
                onClick={() => onOpenChange(false)}
                className="mt-2"
              >
                Close
              </Button>
            </div>
          )}

          {/* Title bar (auto-hide) */}
          <div
            className={cn(
              "pointer-events-none absolute inset-x-0 top-0 z-20 flex items-center justify-between gap-3 p-3 sm:p-4",
              "bg-gradient-to-b from-black/70 to-transparent",
              "transition-opacity duration-300",
              controlsVisible ? "opacity-100" : "opacity-0",
            )}
          >
            <h3 className="pointer-events-auto line-clamp-1 text-sm font-semibold tracking-tight text-white drop-shadow sm:text-base">
              {title}
            </h3>
            <Button
              variant="ghost"
              size="icon"
              onClick={() => onOpenChange(false)}
              className="pointer-events-auto size-9 rounded-full bg-black/40 text-white hover:bg-black/60 hover:text-white"
              aria-label="Close player"
            >
              <X className="size-5" />
            </Button>
          </div>

          {/* Bottom controls */}
          {selectedSource && !isError && (
            <div
              className={cn(
                "pointer-events-none absolute inset-x-0 bottom-0 z-20 flex items-end justify-between gap-3 p-3 sm:p-4",
                "bg-gradient-to-t from-black/70 to-transparent",
                "transition-opacity duration-300",
                controlsVisible ? "opacity-100" : "opacity-0",
              )}
            >
              {/* Source picker (bottom-left) */}
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button
                    variant="secondary"
                    size="sm"
                    className="pointer-events-auto gap-2 rounded-full bg-black/60 text-white hover:bg-black/80 hover:text-white"
                  >
                    <Settings2 className="size-3.5" />
                    <span className="max-w-[12ch] truncate">
                      {selectedSource ? sourceShortName(selectedSource) : "Source"}
                    </span>
                    <Badge variant="outline" className="border-white/30 bg-transparent text-[10px] text-white">
                      {qualityLabel(selectedSource.quality)}
                    </Badge>
                    <ChevronDown className="size-3.5" />
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="start" className="max-w-[20rem]">
                  <DropdownMenuLabel>Sources</DropdownMenuLabel>
                  <DropdownMenuSeparator />
                  {links.map((l, i) => (
                    <DropdownMenuItem
                      key={`${l.url}-${i}`}
                      onSelect={() => setSelectedSource(l)}
                      className="flex items-center justify-between gap-3"
                    >
                      <span className="flex min-w-0 flex-col">
                        <span className="truncate text-sm">{sourceShortName(l)}</span>
                        <span className="truncate text-[10px] text-muted-foreground">
                          {l.source}
                        </span>
                      </span>
                      <Badge variant="secondary" className="text-[10px]">
                        {qualityLabel(l.quality)}
                      </Badge>
                    </DropdownMenuItem>
                  ))}
                  {links.length === 0 && (
                    <DropdownMenuItem disabled>No sources</DropdownMenuItem>
                  )}
                </DropdownMenuContent>
              </DropdownMenu>

              {/* Subtitle picker (bottom-right) */}
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button
                    variant="secondary"
                    size="sm"
                    className="pointer-events-auto gap-2 rounded-full bg-black/60 text-white hover:bg-black/80 hover:text-white"
                    disabled={subtitles.length === 0}
                  >
                    <Subtitles className="size-3.5" />
                    <span className="max-w-[10ch] truncate text-xs">
                      {selectedSubtitle ? selectedSubtitle.lang : "Subs"}
                    </span>
                    <ChevronDown className="size-3.5" />
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end" className="max-w-[16rem]">
                  <DropdownMenuLabel>Subtitles</DropdownMenuLabel>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem onSelect={() => setSelectedSubtitle(null)}>
                    Off
                  </DropdownMenuItem>
                  {subtitles.map((s, i) => (
                    <DropdownMenuItem
                      key={`${s.lang}-${i}`}
                      onSelect={() => setSelectedSubtitle(s)}
                    >
                      <span className="truncate text-sm">{s.lang}</span>
                    </DropdownMenuItem>
                  ))}
                </DropdownMenuContent>
              </DropdownMenu>
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}

/**
 * Convenience wrapper: opens a player for a movie (dataUrl) or an episode.
 * Usage from page.tsx.
 */
export interface PlayerTarget {
  title: string;
  apiName: string;
  data: string | null;
  subtitles?: SubtitleFile[];
}

/** Build a PlayerTarget from a MovieLoadResponse. */
export function moviePlayerTarget(load: MovieLoadResponse): PlayerTarget {
  return {
    title: load.name,
    apiName: load.apiName,
    data: load.dataUrl,
  };
}

/** Build a PlayerTarget from a TV series episode. */
export function episodePlayerTarget(
  load: TvSeriesLoadResponse,
  episode: Episode,
): PlayerTarget {
  return {
    title: `${load.name} — ${episode.name ?? `E${episode.episode ?? ""}`}`,
    apiName: load.apiName,
    data: episode.data,
  };
}
