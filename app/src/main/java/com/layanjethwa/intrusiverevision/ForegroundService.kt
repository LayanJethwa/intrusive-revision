package com.layanjethwa.intrusiverevision

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import java.util.Timer
import java.util.TimerTask


class ForegroundService : Service() {
    private lateinit var window: Window
    private lateinit var timer: Timer
    private lateinit var timerTask: TimerTask
    private val handler = Handler(Looper.getMainLooper())
    private var systemApps: MutableList<String> = mutableListOf("com.layanjethwa.intrusiverevision","android")

    override fun onCreate() {
        super.onCreate()

        val appInfos = packageManager.getInstalledApplications( PackageManager.GET_META_DATA )
        for ( appInfo in appInfos ) {
            if (appInfo.flags == ApplicationInfo.FLAG_SYSTEM) {
                systemApps.add(appInfo.packageName)
            }
        }

        getSharedPreferences("appRunning",0).edit().putBoolean("serviceActive",false).apply()
        getSharedPreferences("appRunning",0).edit().putBoolean("isActive",true).apply()
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            startMyOwnForeground()
        } else {
            startForeground(1, Notification())
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        timerTask = object : TimerTask() {
            override fun run() {
                handler.post {
                    window = Window(context = applicationContext)
                    window.open()
                }
            }
        }
        val checkApps = object : TimerTask() {
            override fun run() {
                val usageStatsManager =
                    applicationContext.getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
                val currentTime = System.currentTimeMillis()
                val usageEvents = usageStatsManager.queryEvents(currentTime - 1000, currentTime)
                val usageEvent = UsageEvents.Event()

                while (usageEvents.hasNextEvent()) {
                    usageEvents.getNextEvent(usageEvent)
                    val id = usageEvent.packageName
                    if (id !in systemApps && usageEvent.eventType in mutableListOf(1,19)) {
                        getSharedPreferences("appRunning",0).edit().putString("currentApp", id).apply()
                        Log.e("APP", "$id ${usageEvent.eventType}")
                        if (getSharedPreferences(id, 0).getInt(
                                "timeInterval",
                                0
                            ) != 0
                            && getSharedPreferences(
                                id,
                                0
                            ).getInt("newQuestions", 0) != 0
                        ) {

                            val appTimerTask = object : TimerTask() {
                                override fun run() {
                                    handler.post {
                                        window = Window(context = applicationContext)
                                        window.open(id)
                                    }
                                }
                            }

                            Timer().schedule(
                                appTimerTask,
                                getSharedPreferences(
                                    id,
                                    0
                                ).getInt("timeInterval", 0).toLong()
                            )

                        }
                    }
                }
            }
        }
        val interval: Long =
            getSharedPreferences("globalSettings", 0).getInt("timeInterval", 0).toLong()
        if (interval > 0) {
            Timer().schedule(timerTask, interval * 1000 * 60)
        }

        Timer().schedule(checkApps, 0, 2000)
        return START_STICKY
    }

    private fun startMyOwnForeground() {
        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel()
            } else {""}

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
        val notification = notificationBuilder.setOngoing(true)
            .setContentTitle("Service running")
            .setContentText("Displaying over other apps")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        startForeground(2, notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): String {
        val channelId = "example.permanence"
        val channelName = "Background Service"
        val chan = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_MIN
        )

        val manager = checkNotNull(getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
        manager.createNotificationChannel(chan)

        return channelId
    }

    override fun onDestroy() {
        super.onDestroy()
        getSharedPreferences("appRunning",0).edit().putBoolean("serviceActive",false).apply()
        getSharedPreferences("appRunning",0).edit().putBoolean("isActive",false).apply()
        window.close()
        timer.cancel()
        timer.purge()

        val broadcastIntent = Intent()
        broadcastIntent.setAction("restartservice")
        broadcastIntent.setClass(this, Restarter::class.java)
        this.sendBroadcast(broadcastIntent)
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
}