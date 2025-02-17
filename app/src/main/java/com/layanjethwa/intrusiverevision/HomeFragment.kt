package com.layanjethwa.intrusiverevision

import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import com.google.android.material.chip.Chip
import java.io.File
import kotlin.random.Random

class HomeFragment : Fragment(R.layout.home_layout) {

    private var currentSet = "NONE"
    private var flashcards = listOf("")

    private var fullQuestion = listOf("NONE","NONE")
    private var currentTerm = fullQuestion[0]
    private var currentDef = fullQuestion[1]

    private lateinit var buttonTopLeft: Button
    private lateinit var buttonTopRight: Button
    private lateinit var buttonBottomLeft: Button
    private lateinit var buttonBottomRight: Button
    private lateinit var questionText: TextView

    private lateinit var tickLeftChip: Chip
    private lateinit var crossLeftChip: Chip
    private lateinit var tickRightChip: Chip
    private lateinit var crossRightChip: Chip

    private var sessionTicks = 0
    private var sessionCrosses = 0
    private var globalTicks = 0
    private var globalCrosses = 0

    private var settings = this.activity?.getSharedPreferences("setsStats", 0)
    private var editor = settings?.edit()

    private var statsType = "session"
    private var questionType = "spaced"
    private val allCorrectScore = 0.1F
    private val allIncorrectScore = 10F
    private val newQuestionScore = 5F

