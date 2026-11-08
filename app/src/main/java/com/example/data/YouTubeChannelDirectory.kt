package com.example.data

/** Country tab a video belongs to (same tabs as the news). */
enum class VideoRegion { BANGLADESH, INDIA, MYANMAR, INTERNATIONAL }

/**
 * Kind of source inside a country tab: TV channel, national paper/portal, local (district and
 * regional outlets) or other (bloggers and every uploader that is not a known outlet).
 */
enum class SourceKind { TV, NATIONAL, LOCAL, OTHER }

/** Where an uploader is from and what kind of source it is. */
data class ChannelInfo(val region: VideoRegion, val kind: SourceKind) {
    val isMedia: Boolean get() = kind != SourceKind.OTHER
}

/**
 * Sorts video uploaders by country and by kind (news outlet vs local uploader).
 *
 * Evidence, strongest first:
 *  1. channel ID in one of the known lists below (or the user's own channel IDs → Bangladeshi media);
 *  2. the country the channel declares on YouTube (looked up with the API key and cached);
 *  3. the channel name (distinctive outlet names only) and the script of the video title
 *     (Hindi and other Indian scripts → India, Burmese → Myanmar);
 *  4. otherwise a Bengali or Latin title is taken as Bangladeshi, anything else as international.
 */
object YouTubeChannelDirectory {

    /** International outlets publishing in Bengali; polled with the Bangladeshi channels. */
    val internationalMediaIds: Set<String> = setOf(
        "UChPD2BQ2dJ-w00klQQkc-Qg", // BBC News Bangla
        "UCIIkOzwZIdLfHa4Q13dQrkw"  // DW Bangla
    )

    /** Second channels of Bangladeshi outlets and BGB's own channel: media, but not polled. */
    val extraBangladeshMediaIds: Set<String> = setOf(
        "UCa6T5wItwRQyL7N72Zq6cmg", // Border Guard Bangladesh (BGB)
        "UCTNQuDyqYnFJPME529Twl0g", // Jugantor News
        "UCuzoe7B4zeAKhdhcNKsQJ1w", // Dhaka Post (second channel)
        "UCKteCkx2KDUgVw_EzYM0bsA", // Ittefaq Digital
        "UC2lQ-lzB4_fIShEKm89U5hg", // ATN Bangla Digital
        "UCJ-Rn3KyQlJC6aONZGOquOA", // BanglaVision Network
        "UCJY7S9QGo8JBGuVc-1LGagA"  // Banglabazar Patrika
    )

    /** Indian news outlets that are not polled but turn up in searches. */
    private val extraIndianMediaIds: Set<String> = setOf(
        "UCh-fA3jGjpMPRv-x6KfNC1w", // R Bangla News
        "UCe4GErYKwpMUJWZpxOiATYA", // DD Bangla News
        "UCjXz2CmSoWyY1iM5AjU_DSw", // ntvwb news (West Bengal)
        "UCytSP0M0Jdnw6qIy3Y-nTig", // NewsX
        "UCz8QaiQxApLq8sLNcszYyJw", // Firstpost
        "UCBc13XYipnBIBE3Ff8QaaGg", // IBC24
        "UCDnrXkM5KnZnfheiLDjaNbg", // Time8 News (Assam)
        "UCyYrS70unXk8Yg5B48eBIxQ", // Koktripura Exclusive
        "UCQIqJMi1G_0yxzAbYQ-o-WA", // Gomati Express News (Tripura)
        "UCm7slMV4azBfN3NxmVEIqQQ"  // News Surge India
    )

    /** Indian uploaders that are not news outlets. */
    private val indianOtherIds: Set<String> = setOf(
        "UCMPHUYgfD0hNX07KqPRN5kg", // BJP West Bengal
        "UC02VjVpNCmhb79HF60oBMNQ"  // Indian Fauji
    )

    val indianMediaIds: Set<String> = NewsSources.youtubeIndianChannels.toSet() + extraIndianMediaIds

