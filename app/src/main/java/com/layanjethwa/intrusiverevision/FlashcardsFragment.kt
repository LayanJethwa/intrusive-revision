package com.layanjethwa.intrusiverevision

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources.getSystem
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar.LayoutParams
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.jsoup.Jsoup
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar

class FlashcardsFragment: Fragment(R.layout.flashcards_layout) {

    data class CardComponents(
        val tickChip: Chip,
        val crossChip: Chip,
        val checkBox: CheckBox
    )

    private val sets = mutableListOf<MaterialCardView>()
    private val cardGraphics = mutableMapOf<MaterialCardView, CardComponents>()
    private val setFileNames = mutableMapOf<MaterialCardView, String>()
    private lateinit var layout : ConstraintLayout
    private var currentSetFileNameInit = ""
    private var currentSetFileName = ""

    private lateinit var currentSetTitle : TextView
    private lateinit var currentSetTerms : Chip
    private lateinit var currentSetDate : Chip
    private lateinit var currentSetTick : Chip
    private lateinit var currentSetCross : Chip
    //private lateinit var currentSetRename : ImageButton

    private lateinit var settings : SharedPreferences

    private var resumeCount = 0

    private fun dpToPx(dp: Int): Float {
        return (dp * getSystem().displayMetrics.density)
    }

    @SuppressLint("SetTextI18n")
    private fun setCurrentSet(set: String) {
        setFragmentResult("currentSet", bundleOf("currentSet" to set))
        activity?.applicationContext?.openFileOutput("currentSet.txt", Context.MODE_PRIVATE).use {
            it?.write(set.toByteArray())
        }

        currentSetFileName = set

        val setData = set.split("--")

        var currentTermCount: Int
        context?.openFileInput(set).use { stream ->
            currentTermCount = stream?.bufferedReader().use { it?.readText() ?: "ERROR" }.split("\n").size
        }
        val day = setData[0].slice(4..5)
        val month = setData[0].slice(2..3)
        val year = setData[0].slice(0..1)
        val date = "$day/$month/$year"

        requireActivity().runOnUiThread {
            currentSetTitle.text = setData[1].replace(".txt","")
            currentSetDate.text = date
            currentSetTerms.text = "$currentTermCount terms"
            currentSetTick.text = settings.getInt("${set.replace(".txt","")}--ticks",0).toString()
            currentSetCross.text = settings.getInt("${set.replace(".txt","")}--crosses",0).toString()
            currentSetTitle.requestLayout()
            //currentSetRename.requestLayout()
            layout.requestLayout()
        }
    }

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    private fun addCard(date: String, title: String, terms: Int, setFileName: String, border: Boolean = false) {
        val myCard = context?.let { MaterialCardView(it) }
        val mySetTitle = context?.let{ TextView(it) }
        val myTermChip = context?.let { Chip(it) }
        val myDateChip = context?.let { Chip(it) }
        val myTickChip = context?.let { Chip(it) }
        val myCrossChip = context?.let { Chip(it) }
        val myBinButton = context?.let { ImageButton(it) }
        //val myRenameButton = context?.let { ImageButton(it) }
        val mySelectBox = context?.let { CheckBox(it) }

        val cardLayoutParams = ConstraintLayout.LayoutParams(0,dpToPx(50).toInt())
        val setTitleLayoutParams = ConstraintLayout.LayoutParams(0,LayoutParams.WRAP_CONTENT)
        val termChipLayoutParams = ConstraintLayout.LayoutParams(LayoutParams.WRAP_CONTENT,dpToPx(30).toInt())
        val dateChipLayoutParams = ConstraintLayout.LayoutParams(LayoutParams.WRAP_CONTENT,dpToPx(30).toInt())
        val tickChipLayoutParams = ConstraintLayout.LayoutParams(LayoutParams.WRAP_CONTENT,dpToPx(30).toInt())
        val crossChipLayoutParams = ConstraintLayout.LayoutParams(LayoutParams.WRAP_CONTENT,dpToPx(30).toInt())
        val binButtonLayoutParams = ConstraintLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT)
        //val renameButtonLayoutParams = ConstraintLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT)
        val selectBoxLayoutParams = ConstraintLayout.LayoutParams(dpToPx(30).toInt(),dpToPx(30).toInt())

