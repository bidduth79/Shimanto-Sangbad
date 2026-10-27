import os

filepath = "app/src/main/java/com/example/data/YouTubeRepository.kt"
with open(filepath, "r") as f:
    content = f.read()

# 1. Increase publishedAfter to 7 days
old_time = "val publishedAfter = sdf.format(java.util.Date(System.currentTimeMillis() - 24L * 60 * 60 * 1000))"
new_time = "val publishedAfter = sdf.format(java.util.Date(System.currentTimeMillis() - 7L * 24L * 60L * 60L * 1000L))"
content = content.replace(old_time, new_time)

# 2. Fix the client side filter (allow 7 days)
old_filter = "diff >= -300_000L && diff <= 24L * 60 * 60 * 1000"
new_filter = "diff >= -300_000L && diff <= 7L * 24L * 60L * 60L * 1000L"
content = content.replace(old_filter, new_filter)

# 3. Change channel search to use search endpoint instead of playlist to get better keyword matching
old_channel_logic = """            } else {
                for (channelId in channels) {
                    try {
                        val channelResponse = apiService.getChannels(
                            channelId = channelId,
                            apiKey = apiKey
                        )
                        val uploadsPlaylistId = channelResponse.items.firstOrNull()?.contentDetails?.relatedPlaylists?.uploads
                        if (!uploadsPlaylistId.isNullOrBlank()) {
                            val response = apiService.getPlaylistItems(
                                playlistId = uploadsPlaylistId,
                                apiKey = apiKey
                            )
                            for (item in response.items) {
                                val title = item.snippet?.title ?: ""
                                val desc = item.snippet?.description ?: ""
                                val matchesKeyword = keywords.any { k -> title.contains(k, ignoreCase = true) || desc.contains(k, ignoreCase = true) }
                                if (matchesKeyword) {
                                    val searchResult = YouTubeSearchResult(
                                        id = YouTubeVideoId(videoId = item.snippet?.resourceId?.videoId),
                                        snippet = item.snippet
                                    )
                                    allVideoItems.add(searchResult)
                                }
                            }
                        }
                    } catch (e: HttpException) {
                        if (e.code() == 403 || e.code() == 400 || e.code() == 429) {
                            throw e // rethrow to be caught by the outer catch
                        }
                        Log.e("YouTubeRepository", "Error fetching videos for channel $channelId", e)
                    }
                }
            }"""

new_channel_logic = """            } else {
                for (channelId in channels) {
                    for (keyword in keywords) {
                        try {
                            val response = apiService.searchVideos(
                                query = keyword,
                                channelId = channelId,
                                publishedAfter = publishedAfter,
                                apiKey = apiKey
                            )
                            allVideoItems.addAll(response.items)
                        } catch (e: HttpException) {
                            if (e.code() == 403 || e.code() == 400 || e.code() == 429) {
                                throw e // rethrow to be caught by the outer catch
                            }
                            Log.e("YouTubeRepository", "Error fetching channel $channelId for keyword $keyword", e)
                        }
                    }
                }
            }"""

content = content.replace(old_channel_logic, new_channel_logic)

with open(filepath, "w") as f:
    f.write(content)
