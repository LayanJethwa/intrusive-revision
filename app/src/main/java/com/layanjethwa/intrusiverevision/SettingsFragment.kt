package com.layanjethwa.intrusiverevision

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Resources.getSystem
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.ChipGroup
import java.util.Locale


class SettingsFragment: Fragment(R.layout.settings_layout) {
    @SuppressLint("CommitPrefEdits")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val fragmentView = inflater.inflate(R.layout.settings_layout, container, false)

        val settings = this.activity?.getSharedPreferences("globalSettings", 0)
        val editor = settings?.edit()

        val globalSettings: MaterialCardView = fragmentView.findViewById(R.id.globalSettings)
        var globalSettingsCollapsed = true

        val globalTextGroup: ChipGroup = fragmentView.findViewById(R.id.globalTextGroup)
        val collapseChevron: ImageView = fragmentView.findViewById(R.id.collapseChevron)
        val newQuestions: EditText = fragmentView.findViewById(R.id.newQuestions)
        val timeInterval: EditText = fragmentView.findViewById(R.id.timeInterval)
        val penaltyQuestions: EditText = fragmentView.findViewById(R.id.penaltyQuestions)

        fun dpToPx(dp: Int): Float {
            return (dp * getSystem().displayMetrics.density)
        }

        globalSettings.setOnClickListener{
            val params: ViewGroup.LayoutParams = globalSettings.layoutParams

            if (globalSettingsCollapsed) {
                globalSettingsCollapsed = false
                params.height = dpToPx(160).toInt()
                globalTextGroup.visibility = View.VISIBLE
                collapseChevron.setImageResource(R.drawable.baseline_arrow_drop_up_24)
            } else {
                globalSettingsCollapsed = true
                params.height = dpToPx(25).toInt()
                globalTextGroup.visibility = View.INVISIBLE
                collapseChevron.setImageResource(R.drawable.baseline_arrow_drop_down_24)
            }

            globalSettings.layoutParams = params
        }

        newQuestions.setText(settings?.getInt("newQuestions",0).toString())
        timeInterval.setText(settings?.getInt("timeInterval",0).toString())
        penaltyQuestions.setText(settings?.getInt("penaltyQuestions",0).toString())

        newQuestions.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                if (!newQuestions.text.isDigitsOnly()) {
                    newQuestions.setText(R.string.zero_text)
                }
                editor?.putInt("newQuestions",newQuestions.text.toString().toInt())
                editor?.apply()
            } else {
                newQuestions.text.clear()
            }
        }

        timeInterval.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                if (!timeInterval.text.isDigitsOnly()) {
                    timeInterval.setText(R.string.zero_text)
                }
                editor?.putInt("timeInterval",timeInterval.text.toString().toInt())
                editor?.apply()
            } else {
                timeInterval.text.clear()
            }
        }

        penaltyQuestions.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                if (!penaltyQuestions.text.isDigitsOnly()) {
                    penaltyQuestions.setText(R.string.zero_text)
                }
                editor?.putInt("penaltyQuestions",penaltyQuestions.text.toString().toInt())
                editor?.apply()
            } else {
                penaltyQuestions.text.clear()
            }
        }

        val packageManager = context?.packageManager!!
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)

        val recyclerView: RecyclerView = fragmentView.findViewById(R.id.recyclerView)

        recyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        val dataList = ArrayList<Model>()

        Thread {
            val resolvedInfos = packageManager.queryIntentActivities(mainIntent, 0)
            resolvedInfos.sortBy { it.activityInfo.applicationInfo.loadLabel(packageManager).toString().lowercase(Locale.getDefault()) }
            for (info in resolvedInfos) {
                dataList.add(
                    Model(
                        info.activityInfo.applicationInfo.loadLabel(packageManager).toString(),
                        info.activityInfo.loadIcon(packageManager)
                    )
                )
            }
            val rvAdapter = RvAdapter(dataList)
            requireActivity().runOnUiThread {
                recyclerView.adapter = rvAdapter
            }
        }.start()

        return fragmentView
    }
}