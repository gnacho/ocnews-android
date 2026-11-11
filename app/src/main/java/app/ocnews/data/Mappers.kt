package app.ocnews.data

import app.ocnews.data.local.FeedEntity
import app.ocnews.data.local.FolderEntity
import app.ocnews.data.local.ItemEntity
import app.ocnews.data.remote.dto.FeedDto
import app.ocnews.data.remote.dto.FolderDto
import app.ocnews.data.remote.dto.ItemDto
import app.ocnews.util.firstImageUrl
import app.ocnews.util.unpackImageUrl

fun FolderDto.toEntity(): FolderEntity = FolderEntity(
    id = id,
    name = name,
    parentId = parentId,
)

fun FeedDto.toEntity(): FeedEntity = FeedEntity(
    id = id,
    url = url,
    title = title,
    faviconLink = faviconLink.orEmpty(),
    added = added,
    nextUpdateTime = nextUpdateTime ?: 0,
    folderId = folderId,
    unreadCount = unreadCount,
    ordering = ordering,
    link = link.orEmpty(),
    pinned = pinned,
    updateErrorCount = updateErrorCount,
    lastUpdateError = lastUpdateError,
    urlHash = urlHash,
    isPodcast = isPodcast,
)

fun ItemDto.toEntity(): ItemEntity = ItemEntity(
    id = id,
    guid = guid,
    guidHash = guidHash,
    url = url,
    title = title,
    author = author,
    pubDate = pubDate,
    body = body,
    enclosureMime = enclosureMime,
    enclosureLink = enclosureLink,
    mediaThumbnail = mediaThumbnail,
    mediaDescription = mediaDescription,
    thumbnailUrl = firstImageUrl(body)?.let { unpackImageUrl(it) },
    feedId = feedId,
    unread = unread,
    starred = starred,
    filtered = filtered,
    rtl = rtl,
    lastModified = lastModified,
    fingerprint = fingerprint,
)
