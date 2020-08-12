package com.dolotdev.customviewslib.extension

import android.view.View

fun View?.translateTo(translation: Float) {
    this?.translationX = translation
}

fun View?.translateBy(translation: Float) {
    this?.apply {
        translationX += translation
    }
}