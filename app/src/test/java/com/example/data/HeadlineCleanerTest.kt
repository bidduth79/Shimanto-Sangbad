package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Test

/** Every case is a real headline stored by the app (2026-11-14). */
class HeadlineCleanerTest {

    private fun clean(title: String, publisher: String = "") = HeadlineCleaner.clean(title, publisher)

    @Test
    fun `publisher tail goes when it names the source`() {
        assertEquals("আশুলিয়ায় শ্রমিক-পুলিশ সংঘর্ষ, বিজিবি মোতায়েন", clean("আশুলিয়ায় শ্রমিক-পুলিশ সংঘর্ষ, বিজিবি মোতায়েন - Ntvbd", "Google • Ntvbd"))
        assertEquals("সীমান্ত পার হবার প্র‍্যাক্টিস করছেন আপা।", clean("সীমান্ত পার হবার প্র‍্যাক্টিস করছেন আপা। - Facebook", "Facebook"))
        // a " - " that is part of the headline stays
        assertEquals("গাংনী সীমান্তে বিজিবির অভিযানে মাদকসহ আটক-১ - ভিন্ন কথা", clean("গাংনী সীমান্তে বিজিবির অভিযানে মাদকসহ আটক-১ - ভিন্ন কথা", "Google • Meherpur News"))
    }

    @Test
    fun `symbols hidden inside words are removed`() {
        assertEquals(
            "রাজ্যে বিএসএফের উপর হামলার ঘটনার তীব্র নিন্দা জানালেন প্রখ্যাত বলিউড অভিনেতা অনুপম খের।",
            clean("রাজ্যে বিএসএফের উপর হা*মলার ঘ*টনার তী*ব্র নি*ন্দা জানালেন প্রখ্যাত বলিউড অভিনেতা অনুপম খের। - Facebook", "Facebook")
        )
        assertEquals("নিষিদ্ধ ছাত্রলীগের ধর্মঘট ঘিরে মিরপুরে কঠোর অবস্থানে বিজিবি", clean("নি‘ষি‘দ্ধ ছাত্রলীগের ধর্মঘট ঘিরে মিরপুরে ক‘ঠোর অবস্থানে বিজিবি - Facebook", "Facebook"))
        assertEquals("সীমান্ত দিয়ে অবৈধ অনুপ্রবেশ, অস্ত্র চোরাচালান এবং সাম্প্রতিক সহিংসতা", clean("সীমান্ত দিয়ে অ/বৈধ অনুপ্রবেশ, অ/স্ত্র চো/রাচালান এবং সাম্প্রতিক সহিং/সতা"))
        assertEquals("নাইক্ষ্যংছড়ি সীমান্তে ১১ বিজিবির অভিযান বিদেশি পিস্তল", clean("নাইক্ষ্যংছড়ি সীমান্তে ১১ বিজিবির অভি-যান বিদেশি পি-স্ত-ল"))
    }

    @Test
    fun `words split several times are joined whole, real joins inside them stay`() {
        assertEquals("রামু সীমান্ত দিয়ে বিনিময়ে ঢুকছে ইয়াবা-অস্ত্র", clean("রামু সীমান্ত দিয়ে বিনিময়ে ঢুকছে ই'য়া'বা-অ'স্ত্র - Facebook", "Facebook"))
        assertEquals("পাহাড়ি সন্ত্রাসীদের হাতে অস্ত্র", clean("পাহাড়ি স/ন্ত্রা/সীদের হাতে অ/স্ত্র | Hill Tracts | Army | BGB | Amar Desh"))
        assertEquals("সীমান্তে গুলিবিদ্ধ বাংলাদেশিকে ধরে নিয়ে গেছে বিএসএফ", clean("সীমান্তে গু/লি/বি/দ্ধ বাংলাদেশিকে ধরে নিয়ে গেছে বিএসএফ | Amar Desh"))
        assertEquals("BSF নিগ্রহের ঘটনায়, বিএসএফ'র পাশে", clean("BSF নিগ্রহের ঘটনায়, বিএসএফ'র পাশে"))
        assertEquals("পালংখালীতে অস্ত্রের মহড়া", clean("পালংখালীতে অ'স্ত্রে'র মহড়া"))
    }

    @Test
    fun `hashtag-only titles keep their words, numbered posters and double marks are handled`() {
        assertEquals("গাজীপুর নারায়ণগঞ্জ বিজিবি", clean("#গাজীপুর #নারায়ণগঞ্জ #বিজিবি | Samakal News"))
        assertEquals("BGB BorderGuardBangladesh industrialarea LawAndOrder", clean("#BGB #BorderGuardBangladesh #industrialarea #LawAndOrder - Facebook", "Facebook"))
        assertEquals("সীমান্তের ওপার থেকে সহিংসতার ইন্ধন", clean("Channel 24: সীমান্তের ওপার থেকে স'\\হিংসতার ইন্ধন - Facebook", "Facebook"))
    }

