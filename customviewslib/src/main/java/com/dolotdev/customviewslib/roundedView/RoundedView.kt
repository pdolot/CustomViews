package com.dolotdev.customviewslib.roundedView

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.dolotdev.customviewslib.R

class RoundedView @JvmOverloads constructor(
	context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

	var bgColor = 0
		set(value) {
			field = value
			bgPaint.color = value
			invalidate()
		}

	var strokeColor = 0
		set(value) {
			field = value
			strokePaint.color = value
			invalidate()
		}

	private var strokeWidth = 0f
		set(value) {
			field = value
			strokePaint.strokeWidth = value
			invalidate()
		}

	private var bgPaint = Paint().apply {
		isDither = true
		isAntiAlias = true
		setLayerType(LAYER_TYPE_HARDWARE, this)
		style = Paint.Style.FILL
	}

	private var strokePaint = Paint().apply {
		isAntiAlias = true
		style = Paint.Style.STROKE
		strokeJoin = Paint.Join.ROUND
	}

	private var maxRadius = 0
		set(value) {
			field = value
			radius = field.toFloat()
		}

	private var minRadius = 0
	private var roundedCorners = 0
	private var radius = 0f
		set(value) {
			field = value
			setRadii(radius)
		}

	private var border = 0

	var scale = 1.0f
		set(value) {
			field = value
			radius = maxRadius * field
		}

	private var radii: FloatArray? = null
	private var binary: String = "0000"
	private var borderBinary: String = "0000"
	private var viewBound = RectF()

	//shadow
	private var shadowRadius = 0f
	private var shadowDx = 0f
	private var shadowDy = 0f
	private var shadowColor = 0
		set(value) {
			field = value
			bgPaint.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor)
		}

	// gradient
	private var gradientEnabled = false
	private var gradientColors = ArrayList<Int>()
	private var gradientAngle = 0

	init {
		init(context, attrs, defStyleAttr)
	}

	fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
		val a = context.theme.obtainStyledAttributes(
			attrs,
			R.styleable.RoundedView,
			defStyleAttr,
			0
		)

		gradientEnabled = a.getBoolean(R.styleable.RoundedView_gradientEnabled, false)

		if (gradientEnabled) {
			var gradientColorsRef = resources.obtainTypedArray(
				a.getResourceId(
					R.styleable.RoundedView_gradientColors,
					R.array.gradientColors
				)
			)

			gradientColors.clear()

			if (gradientColorsRef.length() == 1) {
				gradientColors.add(gradientColorsRef.getColor(0, Color.BLACK))
				gradientColors.add(gradientColorsRef.getColor(0, Color.BLACK))
			} else if (gradientColorsRef.length() == 0) {
				gradientColors.add(Color.BLACK)
				gradientColors.add(Color.BLACK)
			} else {
				for (i in 0 until gradientColorsRef.length()) {
					gradientColors.add(gradientColorsRef.getColor(i, Color.BLACK))
				}
			}

			gradientColorsRef.recycle()

			gradientAngle = a.getInteger(R.styleable.RoundedView_gradientAngle, 0)

		} else {
			bgColor = a.getColor(
				R.styleable.RoundedView_backgroundColor,
				Color.WHITE
			)
		}

		strokeColor = a.getColor(
			R.styleable.RoundedView_strokeColor,
			Color.LTGRAY
		)

		strokeWidth = a.getDimensionPixelSize(R.styleable.RoundedView_strokeWidth, 0).toFloat()

		roundedCorners = a.getInteger(R.styleable.RoundedView_roundedCorners, 0)

		if (roundedCorners > 15) {
			roundedCorners = 15
		}

		border = a.getInteger(R.styleable.RoundedView_border, 0)
		if (border > 15) {
			border = 15
		}

		binary = roundedCorners.toString(2)
		binary = binary.padStart(4, '0')

		borderBinary = border.toString(2)
		borderBinary = borderBinary.padStart(4, '0')

		maxRadius = a.getDimensionPixelSize(R.styleable.RoundedView_maxRadius, 0)
		minRadius = a.getDimensionPixelSize(R.styleable.RoundedView_minRadius, 0)
		shadowRadius = a.getDimensionPixelSize(R.styleable.RoundedView_shadowRadius, 0).toFloat()
		shadowDx = a.getDimensionPixelSize(R.styleable.RoundedView_shadowDx, 0).toFloat()
		shadowDy = a.getDimensionPixelSize(R.styleable.RoundedView_shadowDy, 0).toFloat()
		shadowColor = a.getColor(
			R.styleable.RoundedView_shadowColor,
			Color.TRANSPARENT
		)

		a.recycle()
	}

	fun getCornerRadius() = radius.toInt()

	fun getStrokeWidth() = strokeWidth.toInt()

	private fun setRadii(radii: Float) {
		this.radii = floatArrayOf(
			if (binary[3] == '0') 0f else radii,
			if (binary[3] == '0') 0f else radii,
			if (binary[2] == '0') 0f else radii,
			if (binary[2] == '0') 0f else radii,
			if (binary[1] == '0') 0f else radii,
			if (binary[1] == '0') 0f else radii,
			if (binary[0] == '0') 0f else radii,
			if (binary[0] == '0') 0f else radii
		)
		invalidate()
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec)
		setBounds()
	}

	override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
		super.onSizeChanged(w, h, oldw, oldh)
		if (gradientEnabled) initGradient()
	}

	private fun setBounds() {
		val offsetLeft = if (borderBinary[3] == '1') strokeWidth / 2 else 0f
		val offsetTop = if (borderBinary[2] == '1') strokeWidth / 2 else 0f
		val offsetRight = if (borderBinary[1] == '1') strokeWidth / 2 else 0f
		val offsetBottom = if (borderBinary[0] == '1') strokeWidth / 2 else 0f
		viewBound = RectF(
			0f + paddingStart + offsetLeft,
			0f + paddingTop + offsetTop,
			measuredWidth.toFloat() - paddingEnd - offsetRight,
			measuredHeight.toFloat() - paddingBottom - offsetBottom
		)
		invalidate()
	}

	fun setGradientArray(gradient: List<Int>) {
		gradientColors.clear()
		gradientColors.addAll(gradient)
		shadowColor = Color.BLACK
		initGradient()
		invalidate()
	}

	private fun initGradient() {
		var paintShaderPositions = FloatArray(gradientColors.size)
		var jump = 1.0f / (gradientColors.size - 1)

		for (i in gradientColors.indices) {
			paintShaderPositions[i] = 0.0f + (jump * i)
		}

		val centerY = viewBound.centerY()
		val centerX = viewBound.centerX()

		val gradient = when (gradientAngle % 360) {
			0 -> {
				LinearGradient(
					viewBound.left,
					centerY,
					viewBound.right,
					centerY,
					gradientColors.toIntArray(),
					paintShaderPositions,
					Shader.TileMode.CLAMP
				)
			}
			45 -> {
				LinearGradient(
					viewBound.left,
					viewBound.bottom,
					viewBound.right,
					viewBound.top,
					gradientColors.toIntArray(),
					paintShaderPositions,
					Shader.TileMode.CLAMP
				)
			}
			90 -> {
				LinearGradient(
					centerX,
					viewBound.bottom,
					centerX,
					viewBound.top,
					gradientColors.toIntArray(),
					paintShaderPositions,
					Shader.TileMode.CLAMP
				)
			}
			135 -> {
				LinearGradient(
					viewBound.right,
					viewBound.bottom,
					viewBound.left,
					viewBound.top,
					gradientColors.toIntArray(),
					paintShaderPositions,
					Shader.TileMode.CLAMP
				)
			}
			180 -> {
				LinearGradient(
					viewBound.right,
					centerY,
					viewBound.left,
					centerY,
					gradientColors.toIntArray(),
					paintShaderPositions,
					Shader.TileMode.CLAMP
				)
			}
			225 -> {
				LinearGradient(
					viewBound.right,
					viewBound.top,
					viewBound.left,
					viewBound.bottom,
					gradientColors.toIntArray(),
					paintShaderPositions,
					Shader.TileMode.CLAMP
				)
			}
			270 -> {
				LinearGradient(
					centerX,
					viewBound.top,
					centerX,
					viewBound.bottom,
					gradientColors.toIntArray(),
					paintShaderPositions,
					Shader.TileMode.CLAMP
				)
			}
			315 -> {
				LinearGradient(
					viewBound.left,
					viewBound.top,
					viewBound.right,
					viewBound.bottom,
					gradientColors.toIntArray(),
					paintShaderPositions,
					Shader.TileMode.CLAMP
				)
			}
			360 -> {
				LinearGradient(
					viewBound.left,
					centerY,
					viewBound.right,
					centerY,
					gradientColors.toIntArray(),
					paintShaderPositions,
					Shader.TileMode.CLAMP
				)
			}
			else -> {
				LinearGradient(
					viewBound.left,
					centerY,
					viewBound.right,
					centerY,
					gradientColors.toIntArray(),
					paintShaderPositions,
					Shader.TileMode.CLAMP
				)
			}
		}

		bgPaint.shader = gradient
	}

    override fun dispatchDraw(canvas: Canvas){


		val path = Path()
		val borderPath = Path()

		path.addRoundRect(
			viewBound, radii, Path.Direction.CW
		)

		if (border < 15) {

			if (borderBinary[3] == '1') {
				borderPath.reset()

				borderPath.arcTo(
					getRectF(
						viewBound.left,
						viewBound.bottom - (radii?.get(6) ?: 0f) * 2f,
						viewBound.left + (radii?.get(6) ?: 0f) * 2f,
						viewBound.bottom
					),
					90f,
					90f,
					true
				)
				borderPath.lineTo(viewBound.left, viewBound.top + (radii?.get(0) ?: 0f))
				borderPath.arcTo(
					getRectF(
						viewBound.left,
						viewBound.top,
						viewBound.left + (radii?.get(0) ?: 0f) * 2f,
						viewBound.top + (radii?.get(0) ?: 0f) * 2f
					),
					180f,
					90f,
					true
				)
				canvas.drawPath(borderPath, strokePaint)
			}

			if (borderBinary[2] == '1') {
				borderPath.reset()

				borderPath.arcTo(
					getRectF(
						viewBound.left,
						viewBound.top,
						viewBound.left + (radii?.get(0) ?: 0f) * 2f,
						viewBound.top + (radii?.get(0) ?: 0f) * 2f
					),
					180f,
					90f,
					true
				)
				borderPath.lineTo(viewBound.right - (radii?.get(2) ?: 0f), viewBound.top)
				borderPath.arcTo(
					getRectF(
						viewBound.right - (radii?.get(2) ?: 0f) * 2f,
						viewBound.top,
						viewBound.right,
						viewBound.top + (radii?.get(2) ?: 0f) * 2f
					),
					270f,
					90f,
					true
				)
				canvas.drawPath(borderPath, strokePaint)
			}

			if (borderBinary[1] == '1') {
				borderPath.reset()
				borderPath.arcTo(
					getRectF(
						viewBound.right - (radii?.get(2) ?: 0f) * 2f,
						viewBound.top,
						viewBound.right,
						viewBound.top + (radii?.get(2) ?: 0f) * 2f
					),
					270f,
					90f,
					true
				)
				borderPath.lineTo(
					viewBound.right,
					viewBound.bottom - (radii?.get(4) ?: 0f)
				)
				borderPath.arcTo(
					getRectF(
						viewBound.right - (radii?.get(4) ?: 0f) * 2f,
						viewBound.bottom - (radii?.get(4) ?: 0f) * 2f,
						viewBound.right,
						viewBound.bottom
					),
					0f,
					90f,
					true
				)
				canvas.drawPath(borderPath, strokePaint)
			}

			if (borderBinary[0] == '1') {
				borderPath.reset()
				borderPath.arcTo(
					getRectF(
						viewBound.right - (radii?.get(4) ?: 0f) * 2f,
						viewBound.bottom - (radii?.get(4) ?: 0f) * 2f,
						viewBound.right,
						viewBound.bottom
					),
					0f,
					90f,
					true
				)
				borderPath.lineTo(
					viewBound.left + (radii?.get(6) ?: 0f),
					viewBound.bottom
				)
				borderPath.arcTo(
					getRectF(
						viewBound.left,
						viewBound.bottom - (radii?.get(6) ?: 0f) * 2f,
						viewBound.left + (radii?.get(6) ?: 0f) * 2f,
						viewBound.bottom
					),
					90f,
					90f,
					true
				)
				canvas.drawPath(borderPath, strokePaint)
			}

		} else {
			canvas.drawPath(path, strokePaint)
		}

		canvas.drawPath(path, bgPaint)
        super.dispatchDraw(canvas)
	}


	private fun getRectF(left: Float, top: Float, right: Float, bottom: Float) =
		RectF(left, top, right, bottom)
}