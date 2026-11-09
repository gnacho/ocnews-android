package app.ocnews.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class SwipeAction { STAR, READ, NOTHING }

enum class SortOrder { NEWEST_FIRST, OLDEST_FIRST }

enum class SearchScope { TITLE, CONTENT, BOTH }

enum class FeedListStyle { THUMBNAILS, TITLES_ONLY }

enum class TextSize { SMALL, DEFAULT, LARGE }

// When the offline image download is allowed to run.
enum class ImageCacheMode { NEVER, WIFI_ONLY, WIFI_AND_MOBILE, ASK }

data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val syncIntervalMinutes: Int = 15,
    val syncOnStart: Boolean = true,
    val onlyUnread: Boolean = false,
    val swipeRight: SwipeAction = SwipeAction.STAR,
    val swipeLeft: SwipeAction = SwipeAction.READ,
    val sortOrder: SortOrder = SortOrder.NEWEST_FIRST,
    val searchIn: SearchScope = SearchScope.TITLE,
    val feedListStyle: FeedListStyle = FeedListStyle.THUMBNAILS,
    val textSize: TextSize = TextSize.DEFAULT,
    // Pure black background when the dark theme is active (OLED screens).
    val oledBackground: Boolean = false,
    // Volume keys move between articles while the reader is open.
    val navigateWithVolumeButtons: Boolean = false,
    // Offline image download policy and Coil disk cache size.
    val imageCacheMode: ImageCacheMode = ImageCacheMode.WIFI_AND_MOBILE,
    val maxCacheSizeMb: Int = 500,
)

interface SettingsStore {
    val settings: Flow<AppSettings>
    suspend fun setTheme(mode: ThemeMode)
    suspend fun setSyncInterval(minutes: Int)
    suspend fun setSyncOnStart(value: Boolean)
    suspend fun setOnlyUnread(value: Boolean)
    suspend fun setSwipeRight(action: SwipeAction)
    suspend fun setSwipeLeft(action: SwipeAction)
    suspend fun setSortOrder(order: SortOrder)
    suspend fun setSearchIn(scope: SearchScope)
    suspend fun setFeedListStyle(style: FeedListStyle)
    suspend fun setTextSize(size: TextSize)
    suspend fun setOledBackground(value: Boolean)
    suspend fun setNavigateWithVolumeButtons(value: Boolean)
    suspend fun setImageCacheMode(mode: ImageCacheMode)
    suspend fun setMaxCacheSizeMb(mb: Int)
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

private inline fun <reified T : Enum<T>> decode(name: String?, default: T): T =
    name?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

class DataStoreSettingsStore(private val context: Context) : SettingsStore {
    private val kTheme = stringPreferencesKey("theme")
    private val kInterval = intPreferencesKey("sync_interval_minutes")
    private val kSyncOnStart = booleanPreferencesKey("sync_on_start")
    private val kOnlyUnread = booleanPreferencesKey("only_unread")
    private val kSwipeRight = stringPreferencesKey("swipe_right")
    private val kSwipeLeft = stringPreferencesKey("swipe_left")
    private val kSortOrder = stringPreferencesKey("sort_order")
    private val kSearchIn = stringPreferencesKey("search_in")
    private val kFeedList = stringPreferencesKey("feed_list_style")
    private val kTextSize = stringPreferencesKey("text_size")
    private val kOled = booleanPreferencesKey("oled_background")
    private val kVolumeNav = booleanPreferencesKey("navigate_with_volume_buttons")
    private val kImageCacheMode = stringPreferencesKey("image_cache_mode")
    private val kMaxCacheSize = intPreferencesKey("max_cache_size_mb")

    override val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            theme = decode(p[kTheme], ThemeMode.SYSTEM),
            syncIntervalMinutes = p[kInterval] ?: 15,
            syncOnStart = p[kSyncOnStart] ?: true,
            onlyUnread = p[kOnlyUnread] ?: false,
            swipeRight = decode(p[kSwipeRight], SwipeAction.STAR),
            swipeLeft = decode(p[kSwipeLeft], SwipeAction.READ),
            sortOrder = decode(p[kSortOrder], SortOrder.NEWEST_FIRST),
            searchIn = decode(p[kSearchIn], SearchScope.TITLE),
            feedListStyle = decode(p[kFeedList], FeedListStyle.THUMBNAILS),
            textSize = decode(p[kTextSize], TextSize.DEFAULT),
            oledBackground = p[kOled] ?: false,
            navigateWithVolumeButtons = p[kVolumeNav] ?: false,
            imageCacheMode = decode(p[kImageCacheMode], ImageCacheMode.WIFI_AND_MOBILE),
            maxCacheSizeMb = p[kMaxCacheSize] ?: 500,
        )
    }

    override suspend fun setTheme(mode: ThemeMode) {
        context.dataStore.edit { it[kTheme] = mode.name }
    }

    override suspend fun setSyncInterval(minutes: Int) {
        context.dataStore.edit { it[kInterval] = minutes }
    }

    override suspend fun setSyncOnStart(value: Boolean) {
        context.dataStore.edit { it[kSyncOnStart] = value }
    }

    override suspend fun setOnlyUnread(value: Boolean) {
        context.dataStore.edit { it[kOnlyUnread] = value }
    }

    override suspend fun setSwipeRight(action: SwipeAction) {
        context.dataStore.edit { it[kSwipeRight] = action.name }
    }

    override suspend fun setSwipeLeft(action: SwipeAction) {
        context.dataStore.edit { it[kSwipeLeft] = action.name }
    }

    override suspend fun setSortOrder(order: SortOrder) {
        context.dataStore.edit { it[kSortOrder] = order.name }
    }

    override suspend fun setSearchIn(scope: SearchScope) {
        context.dataStore.edit { it[kSearchIn] = scope.name }
    }

    override suspend fun setFeedListStyle(style: FeedListStyle) {
        context.dataStore.edit { it[kFeedList] = style.name }
    }

    override suspend fun setTextSize(size: TextSize) {
        context.dataStore.edit { it[kTextSize] = size.name }
    }

    override suspend fun setOledBackground(value: Boolean) {
        context.dataStore.edit { it[kOled] = value }
    }

    override suspend fun setNavigateWithVolumeButtons(value: Boolean) {
        context.dataStore.edit { it[kVolumeNav] = value }
    }

    override suspend fun setImageCacheMode(mode: ImageCacheMode) {
        context.dataStore.edit { it[kImageCacheMode] = mode.name }
    }

    override suspend fun setMaxCacheSizeMb(mb: Int) {
        context.dataStore.edit { it[kMaxCacheSize] = mb }
    }
}
