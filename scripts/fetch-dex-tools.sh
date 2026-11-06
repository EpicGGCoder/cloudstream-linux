#!/usr/bin/env bash
# Downloads dex-tools (the dex2jar distribution) and extracts it into
# app/resources/dex-tools/ so it gets bundled into the AppImage.

set -euo pipefail

DEX_TOOLS_VERSION="v10.2-r4"
DEX_TOOLS_URL="https://github.com/ThexXTURBOX3/dex2jar/releases/download/${DEX_TOOLS_VERSION}/dex-tools-${DEX_TOOLS_VERSION}.zip"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TARGET_DIR="${SCRIPT_DIR}/../app/resources/dex-tools"
TEMP_ZIP="$(mktemp -d)/dex-tools.zip"

echo "Downloading dex-tools ${DEX_TOOLS_VERSION}..."
curl -sL "${DEX_TOOLS_URL}" -o "${TEMP_ZIP}"

echo "Extracting to ${TARGET_DIR}..."
rm -rf "${TARGET_DIR}"
mkdir -p "${TARGET_DIR}"
unzip -q "${TEMP_ZIP}" -d "${TARGET_DIR}"

shopt -s dotglob
for sub in "${TARGET_DIR}"/*/; do
  if [[ -d "${sub}" ]]; then
    mv "${sub}"* "${TARGET_DIR}/"
    rmdir "${sub}"
  fi
done

chmod +x "${TARGET_DIR}"/d2j-*.sh 2>/dev/null || true
rm -f "${TEMP_ZIP}"
echo "✓ dex-tools installed at ${TARGET_DIR}"
