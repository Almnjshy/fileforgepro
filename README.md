# FileForge Pro

**Professional Android File Manager** — a desktop-class file explorer built natively for Android phones, tablets, and large screens. Built with Kotlin, Jetpack Compose, Material 3, and a strict multi-module Clean Architecture.

> **Status:** Phase 1 (Core Architecture) + Phase 2 (Real Filesystem) + Phase 3 (File Explorer UI) implemented. Phases 4–18 are planned per the Master Specification.

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Module Map](#module-map)
4. [Build & Run](#build--run)
5. [Project Tree](#project-tree)
6. [Tech Stack](#tech-stack)
7. [Roadmap](#roadmap)
8. [License](#license)

---

## Overview

FileForge Pro brings the power and feel of a desktop file explorer (Windows Explorer / macOS Finder) to Android, while staying optimized for touch screens. It is NOT a demo and NOT a mock UI — every piece of file-system code talks to real Android storage via the proper abstractions (java.io, SAF, MediaStore).

The app is structured around the principle that **the File Browser knows nothing about where files come from or how they're displayed** — it only knows `StorageProvider` and `FileTypeRegistry`. This makes the codebase cleanly extensible: adding FTP, SMB, or a new file-type handler does not require touching the browser.

### Key features implemented (Phases 1–3)

- **Multi-module Gradle build** with 25+ modules (app, core:7, data, domain, engine, feature:16)
- **Domain layer** with pure-Kotlin models: `FPath`, `FFile`, `StorageSource`, `FileOperation`, `WindowSpec`, `ViewSettings`
- **StorageProvider abstraction** with `LocalFilesystemProvider` for Internal Storage + SD Card + USB OTG
- **FileRepository** delegating to providers via `StorageProviderRegistry`
- **Room database** for Favorites, Recent, Search History, Window State
- **DataStore** for view preferences (theme, sort, item size, show hidden)
- **Browser UI** with Toolbar (Back/Forward/Up/Refresh/Search/More) + clickable Breadcrumb
- **7 view modes**: Large Grid, Medium Grid, Small Grid, List, Compact List, Details, Thumbnail
- **Sorting**: Name / Size / Type / Modified, Asc / Desc, folders-first
- **Filtering** by type / extension / size / date / hidden / search query
- **Multi-selection** via long-press with selection-mode state
- **File-type detection** covering 100+ extensions (text, image, video, audio, archive, APK, PDF) + extensionless files (Dockerfile, Makefile, .gitignore, ...)
- **ThumbnailEngine** with two-tier cache (memory LRU + disk) for images and video frames
- **WindowManager** (in-app floating windows) — open/close/focus/minimize/maximize/move
- **Hilt DI** wired across all modules
- **Arabic + English** string resources with full RTL support
- **Material 3** theming with Light / Dark / System / AMOLED + dynamic color

---

## Architecture

```
┌────────────────────────────────────────────────────────────┐
│                          :app                              │
│   MainActivity, FileForgeApp, NavHost, Theme, DI modules   │
└──────────────────┬─────────────────────────────────────────┘
                   │
        ┌──────────┴──────────┐
        ▼                     ▼
┌──────────────┐      ┌────────────────┐
│  :feature:*  │      │   :engine      │
│  (16 modules)│      │  providers,    │
│  Compose UI  │      │  engines       │
│  ViewModels  │      │  (real I/O)    │
└──────┬───────┘      └────────┬───────┘
       │                       │
       ▼                       ▼
┌──────────────────────────────────────┐
│              :domain                 │
│   models, repository interfaces,     │
│   use cases (pure Kotlin)            │
└──────────────────┬───────────────────┘
                   │
       ┌───────────┴───────────┐
       ▼                       ▼
┌──────────────┐      ┌────────────────┐
│   :data      │      │    :core:*     │
│  Room, Data- │      │  common, fs,   │
│  Store, repo │      │  storage, perm,│
│  impls       │      │  security, nav │
└──────────────┘      └────────────────┘
```

### Layering rules

1. `:domain` is **pure Kotlin** — no Android dependency, no Hilt, no Room.
2. `:core:*` holds abstractions and shared infrastructure (no business logic).
3. `:data` implements `:domain` interfaces using Room + DataStore + StorageProvider.
4. `:engine` implements `:domain` interfaces for I/O-heavy operations (filesystem, thumbnails, archives, windows).
5. `:feature:*` only depend on `:domain` + `:core:ui`. They NEVER touch `:data` or `:engine` directly.
6. `:app` wires everything via Hilt.

### The three architectural invariants (Master Spec §81)

```
File Browser        UI
       │             │
       ▼             ▼
StorageProvider  ViewModel
       │             │
       ▼             ▼
Internal / USB /  UseCase
FTP / SMB / Cloud  │
                   ▼
              FileOperationEngine
                   │
                   ▼
              StorageProvider
```

```
File → FileTypeDetector → FileType → FileHandler (text/image/video/...)
```

---

## Module Map

| Module | Type | Responsibility |
|---|---|---|
| `:app` | Application | MainActivity, FileForgeApp, NavHost, Hilt modules, Manifest |
| `:core:common` | Lib | Result, FileError, Logger, FormatUtils, AppDispatchers |
| `:core:filesystem` | Lib | FileBridge, FileTypeDetector, MimeTypes |
| `:core:storage` | Lib | StorageProvider interface, StorageProviderRegistry |
| `:core:permissions` | Lib | PermissionManager, AppPermission, PermissionRequester |
| `:core:security` | Lib | AesGcmCrypto, FileHasher |
| `:core:navigation` | Lib | TopRoute, NavItem, NavItems |
| `:core:ui` | Compose Lib | Theme, Colors, Typography, FileTypeIcon, FolderIcon |
| `:domain` | Kotlin JVM | Models, repository interfaces |
| `:data` | Lib | Room database, DAOs, DataStore, repository impls |
| `:engine` | Lib | LocalFilesystemProvider, ThumbnailEngine, WindowManager, FileTypeRegistryImpl |
| `:feature:home` | Compose | Home dashboard |
| `:feature:browser` | Compose | File browser (toolbar, breadcrumb, 7 view modes, selection) |
| `:feature:windows` | Compose | Floating window host |
| `:feature:search` | Compose | Search (stub — Phase 7) |
| `:feature:recent` | Compose | Recent files (stub) |
| `:feature:favorites` | Compose | Favorites (stub) |
| `:feature:properties` | Compose | File properties (stub) |
| `:feature:storage` | Compose | Storage volumes (stub) |
| `:feature:analyzer` | Compose | Storage analyzer (stub — Phase 9) |
| `:feature:archive` | Compose | Archive manager (stub — Phase 12) |
| `:feature:media` | Compose | Media preview (stub — Phase 10) |
| `:feature:texteditor` | Compose | Text editor (stub — Phase 11) |
| `:feature:apk` | Compose | APK manager (stub — Phase 13) |
| `:feature:vault` | Compose | Secure vault (stub — Phase 15) |
| `:feature:network` | Compose | Network storage (stub — Phase 14) |
| `:feature:settings` | Compose | Settings |

---

## Build & Run

### Requirements

- **Android Studio** Hedgehog (2023.1.1) or newer
- **JDK 17** (bundled with Android Studio)
- **Android SDK** with:
  - Platform 34 (Android 14)
  - Build-Tools 34.0.0
  - Platform-Tools
- **Kotlin** 1.9.24 (bundled)
- **Gradle** 8.8 (wrapper included)

### Steps

1. Clone the repo:
   ```bash
   git clone <your-repo-url> FileForgePro
   cd FileForgePro
   ```

2. Open in Android Studio: **File → Open → select the `FileForgePro` folder**.

3. Wait for Gradle sync to complete (~3–5 minutes on first run).

4. Connect a device or start an emulator (Android 8.0+ / API 26+).

5. Click **Run** (Shift+F10) or build from CLI:
   ```bash
   ./gradlew assembleDebug
   # Output: app/build/outputs/apk/debug/app-debug.apk
   ```

6. Install manually if needed:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

### First-run permissions

On first launch the app opens on the Home dashboard. To browse files:

1. Tap any storage source → Android will prompt for `MANAGE_EXTERNAL_STORAGE`.
2. Tap **Grant** → Android opens the system "All files access" screen.
3. Toggle **FileForge Pro** on.
4. Return to the app — file listing will now work.

For Android 13+ (API 33+), media browsing uses `READ_MEDIA_IMAGES/VIDEO/AUDIO` instead.


## Project Tree

```
FileForgePro/
├── settings.gradle.kts              # 25 modules declared
├── build.gradle.kts                 # root build (plugin aliases only)
├── gradle.properties                # JVM args, AndroidX, caching
├── gradle/
│   ├── libs.versions.toml           # version catalog
│   └── wrapper/gradle-wrapper.properties
├── gradlew, gradlew.bat             # wrapper scripts
├── .gitignore
│
├── .github/
│   └── workflows/
│       └── build.yml                # build debug APK on push/PR
│
├── README.md
│
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/fileforge/pro/
│       │   ├── app/{FileForgeApp, MainActivity}.kt
│       │   ├── di/{App, Database, Repository}Module.kt
│       │   ├── ui/FileForgeApp.kt
│       │   └── navigation/FileForgeNavHost.kt
│       └── res/
│           ├── values/strings.xml, themes.xml, colors.xml
│           ├── values-ar/strings.xml
│           ├── drawable/ic_splash_logo.xml
│           ├── mipmap-*/ic_launcher*.xml
│           └── xml/{backup_rules, data_extraction_rules, file_paths}.xml
│
├── core/
│   ├── common/      # Result, FileError, Logger, FormatUtils, AppDispatchers, Releasable, LogTags
│   ├── filesystem/  # FileBridge, FileTypeDetector, MimeTypes
│   ├── storage/     # StorageProvider, StorageProviderRegistry, StorageVolumeInfo, StorageStats
│   ├── permissions/ # PermissionManager, AppPermission, PermissionRequester
│   ├── security/    # AesGcmCrypto, FileHasher
│   ├── navigation/  # TopRoute, NavItem, NavItems
│   └── ui/          # Theme, Colors, Typography, FileTypeIcon, FolderIcon
│
├── domain/
│   └── src/main/java/.../domain/
│       ├── model/      # FPath, FFile, StorageSource, FileType, ViewSettings, FileFilter, FileOperation, WindowSpec
│       └── repository/ # FileRepository, StorageSourceRepository, FavoritesRepository, RecentRepository, ViewSettingsRepository, SearchHistoryRepository, FileTypeRegistry, FileOperationRepository, SearchRepository, WindowManagerRepository
│
├── data/
│   └── src/main/java/.../data/
│       ├── database/   # FileForgeDatabase + entity/ + dao/
│       ├── preferences/# PreferencesModule
│       └── repository/ # FavoritesRepositoryImpl, RecentRepositoryImpl, SearchHistoryRepositoryImpl, ViewSettingsRepositoryImpl, FileRepositoryImpl, StorageSourceRepositoryImpl
│
├── engine/
│   └── src/main/java/.../engine/
│       ├── filesystem/    # LocalFilesystemProvider
│       ├── metadata/      # FileTypeRegistryImpl
│       ├── thumbnail/     # ThumbnailEngine
│       └── window/        # WindowManagerImpl
│
└── feature/
    ├── home/      # HomeViewModel + HomeScreen
    ├── browser/   # BrowserViewModel + BrowserScreen + components/{Breadcrumb, FileGridItem, FileListItem, FileDetailsRow}
    ├── windows/   # WindowViewModel + WindowHost
    ├── search/    # SearchScreen (stub)
    ├── recent/    # RecentScreen (stub)
    ├── favorites/ # FavoritesScreen (stub)
    ├── properties/# PropertiesScreen (stub)
    ├── storage/   # StorageScreen (stub)
    ├── analyzer/  # AnalyzerScreen (stub)
    ├── archive/   # ArchiveScreen (stub)
    ├── media/     # MediaScreen (stub)
    ├── texteditor/# TextEditorScreen (stub)
    ├── apk/       # ApkScreen (stub)
    ├── vault/     # VaultScreen (stub)
    ├── network/   # NetworkScreen (stub)
    └── settings/  # SettingsScreen (stub)
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 1.9.24 |
| UI | Jetpack Compose (BOM 2024.06), Material 3 |
| Async | Coroutines + Flow + StateFlow |
| DI | Hilt 2.51 |
| Persistence | Room 2.6 (KSP), DataStore Preferences 1.1 |
| Storage Access | SAF (DocumentFile), MediaStore, ContentResolver |
| Image loading | Coil 2.6 (compose + video + gif) |
| Archives | Apache Commons Compress 1.26, Zip4j 2.11 |
| Network | commons-net (FTP), jcifs (SMB), sardine (WebDAV) |
| Media | Media3 ExoPlayer 1.3 |
| PDF | PDFBox-Android 2.0.27 |
| Background | WorkManager 2.9 + Foreground Service |
| Navigation | Navigation Compose 2.7 |
| Splash | SplashScreen 1.0 |

### SDK targets

- `compileSdk = 34`
- `targetSdk = 34`
- `minSdk = 26` (Android 8.0 — ~97% device coverage)
- Java/Kotlin target: 17

---

## Roadmap

| Phase | Status | Scope |
|---|---|---|
| 0 — Audit | ✅ Done | Project audit report |
| 1 — Core Architecture | ✅ Done | Multi-module Gradle, domain/data/core/engine skeletons, Hilt |
| 2 — Real Filesystem | ✅ Done | LocalFilesystemProvider, FileRepository, StorageProviderRegistry |
| 3 — File Explorer UI | ✅ Done | Browser: toolbar, breadcrumb, 7 view modes, sort, filter, selection |
| 4 — File Operations | ⏳ Next | Copy/Move/Delete engine, progress UI, conflict manager, background service |
| 5 — Multi-window | ⏳ Planned | Floating windows with drag/resize, content rendering per type |
| 6 — Dual Pane | ⏳ Planned | Two browsers side-by-side, cross-pane D&D |
| 7 — Search | ⏳ Planned | Parsed query syntax, streaming results |
| 8 — Home Dashboard | Partial | Storage card + quick access + sources done; recent/favorites wiring TBD |
| 9 — Storage Tools | ⏳ Planned | Analyzer, large files, duplicate finder |
| 10 — Media | ⏳ Planned | Image/video/audio preview + thumbnail engine integration |
| 11 — Text System | ⏳ Planned | Editor, encoding, syntax highlighting, large-file streaming |
| 12 — Archives | ⏳ Planned | ZIP/TAR/GZIP/7Z via commons-compress + zip4j |
| 13 — APK | ⏳ Planned | APK inspector + PackageInstaller integration |
| 14 — Network | ⏳ Planned | FTP/SMB/WebDAV providers |
| 15 — Security | ⏳ Planned | Secure Vault with AES-256-GCM + biometric auth |
| 16 — Developer Tools | ⏳ Planned | Hidden files, permissions view, MIME, terminal integration |
| 17 — Polish | ⏳ Planned | Animations, a11y, RTL verification, performance pass |
| 18 — Final QA | ⏳ Planned | Full test matrix per Master Spec §74–75 |

---

## License

This project is proprietary. All rights reserved.
