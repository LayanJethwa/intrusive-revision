package com.example.intrusiverevision

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import java.io.File

class HomeFragment : Fragment(R.layout.home_layout) {

    private var currentSet = "NONE"
    private var flashcards = listOf("")

    private var fullQuestion = listOf("NONE","NONE")
    private var currentTerm = fullQuestion[0]
    private var currentDef = fullQuestion[1]

    lateinit var buttonTopLeft: Button
    lateinit var buttonTopRight: Button
    lateinit var buttonBottomLeft: Button
    lateinit var buttonBottomRight: Button
    lateinit var questionText: TextView

    private fun nextQuestion(term: String, ans: String): List<String> {
        if (currentSet == "NONE") {
            Toast.makeText(
                activity?.applicationContext,
                "No set initialised",
                Toast.LENGTH_LONG
            ).show()
            return listOf("NONE", "NONE")
        } else {
            var term: String
            var answer = ""

            var question = flashcards.random().split("||")
            term = question[0]
            answer = question[1]

            while (("$term||$ans" in flashcards) || (term == "")) {
                question = flashcards.random().split("||")
                term = question[0]
                answer = question[1]
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFragmentResultListener("currentSet") { requestKey, bundle ->
            currentSet = bundle.getString("currentSet").toString()
            context?.openFileInput(currentSet).use { stream ->
                flashcards = stream?.bufferedReader().use { it?.readText() ?: "ERROR" }.split("\n")
                fullQuestion = nextQuestion("NONE", "NONE")
                currentTerm = fullQuestion[0]
                currentDef = fullQuestion[1]
            }
        }
    }

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

        fun validateInput(expected: String, clicked: String, term: String): Unit {
            if (currentSet != "NONE") {
                if (expected == clicked) {
                    validate.setCardBackgroundColor(
                        ContextCompat.getColor(
                            requireActivity(),
                            R.color.pastel_green
                        )
                    )
                    validateText.text = getString(R.string.correct_text)
                } else {
                    validate.setCardBackgroundColor(
                        ContextCompat.getColor(
                            requireActivity(),
                            R.color.pastel_red
                        )
                    )
                    validateText.text = getString(R.string.incorrect_text)
                }
                termText.text = term
                definitionText.text = expected
                equalsText.text = getString(R.string.equals_text)
            }
        }



        fullQuestion = nextQuestion("NONE", "NONE")
        currentTerm = fullQuestion[0]
        currentDef = fullQuestion[1]

        buttonTopLeft.setOnClickListener {
            validateInput(currentDef, buttonTopLeft.text.toString(), currentTerm)
            fullQuestion = nextQuestion(currentTerm, currentDef)
            currentTerm = fullQuestion[0]
            currentDef = fullQuestion[1]
        }
        buttonTopRight.setOnClickListener {
            validateInput(currentDef, buttonTopRight.text.toString(), currentTerm)
            fullQuestion = nextQuestion(currentTerm, currentDef)
            currentTerm = fullQuestion[0]
            currentDef = fullQuestion[1]
        }
        buttonBottomLeft.setOnClickListener {
            validateInput(currentDef, buttonBottomLeft.text.toString(), currentTerm)
            fullQuestion = nextQuestion(currentTerm, currentDef)
            currentTerm = fullQuestion[0]
            currentDef = fullQuestion[1]
        }
        buttonBottomRight.setOnClickListener {
            validateInput(currentDef, buttonBottomRight.text.toString(), currentTerm)
            fullQuestion = nextQuestion(currentTerm, currentDef)
            currentTerm = fullQuestion[0]
            currentDef = fullQuestion[1]
        }

        return fragmentView
    }
}