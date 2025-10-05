package com.layanjethwa.intrusiverevision

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class AppMonitorService : AccessibilityService() {

    companion object {
        // Singleton instance to expose current foreground app
        var instance: AppMonitorService? = null
            private set

        private val activePopups = mutableSetOf<String>()

        fun getForegroundAppPackage(): String? {
            return instance?.currentForegroundApp
        }

        fun notifyPopupClosed(appId: String) {
            activePopups.remove(appId)
            Log.i("AppMonitorService", "$appId popup closed, can schedule next")
        }
    }

    private var currentForegroundApp: String? = null
    private val ignoredApps = setOf("com.layanjethwa.intrusiverevision", "android")

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i("AppMonitorService", "AccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName in ignoredApps) return

        currentForegroundApp = packageName

        val prefs = getSharedPreferences(packageName, MODE_PRIVATE)
        val timeInterval = prefs.getInt("timeInterval", 0)
        val newQuestions = prefs.getInt("newQuestions", 0)
        val penaltyQuestions = prefs.getInt("penaltyQuestions", 0)

        // Only schedule a popup if at least one setting is non-zero
        if (timeInterval > 0 && (newQuestions > 0 || penaltyQuestions > 0)) {
            if (!activePopups.contains(packageName)) {
                activePopups.add(packageName)
                val intent = Intent(this, PopupReceiver::class.java).apply {
                    putExtra("appId", packageName)
                }

            // Schedule the first popup
            AlarmScheduler.schedulePopup(this, intent, timeInterval.toLong())
                Log.i("AlarmScheduler", "$packageName popup initialised")
            } else {
                Log.i("AppMonitorService", "$packageName already has an active popup, skipping")
            }
        }
    }

    override fun onInterrupt() {
        // Required, but not used
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) instance = null
    }
}
