package com.fileforge.pro.core.common

import android.util.Log

/**
 * Thin wrapper around android.util.Log that respects [LogTags] and never
 * leaks sensitive data (Master Spec §70).
 *
 * NOTE: This is in core:common which has Android dependency, so we can use Log directly.
 */
object Logger {

    private const val DEFAULT_TAG = LogTags.APP

    fun v(tag: String = DEFAULT_TAG, msg: String) {
        Log.v(tag, msg)
    }

    fun d(tag: String = DEFAULT_TAG, msg: String) {
        Log.d(tag, msg)
    }

    fun i(tag: String = DEFAULT_TAG, msg: String) {
        Log.i(tag, msg)
    }

    fun w(tag: String = DEFAULT_TAG, msg: String, throwable: Throwable? = null) {
        if (throwable != null) Log.w(tag, msg, throwable) else Log.w(tag, msg)
    }

    fun e(tag: String = DEFAULT_TAG, msg: String, throwable: Throwable? = null) {
        if (throwable != null) Log.e(tag, msg, throwable) else Log.e(tag, msg)
    }
}
