package com.example.data

/**
 * Tidies headlines before they are matched, stored or shown. Every rule comes from junk seen in
 * real stored headlines (2026-11-14: 1878 news headlines, 253 video titles):
 *
 *  - " - Publisher" tails Google News appends (the publisher is shown separately),
 *  - "| BGB | Gazipur | Channel" tag tails and hashtags of video and Facebook titles,
 *  - symbols put inside words to dodge Facebook's filters ("হা*মলা", "অ/বৈধ", "গু'লি", "অভি-যান"),
 *  - emoji, the poster's name before a Facebook post ("Md Sahalom: …", "Daily Jugantor. . …"),
 *  - "[Prothom Alo]" prefixes, a headline repeated after " / ", HTML entities, invisible
 *    characters, runs of spaces and "!!!".
 *
 * Joining marks Bengali needs (ZWJ/ZWNJ, as in "র‍্যাব") are kept. Pure, so it is unit-tested.
 */
object HeadlineCleaner {

    /** Bengali letters and signs; Bengali digits (০-৯) are not part of a word piece. */
    private fun isBengali(c: Char) = c in 'ঀ'..'৿' && c !in '০'..'৯'

    /** Clean form of [title]; [publisher] is the source's name, so a matching " - name" tail goes. */
    fun clean(title: String, publisher: String = ""): String {
        if (title.isBlank()) return title.trim()
        val cleaned = clean(title, publisher, keepHashtagWords = false)
        // A title made only of hashtags ("#গাজীপুর #বিজিবি | Samakal News") keeps their words
        if (cleaned.split(' ').count { w -> w.any(Char::isLetter) } >= 3) return cleaned
        // Never return nothing: an all-junk title is better than an empty card
        return clean(title, publisher, keepHashtagWords = true).ifBlank { cleaned.ifBlank { title.trim() } }
    }

    private fun clean(title: String, publisher: String, keepHashtagWords: Boolean): String {
        var t = decodeEntities(title)
        t = INVISIBLE.replace(t, "").replace(' ', ' ')
        t = BRACKET_PREFIX.replace(t, "")
        t = dropPublisherTail(t, publisher)
        t = dropPosterPrefix(t)
        t = if (keepHashtagWords) t.replace("#", " ") else HASHTAG.replace(t, " ")
        t = EMOJI.replace(t, " ")
        t = unObfuscate(t)
        t = dropTagSegments(t)
        t = dropRepeatedHalf(t)
        t = FILLER.replace(t, " ")
        t = REPEATED_MARKS.replace(t) { it.value.first().toString() }
        t = SPACED_DOTS.replace(t, " ")
        t = SPACES.replace(t, " ").trim()
        t = t.trim { it == '|' || it == '-' || it == ':' || it == '·' || it == ',' || it.isWhitespace() }
        return t
    }

    // ---------------------------------------------------------------- entities and invisible text

    private val NUMERIC_ENTITY = Regex("&#(x[0-9a-fA-F]+|\\d+);")
    private val NAMED_ENTITIES = mapOf(
        "&amp;" to "&", "&quot;" to "\"", "&apos;" to "'", "&lt;" to "<", "&gt;" to ">", "&nbsp;" to " ",
        "&ndash;" to "–", "&mdash;" to "—", "&lsquo;" to "‘", "&rsquo;" to "’", "&ldquo;" to "“", "&rdquo;" to "”",
        "&hellip;" to "…"
    )

    private fun decodeEntities(text: String): String {
        var t = text
        repeat(2) { // "&amp;#39;" needs two passes
            NAMED_ENTITIES.forEach { (k, v) -> t = t.replace(k, v) }
            t = NUMERIC_ENTITY.replace(t) { m ->
                val code = m.groupValues[1].let { if (it.startsWith("x")) it.drop(1).toIntOrNull(16) else it.toIntOrNull() }
                if (code != null && code in 1..0x10FFFF) String(Character.toChars(code)) else m.value
            }
        }
        return t
    }

    /** Zero-width space, BOM, soft hyphen, word joiner. ZWJ/ZWNJ (U+200C/D) are real Bengali. */
    private val INVISIBLE = Regex("[\\u200B\\uFEFF\\u00AD\\u2060]")

