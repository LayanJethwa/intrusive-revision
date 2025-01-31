package com.layanjethwa.intrusiverevision

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources.getSystem
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.isDigitsOnly
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.ChipGroup

class RvAdapter(private val userList: ArrayList<Model>, val context: Context) : RecyclerView.Adapter<RvAdapter.ViewHolder>() {
    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {
        val v = LayoutInflater.from(p0.context).inflate(R.layout.adapter_item_layout, p0, false)
        return ViewHolder(v)
    }
    override fun getItemCount(): Int {
        return userList.size
    }
    private fun dpToPx(dp: Int): Float {
        return (dp * getSystem().displayMetrics.density)
    }
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(p0: ViewHolder, p1: Int) {
        val settings = context.getSharedPreferences(userList[p1].id, 0)
        val editor = settings.edit()

        p0.name.text = userList[p1].name
        p0.icon.setImageDrawable(userList[p1].icon)
        p0.card.setOnClickListener{
            val params: ViewGroup.LayoutParams = p0.card.layoutParams
            val collapsedHeight = dpToPx(40).toInt()
            if ((collapsedHeight-2..collapsedHeight+2).contains(params.height)) {
                params.height = dpToPx(160).toInt()
                p0.chipGroup.visibility = View.VISIBLE
                p0.chevron.setImageResource(R.drawable.baseline_arrow_drop_up_24)
            } else {
                params.height = dpToPx(40).toInt()
                p0.chipGroup.visibility = View.INVISIBLE
                p0.chevron.setImageResource(R.drawable.baseline_arrow_drop_down_24)
            }

            p0.card.layoutParams = params
        }
        p0.newQuestions.setText(settings.getInt("newQuestions",0).toString())
        p0.timeInterval.setText(settings.getInt("timeInterval",0).toString())
        p0.penaltyQuestions.setText(settings.getInt("penaltyQuestions",0).toString())

        fun checkBorder() {
            if (!(settings.getInt("newQuestions",0) == 0 &&
                settings.getInt("timeInterval",0) == 0 &&
                settings.getInt("penaltyQuestions",0) == 0)) {
                p0.card.strokeWidth = dpToPx(2).toInt()
            } else {
                p0.card.strokeWidth = 0
            }
        }

        checkBorder()

        p0.newQuestions.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                if (!p0.newQuestions.text.isDigitsOnly() || p0.newQuestions.text.isBlank()) {
                    p0.newQuestions.setText(R.string.zero_text)
                }
                editor?.putInt("newQuestions",p0.newQuestions.text.toString().toInt())
                editor?.apply()
                checkBorder()
            } else {
                p0.newQuestions.text.clear()
            }
        }

        p0.timeInterval.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                if (!p0.timeInterval.text.isDigitsOnly() || p0.timeInterval.text.isBlank()) {
                    p0.timeInterval.setText(R.string.zero_text)
                }
                editor?.putInt("timeInterval",p0.timeInterval.text.toString().toInt())
                editor?.apply()
                checkBorder()
            } else {
                p0.timeInterval.text.clear()
            }
        }

        p0.penaltyQuestions.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                if (!p0.penaltyQuestions.text.isDigitsOnly() || p0.penaltyQuestions.text.isBlank()) {
                    p0.penaltyQuestions.setText(R.string.zero_text)
                }
                editor?.putInt("penaltyQuestions",p0.penaltyQuestions.text.toString().toInt())
                editor?.apply()
                checkBorder()
            } else {
                p0.penaltyQuestions.text.clear()
            }
        }
    }
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.appTitle)
        val icon: ImageView = itemView.findViewById(R.id.appIcon)
        val chevron: ImageView = itemView.findViewById(R.id.appChevron)
        val card: MaterialCardView = itemView.findViewById(R.id.appCard)
        val chipGroup: ChipGroup = itemView.findViewById(R.id.globalTextGroup)
        val newQuestions: EditText = itemView.findViewById(R.id.newQuestions)
        val timeInterval: EditText = itemView.findViewById(R.id.timeInterval)
        val penaltyQuestions: EditText = itemView.findViewById(R.id.penaltyQuestions)
    }
}