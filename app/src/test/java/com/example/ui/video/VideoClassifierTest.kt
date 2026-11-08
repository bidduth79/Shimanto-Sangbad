package com.example.ui.video

import com.example.data.ChannelInfo
import com.example.data.SourceKind.LOCAL
import com.example.data.SourceKind.NATIONAL
import com.example.data.SourceKind.OTHER
import com.example.data.SourceKind.TV
import com.example.data.VideoRegion
import com.example.data.VideoRegion.BANGLADESH
import com.example.data.VideoRegion.INDIA
import com.example.data.VideoRegion.INTERNATIONAL
import com.example.data.VideoRegion.MYANMAR
import com.example.data.YouTubeSearchResult
import com.example.data.YouTubeSnippet
import com.example.data.YouTubeVideoId
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoClassifierTest {

    private fun video(id: String, channel: String, channelId: String?, title: String = "বিজিবি $id", country: String? = null) =
        YouTubeSearchResult(
            id = YouTubeVideoId(videoId = id),
            snippet = YouTubeSnippet(title = title, channelTitle = channel, channelId = channelId, channelCountry = country)
        )

    private val cases: List<Pair<YouTubeSearchResult, ChannelInfo>> = listOf(
        video("a", "SOMOY TV", "UCxHoBXkY88Tb8z1Ssj6CWsQ") to ChannelInfo(BANGLADESH, TV),  // known TV ID
        video("b", "Prothom Alo", null) to ChannelInfo(BANGLADESH, NATIONAL),             // scraped, outlet name
        video("c", "Border Vlog", "UC_blogger") to ChannelInfo(BANGLADESH, OTHER),         // blogger
        video("d", "TV9 Bangla", "UCHCR4UFsGwd_VcDa0-a4haw") to ChannelInfo(INDIA, TV),  // Indian TV list
        video("e", "Some Channel", "UC_x", country = "IN") to ChannelInfo(INDIA, OTHER),   // declared country
        video("f", "Fauji Tube", "UC_y", title = "बीएसएफ बॉर्डर पर") to ChannelInfo(INDIA, OTHER), // Hindi script
        video("g", "Jugantor News", "UCTNQuDyqYnFJPME529Twl0g") to ChannelInfo(BANGLADESH, NATIONAL),  // 2nd BD channel
        video("h", "ABP ANANDA", null) to ChannelInfo(INDIA, TV),                          // not "Ananda TV"
        video("i", "BBC News Bangla", "UChPD2BQ2dJ-w00klQQkc-Qg") to ChannelInfo(INTERNATIONAL, NATIONAL),
        video("j", "Rakhine Daily", "UC_z", title = "မြန်မာ နယ်စပ် BGB") to ChannelInfo(MYANMAR, OTHER), // Burmese
        video("k", "Al Jazeera English", "UC_aj", title = "Rohingya at the border") to ChannelInfo(INTERNATIONAL, TV),
        video("l", "Some Vlogger", "UC_v", country = "US") to ChannelInfo(INTERNATIONAL, OTHER),
        video("m", "Sylhet News 24", "UC_s") to ChannelInfo(BANGLADESH, LOCAL)                // district outlet
    )
    private val videos = cases.map { it.first }
    private val classifier = VideoClassifier(videos, setOf("UC_somoy"), setOf(watchedLinkOf("c")))

    @Test
    fun `each video gets a country and a kind`() {
        cases.forEach { (video, expected) ->
            assertEquals(video.snippet?.channelTitle, expected, classifier.infoOf(video))
        }
    }

    @Test
    fun `tab counts, chip counts and filters`() {
        assertEquals(RegionCounts(all = 13, bangladesh = 5, india = 4, myanmar = 1, international = 3), classifier.regionCounts(videos))

        val bangladesh = videos.filter { classifier.inRegion(it, VideoRegion.BANGLADESH) }
        assertEquals(VideoCounts(all = 5, tv = 1, national = 2, local = 1, other = 1, unread = 4, read = 1), classifier.counts(bangladesh))
        assertEquals(listOf("c"), bangladesh.filter { classifier.matches(it, VideoFilter.OTHER) }.map { it.id?.videoId })
        assertEquals(listOf("m"), bangladesh.filter { classifier.matches(it, VideoFilter.LOCAL) }.map { it.id?.videoId })
    }
}
