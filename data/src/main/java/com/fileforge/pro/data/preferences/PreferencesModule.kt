package com.fileforge.pro.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext ctx: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { java.io.File(ctx.filesDir, "datastore/fileforge.preferences_pb") }
        )
}