    @Test
    fun `real hyphens and slashes stay`() {
        assertEquals("রহিমপুর বাজারে বিএসএফ-এর সাঁড়াশি অভিযান!", clean("রহিমপুর বাজারে বিএসএফ-এর সাঁড়াশি অভিযান!"))
        assertEquals("জামালপুর সীমান্তে পুশ-ইনের চেষ্টা ব্যর্থ", clean("জামালপুর সীমান্তে পুশ-ইনের চেষ্টা ব্যর্থ"))
        assertEquals("ঢাকা/গাজীপুর শিল্পাঞ্চলে বিজিবি", clean("ঢাকা/গাজীপুর শিল্পাঞ্চলে বিজিবি"))
        assertEquals("বিজিবি-পুলিশের ওপর হামলা, আটক ৩", clean("বিজিবি-পুলিশের ওপর হামলা, আটক ৩"))
    }

    @Test
    fun `tags, hashtags, emoji and poster names go`() {
        assertEquals(
            "ভারতীয় গুলির জবাবে মর্টার মহড়া বিজিবির",
            clean("Mutahar Ahmed Badsa: ভারতীয় গু'লির জবাবে মর্টার মহড়া বিজিবির | BSF | Bordersecurity | Bangladesh | Ekattor TV - Facebook", "Facebook")
        )
        assertEquals(
            "ঢাকাসহ সতর্ক তিন জেলা, ৩৬ প্লাটুন বিজিবি মোতায়েন",
            clean("Daily Jugantor. . ঢাকাসহ সতর্ক তিন জেলা, ৩৬ প্লাটুন বিজিবি মোতায়েন | BGB | Gazipur | Narayanganj | Jugantor - Facebook", "Facebook")
        )
        assertEquals("মিরপুর-১০-এ বিজিবি মোতায়েন...", clean("মিরপুর-১০-এ বিজিবি মোতায়েন... #mirpur10 #BGB #NewsUpdate - Facebook", "Facebook"))
        assertEquals("ৰাজপথত BSFক আক্ৰমণকাৰীৰ দৰ্পচূৰ্ণ ইনাই-বিনাই কান্দোন অভিযুক্তৰ", clean("📌ৰাজপথত BSFক আক্ৰমণকাৰীৰ দৰ্পচূৰ্ণ 📌ইনাই-বিনাই কান্দোন অভিযুক্তৰ #Tripura #BSF - Facebook", "Facebook"))
        assertEquals("আশুলিয়ায় পুলিশের সঙ্গে সংঘর্ষ, বিজিবি মোতায়েন", clean("আশুলিয়ায় পুলিশের সঙ্গে সং*ঘর্ষ, বিজিবি মোতায়েন বিস্তারিত কমেন্টে ... - Facebook", "Facebook"))
    }

    @Test
    fun `entities, invisible characters, prefixes and repeats`() {
        assertEquals("পুটখালীতে ২১ বিজিবি’র অভিযান", clean("​পুটখালীতে ২১ বিজিবি’র অভিযান - কালের বিবর্তন", "Google • কালের বিবর্তন"))
        assertEquals("Bangladesh’s Hajj quota & BGB", clean("Bangladesh&#8217;s Hajj quota &amp; BGB"))
        assertEquals("গুলিতে আহত মনিরুল", clean("[দৈনিক মাথাভাঙ্গা] গুলিতে আহত মনিরুল"))
        assertEquals("স্মৃতি খাতুনসহ গ্রেফতার ৪, নগদ টাকা উদ্ধার", clean("স্মৃতি খাতুনসহ গ্রেফতার ৪, নগদ টাকা উদ্ধার / স্মৃতি খাতুনসহ গ্রেফতার ৪, নগদ টাকা উদ্ধার চুয়াডাঙ্গা সদর"))
        assertEquals("BSS-18 33 BGB platoons deployed", clean("BSS-18  33 BGB platoons deployed"))
        assertEquals("সীমান্তে উত্তেজনা!", clean("সীমান্তে উত্তেজনা!!!"))
    }

    @Test
    fun `headlines that are fine are not changed`() {
        listOf(
            "LIVE: Indian Army Celebrates Diwali Along LoC",
            "BGB: Border sealed after clash",
            "33 BGB platoons deployed in Dhaka, Gazipur, Narayanganj industrial zones",
            "টেকনাফ সীমান্তে ৫০ হাজার ইয়াবা জব্দ করেছে বিজিবি",
            "পুলিশ, বিজিবি, অধুনালুপ্ত র‌্যাব, আর সাদা পোশাকে গোয়েন্দা"
        ).forEach { assertEquals(it, clean(it)) }
    }
}
