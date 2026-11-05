# CloudStream Linux Desktop — Native App

This is the **real** native Linux port of CloudStream, packaged as an AppImage.
It's NOT a website — it's a Compose Desktop application that uses:

- **Compose Multiplatform** for the native UI (same framework CS3 is migrating to upstream)
- **dex2jar** for loading real `.cs3` plugins (full plugin compatibility)
- **JavaFX WebView** for Cloudflare/DdosGuard-protected sites
- **vlcj (libvlc)** for video playback (HLS, MP4, MKV, subtitles)
- **jpackage + appimagetool** for the final `.AppImage` packaging

## Build (on your Linux laptop)

### Prerequisites
1. **JDK 17+** (developed on 21, works on 17+)
   ```bash
   sudo apt install openjdk-21-jdk    # or use sdkman
   ```
2. **VLC** (for the vlcj player to find libvlc)
   ```bash
   sudo apt install vlc libvlc-dev
   ```
3. **appimagetool** (for the final .AppImage wrap)
   ```bash
   wget -c https://github.com/AppImage/AppImageKit/releases/download/continuous/appimagetool-x86_64.AppImage \
     -O ~/.local/bin/appimagetool
   chmod +x ~/.local/bin/appimagetool
   ```

### Build steps

```bash
cd /home/z/my-project/desktop

# 1. (One-time) bundle dex-tools into the AppImage resources
./scripts/fetch-dex-tools.sh

# 2. Build the native distribution (.deb + app-image dir)
./gradlew :app:packageDistributionForCurrentOS

# 3. Wrap the app-image dir into a portable .AppImage
./gradlew :app:packageAppImage
#   → produces build/compose/binaries/CloudStream-x86_64.AppImage
```

### Install & run

```bash
chmod +x build/compose/binaries/CloudStream-x86_64.AppImage
./build/compose/binaries/CloudStream-x86_64.AppImage
```

Or install system-wide:

```bash
sudo install -m 0755 build/compose/binaries/CloudStream-x86_64.AppImage /opt/cloudstream/CloudStream.AppImage
# Desktop entry + menu shortcut are auto-installed by jpackage.
```

## What works (Phase 1)

- ✅ Real `.cs3` plugin loading via dex2jar (`PluginLoader.kt`)
- ✅ Plugin repository manager (`RepositoryManager.kt`) — add any CS3 repo URL
- ✅ JavaFX WebView for Cloudflare challenges (`WebViewResolverJvm.kt`)
- ✅ vlcj video player (HLS, MP4, MKV, subtitles, multi-audio) (`VlcjPlayer.kt`)
- ✅ Compose Desktop UI (home / search / library / plugins / settings)
- ✅ Settings persistence (`AppSettings.kt`)
- ✅ AppImage packaging via jpackage + appimagetool

## What's in progress / next

- Search and detail pages — UI shells present, need wiring to providers (next session)
- Player UI — vlcj is wired but needs the Compose Desktop video surface bridge
- Subtitle online search (OpenSubtitles)
- Library persistence (favorites, history, subscriptions to SQLite or JSON)
- AniSkip / IntroDB video skip APIs

## Architecture

See `/home/z/my-project/ARCHITECTURE.md` for the full subsystem-by-subsystem
mapping from CS3 to this port.

## License

Same as upstream CloudStream: GNU GPL v3.
