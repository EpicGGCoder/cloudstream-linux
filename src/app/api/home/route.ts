/**
 * GET /api/home
 * Returns the aggregated home feed: all enabled providers' main pages,
 * fetched in parallel. Mirrors CS3's HomeViewModel behavior.
 *
 * Response:
 *   { rows: { id: string; name: string; provider: string; items: SearchResponse[] }[] }
 */

import { NextResponse } from "next/server";
import { initPlugins } from "@/lib/plugins/loader";
import { registry } from "@/lib/plugins/registry";
import type { SearchResponse } from "@/lib/plugins/types";

export const dynamic = "force-dynamic";
export const revalidate = 0;

interface HomeRow {
  id: string;
  name: string;
  provider: string;
  items: SearchResponse[];
}

interface MainPageSection {
  name: string;
  data: string;
  horizontalImages?: boolean;
}

/**
 * Each Provider wraps a MainAPI. The underlying MainAPI instance exposes
 * its `mainPage` config (a list of sections). We reach into the adapter
 * to read it so we know which sections to fetch.
 */
function getMainPageSections(provider: unknown): MainPageSection[] {
  // Provider wraps a MainAPI. The adapter exposes `api` internally;
  // we access the underlying MainAPI via a typed cast.
  const api = (provider as { api?: { mainPage?: MainPageSection[] } }).api;
  if (api?.mainPage && api.mainPage.length > 0) return api.mainPage;
  return [{ name: "Latest", data: "" }];
}

export async function GET() {
  initPlugins();
  const providers = registry.listEnabled().filter((p) => p.info.hasMainPage && p.getMainPage);

  const rows: HomeRow[] = [];

  await Promise.all(
    providers.map(async (provider) => {
      const info = provider.info;
      const sections = getMainPageSections(provider);
      try {
        await Promise.all(
          sections.map(async (section) => {
            const resp = await provider.getMainPage!(1, {
              name: section.name,
              data: section.data,
              horizontalImages: section.horizontalImages ?? false,
            });
            if (!resp) return;
            for (const list of resp.items) {
              rows.push({
                id: `${info.name}::${list.name}`,
                name: list.name,
                provider: info.name,
                items: list.list,
              });
            }
          }),
        );
      } catch (e) {
        console.error(`[api/home] provider ${info.name} failed:`, e);
      }
    }),
  );

  // Stable ordering: rows in the order providers were registered.
  return NextResponse.json({ rows });
}
