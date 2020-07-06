package com.dolotdev.customviewslib.roundedView

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.dolotdev.customviewslib.R
import com.dolotdev.customviewslib.util.ViewUtil

class RoundedSView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var radius = 0f
    private var shadowRadius = 0f
    private var spaceHeight = 0f
    private var shadowDx = 0f
    private var shadowDy = 0f
    private var shadowColor = 0
    private var shapeBackgroundColor = 0
    private var shapeBackgroundPaint: Paint
    private var shapeBackgroundPaintWithoutShadow: Paint
    private lateinit var shapeBound: RectF

    private var displayOn = 3
    private var mirrorReflectionEnabled = false

    init {
        val a =
            context.theme.obtainStyledAttributes(attrs, R.styleable.RoundedSView, defStyleAttr, 0)
        radius = a.getDimension(R.styleable.RoundedSView_radius, 0f)
        spaceHeight = a.getDimension(R.styleable.RoundedSView_spaceHeight, 0f)
        shadowRadius = a.getDimension(R.styleable.RoundedSView_shadowRadius, 0f)
        shadowDx = a.getDimension(R.styleable.RoundedSView_shadowDx, 0f)
        shadowDy = a.getDimension(R.styleable.RoundedSView_shadowDy, 0f)
        shadowColor = a.getColor(
            R.styleable.RoundedSView_shadowColor,
            Color.BLACK
        )
        shapeBackgroundColor = a.getColor(
            R.styleable.RoundedSView_shapeBackgroundColor,
            Color.WHITE
        )

        shapeBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = shapeBackgroundColor
            setShadowLayer(
                shadowRadius * 0.75f,
                shadowDx,
                shadowDy,
                shadowColor
            )
        }

        shapeBackgroundPaintWithoutShadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = shapeBackgroundColor
        }

        displayOn = a.getInteger(R.styleable.RoundedSView_displayOn, 3)
        mirrorReflectionEnabled =
            a.getBoolean(R.styleable.RoundedSView_mirrorReflectionEnabled, false)
        a.recycle()
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            ViewUtil.measureDimension(measuredWidth, widthMeasureSpec),
            ViewUtil.measureDimension(
                when (displayOn) {
                    1, 2 -> radius.toInt() * 2
                    4 -> radius.toInt() * 3
                    else -> {
                        radius.toInt() * 4 + spaceHeight.toInt()
                    }
                }, heightMeasureSpec
            )
        )

        shapeBound = getRectF(
            paddingLeft.toFloat(),
            paddingTop.toFloat(),
            measuredWidth.toFloat() - paddingRight,
            measuredHeight.toFloat() - paddingBottom.toFloat()
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        when (displayOn) {
            1 -> drawTop(canvas)
            2 -> drawBottom(canvas)
            3 -> {
                drawTop(canvas)
                drawBottom(canvas)
            }
            4 -> {
                drawMerged(canvas)
            }
        }
    }

    private fun drawMerged(canvas: Canvas) {
        val path = Path()
        path.reset()
//        path.moveTo(shapeBound.left + radius, shapeBound.top + radius)
        path.moveTo(shapeBound.right, shapeBound.top)
        path.arcTo(
            getRectF(
                shapeBound.right - radius * 2,
                shapeBound.top - radius,
                shapeBound.right,
                shapeBound.top + radius
            ),
            0f, 90f, false
        )
        path.lineTo(shapeBound.left + radius, shapeBound.top + radius)
        path.arcTo(
            getRectF(
                shapeBound.left,
                shapeBound.top + radius,
                shapeBound.left + radius * 2,
                shapeBound.bottom
            ),
            270f, -90f, false
        )
        path.lineTo(shapeBound.left, shapeBound.bottom)
        path.arcTo(
            getRectF(
                shapeBound.left,
                shapeBound.bottom - radius,
                shapeBound.left + radius * 2,
                shapeBound.bottom + radius
            ),
            180f, 90f, false
        )
        path.lineTo(shapeBound.right - radius, shapeBound.bottom - radius)
        path.arcTo(
            getRectF(
                shapeBound.right - radius * 2,
                shapeBound.top,
                shapeBound.right,
                shapeBound.bottom - radius
            ),
            90f, -90f, false
        )
        path.lineTo(shapeBound.right, shapeBound.top)
        path.close()
        canvas.drawPath(path, shapeBackgroundPaint)
    }

    private fun drawTop(canvas: Canvas) {
        val path = Path()
        if (!mirrorReflectionEnabled) {
            path.reset()
            path.moveTo(shapeBound.right, shapeBound.top)
            path.lineTo(shapeBound.right, shapeBound.top + radius * 2)
            path.arcTo(
                getRectF(
                    shapeBound.right - radius * 2,
                    shapeBound.top + radius,
                    shapeBound.right,
                    shapeBound.top + radius * 3
                ),
                0f, -90f, true
            )
            path.lineTo(shapeBound.left + radius, shapeBound.top + radius)
            path.arcTo(
                getRectF(
                    shapeBound.left,
                    shapeBound.top - radius,
                    shapeBound.left + radius * 2,
                    shapeBound.top + radius
                ),
                90f, 90f, true
            )
            path.lineTo(shapeBound.right, shapeBound.top)
            path.lineTo(shapeBound.right, shapeBound.top + radius * 2)
            path.close()
        } else {
            path.reset()
            path.moveTo(shapeBound.left, shapeBound.top)
            path.lineTo(shapeBound.right, shapeBound.top)
            path.arcTo(
                getRectF(
                    shapeBound.right - radius * 2,
                    shapeBound.top - radius,
                    shapeBound.right,
                    shapeBound.top + radius
                ),
                0f, 90f, true
            )
            path.lineTo(shapeBound.left + radius, shapeBound.top + radius)
            path.arcTo(
                getRectF(
                    shapeBound.left,
                    shapeBound.top + radius,
                    shapeBound.left + radius * 2,
                    shapeBound.top + radius * 3
                ),
                270f, -90f, true
            )
            path.lineTo(shapeBound.left, shapeBound.top)
            path.lineTo(shapeBound.right, shapeBound.top)
            path.close()
        }


        canvas.drawPath(path, shapeBackgroundPaint)
    }

    private fun drawBottom(canvas: Canvas) {
        val path2 = Path()

        if (!mirrorReflectionEnabled) {
            path2.reset()
            path2.moveTo(shapeBound.right, shapeBound.bottom)
            path2.arcTo(
                getRectF(
                    shapeBound.right - 2 * radius,
                    shapeBound.bottom - radius,
                    shapeBound.right,
                    shapeBound.bottom + radius
                ),
                0f,
                -90f,
                true
            )
            path2.lineTo(shapeBound.left + radius, shapeBound.bottom - radius)
            path2.arcTo(
                getRectF(
                    shapeBound.left,
                    shapeBound.bottom - 3 * radius,
                    shapeBound.left + 2 * radius,
                    shapeBound.bottom - radius
                ),
                90f,
                90f,
                true
            )
            path2.lineTo(shapeBound.left, shapeBound.bottom)
            path2.lineTo(shapeBound.right, shapeBound.bottom)
            path2.close()
        } else {
            path2.reset()
            path2.moveTo(shapeBound.left + radius, shapeBound.bottom - radius)
            path2.lineTo(shapeBound.right - radius, shapeBound.bottom - radius)
            path2.arcTo(
                getRectF(
                    shapeBound.right - 2 * radius,
                    shapeBound.bottom - 3 * radius,
                    shapeBound.right,
                    shapeBound.bottom - radius
                ),
                90f,
                -90f,
                true
            )
            path2.lineTo(shapeBound.right, shapeBound.bottom)
            path2.lineTo(shapeBound.left, shapeBound.bottom)
            path2.arcTo(
                getRectF(
                    shapeBound.left,
                    shapeBound.bottom - radius,
                    shapeBound.left + 2 * radius,
                    shapeBound.bottom + radius
                ),
                180f,
                90f,
                true
            )
            path2.lineTo(shapeBound.right - radius, shapeBound.bottom - radius)
            path2.close()
        }

        canvas.drawPath(path2, shapeBackgroundPaint)

    }

    private fun getRectF(left: Float, top: Float, right: Float, bottom: Float) =
        RectF(left, top, right, bottom)
}
