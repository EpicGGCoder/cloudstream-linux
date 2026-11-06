/**
 * GET /api/search?q=<query>&page=1
 * Aggregated search across all enabled providers in parallel.
 * Mirrors CS3's SearchViewModel behavior.
 *
 * Response:
 *   { query, page, results: { apiName, items, hasNext }[] }
 */

import { NextRequest, NextResponse } from "next/server";
import { initPlugins } from "@/lib/plugins/loader";
import { registry } from "@/lib/plugins/registry";
import type { SearchResponse } from "@/lib/plugins/types";

export const dynamic = "force-dynamic";

interface ProviderResult {
  apiName: string;
  items: SearchResponse[];
  hasNext: boolean;
  error?: string;
}

export async function GET(req: NextRequest) {
  initPlugins();
  const sp = req.nextUrl.searchParams;
  const query = sp.get("q")?.trim() ?? "";
  const page = Number(sp.get("page") ?? "1");

  if (!query) return NextResponse.json({ query, page, results: [] });

  const providers = registry.listEnabled();
  const results: ProviderResult[] = [];

  await Promise.all(
    providers.map(async (provider) => {
      try {
        const r = await provider.search(query, page);
        results.push({
          apiName: provider.info.name,
          items: r?.items ?? [],
          hasNext: r?.hasNext ?? false,
        });
      } catch (e) {
        console.error(`[api/search] provider ${provider.info.name} failed:`, e);
        results.push({
          apiName: provider.info.name,
          items: [],
          hasNext: false,
          error: String(e),
        });
      }
    }),
  );

  return NextResponse.json({ query, page, results });
}
