package com.shanufx.hotspotx.di

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.telephony.TelephonyManager
import androidx.room.Room
import com.shanufx.hotspotx.data.db.HotspotDatabase
import com.shanufx.hotspotx.data.db.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): HotspotDatabase =
        Room.databaseBuilder(ctx, HotspotDatabase::class.java, "hotspotx.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideDeviceDao(db: HotspotDatabase): DeviceDao           = db.deviceDao()
    @Provides fun provideUsageDao(db: HotspotDatabase): UsageSnapshotDao      = db.usageSnapshotDao()
    @Provides fun provideScheduleDao(db: HotspotDatabase): ScheduleDao        = db.scheduleDao()
    @Provides fun provideSessionDao(db: HotspotDatabase): SessionDao          = db.sessionDao()
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideWifiManager(@ApplicationContext ctx: Context): WifiManager =
        ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    @Provides @Singleton
    fun provideConnectivityManager(@ApplicationContext ctx: Context): ConnectivityManager =
        ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @Provides @Singleton
    fun provideTelephonyManager(@ApplicationContext ctx: Context): TelephonyManager =
        ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
}