    /** Bangladeshi TV channels among the known IDs; the other known Bangladeshi IDs are papers/portals. */
    private val bangladeshTvIds: Set<String> = setOf(
        "UCxHoBXkY88Tb8z1Ssj6CWsQ", "UCN6sm8iHiPd0cnoUardDAnw", "UCATUkaOHwO9EP_W87zCiPbA", "UCtqvtAVmad5zywaziN6CbfA",
        "UCHLqIOMPk20w-6cFgkA90jw", "UCUvXoiDEKI8VZJrr58g4VAw", "UCUDQdVsKssximyFwg4IxnOQ", "UC2P5Fd5g41Gtdqf0Uzh8Qaw",
        "UCKlhfq1ILoAFav7iw5iCnfA", "UCbgcYEdMsuypG2NJ-znBp3w", "UCA4y9g_GRdNrZLH_u5LLSdw", "UCWVqdPTigfQ-cSNwG7O9MeA",
        "UCPREnbhKQP-hsVfsfKP-mCw", "UCmCCTsDl-eCKw91shC7ZmMw", "UCBQxpxkI2qLRAFR_6PNdwIA", "UCTSvHZkOStz5Ak2J_q6YRlA",
        "UCUxdUUZZNXrZprkqTja0Ltg", "UCMMCiB3he7fwFUvLBRMTaZQ", "UC8uzDYcztPf-fICdGTowAAA", "UC66GWNX7DG04XOMIee47Hwg",
        "UC9zgYOQB6zkleAQjW53DLeQ", "UC8NcXMG3A3f2aFQyGTpSNww", "UCHR-p802PibDYG1btNJcOew", "UC-NvlImq-FA3X62HjtGkvGQ",
        "UCjIi0uZg4h5Ar8gvLDTrGwA", "UCHd3pezHElEC3_jv8qTvjVw", "UClzwimpLoZu9us9MwalLbtA", "UC9b_MnX3HN9ABYT89QqYMTw",
        "UCjYHHBaJWbqFVBKzyq495QA", "UCaXcMdJaXNGlPMC1xlNWFdw", "UCdToTtQYK7AYsarULHfU1EQ", "UC0ESiUN0fy6sS_zxO5jOP-g",
        "UCgt4pZnDTDs8lLPp2GOjLIw", "UC2lQ-lzB4_fIShEKm89U5hg", "UCJ-Rn3KyQlJC6aONZGOquOA"
    )

    /** District and regional Bangladeshi outlets. */
    private val bangladeshLocalIds: Set<String> = setOf(
        "UCeU0BTbx7aDFfGSEF-2sZ5Q", // Chattogram 24
        "UCIS8vxDuiRtCNYn92NP94Lw", // Daily Karatoa (Bogura)
        "UCPczt8v8G_NtXh8vbTUOh6Q", // Daily Purbokone (Chattogram)
        "UCNLMkA8j786HsqnI18ilwfA", // Barishal dot News
        "UCIv1CRETiZfcHyJBs9SGSww", // Sylhetview
        "UCGFPxFYEFx7c3qk3kyEACUw"  // Noakhali TV
    )

    /** Indian TV channels; the other known Indian outlets are papers/portals. */
    private val indianTvIds: Set<String> = setOf(
        "UCv3rFzn-GHGtqzXiaq3sWNg", "UCdF5Q5QVbYstYrTfpgUl0ZA", "UCHCR4UFsGwd_VcDa0-a4haw", "UCbf0XHULBkTfv2hBjaaDw9Q",
        "UCNvCQpcafnbW4KQ8X7oQ9kg", "UCJ3I6MHOz5exARlTW_meOGQ", "UCycQauzU5n3DwQdgk3cw91A", "UCdfOu3dJAYCc99mUy8hyfMw",
        "UCajVjEHDoVn_AHsunUZz_EQ", "UCt4t-jeY85JegMlZ-E5UWtA", "UC9CYT9gSNLevX5ey2_6CK0Q", "UCZFMm1mMw0F81Z37aaEzTUA",
        "UCYPvAwZP8pZhSMW8qs7cVCw", "UCRWFSbif-RFENbBrSiez1DA", "UCIvaYmXn910QMdemBG3v1pQ", "UC7wXt18f2iA3EDXeqAVuKng",
        "UCwqusr8YDwM-3mEYTDeJHzw", "UCef1-8eOpJgud7szVPlZQAQ", "UC6RJ7-PaXg6TIH2BzZfTV7w", "UC_gUM8rL-Lrg6O3adPW9K1g",
        "UCKwucPzHZ7zCUIf7If-Wo1g", "UCh-fA3jGjpMPRv-x6KfNC1w", "UCe4GErYKwpMUJWZpxOiATYA", "UCytSP0M0Jdnw6qIy3Y-nTig",
        "UCBc13XYipnBIBE3Ff8QaaGg"
    )

