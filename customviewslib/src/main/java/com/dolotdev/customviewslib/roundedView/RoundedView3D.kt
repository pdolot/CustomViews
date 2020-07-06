package com.dolotdev.customviewslib.roundedView

import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.dolotdev.customviewslib.R

class RoundedView3D @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var bgColor = 0
        set(value) {
            field = value
            bgPaint.color = value
            invalidate()
        }

    private var bgPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    //shadow
    private var shadowRadius = 0f
        set(value) {
            field = value
            shadowPaint.setShadowLayer(shadowRadius, 0f, 0f, Color.BLACK)
        }

    private var shadowPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    // shadow gradient
    private var gradientColors = ArrayList<Int>()

    private var cornerRadius = 0
        set(value) {
            field = value
            setRadii(field.toFloat())
        }


    private var roundedCorners = 0
    private var radii: FloatArray? = null
    private var roundedCornersBinary: String = "0000"

    private var viewBound = RectF()
    private var shadowBound = RectF()

    var embossing = 1
        set(value) {
            field = value
            setBounds()
            initGradient()
            invalidate()
        }


    init {
        init(context, attrs, defStyleAttr)
    }

    fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.RoundedView3D,
            defStyleAttr,
            0
        )


        var gradientColorsRef = resources.obtainTypedArray(
            a.getResourceId(
                R.styleable.RoundedView3D_gradientColors,
                R.array.rounded3dGradientColors
            )
        )
        gradientColors.clear()

        when {
            gradientColorsRef.length() == 1 -> {
                gradientColors.add(gradientColorsRef.getColor(0, Color.BLACK))
                gradientColors.add(gradientColorsRef.getColor(0, Color.BLACK))
            }
            gradientColorsRef.length() == 0 -> {
                gradientColors.add(Color.BLACK)
                gradientColors.add(Color.BLACK)
            }
            else -> {
                for (i in 0 until gradientColorsRef.length()) {
                    gradientColors.add(gradientColorsRef.getColor(i, Color.BLACK))
                }
            }
        }

        gradientColorsRef.recycle()

        bgColor = a.getColor(
            R.styleable.RoundedView3D_backgroundColor,
            Color.WHITE
        )

        roundedCorners = a.getInteger(R.styleable.RoundedView3D_roundedCorners, 0)

        embossing = a.getInteger(R.styleable.RoundedView3D_embossing, 1)

        if (roundedCorners > 15) {
            roundedCorners = 15
        }

        roundedCornersBinary = roundedCorners.toString(2)
        roundedCornersBinary = roundedCornersBinary.padStart(4, '0')


        cornerRadius = a.getDimensionPixelSize(R.styleable.RoundedView3D_radius, 0)

        shadowRadius = a.getDimensionPixelSize(R.styleable.RoundedView3D_shadowRadius, 0).toFloat()

        a.recycle()
    }


    private fun setRadii(radii: Float) {
        this.radii = floatArrayOf(
            if (roundedCornersBinary[3] == '0') 0f else radii,
            if (roundedCornersBinary[3] == '0') 0f else radii,
            if (roundedCornersBinary[2] == '0') 0f else radii,
            if (roundedCornersBinary[2] == '0') 0f else radii,
            if (roundedCornersBinary[1] == '0') 0f else radii,
            if (roundedCornersBinary[1] == '0') 0f else radii,
            if (roundedCornersBinary[0] == '0') 0f else radii,
            if (roundedCornersBinary[0] == '0') 0f else radii
        )
        invalidate()
    }

    private fun setBounds() {

        var shadowOffset = 0f
        var offset = 0f

        when (embossing) {
            1 -> {
                shadowOffset = shadowRadius * 1.25f
                offset = shadowRadius * 1.25f
            }
            0 -> {
                shadowOffset = shadowRadius * 1.25f
                offset = shadowRadius * 2f
            }
        }

        viewBound = RectF(
            0f + paddingStart + offset,
            0f + paddingTop + offset,
            measuredWidth.toFloat() - paddingEnd - offset,
            measuredHeight.toFloat() - paddingBottom - offset
        )
        shadowBound = RectF(
            0f + paddingStart + shadowOffset,
            0f + paddingTop + shadowOffset,
            measuredWidth.toFloat() - paddingEnd - shadowOffset,
            measuredHeight.toFloat() - paddingBottom - shadowOffset
        )

        invalidate()
    }


    private fun initGradient() {

        var paintShaderPositions = FloatArray(gradientColors.size)
        var jump = 1.0f / (gradientColors.size - 1)

        for (i in gradientColors.indices) {
            paintShaderPositions[i] = 0.0f + (jump * i)
        }

        val gradient = LinearGradient(
            viewBound.left,
            viewBound.top,
            viewBound.right,
            viewBound.bottom,
            when (embossing) {
                0 -> gradientColors.reversed().toIntArray()
                else -> gradientColors.toIntArray()
            },
            paintShaderPositions,
            Shader.TileMode.CLAMP
        )

        shadowPaint.shader = gradient
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setBounds()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        initGradient()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val path = Path()

        val shadowPath = Path()

        when (embossing) {
            1 -> {
                path.addRoundRect(viewBound, radii, Path.Direction.CW)
                shadowPath.addRoundRect(shadowBound, radii, Path.Direction.CW)

                canvas.drawPath(shadowPath, shadowPaint)
                canvas.drawPath(path, bgPaint)
            }
            0 -> {
                val innerPath = Path()
                innerPath.addRoundRect(shadowBound, radii, Path.Direction.CW)

                shadowPath.addRect(
                    2f,
                    2f,
                    measuredWidth.toFloat() - 2f,
                    measuredHeight.toFloat() - 2f,
                    Path.Direction.CW
                )
                shadowPath.addRoundRect(shadowBound, radii, Path.Direction.CW)
                shadowPath.fillType = Path.FillType.EVEN_ODD

                path.addRect(
                    0f,
                    0f,
                    measuredWidth.toFloat(),
                    measuredHeight.toFloat(),
                    Path.Direction.CW
                )
                path.addRoundRect(shadowBound, radii, Path.Direction.CW)
                path.fillType = Path.FillType.EVEN_ODD

                canvas.drawPath(innerPath, bgPaint)
                canvas.drawPath(shadowPath, shadowPaint)
                canvas.drawPath(path, Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.FILL
                    color = (this@RoundedView3D.background as ColorDrawable).color
                })
            }
        }


    }
}