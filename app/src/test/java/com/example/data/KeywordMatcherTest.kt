package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeywordMatcherTest {
    private val keywords = listOf(
        "বিজিবি", "বর্ডার গার্ড বাংলাদেশ", "BGB", "বিএসএফ", "BSF",
        "সীমান্ত", "বাংলাদেশ ভারত সীমান্ত", "সীমান্ত সংবাদ"
    )

    private fun about(title: String) = KeywordMatcher.matches(title, keywords)

    @Test
    fun `headlines that were wrongly shown are rejected`() {
        // Real off-topic results that search engines returned for these keywords
        assertFalse(about("পাংশায় ৫৫তম জাতীয় সমবায় দিবস উদযাপন - Bd24live"))
        assertFalse(about("সমবায়ের মাধ্যমে প্রান্তিক জনগোষ্ঠীর সামাজিক নিরাপত্তা নিশ্চিত করতে হবে - কালের কণ্ঠ"))
        assertFalse(about("মাগুরায় খাদ্যে ভেজাল রোধে কঠোর ব্যবস্থার সিদ্ধান্ত - দৈনিক ইনকিলাব"))
        assertFalse(about("Two more children die with measles symptoms - Bangladesh Sangbad Sangstha (BSS)"))
        assertFalse(about("COP31 in Antalya: People Must be at the Centre of Climate Action - IOM"))
        assertFalse(about("India's new passport rules: Who gets exempted - Gulf Business"))
    }

    @Test
    fun `bengali keywords match with grammatical suffixes`() {
        assertTrue(about("কানাইঘাট-জৈন্তাপুর সীমান্তে দুই দিনের অভিযানে ৪২ লাখ টাকার পণ্য জব্দ"))
        assertTrue(about("নেত্রকোনা বিজিবির অভিযানে ৭০ বোতল ভারতীয় মদ আটক"))
        assertEquals("বিএসএফ", KeywordMatcher.matchingKeyword("দীপাবলিতে বিএসএফ জওয়ানদের সম্মান", keywords))
    }

    @Test
    fun `english aliases match the bengali keyword`() {
        assertEquals("বিজিবি", KeywordMatcher.matchingKeyword("BGB seizes Indian liquor in Netrokona", keywords))
        assertTrue(about("Border Guard Bangladesh steps up patrol"))
        assertTrue(about("BSF jawan killed near Bangladesh border"))
    }

    @Test
    fun `german civil code BGB is not the border guard`() {
        assertFalse(about("Suspension of the Statute of Limitations under §§ 203, 204, 205 BGB"))
        assertFalse(about("Hemmung der Verjährung nach BGB einfach erklärt"))
    }

    @Test
    fun `generic english border needs south asia context`() {
        assertFalse(about("Mexico border wall update"))
        assertTrue(about("Tension at Bangladesh border after firing"))
    }

    @Test
    fun `publisher name after the dash does not count`() {
        assertFalse(about("Market prices rise again - সীমান্ত বাংলা"))
    }

    @Test
    fun `acronyms must stand alone`() {
        assertNull(KeywordMatcher.matchingKeyword("BSFX shares jump in Dhaka", listOf("BSF")))
    }

    @Test
    fun `hindi headlines from indian channels match through their aliases`() {
        assertTrue(KeywordMatcher.matches("बांग्लादेश बॉर्डर पर बीएसएफ की बड़ी कार्रवाई", listOf("বিএসএফ")))
        assertTrue(KeywordMatcher.matches("बीएसएफ ने घुसपैठिए को पकड़ा", listOf("BSF")))
        assertFalse(KeywordMatcher.matches("सीमा हैदर का नया वीडियो", listOf("বিএসএফ", "বিজিবি")))
    }
}
