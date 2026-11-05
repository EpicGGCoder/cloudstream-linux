"use client";

import * as React from "react";
import {
  Settings as SettingsIcon,
  AlertCircle,
  Globe,
  Layers,
  Tag,
  Cpu,
  Info,
  Github,
} from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Switch } from "@/components/ui/switch";
import { Skeleton } from "@/components/ui/skeleton";
import { useProviders } from "@/lib/hooks/use-cloudstream";
import type { ProviderInfo, TvType } from "@/lib/types/shared";

const APP_VERSION = "0.1.0";

const TV_TYPE_LABELS: Partial<Record<TvType, string>> = {
  Movie: "Movie",
  AnimeMovie: "Anime Movie",
  TvSeries: "TV Series",
  Cartoon: "Cartoon",
  Anime: "Anime",
  OVA: "OVA",
  Documentary: "Documentary",
  AsianDrama: "Drama",
  Live: "Live",
  Torrent: "Torrent",
  Others: "Other",
  Music: "Music",
  AudioBook: "AudioBook",
  CustomMedia: "Custom",
  Audio: "Audio",
  Podcast: "Podcast",
  Video: "Video",
};

function tvTypeLabel(t: TvType): string {
  return TV_TYPE_LABELS[t] ?? t;
}

interface SettingsViewProps {
  /** Notify parent that settings was interacted with (no-op for now). */
  onProviderToggle?: (name: string, enabled: boolean) => void;
}

/**
 * Settings tab — list of providers (from /api/providers), each with its
 * name, lang, supported types as badges, and a (visual-only) toggle.
 * Also shows app info / version.
 */
export function SettingsView({ onProviderToggle }: SettingsViewProps) {
  const { data, isLoading, isError, error } = useProviders();
  const providers = data?.providers ?? [];

  // Local toggle state — visual only.
  const [enabledMap, setEnabledMap] = React.useState<Record<string, boolean>>({});
  React.useEffect(() => {
    if (!providers.length) return;
    setEnabledMap((prev) => {
      const next = { ...prev };
      for (const p of providers) {
        if (!(p.name in next)) next[p.name] = p.enabled;
      }
      return next;
    });
  }, [providers]);

  const onToggle = (name: string, checked: boolean) => {
    setEnabledMap((prev) => ({ ...prev, [name]: checked }));
    onProviderToggle?.(name, checked);
  };

  return (
    <div className="flex flex-1 flex-col gap-8 px-3 pb-6 pt-4 sm:px-6">
      {/* Header */}
      <div className="flex items-center gap-3">
        <SettingsIcon className="size-6 text-primary" />
        <h1 className="text-2xl font-bold tracking-tight sm:text-3xl">
          Settings
        </h1>
      </div>

      {/* Providers */}
      <section className="flex flex-col gap-3">
        <div className="flex items-center justify-between">
          <h2 className="flex items-center gap-2 text-sm font-semibold tracking-tight text-muted-foreground">
            <Layers className="size-4" />
            Providers
          </h2>
          {providers.length > 0 && (
            <span className="text-xs text-muted-foreground">
              {providers.length} registered
            </span>
          )}
        </div>

        {isLoading && (
          <div className="flex flex-col gap-3">
            {Array.from({ length: 3 }).map((_, i) => (
              <div
                key={i}
                className="flex items-center justify-between gap-3 rounded-lg border border-border/50 bg-card/40 p-4"
              >
                <div className="flex flex-1 flex-col gap-2">
                  <Skeleton className="h-4 w-1/3" />
                  <Skeleton className="h-3 w-1/2" />
                  <div className="flex gap-1.5">
                    {Array.from({ length: 3 }).map((_, j) => (
                      <Skeleton key={j} className="h-4 w-12" />
                    ))}
                  </div>
                </div>
                <Skeleton className="h-5 w-9" />
              </div>
            ))}
          </div>
        )}

        {!isLoading && isError && (
          <div className="flex flex-col items-center justify-center gap-3 rounded-lg border border-dashed border-border/60 bg-card/30 px-6 py-12 text-center">
            <AlertCircle className="size-10 text-destructive" />
            <h3 className="text-lg font-semibold tracking-tight">
              Could not load providers
            </h3>
            <p className="max-w-md text-sm text-muted-foreground">
              {String(error?.message ?? "").slice(0, 200)}
            </p>
          </div>
        )}

        {!isLoading && !isError && providers.length === 0 && (
          <div className="flex flex-col items-center justify-center gap-3 rounded-lg border border-dashed border-border/60 bg-card/30 px-6 py-12 text-center">
            <Layers className="size-10 text-muted-foreground" />
            <h3 className="text-lg font-semibold tracking-tight">
              No providers registered
            </h3>
            <p className="max-w-md text-sm text-muted-foreground">
              The plugin loader could not find any providers. Make sure at
              least one plugin is enabled.
            </p>
          </div>
        )}

        {!isLoading && !isError && providers.length > 0 && (
          <ul className="flex flex-col gap-2">
            {providers.map((p) => (
              <ProviderRow
                key={p.name}
                info={p}
                enabled={enabledMap[p.name] ?? p.enabled}
                onToggle={(checked) => onToggle(p.name, checked)}
              />
            ))}
          </ul>
        )}
      </section>

      {/* About */}
      <section className="flex flex-col gap-3">
        <h2 className="flex items-center gap-2 text-sm font-semibold tracking-tight text-muted-foreground">
          <Info className="size-4" />
          About
        </h2>
        <div className="flex flex-col gap-3 rounded-lg border border-border/50 bg-card/40 p-4">
          <AboutRow icon={Tag} label="App" value="CloudStream — Linux Port" />
          <AboutRow icon={Cpu} label="Version" value={`v${APP_VERSION}`} />
          <AboutRow
            icon={Github}
            label="Source"
            value="Based on CloudStream 3 / 4"
          />
        </div>
      </section>
    </div>
  );
}

