# Changelog

All notable changes to ocnews-android are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/).

## [0.1.40] - 2026-11-09

### Added

- Download articles offline: fetches the full text of every unread article from the server and stores it locally, so articles can be read without a connection.
- Download images: prefetches the hero image of every unread article into the disk cache.
- Both actions live in the top bar menu, show progress while running and summarise what was downloaded when they finish.

### Notes

- The full text is kept when a later sync replaces the item.
- Room schema v3 adds the fullBody column, migrated in place.

## [0.1.38] - 2026-11-09

### Fixed

- Every way of closing a menu now plays the same collapse animation and leaves the screen behind untouched: a tap outside, the system back gesture, and a tap on the menu card outside its entries.
- Back no longer reaches the screen behind while a menu is open, so it cannot close the reader or navigate away.

## [0.1.36] - 2026-11-09

### Fixed

- Tapping outside an open menu no longer activates anything underneath it: taps outside only close the menu. This affected the top bar menu, the reader menu and the choice rows in Settings.
- Menus now collapse with a fade and scale animation instead of disappearing.

## [0.1.34] - 2026-11-09

### Changed

- The reader header shows the feed favicon before the author and keeps the publication time on the same line, aligned to the right.

## [0.1.32] - 2026-11-09

### Changed

- The list, the unread view and the starred view read a lightweight projection without the article HTML, and the thumbnail is precomputed at sync time, so marking an article read or starred no longer reloads every article body.
- Body search now runs in SQLite and returns only matching ids.
- The reader loads the full article on demand, one page at a time.

### Fixed

- Opening a long article no longer freezes the reader: the HTML is parsed off the main thread and paragraphs are laid out lazily.

### Upgrade notes

- Room schema v2 adds a `thumbnailUrl` column. Existing rows are migrated in place and their thumbnails are backfilled in the background on first launch.

## [0.1.30] - 2026-11-08

### Changed

- The reader now renders articles natively with Compose instead of a WebView. The body keeps bold, italic and links, and the first image is shown as a hero.

### Fixed

- Swipe between articles is smooth and responds to fling: swipe left goes to the next article, swipe right to the previous one, even with a short flick.
- Vertical scrolling never switches article and no longer fights the horizontal gesture.
- Removed the flicker caused by recreating the WebView on every page.
- The reader top bar, title, meta and body follow the theme, including dark mode.

## [0.1.28] - 2026-11-08

### Fixed

- A quick flick now advances to the next or previous article using the fling velocity, instead of requiring a long drag; a slow short drag still snaps to the nearest page.

## [0.1.26] - 2026-11-08

### Fixed

- Reader swipe is now axis-locked: it follows the finger live but only on clearly horizontal drags, so scrolling vertically never switches article.
- The reader top bar darkens with the theme (surface background plus theme text/icon colors).
- Reduced WebView flicker with a hardware layer.

## [0.1.24] - 2026-11-08

### Fixed

- Smooth swipe between articles: the reader uses the native pager again (the article follows the finger and snaps) instead of a jump-on-release detector that felt stuck; vertical drags still scroll the article.

## [0.1.22] - 2026-11-08

### Changed

- The reader now respects the dark theme (dark background and text in the WebView).
- The top-bar title is the All / Unread articles toggle; the duplicated toggle button is gone.
- Drawer is more compact (smaller fonts, less vertical space); About moved to Settings.
- Settings General now has New feed, New folder and Import OPML.

## [0.1.20] - 2026-11-08

### Changed

- Top bar: add-feed, refresh and logout moved into a three-dot profile menu; the extra +, sync glyph and avatar are gone.
- Drawer: 80% width, smaller fonts; Import OPML and Log out moved out.
- List: smaller title font, square 80dp thumbnails, configurable swipe actions, optional titles-only list.
- Import OPML moved to Settings.

### Added

- Settings screen mirroring Nextcloud News: General (sync on start, only unread, swipe right/left action, sort, search in), Display (theme, feed list, text size), Data & sync (sync interval, clear cache), About (license, report, contribute, version).
- Mark all as read.
- Log out now asks for confirmation.

