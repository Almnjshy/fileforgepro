package com.fileforge.pro.core.common

/**
 * Marker for singletons that need explicit cleanup on Application.onTerminate
 * or process death. The Application class iterates over all registered
 * [Releasable] instances.
 */
interface Releasable {
    fun release()
}
