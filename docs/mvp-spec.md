# MVP spec — ocnews-android

Native Kotlin client for the News REST API v1.3 served by ocnews. No fork:
original implementation, reference is `nextcloud/news-android` (see
[audit.md](audit.md)).

## 1. Goal

A usable RSS reader on Android against ocnews: log in with an OpenCloud app
token, sync feeds/folders/items, read articles, manage read/starred state,
subscribe and import OPML. Offline-capable for already-synced content.

## 2. Non-goals (MVP)

- Podcasts (list + playback). → M2
- Offline image / webpage download. → M3
- Home-screen widget. → M3
- TTS reading. → M3
- Android Auto. → M3
- Single sign-on (Nextcloud Files app). OpenCloud auth is app-token only.
- Per-feed filters / rules / retention / scraper UI. (Backend supports them;
  expose later.)

## 3. Scope (MVP feature list)

1. Login: server URL + username + app token (Basic auth). Validate via
   `GET /version`. Store credentials securely.
2. Initial sync: folders, feeds, unread items, starred items.
3. Incremental sync: `/items/updated` watermark polling.
4. Article list with three scopes (All / Starred / per-feed), only-unread toggle.
5. Article reader: render HTML body, open original in Custom Tabs.
6. State: mark read/unread, star/unstar, batch push to server.
7. Feed management: add (with discover fallback), rename, move, delete.
8. Folder management: create, rename, delete.
9. OPML import (local parse → create folders/feeds via API).
10. Settings: sync interval, theme (system/light/dark), only-unread default,
    clear local data.
11. Basic unread-count badges on folders/feeds.

## 4. Architecture

- **Language/UI**: Kotlin + Jetpack Compose (Material 3), single-activity +
  Navigation Compose.
- **Pattern**: MVVM. UI → ViewModel (StateFlow) → Repository → DataSource
  (Room + remote). Unidirectional data flow.
- **DI**: Hilt (small graph; if it adds friction, manual DI via an AppContainer).
- **Async**: Coroutines + Flow. Repository is the single concurrency boundary.
- **Network**: Retrofit + OkHttp + kotlinx.serialization (or Moshi). Basic-auth
  interceptor.
- **Local DB**: Room (WAL). Entities mirror the server model (see §6).
- **Background sync**: WorkManager (periodic 15 min, battery/network aware) +
  on-demand refresh.
- **Image loading**: Coil (favicons + thumbnails). Degrade to initials.

Package: `app.ocnews` (tentative, matches `app.revela` convention; easy to
change before first build).

### Module layout (single module is enough for MVP)

```
app/
  src/main/java/app/ocnews/
    di/            # Hilt modules
    data/
      remote/      # Retrofit API + DTOs
      local/       # Room entities, DAOs, Database
      repo/        # Repository implementations
    domain/        # models, sync engine (pure)
    ui/
      login/
      list/        # drawer + article list
      detail/      # reader
      addfeed/
      opml/
      settings/
      theme/
    sync/          # WorkManager workers + sync logic
```

## 5. API contract (endpoints used in MVP)

Base: `/index.php/apps/news/api/v1-3`. Auth: `Authorization: Basic base64(user:app-token)`.
Server replies 401 with `WWW-Authenticate: Basic realm="ocnews"` on bad creds.

| Endpoint | Method | Payload / query | Used for |
|---|---|---|---|
| `/version` | GET | — → `{"version":"28.4.0"}` | login validation |
| `/user` | GET | — | display name |
| `/folders` | GET / POST | POST `{"name":...}` | list / create folder |
| `/folders/{id}` | PUT / DELETE | PUT `{"name":...}` | rename / delete folder |
| `/folders/{id}/read` | POST | — | mark folder read |
| `/feeds` | GET / POST | POST `{"url":...,"folderId":...}` (also form) | list / add feed |
| `/feeds/discover` | GET | `?url=` | resolve feed URL from page |
| `/feeds/{id}/move` | PUT | `{"folderId":N}` (0 = root) | move feed |
| `/feeds/{id}/rename` | PUT | `{"feedTitle":...}` | rename feed |
| `/feeds/{id}` | DELETE | — | unsubscribe |
| `/items` | GET | `?type=0/1/2/3&id=&getRead=&batchSize=&offset=&oldestFirst=` | list items |
| `/items/updated` | GET | `?lastModified=&type=3&id=0` (server forces getRead=true, batchSize=-1) | incremental |
| `/items/{id}/read` | POST | — | single read |
| `/items/{id}/unread` | POST | — | single unread |
| `/items/{id}/star` | POST | — | single star |
| `/items/{id}/unstar` | POST | — | single unstar |
| `/items/read/multiple` | PUT | `{"items":[ids]}` | batch read |
| `/items/unread/multiple` | PUT | `{"items":[ids]}` | batch unread |
| `/items/star/multiple` | PUT | `{"items":[ids]}` | batch star |
| `/items/unstar/multiple` | PUT | `{"items":[ids]}` | batch unstar |
| `/items/read` | POST | — | mark all read |