    // ---------------------------------------------------------------- prefixes and tails

    /** "[Prothom Alo] headline" (older custom feeds). */
    private val BRACKET_PREFIX = Regex("^\\s*\\[[^\\]]{1,40}]\\s*")

    private fun dropPublisherTail(text: String, publisher: String): String {
        val i = text.lastIndexOf(" - ")
        if (i <= 0) return text
        val tail = text.substring(i + 3).trim().trimEnd('.', '…')
        if (tail.isEmpty() || tail.length > 60) return text
        val pub = publisher.substringAfter(" • ").trim()
        val same = tail.equals("Facebook", ignoreCase = true) ||
            (pub.isNotEmpty() && (normalize(tail) == normalize(pub) ||
                normalize(pub).contains(normalize(tail)) || normalize(tail).contains(normalize(pub))))
        return if (same) text.substring(0, i) else text
    }

    private fun normalize(s: String) = s.lowercase().filter { it.isLetterOrDigit() }

    /**
     * Facebook posts begin with whoever posted them: "Md Sahalom: …", "Daily Jugantor. . …".
     * Only a short Latin-script name counts, so a Bengali "বিজিবি: …" headline is left alone.
     */
    private val POSTER_COLON = Regex("^[A-Z][A-Za-z0-9.'’\\- ]{1,40}:\\s+(?=\\S)")
    private val POSTER_DOTS = Regex("^[A-Za-z0-9][A-Za-z0-9.'’ \\-]{1,40}?\\.\\s*\\.\\s+")

    private fun dropPosterPrefix(text: String): String {
        POSTER_DOTS.find(text)?.let { return text.substring(it.range.last + 1) }
        val m = POSTER_COLON.find(text) ?: return text
        val name = m.value.substringBefore(':').trim()
        // "LIVE: …", "BREAKING: …" and English headlines ("BGB: Border sealed") are not names
        return if (name.split(' ').size in 2..4 && name.uppercase() != name) text.substring(m.range.last + 1) else text
    }

    private val HASHTAG = Regex("#[\\p{L}\\p{M}\\p{N}_]+|#(?=\\s|$)")
    private val EMOJI = Regex(
        "[\\x{1F000}-\\x{1FAFF}\\x{2600}-\\x{27BF}\\x{2B00}-\\x{2BFF}\\x{1F1E6}-\\x{1F1FF}\\x{FE0F}\\x{20E3}\\x{2190}-\\x{21FF}\\x{2300}-\\x{23FF}\\x{25A0}-\\x{25FF}]"
    )

    /**
     * " | " separated titles: segments of one to three words are tags or channel names
     * ("BGB", "Gazipur", "DBC NEWS"); longer segments are the headline and stay.
     */
    private fun dropTagSegments(text: String): String {
        if (!text.contains('|')) return text
        val parts = text.split('|').map { it.trim() }.filter { it.isNotEmpty() }
        val kept = parts.filter { it.split(' ').count { w -> w.any(Char::isLetter) } >= 4 }
        return when {
            kept.isEmpty() -> parts.maxByOrNull { it.length } ?: text
            else -> kept.joinToString(" | ")
        }
    }

    /** "Headline / Headline (with more words)": an RSS title followed by its own description. */
    private fun dropRepeatedHalf(text: String): String {
        val i = text.indexOf(" / ")
        if (i < 15) return text
        val first = text.substring(0, i).trim()
        val rest = text.substring(i + 3).trim()
        return if (rest.startsWith(first.take(20))) first else text
    }

    /** Bengali "details in the comments" bait. */
    private val FILLER = Regex("বিস্তারিত\\s+কমেন্টে\\s*[.…]*")
    private val REPEATED_MARKS = Regex("([!?।,])\\1+")
    private val SPACED_DOTS = Regex("(?:\\s\\.){2,}\\s?|(?<=\\S)\\s\\.\\s\\.(?=\\s)")
    private val SPACES = Regex("\\s{2,}")

    // ---------------------------------------------------------------- symbols hidden inside words

    private val QUOTE_MARKS = setOf('\'', '‘', '’', '`', '"', '“', '”', '´', '\\')

