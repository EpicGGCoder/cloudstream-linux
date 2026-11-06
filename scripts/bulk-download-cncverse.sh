#!/usr/bin/env bash
# Bulk download all CNCVerse plugins + convert each via dex2jar.
# Produces a directory of .cs3 + .jar pairs ready for the batch test.

set -euo pipefail

PLUGINS_DIR="/home/z/my-project/desktop/test-plugins"
mkdir -p "$PLUGINS_DIR"
cd "$PLUGINS_DIR"

# Read plugin list from CNCVerse repo
echo "=== Fetching CNCVerse plugin list ==="
curl -sL --max-time 30 "https://raw.githubusercontent.com/NivinCNC/CNCVerse-Cloud-Stream-Extension/builds/plugins.json" \
  -o /tmp/cncverse-plugins.json

# Parse and download each plugin in parallel (max 8 concurrent)
python3 <<'PYEOF'
import json, os, subprocess, concurrent.futures

with open("/tmp/cncverse-plugins.json") as f:
    plugins = json.load(f)

plugins_dir = "/home/z/my-project/desktop/test-plugins"

def fetch(plugin):
    name = plugin["name"]
    url = plugin["url"]
    safe_name = name.replace(" ", "_")
    cs3_path = os.path.join(plugins_dir, f"{safe_name}.cs3")
    if not os.path.exists(cs3_path):
        subprocess.run(["curl", "-sL", "--max-time", "60", url, "-o", cs3_path],
                       check=False, timeout=90)
    return name, safe_name, cs3_path, os.path.exists(cs3_path) and os.path.getsize(cs3_path) > 1000

print(f"Downloading {len(plugins)} plugins in parallel (max 8 concurrent)...")
with concurrent.futures.ThreadPoolExecutor(max_workers=8) as ex:
    results = list(ex.map(fetch, plugins))

ok = sum(1 for _, _, _, r in results if r)
print(f"Downloaded {ok}/{len(plugins)} plugins")
for name, safe, path, success in results:
    status = "✓" if success else "✗"
    print(f"  {status} {name} → {safe}.cs3")
PYEOF

echo
echo "=== Converting each .cs3 → .jar via dex2jar ==="
DEX_TOOLS="/home/z/my-project/desktop/app/resources/dex-tools/d2j-dex2jar.sh"

ok=0
fail=0
for cs3 in *.cs3; do
    [[ -f "$cs3" ]] || continue
    base="${cs3%.cs3}"
    jar="${base}.jar"
    if [[ -f "$jar" && "$jar" -nt "$cs3" ]]; then
        ok=$((ok+1))
        continue
    fi
    # Extract classes.dex
    rm -f classes.dex
    unzip -qo "$cs3" classes.dex 2>/dev/null || {
        echo "  ✗ $base (no classes.dex)"
        fail=$((fail+1))
        continue
    }
    # Convert
    if bash "$DEX_TOOLS" -f -o "$jar" classes.dex > /dev/null 2>&1; then
        ok=$((ok+1))
        echo "  ✓ $base"
    else
        fail=$((fail+1))
        echo "  ✗ $base (dex2jar failed)"
    fi
done

rm -f classes.dex
echo
echo "=== Summary: $ok converted, $fail failed ==="
ls *.cs3 2>/dev/null | wc -l
echo "plugins total"
