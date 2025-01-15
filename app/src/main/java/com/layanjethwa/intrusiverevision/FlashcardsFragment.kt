package com.layanjethwa.intrusiverevision

import android.content.Context
import android.content.res.Resources.getSystem
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.layanjethwa.intrusiverevision.R
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.jsoup.Jsoup
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar

class FlashcardsFragment: Fragment(R.layout.flashcards_layout) {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val fragmentView = inflater.inflate(R.layout.flashcards_layout, container, false)

        val layout : ConstraintLayout = fragmentView.findViewById(R.id.constraintLayout)
        val inputBox : TextInputLayout = fragmentView.findViewById(R.id.addSet)
        val inputText : TextInputEditText = fragmentView.findViewById(R.id.addSetText)

        val currentSetTitle : TextView = fragmentView.findViewById(R.id.setTitle)
        val currentSetTerms : Chip = fragmentView.findViewById(R.id.termChip)
        val currentSetDate : Chip = fragmentView.findViewById(R.id.dateChip)
        val currentSetTick : Chip = fragmentView.findViewById(R.id.tickChip)
        val currentSetCross : Chip = fragmentView.findViewById(R.id.crossChip)
        val currentSetRename : ImageButton = fragmentView.findViewById(R.id.renameButton)

        val sets = mutableListOf<MaterialCardView>()

        val settings = this.activity?.getSharedPreferences("setsStats", 0)

        fun dpToPx(dp: Int): Float {
            return (dp * getSystem().displayMetrics.density)
        }