    /** State and district Indian outlets (North Bengal, Tripura, Assam, West Bengal locals). */
    private val indianLocalIds: Set<String> = setOf(
        "UCXDw7ADM9CnoGPEk3aSsHaQ", // Uttarbanga Sambad TV
        "UCu43V7JTZtMXIf7s2XciAUQ", // Syandan TV (Tripura)
        "UCyYrS70unXk8Yg5B48eBIxQ", // Koktripura Exclusive
        "UCQIqJMi1G_0yxzAbYQ-o-WA", // Gomati Express News
        "UCDnrXkM5KnZnfheiLDjaNbg", // Time8 News (Assam)
        "UCjXz2CmSoWyY1iM5AjU_DSw"  // ntvwb news
    )

    /** Every channel ID whose country is already known (no API lookup needed). */
    val knownIds: Set<String> = NewsSources.youtubeNewsChannels.toSet() + internationalMediaIds +
        extraBangladeshMediaIds + indianMediaIds + indianOtherIds

    // Channel-name words, matched as whole words, case-insensitive.
    private val indianMediaNames = listOf(
        "abp", "news18", "tv9", "zee", "ndtv", "aaj tak", "india today", "republic", "times now", "wion",
        "dd news", "dd bangla", "dd india", "the hindu", "hindustan times", "times of india", "indian express",
        "anandabazar", "bartaman", "sangbad pratidin", "r plus", "r bangla", "kolkata tv", "calcutta news",
        "firstpost", "ei samay", "aajkaal", "uttarbanga", "the wall", "lallantop", "asianet", "etv bharat",
        "time8", "doordarshan", "cnn-news18", "news nation", "india tv"
    )
    private val indianPlaceNames = listOf(
        "india", "indian", "bharat", "kolkata", "calcutta", "tripura", "assam", "guwahati", "siliguri",
        "meghalaya", "mizoram", "manipur", "west bengal", "hindi", "bjp", "trinamool", "tmc", "congress",
        "northeast", "north east", "koktripura", "agartala"
    )
    private val myanmarNames = listOf(
        "myanmar", "burma", "burmese", "irrawaddy", "mizzima", "dvb", "khit thit", "myanmar now",
        "rfa burmese", "bbc burmese", "eleven media", "arakan"
    )
    private val internationalMediaNames = listOf(
        "bbc", "dw", "deutsche welle", "voa", "al jazeera", "aljazeera", "cnn", "reuters", "associated press",
        "france 24", "trt", "cgtn", "sky news", "abc news", "nbc news", "cbs news", "fox news", "the guardian",
        "nikkei", "dawn news", "geo news", "ary news", "press tv", "euronews", "channel 4 news", "wion"
    )
    private val districtNames = listOf(
        "sylhet", "chattogram", "chittagong", "ctg", "khulna", "barishal", "barisal", "rajshahi", "rangpur",
        "cumilla", "comilla", "noakhali", "feni", "cox's bazar", "coxsbazar", "ukhiya", "teknaf", "satkhira",
        "jashore", "jessore", "bogura", "bogra", "dinajpur", "thakurgaon", "kushtia", "chuadanga", "meherpur",
        "lalmonirhat", "kurigram", "nilphamari", "panchagarh", "sunamganj", "moulvibazar", "habiganj",
        "brahmanbaria", "chandpur", "lakshmipur", "jamalpur", "mymensingh", "sherpur", "netrokona", "tangail",
        "kuakata", "bandarban", "rangamati", "khagrachari", "naogaon", "joypurhat", "chapainawabganj",
        "tripura", "agartala", "malda", "siliguri", "cooch behar", "murshidabad", "nadia", "assam", "meghalaya",
        "সিলেট", "চট্টগ্রাম", "খুলনা", "বরিশাল", "রাজশাহী", "রংপুর", "কুমিল্লা", "নোয়াখালী", "ফেনী", "কক্সবাজার",
        "সাতক্ষীরা", "যশোর", "বগুড়া", "দিনাজপুর", "কুষ্টিয়া", "চুয়াডাঙ্গা", "লালমনিরহাট", "কুড়িগ্রাম", "সুনামগঞ্জ",
        "মৌলভীবাজার", "হবিগঞ্জ", "চাঁদপুর", "ময়মনসিংহ", "টাঙ্গাইল", "ত্রিপুরা"
    )
    private val outletWords = listOf(
        "news", "tv", "times", "barta", "khobor", "protidin", "pratidin", "post", "express", "view", "kantho",
        "24", "live", "নিউজ", "টিভি", "খবর", "বার্তা", "প্রতিদিন", "সংবাদ", "কণ্ঠ", "টাইমস"
    )
    private val bangladeshTvNames = listOf(
        "somoy tv", "jamuna tv", "independent television", "independent tv", "ekattor", "channel 24", "dbc news",
        "ntv", "rtv", "atn news", "atn bangla", "banglavision", "ekhon tv", "desh tv", "ekushey", "etv",
        "boishakhi", "global tv", "satv", "deepto", "mytv", "channel i", "maasranga", "asian tv", "asian television",
        "gtv", "nagorik tv", "bangladesh television", "btv", "mohona tv", "ananda tv", "bijoy tv", "channel s",
        "channel nine"
    )
    private val indianTvNames = listOf(
        "abp", "zee", "tv9", "news18", "kolkata tv", "calcutta news", "r plus", "r bangla", "republic", "aaj tak",
        "ndtv", "india today", "times now", "wion", "dd news", "dd bangla", "dd india", "cnn-news18", "news nation",
        "india tv", "newsx", "ibc24"
    )
    private val bangladeshMediaNames = listOf(
        "somoy tv", "jamuna tv", "independent television", "independent tv", "ekattor", "channel 24",
        "dbc news", "ntv news", "rtv news", "atn news", "atn bangla", "banglavision", "ekhon tv", "desh tv",
        "ekushey", "etv", "boishakhi", "global tv", "satv", "deepto", "prothom alo", "kaler kantho",
        "jugantor", "samakal", "bangladesh pratidin", "kalbela", "ittefaq", "jago news", "dhaka post",
        "bdnews24", "bangla tribune", "dhaka tribune", "daily star", "barta24", "times of bangladesh", "unb",
        "shomoyer alo", "daily sun", "mytv", "npb news", "bhorer kagoj", "desh rupantor", "ajker patrika",
        "channel i", "maasranga", "asian tv", "gtv news", "nagorik tv", "bangladesh television", "btv",
        "mohona tv", "ananda tv", "bijoy tv", "naya diganta", "manabzamin", "inqilab", "amar desh",
        "protidiner bangladesh", "banglanews24", "risingbd", "sarabangla", "the business standard",
        "channel s", "border guard bangladesh", "banglabazar", "bonik barta", "janakantha", "amader shomoy",
        "dainik bangla", "sangram", "dhaka times"
    )

