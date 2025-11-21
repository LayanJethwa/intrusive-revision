package com.layanjethwa.intrusiverevision

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.graphics.PixelFormat
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import java.io.File
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.timerTask
import kotlin.random.Random


@SuppressLint("InflateParams", "SetTextI18n")
class Window(
    private val context: Context
) {
    var onClose: (() -> Unit)? = null

    private val mView: View
    private var mParams: WindowManager.LayoutParams? = null
    private val mWindowManager: WindowManager
    private val layoutInflater: LayoutInflater


    private var selectedSets = mutableListOf<String>()
    private var flashcards = mutableListOf<String>()
    private var currentQuestionSetFile = ""

    private var fullQuestion = listOf("NONE","NONE")
    private var currentTerm = fullQuestion[0]
    private var currentDef = fullQuestion[1]

    private var buttonTopLeft: Button
    private var buttonTopRight: Button
    private var buttonBottomLeft: Button
    private var buttonBottomRight: Button
    private var questionText: TextView

    private var tickLeftChip: Chip
    private var crossLeftChip: Chip
    private var tickRightChip: Chip
    private var crossRightChip: Chip

    private var validate: androidx.cardview.widget.CardView
    private var validateText: TextView
    private var termText: TextView
    private var definitionText: TextView
    private var equalsText: TextView

    private var statsSwitch: SwitchCompat
    private var checkBox: CheckBox

    private var scoreText: TextView
    private var closeButton: MaterialButton

    private var sessionTicks = 0
    private var sessionCrosses = 0
    private var globalTicks = 0
    private var globalCrosses = 0

    private var settings = context.getSharedPreferences("setsStats", 0)
    private var editor = settings?.edit()

    private var statsType = "session"
    private var questionType = context.getSharedPreferences("globalSettings", 0).getString("questionType", "random")
    private val allCorrectScore = 0.1F
    private val allIncorrectScore = 10F
    private val newQuestionScore = 5F

    private var remainingQuestions = 0
    private var interval: Long = 0L
    private var newQuestions: Int = 0
    private var penaltyQuestions: Int = 0

    private var settingsType: String = "globalSettings"

    private val correctSound = MediaPlayer.create(context, R.raw.correct)
    private val incorrectSound = MediaPlayer.create(context, R.raw.incorrect)
    private var muted = context.getSharedPreferences("globalSettings", 0).getBoolean("muted", false)
    private var muteButton: ImageView

    companion object {
        @SuppressLint("StaticFieldLeak")
        var activeWindow: Window? = null
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.OPAQUE
            )
        }
        context.setTheme(R.style.Theme_IntrusiveRevision)
        layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        mView = layoutInflater.inflate(R.layout.popup_layout, null)

        buttonTopLeft = mView.findViewById(R.id.buttonTopLeft)
        buttonTopRight = mView.findViewById(R.id.buttonTopRight)
        buttonBottomLeft = mView.findViewById(R.id.buttonBottomLeft)
        buttonBottomRight = mView.findViewById(R.id.buttonBottomRight)
        questionText = mView.findViewById(R.id.questionText)

        validate = mView.findViewById(R.id.validate)
        validateText = mView.findViewById(R.id.validateText)
        termText = mView.findViewById(R.id.termText)
        definitionText = mView.findViewById(R.id.definitionText)
        equalsText = mView.findViewById(R.id.equalsText)

        tickLeftChip = mView.findViewById(R.id.tickLeftChip)
        crossLeftChip = mView.findViewById(R.id.crossLeftChip)
        tickRightChip = mView.findViewById(R.id.tickRightChip)
        crossRightChip = mView.findViewById(R.id.crossRightChip)
        statsSwitch = mView.findViewById(R.id.statsSwitch)
        checkBox = mView.findViewById(R.id.checkBox)

        scoreText = mView.findViewById(R.id.scoreText)
        closeButton = mView.findViewById(R.id.closeButton)
        muteButton = mView.findViewById(R.id.muteButton)

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
        if (questionType == "random"){
            checkBox.isChecked = false
        } else if (questionType == "spaced") {
            checkBox.isChecked = true
        }
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            questionType = if (isChecked) {
                "spaced"
            } else {
                "random"
            }
            context.getSharedPreferences("globalSettings", 0).edit().putString("questionType", questionType).apply()
        }
        muteButton.setOnClickListener {
            if (muted) {
                muted = false
                context.getSharedPreferences("globalSettings", 0).edit().putBoolean("muted", false).apply()
                muteButton.setImageResource(R.drawable.volume)
                muteButton.setColorFilter(ContextCompat.getColor(context, R.color.pastel_green))
            } else {
                muted = true
                context.getSharedPreferences("globalSettings", 0).edit().putBoolean("muted", true).apply()
                muteButton.setImageResource(R.drawable.mute)
                muteButton.setColorFilter(ContextCompat.getColor(context, R.color.pastel_red))
            }
        }

        closeButton.setOnClickListener {
            val handler = Handler(Looper.getMainLooper())
            val timerTask = object : TimerTask() {
                override fun run() {
                    context.getSharedPreferences("appRunning",0).edit().putInt("currentTimers",0).apply()
                    handler.post {
                        val window = Window(context = context)
                        window.open(settingsType)
                    }
                }
            }

            if (closeButton.isEnabled) {
                Timer().schedule(timerTask { close() }, 100)
                if (interval > 0 && newQuestions > 0 && context.getSharedPreferences("appRunning", 0).getString("currentApp","") == settingsType
                    && context.getSharedPreferences("appRunning",0).getInt("currentTimers",0) == 0) {
                    Timer().schedule(timerTask, interval*1000*60)
                    context.getSharedPreferences("appRunning",0).edit().putInt("currentTimers",1).apply()
                }
            }
        }

        mParams!!.gravity = Gravity.TOP or Gravity.START
        mParams!!.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        mWindowManager = context.getSystemService(WINDOW_SERVICE) as WindowManager
    }

    fun open(thisSettingsType: String = "globalSettings") {
        Log.i("Window", "Window open called")

        if (activeWindow != null) return
        activeWindow = this

        settingsType = thisSettingsType
        newQuestions =  context.getSharedPreferences(settingsType,0).getInt("newQuestions",0)
        penaltyQuestions = context.getSharedPreferences(settingsType,0).getInt("penaltyQuestions",0)
        interval = context.getSharedPreferences(settingsType,0).getInt("timeInterval",0).toLong()
        remainingQuestions = newQuestions
        try {
            if (mView.parent == null && remainingQuestions != 0) {
                    mWindowManager.addView(mView, mParams)
                    scoreText.text = "0/$newQuestions"
                    context.getSharedPreferences("appRunning",0).edit().putBoolean("serviceActive",true).apply()

                    val savedSets = settings?.getString("selectedSetsList", "") ?: ""
                    if (savedSets.isNotBlank()) {
                        selectedSets = savedSets.split(",").toMutableList()
                        flashcards.clear()
                        for (setFile in selectedSets) {
                            val file = File(context.filesDir, setFile)
                            if (file.exists()) {
                                context.openFileInput(setFile).use { stream ->
                                    val content = stream?.bufferedReader()?.readText() ?: ""
                                    flashcards.addAll(
                                        content.split("\n").filter { it.contains("||") && it.isNotBlank() }
                                    )
                                }
                            }
                        }
                    }

                    fullQuestion = nextQuestion("NONE")
                    currentTerm = fullQuestion[0]
                    currentDef = fullQuestion[1]

                    sessionTicks = 0
                    sessionCrosses = 0
                    globalTicks = selectedSets.sumOf { settings?.getInt(it.replace(".txt", "--ticks"), 0) ?: 0 }
                    globalCrosses = selectedSets.sumOf { settings?.getInt(it.replace(".txt", "--crosses"), 0) ?: 0 }
                    if (statsType == "session") {
                        tickLeftChip.text = "0"
                        crossLeftChip.text = "0"
                    } else if (statsType == "global") {
                        tickLeftChip.text = globalTicks.toString()
                        crossLeftChip.text = globalCrosses.toString()
                    }

                    muted = context.getSharedPreferences("globalSettings", 0).getBoolean("muted", false)

                    if (!muted) {
                        muteButton.setImageResource(R.drawable.volume)
                        muteButton.setColorFilter(ContextCompat.getColor(context, R.color.pastel_green))
                    } else {
                        muteButton.setImageResource(R.drawable.mute)
                        muteButton.setColorFilter(ContextCompat.getColor(context, R.color.pastel_red))
                    }

            }
        } catch (e: Exception) {
            Log.d("Error1", e.toString())
        }
    }

    private fun close() {
        try {
            (context.getSystemService(WINDOW_SERVICE) as WindowManager).removeView(mView)
            mView.invalidate()
            if (mView.parent != null) {
                (mView.parent as ViewGroup).removeAllViews()
            }
        } catch (e: Exception) {
            Log.d("Error2", e.toString())
        } finally {
            activeWindow = null
            onClose?.invoke()
        }
    }

    @SuppressLint("SetTextI18n")
    fun validateInput(expected: String, clicked: String, term: String) {
        if (selectedSets.isEmpty() || currentQuestionSetFile.isEmpty()) {
            Toast.makeText(context, "No set selected", Toast.LENGTH_SHORT).show()
            return
        }

        val setName = currentQuestionSetFile.replace(".txt", "")
        val baseKey = "$setName--${term}--${expected}"

        if (expected == clicked) {
            if (!muted) {
                correctSound.start()
                correctSound.pause()
                correctSound.seekTo(0)
                correctSound.start()
            }
            validate.setCardBackgroundColor(
                ContextCompat.getColor(
                    context,
                    R.color.pastel_green
                )
            )
            validateText.text = context.getString(R.string.correct_text)

            sessionTicks ++
            globalTicks ++

            editor?.putInt("${baseKey}--ticks",
                settings?.getInt("${baseKey}--ticks", 0)?.plus(1) ?: 1
            )
            editor?.putInt("$setName--ticks",
                settings?.getInt("$setName--ticks", 0)?.plus(1) ?: 1
            )
            editor?.apply()

            if (statsType == "session") {
                tickLeftChip.text = sessionTicks.toString()
            } else if (statsType == "global") {
                tickLeftChip.text = globalTicks.toString()
            }

            if (!closeButton.isEnabled) {
                remainingQuestions--

                scoreText.text = "${newQuestions - remainingQuestions}/$newQuestions"
                if (remainingQuestions == 0) {
                    scoreText.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.green
                        )
                    )
                } else if (newQuestions - remainingQuestions >= 0) {
                    scoreText.setTextColor(ContextCompat.getColor(context, R.color.highlight))
                }
            }

            if (remainingQuestions == 0) {
                closeButton.isEnabled = true
                closeButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.highlight))
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
                    context,
                    R.color.pastel_red
                )
            )
            validateText.text = context.getString(R.string.incorrect_text)

            sessionCrosses ++
            globalCrosses ++

            editor?.putInt("${baseKey}--crosses",
                settings?.getInt("${baseKey}--crosses", 0)?.plus(1) ?: 1
            )
            editor?.putInt("$setName--crosses",
                settings?.getInt("$setName--crosses", 0)?.plus(1) ?: 1
            )
            editor?.apply()

            if (statsType == "session") {
                crossLeftChip.text = sessionCrosses.toString()
            } else if (statsType == "global") {
                crossLeftChip.text = globalCrosses.toString()
            }
            if (!closeButton.isEnabled) {
                remainingQuestions += penaltyQuestions
                scoreText.text = "${newQuestions - remainingQuestions}/$newQuestions"

                if (newQuestions - remainingQuestions < 0) {
                    scoreText.setTextColor(ContextCompat.getColor(context, R.color.red))
                } else if (newQuestions - remainingQuestions >= 0) {
                    scoreText.setTextColor(ContextCompat.getColor(context, R.color.highlight))
                }
            }
        }
        tickRightChip.text = settings?.getInt("${baseKey}--ticks",0).toString()
        crossRightChip.text = settings?.getInt("${baseKey}--crosses",0).toString()
        termText.text = term
        definitionText.text = expected
        equalsText.text = context.getString(R.string.equals_text)
    }

    private fun nextQuestion(ans: String): List<String> {
        if (flashcards.isEmpty()) {
            Toast.makeText(
                context,
                "No set initialised",
                Toast.LENGTH_LONG
            ).show()
            return listOf("NONE", "NONE")
        }

        var term = ""
        var answer = ""
        var currentSetFile = ""

        if (questionType == "random") {

            var question = flashcards.random().split("||")
            term = question[0]
            answer = question[1]

            currentSetFile = cardSetFor("$term||$answer")

            while (("$term||$ans" in flashcards) || (term == "")) {
                question = flashcards.random().split("||")
                term = question[0]
                answer = question[1]
                currentSetFile = cardSetFor("$term||$answer")
            }

        } else {
            val ratios = mutableListOf<Float>()
            for (card in flashcards) {
                val ticks = settings?.getInt("${cardSetFor(card)}--ticks",0)?.toFloat() // CHANGED
                val crosses = settings?.getInt("${cardSetFor(card)}--crosses",0)?.toFloat() // CHANGED
                if (crosses == 0F) {
                    ratios.add(if (ticks != 0F) allCorrectScore / ticks!! else newQuestionScore)
                } else if (ticks == 0F) {
                    ratios.add(if (crosses != 0F) allIncorrectScore else newQuestionScore)
                } else {
                    ratios.add(crosses!! / ticks!!)
                }
            }
            val sumRatios = ratios.sum()
            val newRatios = ratios.map { it / sumRatios }
            val cumulativeRatios = mutableListOf<Float>()
            var currentScore = 0F
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

            currentSetFile = selectedSets.find { set ->
                File(context.filesDir, set).readText().contains("$term||$answer")
            } ?: selectedSets.firstOrNull() ?: ""
        }

        val answers = mutableListOf(answer)
        val setFlashcards = mutableListOf<String>()
        if (currentSetFile.isNotEmpty()) {
            val file = File(context.filesDir, currentSetFile)
            if (file.exists()) {
                context.openFileInput(currentSetFile).use { stream ->
                    val lines = stream?.bufferedReader()?.readText() ?: ""
                    if (lines.isNotEmpty()) {
                        setFlashcards.addAll(
                            lines.split("\n").filter { it.contains("||") && it.isNotBlank() }
                        )
                    }
                }
            }
        }

        while (answers.size < 4 && setFlashcards.isNotEmpty()) {
            val newAns = setFlashcards.random().split("||")[1]
            if (newAns !in answers) answers.add(newAns)
        }

        val shuffled = answers.shuffled()
        buttonTopLeft.text = shuffled[0]
        buttonTopRight.text = shuffled[1]
        buttonBottomLeft.text = shuffled[2]
        buttonBottomRight.text = shuffled[3]

        questionText.text = term
        currentQuestionSetFile = currentSetFile

        return listOf(term, answer)
    }

    private fun cardSetFor(card: String): String {
        return selectedSets.find { set ->
            File(context.filesDir, set).readText().contains(card)
        } ?: selectedSets.firstOrNull() ?: ""
    }
}