        fun setCurrentSet(set: String) {
            setFragmentResult("currentSet", bundleOf("currentSet" to set))
            activity?.applicationContext?.openFileOutput("currentSet.txt", Context.MODE_PRIVATE).use {
                it?.write(set.toByteArray())
            }

            val setData = set.split("--")

            var currentTermCount = 0
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
                currentSetTick.text = settings?.getInt("${setData[0]}--${setData[1]}--ticks",0).toString()
                currentSetCross.text = settings?.getInt("${setData[0]}--${setData[1]}--crosses",0).toString()
                currentSetTitle.requestLayout()
                currentSetRename.requestLayout()
                layout.requestLayout()
            }
        }

        fun addCard(date: String, title: String, terms: Int, setFileName: String, border: Boolean = false) {
            val myCard = context?.let { MaterialCardView(it) }
            val mySetTitle = context?.let{ TextView(it) }
            val myTermChip = context?.let { Chip(it) }
            val myDateChip = context?.let { Chip(it) }
            val myTickChip = context?.let { Chip(it) }
            val myCrossChip = context?.let { Chip(it) }
            val myBinButton = context?.let { ImageButton(it) }
            val myRenameButton = context?.let { ImageButton(it) }

            val cardLayoutParams = ConstraintLayout.LayoutParams(0,dpToPx(50).toInt())
            val setTitleLayoutParams = ConstraintLayout.LayoutParams(0,LayoutParams.WRAP_CONTENT)
            val termChipLayoutParams = ConstraintLayout.LayoutParams(LayoutParams.WRAP_CONTENT,dpToPx(30).toInt())
            val dateChipLayoutParams = ConstraintLayout.LayoutParams(LayoutParams.WRAP_CONTENT,dpToPx(30).toInt())
            val tickChipLayoutParams = ConstraintLayout.LayoutParams(LayoutParams.WRAP_CONTENT,dpToPx(30).toInt())
            val crossChipLayoutParams = ConstraintLayout.LayoutParams(LayoutParams.WRAP_CONTENT,dpToPx(30).toInt())
            val binButtonLayoutParams = ConstraintLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT)
            val renameButtonLayoutParams = ConstraintLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT)

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
            renameButtonLayoutParams.setMargins(dpToPx(8).toInt(),0,0,0)

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

            myRenameButton?.setBackgroundColor(ContextCompat.getColor(requireActivity(), android.R.color.transparent))
            myRenameButton?.setImageResource(R.drawable.rename)
            myRenameButton?.setColorFilter(ContextCompat.getColor(requireActivity(), R.color.black))

            val numTermsText = "$terms terms"
            mySetTitle?.text = title
            myTermChip?.text = numTermsText
            myDateChip?.text = date

            val fileFormatDate = date.split("/").reversed().joinToString(separator = "")

            myTickChip?.text = settings?.getInt("${fileFormatDate}--$title--ticks",0).toString()
            myCrossChip?.text = settings?.getInt("${fileFormatDate}--$title--crosses",0).toString()

            myCard?.layoutParams = cardLayoutParams
            mySetTitle?.layoutParams = setTitleLayoutParams
            myTermChip?.layoutParams = termChipLayoutParams
            myDateChip?.layoutParams = dateChipLayoutParams
            myTickChip?.layoutParams = tickChipLayoutParams
            myCrossChip?.layoutParams = crossChipLayoutParams
            myBinButton?.layoutParams = binButtonLayoutParams
            myRenameButton?.layoutParams = renameButtonLayoutParams

            myCard?.id = View.generateViewId()
            mySetTitle?.id = View.generateViewId()
            myTermChip?.id = View.generateViewId()
            myDateChip?.id = View.generateViewId()
            myTickChip?.id = View.generateViewId()
            myCrossChip?.id = View.generateViewId()
            myBinButton?.id = View.generateViewId()
            myRenameButton?.id = View.generateViewId()

            myCard?.setOnClickListener{

                for (card in sets){
                    card.strokeWidth = 0
                }

                myCard.strokeColor = ContextCompat.getColor(requireActivity(), R.color.highlight)
                myCard.strokeWidth = dpToPx(3).toInt()

                setCurrentSet(setFileName)
            }

            layout.addView(myCard)
            layout.addView(mySetTitle)
            layout.addView(myTermChip)
            layout.addView(myDateChip)
            layout.addView(myTickChip)
            layout.addView(myCrossChip)
            layout.addView(myBinButton)
            layout.addView(myRenameButton)

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


            myRenameButton?.id?.let { mySetTitle?.id?.let { it1 ->
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
                layout.removeView(myRenameButton)

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

            cardConstraintParams.applyTo(layout)
            layout.requestLayout()

            if (myCard != null) {
                sets.add(myCard)
            }

        }

        fun addSet() {
            requireActivity().runOnUiThread {
                Toast.makeText(
                    activity?.applicationContext,
                    "Fetching cards, this may take a while for a large set",
                    Toast.LENGTH_LONG
                ).show()
            }
            var date = ""
            var title = ""
            var terms = 0
            val fileRead = Thread {
                val url = inputText.text.toString()
                var doc: String
                try {
                    doc = Jsoup.connect(url)
                        .maxBodySize(0)
                        .timeout(0)
                        .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:109.0) Gecko/20100101 Firefox/116.0")
                        .get().toString()
                } catch (e: Exception) {
                    doc = "ERROR"
                }

                if (doc != "ERROR") {
                    requireActivity().runOnUiThread {
                        Toast.makeText(
                                activity?.applicationContext,
                                "Parsing cards",
                                Toast.LENGTH_LONG
                            ).show()
                    }
                    val cards = Regex("""\\"label\\":\\"(?:word|definition)\\",\\"media\\":\[\{\\"type\\":1,\\"plainText\\":\\"(.+?)\\""").findAll(doc)
                    title = Regex("""<title>(.+?) Flashcards""").findAll(doc).toList()[0].groups[1]?.value.toString()
                    date = SimpleDateFormat("dd/MM/yy").format(Calendar.getInstance().time).toString()
                    val fileDate = SimpleDateFormat("yyMMdd").format(Calendar.getInstance().time).toString()
                    terms = (cards.toList().size/2).toInt()
                    var counter = 0

                    val file = File(context?.filesDir, "$fileDate--$title.txt")
                    if (file.exists()) {
                        file.delete()
                    }

                    if (cards.toList().size > 1) {
                        activity?.applicationContext?.openFileOutput(
                            "$fileDate--$title.txt",
                            Context.MODE_PRIVATE
                        )
                            .use {
                                for (card in cards) {
                                    it?.write(
                                        card.groups[1]?.value?.toByteArray()
                                            ?: "ERROR".toByteArray()
                                    )
                                    if ((counter % 2 == 1) && (counter != (cards.toList().size - 1))) {
                                        it?.write("\n".toByteArray())
                                    } else if (counter != (cards.toList().size - 1)) {
                                        it?.write("||".toByteArray())
                                    }
                                counter++
                                }
                                it?.flush()
                                it?.close()
                            }

                        requireActivity().runOnUiThread {
                            addCard(date, title, terms, "$fileDate--$title.txt", border = true)
                            Toast.makeText(
                                activity?.applicationContext,
                                "Set initialised",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        setCurrentSet("$fileDate--$title.txt")
                    } else {
                        requireActivity().runOnUiThread {
                            Toast.makeText(
                                activity?.applicationContext,
                                "Invalid link",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                } else {
                    requireActivity().runOnUiThread {
                        Toast.makeText(
                            activity?.applicationContext,
                            "Connection error",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

            fileRead.start()
        }

        inputBox.setEndIconOnClickListener{
            addSet()
            inputBox.clearFocus()
            inputText.text?.clear()
        }


        val currentSetFile = File(context?.filesDir?.path, "currentSet.txt")
        var currentSetFileNameInit = ""
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
        files?.forEach {
            if ("-" in it.toString()) {
                val fileNameInit = it.toString().split("/").last()
                val fileData = fileNameInit.split("--")
                var termCount = 0
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
}