# CloudStream Linux Port — Master Plan

> **Goal**: A faithful, native-feeling Linux desktop port of CloudStream, packaged as an `.AppImage`, that supports real `.cs3` plugins natively.
>
> **Status**: Phase 1 in progress. See `ARCHITECTURE.md` for technical analysis.

This is a **multi-session engineering project**. We ship in phases. Each phase produces something usable.

---

## Honest Constraints (read these first)

These are not opinions — they are facts about CloudStream's design that constrain any port:

1. **`.cs3` plugins are Dalvik DEX bytecode.** They cannot run on a stock JVM without either (a) recompiling plugin source to JVM bytecode, or (b) running an Android runtime. This is the single biggest porting obstacle.
2. **`WebViewResolver.jvm.kt` is `TODO("Not yet implemented")`** in upstream. Cloudflare/DdosGuard-protected providers will not work without a real WebView (headless Chromium).
3. **Upstream's own `desktopApp/` is a 62-line "Hello World"** — there is no upstream desktop port to inherit.
4. **ExoPlayer/Chromecast/WorkManager/biometric** are all Android-only. Each needs a Linux replacement.

This sandbox (Next.js 16) **cannot compile Kotlin or build AppImages**. So our strategy is:

- **Here**: Build the web-based UI, plugin engine, persistence layer, sync providers, meta-providers (TMDB), player, scraper utilities — a faithful re-implementation in TypeScript that mirrors the CS3 architecture. This is **the development harness** — fully usable as a web app, and ready to be wrapped by Tauri for the AppImage.
- **On your laptop (later)**: A small **JVM sidecar** (built from upstream `:library` + a thin RPC server) that loads real `.cs3` plugins and exposes their `search`/`load`/`loadLinks` over stdio JSON-RPC. The web UI talks to this sidecar for plugin calls. This is what gives "real plugin support."
- **Final**: The Tauri bundle (web UI + bundled JVM sidecar) is packaged with `appimagetool` → your `CloudStream.AppImage`.

---

## Phased Roadmap

### Phase 1 — Web foundation (current)
**Deliverable**: A usable Next.js web app that mirrors CS3's UX: home feed, search, detail page, player, library. Uses TMDB + a TypeScript plugin system that mirrors the CS3 `MainAPI` contract.

| Step | Description | Status |
|---|---|---|
| 1A | Prisma schema mirroring CS3 `DataStoreHelper` keys | pending |
| 1B | TypeScript plugin engine — `MainAPI` contract mirror + registry + `.ts` plugin loader | pending |
| 1C | TMDB API client + backend route handlers | pending |
| 1D | Home page UI (hero carousel + horizontal rows) | pending |
| 1E | Search page (cross-provider aggregated results) | pending |
| 1F | Detail page (seasons/episodes) | pending |
| 1G | Player page (hls.js, source picker, subtitles) | pending |
| 1H | Library (favorites/history/subscriptions) + settings skeleton | pending |

**Demo content**: TMDB metadata + a bundled TypeScript provider for public-domain content (Internet Archive). The plugin system is left open — you'll be able to drop in your own TS providers later, or use the JVM sidecar for real `.cs3` plugins.

### Phase 2 — Native shell + PWA (next session)
- Tauri wrapper config (`src-tauri/`) — bundles the Next.js static export with a tiny Rust binary → `.deb` + `.AppImage` skeleton.
- PWA manifest + service worker → installable as a desktop app even without Tauri.
- Plugin repo manager UI (add GitHub repo URL, browse plugins, install).
- Subtitle online search (OpenSubtitles).
- Video skip APIs (AniSkip, IntroDB).
- Backup/restore JSON.

### Phase 3 — JVM sidecar (your laptop)
- A small Kotlin/JVM project (`jvm-sidecar/`) depending on upstream `:library`.
- Thin JSON-RPC server (stdio or local socket) exposing: `plugin.list`, `plugin.search`, `plugin.load`, `plugin.loadLinks`, `plugin.getMainPage`, `repo.add`, `repo.list`, `plugin.install`.
- Uses `URLClassLoader` to load `.cs3` plugins compiled to JVM bytecode (we'll provide a build script for popular plugins).
- Bundled into AppImage via Tauri's `resources` + JRE (jlink-customized JRE ~30MB).

### Phase 4 — AppImage final (your laptop)
- Tauri `appimagetool` integration.
- Desktop entry, icon, MIME types (`x-scheme-handler/cloudstream`).
- Auto-update via GitHub releases.
- Release pipeline: `tauri build` → `.AppImage` artifact.

---

## Plugin strategy

**Two plugin tiers**, both supported simultaneously:

1. **TypeScript plugins** (`.ts` files in `plugins/` or fetched from a JS repo URL)
   - Mirror the CS3 `MainAPI` contract: `name`, `mainUrl`, `lang`, `supportedTypes`, `getMainPage`, `search`, `quickSearch`, `load`, `loadLinks`.
   - Loaded at runtime by the web app via dynamic `import()`.
   - Sandbox via `vm2`-style isolation (or accept the trust model — same as CS3).
   - Used for: simple HTML scraping providers, public-domain catalogs, custom integrations.

2. **Kotlin `.cs3` plugins** (via JVM sidecar)
   - Real CS3 plugins, unmodified.
   - Loaded into the JVM sidecar via `URLClassLoader`.
   - Sidecar proxy: every provider call from the web UI → JSON-RPC → sidecar → real plugin → JSON back.
   - Used for: any existing CS3 plugin you want to keep.

The web UI doesn't care which tier a provider is on — the registry abstracts both behind the same `Provider` interface.

---

## What I will NOT do

Per CS3's `AI-POLICY.md` and standard ethics:
- I will not write scrapers for specific piracy streaming sites.
- I will not bundle or distribute copyrighted content.
- The plugin system is open — what you put in it is your responsibility.
- All demo content uses legal sources: TMDB metadata, Internet Archive public-domain media.

---

## How to use this plan

- Each session, I'll work on the next pending step(s).
- After each step, I verify with Agent Browser.
- You can run `bun run dev` locally and click around the Preview Panel to see live progress.
- When Phase 2 is ready, we set up Tauri on your laptop.
- When Phase 3 is ready, we set up the JVM sidecar on your laptop.
- When Phase 4 is ready, you have your `CloudStream.AppImage`.

See `ARCHITECTURE.md` for the technical deep-dive on CS3's design and how each subsystem maps to the port.
