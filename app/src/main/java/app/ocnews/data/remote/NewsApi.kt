package app.ocnews.data.remote

import app.ocnews.data.remote.dto.CreateFeedRequest
import app.ocnews.data.remote.dto.CreateFolderRequest
import app.ocnews.data.remote.dto.DiscoverResponse
import app.ocnews.data.remote.dto.FeedsResponse
import app.ocnews.data.remote.dto.FoldersResponse
import app.ocnews.data.remote.dto.ItemsResponse
import app.ocnews.data.remote.dto.MarkMultipleRequest
import app.ocnews.data.remote.dto.MoveFeedRequest
import app.ocnews.data.remote.dto.RenameFeedRequest
import app.ocnews.data.remote.dto.RenameFolderRequest
import app.ocnews.data.remote.dto.UserResponse
import app.ocnews.data.remote.dto.VersionResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

// Nextcloud News REST API v1.3. Base: /index.php/apps/news/api/v1-3.
// `type`: 0=feed, 1=folder, 2=starred, 3=all.
interface NewsApi {
    @GET("version")
    suspend fun version(): VersionResponse

    @GET("user")
    suspend fun user(): UserResponse

    @GET("folders")
    suspend fun folders(): FoldersResponse

    @POST("folders")
    suspend fun createFolder(@Body body: CreateFolderRequest): FoldersResponse

    @PUT("folders/{id}")
    suspend fun renameFolder(@Path("id") id: Long, @Body body: RenameFolderRequest)

    @DELETE("folders/{id}")
    suspend fun deleteFolder(@Path("id") id: Long)

    @POST("folders/{id}/read")
    suspend fun markFolderRead(@Path("id") id: Long)

    @GET("feeds")
    suspend fun feeds(): FeedsResponse

    @POST("feeds")
    suspend fun createFeed(@Body body: CreateFeedRequest): FeedsResponse

    @GET("feeds/discover")
    suspend fun discover(@Query("url") url: String): DiscoverResponse

    @DELETE("feeds/{id}")
    suspend fun deleteFeed(@Path("id") id: Long)

    @PUT("feeds/{id}/move")
    suspend fun moveFeed(@Path("id") id: Long, @Body body: MoveFeedRequest)

    @PUT("feeds/{id}/rename")
    suspend fun renameFeed(@Path("id") id: Long, @Body body: RenameFeedRequest)

    @GET("items")
    suspend fun items(
        @Query("type") type: Int = 3,
        @Query("id") id: Long? = null,
        @Query("getRead") getRead: Boolean = false,
        @Query("batchSize") batchSize: Int = -1,
        @Query("offset") offset: Long? = null,
        @Query("oldestFirst") oldestFirst: Boolean = false,
    ): ItemsResponse

    @GET("items/updated")
    suspend fun itemsUpdated(
        @Query("lastModified") lastModified: Long,
        @Query("type") type: Int = 3,
        @Query("id") id: Long = 0,
    ): ItemsResponse

    @POST("items/{id}/read")
    suspend fun markRead(@Path("id") id: Long)

    @POST("items/{id}/unread")
    suspend fun markUnread(@Path("id") id: Long)

    @POST("items/{id}/star")
    suspend fun markStar(@Path("id") id: Long)

    @POST("items/{id}/unstar")
    suspend fun markUnstar(@Path("id") id: Long)

    @PUT("items/read/multiple")
    suspend fun markReadMultiple(@Body body: MarkMultipleRequest)

    @PUT("items/unread/multiple")
    suspend fun markUnreadMultiple(@Body body: MarkMultipleRequest)

    @PUT("items/star/multiple")
    suspend fun markStarMultiple(@Body body: MarkMultipleRequest)

    @PUT("items/unstar/multiple")
    suspend fun markUnstarMultiple(@Body body: MarkMultipleRequest)

    @POST("items/read")
    suspend fun markAllRead()
}
