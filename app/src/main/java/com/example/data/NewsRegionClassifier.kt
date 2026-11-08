package com.example.data

/**
 * Decides which country tab a news article belongs to from WHO PUBLISHED it, not from which
 * search query happened to find it. A Kolkata paper found by a "Bangladesh" search still goes
 * to the India tab.
 *
 * Order of evidence:
 *  1. Built-in newspaper feeds carry their region directly.
 *  2. Publisher name / link / Google News title tail matched against Myanmar, Bangladeshi and
 *     Indian media lists.
 *  3. Otherwise the tab the article was fetched for (its category).
 */
object NewsRegionClassifier {

    /** Region per article link. A link's publisher never changes, so each is classified once. */
    private val cache = java.util.concurrent.ConcurrentHashMap<String, NewsRegion>()

    fun regionOf(article: NewsArticle): NewsRegion =
        cache.getOrPut(article.link + "|" + article.category) { classify(article) }

    private fun classify(article: NewsArticle): NewsRegion {
        feedRegion(article)?.let { return it }
        val haystack = publisherText(article)
        return when {
            MYANMAR_MEDIA_MARKERS.any { haystack.contains(it) } -> NewsRegion.MYANMAR
            isBangladeshiMedia(article) -> NewsRegion.BANGLADESH
            looksIndian(article, haystack) -> NewsRegion.INDIA
            else -> fallbackRegion(article.category)
        }
    }

    fun isBangladeshiMedia(article: NewsArticle): Boolean {
        feedRegion(article)?.let { return it == NewsRegion.BANGLADESH }
        val sourceLower = article.source.lowercase()
        val host = publisherHost(article)
        val titleTailLower = titleTail(article)
        if (host.endsWith(".bd") || sourceLower.endsWith(".bd") || titleTailLower.endsWith(".bd")) return true
        return BD_MEDIA_MARKERS.any { marker ->
            sourceLower.contains(marker) || host.contains(marker) || titleTailLower.contains(marker)
        }
    }

    private fun looksIndian(article: NewsArticle, haystack: String): Boolean {
        if (publisherHost(article).endsWith(".in")) return true
        return INDIA_MEDIA_MARKERS.any { haystack.contains(it) }
    }

    private fun feedRegion(article: NewsArticle): NewsRegion? {
        if (!article.source.startsWith(NewsSources.NEWSPAPER_SOURCE_PREFIX)) return null
        val feedName = article.source.substringAfter("•").trim()
        return NewsSources.newspaperFeeds.firstOrNull { it.name == feedName }?.region
    }

    /** Source, link and the " - Publisher" tail Google News appends to titles. Never the headline body. */
    private fun publisherText(article: NewsArticle): String =
        "${article.source} ${publisherHost(article)} ${titleTail(article)}".lowercase()

    /**
     * The publisher's domain, or "" when the link points at a search engine. Only the host is used:
     * a Google News link's path is a random-looking ID that can contain marker words like "tv9" by chance.
     */
    private fun publisherHost(article: NewsArticle): String {
        val host = runCatching { java.net.URI(article.originalLink ?: article.link).host }.getOrNull()
            ?.lowercase()?.removePrefix("www.") ?: return ""
        return if (AGGREGATOR_HOSTS.any { host == it || host.endsWith(".$it") }) "" else host
    }

    private fun titleTail(article: NewsArticle): String =
        if (article.title.contains(" - ")) article.title.substringAfterLast(" - ").lowercase() else ""

    /** The tab an article was fetched for; used when its publisher's country isn't known. */
    fun fallbackRegion(category: String): NewsRegion = when (category) {
        "indian" -> NewsRegion.INDIA
        "myanmar" -> NewsRegion.MYANMAR
        "international" -> NewsRegion.INTERNATIONAL
        else -> NewsRegion.BANGLADESH
    }

    /** Bump when the rules change, so regions stored by older rules are recomputed. */
    const val VERSION = 2

    private val AGGREGATOR_HOSTS = listOf(
        "news.google.com", "google.com", "bing.com", "yahoo.com", "duckduckgo.com", "reddit.com"
    )

