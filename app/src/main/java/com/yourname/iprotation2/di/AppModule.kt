package com.yourname.iprotation2.di

import android.content.Context
import androidx.room.Room
import com.yourname.iprotation2.data.local.AppDatabase
import com.yourname.iprotation2.data.local.IpHistoryDao
import com.yourname.iprotation2.data.local.ProxyDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.Dispatcher
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "iprotation.db").build()

    @Provides
    fun provideProxyDao(db: AppDatabase): ProxyDao = db.proxyDao()

    @Provides
    fun provideHistoryDao(db: AppDatabase): IpHistoryDao = db.historyDao()

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient {
        val dispatcher = Dispatcher().apply {
            maxRequests = 64
            maxRequestsPerHost = 32
        }
        return OkHttpClient.Builder().dispatcher(dispatcher).build()
    }
}