    /**
     * Repairs words split by marks, one word (run of non-space characters) at a time and in three
     * steps, so each step measures the pieces left by the one before:
     *  1. "হা*মলা" → a star between Bengali letters always goes. Quote marks go when a word has
     *     two or more ("ই'য়া'বা", "অ'ভি'যা'নে"); a single one goes unless what follows is a case
     *     ending ("গু'লির" → "গুলির", but "বিজিবি’র" stays).
     *  2. "অ/বৈধ", "স/ন্ত্রা/সীদের" → slashes go when a piece of the word is at most three
     *     characters ("ঢাকা/গাজীপুর" stays).
     *  3. "অভি-যান", "পি-স্ত-ল", "গু-লি" → hyphens go when every piece is at most three characters
     *     and a single hyphen is not followed by a case ending ("বিএসএফ-এর", "শ্রমিক-পুলিশ",
     *     "ইয়াবা-অস্ত্র" stay).
     */
    private fun unObfuscate(text: String): String =
        SPACE_SPLIT.split(text).joinToString("") { part ->
            if (part.isBlank()) part else fixHyphens(fixSlashes(fixQuotesAndStars(part)))
        }

    private val SPACE_SPLIT = Regex("(?<=\\s)|(?=\\s)")

    /**
     * Runs of [isMark] characters that sit between two Bengali letters, as index ranges
     * ("স'\হিংসতা" has one run of two marks).
     */
    private fun markRuns(word: String, isMark: (Char) -> Boolean): List<IntRange> {
        val runs = ArrayList<IntRange>()
        var i = 1
        while (i < word.length - 1) {
            if (isMark(word[i]) && isBengali(word[i - 1])) {
                var end = i
                while (end + 1 < word.length && isMark(word[end + 1])) end++
                if (end + 1 < word.length && isBengali(word[end + 1])) runs += i..end
                i = end + 1
            } else {
                i++
            }
        }
        return runs
    }

    /** Bengali letters in each piece of [word] between the [runs]. */
    private fun pieceSizes(word: String, runs: List<IntRange>): List<Int> {
        val starts = listOf(0) + runs.map { it.last + 1 }
        val ends = runs.map { it.first } + word.length
        return starts.zip(ends) { a, b -> word.substring(a, b).count(::isBengali) }
    }

    private fun lettersAfter(word: String, run: IntRange): String = word.substring(run.last + 1).takeWhile(::isBengali)

    private fun removeRuns(word: String, runs: Collection<IntRange>): String {
        if (runs.isEmpty()) return word
        val drop = runs.flatMap { it.toList() }.toSet()
        return word.filterIndexed { i, _ -> i !in drop }
    }

    private fun fixQuotesAndStars(word: String): String {
        val stars = markRuns(word) { it == '*' }
        val quotes = markRuns(word) { it in QUOTE_MARKS }
        val dropQuotes = when {
            quotes.size >= 2 -> quotes
            quotes.size == 1 && (quotes[0].count() > 1 || lettersAfter(word, quotes[0]) !in SUFFIXES) -> quotes
            else -> emptyList()
        }
        return removeRuns(word, stars + dropQuotes)
    }

    private fun fixSlashes(word: String): String {
        val slashes = markRuns(word) { it == '/' }
        if (slashes.isEmpty()) return word
        return if (pieceSizes(word, slashes).any { it <= 3 }) removeRuns(word, slashes) else word
    }

    private fun fixHyphens(word: String): String {
        val hyphens = markRuns(word) { it == '-' }
        if (hyphens.isEmpty()) return word
        val split = pieceSizes(word, hyphens).all { it <= 3 } &&
            (hyphens.size >= 2 || lettersAfter(word, hyphens[0]) !in SUFFIXES)
        return if (split) removeRuns(word, hyphens) else word
    }

    /** Case endings written after a hyphen ("বিএসএফ-এর", "ঢাকা-তে"): never part of a split word. */
    private val SUFFIXES = setOf("এর", "র", "য়ের", "য়", "তে", "কে", "রা", "দের", "এ", "ও", "ই", "টি", "টা")
}
