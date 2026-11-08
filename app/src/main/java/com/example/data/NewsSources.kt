package com.example.data

/**
 * Built-in free news sources. Every feed URL and channel ID below was checked to be live
 * (RSS returning items / YouTube channel resolving to the named outlet) on 2026-11-08.
 */
enum class NewsRegion { BANGLADESH, INDIA, MYANMAR, INTERNATIONAL }

data class NewspaperFeed(
    val name: String,
    val url: String,
    val region: NewsRegion
)

object NewsSources {

    /** Source prefix used to tag articles that came straight from a newspaper's own RSS feed. */
    const val NEWSPAPER_SOURCE_PREFIX = "পত্রিকা"

    val newspaperFeeds = listOf(
        // Bangladesh — major national dailies, portals and TV news sites
        NewspaperFeed("প্রথম আলো", "https://www.prothomalo.com/feed/", NewsRegion.BANGLADESH),
        NewspaperFeed("জাগো নিউজ", "https://www.jagonews24.com/rss/rss.xml", NewsRegion.BANGLADESH),
        NewspaperFeed("ঢাকা পোস্ট", "https://www.dhakapost.com/rss/rss.xml", NewsRegion.BANGLADESH),
        NewspaperFeed("বার্তা২৪", "https://barta24.com/rss/rss.xml", NewsRegion.BANGLADESH),
        NewspaperFeed("বাংলা ট্রিবিউন", "https://www.banglatribune.com/feed/", NewsRegion.BANGLADESH),
        NewspaperFeed("ইত্তেফাক", "https://www.ittefaq.com.bd/feed/", NewsRegion.BANGLADESH),
        NewspaperFeed("দেশ রূপান্তর", "https://www.deshrupantor.com/feed/", NewsRegion.BANGLADESH),
        NewspaperFeed("আজকের পত্রিকা", "https://www.ajkerpatrika.com/feed", NewsRegion.BANGLADESH),
        NewspaperFeed("একাত্তর", "https://ekattor.tv/feed/", NewsRegion.BANGLADESH),
        NewspaperFeed("দৈনিক আজাদী", "https://dainikazadi.net/feed/", NewsRegion.BANGLADESH),
        NewspaperFeed("বিবিসি বাংলা", "https://feeds.bbci.co.uk/bengali/rss.xml", NewsRegion.BANGLADESH),
        NewspaperFeed("The Daily Star", "https://www.thedailystar.net/news/bangladesh/rss.xml", NewsRegion.BANGLADESH),
        NewspaperFeed("Dhaka Tribune", "https://www.dhakatribune.com/feed/", NewsRegion.BANGLADESH),
        NewspaperFeed("Prothom Alo English", "https://en.prothomalo.com/feed/", NewsRegion.BANGLADESH),
        // Bangladesh — from the RKB suite domain list (feeds with items from the last 48h)
        NewspaperFeed("ওয়ান নিউজ বিডি", "https://www.onenewsbd.com/feed", NewsRegion.BANGLADESH),
        NewspaperFeed("আজকের বাজার", "https://www.ajkerbazzar.com/feed/", NewsRegion.BANGLADESH),
        NewspaperFeed("Ajker Jamalpur", "https://ajkerjamalpur.com/feed/", NewsRegion.BANGLADESH),
        NewspaperFeed("আলোকিত বাংলাদেশ", "https://www.alokitobangladesh.com/rss/rss.xml", NewsRegion.BANGLADESH),
        NewspaperFeed("Amader Brahmanbaria", "https://amaderbrahmanbaria.com/feed/", NewsRegion.BANGLADESH),
        NewspaperFeed("Amar Noakhali", "https://www.amarnoakhali.com/feed", NewsRegion.BANGLADESH),
        NewspaperFeed("আমার সংবাদ", "https://www.amarsangbad.com/rss/rss.xml", NewsRegion.BANGLADESH),
        NewspaperFeed("অর্থসূচক", "https://www.arthosuchak.com/feed/", NewsRegion.BANGLADESH),
        NewspaperFeed("Asia Post", "https://www.asia-post.com/feed", NewsRegion.BANGLADESH),
        NewspaperFeed("Asian TV", "https://www.asiantvonline.com/rss-feed.xml", NewsRegion.BANGLADESH),
        NewspaperFeed("বাংলা ডেইলি ২৪", "https://www.bangladaily24.com/feed/", NewsRegion.BANGLADESH),
        NewspaperFeed("Bangla Hunt", "https://banglahunt.com/feed/", NewsRegion.INDIA),
        NewspaperFeed("বাংলানিউজ টোয়েন্টিফোর", "https://www.banglanews24.com/rss.xml", NewsRegion.BANGLADESH),
        NewspaperFeed("বার্তা বাংলা", "https://bartabangla.com/feed/", NewsRegion.BANGLADESH),
        NewspaperFeed("বার্তা বাজার", "https://bartabazar.com/feed/", NewsRegion.BANGLADESH),
        NewspaperFeed("bartakontho.com", "https://bartakontho.com/feed", NewsRegion.BANGLADESH),
        NewspaperFeed("বাংলাদেশ প্রতিদিন", "https://www.bd-pratidin.com/rss.xml", NewsRegion.BANGLADESH),
        NewspaperFeed("বিডি২৪লাইভ", "https://www.bd24live.com/feed/", NewsRegion.BANGLADESH),
        NewspaperFeed("বিডি টাইপ", "https://www.bdtype.com/public/feed", NewsRegion.BANGLADESH),
        NewspaperFeed("bijoytv.com", "https://bijoy.tv/feed", NewsRegion.BANGLADESH),
        NewspaperFeed("বগুড়া সংবাদ", "https://bograsangbad.com/feed", NewsRegion.BANGLADESH),
        NewspaperFeed("বাসস", "https://www.bssnews.net/rss/rss.xml", NewsRegion.BANGLADESH),
        NewspaperFeed("Chakaria News", "https://chakarianews.com/?feed=rss2", NewsRegion.BANGLADESH),
        NewspaperFeed("চ্যানেল২৪", "https://www.channel24bd.tv/rss/rss.xml", NewsRegion.BANGLADESH),
        NewspaperFeed("চ্যানেল আই", "https://www.channelionline.com/rss.xml", NewsRegion.BANGLADESH),
        NewspaperFeed("Chattagram News", "https://chattogramnews.com/feed", NewsRegion.BANGLADESH),
        NewspaperFeed("কক্সবাজার নিউজ", "https://www.coxsbazarnews.com/feed", NewsRegion.BANGLADESH),
        NewspaperFeed("চট্টগ্রাম বাংলানিউজ", "https://ctgbanglanews24.com/feed", NewsRegion.BANGLADESH),
        NewspaperFeed("ডেইলি বাংলাদেশ", "https://www.daily-bangladesh.com/rss", NewsRegion.BANGLADESH),
        NewspaperFeed("আমার দেশ", "https://www.dailyamardesh.com/feed", NewsRegion.BANGLADESH),
        NewspaperFeed("ইনকিলাব", "https://dailyinqilab.com/rss/rss.xml", NewsRegion.BANGLADESH),
        NewspaperFeed("জনকণ্ঠ", "https://www.dailyjanakantha.com/rss/rss.xml", NewsRegion.BANGLADESH),
        NewspaperFeed("dailynabochatona.com", "https://dailynabochatona.com/feed", NewsRegion.BANGLADESH),
        NewspaperFeed("Daily Sylhet", "https://dailysylhet.com/feed/", NewsRegion.BANGLADESH),
        NewspaperFeed("ভোরের পাতা", "https://www.dailyvorerpata.com/rss", NewsRegion.BANGLADESH),
        NewspaperFeed("Dainik Sylhet", "https://dainiksylhet.com/feed/", NewsRegion.BANGLADESH),
        NewspaperFeed("দেশেবিদেশে", "https://www.deshebideshe.com/feed/", NewsRegion.INTERNATIONAL),
        NewspaperFeed("দেশের সংবাদ", "https://deshersangbad.com/feed/", NewsRegion.BANGLADESH),
        NewspaperFeed("দেশ সংবাদ", "https://www.deshsangbad.com/rss", NewsRegion.BANGLADESH),
        NewspaperFeed("ঢাকা প্রতিদিন", "https://www.dhakaprotidin.com/feed/", NewsRegion.BANGLADESH),
        NewspaperFeed("একুশে টিভি", "https://www.ekushey-tv.com/rss/rss.xml", NewsRegion.BANGLADESH),
        NewspaperFeed("Face The People", "https://facethepeople.com.bd/rss", NewsRegion.BANGLADESH),
        NewspaperFeed("Ganadabi", "https://ganadabi.com/feed/", NewsRegion.BANGLADESH),
        NewspaperFeed("gnewsbd24.com", "https://gnewsbd24.com/feed/", NewsRegion.BANGLADESH),
        NewspaperFeed("কালবেলা", "https://www.kalbela.com/rss/latest-rss.xml", NewsRegion.BANGLADESH),
        NewspaperFeed("খবরপত্র", "https://khoborpatrabd.com/feed", NewsRegion.BANGLADESH),
        NewspaperFeed("খোলা কাগজ", "https://www.kholakagojbd.com/rss", NewsRegion.BANGLADESH),
        NewspaperFeed("খুলনাঞ্চল", "https://khulnanchal.com/?feed=rss2", NewsRegion.BANGLADESH),
        NewspaperFeed("লেটেস্ট বিডি নিউজ", "https://www.latestbdnews.com/feed/", NewsRegion.BANGLADESH),
        NewspaperFeed("Malay News Bangla", "https://www.malay.news/feed/", NewsRegion.INTERNATIONAL),
        NewspaperFeed("নিউজ ২৪", "https://www.mtnews24.com/feed/", NewsRegion.BANGLADESH),
        NewspaperFeed("Nagorik TV", "https://nagorik.com/feed/", NewsRegion.BANGLADESH),
        NewspaperFeed("newsbangla24.com", "https://www.newsbangla24.com/rss.xml", NewsRegion.BANGLADESH),
        NewspaperFeed("ডেইলি অবজার্ভার", "https://www.observerbd.com/rss", NewsRegion.BANGLADESH),
        NewspaperFeed("দৈনিক অধিকার", "https://odhikar.news/feed/", NewsRegion.BANGLADESH),
        NewspaperFeed("onabil.net", "https://onabil.net/?feed=rss2", NewsRegion.BANGLADESH),
        NewspaperFeed("অর্থসংবাদ", "https://orthosongbad.com/feed/", NewsRegion.BANGLADESH),
        NewspaperFeed("outlookbangla.com", "https://outlookbangla.com/feed/", NewsRegion.BANGLADESH),
        NewspaperFeed("পাহাড়বার্তা", "https://paharbarta.com/feed/", NewsRegion.BANGLADESH),
        NewspaperFeed("প্রতিক্ষণ", "https://www.protikhon.com/feed/", NewsRegion.BANGLADESH),
        NewspaperFeed("পুবের কলম", "https://www.puberkalom.com/feed", NewsRegion.INDIA),
        NewspaperFeed("Daily Purbanchal", "https://purbanchal.com/feed/", NewsRegion.BANGLADESH),
        NewspaperFeed("Rising Cumilla", "https://risingcumilla.com/feed/", NewsRegion.BANGLADESH),
        NewspaperFeed("সমকাল", "https://samakal.com/rss", NewsRegion.BANGLADESH),
        NewspaperFeed("সংবাদ", "https://sangbad.net/rss.xml", NewsRegion.BANGLADESH),
        NewspaperFeed("সংবাদ কণিকা", "https://sangbadkonika.com/feed/", NewsRegion.BANGLADESH),
        NewspaperFeed("এসএ টিভি", "https://www.satv.tv/feed/", NewsRegion.BANGLADESH),
        NewspaperFeed("শেয়ার বিজ", "https://www.sharebiz.net/rss/rss.xml", NewsRegion.BANGLADESH),
        NewspaperFeed("শেয়ার নিউজ ২৪", "https://sharenews24.com/rss/rss.xml", NewsRegion.BANGLADESH),
        NewspaperFeed("সময়ের আলো", "https://www.shomoyeralo.com/rss", NewsRegion.BANGLADESH),
        NewspaperFeed("সুপ্রভাত", "https://suprobhat.com/feed/", NewsRegion.BANGLADESH),
        NewspaperFeed("সুরমা টাইমস", "https://surmatimes.com/feed", NewsRegion.BANGLADESH),
        NewspaperFeed("The Bengali Times", "https://www.thebengalitimes.com/feed/", NewsRegion.INTERNATIONAL),
        NewspaperFeed("দ্য রিপোর্ট ২৪", "https://www.thereport24.com/rss/rss.xml", NewsRegion.BANGLADESH),
        NewspaperFeed("ঠিকানা", "https://thikananews.com/feed", NewsRegion.INTERNATIONAL),
        NewspaperFeed("উখিয়া নিউজ", "https://www.ukhiyanews.com/feed/", NewsRegion.BANGLADESH),
        NewspaperFeed("জুমবাংলা", "https://zoombangla.com/feed/", NewsRegion.BANGLADESH),
        // India (West Bengal / Tripura / Northeast border coverage)
        NewspaperFeed("সংবাদ প্রতিদিন", "https://www.sangbadpratidin.in/feed/", NewsRegion.INDIA),
        NewspaperFeed("আজকাল", "https://www.aajkaal.in/rss.xml", NewsRegion.INDIA),
        NewspaperFeed("এই সময়", "https://eisamay.com/stories.rss", NewsRegion.INDIA),
        NewspaperFeed("উত্তরবঙ্গ সংবাদ", "https://uttarbangasambad.com/feed/", NewsRegion.INDIA),
        NewspaperFeed("জাগরণ ত্রিপুরা", "https://jagarantripura.com/feed/", NewsRegion.INDIA),
        NewspaperFeed("একদিন", "https://www.ekdin-epaper.com/rss", NewsRegion.INDIA),
        NewspaperFeed("Kolkata24x7", "https://kolkata24x7.in/feed/", NewsRegion.INDIA),
        NewspaperFeed("Northeast Now", "https://nenow.in/feed", NewsRegion.INDIA),
        NewspaperFeed("The Hindu", "https://www.thehindu.com/news/national/feeder/default.rss", NewsRegion.INDIA),
        // Myanmar
        NewspaperFeed("The Irrawaddy", "https://www.irrawaddy.com/feed", NewsRegion.MYANMAR),
        // International
        NewspaperFeed("Al Jazeera", "https://www.aljazeera.com/xml/rss/all.xml", NewsRegion.INTERNATIONAL),
        NewspaperFeed("The Guardian", "https://www.theguardian.com/international/rss", NewsRegion.INTERNATIONAL)
    )

