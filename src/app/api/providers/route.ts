/**
 * GET /api/providers
 * Lists all registered providers (for settings UI + showing provider badges in search results).
 */

import { NextResponse } from "next/server";
import { initPlugins } from "@/lib/plugins/loader";
import { registry } from "@/lib/plugins/registry";

export const dynamic = "force-dynamic";

export async function GET() {
  initPlugins();
  return NextResponse.json({ providers: registry.list().map((p) => p.info) });
}
