package com.dolotdev.customviewslib.util

import android.graphics.PointF
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object TrigonometricHelper {

    const val _4_3 = 1.3333333333333f;

    fun measureCubicBezierControlPoint(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        circleCenterX: Float,
        circleCenterY: Float
    ): Pair<PointF, PointF> {
        val ax = startX - circleCenterX
        val ay = startY - circleCenterY
        val bx = endX - circleCenterX
        val by = endY - circleCenterY
        val q1 = (ax * ax) + (ay * ay)
        val q2 = q1 + (ax * bx) + (ay * by)
        val k2 = _4_3 * ((sqrt(2f * q1 * q2) - q2) / ((ax * by) - (ay * bx)))

        val x1 = circleCenterX + ax - (k2 * ay)
        val y1 = circleCenterY + ay + (k2 * ax)

        val x2 = circleCenterX + bx + (k2 * by)
        val y2 = circleCenterY + by - (k2 * bx)

        return Pair(PointF(x1.toFloat(), -y1.toFloat()), PointF(x2.toFloat(), -y2.toFloat()))
    }

    fun measurePositionFromAngle(
        radius: Float,
        startAngle: Float,
        sweepAngle: Float,
        circleCenterPoint: PointF
    ): PointF {

        return PointF(
            circleCenterPoint.x + (radius * cos(Math.toRadians(startAngle.toDouble() + sweepAngle.toDouble()))).toInt(),
            circleCenterPoint.y + (radius * sin(Math.toRadians(startAngle.toDouble() + sweepAngle.toDouble()))).toInt()
        )
    }
}