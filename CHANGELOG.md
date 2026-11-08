# Changelog

All notable changes to ocnews-android are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/).

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
