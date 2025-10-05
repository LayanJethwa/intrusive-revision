package com.layanjethwa.intrusiverevision

import android.graphics.drawable.Drawable

data class Model(
    val name: String,
    val icon: Drawable,
    val id: String,             // package name
    var timeInterval: Int = 0,  // minutes between popups
    var newQuestions: Int = 0,  // number of new questions
    var penaltyQuestions: Int = 0 // penalty for wrong answers
)
