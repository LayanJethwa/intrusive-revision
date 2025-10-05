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


    private var currentSet = "NONE"
    private var flashcards = listOf("")

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
    private var questionType = "spaced"
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

                    val currentSetFile = File(context.filesDir?.path, "currentSet.txt")
                    if (currentSetFile.exists()) {
                        context.openFileInput("currentSet.txt").use { stream ->
                            currentSet = stream?.bufferedReader().use {
                                it?.readText().toString()
                            }
                        }
                    }

                    val setFile = File(context.filesDir?.path, currentSet)
                    if (setFile.exists() && currentSet != "NONE") {
                        context.openFileInput(currentSet).use { stream ->
                            flashcards = stream?.bufferedReader().use { it?.readText() ?: "ERROR" }.split("\n")
                        }
                    }

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
                        context,
                        R.color.pastel_green
                    )
                )
                validateText.text = context.getString(R.string.correct_text)

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
            tickRightChip.text = settings?.getInt("${currentSet.replace(".txt","--ticks")}--${term}--${expected}",0).toString()
            crossRightChip.text = settings?.getInt("${currentSet.replace(".txt","--crosses")}--${term}--${expected}",0).toString()
            termText.text = term
            definitionText.text = expected
            equalsText.text = context.getString(R.string.equals_text)
        }
    }

    private fun nextQuestion(ans: String): List<String> {
        if (currentSet == "NONE") {
            Toast.makeText(
                context,
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
}