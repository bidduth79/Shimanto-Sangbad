package com.example.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.example.data.YouTubeApiService
import com.example.data.local.NewsDao
import com.example.data.local.NewsDatabase
import com.example.data.settings.SecretStore
import com.example.data.settings.SettingsRepository
import com.example.data.settings.SettingsStore
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/** App-wide singletons. Classes with an @Inject constructor need no entry here. */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /** One HTTP client (shared connection pool) for news, YouTube and link decoding. */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectionPool(ConnectionPool(16, 5, TimeUnit.MINUTES))
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    @Provides
    @Singleton
    fun provideYouTubeApi(client: OkHttpClient): YouTubeApiService = Retrofit.Builder()
        .baseUrl("https://www.googleapis.com/youtube/v3/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
        .build()
        .create(YouTubeApiService::class.java)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NewsDatabase = NewsDatabase.build(context)

    @Provides
    fun provideNewsDao(database: NewsDatabase): NewsDao = database.newsDao()

    @Provides
    fun provideVideoDao(database: NewsDatabase): com.example.data.local.VideoDao = database.videoDao()

    @Provides
    @Singleton
    @SettingsStore
    fun provideSettingsStore(@ApplicationContext context: Context): DataStore<Preferences> =
        SettingsRepository.createSettingsStore(context)

    @Provides
    @Singleton
    @SecretStore
    fun provideSecretStore(@ApplicationContext context: Context): DataStore<Preferences> =
        SettingsRepository.createSecretStore(context)
}
