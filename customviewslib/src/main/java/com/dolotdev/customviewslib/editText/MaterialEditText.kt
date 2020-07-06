package com.dolotdev.customviewslib

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Parcel
import android.os.Parcelable
import android.text.InputFilter
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.getFontOrThrow
import androidx.core.view.marginStart
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.dolotdev.customviewslib.extension.setNewTint
import kotlinx.android.synthetic.main.custom_view_material_edit_text.view.*
import java.util.*

class MaterialEditText @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var isFocus = false
    private val _4dp = resources.getDimension(R.dimen.font_padding)
    private val _2dp = _4dp / 2f
    private val _8dp = 2 * _4dp
    private val _12sp = resources.getDimensionPixelSize(R.dimen.small_text).toFloat()
    private var labelMarginTop = (_12sp + (_4dp * 3)).toInt()
    private var labelMarginStart = 0
    private var viewMarginStart = 0

    private var drawableLeft: Drawable? = null
    private var drawableRight: Drawable? = null
    private var textSize = 0f

    // colors
    private var activeColor: Int? = null
    private var hintColor = 0
    private var textColor = 0
    private var strokeNormalColor = 0
    private var drawableColor = 0

    // editText
    var passwordVisible = false

    var onLeftDrawableClickListener: () -> Unit = {}
    var onRightDrawableClickListener: () -> Unit = {}
    var setOnFocusChangeListener: (view: View, hasFocus: Boolean) -> Unit = { _, _ -> }

    private var rightIconShouldHide = false
    private var iconShouldChangeColor = false

    private val passwordNotVisibleDrawable =
        ContextCompat.getDrawable(context, R.drawable.password_visibility_off)
    private val passwordVisibleDrawable =
        ContextCompat.getDrawable(context, R.drawable.password_visibility_on)

    var editText: EditText? = null
        get() = input

    private var maxLength = Int.MAX_VALUE

    var text: String = ""
        set(value) {
            field = value
            input.setText(field)
            checkTextSize()
        }
        get() = input.text.toString()

    init {
        View.inflate(context, R.layout.custom_view_material_edit_text, this)

        input.id += Random().nextInt(Int.MAX_VALUE - input.id - 1)
        errorText.id += Random().nextInt(Int.MAX_VALUE - errorText.id - 1)
        leftDrawable.id += Random().nextInt(Int.MAX_VALUE - leftDrawable.id - 1)
        rightDrawable.id += Random().nextInt(Int.MAX_VALUE - rightDrawable.id - 1)
        hint.id += Random().nextInt(Int.MAX_VALUE - hint.id - 1)

        viewBackground.init(context, attrs, defStyleAttr)
        strokeNormalColor = viewBackground.strokeColor

        init(context, attrs, defStyleAttr)

        input.hint = ""


        input.setOnFocusChangeListener { view, hasFocus ->
            isFocus = hasFocus

            activeColor?.let {
                viewBackground.strokeColor = if (hasFocus) it else strokeNormalColor
                hint.setTextColor(if (hasFocus) it else hintColor)
                input.setTextColor(if (hasFocus) it else textColor)
                if (iconShouldChangeColor) {
                    leftDrawable.drawable?.setNewTint(if (hasFocus) it else drawableColor)
                    rightDrawable.drawable?.setNewTint(if (hasFocus) it else drawableColor)
                }
            }
            if (rightIconShouldHide) {
                drawableRight?.let {
                    rightDrawable.visibility = if (hasFocus) View.VISIBLE else View.INVISIBLE
                }
            }
            resizeLabel(hasFocus)

            setOnFocusChangeListener(view, hasFocus)

            input.setSelection(if (hasFocus) input.text.length else 0)

            errorText.text = ""

            if (!hasFocus) {
                (context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
                    windowToken,
                    0
                )
            }
        }

        leftDrawable.setOnClickListener { onLeftDrawableClickListener() }
        rightDrawable.setOnClickListener { onRightDrawableClickListener() }

    }

    fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.MaterialEditText,
            defStyleAttr,
            0
        )

        rightIconShouldHide = a.getBoolean(R.styleable.MaterialEditText_shouldRightIconHide, false)
        iconShouldChangeColor =
            a.getBoolean(R.styleable.MaterialEditText_shouldIconChangeColor, false)

        try {
            val typeFace = a.getFontOrThrow(R.styleable.MaterialEditText_textFontFamily)
            input.typeface = typeFace
        } catch (e: IllegalArgumentException) {
        }

        try {
            val typeFace = a.getFontOrThrow(R.styleable.MaterialEditText_hintFontFamily)
            hint.typeface = typeFace
            errorText.typeface = typeFace
        } catch (e: IllegalArgumentException) {
        }

        (input.layoutParams as MarginLayoutParams).apply {
            topMargin = paddingTop + _8dp.toInt()
        }

        (errorText.layoutParams as MarginLayoutParams).apply {
            topMargin = paddingBottom + _8dp.toInt()
        }

        labelMarginTop += paddingTop

        drawableLeft = a.getDrawable(R.styleable.MaterialEditText_drawableLeft)
        drawableRight = a.getDrawable(R.styleable.MaterialEditText_drawableRight)

        drawableLeft?.let {
            leftDrawable.setImageDrawable(it)
            leftDrawable.visibility = View.VISIBLE
        }

        drawableRight?.let {
            rightDrawable.setImageDrawable(it)
            rightDrawable.visibility = View.VISIBLE
        }

        viewMarginStart =
            _8dp.toInt() + viewBackground.getStrokeWidth() + (viewBackground.getCornerRadius() / 2)


        drawableLeft?.let {
            (leftDrawable.layoutParams as MarginLayoutParams).apply {
                marginStart = paddingStart + viewMarginStart
            }

            (input.layoutParams as MarginLayoutParams).apply {
                marginStart = _8dp.toInt()
            }

        } ?: (input.layoutParams as MarginLayoutParams).apply {
            marginStart = paddingStart + viewMarginStart
        }

        drawableRight?.let {
            (rightDrawable.layoutParams as MarginLayoutParams).apply {
                marginEnd = paddingEnd + viewMarginStart
            }

            (input.layoutParams as MarginLayoutParams).apply {
                marginEnd = _8dp.toInt()
            }

        } ?: (input.layoutParams as MarginLayoutParams).apply {
            marginEnd = paddingEnd + viewMarginStart
        }

        hint.text = a.getString(R.styleable.MaterialEditText_hint)

        val text = a.getString(R.styleable.MaterialEditText_text)

        text?.let {
            input.setText(text)
            hint.setTextSize(TypedValue.COMPLEX_UNIT_PX, _12sp)
        }

        textSize = a.getDimensionPixelSize(
            R.styleable.MaterialEditText_textSize,
            _12sp.toInt() + _8dp.toInt()
        ).toFloat()

        labelMarginStart =
            hint.marginStart + textSize.toInt() + _4dp.toInt() + _2dp.toInt() + _8dp.toInt()

        if (textSize > 0) {
            if (text == null) {
                hint.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
                (hint.layoutParams as MarginLayoutParams).apply {
                    topMargin = labelMarginTop
                }
                hint.setPadding(
                    if (drawableLeft == null) viewMarginStart else labelMarginStart,
                    0,
                    0,
                    0
                )
                errorText.setPadding(
                    if (drawableLeft == null) viewMarginStart else labelMarginStart,
                    0,
                    0,
                    0
                )
            } else {
                hint.setPadding(if (drawableLeft == null) viewMarginStart else 0, 0, 0, 0)
            }
            input.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
        }

        hintColor = a.getColor(R.styleable.MaterialEditText_hintColor, Color.GRAY)
        textColor = a.getColor(R.styleable.MaterialEditText_textColor, Color.BLACK)
        hint.setTextColor(hintColor)
        input.setTextColor(textColor)

        activeColor = a.getColor(R.styleable.MaterialEditText_activeColor, strokeNormalColor)
        drawableColor = a.getColor(R.styleable.MaterialEditText_drawableColor, Color.BLACK)

        when (val inputType =
            a.getInt(R.styleable.MaterialEditText_inputType, InputType.TYPE_NULL)) {
            129, 225, 18 -> {

                input.transformationMethod = PasswordTransformationMethod()
                drawableRight = passwordNotVisibleDrawable
                rightDrawable.setImageDrawable(drawableRight)

                rightDrawable.visibility = View.VISIBLE

                (rightDrawable.layoutParams as MarginLayoutParams).apply {
                    marginEnd = paddingEnd + viewMarginStart
                }

                (input.layoutParams as MarginLayoutParams).apply {
                    marginEnd = _8dp.toInt()
                }

                onRightDrawableClickListener = {
                    if (passwordVisible) {
                        drawableRight = passwordNotVisibleDrawable
                        rightDrawable.setImageDrawable(drawableRight)
                        input.transformationMethod = PasswordTransformationMethod()
                    } else {
                        drawableRight = passwordVisibleDrawable
                        rightDrawable.setImageDrawable(drawableRight)
                        input.transformationMethod = null
                    }
                    input.setSelection(input.text.length)


                    rightDrawable.drawable?.setNewTint(
                        if (isFocus) activeColor ?: drawableColor else drawableColor
                    )

                    passwordVisible = !passwordVisible
                }
            }
            else -> input.inputType = inputType
        }

        maxLength = a.getInt(R.styleable.MaterialEditText_maxLength, Int.MAX_VALUE)

        input.filters = arrayOf(InputFilter.LengthFilter(maxLength))
        input.maxLines = a.getInt(R.styleable.MaterialEditText_maxLines, 1)

        errorText.setTextColor(a.getColor(R.styleable.MaterialEditText_errorTextColor, Color.RED))

        leftDrawable.drawable?.setNewTint(drawableColor)
        rightDrawable.drawable?.setNewTint(drawableColor)

        drawableRight?.let {
            rightDrawable.visibility = if (rightIconShouldHide) View.INVISIBLE else View.VISIBLE
        }

        errorText.setPadding(viewMarginStart, 0, 0, 0)
        setPadding(0, 0, 0, 0)
        a.recycle()
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()

        return if (superState != null) {
            val state = SavedState(superState)
            state.value = editText?.text.toString()
            state
        } else {
            superState
        }
    }


    override fun onRestoreInstanceState(state: Parcelable?) {
        val savedState = state as SavedState
        super.onRestoreInstanceState(savedState.superState)

        editText?.setText(savedState.value)
        checkTextSize()
    }

    override fun onDetachedFromWindow() {
        errorText.text = ""
        super.onDetachedFromWindow()
    }

    private fun checkTextSize() {
        if (!input.text.isNullOrBlank()) {
            hint.setPadding(if (drawableLeft == null) viewMarginStart else 0, 0, 0, 0)

            hint.setTextSize(TypedValue.COMPLEX_UNIT_PX, _12sp)
            (hint.layoutParams as MarginLayoutParams).apply {
                topMargin = 0
            }
        }
        input.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
    }

    fun setErrorMessage(message: String) {
        errorText.text = message
    }

    private fun resizeLabel(hasFocus: Boolean) {

        if (input.text.isBlank()) {
            val animator =
                if (hasFocus) ValueAnimator.ofFloat(textSize, _12sp) else ValueAnimator.ofFloat(
                    _12sp,
                    textSize
                )
            val animator2 = if (hasFocus) ValueAnimator.ofInt(
                labelMarginTop,
                0
            ) else ValueAnimator.ofInt(0, labelMarginTop)

            val marginStartAnimator = if (hasFocus) ValueAnimator.ofInt(
                if (drawableLeft == null) viewMarginStart else labelMarginStart,
                if (drawableLeft == null) viewMarginStart else 0
            ) else ValueAnimator.ofInt(
                if (drawableLeft == null) viewMarginStart else 0,
                if (drawableLeft == null) viewMarginStart else labelMarginStart
            )

            animator.addUpdateListener {
                hint.setTextSize(TypedValue.COMPLEX_UNIT_PX, it.animatedValue as Float)
            }

            animator2.addUpdateListener {
                (hint.layoutParams as MarginLayoutParams).apply {
                    topMargin = it.animatedValue as Int
                }
            }

            marginStartAnimator.addUpdateListener {
                hint.setPadding(it.animatedValue as Int, 0, 0, 0)
            }

            AnimatorSet().apply {
                playTogether(animator, animator2, marginStartAnimator)
                interpolator = FastOutSlowInInterpolator()
                duration = 100
                start()
            }
        }
    }

    fun hideIconWhenHasNotFocus() {
        rightIconShouldHide = true
    }

    companion object {
        class SavedState : BaseSavedState {
            var value: String? = null

            constructor(superState: Parcelable) : super(superState)

            private constructor(in_: Parcel) : super(in_) {
                value = in_.readString()
            }

            override fun writeToParcel(out: Parcel?, flags: Int) {
                super.writeToParcel(out, flags)
                out?.writeString(value)
            }

            companion object CREATOR : Parcelable.Creator<SavedState> {
                override fun createFromParcel(parcel: Parcel): SavedState {
                    return SavedState(parcel)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

}