    /**
     * Bangladeshi news channels on YouTube. Uploads are read through the channel's "UU…" uploads
     * playlist, which costs 1 quota unit per call instead of 100 for a search.
     */
    val youtubeNewsChannels = listOf(
        "UCxHoBXkY88Tb8z1Ssj6CWsQ", // Somoy TV
        "UCN6sm8iHiPd0cnoUardDAnw", // Jamuna TV
        "UCATUkaOHwO9EP_W87zCiPbA", // Independent Television
        "UCtqvtAVmad5zywaziN6CbfA", // Ekattor TV
        "UCHLqIOMPk20w-6cFgkA90jw", // Channel 24
        "UCUvXoiDEKI8VZJrr58g4VAw", // DBC News
        "UCUDQdVsKssximyFwg4IxnOQ", // NTV News
        "UC2P5Fd5g41Gtdqf0Uzh8Qaw", // Rtv News
        "UCKlhfq1ILoAFav7iw5iCnfA", // ATN News
        "UCbgcYEdMsuypG2NJ-znBp3w", // ATN Bangla News
        "UCA4y9g_GRdNrZLH_u5LLSdw", // BanglaVision News
        "UCWVqdPTigfQ-cSNwG7O9MeA", // Ekhon TV
        "UCPREnbhKQP-hsVfsfKP-mCw", // News24
        "UCmCCTsDl-eCKw91shC7ZmMw", // Desh TV News
        "UCBQxpxkI2qLRAFR_6PNdwIA", // Ekushey Television
        "UCTSvHZkOStz5Ak2J_q6YRlA", // Boishakhi TV News
        "UCUxdUUZZNXrZprkqTja0Ltg", // Global TV News
        "UCMMCiB3he7fwFUvLBRMTaZQ", // SATV News
        "UC8uzDYcztPf-fICdGTowAAA", // Deepto News
        "UChPD2BQ2dJ-w00klQQkc-Qg", // BBC News Bangla
        "UCIIkOzwZIdLfHa4Q13dQrkw", // DW Bangla
        "UCeG7m5-AJ4I0H4EIleIty4Q", // Prothom Alo
        "UCsBqX8QguRdkawdlNbIL1dw", // Kaler Kantho
        "UCKbhCXpZwAmFN4UHeT8HTjA", // Daily Jugantor
        "UCnetEdV8EwzGn36f3pq50ZA", // Samakal News
        "UC0g7R-oqBXYZDIE8yPgT6rA", // Bangladesh Pratidin
        "UCRt2klyaxgx89vPF8cFMfnQ", // Kalbela News
        "UCFRxWDF-ZHgs0X6PckJVC9A", // Daily Ittefaq
        "UCWJkfdG6J2HUxOHrORuHZ2g", // Jago News
        "UCI_ANQpJ3dxPimvPszWg60w", // Dhaka Post
        "UCjFMRqrsABGBh-6eFUlMuAA", // bdnews24.com
        "UCPbpOnmqK8O_FC4ejdzDTHQ", // Bangla Tribune
        "UCB58cDLM3zZlgV6iIG9bdoQ", // Dhaka Tribune
        "UCtkUEY0iLmqCP92eVwvYXMg", // The Daily Star Bangladesh
        "UCqhC0YFetNVYsCJR3nPCSQQ", // Barta24
        // Added Nov 2026: channels that regularly post border/BGB reports (each checked as active)
        "UCC7AtFMKr0Mzd7B66MYWhxw", // Times Of Bangladesh
        "UCO2Ds5LXqzQSH6HMnJFWYBA", // UNB - United News of Bangladesh
        "UCQ3v4nNWdPOnFe691ZCon9A", // Shomoyer Alo
        "UC4o-4fJ_sNfz_AH9503JLUA", // Daily Sun
        "UCBoKYPIjkXcLhvHmHOJuPyQ", // Daily Sun Bangla
        "UC66GWNX7DG04XOMIee47Hwg", // mytvbd news
        "UC9zgYOQB6zkleAQjW53DLeQ", // mytv Bangladesh
        "UCDZwDKMh1kgfh9jQs8fpH6Q", // NPB News
        "UCXs9EVaxc1rQjTM9Ber2pAg", // Bhorer Kagoj Live
        "UCRpXgF0n4HQVWcZpPN2QXMQ", // Bhorer Kagoj News
        "UCMbO-c2g92KPgOzYKDsTbug", // Desh Rupantor
        "UCXmVpJx1jaHuDUc0hOvms1w", // Ajker Patrika
        "UC5sKprGjitm2ktc6ZW1cZXg", // Ajker Patrika Digital
        "UC8NcXMG3A3f2aFQyGTpSNww", // Channel i News
        "UCHR-p802PibDYG1btNJcOew", // Maasranga News
        "UC-NvlImq-FA3X62HjtGkvGQ", // Asian News
        "UCjIi0uZg4h5Ar8gvLDTrGwA", // GTV News
        "UCHd3pezHElEC3_jv8qTvjVw", // Nagorik TV News
        "UClzwimpLoZu9us9MwalLbtA", // Bangladesh Television
        "UC9b_MnX3HN9ABYT89QqYMTw", // Mohona TV
        "UCjYHHBaJWbqFVBKzyq495QA", // Ananda TV
        "UCaXcMdJaXNGlPMC1xlNWFdw", // Bijoy TV
        "UCsS8Tv-eBsgZzC6bZcI9c6g", // Naya Diganta
        "UCgbaV2akib7J5wpO-Sq4XBg", // The Daily ManabZamin
        "UC1RJ3aEdvBMJSinziVp0U2Q", // The Daily Inqilab
        "UCVBUCoStRou7DZtlTxmhAmQ", // Daily Amar Desh
        "UC7PwfNI2zuokhCctN9VymLg", // Protidiner Bangladesh
        "UCzfkD4dNxVlIhUW3xaM4uxw", // Banglanews24
        "UCpWiZlOEsWm27r3S346eIDQ", // Banglanews24 Country
        "UC01Q70O5uADZ4d5I80MU1Og", // Risingbd.com
        "UC-BJx_0um7cpaIpfXqqKEIA", // Sarabangla News
        "UCUNEzmQ3zcifrhn0HwCgjYg", // The Business Standard
        "UCdToTtQYK7AYsarULHfU1EQ", // Channel S News
        "UCEwqW2yZ5XP-g0AVIYBAKnQ", // Daily Bonik Barta
        "UC0GfqDQyXKHM-bkyHkAV-Ig", // Amader Shomoy
        "UCZax0pF8RN8CKHfXx2SrDeQ", // The Daily Sangram
        "UCgT0-UWZW2Fk_fmkglL3dOw", // Dhaka Times 24
        // Added after checking which outlets posted BGB videos in the past year
        "UCmAdYhT7htWR-zC1OOOigLQ", // Khaborpatra
        "UCbQ0yQCRS94ObpIdgROQEXA", // Khaborer Kagoj
        "UC7OudmOtSkcVSNVB8hSi1dg", // Khola Kagoj
        "UCSVRiIMxiEMSeABYufHDKQA", // Daily Gonokantho
        "UCBl9dksVqTm5fe3CBiUdllg", // Daily Janakantha
        "UCQncJ_zr35KKJrHKjHbOzPw", // Dinkal Digital
        "UCENjI4wmuVTWtduhQN8Ut9A", // Dainik Odhikar
        "UCv0kJ025utcN8SHCDDUaQEA", // Bangladesh Bulletin
        "UCYge972sGn13hN7bRMJtzmA", // Manobkantha News
        "UC_TvB-xjRJVcPjPyAEk5lWA", // Daily Jaijaidin
        "UCdu6q20-h1Vwyv1WLzUR5iA", // Sorejomin Barta
        "UCeU0BTbx7aDFfGSEF-2sZ5Q", // Chattogram 24
        "UCIS8vxDuiRtCNYn92NP94Lw", // Daily Karatoa
        "UCPczt8v8G_NtXh8vbTUOh6Q", // Daily Purbokone
        "UCNLMkA8j786HsqnI18ilwfA", // Barishal dot News
        "UCIv1CRETiZfcHyJBs9SGSww", // Sylhetview
        "UC0ESiUN0fy6sS_zxO5jOP-g", // Channel Nine BD
        "UCgt4pZnDTDs8lLPp2GOjLIw", // Channel S
        "UCGFPxFYEFx7c3qk3kyEACUw", // Noakhali TV
        "UCrbWzP4kllmFMr_uWphAbXg", // Channel One News (1News BD)
        "UCtLqtUKyLV1YNaahnkFcoRA", // Asia Post
        "UCDtQa8eupV5x8AMnOc0lwLQ", // Face The People
        "UCI8cVSnGKbA3p8Oq2yFp61A", // Dainik Nabochatona
        "UCRorOml5erbG8j_uqh8Sx6g", // The Press24
        "UCUbXo6u2RZWqFm2H-m_YPYA", // Provat News
        "UCqqgO2MgmsfVK4-_4Z79E_w", // Campus Times
        "UCG3Xz6B9N-dPsDC_inqdMOw", // NewsG24
        "UC0xxD1tl7DZa1N7mZcibzJA", // Bangla Edition
        "UCyQbsIEL-oJVH3QlAwIWu-g", // Somoyer Konthosor
        "UC4VSoZhXOrcHl0SC_WxbJ3w", // Sunbd 24
        "UCVTMj-PWsxNAPSYCHbxuMFQ"  // Suprovat Dhaka
    )

