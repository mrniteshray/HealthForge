package com.niteshray.xapps.healthforge.core.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.niteshray.xapps.healthforge.core.dataStore.DataStore
import com.niteshray.xapps.healthforge.core.dataStore.PreferenceKey
import com.niteshray.xapps.healthforge.core.utils.Constants.cerebasApiKey
import com.niteshray.xapps.healthforge.feature.auth.data.Authentication
import com.niteshray.xapps.healthforge.feature.auth.domain.repo.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        val logger = HttpLoggingInterceptor()
        logger.setLevel(HttpLoggingInterceptor.Level.BODY)
        return logger
    }

    @Provides
    @Singleton
    fun provideFirestore() : FirebaseFirestore{
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        dataStore : DataStore
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
//            .addInterceptor { chain ->
//                val token = runBlocking {
//                    dataStore.getStringOnce(PreferenceKey.AUTH_TOKEN)
//                }
//
//                val newRequest = chain.request().newBuilder()
//                    .addHeader("Authorization", "Bearer $token")
//                    .build()
//
//                chain.proceed(newRequest)
//            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().create()

    @Provides
    @Singleton
    @Named("BackendRetrofit")
    fun provideBackendRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://healthforgee-backend.vercel.app/api/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()


    @Provides
    @Singleton
    @Named("CerebrasRetrofit")
    fun provideCerebrasRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        val cerebrasOkHttpClient = okHttpClient.newBuilder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $cerebasApiKey")
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .build()

        return Retrofit.Builder()
            .baseUrl("https://api.cerebras.ai/")
            .client(cerebrasOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApiService(@Named("BackendRetrofit") retrofit: Retrofit): Authentication =
        retrofit.create(Authentication::class.java)

    @Provides
    @Singleton
    fun provideAuthRepository(authApiService: Authentication , firebaseAuth: FirebaseAuth): AuthRepository =
        AuthRepository(authApiService, firebaseAuth)

    @Provides
    @Singleton
    fun provideCerebrasApi(@Named("CerebrasRetrofit") retrofit: Retrofit): CerebrasApi =
        retrofit.create(CerebrasApi::class.java)
}
