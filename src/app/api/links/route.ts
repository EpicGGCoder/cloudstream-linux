/**
 * POST /api/links
 * Body: { apiName: string; data: string; isCasting?: boolean }
 * Returns playable links + subtitles for an episode or movie.
 * Mirrors CS3's RepoLinkGenerator + 20-min cache.
 *
 * Response:
 *   { apiName, data, links: ExtractorLink[], subtitles: SubtitleFile[] }
 */

import { NextRequest, NextResponse } from "next/server";
import { initPlugins } from "@/lib/plugins/loader";
import { registry } from "@/lib/plugins/registry";

export const dynamic = "force-dynamic";

// 20-minute TTL cache, mirroring CS3's RepoLinkGenerator.cache.
interface CachedLinks {
  links: unknown[];
  subtitles: unknown[];
  fetchedAt: number;
}
const CACHE_TTL_MS = 20 * 60 * 1000;
const linkCache = new Map<string, CachedLinks>();

export async function POST(req: NextRequest) {
  initPlugins();
  const body = (await req.json()) as { apiName: string; data: string; isCasting?: boolean };
  const { apiName, data, isCasting = false } = body;

  if (!apiName || !data) {
    return NextResponse.json({ error: "apiName and data are required" }, { status: 400 });
  }

  const cacheKey = `${apiName}::${data}`;
  const cached = linkCache.get(cacheKey);
  if (cached && Date.now() - cached.fetchedAt < CACHE_TTL_MS) {
    return NextResponse.json({
      apiName,
      data,
      links: cached.links,
      subtitles: cached.subtitles,
      cached: true,
    });
  }

  const provider = registry.get(apiName);
  if (!provider) {
    return NextResponse.json({ error: `Provider "${apiName}" not found` }, { status: 404 });
  }

  try {
    const { links, subtitles } = await provider.loadLinks(data, isCasting);
    linkCache.set(cacheKey, { links, subtitles, fetchedAt: Date.now() });
    return NextResponse.json({ apiName, data, links, subtitles, cached: false });
  } catch (e) {
    console.error(`[api/links] loadLinks failed for ${apiName}:${data}`, e);
    return NextResponse.json({ error: String(e) }, { status: 500 });
  }
}
