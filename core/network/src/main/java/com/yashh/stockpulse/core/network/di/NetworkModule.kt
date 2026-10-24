package com.yashh.stockpulse.core.network.di

import com.yashh.stockpulse.core.network.NetworkConstants
import com.yashh.stockpulse.core.network.api.FinnhubApi
import com.yashh.stockpulse.core.network.websocket.StockWebSocketClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val originalHttpUrl = original.url

            // Add API key to every request
            val url = if (originalHttpUrl.queryParameter("token") == null) {
                originalHttpUrl.newBuilder().addQueryParameter("token", NetworkConstants.API_KEY).build()
            } else originalHttpUrl

            val requestBuilder = original.newBuilder().url(url)
            chain.proceed(requestBuilder.build())
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(NetworkConstants.TIMEOUT_SEC, TimeUnit.SECONDS)
            .readTimeout(NetworkConstants.TIMEOUT_SEC, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(NetworkConstants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideFinnhubApi(retrofit: Retrofit): FinnhubApi {
        return retrofit.create(FinnhubApi::class.java)
    }

    @Provides
    @Singleton
    fun provideWebSocketClient(okHttpClient: OkHttpClient): StockWebSocketClient {
        return StockWebSocketClient(okHttpClient)
    }
}
