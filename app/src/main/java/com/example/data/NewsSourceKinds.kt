package com.example.data

/**
 * Kind of a news story's publisher, for the home screen's chips (TV / national / local / other).
 * Sources look like "পত্রিকা • ইত্তেফাক", "Google • Bd24live" or "Facebook • …"; the part after the
 * dot is the publisher. Facebook posts count as "other"; a publisher the rules don't recognise is a
 * national outlet, since news searches only return news sites.
 */
object NewsSourceKinds {
    fun publisherOf(article: NewsArticle): String =
        article.source.substringAfter(" • ", article.source).trim()

    fun kindOf(article: NewsArticle): SourceKind {
        if (searchMediumOf(article) == SearchMedium.FACEBOOK) return SourceKind.OTHER
        return YouTubeChannelDirectory.kindFromName(publisherOf(article)) ?: SourceKind.NATIONAL
    }
}
