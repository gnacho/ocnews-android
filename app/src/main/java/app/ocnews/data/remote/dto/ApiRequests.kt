package app.ocnews.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateFolderRequest(val name: String)

@Serializable
data class CreateFeedRequest(
    val url: String,
    val folderId: Long? = null,
)

@Serializable
data class RenameFeedRequest(val feedTitle: String)

@Serializable
data class RenameFolderRequest(val name: String)

@Serializable
data class MoveFeedRequest(val folderId: Long)

// v1.3: read/unread/star/unstar multiple take plain item ids.
@Serializable
data class MarkMultipleRequest(val items: List<Long>)