        cardLayoutParams.setMargins(
            dpToPx(8).toInt(),
            dpToPx(32).toInt(),
            dpToPx(8).toInt(),
            0
        )

        setTitleLayoutParams.setMargins(dpToPx(16).toInt(),dpToPx(4).toInt(),0,0)
        setTitleLayoutParams.matchConstraintMaxWidth = LayoutParams.WRAP_CONTENT
        termChipLayoutParams.setMargins(dpToPx(8).toInt(),0,0,dpToPx(-4).toInt())
        dateChipLayoutParams.setMargins(dpToPx(8).toInt(),0,0,dpToPx(-4).toInt())
        tickChipLayoutParams.setMargins(dpToPx(8).toInt(),0,0,dpToPx(-4).toInt())
        crossChipLayoutParams.setMargins(dpToPx(8).toInt(),0,0,dpToPx(-4).toInt())
        binButtonLayoutParams.setMargins(0,0,dpToPx(8).toInt(),dpToPx(8).toInt())
        //renameButtonLayoutParams.setMargins(dpToPx(8).toInt(),0,0,0)
        selectBoxLayoutParams.setMargins(0,0,0,dpToPx(8).toInt())

        myCard?.setCardBackgroundColor(ContextCompat.getColor(requireActivity(),
            R.color.light_grey
        ))
        myCard?.radius = dpToPx(16)
        myCard?.elevation = 0F

        if (border) {
            myCard?.strokeColor = ContextCompat.getColor(requireActivity(), R.color.highlight)
            for (card in sets) {
                card.strokeWidth = 0
            }
            myCard?.strokeWidth = dpToPx(3).toInt()
        }


