package com.example.data.settings

import javax.inject.Qualifier

/** The main settings DataStore (backed up). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SettingsStore

/** The DataStore holding the YouTube API key (excluded from backup). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SecretStore
