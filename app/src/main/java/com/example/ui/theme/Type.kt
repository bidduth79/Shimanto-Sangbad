package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val HindSiliguriFontFamily = FontFamily(
    Font(R.font.hind_siliguri, FontWeight.Normal),
    Font(R.font.hind_siliguri, FontWeight.Medium),
    Font(R.font.hind_siliguri, FontWeight.SemiBold),
    Font(R.font.hind_siliguri, FontWeight.Bold)
)

/**
 * The app's six text sizes. Every Text uses one of these (through [Typography] below), so the app
 * reads as one consistent design. Sizes are in `sp`: they follow each phone's screen density and
 * the user's font-size setting, and the layouts wrap or ellipsize rather than break when enlarged.
 *
 * Bengali conjuncts and vowel signs reach above and below the Latin line box, so line heights are
 * a little more generous than Material's defaults.
 */
private fun style(size: Int, weight: FontWeight, lineHeight: Int) = TextStyle(
    fontFamily = HindSiliguriFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp
)

/** Screen titles and the app name. */
private val Display = style(22, FontWeight.Bold, 30)

/** Top-story headline, dialog and empty-state titles. */
private val Title = style(17, FontWeight.Bold, 25)

/** News card headlines: the text people read most. */
private val Headline = style(15, FontWeight.SemiBold, 23)

/** Descriptions, list rows, text fields and most buttons. */
private val Body = style(13, FontWeight.Normal, 19)

/** Chips, tags and compact buttons. */
private val Label = style(12, FontWeight.Medium, 17)

/** Time, source, counts and other small print. */
private val Caption = style(11, FontWeight.Normal, 15)

val Typography = Typography(
    displaySmall = Display,
    headlineLarge = Display,
    headlineMedium = Display,
    headlineSmall = Title,
    titleLarge = Display,
    titleMedium = Title,
    titleSmall = Headline,
    bodyLarge = Headline,
    bodyMedium = Body,
    bodySmall = Caption,
    labelLarge = Label,
    labelMedium = Label,
    labelSmall = Caption
)
