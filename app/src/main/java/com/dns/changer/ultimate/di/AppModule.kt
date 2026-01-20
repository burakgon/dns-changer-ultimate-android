package com.dns.changer.ultimate.di

import android.content.Context
import com.dns.changer.ultimate.ads.AdMobManager
import com.dns.changer.ultimate.data.preferences.AppLockPreferences
import com.dns.changer.ultimate.data.preferences.DnsPreferences
import com.dns.changer.ultimate.data.repository.DnsRepository
import com.dns.changer.ultimate.service.DnsConnectionManager
import com.dns.changer.ultimate.service.DnsSpeedTestService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDnsPreferences(
        @ApplicationContext context: Context
    ): DnsPreferences = DnsPreferences(context)

    @Provides
    @Singleton
    fun provideAppLockPreferences(
        @ApplicationContext context: Context
    ): AppLockPreferences = AppLockPreferences(context)

    @Provides
    @Singleton
    fun provideDnsRepository(
        preferences: DnsPreferences
    ): DnsRepository = DnsRepository(preferences)

    @Provides
    @Singleton
    fun provideDnsConnectionManager(
        @ApplicationContext context: Context,
        repository: DnsRepository
    ): DnsConnectionManager = DnsConnectionManager(context, repository)

    @Provides
    @Singleton
    fun provideDnsSpeedTestService(): DnsSpeedTestService = DnsSpeedTestService()

    @Provides
    @Singleton
    fun provideAdMobManager(
        @ApplicationContext context: Context
    ): AdMobManager = AdMobManager(context)
}
