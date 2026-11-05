/**
 * Plugin loader — discovers and registers built-in TypeScript plugins.
 *
 * Phase 3 will add a JSON-RPC bridge to the JVM sidecar for .cs3 plugins.
 */

import { registry } from "./registry";
import type { ProviderInfo } from "./types";

// Built-in TS providers — imported eagerly so they're always available.
// (Phase 3 will add dynamic .cs3 plugin loading via the JVM sidecar.)
import { archiveProvider } from "@/plugins/archive";
import { tmdbMetaProvider } from "@/plugins/tmdb-meta";

let initialized = false;

export function initPlugins(): void {
  if (initialized) return;
  initialized = true;

  // Register built-in providers.
  registry.register(tmdbMetaProvider);
  registry.register(archiveProvider);

  console.info(
    `[plugins] initialized ${registry.list().length} provider(s):`,
    registry.list().map((p) => p.info.name).join(", "),
  );
}

export function listProviders(): ProviderInfo[] {
  return registry.list().map((p) => p.info);
}
