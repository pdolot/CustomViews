package com.dolotdev.customviewslib.chart

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.dolotdev.customviewslib.R
import com.dolotdev.customviewslib.util.ViewUtil
import kotlin.math.PI
import kotlin.math.min

class CircleChart @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private var strokeWidth = 0f
        set(value) {
            field = value
            paint.strokeWidth = field
        }

    private var maxValue = 0f
    private var spaceBetween = 0f

    private var colorsRef: TypedArray? = null
    private var colors = ArrayList<Int>()

    private var dataRef: TypedArray? = null
    private var data = ArrayList<Float>()

    private var bound: RectF? = null
    private var circleRadius = 0f
        set(value) {
            field = value
            circleLength = 2f * PI.toFloat() * field
        }

    private var angles = ArrayList<Float>()

    private var circleLength = 0f

    init {
        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CircleChart,
            defStyleAttr,
            0
        )

        strokeWidth = a.getDimensionPixelSize(R.styleable.CircleChart_strokeWidth, 2).toFloat()
        spaceBetween = a.getDimensionPixelSize(R.styleable.CircleChart_spaceBetween, 0).toFloat()

        colorsRef = resources.obtainTypedArray(
            a.getResourceId(
                R.styleable.CircleChart_colors,
                R.array.colors
            )
        )

        dataRef = resources.obtainTypedArray(
            a.getResourceId(
                R.styleable.CircleChart_dataRef,
                R.array.circleChartData
            )
        )

        dataRef?.let {
            for (i in 0 until it.length()) {
                data.add(it.getFloat(i, 0f))
            }

            it.recycle()
        }

        data.forEach {
            maxValue += it
        }

        measureAngles()

        colorsRef?.let {
            for (i in 0 until data.size) {
                colors.add(it.getColor(i % it.length(), 0))
            }

            it.recycle()
        }

        a.recycle()
    }

    fun setData(data: List<Float>) {
        this.data.clear()
        this.data.addAll(data)
        measureAngles()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val spec =
            if (measuredHeight > measuredWidth) widthMeasureSpec else if (measuredWidth > measuredHeight) heightMeasureSpec else widthMeasureSpec
        val min = min(measuredWidth, measuredHeight)

        setMeasuredDimension(
            ViewUtil.measureDimension(min, spec),
            ViewUtil.measureDimension(min, spec)
        )

        measureBounds()
    }

    private fun measureAngles() {
        angles.clear()
        data.forEach {
            angles.add((it * 360f) / maxValue)
        }
    }

    private fun measureBounds() {
        val offset = strokeWidth / 2
        bound = RectF(
            0f + paddingStart + offset,
            0f + paddingTop + offset,
            measuredWidth.toFloat() - paddingEnd - offset,
            measuredHeight.toFloat() - paddingBottom - offset
        )

        bound?.let {
            circleRadius = (it.bottom - it.top) / 2
        }

    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val path = Path()
        var startAngle = 0f
        val offset = (spaceBetween / circleLength) * 360

        for (i in 0 until data.size) {
            path.reset()
            path.addArc(
                bound,
                startAngle + (offset / 2f),
                angles[i] - offset
            )

            startAngle += angles[i]

            canvas.drawPath(path, paint.apply {
                color = colors[i]
            })
        }
    }

}