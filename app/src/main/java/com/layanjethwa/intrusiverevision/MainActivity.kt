package com.layanjethwa.intrusiverevision

import android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.material.tabs.TabLayoutMediator
import com.layanjethwa.intrusiverevision.databinding.LayoutBinding
import android.content.Context
import android.view.accessibility.AccessibilityManager

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    private lateinit var binding: LayoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        getSharedPreferences("appRunning",0).edit().putBoolean("isActive", true).apply()

        checkPermissions()
        ensureAccessibilityEnabled()

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
        binding.pager.post { binding.pager.setCurrentItem(1,true) }

        TabLayoutMediator(binding.tabLayout, binding.pager){ tab, position ->
            tab.text = when(position){
                0 -> "Flashcards"
                1 -> "Questions"
                else -> "Settings"
            }
        }.attach()
    }

    private fun checkPermissions() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
        }

        if (!NotificationManagerCompat.from(this).areNotificationsEnabled() &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                1
            )
        }

        val appOpsManager = getSystemService(APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOpsManager.unsafeCheckOpNoThrow(
                "android:get_usage_stats",
                android.os.Process.myUid(),
                packageName
            )
        } else {
            appOpsManager.checkOpNoThrow(
                "android:get_usage_stats",
                android.os.Process.myUid(),
                packageName
            )
        }
        if (mode != android.app.AppOpsManager.MODE_ALLOWED) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
        }
    }

    private fun ensureAccessibilityEnabled() {
        val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val accessibilityServices = accessibilityManager.getEnabledAccessibilityServiceList(FEEDBACK_ALL_MASK)
        val accessibilityPackages = mutableListOf<String>()
        for (info in accessibilityServices) {
            accessibilityPackages.add(info.resolveInfo.serviceInfo.packageName)
        }

        if (!accessibilityPackages.contains(packageName)) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        getSharedPreferences("appRunning",0).edit().putBoolean("isActive", true).apply()
    }
}
