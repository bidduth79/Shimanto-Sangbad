package com.example.ui.intro

/**
 * Splits text into the units a typewriter effect should reveal one at a time.
 *
 * Revealing Bengali character by character shows broken intermediate shapes: a consonant with a
 * visible hasanta ("ন্") before the conjunct "ন্ত" forms, or a bare consonant before its vowel sign.
 * A unit here is a whole written syllable cluster: a base character plus any following
 * hasanta + consonant (conjuncts) and its vowel signs / marks. Other scripts fall back to one
 * character per unit.
 */
fun typingUnits(text: String): List<String> {
    val units = mutableListOf<StringBuilder>()
    var previous: Char? = null
    for (c in text) {
        val joinsPrevious = previous != null && (isCombiningMark(c) || previous == HASANTA || c == ZWJ || c == ZWNJ)
        if (joinsPrevious && units.isNotEmpty()) units.last().append(c) else units.add(StringBuilder().append(c))
        previous = c
    }
    return units.map { it.toString() }
}

private const val HASANTA = '্'
private const val ZWJ = '‍'
private const val ZWNJ = '‌'

/** Bengali signs that attach to the preceding letter: candrabindu, anusvara, visarga, nukta, vowel signs, hasanta, au length mark. */
private fun isCombiningMark(c: Char): Boolean =
    c in 'ঁ'..'ঃ' || c == '়' || c in 'া'..'্' || c == 'ৗ' || c in 'ৢ'..'ৣ'
