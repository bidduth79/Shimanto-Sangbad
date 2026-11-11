package com.example.data

/**
 * Turns the user's keywords into Google News queries that ask for the keyword IN THE HEADLINE.
 *
 * Why (measured on 2026-11-10): a Google News search returns at most 100 stories, ranked by
 * relevance, and a plain keyword matches anywhere in an article. Most of those 100 only mention the
 * word in passing and are then rejected by [KeywordMatcher], while real headline matches beyond the
 * first 100 are never seen. Asking for the headline directly (`intitle:`) returned 1.5 to 5 times
 * more usable stories per search, and a short time window (`when:3h`) returns the newest ones that
 * the whole-day search leaves out.
 */
object SearchQueries {

    /** One thing to search for, and the user keyword it stands for. */
    data class Term(val text: String, val keyword: String)

    /** Region words asked for anywhere in the article when a term is too generic on its own. */
    const val REGION_CONTEXT = "(Bangladesh OR Myanmar OR Rohingya OR BSF OR BGB OR Tripura)"

    /** Whole words to search for where the matcher works with a stem. */
    private val STEM_FORMS = mapOf(
        "smuggl" to listOf("smuggling", "smuggled", "smugglers", "smuggler"),
        "infiltrat" to listOf("infiltration", "infiltrators", "infiltrator", "infiltrated")
    )

    /** Bengali-script search terms: the keywords and their Bengali aliases. */
    fun bengaliTerms(keywords: List<String>): List<Term> = terms(keywords) { NewsFilters.isBengali(it) }

    /** Latin-script search terms: the keywords and their English aliases. */
    fun englishTerms(keywords: List<String>): List<Term> = terms(keywords, ::isLatin)

    /**
     * Every keyword and alias accepted by [wanted], once. A term that merely extends another one is
     * left out: every headline containing "ভারত সীমান্ত" also contains "সীমান্ত", so searching the
     * shorter term already finds it.
     */
    private fun terms(keywords: List<String>, wanted: (String) -> Boolean): List<Term> {
        val all = keywords
            .flatMap { keyword -> KeywordMatcher.termsFor(keyword).filter(wanted).map { Term(it.trim(), keyword) } }
            .filter { it.text.isNotBlank() }
            .distinctBy { it.text.lowercase() }
        return all.filter { term ->
            val words = wordsOf(term.text)
            all.none { other ->
                val otherWords = wordsOf(other.text)
                otherWords.size < words.size && words.containsAll(otherWords)
            }
        }
    }

    private fun wordsOf(text: String): List<String> = text.lowercase().split(' ', '-').filter { it.isNotBlank() }

    private fun isLatin(text: String): Boolean =
        text.any { it.isLetter() } && text.all { !it.isLetter() || it.code < 128 }

    /** The query pieces asking for [term] in the headline (several for a stem such as "smuggl"). */
    private fun inTitleForms(term: String): List<String> {
        val clean = term.trim()
        STEM_FORMS[clean.lowercase()]?.let { forms -> return forms.map { "intitle:$it" } }
        return listOf(if (clean.contains(' ')) "intitle:\"$clean\"" else "intitle:$clean")
    }

    /** `intitle:বিজিবি`, `intitle:"বর্ডার গার্ড"`. */
    fun inTitle(term: String): String = join(inTitleForms(term))

    /** One query for several terms: `(intitle:বিজিবি OR intitle:"বর্ডার গার্ড")`. */
    fun anyInTitle(terms: List<Term>): String = join(terms.flatMap { inTitleForms(it.text) })

    private fun join(forms: List<String>): String =
        if (forms.size == 1) forms[0] else forms.joinToString(" OR ", prefix = "(", postfix = ")")

    /**
     * English query for [terms]. Generic words ("border") always come with [REGION_CONTEXT];
     * acronyms (BGB, BSF) need it only in [global] (US/UK) editions, where "BGB" is usually the
     * German civil code. The headline rule in [KeywordMatcher] still has the final say.
     */
    fun english(terms: List<Term>, global: Boolean): String {
        val needsContext = terms.any {
            KeywordMatcher.isGenericEnglishTerm(it.text) || (global && KeywordMatcher.isShortAcronym(it.text))
        }
        val base = anyInTitle(terms)
        return if (needsContext) "$base $REGION_CONTEXT" else base
    }
}