        mySetTitle?.ellipsize = TextUtils.TruncateAt.END
        mySetTitle?.maxLines = 1
        mySetTitle?.setTextColor(ContextCompat.getColor(requireActivity(), R.color.black))
        mySetTitle?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16F)

        myTermChip?.setTextColor(ContextCompat.getColor(requireActivity(), R.color.light_text))
        myTermChip?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12F)
        myTermChip?.setChipBackgroundColorResource(R.color.dark_grey)
        myTermChip?.setPadding(dpToPx(4).toInt(),0,dpToPx(4).toInt(),0)

        myDateChip?.setTextColor(ContextCompat.getColor(requireActivity(), R.color.light_text))
        myDateChip?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12F)
        myDateChip?.setChipBackgroundColorResource(R.color.dark_grey)
        myDateChip?.setPadding(dpToPx(4).toInt(),0,dpToPx(4).toInt(),0)

        myTickChip?.setTextColor(ContextCompat.getColor(requireActivity(), R.color.light_text))
        myTickChip?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12F)
        myTickChip?.setChipBackgroundColorResource(R.color.dark_grey)
        myTickChip?.setPadding(dpToPx(4).toInt(),0,dpToPx(4).toInt(),0)
        myTickChip?.setChipIconResource(R.drawable.tick)
        myTickChip?.chipIconSize = dpToPx(12)

        myCrossChip?.setTextColor(ContextCompat.getColor(requireActivity(), R.color.light_text))
        myCrossChip?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12F)
        myCrossChip?.setChipBackgroundColorResource(R.color.dark_grey)
        myCrossChip?.setPadding(dpToPx(4).toInt(),0,dpToPx(4).toInt(),0)
        myCrossChip?.setChipIconResource(R.drawable.cross)
        myCrossChip?.chipIconSize = dpToPx(12)

        myBinButton?.setBackgroundColor(ContextCompat.getColor(requireActivity(), android.R.color.transparent))
        myBinButton?.setImageResource(R.drawable.bin)
        myBinButton?.setColorFilter(ContextCompat.getColor(requireActivity(), R.color.red))

        /*myRenameButton?.setBackgroundColor(ContextCompat.getColor(requireActivity(), android.R.color.transparent))
        myRenameButton?.setImageResource(R.drawable.rename)
        myRenameButton?.setColorFilter(ContextCompat.getColor(requireActivity(), R.color.black))*/

        mySelectBox?.buttonTintList = ContextCompat.getColorStateList(requireActivity(), R.color.highlight)
        mySelectBox?.isClickable = true
        mySelectBox?.isFocusable = true

        val updateSelectedSetsPref: (String, Boolean) -> Unit = { file, add ->
            val selected = settings.getString("selectedSetsList", "") ?: ""
            val list = selected.split(",").filter { it.isNotBlank() }.toMutableList()
            if (add) {
                if (!list.contains(file)) list.add(file)
            } else {
                list.removeAll { it == file }
            }
            settings.edit().putString("selectedSetsList", list.joinToString(",")).apply()
            setFragmentResult("setsList", bundleOf("trigger" to "data"))
        }

        val numTermsText = "$terms terms"
        mySetTitle?.text = title
        myTermChip?.text = numTermsText
        myDateChip?.text = date

        val fileFormatDate = date.split("/").reversed().joinToString(separator = "")

        myTickChip?.text = settings.getInt("${fileFormatDate}--$title--ticks",0).toString()
        myCrossChip?.text = settings.getInt("${fileFormatDate}--$title--crosses",0).toString()

        myCard?.layoutParams = cardLayoutParams
        mySetTitle?.layoutParams = setTitleLayoutParams
        myTermChip?.layoutParams = termChipLayoutParams
        myDateChip?.layoutParams = dateChipLayoutParams
        myTickChip?.layoutParams = tickChipLayoutParams
        myCrossChip?.layoutParams = crossChipLayoutParams
        myBinButton?.layoutParams = binButtonLayoutParams
        //myRenameButton?.layoutParams = renameButtonLayoutParams
        mySelectBox?.layoutParams = selectBoxLayoutParams

        myCard?.id = View.generateViewId()
        mySetTitle?.id = View.generateViewId()
        myTermChip?.id = View.generateViewId()
        myDateChip?.id = View.generateViewId()
        myTickChip?.id = View.generateViewId()
        myCrossChip?.id = View.generateViewId()
        myBinButton?.id = View.generateViewId()
        //myRenameButton?.id = View.generateViewId()
        mySelectBox?.id = View.generateViewId()

        myCard?.setOnClickListener{

            for (card in sets){
                card.strokeWidth = 0
                cardGraphics[card]?.checkBox?.isChecked = false
            }

            myCard.strokeColor = ContextCompat.getColor(requireActivity(), R.color.highlight)
            myCard.strokeWidth = dpToPx(3).toInt()
            cardGraphics[myCard]?.checkBox?.isChecked = true

            val editor = settings.edit()
            editor.putString("selectedSetsList", setFileName)
            editor.apply()
            setFragmentResult("setsList", bundleOf("trigger" to "data"))
            setCurrentSet(setFileName)
        }

        layout.addView(myCard)
        layout.addView(mySetTitle)
        layout.addView(myTermChip)
        layout.addView(myDateChip)
        layout.addView(myTickChip)
        layout.addView(myCrossChip)
        layout.addView(myBinButton)
        //layout.addView(myRenameButton)
        layout.addView(mySelectBox)

        val cardConstraintParams = ConstraintSet()
        cardConstraintParams.clone(layout)


        myCard?.id?.let { cardConstraintParams.connect(it, ConstraintSet.END,
            R.id.marginRight, ConstraintSet.START) }
        myCard?.id?.let { cardConstraintParams.connect(it, ConstraintSet.START,
            R.id.marginLeft, ConstraintSet.START) }


        mySetTitle?.id?.let { myCard?.id?.let { it1 ->
            cardConstraintParams.connect(it, ConstraintSet.BOTTOM,
                it1, ConstraintSet.BOTTOM)
        } }
        mySetTitle?.id?.let { cardConstraintParams.connect(it, ConstraintSet.END,
            R.id.marginSetTitles, ConstraintSet.START)}
        mySetTitle?.id?.let { myCard?.id?.let { it1 ->
            cardConstraintParams.connect(it, ConstraintSet.START,
                it1, ConstraintSet.START)
        } }
        mySetTitle?.id?.let { myCard?.id?.let { it1 ->
            cardConstraintParams.connect(it, ConstraintSet.TOP,
                it1, ConstraintSet.TOP)
        } }
        mySetTitle?.id?.let { cardConstraintParams.setHorizontalBias(it, 0F) }
        mySetTitle?.id?.let { cardConstraintParams.setVerticalBias(it, 0F) }


        myTermChip?.id?.let { myCard?.id?.let { it1 ->
            cardConstraintParams.connect(it, ConstraintSet.BOTTOM,
                it1, ConstraintSet.BOTTOM)
        } }
        myTermChip?.id?.let { myCard?.id?.let { it1 ->
            cardConstraintParams.connect(it, ConstraintSet.START,
                it1, ConstraintSet.START)
        } }


        myDateChip?.id?.let { myCard?.id?.let { it1 ->
            cardConstraintParams.connect(it, ConstraintSet.BOTTOM,
                it1, ConstraintSet.BOTTOM)
        } }
        myDateChip?.id?.let { myTermChip?.id?.let { it1 ->
            cardConstraintParams.connect(it, ConstraintSet.START,
                it1, ConstraintSet.END)
        } }


        myTickChip?.id?.let { myCard?.id?.let { it1 ->
            cardConstraintParams.connect(it, ConstraintSet.BOTTOM,
                it1, ConstraintSet.BOTTOM)
        } }
        myTickChip?.id?.let { myDateChip?.id?.let { it1 ->
            cardConstraintParams.connect(it, ConstraintSet.START,
                it1, ConstraintSet.END)
        } }


        myCrossChip?.id?.let { myCard?.id?.let { it1 ->
            cardConstraintParams.connect(it, ConstraintSet.BOTTOM,
                it1, ConstraintSet.BOTTOM)
        } }
        myCrossChip?.id?.let { myTickChip?.id?.let { it1 ->
            cardConstraintParams.connect(it, ConstraintSet.START,
                it1, ConstraintSet.END)
        } }


        myBinButton?.id?.let { myCard?.id?.let { it1 ->
            cardConstraintParams.connect(it, ConstraintSet.BOTTOM,
                it1, ConstraintSet.BOTTOM)
        } }
        myBinButton?.id?.let { myCard?.id?.let { it1 ->
            cardConstraintParams.connect(it, ConstraintSet.END,
                it1, ConstraintSet.END)
        } }


        /*myRenameButton?.id?.let { mySetTitle?.id?.let { it1 ->
            cardConstraintParams.connect(it, ConstraintSet.BOTTOM,
                it1, ConstraintSet.BOTTOM)
        } }
        myRenameButton?.id?.let { mySetTitle?.id?.let { it1 ->
            cardConstraintParams.connect(it, ConstraintSet.START,
                it1, ConstraintSet.END)
        } }
        myRenameButton?.id?.let { mySetTitle?.id?.let { it1 ->
            cardConstraintParams.connect(it, ConstraintSet.TOP,
                it1, ConstraintSet.TOP)
        } }*/

        mySelectBox?.id?.let { myCrossChip?.id?.let { it1 ->
            cardConstraintParams.connect(it, ConstraintSet.BOTTOM,
                it1, ConstraintSet.BOTTOM)
        } }
        mySelectBox?.id?.let { myBinButton?.id?.let { it1 ->
            cardConstraintParams.connect(it, ConstraintSet.END,
                it1, ConstraintSet.START)
        } }
        mySelectBox?.id?.let { myCrossChip?.id?.let { it1 ->
            cardConstraintParams.connect(it, ConstraintSet.START,
                it1, ConstraintSet.END)
        } }

        myCard?.id?.let {
            cardConstraintParams.connect(
                it,
                ConstraintSet.TOP,
                R.id.currentSet,
                ConstraintSet.BOTTOM
            )
        }

        if (sets.size > 0) {
            cardConstraintParams.clear(sets.last().id, ConstraintSet.TOP)
            sets.last().id.let {
                myCard?.id?.let { it1 ->
                    cardConstraintParams.connect(
                        it,
                        ConstraintSet.TOP,
                        it1,
                        ConstraintSet.BOTTOM
                    )
                }
            }

            cardConstraintParams.setMargin(sets.last().id, ConstraintSet.TOP, dpToPx(8).toInt())
        }

        if (myCard != null && mySetTitle != null && myTermChip != null && myDateChip != null && myTickChip != null && myCrossChip != null && myBinButton != null) { //&& myRenameButton != null) {
            cardGraphics[myCard] = mySelectBox?.let { CardComponents(myTickChip,myCrossChip, it) }!!
        }

        myBinButton?.setOnClickListener {
            File(context?.filesDir, setFileName).delete()

            val changeCardConstraintParams = ConstraintSet()
            changeCardConstraintParams.clone(layout)

            layout.removeView(myCard)
            layout.removeView(mySetTitle)
            layout.removeView(myTermChip)
            layout.removeView(myDateChip)
            layout.removeView(myTickChip)
            layout.removeView(myCrossChip)
            layout.removeView(myBinButton)
            //layout.removeView(myRenameButton)
            layout.removeView(mySelectBox)

            if (myCard == sets.last() && myCard != sets.first()) {
                val cardChanged = sets[sets.size-2]
                changeCardConstraintParams.clear(cardChanged.id, ConstraintSet.TOP)
                changeCardConstraintParams.connect(cardChanged.id, ConstraintSet.TOP,
                    R.id.currentSet, ConstraintSet.BOTTOM)
                changeCardConstraintParams.setMargin(cardChanged.id, ConstraintSet.TOP, dpToPx(32).toInt())
            } else if (myCard != sets.first()) {
                val delIndex = sets.indexOf(myCard)
                changeCardConstraintParams.clear(sets[delIndex-1].id, ConstraintSet.TOP)
                changeCardConstraintParams.connect(sets[delIndex-1].id, ConstraintSet.TOP, sets[delIndex+1].id, ConstraintSet.BOTTOM)
                changeCardConstraintParams.setMargin(sets[delIndex-1].id, ConstraintSet.TOP, dpToPx(8).toInt())
            }

            sets.remove(myCard)
            setFileNames.remove(myCard)

            if (mySetTitle?.text == currentSetTitle.text) {
                currentSetTitle.text = ""
                currentSetDate.text = ""
                currentSetTerms.text = ""
                currentSetCross.text = ""
                currentSetTick.text = ""
                val thisCurrentSetFile = File(context?.filesDir?.path, "currentSet.txt")
                if (thisCurrentSetFile.exists()) {
                    thisCurrentSetFile.delete()
                }
            }

            changeCardConstraintParams.applyTo(layout)
            layout.requestLayout()
        }

        mySelectBox?.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val newChecked = !(mySelectBox.isChecked ?: false)
                mySelectBox.isChecked = newChecked
                updateSelectedSetsPref(setFileName, newChecked)
            }
            true
        }

        cardConstraintParams.applyTo(layout)
        layout.requestLayout()

        if (myCard != null) {
            sets.add(myCard)
            setFileNames[myCard] = setFileName
        }

    }

    @SuppressLint("UseRequireInsteadOfGet")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val fragmentView = inflater.inflate(R.layout.flashcards_layout, container, false)
        layout = fragmentView.findViewById(R.id.constraintLayout)

        settings = this.activity?.getSharedPreferences("setsStats", 0)!!

        val inputBox : TextInputLayout = fragmentView.findViewById(R.id.addSet)
        val inputText : TextInputEditText = fragmentView.findViewById(R.id.addSetText)

        currentSetTitle = fragmentView.findViewById(R.id.setTitle)
        currentSetTerms = fragmentView.findViewById(R.id.termChip)
        currentSetDate = fragmentView.findViewById(R.id.dateChip)
        currentSetTick = fragmentView.findViewById(R.id.tickChip)
        currentSetCross = fragmentView.findViewById(R.id.crossChip)
        //currentSetRename = fragmentView.findViewById(R.id.renameButton)

        @SuppressLint("SimpleDateFormat")
        fun addSet() {
            requireActivity().runOnUiThread {
                Toast.makeText(
                    activity?.applicationContext,
                    "Fetching cards, this may take a while for a large set",
                    Toast.LENGTH_LONG
                ).show()
            }

            val url = inputText.text.toString()
            Thread {
                val cards: List<Pair<String, String>>
                val title: String
                if (url.contains("quizlet.com")) {
                    val doc: String = try {
                        Jsoup.connect(url)
                            .maxBodySize(0)
                            .timeout(0)
                            .userAgent("Mozilla/5.0")
                            .get().toString()
                    } catch (e: Exception) {
                        "ERROR"
                    }

                    if (doc == "ERROR") {
                        requireActivity().runOnUiThread {
                            Toast.makeText(context, "Connection error", Toast.LENGTH_LONG).show()
                        }
                        return@Thread
                    }

                    cards =
                        Regex("""\\"label\\":\\"(?:word|definition)\\",\\"media\\":\[\{\\"type\\":1,\\"plainText\\":\\"(.+?)\\""").findAll(
                            doc
                        ).map { it.groups[1]?.value ?: "" }.toList().chunked(2).map { it[0] to it[1] }
                    if (cards.isEmpty()) {
                        requireActivity().runOnUiThread {
                            Toast.makeText(
                                context,
                                "Invalid or empty Quizlet set",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        return@Thread
                    }

                    val titleMatch = Regex("""<title.*>(.+?) Flashcards""").find(doc)
                    title = titleMatch?.groups?.get(1)?.value ?: "Untitled Set"
                } else {
                    val match = Regex("""(.+?)\|((?:.+?<.+?>)+)""").findAll(url).toList()[0]
                    title = match.groups[1]?.value ?: "Untitled Set"
                    cards = Regex("(.+?)<(.+?)>").findAll(match.groups[2]?.value ?: "term").map {
                        pair ->
                        val term = pair.groups[1]?.value ?: ""
                        val def = pair.groups[2]?.value ?: ""
                        term to def
                    }.toList()
                }

                val date = SimpleDateFormat("dd/MM/yy").format(Calendar.getInstance().time)
                val fileDate = SimpleDateFormat("yyMMdd").format(Calendar.getInstance().time)
                val terms = cards.size

                val fileName = "$fileDate--$title.txt"
                val file = File(context?.filesDir, fileName)
                if (file.exists()) file.delete()

                activity?.applicationContext?.openFileOutput(fileName, Context.MODE_PRIVATE).use { out ->
                    for (card in cards) {
                        if (card != cards.last()) {
                            out?.write("${card.first}||${card.second}\n".toByteArray())
                        } else {
                            out?.write("${card.first}||${card.second}".toByteArray())
                        }
                    }
                }

                requireActivity().runOnUiThread {
                    addCard(date, title, terms, fileName, border = true)
                    setCurrentSet(fileName)
                    Toast.makeText(context, "Set initialized", Toast.LENGTH_LONG).show()
                }

            }.start()
        }

        inputBox.setEndIconOnClickListener{
            addSet()
            inputBox.clearFocus()
            inputText.text?.clear()
        }


        val currentSetFile = File(context?.filesDir?.path, "currentSet.txt")
        if (currentSetFile.exists()) {
            context?.openFileInput("currentSet.txt").use { stream ->
                currentSetFileNameInit = stream?.bufferedReader().use {
                    it?.readText().toString()
                }
                setCurrentSet(currentSetFileNameInit)
            }
        }

        val files = activity?.filesDir?.listFiles()
        files?.sortWith { text1, text2 ->
            text1.compareTo(text2)
        }
        files?.forEach { it ->
            if ("-" in it.toString()) {
                val fileNameInit = it.toString().split("/").last()
                val fileData = fileNameInit.split("--")
                var termCount: Int
                context?.openFileInput(fileNameInit).use { stream ->
                    termCount = stream?.bufferedReader().use { it?.readText() ?: "ERROR" }.split("\n").size
                }
                val day = fileData[0].slice(4..5)
                val month = fileData[0].slice(2..3)
                val year = fileData[0].slice(0..1)
                val date = "$day/$month/$year"

                var borderInit = false
                if (fileNameInit == currentSetFileNameInit) {
                    borderInit = true
                }

                addCard(date, fileData[1].replace(".txt",""), termCount, fileNameInit, border = borderInit)
            }
        }


        return fragmentView
    }

    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()
        resumeCount ++
        if (resumeCount > 1) {
            for (card in sets) {
                cardGraphics[card]?.tickChip?.text = settings.getInt("${setFileNames[card]?.replace(".txt","")}--ticks",0)
                    .toString()
                cardGraphics[card]?.crossChip?.text = settings.getInt("${setFileNames[card]?.replace(".txt","")}--crosses",0)
                    .toString()
            }
            currentSetTick.text = settings.getInt("${currentSetFileName.replace(".txt","")}--ticks",0)
                .toString()
            currentSetCross.text = settings.getInt("${currentSetFileName.replace(".txt","")}--crosses",0)
                .toString()
        }
    }
}