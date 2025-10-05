package com.layanjethwa.intrusiverevision

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class PopupReceiver : BroadcastReceiver() {
    companion object {
        private const val COOLDOWN_MS = 30_000L // minimum seconds between popups per app
    }

    override fun onReceive(context: Context, intent: Intent) {
        val appId = intent.getStringExtra("appId") ?: return

        // Get foreground app from AppMonitorService
        val foregroundApp = AppMonitorService.getForegroundAppPackage()
        if (foregroundApp != appId) {
            Log.i("PopupReceiver", "$appId not in foreground; skipping popup")
            return
        }

        // Check cooldown
        val prefs = context.getSharedPreferences(appId, Context.MODE_PRIVATE)
        val lastPopup = prefs.getLong("lastPopup", 0)
        val now = System.currentTimeMillis()
        if (now - lastPopup < COOLDOWN_MS) {
            Log.i("PopupReceiver", "$appId cooldown active, skipping popup")
            return
        }

        if (Window.activeWindow != null) {
            Log.i("PopupReceiver", "Window already open, skipping popup for $appId")
            return
        }

        // Create a new window
        val window = Window(context)

        // Schedule next popup after this window closes
        window.onClose = {
            AppMonitorService.notifyPopupClosed(appId)
            prefs.edit().putLong("lastPopup", System.currentTimeMillis()).apply()
            val interval = prefs.getInt("timeInterval", 0)
            if (interval > 0) {
                val nextIntent = Intent(context, PopupReceiver::class.java).apply {
                    putExtra("appId", appId)
                }
                AlarmScheduler.schedulePopup(context, nextIntent, interval.toLong())
                Log.i("PopupReceiver", "$appId popup scheduled")
            }
        }

        // Open the window
        Log.i("PopupReceiver", "$appId popup triggered")
        window.open(appId)
    }
}