    fun infoOf(
        channelId: String?,
        channelTitle: String?,
        videoTitle: String?,
        country: String?,
        bangladeshMediaIds: Set<String>
    ): ChannelInfo {
        if (channelId != null) {
            when (channelId) {
                in internationalMediaIds -> return ChannelInfo(VideoRegion.INTERNATIONAL, SourceKind.NATIONAL)
                in bangladeshTvIds -> return ChannelInfo(VideoRegion.BANGLADESH, SourceKind.TV)
                in bangladeshLocalIds -> return ChannelInfo(VideoRegion.BANGLADESH, SourceKind.LOCAL)
                in bangladeshMediaIds, in extraBangladeshMediaIds -> return ChannelInfo(VideoRegion.BANGLADESH, SourceKind.NATIONAL)
                in indianTvIds -> return ChannelInfo(VideoRegion.INDIA, SourceKind.TV)
                in indianLocalIds -> return ChannelInfo(VideoRegion.INDIA, SourceKind.LOCAL)
                in indianOtherIds -> return ChannelInfo(VideoRegion.INDIA, SourceKind.OTHER)
                in indianMediaIds -> return ChannelInfo(VideoRegion.INDIA, SourceKind.NATIONAL)
            }
        }
        val name = channelTitle.orEmpty().lowercase()
        val title = videoTitle.orEmpty()
        fun named(words: List<String>) = words.any { containsWord(name, it) }

        // Outlet names say both the country and the kind. Indian first: "ABP Ananda" is not
        // Bangladesh's "Ananda TV".
        if (named(indianTvNames)) return ChannelInfo(VideoRegion.INDIA, SourceKind.TV)
        if (named(indianMediaNames)) return ChannelInfo(VideoRegion.INDIA, SourceKind.NATIONAL)
        if (named(internationalMediaNames)) return ChannelInfo(VideoRegion.INTERNATIONAL, SourceKind.TV)
        if (named(myanmarNames)) {
            val outlet = myanmarNames.drop(3).any { containsWord(name, it) }
            return ChannelInfo(VideoRegion.MYANMAR, if (outlet) SourceKind.NATIONAL else SourceKind.OTHER)
        }
        val bdKind = when {
            // "Sylhet News 24", "খুলনার খবর": a place in an outlet's name makes it a district outlet
            named(districtNames) && named(outletWords) -> SourceKind.LOCAL
            named(bangladeshTvNames) -> SourceKind.TV
            named(bangladeshMediaNames) -> SourceKind.NATIONAL
            else -> null
        }

        val region = when (country?.uppercase()) {
            "IN" -> VideoRegion.INDIA
            "MM" -> VideoRegion.MYANMAR
            "BD" -> VideoRegion.BANGLADESH
            null, "" -> when {
                named(indianPlaceNames) || hasIndianScript(title) -> VideoRegion.INDIA
                hasBurmeseScript(title) -> VideoRegion.MYANMAR
                bdKind != null || hasBengaliOrLatinOnly(title) -> VideoRegion.BANGLADESH
                else -> VideoRegion.INTERNATIONAL
            }
            else -> VideoRegion.INTERNATIONAL
        }
        val kind = when {
            region == VideoRegion.BANGLADESH && bdKind != null -> bdKind
            // a district outlet's name usually carries its place: "Sylhet News 24", "খুলনার খবর"
            named(districtNames) && named(outletWords) -> SourceKind.LOCAL
            else -> SourceKind.OTHER
        }
        return ChannelInfo(region, kind)
    }

