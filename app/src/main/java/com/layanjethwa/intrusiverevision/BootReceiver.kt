package com.layanjethwa.intrusiverevision

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i("BootReceiver", "Device rebooted, rescheduling active popups")

            val masterPrefs = context.getSharedPreferences("appRunning", Context.MODE_PRIVATE)
            val apps = masterPrefs.getStringSet("activeApps", emptySet()) ?: emptySet()

            for (appId in apps) {
                val prefs = context.getSharedPreferences(appId, Context.MODE_PRIVATE)
                val interval = prefs.getInt("timeInterval", 0)
                val newQuestions = prefs.getInt("newQuestions", 0)
                if (interval > 0 && newQuestions > 0) {
                    val popupIntent = Intent(context, PopupReceiver::class.java).apply {
                        putExtra("appId", appId)
                    }
                    AlarmScheduler.schedulePopup(context, popupIntent, interval.toLong())
                }
            }
        }
    }
}
