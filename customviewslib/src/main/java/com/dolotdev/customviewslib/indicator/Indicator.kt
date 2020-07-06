package com.dolotdev.customviewslib.indicator

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.dolotdev.customviewslib.R

class Indicator @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 20f
    }

    private var indicatorWidth = 10f
    private var color = 0
        set(value) {
            field = value
            paint.color = field
        }

    private var activeColor = 0
    private var indicatorStroke = 10f
        set(value) {
            field = value
            paint.strokeWidth = field
        }
    private var indicatorActiveStroke = 20f

    private var spaceBetween = 10f

    var count = 12
        set(value) {
            field = value
            measureBounds()
            invalidate()
        }

    private var indicatorBounds = ArrayList<RectF>()

    var active = 0
        set(value) {
            field = value
            invalidate()
        }

    init {
        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.Indicator,
            defStyleAttr,
            0
        )

        count = a.getInteger(R.styleable.Indicator_itemCount, 1)
        indicatorWidth = a.getDimensionPixelSize(R.styleable.Indicator_indicatorWidth, 10).toFloat()
        color = a.getColor(R.styleable.Indicator_color, Color.GRAY)
        activeColor = a.getColor(R.styleable.Indicator_activeColor, Color.RED)
        indicatorStroke = a.getDimensionPixelSize(R.styleable.Indicator_strokeWidth, 10).toFloat()
        indicatorActiveStroke =
            a.getDimensionPixelSize(R.styleable.Indicator_activeStrokeWidth, 10).toFloat()
        spaceBetween = a.getDimensionPixelSize(R.styleable.Indicator_spaceBetween, 10).toFloat()

        a.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        measureBounds()
    }

    private fun measureBounds() {
        indicatorBounds.clear()

        val viewStart = paddingStart
        val viewEnd = measuredWidth - paddingEnd
        val viewWidth = viewEnd - viewStart
        val centerY = measuredHeight / 2f

        var indicatorRequiredSpace = indicatorWidth + (indicatorActiveStroke / 2f) + spaceBetween

        var startX = (viewWidth - (indicatorRequiredSpace * count)) / 2f

        if (viewWidth < indicatorRequiredSpace * count) {
            indicatorRequiredSpace = (viewWidth / count).toFloat()
            startX = 0f
        }

        for (i in 0 until count) {
            val startPosition = startX + indicatorRequiredSpace * i
            indicatorBounds.add(
                RectF(
                    startPosition,
                    centerY - (indicatorActiveStroke / 2f),
                    startPosition + indicatorRequiredSpace,
                    centerY + (indicatorActiveStroke / 2f)
                )
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val padding = (spaceBetween / 2f) + (indicatorActiveStroke / 2f)

        val centerY = paddingTop + (measuredHeight - paddingBottom - paddingTop) / 2f

        for (i in 0 until count) {
            indicatorBounds[i].let {
                canvas.drawLine(
                    it.left + padding,
                    centerY,
                    it.right - padding,
                    centerY,
                    paint.apply {
                        color =
                            if (i == active) this@Indicator.activeColor else this@Indicator.color
                        strokeWidth =
                            if (i == active) this@Indicator.indicatorActiveStroke else this@Indicator.indicatorStroke
                    })
            }
        }

    }
}