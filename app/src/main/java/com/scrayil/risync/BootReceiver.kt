package com.scrayil.risync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Log.i("Risync", "boot completed, starting service")
        context.startForegroundService(Intent(context, SyncthingService::class.java))
    }
}