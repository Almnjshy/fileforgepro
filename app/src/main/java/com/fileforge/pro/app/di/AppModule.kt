package com.fileforge.pro.app.di

import android.content.Context
import com.fileforge.pro.core.common.AppDispatchers
import com.fileforge.pro.core.common.DefaultAppDispatchers
import com.fileforge.pro.core.common.Releasable
import com.fileforge.pro.core.permissions.PermissionManager
import com.fileforge.pro.core.storage.StorageProviderRegistry
import com.fileforge.pro.data.database.FileForgeDatabase
import com.fileforge.pro.engine.filesystem.LocalFilesystemProvider
import com.fileforge.pro.engine.thumbnail.ThumbnailEngine
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
    fun provideAppDispatchers(): AppDispatchers = DefaultAppDispatchers()

    @Provides
    @Singleton
    fun providePermissionManager(@ApplicationContext ctx: Context): PermissionManager =
        PermissionManager(ctx)

    @Provides
    @Singleton
    fun provideStorageProviderRegistry(
        @ApplicationContext ctx: Context,
    ): StorageProviderRegistry {
        val registry = StorageProviderRegistry()
        registry.register(LocalFilesystemProvider.forInternal(ctx))
        LocalFilesystemProvider.probeRemovable(ctx).forEach { registry.register(it) }
        return registry
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): FileForgeDatabase =
        androidx.room.Room.databaseBuilder(
            ctx, FileForgeDatabase::class.java, FileForgeDatabase.DB_NAME
        ).fallbackToDestructiveMigration().build()

    @Provides
    fun provideReleasables(
        thumbnailEngine: ThumbnailEngine,
    ): Set<@JvmSuppressWildcards Releasable> = setOf(thumbnailEngine)
}
