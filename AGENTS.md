# AGENTS.md

Guidance for AI coding agents (and human contributors) working on this project.

## Project overview

An Android e-book reader app named **BookAppp** (`com.bookappp.app`), written in Kotlin. It is inspired by the general feature set of reader apps like Apple Books, but it is an original app with its own name, icon, UI design, and branding.

**Current status:** MVP features through build-order step 11 are implemented. The optional step 12 extras and release preparation remain.

**Goals**
- Open and read DRM-free EPUB and PDF files
- Library screen with covers and reading progress
- Reader customization: font, text size, theme (day/night/sepia), margins, brightness
- Table of contents, in-book search, bookmarks, highlights, and notes
- Optional later: text-to-speech, cross-device sync, audiobooks

**Non-goals**
- Do not copy Apple's code, icons, branding, wording, or exact visual design.
- Do not attempt to open FairPlay or other DRM-protected books.
- Do not build a book store in the MVP.

## Tech stack

| Area | Choice |
|---|---|
| Language | Kotlin (version pinned in `gradle/libs.versions.toml`) |
| UI | Jetpack Compose + Material 3 |
| EPUB/PDF engine | Readium Kotlin Toolkit 3.4.0 (pinned; check migration notes before upgrading) |
| Local database | Room |
| Preferences | DataStore (Preferences) |
| Async | Kotlin Coroutines + Flow |
| DI | Hilt |
| Images | Coil 3 (book covers) |
| Navigation | Navigation Compose |
| Tests | JUnit, MockK, kotlinx-coroutines-test, Compose UI tests |

Notes:
- Readium's reader/navigator is built on Android Views and Fragments. Embed it in Compose with `AndroidView` or `AndroidViewBinding` / a `FragmentContainerView`. Do not try to rewrite the renderer in pure Compose.
- Always check the current Readium docs and migration notes before writing integration code. Its APIs change between major versions, so do not rely on memory.

## Suggested project structure

```
app/
  src/main/java/com/bookappp/app/
    MainActivity.kt
    App.kt                      # Application class, DI setup
    data/
      db/                       # Room entities, DAOs, database
      repository/               # LibraryRepository, ReaderRepository, SettingsRepository
      files/                    # import, storage, cover extraction
    di/                         # Hilt modules
    domain/                     # models and use cases
    ui/
      theme/                    # colors, typography, shapes, glass tokens
      navigation/               # NavHost and routes
      library/                  # LibraryScreen + LibraryViewModel
      reader/                   # ReaderScreen, ViewModels, Readium EPUB/PDF glue
      settings/                 # app settings hub
      help/ legal/ licenses/    # offline support and legal screens
  src/test/                     # unit tests
  src/androidTest/              # instrumented / UI tests
```

## Architecture rules

- **MVVM + unidirectional data flow.** Composables render state from a ViewModel and send events back up.
- Each screen has a `UiState` data class exposed as `StateFlow`. Composables must not hold business logic.
- ViewModels talk to repositories, never directly to Room DAOs or Readium internals when avoidable.
- Repositories expose `Flow` for observable data and `suspend` functions for one-shot work.
- All disk, database, and parsing work runs off the main thread (`Dispatchers.IO`). Never block the UI thread opening a book.
- Use dependency injection for repositories, DAOs, and dispatchers so they can be faked in tests.
- Keep Readium-specific code isolated under `ui/reader` and `data/files` so the rest of the app doesn't depend on it.

## Data model (starting point)

- `Book`: id, title, author, file URI/path, format (EPUB/PDF), cover path, added date, last opened date
- `ReadingProgress`: bookId, locator (serialized Readium `Locator` JSON), percent complete, updated at
- `Bookmark`: id, bookId, locator, label, created at
- `Highlight`: id, bookId, locator, color, note (optional), created at
- Preferences (DataStore): theme, font family, font size, margins, line spacing, brightness override

Store locators as Readium's JSON representation so positions survive re-pagination when font size changes.

## Build order (follow this sequence)

