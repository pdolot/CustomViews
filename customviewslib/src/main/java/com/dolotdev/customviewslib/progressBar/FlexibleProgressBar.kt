package com.dolotdev.customviewslib.progressBar

import com.dolotdev.customviewslib.R
import com.dolotdev.customviewslib.util.TrigonometricHelper
import com.dolotdev.customviewslib.util.ViewUtil

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs

class FlexibleProgressBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // viewInfo
    private var widthMeasureSpec: Int = 0
    private var heightMeasureSpec: Int = 0
    private var viewMaxWidth: Int = 0
    private var viewMaxHeight: Int = 0
    private var viewMinWidth: Int = 0
    private var viewMinHeight: Int = 0
    private var viewWidth: Int = 0
    var viewHeight: Int = 0

    // viewScale
    private var viewScaleX: Float = 1f
    var viewScaleY: Float = 1f
        set(value) {
            field = value
            measureView()
            invalidate()
        }

    // viewPaint
    private var progressSecondaryPaint: Paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private var progressPaint: Paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND

    }

    private var progressColor: Int = Color.BLACK
        set(value) {
            field = value
            progressPaint.color = field
        }
    private var progressSecondaryColor: Int = 0
        set(value) {
            field = value
            progressSecondaryPaint.color = field
        }

    private var progressStrokeWidth: Float = 30f
        set(value) {
            field = value
            progressSecondaryPaint.strokeWidth = field
            progressPaint.strokeWidth = field
        }

    private lateinit var firstBackgroundArc: FlexibleBezierArc
    private lateinit var secondBackgroundArc: FlexibleBezierArc

    private var radius = 0f
    private lateinit var centerPoint: PointF
    var progress = 0f

    init {
        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.FlexibleProgressBar,
            defStyleAttr,
            0
        )
        viewScaleY = a.getFloat(R.styleable.FlexibleProgressBar_viewScaleY, 1f)
        progressSecondaryColor =
            a.getColor(R.styleable.FlexibleProgressBar_secondaryProgressColor, Color.LTGRAY)
        progressColor = a.getColor(R.styleable.FlexibleProgressBar_progressColor, Color.MAGENTA)
        progressStrokeWidth =
            a.getDimensionPixelSize(R.styleable.FlexibleProgressBar_strokeWidth, 10).toFloat()
        progress = a.getFloat(R.styleable.FlexibleProgressBar_progress, 0f)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        this.widthMeasureSpec = widthMeasureSpec
        this.heightMeasureSpec = heightMeasureSpec
        this.viewMaxWidth = measuredWidth
        this.viewMaxHeight = measuredWidth / 2

        initArc()

        this.viewMinWidth = progressStrokeWidth.toInt() + paddingLeft + paddingRight
        this.viewMinHeight = progressStrokeWidth.toInt() + paddingTop + paddingBottom
        measureView()
    }

    private fun initArc() {
        radius = (measuredWidth / 2f) - progressStrokeWidth - paddingLeft - paddingRight
        val centerX = (measuredWidth / 2f) + (abs(paddingLeft - paddingRight))
        val centerY = paddingTop + radius + (progressStrokeWidth / 2f)
        centerPoint = PointF(centerX, centerY)

        firstBackgroundArc = FlexibleBezierArc(centerPoint, radius, StartAngle.LEFT, 1.0f)
        secondBackgroundArc = FlexibleBezierArc(centerPoint, radius, StartAngle.TOP, 1.0f)

    }

    private fun measureView() {
        viewWidth = if (viewScaleX < 0.1f) {
            viewMinWidth
        } else {
            (viewMaxWidth * viewScaleX).toInt()
        }

        viewHeight = if (viewScaleY < 0.1f) {
            viewMinHeight
        } else {
            (viewMaxHeight * viewScaleY).toInt()
        }

        setMeasuredDimension(
            ViewUtil.measureDimension(viewMaxWidth, widthMeasureSpec),
            ViewUtil.measureDimension(viewMaxHeight, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.apply {
            val secondaryProgressBar = Path()
            secondaryProgressBar.reset()
            secondaryProgressBar.moveTo(
                firstBackgroundArc.startPoint.x - ((1.0f - viewScaleY) * firstBackgroundArc.firstPointXDiff),
                firstBackgroundArc.startPoint.y - ((1.0f - viewScaleY) * firstBackgroundArc.firstPointYDiff)
            )
            secondaryProgressBar.cubicTo(
                firstBackgroundArc.firstControlPoint.x - ((1.0f - viewScaleY) * firstBackgroundArc.firstControlPointXDiff),
                firstBackgroundArc.firstControlPoint.y - ((1.0f - viewScaleY) * firstBackgroundArc.firstControlPointYDiff),
                firstBackgroundArc.secondControlPoint.x - ((1.0f - viewScaleY) * firstBackgroundArc.secondControlPointXDiff),
                firstBackgroundArc.secondControlPoint.y - ((1.0f - viewScaleY) * firstBackgroundArc.secondControlPointYDiff),
                firstBackgroundArc.endPoint.x - ((1.0f - viewScaleY) * firstBackgroundArc.endPointXDiff),
                firstBackgroundArc.endPoint.y - ((1.0f - viewScaleY) * firstBackgroundArc.endPointYDiff)
            )
            secondaryProgressBar.cubicTo(
                secondBackgroundArc.firstControlPoint.x - ((1.0f - viewScaleY) * secondBackgroundArc.firstControlPointXDiff),
                secondBackgroundArc.firstControlPoint.y - ((1.0f - viewScaleY) * secondBackgroundArc.firstControlPointYDiff),
                secondBackgroundArc.secondControlPoint.x - ((1.0f - viewScaleY) * secondBackgroundArc.secondControlPointXDiff),
                secondBackgroundArc.secondControlPoint.y - ((1.0f - viewScaleY) * secondBackgroundArc.secondControlPointYDiff),
                secondBackgroundArc.endPoint.x - ((1.0f - viewScaleY) * secondBackgroundArc.endPointXDiff),
                secondBackgroundArc.endPoint.y - ((1.0f - viewScaleY) * secondBackgroundArc.endPointYDiff)
            )
            this.drawPath(secondaryProgressBar, progressSecondaryPaint)
            this.drawPath(secondaryProgressBar, progressPaint.apply {
                var len = PathMeasure(secondaryProgressBar, false).length
                pathEffect = DashPathEffect(floatArrayOf(len * progress, len * (1.0f - progress) + strokeWidth), 0f)
            })

        }
    }


    companion object {

        private enum class StartAngle {
            LEFT,
            TOP,
            RIGHT,
            BOTTOM
        }

        private class FlexibleBezierArc(
            var centerPoint: PointF,
            var radius: Float,
            var startAngle: StartAngle,
            var progress: Float
        ) {

            lateinit var firstControlPoint: PointF
            lateinit var secondControlPoint: PointF
            var angle = 90f * progress
            lateinit var startPoint: PointF
            lateinit var endPoint: PointF

            lateinit var lineStartPoint: PointF
            lateinit var lineEndPoint: PointF
            lateinit var lineFirstControlPoint: PointF
            lateinit var lineSecondControlPoint: PointF

            var firstPointXDiff = 0f
            var firstPointYDiff = 0f

            var endPointXDiff = 0f
            var endPointYDiff = 0f

            var firstControlPointXDiff = 0f
            var firstControlPointYDiff = 0f

            var secondControlPointXDiff = 0f
            var secondControlPointYDiff = 0f

            init {
                initPoints()
                measureControlPoint()
                initLinePoints()
                measureDifference()
            }

            private fun initPoints() {
                when (startAngle) {
                    StartAngle.LEFT -> {
                        startPoint = PointF(centerPoint.x - radius, centerPoint.y)
                        endPoint = TrigonometricHelper.measurePositionFromAngle(
                            radius,
                            180f,
                            angle,
                            centerPoint
                        )
                    }
                    StartAngle.TOP -> {
                        startPoint = PointF(centerPoint.x, centerPoint.y - radius)
                        endPoint = TrigonometricHelper.measurePositionFromAngle(
                            radius,
                            270f,
                            angle,
                            centerPoint
                        )
                    }
                    StartAngle.RIGHT -> {
                        startPoint = PointF(centerPoint.x + radius, centerPoint.y)
                        endPoint = TrigonometricHelper.measurePositionFromAngle(
                            radius,
                            0f,
                            angle,
                            centerPoint
                        )
                    }
                    StartAngle.BOTTOM -> {
                        startPoint = PointF(centerPoint.x, centerPoint.y + radius)
                        endPoint = TrigonometricHelper.measurePositionFromAngle(
                            radius,
                            90f,
                            angle,
                            centerPoint
                        )
                    }
                }
            }

            private fun initLinePoints() {
                when (startAngle) {
                    StartAngle.LEFT, StartAngle.RIGHT -> {
                        lineStartPoint = PointF(startPoint.x, endPoint.y)
                        lineEndPoint = PointF(endPoint.x, endPoint.y)
                        lineFirstControlPoint =
                            PointF(lineStartPoint.x + (0.33f * radius), lineStartPoint.y)
                        lineSecondControlPoint =
                            PointF(lineEndPoint.x - (0.33f * radius), lineEndPoint.y)
                    }
                    StartAngle.TOP, StartAngle.BOTTOM -> {
                        lineStartPoint = PointF(startPoint.x, startPoint.y)
                        lineEndPoint = PointF(endPoint.x, startPoint.y)
                        lineFirstControlPoint =
                            PointF(lineStartPoint.x + (0.33f * radius), lineStartPoint.y)
                        lineSecondControlPoint =
                            PointF(lineEndPoint.x - (0.33f * radius), lineEndPoint.y)
                    }
                }
            }

            private fun measureDifference() {
                firstPointXDiff = startPoint.x - lineStartPoint.x
                firstPointYDiff = startPoint.y - lineStartPoint.y

                endPointXDiff = endPoint.x - lineEndPoint.x
                endPointYDiff = endPoint.y - lineEndPoint.y

                firstControlPointXDiff = firstControlPoint.x - lineFirstControlPoint.x
                firstControlPointYDiff = firstControlPoint.y - lineFirstControlPoint.y

                secondControlPointXDiff = secondControlPoint.x - lineSecondControlPoint.x
                secondControlPointYDiff = secondControlPoint.y - lineSecondControlPoint.y
            }

            fun measureControlPoint() {
                TrigonometricHelper.measureCubicBezierControlPoint(
                    startPoint.x,
                    -startPoint.y,
                    endPoint.x,
                    -endPoint.y,
                    centerPoint.x,
                    -centerPoint.y
                ).apply {
                    firstControlPoint = this.first
                    secondControlPoint = this.second
                }
            }
        }
    }
}