function ProviderRow({
  info,
  enabled,
  onToggle,
}: {
  info: ProviderInfo;
  enabled: boolean;
  onToggle: (checked: boolean) => void;
}) {
  return (
    <li className="flex items-center justify-between gap-3 rounded-lg border border-border/50 bg-card/40 p-4 transition-colors hover:border-border">
      <div className="flex min-w-0 flex-1 flex-col gap-2">
        <div className="flex items-center gap-2">
          <span className="line-clamp-1 text-sm font-semibold tracking-tight">
            {info.name}
          </span>
          <Badge
            variant={info.enabled ? "default" : "secondary"}
            className="text-[10px]"
          >
            {info.tier}
          </Badge>
          {info.providerType === "MetaProvider" && (
            <Badge variant="outline" className="text-[10px]">
              meta
            </Badge>
          )}
        </div>

        <div className="flex flex-wrap items-center gap-2 text-[11px] text-muted-foreground">
          <span className="inline-flex items-center gap-1">
            <Globe className="size-3" />
            {info.lang}
          </span>
          {info.mainUrl && (
            <span className="truncate opacity-70">{info.mainUrl}</span>
          )}
        </div>

        {info.supportedTypes.length > 0 && (
          <div className="flex flex-wrap gap-1.5">
            {info.supportedTypes.map((t) => (
              <Badge
                key={t}
                variant="secondary"
                className="text-[10px] font-medium"
              >
                {tvTypeLabel(t)}
              </Badge>
            ))}
          </div>
        )}

        <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-[10px] text-muted-foreground/70">
          <span>{info.hasMainPage ? "homepage" : "no homepage"}</span>
          <span>{info.hasQuickSearch ? "quicksearch" : "no quicksearch"}</span>
          <span>{info.hasDownloadSupport ? "downloads" : "no downloads"}</span>
          <span>{info.usesWebView ? "webview" : "no webview"}</span>
        </div>
      </div>

      <Switch checked={enabled} onCheckedChange={onToggle} aria-label={`Toggle ${info.name}`} />
    </li>
  );
}

function AboutRow({
  icon: Icon,
  label,
  value,
}: {
  icon: React.ComponentType<{ className?: string }>;
  label: string;
  value: string;
}) {
  return (
    <div className="flex items-center justify-between gap-3 text-sm">
      <span className="flex items-center gap-2 text-muted-foreground">
        <Icon className="size-4" />
        {label}
      </span>
      <span className="font-medium tracking-tight">{value}</span>
    </div>
  );
}
