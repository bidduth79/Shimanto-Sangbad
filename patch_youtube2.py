import os

filepath = "app/src/main/java/com/example/data/YouTubeRepository.kt"
with open(filepath, "r") as f:
    content = f.read()

old_channel_logic = """            } else {
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

new_channel_logic = """            } else {
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
                                maxResults = 50,
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

content = content.replace(old_channel_logic, new_channel_logic)

with open(filepath, "w") as f:
    f.write(content)
