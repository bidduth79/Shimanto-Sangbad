package com.example.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class NewsFiltersArchiveYearTest {
  private val repository = NewsFilters
  private val currentYear = Calendar.getInstance().get(Calendar.YEAR)

  @Test
  fun `old archive year in url is rejected`() {
    assertTrue(repository.hasOldArchiveYear("https://example.com/news/2019/05/border-story"))
  }

  @Test
  fun `current year in url is accepted`() {
    assertFalse(repository.hasOldArchiveYear("https://example.com/news/$currentYear/05/border-story"))
  }

  @Test
  fun `url without year is accepted`() {
    assertFalse(repository.hasOldArchiveYear("https://example.com/news/border-story"))
  }

  @Test
  fun `old year in title is rejected but last year is allowed`() {
    assertTrue(repository.hasOldArchiveYear("https://example.com/a", "সীমান্ত পরিস্থিতি ২০১৮ 2018"))
    assertFalse(repository.hasOldArchiveYear("https://example.com/a", "Review of ${currentYear - 1}"))
  }
}
