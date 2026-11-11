package com.example.data

/**
 * Decides whether a headline is really about one of the user's keywords.
 *
 * Search engines match a query anywhere in an article (body, sidebar, tags), so their results are
 * full of stories that merely mention a word somewhere. Only the HEADLINE counts here:
 *  - the keyword itself appears in it (Bengali suffixes like "বিজিবির", "সীমান্তে" still match), or
 *  - one of its known aliases does ("বিজিবি" ↔ "BGB" ↔ "Border Guard Bangladesh").
 * Multi-word keywords need every word, in any order.
 * Short Latin acronyms (BGB, BSF) must stand alone and, in a non-Bengali headline, come with
 * South-Asia context — so a German "BGB" (civil code) story is not mistaken for the border guard.
 */
object KeywordMatcher {

    /** The user keyword the headline is about, or null when it is about none of them. */
    fun matchingKeyword(title: String, keywords: List<String>): String? {
        val headline = headlineOf(title)
        val bengaliHeadline = NewsFilters.isBengali(headline)
        for (keyword in compile(keywords)) {
            if (keyword.terms.any { matchesTerm(headline, bengaliHeadline, it) }) return keyword.name
        }
        return null
    }

    fun matches(title: String, keywords: List<String>): Boolean = matchingKeyword(title, keywords) != null

    /** Every keyword the headline is about (a story can be about both "বিজিবি" and "সীমান্ত"). */
    fun matchingKeywords(title: String, keywords: List<String>): List<String> {
        val headline = headlineOf(title)
        val bengaliHeadline = NewsFilters.isBengali(headline)
        return compile(keywords).filter { kw -> kw.terms.any { matchesTerm(headline, bengaliHeadline, it) } }.map { it.name }
    }

    /** The keyword plus its aliases (translations and common alternative names). */
    fun termsFor(keyword: String): List<String> {
        val clean = keyword.trim()
        if (clean.isBlank()) return emptyList()
        val aliases = ALIASES.entries.firstOrNull { it.key.equals(clean, ignoreCase = true) }?.value.orEmpty()
        return listOf(clean) + aliases
    }

    /**
     * True for a single generic English word ("border") or a short Latin acronym ("BGB", "BSF"):
     * in a non-Bengali headline such a term only counts together with South-Asia context.
     */
    fun needsRegionContext(term: String): Boolean = isGenericEnglishTerm(term) || isShortAcronym(term)

    /** "border", "smuggl", "infiltrat": everyday words that mean nothing without a place. */
    fun isGenericEnglishTerm(term: String): Boolean = term.trim().lowercase() in GENERIC_ENGLISH_TERMS

    /** "BGB", "BSF": up to four Latin letters, which other countries use for other things. */
    fun isShortAcronym(term: String): Boolean {
        val word = term.trim().lowercase()
        return word.isNotEmpty() && word.length <= 4 && word.all { it in 'a'..'z' } && word !in GENERIC_ENGLISH_TERMS
    }

    /** A keyword or alias, pre-split and lower-cased once instead of once per headline. */
    private class Term(val words: List<String>, val needsRegionContext: Boolean)
    private class CompiledKeyword(val name: String, val terms: List<Term>)

    // The keyword list rarely changes, so its compiled form is reused across thousands of headlines
    @Volatile private var cache: Pair<List<String>, List<CompiledKeyword>> = emptyList<String>() to emptyList()

    private fun compile(keywords: List<String>): List<CompiledKeyword> {
        val cached = cache
        if (cached.first == keywords) return cached.second
        val compiled = keywords.map { keyword ->
            CompiledKeyword(keyword, termsFor(keyword).mapNotNull { term ->
                val words = term.lowercase().split(' ').filter { it.isNotBlank() }
                if (words.isEmpty()) return@mapNotNull null
                Term(words, needsRegionContext = needsRegionContext(term))
            })
        }
        cache = keywords to compiled
        return compiled
    }

    /** Google News appends " - Publisher" to titles; a publisher's name must not count as a match. */
    private fun headlineOf(title: String): String =
        (if (title.contains(" - ")) title.substringBeforeLast(" - ") else title).lowercase()

