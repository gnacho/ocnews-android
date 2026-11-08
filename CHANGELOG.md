# Changelog

All notable changes to ocnews-android are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/).

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
