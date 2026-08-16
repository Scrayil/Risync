package com.scrayil.risync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import java.io.File
import java.io.IOException

class SyncthingService : Service() {

    @Volatile private var process: Process? = null
    @Volatile private var running = false
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val pm = getSystemService(PowerManager::class.java)
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$TAG::daemon")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification("Starting…"))
        if (!running) {
            running = true
            wakeLock?.acquire()
            Thread(::supervise, "syncthing-supervisor").start()
        }
        return START_STICKY
    }

    private fun supervise() {
        var backoffMs = 1_000L
        while (running) {
            val startedAt = System.currentTimeMillis()
            val code = runOnce()
            if (!running) break

            val ranFor = System.currentTimeMillis() - startedAt
            backoffMs = if (ranFor > 60_000) 1_000L else (backoffMs * 2).coerceAtMost(60_000L)

            updateNotification("Stopped (code=$code) — retry in ${backoffMs / 1000}s")
            Log.w(TAG, "restarting in ${backoffMs}ms")
            try { Thread.sleep(backoffMs) } catch (e: InterruptedException) { break }
        }
    }

    private fun runOnce(): Int {
        val bin = File(applicationInfo.nativeLibraryDir, "libsyncthing.so")
        val home = File(filesDir, "syncthing").apply { mkdirs() }

        val proc = try {
            val builder = ProcessBuilder(
                bin.absolutePath, "serve",
                "--home", home.absolutePath,
                "--no-browser"
            )
            builder.environment()["HOME"] = home.absolutePath
            builder.environment()["TMPDIR"] = cacheDir.absolutePath
            builder.environment()["STNORESTART"] = "1"
            builder.redirectErrorStream(true)
            builder.start()
        } catch (e: IOException) {
            Log.e(TAG, "exec failed: cannot start ${bin.absolutePath}", e)
            return -1
        }

        process = proc
        updateNotification("Running")

        return try {
            proc.inputStream.bufferedReader().forEachLine { Log.i(TAG, it) }
            proc.waitFor()
        } catch (e: IOException) {
            if (running) Log.w(TAG, "output stream closed unexpectedly", e)
            proc.waitFor()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            -1
        } finally {
            process = null
        }
    }

    override fun onDestroy() {
        running = false
        process?.destroy()
        wakeLock?.takeIf { it.isHeld }?.release()
        super.onDestroy()
    }

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Syncthing", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Syncthing")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(open)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIF_ID, buildNotification(text))
    }

    companion object {
        private const val TAG = "Risync"
        private const val CHANNEL_ID = "syncthing"
        private const val NOTIF_ID = 1
    }
}