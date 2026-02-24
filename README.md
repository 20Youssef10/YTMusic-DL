# YtDlp Downloader — Android App

A full-featured, modern Android video/audio downloader powered by **yt-dlp** (1000+ supported sites), built with Jetpack Compose, MVVM architecture, and Material You theming.

---

## ✨ Features

| Feature | Status |
|---------|--------|
| 1000+ site support via yt-dlp | ✅ |
| Format selection (video/audio/codec/bitrate/HDR) | ✅ |
| Best/worst/custom format download | ✅ |
| Audio-only (MP3/M4A/OPUS/FLAC/AAC/WAV) | ✅ |
| Playlist & channel downloads with range | ✅ |
| Shorts/Reels/Live streams | ✅ |
| Subtitles (SRT/VTT) with language selection | ✅ |
| Embed metadata & thumbnail cover | ✅ |
| aria2c multi-connection downloader | ✅ |
| Download queue (pause/resume/cancel/retry/reorder) | ✅ |
| Speed limiting (per task or global) | ✅ |
| Download scheduling | ✅ |
| Background foreground service | ✅ |
| Auto-retry via WorkManager on network loss | ✅ |
| Download history + re-download | ✅ |
| URL auto-detection & paste detection | ✅ |
| Pre-download preview (thumbnail/duration/views) | ✅ |
| Custom command templates | ✅ |
| Cookies import (Netscape format / browser cookies) | ✅ |
| Geo-bypass & proxy/SOCKS5 | ✅ |
| SponsorBlock integration | ✅ |
| Per-task download log viewer | ✅ |
| In-app yt-dlp updates | ✅ |
| Dynamic Material You theming (light/dark/system) | ✅ |
| Share from browser/app directly into app | ✅ |
| Concurrent fragments (multi-threaded) | ✅ |
| RTL support | ✅ |

---

## 🏗️ Architecture

```
com.example.ytdlpdownloader/
├── app/                  # Application class, Hilt init
├── di/                   # Hilt modules (Database, Network)
├── data/
│   ├── local/            # Room database, DAOs, entities, type converters
│   ├── model/            # Data models (DownloadItem, VideoInfo, VideoFormat, DownloadConfig, ...)
│   └── repository/       # DownloadRepository, TemplateRepository
├── domain/
│   └── usecase/          # FetchVideoInfoUseCase, StartDownloadUseCase, CancelDownloadUseCase, ...
├── service/              # DownloadService (Foreground), BootReceiver
├── worker/               # DownloadWorker (WorkManager)
├── util/                 # YtDlpExecutor, PreferencesManager, Extensions, Constants
└── ui/
    ├── theme/            # Material3 Theme, Colors, Typography
    ├── navigation/       # Screen sealed class, NavGraph
    ├── home/             # HomeScreen + HomeViewModel
    ├── queue/            # QueueScreen + QueueViewModel
    ├── history/          # HistoryScreen + HistoryViewModel
    ├── settings/         # SettingsScreen + SettingsViewModel
    ├── log/              # LogScreen + LogViewModel
    └── components/       # Reusable composables (UrlInputField, FormatSelectionSheet, ...)
```

**Key patterns:**
- **MVVM** — ViewModels expose `StateFlow<UiState>`, UI observes via `collectAsStateWithLifecycle`
- **Clean Architecture** — Use Cases sit between UI and Repository
- **Repository Pattern** — Single truth source for data
- **Hilt DI** — Constructor injection everywhere
- **Room** — Persists download queue/history with type converters for serialized config
- **DataStore** — User preferences (settings)
- **WorkManager** — Ensures downloads survive network loss and reboots
- **Foreground Service** — Keeps downloads alive in background

---

## 🚀 Setup & Build

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 17
- NDK (for native libs in YoutubeDL-Boom)
- Android device/emulator API 24+

### Steps

1. **Clone the repo:**
   ```bash
   git clone https://github.com/yourname/ytdlp-downloader.git
   cd ytdlp-downloader
   ```

2. **Open in Android Studio:** `File → Open → select project root`

3. **Sync Gradle:** Wait for Gradle sync to complete (downloads ~200MB of deps including native ABI libs)

4. **Run:**
   - Select a device/emulator
   - Click **▶ Run** or press `Shift+F10`

5. **Grant permissions on first launch:**
   - Storage (API < 33) or Media Access (API 33+)
   - Post notifications (API 33+)
   - Foreground service starts automatically when a download is queued

### Build Release APK
```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/
```

---

## ⚙️ Configuration

All settings are accessible in-app via the **Settings** tab:

| Setting | Description |
|---------|-------------|
| Download Folder | Where files are saved (Scoped Storage) |
| Default Format | yt-dlp format string (e.g. `bestvideo+bestaudio/best`) |
| Concurrent Fragments | Number of parallel fragments (1–16) |
| Speed Limit | KB/s limit (0 = unlimited) |
| aria2c | Enable multi-connection external downloader |
| Proxy/SOCKS5 | Route downloads through proxy |
| Geo-bypass | Bypass region restrictions |
| Cookies File | Netscape cookies.txt for logged-in downloads |
| SponsorBlock | Auto-remove sponsor segments, intros, outros |
| Embed Thumbnail | Embed cover art in media files |
| Embed Metadata | Embed title, artist, album, chapters |
| Embed Subtitles | Download and embed subtitle tracks |
| Theme | Light / Dark / System with Material You dynamic colors |

---

## 📦 Dependencies

| Library | Purpose |
|---------|---------|
| `com.github.farimarwat:YoutubeDl-Boom` | yt-dlp Android bindings |
| `androidx.room` | Local DB for queue/history |
| `androidx.work:work-runtime-ktx` | Background retry/scheduling |
| `com.google.dagger:hilt-android` | Dependency injection |
| `androidx.datastore` | Settings persistence |
| `io.coil-kt:coil-compose` | Image loading (thumbnails) |
| `kotlinx-coroutines` | Async operations |
| `kotlinx-serialization` | JSON serialization for configs |
| `com.squareup.okhttp3:okhttp` | yt-dlp version check |

---

## 🧪 Testing

```bash
# Unit tests (JVM)
./gradlew test

# Instrumented tests (on device)
./gradlew connectedAndroidTest
```

Tests cover:
- `HomeViewModelTest` — URL validation, fetch states, audio mode toggling
- `UseCaseTests` — StartDownload, Cancel, ReDownload, FetchInfo use cases

---

## 🔒 Privacy & Security

- Cookies stored in user-selected file path only; never uploaded or cached in app storage
- No analytics or tracking
- All downloads happen locally via yt-dlp binary
- Scoped Storage used on API 29+ for saved files
- No READ_EXTERNAL_STORAGE needed on API 33+ (uses media APIs)

---

## 🤝 Contributing

1. Fork the repo
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit your changes
4. Open a Pull Request

---

## 📄 License

MIT License. See [LICENSE](LICENSE) for details.

---

## ⚠️ Legal Notice

This app is provided for legitimate personal use (downloading content you own or have rights to). Respect copyright law and terms of service of each platform. The developers are not responsible for misuse.