`type`: 0=feed, 1=folder, 2=starred, 3=all. `id` required for type 0/1.
`offset` is an inclusive item-id cursor (keyset). `batchSize=-1` returns all.
For star/unstar **multiple**, send plain item IDs (v1.3). Do NOT send the
v1-2 `feedId+guidHash` shape.

App-specific (ocnews): `POST /refresh` (manual refresh of all feeds).

## 6. Data model (Room)

```
Folder(id: Long, label: String)
Feed(id: Long, folderId: Long /*0=root*/, title: String, faviconUrl: String?, link: String?)
Item(
  id: Long, feedId: Long,
  title: String, body: String?, author: String?,
  link: String, guid: String?, guidHash: String, fingerprint: String?,
  read: Boolean, starred: Boolean,
  readPending: Boolean, starredPending: Boolean,   // outbound queue
  lastModified: Long, pubDate: Long?,
  enclosureLink: String?, enclosureMime: String?, mediaThumbnail: String?
)
Prefs/watermark: lastSyncModified: Long (in DataStore)
```

Indexes: `Item(feedId)`, `Item(read, starred)`, `Item(lastModified)`.

## 7. Sync engine

### Initial sync (first run after login)

1. `GET /folders` + `GET /feeds` (parallel). Upsert.
2. Unread: `GET /items?type=3&getRead=false&batchSize=-1&oldestFirst=true`. Upsert.
3. Starred: `GET /items?type=2&getRead=true&batchSize=-1&oldestFirst=true`. Upsert.
4. Set `lastSyncModified` = max `lastModified` seen.

### Incremental sync

`GET /items/updated?lastModified=<watermark>&type=3&id=0`. Upsert returned items,
advance watermark to max `lastModified`. No pagination needed (server returns all).

### State push (read/unread/star/unstar)

- UI writes `readPending` / `starredPending` immediately (optimistic) and flips
  the effective `read`/`starred` for display.
- A worker drains pending flags in batches of ≤300, `PUT /items/{op}/multiple`
  with plain IDs. On 2xx, clear the pending flag. On failure, keep for retry
  (exponential backoff).

### Scheduling

- WorkManager periodic worker, default **15 min**, network + battery constraints.
- Manual refresh from the list (and on first launch).

## 8. Screens / navigation

1. **Login**: server URL, username, app token, "Sign in". Errors: 401 (bad
   creds), 302/"News not installed" hint, network.
2. **Feed list** (nav rail / drawer): Starred, All articles, folders (expandable)
   → feeds, unread badges.
3. **Article list**: cards (title, relative time, feed, thumbnail), only-unread
   toggle, tap → reader. Pull-to-refresh.
4. **Reader**: title, author, rendered body, star + read/unread actions, "open
   original" (Custom Tabs).
5. **Add feed**: URL + optional folder; 422 → discover fallback (multi-select).
6. **OPML import**: pick file → parse → create folders/feeds, progress + result.
7. **Settings**: sync interval, theme, only-unread default, clear local data,
   about.

## 9. Security

- App token stored in EncryptedSharedPreferences (or DataStore + Android Keystore).
- No token in logs, no token in the URL. Basic auth via OkHttp interceptor.
- Self-signed cert support deferred to M1 (reference uses MemorizingTrustManager;
  we'll evaluate a pin/allow-once flow then).

## 10. Acceptance criteria (MVP done)

- [ ] Login against a real ocnews instance (app token) succeeds; bad token → 401.
- [ ] Initial sync pulls folders/feeds/unread/starred; unread counts correct.
- [ ] Incremental sync picks up new items after a manual refresh.
- [ ] Read/unread and star/unstar optimistically update and reach the server
      (verify via `/items?getRead=true` after push).
- [ ] Add feed (URL + folder), rename/move/delete feed and folder work.
- [ ] OPML import of a sample file creates the expected folders/feeds.
- [ ] Article body renders; "open original" opens Custom Tabs.
- [ ] Offline: previously synced list/body readable with no network.
- [ ] Unit tests: sync logic (watermark, batching), repository state transitions.

## 11. Later phases

- **M1**: image viewer, OPML export, self-signed cert handling, per-feed filters
  UI (backend ready), search.
- **M2**: podcasts (list + playback + download), unread notification with actions.
- **M3**: widgets, offline image/webpage download, TTS, Android Auto, PiP video.
