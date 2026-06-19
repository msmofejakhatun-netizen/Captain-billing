package com.example.data.di

import android.content.Context
import com.example.BuildConfig
import com.example.data.database.AppDatabase
import com.example.data.database.LocalDatabaseDao
import com.example.data.remote.ApiService
import com.example.data.remote.SupabaseRealtimeClient
import com.example.data.repository.CaptainRepository
import com.example.data.repository.CaptainRepositoryImpl
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

interface AppContainer {
    val apiService: ApiService
    val realtimeClient: SupabaseRealtimeClient
    val repository: CaptainRepository
}

class AppContainerImpl(private val context: Context) : AppContainer {

    // Endpoints and keys can be configured easily or loaded from BuildConfigs
    private val apiBaseUrl: String by lazy {
        val url = BuildConfig.API_BASE_URL.trim()
        if ((url.startsWith("http://") || url.startsWith("https://")) && !url.contains("API_BASE_URL") && !url.contains("example.com")) {
            url
        } else {
            "https://backendrepo-production-7e7f.up.railway.app"
        }
    }
    private val supabaseUrl = BuildConfig.SUPABASE_URL
    private val supabaseKey = BuildConfig.SUPABASE_KEY

    private val moshi: Moshi = Moshi.Builder()
        .add(com.example.data.remote.TablesResponseAdapter())
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val prefs = context.getSharedPreferences("captain_prefs", Context.MODE_PRIVATE)
                val token = prefs.getString("jwt_token", null)
                val originalRequest = chain.request()
                if (!token.isNullOrEmpty()) {
                    val authenticatedRequest = originalRequest.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                    chain.proceed(authenticatedRequest)
                } else {
                    chain.proceed(originalRequest)
                }
            }
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    override val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(apiBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ApiService::class.java)
    }

    override val realtimeClient: SupabaseRealtimeClient by lazy {
        SupabaseRealtimeClient(okHttpClient, moshi, Dispatchers.IO)
    }

    private val localDatabaseDao: LocalDatabaseDao by lazy {
        AppDatabase.getDatabase(context).localDatabaseDao()
    }

    override val repository: CaptainRepository by lazy {
        CaptainRepositoryImpl(apiService, realtimeClient, localDatabaseDao, context, Dispatchers.IO)
    }
}
