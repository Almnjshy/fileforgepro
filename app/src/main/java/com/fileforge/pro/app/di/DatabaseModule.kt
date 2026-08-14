package com.fileforge.pro.app.di

import com.fileforge.pro.data.database.dao.FavoriteDao
import com.fileforge.pro.data.database.dao.RecentDao
import com.fileforge.pro.data.database.dao.SearchHistoryDao
import com.fileforge.pro.data.database.dao.WindowStateDao
import com.fileforge.pro.data.database.FileForgeDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideFavoriteDao(db: FileForgeDatabase): FavoriteDao = db.favoriteDao()

    @Provides @Singleton
    fun provideRecentDao(db: FileForgeDatabase): RecentDao = db.recentDao()

    @Provides @Singleton
    fun provideSearchHistoryDao(db: FileForgeDatabase): SearchHistoryDao = db.searchHistoryDao()

    @Provides @Singleton
    fun provideWindowStateDao(db: FileForgeDatabase): WindowStateDao = db.windowStateDao()
}
