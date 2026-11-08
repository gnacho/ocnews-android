# ocnews-android

![version](https://img.shields.io/badge/version-0.1.10-blue)

Native Android client for [ocnews](https://github.com/gnacho/ocnews), the RSS reader for OpenCloud.

Kotlin + Jetpack Compose + MVVM. It talks to the [Nextcloud News REST API v1.3](https://nextcloud.github.io/news/api/api-v1-3/) that ocnews implements, so it works against ocnews and any Nextcloud News server.

## Features

- Log in with your server URL, username and app token.
- Offline-first sync: folders, feeds and articles, with incremental updates and
  optimistic read/star state pushed back to the server.
- Feed drawer with folders and favicons.
- Article list with relative time and thumbnails; only-unread toggle.
- Reader with star/read actions and "open original" in the browser.
- Add feed (with discover fallback), OPML import.
- Settings: theme (system/light/dark) and sync interval.
- Background periodic sync via WorkManager.

## Build

Requires JDK 17 and the Android SDK (compileSdk 37).

```
./gradlew :app:assembleDebug
```

## License

AGPL-3.0. See [LICENSE](LICENSE).
