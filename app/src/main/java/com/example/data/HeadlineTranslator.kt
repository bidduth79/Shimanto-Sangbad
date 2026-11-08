package com.example.data

import android.util.LruCache
import com.google.android.gms.tasks.Task
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Bengali translations of Hindi and English headlines, done on the phone with ML Kit (free, works
 * offline once the ~30 MB language model has been downloaded on first use). Bengali headlines are
 * left alone.
 */
@Singleton
class HeadlineTranslator @Inject constructor() {
    private val cache = LruCache<String, String>(800)
    private val translators = ConcurrentHashMap<String, Translator>()
    private val modelLock = Mutex()
    private val modelReady = ConcurrentHashMap<String, Boolean>()

    /** The Bengali translation, or null for Bengali text or when translation is unavailable. */
    suspend fun translate(text: String): String? {
        val source = sourceLanguageOf(text) ?: return null
        cache.get(text)?.let { return it }
        val translator = translators.getOrPut(source) {
            Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(source)
                    .setTargetLanguage(TranslateLanguage.BENGALI)
                    .build()
            )
        }
        return runCatching {
            if (modelReady[source] != true) {
                modelLock.withLock {
                    if (modelReady[source] != true) {
                        translator.downloadModelIfNeeded().await()
                        modelReady[source] = true
                    }
                }
            }
            translator.translate(text).await()
        }.getOrNull()?.takeIf { it.isNotBlank() && it != text }?.also { cache.put(text, it) }
    }

    companion object {
        /** Hindi (Devanagari) or English (Latin letters only); null when it is Bengali or mixed. */
        fun sourceLanguageOf(text: String): String? {
            val letters = text.filter { it.isLetter() }
            if (letters.length < 6) return null
            if (letters.any { it in 'ঀ'..'৿' }) return null
            if (letters.count { it in 'ऀ'..'ॿ' } > letters.length / 3) return TranslateLanguage.HINDI
            if (letters.all { it.code < 0x250 }) return TranslateLanguage.ENGLISH
            return null
        }
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}
