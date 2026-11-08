package com.example.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import com.example.R
import com.example.data.HeadlineTranslator
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/** Settings switch: show Bengali translations under Hindi and English headlines. */
val LocalTranslateHeadlines = compositionLocalOf { true }

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TranslatorEntryPoint {
    fun headlineTranslator(): HeadlineTranslator
}

/** "অনুবাদ: …" under a Hindi or English headline; draws nothing for Bengali ones. */
@Composable
fun TranslatedLine(text: String, modifier: Modifier = Modifier) {
    if (!LocalTranslateHeadlines.current || HeadlineTranslator.sourceLanguageOf(text) == null) return
    val context = LocalContext.current
    val translator = remember {
        EntryPointAccessors.fromApplication(context.applicationContext, TranslatorEntryPoint::class.java).headlineTranslator()
    }
    val translated by produceState<String?>(initialValue = null, text) { value = translator.translate(text) }
    translated?.let {
        Text(
            text = stringResource(R.string.translation_prefix, it),
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
        )
    }
}
