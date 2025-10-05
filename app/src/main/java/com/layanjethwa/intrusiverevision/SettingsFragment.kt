package com.layanjethwa.intrusiverevision

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class SettingsFragment : Fragment(R.layout.settings_layout) {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val fragmentView = inflater.inflate(R.layout.settings_layout, container, false)

        val recyclerView: RecyclerView = fragmentView.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)

        val packageManager = requireContext().packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val dataList = ArrayList<Model>()

        val masterPrefs = requireContext().getSharedPreferences("appRunning", Context.MODE_PRIVATE)
        val activeApps = mutableSetOf<String>()
        for (app in dataList) {
            if (app.timeInterval > 0) activeApps.add(app.id)
        }
        masterPrefs.edit().putStringSet("activeApps", activeApps).apply()

        Thread {
            val resolvedInfos = packageManager.queryIntentActivities(mainIntent, 0)
            resolvedInfos.sortBy { it.activityInfo.applicationInfo.loadLabel(packageManager)
                .toString().lowercase(Locale.getDefault()) }

            for (info in resolvedInfos) {
                if (info.activityInfo.packageName != "com.layanjethwa.intrusiverevision") {
                    // Load per-app preferences
                    val prefs = requireContext().getSharedPreferences(info.activityInfo.packageName, Context.MODE_PRIVATE)
                    val timeInterval = prefs.getInt("timeInterval", 0)
                    val newQuestions = prefs.getInt("newQuestions", 0)
                    val penaltyQuestions = prefs.getInt("penaltyQuestions", 0)

                    dataList.add(
                        Model(
                            info.activityInfo.applicationInfo.loadLabel(packageManager).toString(),
                            info.activityInfo.loadIcon(packageManager),
                            info.activityInfo.packageName,
                            timeInterval,
                            newQuestions,
                            penaltyQuestions
                        )
                    )
                }
            }

            requireActivity().runOnUiThread {
                recyclerView.adapter = RvAdapter(dataList, requireContext())
            }
        }.start()

        return fragmentView
    }
}
