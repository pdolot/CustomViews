package com.dolotdev.customviewslib.overlapButton

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.dolotdev.customviewslib.R
import com.dolotdev.customviewslib.extension.setNewTint
import com.dolotdev.customviewslib.util.ViewUtil
import kotlin.math.*

class OverlappingButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var buttonRadius: Float = 100f
    private var spaceWidth: Float = 10f
    private var roundRadius: Float = 30f
    private lateinit var centerPoint: Point
    private lateinit var firstIntersectionPoint: Point
    private lateinit var secondIntersectionPoint: Point
    private var roundAngle = 0f
    private lateinit var firstArcAngle: Arc
    private lateinit var secondArcAngle: Arc
    private lateinit var centerArcAngle: Arc
    private lateinit var leftRoundBound: RectF
    private lateinit var rightRoundBound: RectF
    private lateinit var outlineCircleBound: RectF
    private lateinit var thumbBound: RectF

    var icon: Drawable? = null
        set(value) {
            field = value
            invalidate()
        }
    var iconTint: Int = 0
    var disabledIconTint: Int = 0

    var clickListener: (Boolean) -> Unit = {}

    var clickEnable = true
        set(value) {
            field = value
            thumbPaint.color = if (field) thumbColor else disableColor
            invalidate()
        }

    var bgColor = 0
        set(value) {
            field = value
            backgroundPaint.color = field
            invalidate()
        }

    private var thumbColor = 0
    private var disableColor = 0

    private var thumbPaint: Paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private var backgroundPaint: Paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private var clipOutPaint: Paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private var verticalBias = 0.0f

    private var clipOut = false
    private var clipOutBackgroundColor = 0
        set(value) {
            field = value
            clipOutPaint.color = field
            invalidate()
        }

    init {
        setOnTouchListener(object : OnTouchListener{
            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        if (event.x in thumbBound.left..thumbBound.right && event.y in thumbBound.top..thumbBound.bottom) {
                            return true
                        }
                        return false
                    }
                    MotionEvent.ACTION_UP -> {
                        clickListener(clickEnable)
                    }
                }
                return false
            }

        })


        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.OverlappingButton,
            defStyleAttr,
            0
        )

        buttonRadius = a.getDimensionPixelSize(R.styleable.OverlappingButton_buttonRadius, 48).toFloat()
        roundRadius = a.getDimensionPixelSize(R.styleable.OverlappingButton_roundRadius, 24).toFloat()
        spaceWidth = a.getDimensionPixelSize(R.styleable.OverlappingButton_spaceWidth, 12).toFloat()
        bgColor = a.getColor(R.styleable.OverlappingButton_backgroundColor, Color.BLACK)
        thumbColor = a.getColor(R.styleable.OverlappingButton_thumbColor, Color.GRAY)


        disableColor = a.getColor(R.styleable.OverlappingButton_disableColor, Color.DKGRAY)
        iconTint = a.getColor(R.styleable.OverlappingButton_iconTint, Color.WHITE)
        disabledIconTint = a.getColor(R.styleable.OverlappingButton_iconDisableTint, Color.GRAY)
        icon = a.getDrawable(R.styleable.OverlappingButton_icon)

        clickEnable = a.getBoolean(R.styleable.OverlappingButton_clickEnabled, true)
        verticalBias = a.getFloat(R.styleable.OverlappingButton_verticalBias, 0.0f)

        clipOut = a.getBoolean(R.styleable.OverlappingButton_clipOut, false)
        clipOutBackgroundColor = a.getColor(R.styleable.OverlappingButton_clipOutBackgroundColor, Color.DKGRAY)

        a.recycle()
    }


    private fun measureBound() {
        leftRoundBound = RectF(
            firstIntersectionPoint.x - roundRadius,
            firstIntersectionPoint.y - roundRadius,
            firstIntersectionPoint.x + roundRadius,
            firstIntersectionPoint.y + roundRadius
        )

        rightRoundBound = RectF(
            secondIntersectionPoint.x - roundRadius,
            secondIntersectionPoint.y - roundRadius,
            secondIntersectionPoint.x + roundRadius,
            secondIntersectionPoint.y + roundRadius
        )

        outlineCircleBound = RectF(
            centerPoint.x - (buttonRadius + spaceWidth),
            centerPoint.y - (buttonRadius + spaceWidth),
            centerPoint.x + (buttonRadius + spaceWidth),
            centerPoint.y + (buttonRadius + spaceWidth)
        )

        thumbBound = RectF(
            centerPoint.x - buttonRadius,
            centerPoint.y - buttonRadius,
            centerPoint.x + buttonRadius,
            centerPoint.y + buttonRadius
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            ViewUtil.measureDimension(measuredWidth, widthMeasureSpec),
            ViewUtil.measureDimension((buttonRadius * 2 + spaceWidth).toInt() + paddingTop + paddingBottom, heightMeasureSpec)
        )

        measureIntersectionPoints()
        measureBound()
    }

    private fun measureIntersectionPoints() {

        val startOffset = (measuredWidth - ((buttonRadius + spaceWidth + roundRadius).toInt() * 2)) * verticalBias

        centerPoint = Point(
            startOffset.toInt() + (buttonRadius + spaceWidth + roundRadius).toInt(),
            -(spaceWidth + buttonRadius).toInt() - paddingTop
        )

        val y = centerPoint.y + roundRadius
        val outlineCircleRadius = buttonRadius + spaceWidth + roundRadius
        val b = -2f * centerPoint.x
        val c =
            centerPoint.x.toFloat().pow(2f) + (y - centerPoint.y).pow(2f) - outlineCircleRadius.pow(
                2f
            )
        val delta = b.pow(2f) - (4 * c)

        val x1 = (-b - sqrt(delta)) / 2
        val x2 = (-b + sqrt(delta)) / 2

        firstIntersectionPoint = Point(x1.roundToInt(), -y.toInt())
        secondIntersectionPoint = Point(x2.roundToInt(), -y.toInt())
        val d = sqrt((centerPoint.x - x1).pow(2f) + (centerPoint.y - y).pow(2f))
        roundAngle = sin(roundRadius / d) * 57.29577951308f

        firstArcAngle = Arc(90f, -(90f - roundAngle))
        centerArcAngle = Arc(180f + roundAngle, 180f - (roundAngle * 2))
        secondArcAngle = Arc(180f - roundAngle, -(90f - roundAngle))

        centerPoint.y *= -1
    }

    inner class Arc(val startAngle: Float, val sweepAngle: Float)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val path = Path()
        path.reset()
        path.moveTo(0f, 0f)
        path.lineTo(0f, centerPoint.y.toFloat())
        path.lineTo(firstIntersectionPoint.x.toFloat(), centerPoint.y.toFloat())
        path.arcTo(leftRoundBound, firstArcAngle.startAngle, firstArcAngle.sweepAngle, false)
        path.arcTo(outlineCircleBound, centerArcAngle.startAngle, centerArcAngle.sweepAngle, false)
        path.arcTo(rightRoundBound, secondArcAngle.startAngle, secondArcAngle.sweepAngle, false)
        path.lineTo(measuredWidth.toFloat(), centerPoint.y.toFloat())
        path.lineTo(measuredWidth.toFloat(), 0f)
        path.lineTo(0f, 0f)
        path.close()

        canvas.drawPath(path, backgroundPaint)

        if (clipOut){
            val outPath = Path()
            outPath.reset()
            outPath.moveTo(0f, centerPoint.y.toFloat())
            outPath.lineTo(firstIntersectionPoint.x.toFloat(), centerPoint.y.toFloat())
            outPath.arcTo(leftRoundBound, firstArcAngle.startAngle, firstArcAngle.sweepAngle, false)
            outPath.arcTo(outlineCircleBound, centerArcAngle.startAngle, centerArcAngle.sweepAngle, false)
            outPath.arcTo(rightRoundBound, secondArcAngle.startAngle, secondArcAngle.sweepAngle, false)
            outPath.lineTo(measuredWidth.toFloat(), centerPoint.y.toFloat())
            outPath.lineTo(measuredWidth.toFloat(), measuredHeight.toFloat())
            outPath.lineTo(0f, measuredHeight.toFloat())
            outPath.lineTo(0f, centerPoint.y.toFloat())
            outPath.close()

            canvas.drawPath(outPath, clipOutPaint)
        }

        canvas.drawOval(thumbBound, thumbPaint)

        icon?.apply {
            setBounds(
                (thumbBound.left + buttonRadius * 0.4).toInt(),
                (thumbBound.top + buttonRadius * 0.4).toInt(),
                (thumbBound.right - buttonRadius * 0.4).toInt(),
                (thumbBound.bottom - buttonRadius * 0.4).toInt()
            )
            setNewTint(if (clickEnable) iconTint else disabledIconTint)
            draw(canvas)
        }


    }
}
