package com.fileforge.pro.app.di

import com.fileforge.pro.data.repository.FavoritesRepositoryImpl
import com.fileforge.pro.data.repository.FileRepositoryImpl
import com.fileforge.pro.data.repository.RecentRepositoryImpl
import com.fileforge.pro.data.repository.SearchHistoryRepositoryImpl
import com.fileforge.pro.data.repository.StorageSourceRepositoryImpl
import com.fileforge.pro.data.repository.ViewSettingsRepositoryImpl
import com.fileforge.pro.domain.repository.FavoritesRepository
import com.fileforge.pro.domain.repository.FileRepository
import com.fileforge.pro.domain.repository.FileTypeRegistry
import com.fileforge.pro.domain.repository.FileOperationRepository
import com.fileforge.pro.domain.repository.RecentRepository
import com.fileforge.pro.domain.repository.SearchHistoryRepository
import com.fileforge.pro.domain.repository.SearchRepository
import com.fileforge.pro.domain.repository.StorageSourceRepository
import com.fileforge.pro.domain.repository.ViewSettingsRepository
import com.fileforge.pro.domain.repository.WindowManagerRepository
import com.fileforge.pro.engine.metadata.FileTypeRegistryImpl
import com.fileforge.pro.engine.window.WindowManagerImpl
import com.fileforge.pro.engine.fileoperation.FileOperationEngine
import com.fileforge.pro.engine.search.SearchEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindFileRepository(impl: FileRepositoryImpl): FileRepository

    @Binds @Singleton
    abstract fun bindStorageSourceRepository(impl: StorageSourceRepositoryImpl): StorageSourceRepository

    @Binds @Singleton
    abstract fun bindFavoritesRepository(impl: FavoritesRepositoryImpl): FavoritesRepository

    @Binds @Singleton
    abstract fun bindRecentRepository(impl: RecentRepositoryImpl): RecentRepository

    @Binds @Singleton
    abstract fun bindSearchHistoryRepository(impl: SearchHistoryRepositoryImpl): SearchHistoryRepository

    @Binds @Singleton
    abstract fun bindViewSettingsRepository(impl: ViewSettingsRepositoryImpl): ViewSettingsRepository

    @Binds @Singleton
    abstract fun bindFileTypeRegistry(impl: FileTypeRegistryImpl): FileTypeRegistry

    @Binds @Singleton
    abstract fun bindWindowManager(impl: WindowManagerImpl): WindowManagerRepository

    @Binds @Singleton
    abstract fun bindFileOperationRepository(impl: FileOperationEngine): FileOperationRepository

    @Binds @Singleton
    abstract fun bindSearchRepository(impl: SearchEngine): SearchRepository
}
