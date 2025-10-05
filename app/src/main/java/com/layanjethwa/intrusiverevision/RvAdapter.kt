package com.layanjethwa.intrusiverevision

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources.getSystem
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.text.isDigitsOnly
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.ChipGroup

class RvAdapter(private val userList: ArrayList<Model>, private val context: Context) :
    RecyclerView.Adapter<RvAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.app_item_layout, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int = userList.size

    private fun dpToPx(dp: Int): Int {
        return (dp * getSystem().displayMetrics.density).toInt()
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = userList[position]
        val prefs: SharedPreferences = context.getSharedPreferences(app.id, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        val layoutParams = holder.card.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.topMargin = if (position == 0) dpToPx(24) else dpToPx(0)
        layoutParams.leftMargin = dpToPx(16)
        layoutParams.rightMargin = dpToPx(16)
        layoutParams.bottomMargin = dpToPx(16)
        holder.card.layoutParams = layoutParams

        holder.appTitle.text = app.name
        holder.appIcon.setImageDrawable(app.icon)
        holder.chevron.setImageResource(R.drawable.baseline_arrow_drop_down_24)

        // Load saved values
        holder.newQuestions.setText(prefs.getInt("newQuestions", 0).toString())
        holder.timeInterval.setText(prefs.getInt("timeInterval", 0).toString())
        holder.penaltyQuestions.setText(prefs.getInt("penaltyQuestions", 0).toString())

        // Expand/collapse on chevron click
        holder.card.setOnClickListener {
            val params: ViewGroup.LayoutParams = holder.card.layoutParams
            val collapsedHeight = dpToPx(40)
            if ((collapsedHeight-2..collapsedHeight+2).contains(params.height)) {
                params.height = dpToPx(160)
                holder.chipGroup.visibility = View.VISIBLE
                holder.chevron.setImageResource(R.drawable.baseline_arrow_drop_up_24)
            } else {
                params.height = dpToPx(40)
                holder.chipGroup.visibility = View.INVISIBLE
                holder.chevron.setImageResource(R.drawable.baseline_arrow_drop_down_24)
            }

            holder.card.layoutParams = params
        }

        // Focus change listeners
        editTextHighlight(holder.newQuestions, editor, "newQuestions", holder.card, holder)
        editTextHighlight(holder.timeInterval, editor, "timeInterval", holder.card, holder)
        editTextHighlight(holder.penaltyQuestions, editor, "penaltyQuestions", holder.card, holder)
    }

    @SuppressLint("SetTextI18n")
    private fun editTextHighlight(
        editText: EditText,
        editor: SharedPreferences.Editor,
        key: String,
        card: MaterialCardView,
        holder: ViewHolder
    ) {
        val highlightColor = ContextCompat.getColor(context, R.color.highlight)
        val defaultColor = ContextCompat.getColor(context, R.color.dark_grey)
        val highlightWidth = dpToPx(2)
        val defaultWidth = dpToPx(0)

        val updateHighlight: () -> Unit = {
            val nQ = holder.newQuestions.text.toString().toIntOrNull() ?: 0
            val tI = holder.timeInterval.text.toString().toIntOrNull() ?: 0
            val pQ = holder.penaltyQuestions.text.toString().toIntOrNull() ?: 0

            if (nQ != 0 || tI != 0 || pQ != 0) {
                card.strokeColor = highlightColor
                card.strokeWidth = highlightWidth
            } else {
                card.strokeColor = defaultColor
                card.strokeWidth = defaultWidth
            }
        }

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateHighlight()
            }
        })

        editText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val text = editText.text.toString()
                val value = if (!text.isDigitsOnly() || TextUtils.isEmpty(text)) 0 else text.toInt()
                editText.setText(value.toString())
                editor.putInt(key, value)
                editor.apply()
                updateHighlight()
            }
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appTitle: TextView = itemView.findViewById(R.id.appTitle)
        val appIcon: ImageView = itemView.findViewById(R.id.appIcon)
        val chevron: ImageView = itemView.findViewById(R.id.appChevron)
        val card: MaterialCardView = itemView.findViewById(R.id.appCard)
        val chipGroup: ChipGroup = itemView.findViewById(R.id.globalTextGroup)
        val newQuestions: EditText = itemView.findViewById(R.id.newQuestions)
        val timeInterval: EditText = itemView.findViewById(R.id.timeInterval)
        val penaltyQuestions: EditText = itemView.findViewById(R.id.penaltyQuestions)
    }
}