    private val MYANMAR_MEDIA_MARKERS = listOf(
        "irrawaddy", "mizzima", "myanmar now", "myanmar-now", "dvb.no", "democratic voice of burma",
        "narinjara", "frontier myanmar", "frontiermyanmar", "eleven myanmar", "elevenmyanmar",
        "bnionline", "burma news international", "global new light of myanmar", "khit thit",
        "development media group", ".mm/"
    )

    private val INDIA_MEDIA_MARKERS = listOf(
        // Bengali names
        "আনন্দবাজার", "এই সময়", "সংবাদ প্রতিদিন", "আজকাল", "বর্তমান পত্রিকা", "গণশক্তি",
        "উত্তরবঙ্গ সংবাদ", "পুবের কলম", "টিভি৯", "জি ২৪", "এবিপি আনন্দ", "কলকাতা", "ত্রিপুরা",
        "একদিন", "দৈনিক স্টেটসম্যান",
        // English names & domains
        "anandabazar", "eisamay", "ei samay", "sangbadpratidin", "sangbad pratidin", "aajkaal",
        "bartamanpatrika", "ganashakti", "uttarbangasambad", "puberkalom", "puber kalom", "banglahunt",
        "bangla hunt", "tv9", "zee 24", "zee24", "zeenews", "abp ananda", "abpananda", "abp live",
        "abplive", "times of india", "timesofindia", "hindustan times", "hindustantimes", "ndtv",
        "india today", "indiatoday", "the hindu", "thehindu", "indian express", "indianexpress",
        "telegraph india", "telegraphindia", "news18", "republic world", "republicworld",
        "the wire", "thewire", "scroll.in", "northeast now", "nenow", "sentinel assam",
        "sentinelassam", "assam tribune", "assamtribune", "eastmojo", "shillong times",
        "theshillongtimes", "tripura", "kolkata", "the statesman", "thestatesman", "thewall.in",
        "ekdin", "dainik jagran", "jagran", "amar ujala", "amarujala", "aaj tak", "aajtak",
        "bhaskar", "livehindustan", "press trust of india", "aninews", "firstpost",
        "deccan herald", "deccanherald", "theprint", "economic times", "economictimes",
        "livemint", "outlookindia", "wionews", "india tv", "indiatvnews", "oneindia"
    )