1. Create a Compose project; get it running on a device/emulator.
2. Add dependencies; confirm Readium builds. Run Readium's test app for reference.
3. File picker (Storage Access Framework) to import an EPUB; copy it into app storage (or persist URI permission).
4. Open the book in Readium's navigator inside a `ReaderScreen`.
5. Library screen: cover grid, title/author, last-read progress, backed by Room.
6. Save and restore reading position.
7. Reader settings: font, size, theme, margins, persisted in DataStore.
8. Table of contents, search, bookmarks.
9. Highlights and notes.
10. PDF support.
11. Polish: empty states, errors, accessibility, tablet/landscape layouts.
12. Extras: TTS, sync, audiobooks.

Steps 1–11 are implemented. Step 11 was exercised on an emulator; physical-device, foldable, and full spoken-TalkBack passes remain release QA. Step 12 and release preparation also remain. Finish and verify each step before starting the next. Prefer small, working increments over large unfinished changes.

## Commands

Run from the project root:

```bash
./gradlew assembleDebug             # build debug APK
./gradlew installDebug              # install on connected device/emulator
./gradlew test                      # unit tests
./gradlew connectedDebugAndroidTest # instrumented tests (needs a device/emulator)
./gradlew lint                      # Android lint
./gradlew assembleRelease           # minified unsigned release APK
```

CI runs `./gradlew assembleDebug test assembleDebugAndroidTest lint`. No `ktlint` task is currently configured; Android lint is the configured static-analysis check.

Before declaring a task done, make sure the project builds, `./gradlew test` passes, and lint is clean.

## Code style

- Follow the official Kotlin style guide and Android Kotlin style guide.
- Prefer immutable data (`val`, `data class`, `List` over `MutableList` in public APIs).
- Composables: PascalCase, take a `Modifier` parameter, hoist state, keep them small and previewable. Add `@Preview` for non-trivial UI.
- No hard-coded user-facing strings. Put them in `strings.xml`.
- No hard-coded colors or dimensions in composables. Use the theme.
- Handle null and error cases explicitly. Corrupt or unsupported books must show a friendly error, not crash.
- Keep functions short and names descriptive. Add comments for the "why", not the "what".

## Testing expectations

- Unit test ViewModels, repositories, and any parsing/formatting logic.
- Use fakes for repositories in ViewModel tests.
- Add Compose UI tests for the library and reader-settings flows.
- Keep a few small, legally redistributable sample EPUB/PDF files in `app/src/androidTest/assets/` or `src/test/resources/`. Use public-domain books (e.g. from Project Gutenberg).

## Performance and UX

- Load covers lazily and cache them; never decode full-size images in the list.
- Persist reading position on pause/stop and on a debounce while reading.
- Support dark mode, dynamic font scaling, and TalkBack labels for all controls.
- Support configuration changes (rotation) without losing the reading position.
- Test on a small phone, a large phone, and a tablet or foldable.

## Security and privacy

- Request the minimum permissions. Use the Storage Access Framework instead of broad storage access.
- The app currently has no `INTERNET` permission. Do not add network access without a documented feature and a privacy-policy update.
- Do not log book contents, file paths of user documents, or personal data.
- If sync or analytics are added, document what is collected and add a privacy policy before publishing.
- Never commit API keys, keystores, or `google-services.json` with real credentials. Use `local.properties` or CI secrets.

## Licensing and legal

- Readium is BSD-3-Clause. Keep required license notices in the app's open-source licenses screen.
- Check the license of every dependency or reference project (Librera, KOReader, FBReader, etc.) before reusing any code. Many are GPL/AGPL and are not compatible with closed distribution.
- Use original name, icon, and artwork. Do not use Apple's or Google's logos or trademarks.
- Only bundle content you have the right to distribute.

## Git workflow

- Small, focused commits with clear messages (imperative mood, e.g. "Add library grid screen").
- One feature or fix per branch; open a PR for review even if solo.
- Do not commit build outputs, `.idea/` user files, or `local.properties`.

## Instructions for AI agents

- Read this file and the existing code before making changes. Match existing patterns.
- Make the smallest change that completes the task. Don't refactor unrelated code.
- Verify Readium and Jetpack APIs against current documentation rather than guessing; if unsure, say so.
- Run the build and tests after changes and report the results honestly, including failures.
- Ask before adding a new dependency, changing the architecture, or altering the data model.
- Never remove or weaken tests to make a build pass.
- If a task conflicts with the legal/licensing section above, stop and flag it.