    private fun matchesTerm(headline: String, bengaliHeadline: Boolean, term: Term): Boolean {
        if (!term.words.all { word -> containsWord(headline, word) }) return false
        // A generic word like "border", or an acronym like "BGB", in an English headline needs South-Asia context
        if (term.needsRegionContext && !bengaliHeadline) {
            val word = term.words[0]
            return REGION_CONTEXT.any { it != word && headline.contains(it) }
        }
        return true
    }

    /**
     * Latin words must stand alone ("bsf" must not match inside "bsfx"); Bengali words may carry
     * suffixes ("সীমান্তে", "বিজিবির"), so they match as a prefix of a longer word too.
     */
    private fun containsWord(headline: String, word: String): Boolean {
        if (NewsFilters.isBengali(word)) return headline.contains(word)
        var from = 0
        while (true) {
            val i = headline.indexOf(word, from)
            if (i < 0) return false
            val before = if (i == 0) ' ' else headline[i - 1]
            val after = if (i + word.length >= headline.length) ' ' else headline[i + word.length]
            // English stems like "smuggl" may continue ("smuggling"); acronyms must end at a boundary
            val endOk = !after.isLetterOrDigit() || word in STEMS
            if (!before.isLetterOrDigit() && endOk) return true
            from = i + 1
        }
    }

    private val STEMS = setOf("smuggl", "infiltrat")
    private val GENERIC_ENGLISH_TERMS = setOf("border", "smuggl", "infiltrat")

    /** Words that place an English headline in this app's region. */
    private val REGION_CONTEXT = listOf(
        "bangladesh", "bangladeshi", "dhaka", "india", "indian", "myanmar", "rohingya", "arakan",
        "rakhine", "teknaf", "cox's bazar", "sylhet", "chattogram", "chittagong", "kolkata", "bengal",
        "tripura", "assam", "meghalaya", "mizoram", "bgb", "bsf"
    )

    private val ALIASES: Map<String, List<String>> = mapOf(
        "বিজিবি" to listOf("BGB", "Border Guard Bangladesh", "বর্ডার গার্ড", "बीजीबी"),
        "বর্ডার গার্ড বাংলাদেশ" to listOf("বিজিবি", "BGB", "Border Guard Bangladesh"),
        "BGB" to listOf("বিজিবি", "Border Guard Bangladesh", "বর্ডার গার্ড", "बीजीबी"),
        "বিএসএফ" to listOf("BSF", "Border Security Force", "बीएसएफ"),
        "BSF" to listOf("বিএসএফ", "Border Security Force", "बीएसएफ"),
        "সীমান্ত" to listOf("border"),
        "সীমান্ত সংবাদ" to listOf("সীমান্ত", "border"),
        "বাংলাদেশ ভারত সীমান্ত" to listOf(
            "ভারত সীমান্ত", "India Bangladesh border", "Bangladesh India border", "Indo-Bangla border",
            "बांग्लादेश बॉर्डर", "बांग्लादेश सीमा"
        ),
        "রোহিঙ্গা" to listOf("Rohingya", "रोहिंग्या"),
        "মিয়ানমার" to listOf("মায়ানমার", "Myanmar", "म्यांमार"),
        "মায়ানমার" to listOf("মিয়ানমার", "Myanmar"),
        "আরাকান" to listOf("Arakan", "Rakhine", "রাখাইন"),
        "অনুপ্রবেশ" to listOf("infiltrat", "পুশইন", "push-in"),
        "চোরাচালান" to listOf("smuggl", "চোরাকারবার"),
        "পুশইন" to listOf("push-in", "push in", "অনুপ্রবেশ"),
        "টেকনাফ" to listOf("Teknaf"),
        "তুমব্রু" to listOf("Tumbru"),
        "সেন্টমার্টিন" to listOf("St Martin", "Saint Martin", "সেন্ট মার্টিন"),
        "তিস্তা" to listOf("Teesta")
    )
}
