package app.ocnews.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class FolderDto(
    val id: Long,
    val name: String,
    val parentId: Long? = null,
)

@Serializable
data class FeedDto(
    val id: Long,
    val url: String,
    val title: String,
    val faviconLink: String = "",
    val added: Long = 0,
    val nextUpdateTime: Long = 0,
    val folderId: Long? = null,
    val unreadCount: Long = 0,
    val ordering: Int = 0,
    val link: String = "",
    val pinned: Boolean = false,
    val updateErrorCount: Int = 0,
    val lastUpdateError: String? = null,
    val urlHash: String = "",
    val isPodcast: Boolean = false,
)

@Serializable
data class ItemDto(
    val id: Long,
    val guid: String = "",
    val guidHash: String = "",
    val url: String = "",
    val title: String = "",
    val author: String = "",
    val pubDate: Long = 0,
    val body: String = "",
    val enclosureMime: String? = null,
    val enclosureLink: String? = null,
    val mediaThumbnail: String? = null,
    val mediaDescription: String? = null,
    val feedId: Long = 0,
    val unread: Boolean = false,
    val starred: Boolean = false,
    val filtered: Boolean = false,
    val rtl: Boolean = false,
    val lastModified: Long = 0,
    val fingerprint: String = "",
)

@Serializable
data class VersionResponse(val version: String)

@Serializable
data class UserResponse(
    val userId: String,
    val displayName: String,
    val lastLoginTimestamp: Long = 0,
    val language: String? = null,
    val avatar: String? = null,
)

@Serializable
data class DiscoveredFeedDto(
    val url: String,
    val title: String = "",
    val type: String = "rss",
)

@Serializable
data class FoldersResponse(val folders: List<FolderDto>)

@Serializable
data class FeedsResponse(
    val feeds: List<FeedDto>,
    val starredCount: Long = 0,
    val newestItemId: Long? = null,
)

@Serializable
data class ItemsResponse(val items: List<ItemDto>)

@Serializable
data class DiscoverResponse(val feeds: List<DiscoveredFeedDto>)
