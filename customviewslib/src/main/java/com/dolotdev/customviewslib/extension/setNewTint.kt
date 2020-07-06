package com.dolotdev.customviewslib.extension

import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.DrawableCompat

fun Drawable?.setNewTint(color: Int) {
    this?.mutate()?.let {
        val drawable = DrawableCompat.wrap(it)
        DrawableCompat.setTint(drawable, color)
    }
}
