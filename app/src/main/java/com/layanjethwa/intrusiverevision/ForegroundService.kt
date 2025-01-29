package com.layanjethwa.intrusiverevision

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import java.util.Timer
import java.util.TimerTask


class ForegroundService : Service() {
    private lateinit var window: Window
    private lateinit var timer: Timer
    private lateinit var timerTask: TimerTask
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        getSharedPreferences("appRunning",0).edit().putBoolean("serviceActive",false).apply()
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
                    getSharedPreferences("appRunning",0).edit().putBoolean("serviceActive",true).apply()
                }
            }
        }
        val interval: Long = getSharedPreferences("globalSettings",0).getInt("timeInterval",0).toLong()
        if (!getSharedPreferences("appRunning",0).getBoolean("serviceActive", false) &&
            !getSharedPreferences("appRunning",0).getBoolean("isActive", false) &&
            interval > 0
        ) {
            Timer().schedule(timerTask, interval*1000*60)
        }
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