    /**
     * Kind of an outlet from its name alone (news publishers have no channel ID): TV, national,
     * local (a place in the name, or a known district paper), or null when the name says nothing.
     */
    fun kindFromName(publisher: String): SourceKind? {
        val name = publisher.lowercase()
        fun named(words: List<String>) = words.any { containsWord(name, it) }
        return when {
            named(localOutletNames) -> SourceKind.LOCAL
            named(districtNames) && named(outletWords) -> SourceKind.LOCAL
            named(bengaliTvNames) || named(bangladeshTvNames) || named(indianTvNames) -> SourceKind.TV
            named(bangladeshMediaNames) || named(indianMediaNames) || named(internationalMediaNames) -> SourceKind.NATIONAL
            else -> null
        }
    }

    /** TV channels as Bengali news feeds name them. */
    private val bengaliTvNames = listOf(
        "সময় টিভি", "যমুনা টিভি", "চ্যানেল ২৪", "চ্যানেল টোয়েন্টিফোর", "ডিবিসি", "একাত্তর", "ইনডিপেনডেন্ট টিভি",
        "এনটিভি", "চ্যানেল আই", "এটিএন", "আরটিভি", "বাংলাভিশন", "নিউজ২৪", "নিউজ ২৪", "এখন টিভি", "দেশ টিভি",
        "বৈশাখী", "মাছরাঙা", "একুশে টিভি", "জিটিভি", "বিটিভি", "এসএ টিভি", "মোহনা", "বিজয় টিভি", "মাই টিভি",
        "নাগরিক টিভি", "এশিয়ান টিভি", "এবিপি আনন্দ", "জি ২৪ ঘণ্টা", "টিভি৯", "নিউজ১৮", "কলকাতা টিভি"
    )

    /** District and regional papers that carry no place name. */
    private val localOutletNames = listOf(
        "মাথাভাঙ্গা", "আজাদী", "পূর্বকোণ", "করতোয়া", "সুপ্রভাত বাংলাদেশ", "শাহনামা", "খুলনাঞ্চল", "উত্তরবঙ্গ সংবাদ",
        "স্যন্দন", "পাহাড়বার্তা", "সিলেট ভিউ", "সুরমা টাইমস", "শ্যামল সিলেট", "দৈনিক সংবাদ ত্রিপুরা",
        "azadi", "purbokone", "karatoa", "sylhetview", "uttarbanga sambad", "tripura times", "ne now", "northeast now"
    )

    /** Devanagari, Gurmukhi, Gujarati, Odia, Tamil, Telugu, Kannada or Malayalam (Bengali is excluded). */
    internal fun hasIndianScript(text: String): Boolean =
        text.any { it in 'ऀ'..'ॿ' || it in '਀'..'ൿ' }

    private fun hasBurmeseScript(text: String): Boolean = text.any { it in 'က'..'႟' }

    /** No letters other than Bengali and Latin (digits, punctuation and emoji are ignored). */
    private fun hasBengaliOrLatinOnly(text: String): Boolean =
        text.none { it.isLetter() && it !in 'ঀ'..'৿' && it.code >= 0x250 }

    private fun containsWord(text: String, word: String): Boolean {
        var from = 0
        while (true) {
            val i = text.indexOf(word, from)
            if (i < 0) return false
            val before = if (i == 0) ' ' else text[i - 1]
            val afterIndex = i + word.length
            val after = if (afterIndex >= text.length) ' ' else text[afterIndex]
            if (!before.isLetterOrDigit() && !after.isLetterOrDigit()) return true
            from = i + 1
        }
    }
}
