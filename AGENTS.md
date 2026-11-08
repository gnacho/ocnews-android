# AGENTS.md — ocnews-android

Cliente Android nativo de [ocnews](https://github.com/gnacho/ocnews) (lector RSS para OpenCloud). Kotlin + Jetpack Compose + MVVM, sin fork del cliente Nextcloud: implementación propia contra la News REST API v1.3.

## Qué es
- App Android que consume la News API v1.3 que sirve el backend de ocnews.
- Referencia funcional: `nextcloud/news-android` (AGPL-3.0, Java). NO se copia código ni estructura: se usa solo como referencia de funcionalidad y UX.

## Estructura prevista
- `docs/audit.md` — paridad de funcionalidades + flujo de sync del cliente (referencia).
- `docs/mvp-spec.md` — especificación del MVP.
- `app/` — módulo Android.

## Cómo se trabaja
- Kotlin idiomático, Compose, MVVM (UI + ViewModel + Repository).
- Room + DataStore, Retrofit/OkHttp + kotlinx.serialization, Coroutines/Flow, WorkManager.
- Commits conventional (`feat:`, `fix:`, `refactor:`, `docs:`), ramas `<tipo>/<desc>`.
- Español en la comunicación, inglés en código y docs del repo.
