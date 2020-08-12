package com.dolotdev.customviewslib.swipeLayout

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.children
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.dolotdev.customviewslib.R
import com.dolotdev.customviewslib.extension.*
import kotlin.math.abs

class SwipeLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var surfaceView: View? = null
    private var leftViews: MutableList<View>? = null
    private var rightViews: MutableList<View>? = null
    private var bottomViews: MutableList<View>? = null
    private var lastRightView: View? = null
    private var lastLeftView: View? = null

    private var currentRightIndex = 0
    private var currentLeftIndex = 0

    private var fullTranslation = 0
    private var maxTranslationLeft = 0
    private var maxTranslationRight = 0

    private var touchX = 0f
    private var swipeRatio = 0.4f

    private var swipeListener: OnSwipeListener? = null

    private var childrenArrangement: List<String>? = null

    private val animatorSet = AnimatorSet()
    private val animatorList = ArrayList<Animator>()

    private var swipeBehaviour = SwipeBehaviour.SEQUENTIALLY

    private var allowToCompleteShift = true

    init {

        initAttrs(context, attrs, defStyleAttr)
    }

    private fun initAttrs(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.SwipeLayout,
            defStyleAttr,
            0
        )
        swipeBehaviour =
            SwipeBehaviour.values()[a.getInteger(R.styleable.SwipeLayout_swipeBehaviour, 0)]
        allowToCompleteShift = a.getBoolean(R.styleable.SwipeLayout_allowToCompleteShift, false)
        childrenArrangement = a.getString(R.styleable.SwipeLayout_childrenArrangement)?.split(";")

        a.recycle()
    }

    fun setOnSwipeListener(swipeListener: OnSwipeListener) {
        this.swipeListener = swipeListener
    }

    fun setOnSwipeListener(
        onSwipe: (progress: Float, direction: SwipeDirection) -> Unit,
        onSwiped: (direction: SwipeDirection) -> Unit
    ) {
        setOnSwipeListener(object :
            OnSwipeListener {
            override fun onSwipe(progress: Float, direction: SwipeDirection) {
                onSwipe(progress, direction)
            }

            override fun onSwiped(direction: SwipeDirection) {
                onSwiped(direction)
            }

        })
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        maxTranslationLeft = 0
        maxTranslationRight = 0
        fullTranslation = measuredWidth
        setViews()
        pinSurfaceView()
        initSwipe()

    }

    private fun initSwipe() {
        if (allowToCompleteShift){
            val rightViewsWidth = rightViews?.map { it.measuredWidth }?.sum() ?: 0
            val leftViewsWidth = leftViews?.map { it.measuredWidth }?.sum() ?: 0
            maxTranslationLeft = -measuredWidth - rightViewsWidth
            maxTranslationRight = measuredWidth + leftViewsWidth
        }else{
            if (swipeBehaviour == SwipeBehaviour.SEQUENTIALLY) {
                rightViews?.apply {
                    if (this.isNotEmpty()){
                        maxTranslationLeft = -(rightViews?.get(currentRightIndex)?.measuredWidth ?: 0)
                    }
                }
                leftViews?.apply {
                    if (this.isNotEmpty()){
                        maxTranslationRight = leftViews?.get(currentLeftIndex)?.measuredWidth ?: 0
                    }
                }
            }
        }

        surfaceView?.setOnTouchListener { v, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchX = e.x
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val translation = e.x - touchX
                    if (allowToCompleteShift){
                        translateBy(translation)
                    }else{
                        if (v.translationX + translation >= maxTranslationLeft && v.translationX + translation <= maxTranslationRight) {
                            translateBy(translation)
                        } else if (v.translationX + translation < maxTranslationLeft) {
                            translateTo(maxTranslationLeft.toFloat())
                        } else if (v.translationX + translation > maxTranslationRight) {
                            translateTo(maxTranslationRight.toFloat())
                        }
                    }

                    true
                }
                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                    var swipeDirection =
                        SwipeDirection.LEFT
                    var ratio = if (v.translationX < 0) {
                        abs(v.translationX / maxTranslationLeft)
                    } else {
                        swipeDirection =
                            SwipeDirection.RIGHT
                        abs(v.translationX / maxTranslationRight)
                    }

                    if (ratio >= swipeRatio) {
                        when (swipeDirection) {
                            SwipeDirection.LEFT -> {
                                animateTranslation(maxTranslationLeft.toFloat())
                                unlockNextRightView()
                            }
                            SwipeDirection.RIGHT -> {
                                animateTranslation(maxTranslationRight.toFloat())
                                unlockNextLeftView()
                            }
                        }
                    } else {
                        reset(true)
                    }
                    false
                }
                else -> false
            }
        }
    }

    private fun unlockNextRightView() {
        if (swipeBehaviour == SwipeBehaviour.SEQUENTIALLY) {
            rightViews?.let {
                if (currentRightIndex + 1 < it.size) {
                    currentRightIndex++
                    maxTranslationLeft -= rightViews?.get(currentRightIndex)?.measuredWidth ?: 0
                }
            }
        }
    }

    private fun unlockNextLeftView() {
        if (swipeBehaviour == SwipeBehaviour.SEQUENTIALLY) {
            leftViews?.let {
                if (currentLeftIndex + 1 < it.size) {
                    currentLeftIndex++
                    maxTranslationRight += rightViews?.get(currentLeftIndex)?.measuredWidth ?: 0
                }
            }
        }
    }

    fun reset(animation: Boolean) {
        if (animation) {
            animateTranslation(0f)
        } else {
            translateTo(0f)
        }

        if (swipeBehaviour == SwipeBehaviour.SEQUENTIALLY) {
            rightViews?.apply {
                if (this.isNotEmpty()){
                    maxTranslationLeft = -(rightViews?.get(currentRightIndex)?.measuredWidth ?: 0)
                }
            }
            leftViews?.apply {
                if (this.isNotEmpty()){
                    maxTranslationRight = leftViews?.get(currentLeftIndex)?.measuredWidth ?: 0
                }
            }
        }
        currentRightIndex = 0
        currentLeftIndex = 0
    }

    private fun animateTranslation(translation: Float) {
        animatorList.clear()
        children.forEach {
            animatorList.add(createAnimator(it, translation))
        }

        animatorSet.apply {
            playTogether(animatorList)
            duration = 500
            interpolator = FastOutSlowInInterpolator()
            start()
        }
    }

    private fun createAnimator(view: View, translation: Float): ValueAnimator {
        val animator = ValueAnimator.ofFloat(view.translationX, translation)
        animator.addUpdateListener {
            view.translationX = it.animatedValue as Float
        }
        return animator
    }

    private fun translateBy(translation: Float) {
        children.forEach {
            it.translateBy(translation)
        }
    }

    private fun translateTo(translation: Float) {
        children.forEach {
            it.translateTo(translation)
        }
        
    }

    private fun setViews() {
        leftViews = ArrayList()
        rightViews = ArrayList()
        bottomViews = ArrayList()

        surfaceView = getChildAt(childCount - 1)
        lastLeftView = surfaceView
        lastRightView = surfaceView

        for (i in 0 until childCount - 1) {
            var position = childrenArrangement?.get(i) ?: "BOTTOM"
            when (position) {
                "LEFT" -> {
                    pinLeftView(getChildAt(i))
                    lastLeftView = getChildAt(i)
                    (leftViews as ArrayList<View>).add(getChildAt(i))
                }
                "RIGHT" -> {
                    pinRightView(getChildAt(i))
                    lastRightView = getChildAt(i)
                    (rightViews as ArrayList<View>).add(getChildAt(i))
                }
                "BOTTOM" -> {
                    pinBottomView(getChildAt(i))
                    (bottomViews as ArrayList<View>).add(getChildAt(i))
                }
            }
        }
    }

    private fun pinSurfaceView() {
        surfaceView?.let { sv ->
            val set = ConstraintSet()
            set.clone(this)
            sv.constraintTopToTopOfParent(set)
            sv.constraintBottomToBottomOfParent(set)
            sv.constraintStartToStartOfParent(set)
            sv.constraintEndToEndOfParent(set)
            set.applyTo(this)
        }
    }

    private fun pinBottomView(v: View) {
        surfaceView?.let { sv ->
            val set = ConstraintSet()
            set.clone(this)
            v.constraintTopToTopOf(set, sv)
            v.constraintBottomToBottomOf(set, sv)
            v.constraintStartToStartOf(set, sv)
            v.constraintEndToEndOf(set, sv)
            set.applyTo(this)
        }
    }

    private fun pinRightView(v: View) {
        surfaceView?.let { sv ->
            lastRightView?.let { rv ->
                val set = ConstraintSet()
                set.clone(this)
                v.constraintTopToTopOfParent(set)
                v.constraintBottomToBottomOfParent(set)
                v.constraintStartToEndOf(set, rv)
                set.applyTo(this)

                val lp = v.layoutParams
                if (lp.width == ViewGroup.LayoutParams.MATCH_PARENT) {
                    lp.width = sv.measuredWidth
                    v.layoutParams = lp
                    maxTranslationLeft -= sv.measuredWidth
                } else {
                    maxTranslationLeft -= v.measuredWidth
                }
            }
        }
    }

    private fun pinLeftView(v: View) {
        surfaceView?.let { sv ->
            lastLeftView?.let { lv ->
                val set = ConstraintSet()
                set.clone(this)
                v.constraintTopToTopOfParent(set)
                v.constraintBottomToBottomOfParent(set)
                v.constraintEndToStartOf(set, lv)
                set.applyTo(this)

                val lp = lv.layoutParams
                if (lp.width == ViewGroup.LayoutParams.MATCH_PARENT) {
                    lp.width = sv.measuredWidth
                    lv.layoutParams = lp
                    maxTranslationRight += sv.measuredWidth
                } else {
                    maxTranslationRight += v.measuredWidth
                }
            }
        }
    }

    interface OnSwipeListener {
        fun onSwipe(progress: Float, direction: SwipeDirection)
        fun onSwiped(direction: SwipeDirection)
    }

    enum class SwipeDirection {
        NONE,
        LEFT,
        RIGHT
    }

    enum class SwipeBehaviour {
        SEQUENTIALLY,
        TO_LAST
    }

}