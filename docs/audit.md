# Audit: nextcloud/news-android (reference)

> Read-only reference. We do **not** fork it. We reimplement the functionality in
> Kotlin against the News REST API v1.3 that ocnews already serves.
> Source audited: `nextcloud/news-android` @ `9146f6d` (master), package
> `de.luhmer.owncloudnewsreader`.

## 1. Source summary

| Field | Value |
|---|---|
| License | AGPL-3.0 (GitHub detection; README badge says "GPL v3", stale) |
| Language | Java dominant (~955 KB) + Kotlin (~35 KB) |
| Package | `de.luhmer.owncloudnewsreader` |
| Last release | `v0.9.9.97` (2026-11-02) |
| API target | **News API v1-2** (not v1-3) |
| DB | greenDAO 2.1.0 (not Room) |
| Network | Retrofit 3 + OkHttp 5 + Gson |
| Async | RxJava3 + EventBus |
| DI | Dagger 2 |
| Images | Glide 5 |

## 2. Feature inventory

Grouped by area, with the phase in which we plan to replicate it
(M0 = MVP, M1/M2/M3 = later). Reference locations are relative to
`News-Android-App/src/main/`.

### Screens

| Feature | Phase | Reference |
|---|---|---|
| Login (manual Basic auth + SSO) | M0 | `LoginDialogActivity.java` |
| Feed/folder list (expandable drawer) + article list | M0 | `NewsReaderListActivity.java`, `NewsReaderListFragment.java` |
| Article detail (reader) | M0 | `NewsDetailActivity.java`, `NewsDetailFragment.java`, `NewsReaderDetailFragment.java` |
| Image viewer dialog | M1 | `NewsDetailImageDialogFragment.java` |
| Add feed | M0 | `NewFeedActivity.java` |
| Add/rename/delete folder | M0 | `AddFolderDialogFragment.java`, `FolderOptionsDialogFragment.java` |
| Feed options (rename/move/delete) | M0 | `NewsReaderListDialogFragment.java` |
| OPML import (local parse) | M0 | `NewFeedActivity.ImportOpmlSubscriptionsTask` + `helper/OpmlXmlParser.java` |
| OPML export | M1 | `OpmlXmlParser.GenerateOPML` |
| Podcasts (list + playback) | M2 | `PodcastFragment.java`, `PodcastFragmentActivity.java` |
| Settings | M0 | `SettingsActivity.java`, `SettingsFragment.java` |
| PiP video playback | M3 | `PiPVideoPlaybackActivity.java` |

### Services

| Feature | Phase | Reference |
|---|---|---|
| Sync engine (initial + incremental) | M0 | `authentication/OwnCloudSyncAdapter.java` |
| State push (read/unread/star) | M0 | `services/SyncItemStateService.java` + `reader/nextcloud/ItemStateSync.java` |
| Offline image download | M3 | `services/DownloadImagesService.java` |
| Offline webpage download | M3 | `services/DownloadWebPageService.java` |
| Podcast playback (MediaBrowser) | M2 | `services/podcast/PlaybackService.java` |
| TTS article reading | M3 | `services/podcast/TTSPlaybackService.java` |
| Podcast download | M2 | `services/PodcastDownloadService.java` |

### Other

| Feature | Phase | Reference |
|---|---|---|
| Home-screen widget | M3 | `widget/WidgetProvider.java`, `widget/WidgetNewsViewsFactory.java` |
| Unread notification with actions | M2 | `notification/NextcloudNotificationManager.java` |
| Chrome Custom Tabs (open original) | M0 | `chrometabs/` |
| Android Auto (podcast media) | M3 | `res/xml/automotive_app_desc.xml` |
| Self-signed cert handling (MemorizingTrustManager) | M1 | `ssl/` |

## 3. Client-server sync flow (reference, v1-2)

### Auth / login

- Manual: fields URL + username + password, stored in SharedPreferences, Basic auth.
- Validates by `GET /index.php/apps/news/api/v1-2/version`. Error if version == "0";
  HTTP 302 → "News app not installed".
- SSO: delegates to Nextcloud Files app via `Android-SingleSignOn`.

### Initial sync (first run, `OwnCloudSyncAdapter.sync()`)

1. In parallel: `GET /folders` + `GET /feeds` + push pending state.
2. Folders deduped by label (keeps highest id).
3. Insert folders + feeds into local DB.
4. Unread download loop: `GET /items?batchSize=200&offset=0&type=3&id=0&getRead=false&oldestFirst=true`,
   offset updated with highest item id each page, repeat while page size == 200.
