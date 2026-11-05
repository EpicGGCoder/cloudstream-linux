/**
 * GET /api/detail?apiName=<name>&url=<url>
 * Loads a title's detail page (episodes, metadata, etc.) from a provider.
 * Mirrors CS3's ResultViewModel2.load().
 *
 * Response:
 *   { apiName, url, loadResponse: LoadResponse | null }
 */

import { NextRequest, NextResponse } from "next/server";
import { initPlugins } from "@/lib/plugins/loader";
import { registry } from "@/lib/plugins/registry";

export const dynamic = "force-dynamic";

export async function GET(req: NextRequest) {
  initPlugins();
  const sp = req.nextUrl.searchParams;
  const apiName = sp.get("apiName") ?? "";
  const url = sp.get("url") ?? "";

  if (!apiName || !url) {
    return NextResponse.json({ error: "apiName and url are required" }, { status: 400 });
  }

  const provider = registry.get(apiName);
  if (!provider) {
    return NextResponse.json({ error: `Provider "${apiName}" not found` }, { status: 404 });
  }

  try {
    const loadResponse = await provider.load(url);
    return NextResponse.json({ apiName, url, loadResponse });
  } catch (e) {
    console.error(`[api/detail] load failed for ${apiName}:${url}`, e);
    return NextResponse.json({ error: String(e) }, { status: 500 });
  }
}