    private fun nextQuestion(ans: String): List<String> {
        if (currentSet == "NONE") {
            Toast.makeText(
                activity?.applicationContext,
                "No set initialised",
                Toast.LENGTH_LONG
            ).show()
            return listOf("NONE", "NONE")
        } else {
            var term = ""
            var answer = ""

            if (questionType == "random") {

                var question = flashcards.random().split("||")
                term = question[0]
                answer = question[1]

                while (("$term||$ans" in flashcards) || (term == "")) {
                    question = flashcards.random().split("||")
                    term = question[0]
                    answer = question[1]
                }
            } else {
                val ratios = mutableListOf<Float>()
                for (card in flashcards) {
                    val ticks = settings?.getInt("${currentSet.replace(".txt","--ticks")}--${card.replace("||","--")}",0)?.toFloat()
                    val crosses = settings?.getInt("${currentSet.replace(".txt","--crosses")}--${card.replace("||","--")}",0)?.toFloat()
                    if (crosses == 0F) {
                        if (ticks != 0F) {
                            ratios.add(allCorrectScore / ticks!!)
                        } else {
                            ratios.add(newQuestionScore)
                        }
                    } else if (ticks == 0F) {
                        if (crosses != 0F) {
                            ratios.add(allIncorrectScore)
                        } else {
                            ratios.add(newQuestionScore)
                        }
                    } else {
                        ratios.add(crosses!! / ticks!!)
                    }
                }
                val sumRatios = ratios.sum()
                val newRatios = ratios.map { it / sumRatios}
                var currentScore = 0F
                val cumulativeRatios = mutableListOf<Float>()
                for (ratio in newRatios) {
                    currentScore += ratio
                    cumulativeRatios.add(currentScore)
                }

                while (("$term||$ans" in flashcards) || (term == "")) {
                    val randomScore = Random.nextFloat()
                    var currentCard = 0

                    while (randomScore > cumulativeRatios[currentCard]) {
                        currentCard++
                    }
                    val question = flashcards[currentCard].split("||")
                    term = question[0]
                    answer = question[1]
                }
            }

            val answers = mutableListOf(answer)
            var newAns = answer
            for (i in 1..3) {
                while (newAns in answers) {
                    newAns = flashcards.random().split("||")[1]
                }
                answers.add(newAns)
            }

            newAns = answers.random()
            buttonTopLeft.text = newAns
            answers.remove(newAns)

            newAns = answers.random()
            buttonTopRight.text = newAns
            answers.remove(newAns)

            newAns = answers.random()
            buttonBottomLeft.text = newAns
            answers.remove(newAns)

            newAns = answers.random()
            buttonBottomRight.text = newAns
            answers.remove(newAns)

            questionText.text = term

            return listOf(term, answer)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settings = this.activity?.getSharedPreferences("setsStats", 0)
        editor = settings?.edit()

        setFragmentResultListener("currentSet") { _, bundle ->
            currentSet = bundle.getString("currentSet").toString()
            context?.openFileInput(currentSet).use { stream ->
                flashcards = stream?.bufferedReader().use { it?.readText() ?: "ERROR" }.split("\n")
                fullQuestion = nextQuestion("NONE")
                currentTerm = fullQuestion[0]
                currentDef = fullQuestion[1]
                sessionTicks = 0
                sessionCrosses = 0
                globalTicks = settings?.getInt(currentSet.replace(".txt","--ticks"),0)!!
                globalCrosses = settings?.getInt(currentSet.replace(".txt","--crosses"),0)!!
                if (statsType == "session") {
                    tickLeftChip.text = "0"
                    crossLeftChip.text = "0"
                } else if (statsType == "global") {
                    tickLeftChip.text = globalTicks.toString()
                    crossLeftChip.text = globalCrosses.toString()
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val fragmentView = inflater.inflate(R.layout.home_layout, container, false)

        val currentSetFile = File(context?.filesDir?.path, "currentSet.txt")
        if (currentSetFile.exists()) {
            context?.openFileInput("currentSet.txt").use { stream ->
                currentSet = stream?.bufferedReader().use {
                    it?.readText().toString()
                }
            }
        }

        val setFile = File(context?.filesDir?.path, currentSet)
        if (setFile.exists() && currentSet != "NONE") {
            context?.openFileInput(currentSet).use { stream ->
                flashcards = stream?.bufferedReader().use { it?.readText() ?: "ERROR" }.split("\n")
            }
        }

        buttonTopLeft = fragmentView.findViewById(R.id.buttonTopLeft)
        buttonTopRight = fragmentView.findViewById(R.id.buttonTopRight)
        buttonBottomLeft = fragmentView.findViewById(R.id.buttonBottomLeft)
        buttonBottomRight = fragmentView.findViewById(R.id.buttonBottomRight)
        questionText = fragmentView.findViewById(R.id.questionText)

        val validate: androidx.cardview.widget.CardView = fragmentView.findViewById(R.id.validate)
        val validateText: TextView = fragmentView.findViewById(R.id.validateText)
        val termText: TextView = fragmentView.findViewById(R.id.termText)
        val definitionText: TextView = fragmentView.findViewById(R.id.definitionText)
        val equalsText: TextView = fragmentView.findViewById(R.id.equalsText)

        tickLeftChip = fragmentView.findViewById(R.id.tickLeftChip)
        crossLeftChip = fragmentView.findViewById(R.id.crossLeftChip)
        tickRightChip = fragmentView.findViewById(R.id.tickRightChip)
        crossRightChip = fragmentView.findViewById(R.id.crossRightChip)
        val statsSwitch: SwitchCompat = fragmentView.findViewById(R.id.statsSwitch)
        val checkBox: CheckBox = fragmentView.findViewById(R.id.checkBox)

        val correctSound = MediaPlayer.create(context, R.raw.correct)
        val incorrectSound = MediaPlayer.create(context, R.raw.incorrect)
        val muteButton: ImageView = fragmentView.findViewById(R.id.muteButton)
        var muted = requireActivity().getSharedPreferences("globalSettings", 0)?.getBoolean("muted", false)!!

        if (!muted) {
            muteButton.setImageResource(R.drawable.volume)
            muteButton.setColorFilter(ContextCompat.getColor(requireActivity(), R.color.pastel_green))
        } else {
            muteButton.setImageResource(R.drawable.mute)
            muteButton.setColorFilter(ContextCompat.getColor(requireActivity(), R.color.pastel_red))
        }

        @SuppressLint("SetTextI18n")
        fun validateInput(expected: String, clicked: String, term: String) {
            if (currentSet != "NONE") {
                if (expected == clicked) {
                    if (!muted) {
                        correctSound.start()
                        correctSound.pause()
                        correctSound.seekTo(0)
                        correctSound.start()
                    }
                    validate.setCardBackgroundColor(
                        ContextCompat.getColor(
                            requireActivity(),
                            R.color.pastel_green
                        )
                    )
                    validateText.text = getString(R.string.correct_text)

                    sessionTicks ++
                    globalTicks ++

                    editor?.putInt("${currentSet.replace(".txt","--ticks")}--${term}--${expected}",
                        settings?.getInt("${currentSet.replace(".txt","--ticks")}--${term}--${expected}",0)
                            ?.plus(1) ?: 1
                    )

                    editor?.putInt(currentSet.replace(".txt","--ticks"),globalTicks)
                    editor?.apply()
                    if (statsType == "session") {
                        tickLeftChip.text = sessionTicks.toString()
                    } else if (statsType == "global") {
                        tickLeftChip.text = globalTicks.toString()
                    }

                } else {
                    if (!muted) {
                        incorrectSound.start()
                        incorrectSound.pause()
                        incorrectSound.seekTo(0)
                        incorrectSound.start()
                    }
                    validate.setCardBackgroundColor(
                        ContextCompat.getColor(
                            requireActivity(),
                            R.color.pastel_red
                        )
                    )
                    validateText.text = getString(R.string.incorrect_text)

                    sessionCrosses ++
                    globalCrosses ++

                    editor?.putInt("${currentSet.replace(".txt","--crosses")}--${term}--${expected}",
                        settings?.getInt("${currentSet.replace(".txt","--crosses")}--${term}--${expected}",0)
                            ?.plus(1) ?: 1
                    )

                    editor?.putInt(currentSet.replace(".txt","--crosses"),globalCrosses)
                    editor?.apply()
                    if (statsType == "session") {
                        crossLeftChip.text = sessionCrosses.toString()
                    } else if (statsType == "global") {
                        crossLeftChip.text = globalCrosses.toString()
                    }

                }
                tickRightChip.text = settings?.getInt("${currentSet.replace(".txt","--ticks")}--${term}--${expected}",0).toString()
                crossRightChip.text = settings?.getInt("${currentSet.replace(".txt","--crosses")}--${term}--${expected}",0).toString()
                termText.text = term
                definitionText.text = expected
                equalsText.text = getString(R.string.equals_text)
            }
        }



        fullQuestion = nextQuestion("NONE")
        currentTerm = fullQuestion[0]
        currentDef = fullQuestion[1]

        buttonTopLeft.setOnClickListener {
            validateInput(currentDef, buttonTopLeft.text.toString(), currentTerm)
            fullQuestion = nextQuestion(currentDef)
            currentTerm = fullQuestion[0]
            currentDef = fullQuestion[1]
        }
        buttonTopRight.setOnClickListener {
            validateInput(currentDef, buttonTopRight.text.toString(), currentTerm)
            fullQuestion = nextQuestion(currentDef)
            currentTerm = fullQuestion[0]
            currentDef = fullQuestion[1]
        }
        buttonBottomLeft.setOnClickListener {
            validateInput(currentDef, buttonBottomLeft.text.toString(), currentTerm)
            fullQuestion = nextQuestion(currentDef)
            currentTerm = fullQuestion[0]
            currentDef = fullQuestion[1]
        }
        buttonBottomRight.setOnClickListener {
            validateInput(currentDef, buttonBottomRight.text.toString(), currentTerm)
            fullQuestion = nextQuestion(currentDef)
            currentTerm = fullQuestion[0]
            currentDef = fullQuestion[1]
        }
        statsSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                statsType = "global"
                tickLeftChip.text = globalTicks.toString()
                crossLeftChip.text = globalCrosses.toString()
            } else {
                statsType = "session"
                tickLeftChip.text = sessionTicks.toString()
                crossLeftChip.text = sessionCrosses.toString()
            }
        }
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            questionType = if (isChecked) {
                "spaced"
            } else {
                "random"
            }
        }
        muteButton.setOnClickListener {
            if (muted) {
                muted = false
                this.activity?.getSharedPreferences("globalSettings", 0)?.edit()?.putBoolean("muted", false)?.apply()
                muteButton.setImageResource(R.drawable.volume)
                muteButton.setColorFilter(ContextCompat.getColor(requireActivity(), R.color.pastel_green))
            } else {
                muted = true
                this.activity?.getSharedPreferences("globalSettings", 0)?.edit()?.putBoolean("muted", true)?.apply()
                muteButton.setImageResource(R.drawable.mute)
                muteButton.setColorFilter(ContextCompat.getColor(requireActivity(), R.color.pastel_red))
            }
        }

        return fragmentView
    }
}