5. Starred loop: `GET /items?type=2&getRead=true&oldestFirst=true`, offset from max id.

### Incremental sync

- `GET /items/updated?lastModified=<ts>&type=3&id=0`, streamed, parsed manually,
  root key `"items"`. Inserts in blocks of 100.

### State push (`ItemStateSync`)

- Local DB marks pending changes in `read_temp` / `starred_temp`.
- Batches of 300:
  - read → `PUT /items/read/multiple` body `{"items":[ids]}`
  - unread → `PUT /items/unread/multiple`
  - starred → `PUT /items/star/multiple` body `{"items":[{"feedId":X,"guidHash":"..."}]}` (**v1-2 uses feedId+guidHash for star**)
  - unstarred → `PUT /items/unstar/multiple`
- Temp flags only consolidated to `read`/`starred` after a 2xx.

### Cadence

- Settings: 0 / 5 / 15 / 30 / 45 min / 1..24 h, default **15 min** via
  `ContentResolver.addPeriodicSync`.

## 4. Local data model (reference)

greenDAO entities (→ our Room equivalents):

| Reference entity | Table | Key fields |
|---|---|---|
| `RssItem` | `RSS_ITEM` | id, feedId, link, title, body, read, starred, author, guid, guidHash, fingerprint, read_temp, starred_temp, lastModified, pubDate, enclosureLink, enclosureMime, mediaThumbnail, mediaDescription, rtl |
| `Feed` | `FEED` | id, folderId (0 = root), feedTitle, faviconUrl, link, avgColour, notificationChannel, openIn |
| `Folder` | `FOLDER` | id, label |
| `CurrentRssItemView` | `CURRENT_RSS_ITEM_VIEW` | id, rssItemId |

## 5. v1-2 → v1-3 differences (critical)

ocnews implements v1.3 (`internal/api/server.go`). These are the deltas that
change what our client sends:

1. **Base path**: `/index.php/apps/news/api/v1-3/` (reference uses v1-2).
2. **Star/unstar multiple**: v1.3 accepts **plain item IDs**
   (`{"items":[ids]}` or `{"itemIds":[ids]}`) for *all four* operations
   (read/unread/star/unstar). The reference's `ItemMap` (feedId+guidHash) for
   star/unstar is a v1-2 artifact we must NOT copy.
3. **Methods**: v1.3 accepts both POST and PUT for `/items/*/multiple`,
   `/feeds/{id}/move`, `/feeds/{id}/rename` (ocnews registers both; the spec
   self-contradicts). We standardize on PUT (what the reference uses).
4. **`/items/updated`**: v1.3 forces `getRead=true` and `batchSize=-1`
   (server-side), so no pagination needed on updated.
5. **`batchSize=-1`** supported on `/items` (all items in one response); the
   reference paginates in 200-chunks. We can use `-1` against ocnews.
6. **`reportedVersion = "28.4.0"`** — clients gate features on it; per-feed
   keyword filters require >= 28.4.0.
7. **Extras present in ocnews but not used by the reference client**: search
   (`/items/search`), per-feed filters/rules/retention/scraper/credentials,
   full-text extraction (`/items/{id}/full`), server OPML import/export,
   saved searches, auto-read rules, shares.

## 6. Quirks to preserve vs discard

Preserve (still relevant):
- OPML import is **local** (parse the file, create folders/feeds via API), not
  a server upload. ocnews also has `POST /import/opml`, but the local flow is
  what Android clients do and is more robust.
- Avatar: `/index.php/avatar/{id}/64` (in ocnews/OpenCloud this 404s; degrade
  gracefully to initials).
- `oldestFirst=true` + `getRead=false` for first unread load; `getRead=true`
  for starred.
- Offline-first: temp flags for pending state, consolidated after server 2xx.

Discard (not needed / over-engineering for our MVP):
- greenDAO → Room.
- RxJava3/EventBus → Coroutines + Flow.
- Dagger → Hilt (or manual DI for a small app).
- SSO against Nextcloud Files → not applicable to OpenCloud; app-token only
  (with optional basic user:password fallback).
- The folder label dedup quirk (legacy bug).
- `version == "0"` / "302 = not installed" heuristics are mostly for old
  servers; keep the 302 hint only.
