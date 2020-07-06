package com.dolotdev.customviewslib.roundedView

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.dolotdev.customviewslib.R

class SkewRoundedView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Paint
    private var paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private var strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private var bgColor = Color.WHITE
        set(value) {
            field = value
            paint.color = field
            invalidate()
        }
    private var strokeColor = Color.LTGRAY
        set(value) {
            field = value
            strokePaint.color = field
            invalidate()
        }
    private var strokeWidth = 0f
        set(value) {
            field = value
            strokePaint.strokeWidth = field
            invalidate()
        }

    // Skew
    private var skew = 0f
    var topLeftSkew = false
        set(value) {
            field = value
            measurePoints()
            if (field) skewEnabled = true
        }
    var topRightSkew = false
        set(value) {
            field = value
            measurePoints()
            if (field) skewEnabled = true
        }
    var bottomLeftSkew = false
        set(value) {
            field = value
            measurePoints()
            if (field) skewEnabled = true
        }
    var bottomRightSkew = false
        set(value) {
            field = value
            measurePoints()
            if (field) skewEnabled = true
        }
    private var skewEnabled = false

    // RoundedCorners
    private var radius = 0f
        set(value) {
            field = value
            strokePaint.pathEffect = CornerPathEffect(field)
            paint.pathEffect = CornerPathEffect(field)
        }

    // View bound
    private lateinit var topLeft: PointF
    private lateinit var topRight: PointF
    private lateinit var bottomLeft: PointF
    private lateinit var bottomRight: PointF

    // path
    private lateinit var path: Path

    // Shadow
    private var shadowRadius = 0f
    private var shadowDx = 0f
    private var shadowDy = 0f
    private var shadowColor = Color.TRANSPARENT
        set(value) {
            field = value
            paint.setShadowLayer(shadowRadius, shadowDx, shadowDy, field)
        }

    init {
        init(context, attrs, defStyleAttr)
    }

    fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.SkewRoundedView,
            defStyleAttr,
            0
        )

        parseSkewCorners(a.getInteger(R.styleable.SkewRoundedView_skewCorners, 0))

        bgColor = a.getColor(R.styleable.SkewRoundedView_backgroundColor, Color.WHITE)
        strokeColor = a.getColor(R.styleable.SkewRoundedView_strokeColor, Color.LTGRAY)
        strokeWidth = a.getDimensionPixelSize(R.styleable.SkewRoundedView_strokeWidth, 0).toFloat()
        radius = a.getDimensionPixelSize(R.styleable.SkewRoundedView_radius, 0).toFloat()
        skew = a.getDimensionPixelSize(R.styleable.SkewRoundedView_skew, 0).toFloat()

        shadowRadius =
            a.getDimensionPixelSize(R.styleable.SkewRoundedView_shadowRadius, 0).toFloat()
        shadowDx = a.getDimensionPixelSize(R.styleable.SkewRoundedView_shadowDx, 0).toFloat()
        shadowDy = a.getDimensionPixelSize(R.styleable.SkewRoundedView_shadowDy, 0).toFloat()
        shadowColor = a.getColor(R.styleable.SkewRoundedView_shadowColor, Color.TRANSPARENT)

        a.recycle()
    }

    private fun parseSkewCorners(skewCorners: Int) {
        val binary = skewCorners.toString(2).padStart(4, '0')

        if (binary[3] == '1') topLeftSkew = true
        if (binary[2] == '1') topRightSkew = true
        if (binary[1] == '1') bottomRightSkew = true
        if (binary[0] == '1') bottomLeftSkew = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        measurePoints()
    }

    private fun measurePoints() {
        val shadowOffset = shadowRadius * 1.5f
        topLeft = PointF(
            paddingStart.toFloat() + shadowOffset - shadowDx,
            paddingTop.toFloat() + shadowOffset - shadowDy
        )
        topRight = PointF(
            measuredWidth - paddingEnd.toFloat() - shadowOffset - shadowDx,
            paddingTop.toFloat() + shadowOffset - shadowDy
        )
        bottomRight = PointF(
            measuredWidth - paddingEnd.toFloat() - shadowOffset - shadowDx,
            measuredHeight - paddingBottom.toFloat() - shadowOffset - shadowDy
        )
        bottomLeft = PointF(
            paddingStart.toFloat() + shadowOffset - shadowDx,
            measuredHeight - paddingBottom.toFloat() - shadowOffset - shadowDy
        )


        if (skewEnabled) {

            if (topLeftSkew || topRightSkew) {
                topLeft.y += skew
                topRight.y += skew
            }

            if (bottomRightSkew || bottomLeftSkew) {
                bottomRight.y -= skew
                bottomLeft.y -= skew
            }

            if (topLeftSkew) topLeft.y -= skew
            if (topRightSkew) topRight.y -= skew
            if (bottomRightSkew) bottomRight.y += skew
            if (bottomLeftSkew) bottomLeft.y += skew

        }

        path = Path()
        path.reset()

        path.moveTo(topLeft.x, topLeft.y)
        path.lineTo(topRight.x, topRight.y)
        path.lineTo(bottomRight.x, bottomRight.y)
        path.lineTo(bottomLeft.x, bottomLeft.y)
        path.lineTo(topLeft.x, topLeft.y)
        path.close()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.drawPath(path, paint)
        if (strokeWidth > 0f) canvas?.drawPath(path, strokePaint)
    }
}