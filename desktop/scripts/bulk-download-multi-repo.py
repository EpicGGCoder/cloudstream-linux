#!/usr/bin/env python3
"""
Bulk download plugins from MULTIPLE CS3 repos (not just CNCVerse).
Tests the loader's robustness across the entire plugin ecosystem.

Picks a diverse set of repos from the official cs-repos/repos-db.json:
- recloudstream/extensions (official — Dailymotion, InternetArchive, Youtube, etc.)
- phisher98/cloudstream-extensions-phisher (popular, ~50+ plugins)
- Luna712 (anime focus)
- CakesTwix (Ukrainian)
- Gian-Fr (Italian)
- Bnyro (German)
- doGior (multilingual)
- redowan99
- KSHITIJ8473/raghav
- aslafy-z (catalog-stable)
- Eclipse (cs.eclipsia.dpdns.org)
- saimuelbr
- mouradchaouche (French)
- yorik100
- Reflex755

Downloads up to 5 plugins from each repo (to keep total manageable ~50-70 plugins).
"""

import json, os, subprocess, concurrent.futures, sys, time

PLUGINS_DIR = "/home/z/my-project/desktop/test-plugins-multi"
os.makedirs(PLUGINS_DIR, exist_ok=True)

# Curated list of repos to test (diverse languages/providers)
REPOS = [
    "https://raw.githubusercontent.com/recloudstream/extensions/master/repo.json",
    "https://raw.githubusercontent.com/phisher98/cloudstream-extensions-phisher/refs/heads/builds/repo.json",
    "https://raw.githubusercontent.com/Luna712/Luna712-CloudStream-Extensions/master/repo.json",
    "https://raw.githubusercontent.com/CakesTwix/cloudstream-extensions-uk/master/repo.json",
    "https://raw.githubusercontent.com/Gian-Fr/ItalianProvider/builds/repo.json",
    "https://raw.githubusercontent.com/Bnyro/GermanProviders/refs/heads/master/repo.json",
    "https://raw.githubusercontent.com/doGior/doGiorsHadEnough/refs/heads/builds/repo.json",
    "https://raw.githubusercontent.com/redowan99/Redowan-CloudStream/master/repo.json",
    "https://raw.githubusercontent.com/KSHITIJ8473/raghav/builds/repo.json",
    "https://github.com/aslafy-z/cloudstream-plugins/releases/download/catalog-stable/repo.json",
    "https://cs.eclipsia.dpdns.org/repo.json",
    "https://raw.githubusercontent.com/saimuelbr/saimuelrepo/refs/heads/main/builds/repo.json",
    "https://raw.githubusercontent.com/mouradchaouche/cloudstream-frenchrepo/main/repo.json",
    "https://raw.githubusercontent.com/yorik100/Cloudstream/refs/heads/builds/repo.json",
    "https://raw.githubusercontent.com/Reflex755/ReflexRepo/refs/heads/builds/repo.json",
]

MAX_PLUGINS_PER_REPO = 5

def fetch_repo(repo_url):
    """Fetch a repo JSON and return (repo_name, [plugins])."""
    try:
        result = subprocess.run(
            ["curl", "-sL", "--max-time", "30", repo_url],
            capture_output=True, text=True, timeout=45
        )
        if result.returncode != 0:
            return (repo_url, [], f"curl failed: {result.returncode}")
        repo = json.loads(result.stdout)
        name = repo.get("name", repo_url.split("/")[-1])
        plugins = []
        for list_url in repo.get("pluginLists", []):
            try:
                r = subprocess.run(
                    ["curl", "-sL", "--max-time", "30", list_url],
                    capture_output=True, text=True, timeout=45
                )
                if r.returncode == 0:
                    plugins.extend(json.loads(r.stdout))
            except Exception as e:
                print(f"  ! Failed to fetch plugin list {list_url}: {e}", file=sys.stderr)
        return (name, plugins, None)
    except Exception as e:
        return (repo_url, [], str(e))

def download_plugin(plugin, repo_name):
    """Download a single .cs3 file. Returns (name, cs3_path, success)."""
    name = plugin.get("name", "unknown")
    url = plugin.get("url", "")
    if not url:
        return (name, None, False)
    # Safe filename: repo_name__plugin_name.cs3
    safe_name = f"{repo_name}__{name}".replace(" ", "_").replace("/", "_")
    safe_name = "".join(c for c in safe_name if c.isalnum() or c in "._-")[:80]
    cs3_path = os.path.join(PLUGINS_DIR, f"{safe_name}.cs3")
    if not os.path.exists(cs3_path) or os.path.getsize(cs3_path) < 1000:
        try:
            subprocess.run(
                ["curl", "-sL", "--max-time", "60", url, "-o", cs3_path],
                check=False, timeout=90
            )
        except Exception:
            pass
    success = os.path.exists(cs3_path) and os.path.getsize(cs3_path) > 1000
    return (name, cs3_path, success)

print(f"=== Fetching {len(REPOS)} repos in parallel ===", file=sys.stderr)
all_plugins = []  # list of (repo_name, plugin)
with concurrent.futures.ThreadPoolExecutor(max_workers=8) as ex:
    results = list(ex.map(fetch_repo, REPOS))

total_plugins = 0
for repo_name, plugins, err in results:
    if err:
        print(f"  ✗ {repo_name}: {err}", file=sys.stderr)
        continue
    # Take first MAX_PLUGINS_PER_REPO
    plugins = plugins[:MAX_PLUGINS_PER_REPO]
    print(f"  ✓ {repo_name}: {len(plugins)} plugins", file=sys.stderr)
    for p in plugins:
        all_plugins.append((repo_name, p))
    total_plugins += len(plugins)

print(f"\n=== Downloading {total_plugins} plugins from {len(REPOS)} repos ===", file=sys.stderr)

def download_one(item):
    repo_name, plugin = item
    return download_plugin(plugin, repo_name)

with concurrent.futures.ThreadPoolExecutor(max_workers=8) as ex:
    dl_results = list(ex.map(download_one, all_plugins))

ok = sum(1 for _, _, s in dl_results if s)
print(f"\n=== Downloaded {ok}/{total_plugins} plugins ===", file=sys.stderr)
for name, path, success in dl_results:
    status = "✓" if success else "✗"
    print(f"  {status} {name}", file=sys.stderr)

print(f"\n=== Plugins saved to {PLUGINS_DIR} ===", file=sys.stderr)
