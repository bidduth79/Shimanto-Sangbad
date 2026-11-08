package com.example.ui.intro

import org.junit.Assert.assertEquals
import org.junit.Test

class TypingUnitsTest {

    @Test
    fun `tagline is revealed in whole syllable clusters`() {
        assertEquals(
            listOf("সী", "মা", "ন্তে", "র", " ", "অ", "ত", "ন্দ্র", " ", "প্র", "হ", "রী"),
            typingUnits("সীমান্তের অতন্দ্র প্রহরী")
        )
    }

    @Test
    fun `units always join back into the original text`() {
        val text = "বর্ডার গার্ড বাংলাদেশ"
        assertEquals(text, typingUnits(text).joinToString(""))
    }

    @Test
    fun `latin text is one character per unit`() {
        assertEquals(listOf("B", "G", "B"), typingUnits("BGB"))
    }
}