    private val BD_MEDIA_MARKERS = listOf(
            // Bengali Names
            "প্রথম আলো", "যুগান্তর", "কালের কণ্ঠ", "ইত্তেফাক", "সমকাল", "মানবজমিন",
            "নয়া দিগন্ত", "নয়া দিগন্ত", "বাংলাদেশ প্রতিদিন", "জনকণ্ঠ", "ইনকিলাব",
            "বণিক বার্তা", "আজকের পত্রিকা", "দেশ রূপান্তর", "যায়যায়দিন", "যায়যায়দিন",
            "বিডিনিউজ", "বাংলানিউজ", "ঢাকা পোস্ট", "জাগো নিউজ", "রাইজিংবিডি",
            "বার্তা২৪", "সারাবাংলা", "বাংলা ট্রিবিউন", "ঢাকা টাইমস", "সময় টিভি",
            "সময় টিভি", "সময় নিউজ", "সময় নিউজ", "যমুনা টিভি", "যমুনা নিউজ",
            "চ্যানেল আই", "চ্যানেল ২৪", "একাত্তর", "এনটিভি", "আরটিভি", "ইন্ডিপেন্ডেন্ট",
            "কালবেলা", "আমাদের সময়", "আমাদের সময়", "ভোরের কাগজ", "ডেইলি স্টার",
            "ঢাকা ট্রিবিউন", "বাসস", "ইউএনবি", "দৈনিক বাংলা", "প্রতিদিনের বাংলাদেশ",
            "প্রতিদিনের সংবাদ", "খবরের কাগজ", "মানবকণ্ঠ", "আলোকিত বাংলাদেশ", "ডিবিসি",
            "নিউজ ২৪", "এটিএন", "দীপ্ত টিভি", "একুশে টিভি", "বৈশাখী টিভি", "মাছরাঙা",
            "নাগরিক টিভি", "দেশ টিভি", "ঢাকা মেইল", "ঢাকা প্রকাশ", "দৈনিক আজাদী",
            "দৈনিক পূর্বকোণ", "চট্টগ্রাম প্রতিদিন", "সিলেট ভিউ", "কক্সবাজার নিউজ",
            "পার্বত্য নিউজ", "প্রিয়.কম", "জুমবাংলা", "বার্তা বাজার", "বাংলা ইনসাইডার",
            "বিডি জার্নাল", "অধিকার", "আমার সংবাদ", "নয়া শতাব্দী", "নয়া শতাব্দী",
            "দৈনিক সংগ্রাম", "দৈনিক করতোয়া", "পূর্বাঞ্চল", "সোনার দেশ", "বিডি২৪লাইভ",
            "দিনকাল", "রেডিও টুডে", "জাগো এফএম", "সকালের সময়", "সকালের সময়",
            "খোলা কাগজ", "সময়ের আলো", "সময়ের আলো", "আলোকিত সকাল", "ঢাকা টুডে",
            // English Names & Domains
            "prothom alo", "prothomalo", "jugantor", "kaler kantho", "kalerkantho",
            "ittefaq", "samakal", "manab zamin", "manabzamin", "mzamin",
            "naya diganta", "nayadiganta", "bangladesh pratidin", "bd-pratidin",
            "janakantha", "inqilab", "bonik barta", "bonikbarta", "ajker patrika",
            "ajkerpatrika", "desh rupantor", "deshrupantor", "jaijaidin",
            "bdnews24", "banglanews24", "dhaka post", "dhakapost", "jago news",
            "jagonews", "risingbd", "barta24", "sarabangla", "bangla tribune",
            "banglatribune", "dhaka times", "dhakatimes", "somoy", "jamuna",
            "channel i", "channelionline", "channel 24", "channel24", "ekattor",
            "ntvbd", "ntv online", "rtvonline", "independent24", "independent tv",
            "kalbela", "amader shomoy", "amadershomoy", "bhorer kagoj", "bhorerkagoj",
            "vorerkagoj", "the daily star", "thedailystar", "dailystar.net",
            "dhaka tribune", "dhakatribune", "business standard", "tbsnews", "tbs news",
            "new age", "newagebd", "financial express", "thefinancialexpress",
            "daily sun", "daily-sun", "daily observer", "observerbd", "bssnews",
            "unb.com", "united news of bangladesh", "dainik bangla", "dainikbangla",
            "protidiner bangladesh", "protidinerbangladesh", "protidiner sangbad",
            "protidinersangbad", "khaborer kagoj", "khaborerkagoj", "manobkantha",
            "alokito bangladesh", "alokitobangladesh", "dbc news", "dbcnews",
            "news24bd", "atn bangla", "atn news", "deepto", "ekushey", "boishakhi",
            "maasranga", "nagorik", "desh tv", "desh.tv", "dhaka mail", "dhakamail",
            "dhaka prokash", "dhakaprokash", "dainik azadi", "dainikazadi",
            "purbokone", "ctgpratidin", "sylhetview", "coxsbazarnews", "ukhiyanews",
            "parbattanews", "zoombangla", "dmpnews", "ispr", "bahannonews",
            "shampratikdeshkal", "sharebiz", "arthosuchak", "orthomail",
            "banglainsider", "bdjournal", "opinionbd", "amarsangbad", "amar-sangbad",
            "bbarta24", "campuslive", "odhikar.news", "viewsbangladesh", "karatoa",
            "sonalisangbad", "purbanchal", "bd24live", "bdmorning", "bdtoday",
            "bangladarpan", "banglarbarta", "bangladesh post", "bangladeshpost",
            "daily asian age", "dailyasianage", "bangladesh monitor", "dhaka courier",
            "energy bangla", "textile today", "cvoice24", "shomoyer alo", "shomoyeralo",
            "khola kagoj", "kholakagoj", "deshkal", "bartabazar", "dhakatoday"
    )
}
