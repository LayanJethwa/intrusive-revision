package com.layanjethwa.intrusiverevision

import android.app.ActivityManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.material.tabs.TabLayoutMediator
import com.layanjethwa.intrusiverevision.databinding.LayoutBinding


class MainActivity : AppCompatActivity() {
    private lateinit var binding: LayoutBinding
    private lateinit var mServiceIntent: Intent
    private lateinit var mYourService: ForegroundService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        this.getSharedPreferences("appRunning",0).edit().putBoolean("isActive", true).apply()

        checkPermissions()

        val fragments = listOf(
            FlashcardsFragment(),
            HomeFragment(),
            SettingsFragment()
        )

        val adapter = MyFragmentAdapter(
            fragments,
            supportFragmentManager,
            lifecycle
        )

        binding.pager.adapter = adapter
        binding.pager.post {
            binding.pager.setCurrentItem(1,true)
        }

        TabLayoutMediator(binding.tabLayout, binding.pager){tab, position ->
            tab.text = when(position){
                0 -> "Flashcards"
                1 -> "Questions"
                else -> "Settings"
            }
        }.attach()


    }

    @Suppress("DEPRECATION")
    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                Log.i("Service status", "Running")
                return true
            }
        }
        Log.i("Service status", "Not running")
        return false
    }

    private fun checkPermissions() {
        if (!Settings.canDrawOverlays(this)) {
            val myIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            startActivity(myIntent)
        }
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, Array(1){android.Manifest.permission.POST_NOTIFICATIONS}, 1)
        }
    }

    override fun onDestroy() {
        this.getSharedPreferences("appRunning",0).edit().putBoolean("isActive", false).apply()
        val broadcastIntent = Intent()
        broadcastIntent.setAction("restartservice")
    broadcastIntent.setClass(this, Restarter::class.java)
        this.sendBroadcast(broadcastIntent)
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        this.getSharedPreferences("appRunning",0).edit().putBoolean("isActive", false).apply()
        this.runOnUiThread {
            mYourService = ForegroundService()
            mServiceIntent = Intent(this, mYourService.javaClass)
            if (!isMyServiceRunning(mYourService.javaClass)) {
                startService(mServiceIntent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        this.getSharedPreferences("appRunning",0).edit().putBoolean("isActive", true).apply()
    }

}
