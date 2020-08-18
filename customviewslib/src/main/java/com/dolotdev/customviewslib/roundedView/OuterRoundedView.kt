package com.dolotdev.customviewslib.roundedView

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.dolotdev.customviewslib.R

class OuterRoundedView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var innerBound = RectF()

    private var bgColor = 0
        set(value) {
            field = value
            paint.color = field
        }

    private var paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private var radii: FloatArray? = null
    private var cornerRadius = 0
        set(value) {
            field = value
            setRadii(field.toFloat())
        }
    private var roundedCornersBinary: String = "0000"

    fun create(radius: Int, backgroundColor: Int){
        bgColor = backgroundColor
        initRoundedCorners(15)
        cornerRadius = radius
    }

    init {
        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.OuterRoundedView,
            defStyleAttr,
            0
        )
        val roundedCorners = a.getInteger(R.styleable.OuterRoundedView_roundedCorners, 0)
        initRoundedCorners(roundedCorners)

        cornerRadius = a.getDimensionPixelSize(R.styleable.OuterRoundedView_radius, 0)

        bgColor = a.getColor(R.styleable.OuterRoundedView_backgroundColor, Color.WHITE)
        a.recycle()
    }

    private fun initRoundedCorners(roundedCorners: Int){
        roundedCornersBinary = roundedCorners.toString(2)
        roundedCornersBinary = roundedCornersBinary.padStart(4, '0')
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        innerBound = getInnerBound()
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

    private fun getInnerBound(): RectF {
        return RectF(
            paddingStart.toFloat(),
            paddingTop.toFloat(),
            measuredWidth.toFloat() - paddingEnd.toFloat(),
            measuredHeight.toFloat() - paddingBottom.toFloat()
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val path = Path()

        path.addRect(
            0f,
            0f,
            measuredWidth.toFloat(),
            measuredHeight.toFloat(),
            Path.Direction.CW
        )
        path.addRoundRect(innerBound, radii, Path.Direction.CW)
        path.fillType = Path.FillType.EVEN_ODD

        canvas.drawPath(path, paint)
    }
}