package com.fileforge.pro.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.fileforge.pro.core.common.Logger
import com.fileforge.pro.core.common.Releasable
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FileForgeApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var releasables: java.util.Set<@JvmSuppressWildcards Releasable>

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        Logger.i("FileForge", "FileForgeApp.onCreate()")
    }

    override fun onTerminate() {
        releasables.forEach { it.release() }
        super.onTerminate()
    }
}
