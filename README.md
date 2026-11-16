# BookAppp

BookAppp is a local-first Android e-book reader for DRM-free EPUB and PDF files. It is an original app with its own name, icon, interface, and branding.

> No account, no cloud, no bookstore. Books and reading data stay on the device.

**Status:** MVP, version `0.2.0`. Core features through roadmap step 11 are implemented. Text-to-speech, cross-device sync, audiobooks, and store release preparation are not yet implemented.

## Features

### Library

- Import EPUB and PDF files with the Android Storage Access Framework.
- Keep a private copy of each book, so the original file can be moved or deleted.
- Cover grid with title, author, and reading progress.
- Search and sort by recently added, title, author, or recently opened.
- Remove a book together with its app-owned copy and reading data.

### Reading

- EPUB 2/3 and PDF support through the Readium Kotlin Toolkit.
- Save and restore the reading position, including after process death.
- Table of contents with chapter search and jump-to-percentage.
- In-book search for EPUB.
- Bookmarks with jump-back navigation.
- EPUB text selection, four highlight colors, notes, and a highlights list.
- Pull past the start or end of an EPUB chapter to move to the previous or next chapter.
- PDF continuous-scroll and paginated layouts, with whole-page or page-width fitting.
- Rotation and configuration changes keep the reading position.

### Reader settings

- Day, Sepia, and Night page themes.
- Bundled font choices, including OpenDyslexic and AccessibleDfA.
- Text size, margins, and line spacing.
- Manual brightness override or follow-system brightness.
- Theme-aware glass-style reader chrome and system-bar icon contrast.
- Settings are stored in DataStore and applied to the live book.

### Interface

- Jetpack Compose and Material 3.
- Responsive phone, tablet, and landscape layouts.
- Dynamic color on Android 12+.
- Content descriptions and TalkBack labels on interactive controls.
- Empty states and friendly errors for unsupported, unreadable, or damaged books.

## Supported content

| Format | Support |
|---|---|
| EPUB 2 | Yes, DRM-free |
| EPUB 3 | Yes, DRM-free |
| PDF | Yes, DRM-free |

Not supported: DRM-protected books (including FairPlay, Adobe ADEPT, and Kindle DRM), MOBI, AZW3, KFX, audiobooks, and comics. BookAppp is a reader, not a bookstore.

## Tech stack

| Area | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| EPUB/PDF engine | Readium Kotlin Toolkit `3.4.0` |
| Local database | Room |
| Preferences | DataStore Preferences |
| Async | Coroutines + Flow |
| Dependency injection | Hilt |
| Covers | Coil 3 |
| Navigation | Navigation Compose |
| Tests | JUnit, MockK, kotlinx-coroutines-test, Compose UI tests |

Dependency versions are managed in [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

## Requirements

- JDK 17
- Android Studio with support for Android Gradle Plugin `9.4.1`
- Android SDK 37
- A device or emulator running Android 8.0 (API 26) or newer

Create `local.properties` with your SDK path; it is intentionally not committed:

```properties
sdk.dir=/path/to/Android/Sdk
```

## Build and run

From the repository root:

```bash
./gradlew assembleDebug          # build the debug APK
./gradlew installDebug           # install on a connected device/emulator
./gradlew test                   # JVM unit tests
./gradlew connectedDebugAndroidTest  # instrumented tests (device/emulator)
./gradlew lint                   # Android lint
./gradlew assembleRelease        # minified release build
```

The release APK is unsigned and is written to `app/build/outputs/apk/release/`. Add a signing configuration before distributing it.

Continuous integration runs:

```bash
./gradlew assembleDebug test assembleDebugAndroidTest lint --console=plain
```

No `ktlint` task is currently configured; Android lint is the configured static-analysis check.

## Architecture

BookAppp uses MVVM and unidirectional data flow. Screens expose a `UiState` as `StateFlow`, ViewModels call repositories, and repositories own Room, DataStore, and file access. Disk, database, and parsing work runs off the main thread.

Readium is isolated to `ui/reader` and `data/files`. Its navigators are Android Views and Fragments embedded in Compose through a `FragmentContainerView`; the renderer is not reimplemented in Compose.

```text
app/src/main/java/com/bookappp/app/
  App.kt
  MainActivity.kt
  data/
    db/                 Room entities, DAOs, database
    files/              import, storage, cover extraction
    repository/         library, reader, settings repositories
  di/                   Hilt modules
  domain/               models and use cases
  ui/
    theme/              colors, typography, glass tokens
    navigation/         NavHost and routes
    library/            library screen and ViewModel
    reader/             Compose chrome plus Readium EPUB/PDF glue
    settings/           app settings hub
    help/ legal/ licenses/
  src/test/             JVM unit tests
  src/androidTest/      Room and Compose UI tests
```

Reading locations are stored as serialized Readium `Locator` JSON so positions survive re-pagination when typography changes.

## Testing

- JVM tests cover ViewModels, repositories, preference mapping, and other pure logic.
- Instrumented tests cover the Room migration, library components, and reader-settings controls.
- Add small, legally redistributable EPUB/PDF fixtures for broader end-to-end tests. Do not commit books you do not have the right to distribute.

For detailed engineering rules, see [`AGENTS.md`](AGENTS.md).

## Privacy and permissions

- The app requests no internet permission and makes no network requests.
- Books are chosen through the Storage Access Framework, then copied into app-private storage.
- Book contents, library data, reading positions, bookmarks, highlights, notes, and settings stay on the device.
- There are no ads, analytics, or tracking SDKs.
- Android system backup may include app data when backup is enabled; that is controlled by the device's Android/Google settings.

## Licensing

The repository does not currently include a license file for the application source. Add one before distributing the source or accepting external contributions.

Third-party components keep their own licenses. Readium is BSD-3-Clause, bundled fonts are under the SIL Open Font License, and AndroidX/Jetpack components are Apache-2.0. Required notices and full license texts are available in the app under **Settings > Open-source licenses**.

## Roadmap

Completed:

- Compose app and Readium integration
- Library import, covers, search, sort, progress, and deletion
- EPUB and PDF readers
- Reading-position persistence and restoration
- Reader themes, fonts, typography, brightness, and PDF layout controls
- Table of contents, search, bookmarks, highlights, and notes
- Emulator QA for process death, rotation, large-font, dark-mode, and responsive layouts

Remaining:

- Add redistributable EPUB/PDF test fixtures
- Optional step 12: text-to-speech, cross-device sync, audiobooks
- Release preparation: signing, versioning, store listing, final privacy review
- True backdrop blur for the reader chrome, if a reliable WebView sampling approach is chosen

## Contributing

1. Read [`AGENTS.md`](AGENTS.md) before changing code.
2. Keep changes small and focused.
3. Run `./gradlew test lint` and build the app before opening a pull request.
4. Do not commit `local.properties`, build outputs, keystores, API keys, or real `google-services.json` credentials.
5. Use original artwork and only bundle content you have the right to distribute.
