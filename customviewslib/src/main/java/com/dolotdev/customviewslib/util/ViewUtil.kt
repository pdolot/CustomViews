package com.dolotdev.customviewslib.util

import android.view.View
import kotlin.math.min

object ViewUtil {

    fun measureDimension(desiredSize: Int, measureSpec: Int): Int {
        var result: Int
        val specMode = View.MeasureSpec.getMode(measureSpec)
        val specSize = View.MeasureSpec.getSize(measureSpec)

        if (specMode == View.MeasureSpec.EXACTLY) {
            result = specSize
        } else {
            result = desiredSize
            if (specMode == View.MeasureSpec.AT_MOST) {
                result = min(result, specSize)
            }
        }
        return result
    }

    fun measureIntersectionPoint(
        xA: Float,
        yA: Float,
        xB: Float,
        yB: Float,
        xC: Float,
        yC: Float,
        xD: Float,
        yD: Float
    ): Pair<Float, Float> {
        val dxAC = xC - xA
        val dyAC = yC - yA
        val dxAB = xB - xA
        val dyAB = yB - yA
        val dxCD = xD - xC
        val dyCD = yD - yC

        val k = ((dxAC * dyCD) - (dxCD * dyAC)) / ((dxAB * dyCD) - (dxCD * dyAB))

        val dxAP = k * dxAB
        val dyAP = k * dyAB

        val xP = xA + dxAP
        val yP = yA + dyAP

        return Pair(xP, yP)
    }
}