## [0.1.18] - 2026-11-08

### Changed

- Reader: the hero image now sits below the title/author/time.
- Home list: each article shows a thumbnail on the right (same height as the row), and swipe left marks it read while swipe right stars it.
- The star and read buttons change clearly: starred shows a gold filled star, read shows a filled check versus an empty circle when unread.

### Fixed

- The reader no longer switches articles when you scroll down (the pager now only reacts to deliberate horizontal swipes).

## [0.1.16] - 2026-11-08

### Added

- Fetch full content in the reader: the overflow menu extracts the full article text via the backend (readability) even when the RSS feed only carries a summary.

### Changed

- The reader is now a single scroll: hero image, title, author/time and body scroll together, giving the article the full screen height.
- The floating share button is replaced by "Open in browser" (share stays in the top bar).

### Fixed

- Reader top bar was drawn under the status bar, so the overflow menu was unreachable.
- Out-of-memory crash when swiping between articles: each page's WebView is now destroyed on release (they were leaking native memory).

## [0.1.14] - 2026-11-08

### Added

- Search: filter the article list by title or author from the top bar.
- Swipe between articles in the reader (left/right to move to the next/previous one).

### Fixed

- Out-of-memory crash on large feeds: unread and starred counts now use `SELECT COUNT(*)` instead of loading full items (with their HTML bodies).
- No more crash when a sync fails (e.g. a transient HTTP 502); the app logs it and keeps the cached list.

## [0.1.12] - 2026-11-08

### Added

- Redesigned drawer: leading icons and trailing counts (unread per feed/folder, total unread, starred), clickable folders, bottom actions (Import OPML, Settings, About, Log out) and a user avatar in the top bar.
- Redesigned reader: single top bar with the source feed name, share and overflow, a hero image at the top, and a floating action bar (star, read, share).
- System back in the reader returns to the article list.

### Fixed

- Article images now load: the ocnews `/img?u=` proxy returns 403, so the original CDN image URL is unpacked from the `u` parameter (and the body's image srcs are rewritten) for both the hero image and inline images.

## [0.1.10] - 2026-11-08

### Added

- Spanish (es) localization for all UI strings, error messages and relative time (#10).

### Fixed

- The navigation drawer now has a proper surface background instead of rendering transparent over the article list (#8).
- Feed favicons now load: server-relative favicon URLs are resolved against the server (#9).

## [0.1.8] - 2026-11-08

### Fixed

- Login fields (server URL, username, app token) no longer autocorrect or auto-capitalize, so typing an app token (a space-separated passphrase) or the case-sensitive username no longer causes a spurious "Invalid username or app token".

## [0.1.6] - 2026-11-08

### Fixed

- Incremental sync now refreshes folders and feeds, so changes made from another client reach the app (#1).
- `renameFolder` now updates the local folder state (#2).

### Security

- The app token is encrypted at rest with AES-256-GCM in the Android Keystore instead of plaintext DataStore, with a one-time migration of the legacy plaintext token (#3).

## [0.1.4] - 2026-11-08

### Added

- App launcher icon (Lucide "newspaper" on OpenCloud `#E2BAFF` background): adaptive icon + legacy PNGs.

## [0.1.2] - 2026-11-08

First functional release of the native Android client for ocnews.

### Added

- Login with server URL + username + OpenCloud app token (Basic auth).
- Full News API v1.3 sync: initial (folders/feeds/unread/starred), incremental
  (`/items/updated` watermark) and optimistic state push (read/unread/star/unstar
  with batching).
- Feed drawer with folders, feeds and favicons (Coil).
- Article list with relative time and thumbnails; only-unread toggle.
- Article reader (WebView) with star/read actions and "open original".
- Add feed with discover fallback (422 -> multi-select).
- OPML import (local parse -> create folders/feeds).
- Settings: theme (system/light/dark) and sync interval (WorkManager).
- Offline-first local storage (Room) and periodic background sync.
- 46 unit tests covering the sync engine, repository, parsers and ViewModels.