    /** Indian news channels (Bengali, Hindi and English), polled for the India video tab. */
    val youtubeIndianChannels = listOf(
        // West Bengal / Tripura Bengali TV
        "UCv3rFzn-GHGtqzXiaq3sWNg", // ABP Ananda
        "UCdF5Q5QVbYstYrTfpgUl0ZA", // Zee 24 Ghanta
        "UCHCR4UFsGwd_VcDa0-a4haw", // TV9 Bangla
        "UCbf0XHULBkTfv2hBjaaDw9Q", // News18 Bangla
        "UCNvCQpcafnbW4KQ8X7oQ9kg", // Kolkata TV
        "UCJ3I6MHOz5exARlTW_meOGQ", // Calcutta News
        "UCycQauzU5n3DwQdgk3cw91A", // R Plus News
        "UC7GSS1hr39KcjdsKqAO76aQ", // Asianet News Bangla
        "UCdfOu3dJAYCc99mUy8hyfMw", // Aaj Tak Bangla
        "UCajVjEHDoVn_AHsunUZz_EQ", // Republic Bangla
        // Bengali newspapers and portals
        "UCESY9RzEDPsb9lRLMxAirwQ", // Anandabazar Patrika
        "UCLPCvTftdZpZQSM-mckfqXQ", // Sangbad Pratidin
        "UCWoPoR2Jho2tbFV4TttLNdA", // Bartaman
        "UCxhf49wOiJeWiUzkBfuAEcQ", // The Wall News
        "UCPYZ_TMaFSsAGyedFpwyufA", // Ei Samay
        "UCkwTQGJelg1F-gV2ZmfPg8Q", // Aajkaal
        "UCXDw7ADM9CnoGPEk3aSsHaQ", // Uttarbanga Sambad TV
        // National (Hindi / English)
        "UCt4t-jeY85JegMlZ-E5UWtA", // Aaj Tak
        "UC9CYT9gSNLevX5ey2_6CK0Q", // NDTV India
        "UCZFMm1mMw0F81Z37aaEzTUA", // NDTV
        "UCYPvAwZP8pZhSMW8qs7cVCw", // India Today
        "UCRWFSbif-RFENbBrSiez1DA", // ABP News
        "UCIvaYmXn910QMdemBG3v1pQ", // Zee News
        "UC7wXt18f2iA3EDXeqAVuKng", // Republic Bharat
        "UCwqusr8YDwM-3mEYTDeJHzw", // Republic World
        "UCef1-8eOpJgud7szVPlZQAQ", // CNN-News18
        "UC6RJ7-PaXg6TIH2BzZfTV7w", // Times Now
        "UC_gUM8rL-Lrg6O3adPW9K1g", // WION
        "UCckHqySbfy5FcPP6MD_S-Yg", // Times Of India
        "UC3njZ48-FDxLleBYaP0SZIg", // The Hindu
        "UCJEDFSxHHOW1PpBccdSxOTA", // The Indian Express
        "UCm7lHFkt2yB_WzL67aruVBQ", // Hindustan Times
        "UCKwucPzHZ7zCUIf7If-Wo1g", // DD News
        "UCx8Z14PpntdaxCt2hakbQLQ", // The Lallantop
        "UChu9XfMGcw0xZxaOmdNeXAA", // Bangla Hunt
        "UCu43V7JTZtMXIf7s2XciAUQ"  // Syandan TV (Tripura)
    )

    /**
     * True when [title] matches [query]: a plain substring match, or — for multi-word English
     * variants like "Bangladesh border" — every word appearing somewhere in the title.
     */
    fun titleMatches(title: String, query: String): Boolean {
        val t = title.lowercase()
        val q = query.trim().lowercase()
        if (q.isBlank()) return false
        if (t.contains(q)) return true
        val words = q.split(' ').filter { it.length > 1 }
        return words.size > 1 && words.all { t.contains(it) }
    }
}
