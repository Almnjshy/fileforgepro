package com.fileforge.pro.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.fileforge.pro.core.common.Logger
import dagger.hilt.android.AndroidEntryPoint

/**
 * Foreground service for long-running file operations (Master Spec §29).
 *
 * Phase 4 will wire this to FileOperationEngine via WorkManager +
 * coroutine workers. For now it's a stub that keeps the service running
 * so notifications can be posted during copy/move/delete operations.
 */
@AndroidEntryPoint
class FileOperationService : Service() {

    companion object {
        const val CHANNEL_ID = "fileforge_operations"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.fileforge.pro.action.START_OPERATION"
        const val ACTION_STOP = "com.fileforge.pro.action.STOP_OPERATION"
        const val EXTRA_OPERATION_ID = "operation_id"
        const val EXTRA_OPERATION_TYPE = "operation_type"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val opId = intent.getStringExtra(EXTRA_OPERATION_ID) ?: "unknown"
                val opType = intent.getStringExtra(EXTRA_OPERATION_TYPE) ?: "operation"
                Logger.i("FileOperation", "Service started: $opType ($opId)")
                startForeground(NOTIFICATION_ID, buildNotification(opType, "In progress"))
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "File Operations",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shows progress of copy, move, delete operations"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
