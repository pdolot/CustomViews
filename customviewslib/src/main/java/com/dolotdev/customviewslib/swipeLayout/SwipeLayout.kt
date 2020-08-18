package com.dolotdev.customviewslib.swipeLayout

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.*
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.dolotdev.customviewslib.R
import com.dolotdev.customviewslib.extension.*
import com.dolotdev.customviewslib.roundedView.OuterRoundedView
import com.dolotdev.customviewslib.swipeLayout.SwipeLayout.ViewArrangement.*
import kotlin.math.abs

class SwipeLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var surfaceView: Pair<View, Boolean>? = null
    private var outerRoundedView: View? = null
    private var leftViews: MutableList<Pair<View, Boolean>>? =
        null  // if second is true that means that view is rounded
    private var rightViews: MutableList<Pair<View, Boolean>>? =
        null // if second is true that means that view is rounded
    private var bottomViews: MutableList<View>? = null
    private var lastRightView: Pair<View, Boolean>? = null
    private var lastLeftView: Pair<View, Boolean>? = null

    private var translatableViews: MutableList<View>? = null

    private var rightViewsWidth = 0
    private var leftViewsWidth = 0
    private var surfaceViewWidth = 0

    private var currentRightIndex = 0
    private var currentLeftIndex = 0

    private var fullTranslation = 0
    private var maxTranslationLeft = 0
    private var maxTranslationRight = 0

    private var touchX = 0f
    private var previousTouchX = 0f
    private var swipeRatio = 0.4f

    private var swipeListener: OnSwipeListener? = null

    private var childrenArrangement: List<ViewArrangement>? = null

    private val animatorSet = AnimatorSet()
    private val animatorList = ArrayList<Animator>()

    private var childSwipeBehaviour = ChildSwipeBehaviour.TO_LAST
    private var swipeBehaviour: SwipeBehaviour = SwipeBehaviour.SWIPE_LOCKED_TO_SIDE_VIEWS

    private var allowToCompleteShift = true
    private var canSwipeLeft = true
    private var canSwipeRight = true
    private var swipeDirection: SwipeDirection = SwipeDirection.NONE

    private var cornerRadius = 0

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
        childSwipeBehaviour =
            ChildSwipeBehaviour.values()[a.getInteger(R.styleable.SwipeLayout_swipeBehaviour, 0)]
        allowToCompleteShift = a.getBoolean(R.styleable.SwipeLayout_allowToCompleteShift, false)
        childrenArrangement = a.getString(R.styleable.SwipeLayout_childrenArrangement)?.split(";")
            ?.map { valueOf(it) }

        cornerRadius = a.getDimensionPixelSize(R.styleable.SwipeLayout_radius, 0)
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


    private fun initSwipe() {
        when (swipeBehaviour) {
            SwipeBehaviour.ONLY_SURFACE_AND_BOTTOM -> {
                maxTranslationRight = 0
                maxTranslationLeft = 0

                if (canSwipeLeft && canSwipeRight) {
                    maxTranslationRight = surfaceViewWidth
                    maxTranslationLeft = -surfaceViewWidth
                } else if (canSwipeRight && !canSwipeLeft) {
                    maxTranslationRight = surfaceViewWidth
                } else if (!canSwipeRight && canSwipeLeft) {
                    maxTranslationLeft = -surfaceViewWidth
                }
            }
            SwipeBehaviour.SWIPE_LOCKED_TO_SIDE_VIEWS -> {
                maxTranslationRight = 0
                maxTranslationLeft = 0

                if (childSwipeBehaviour == ChildSwipeBehaviour.SEQUENTIALLY) {
                    rightViews?.apply {
                        if (this.isNotEmpty() && canSwipeLeft) {
                            for (i in 0..currentRightIndex)
                                maxTranslationLeft =
                                    -(rightViews?.get(currentRightIndex)?.first?.measuredWidth ?: 0)
                        }
                    }
                    leftViews?.apply {
                        if (this.isNotEmpty() && canSwipeRight) {
                            maxTranslationRight =
                                leftViews?.get(currentLeftIndex)?.first?.measuredWidth ?: 0
                        }
                    }
                } else {
                    if (canSwipeLeft && canSwipeRight) {
                        maxTranslationLeft = -rightViewsWidth
                        maxTranslationRight = leftViewsWidth
                    } else if (canSwipeRight && !canSwipeLeft) {
                        maxTranslationRight = leftViewsWidth
                    } else if (!canSwipeRight && canSwipeLeft) {
                        maxTranslationLeft = -rightViewsWidth
                    }
                }
            }
            SwipeBehaviour.FULL_SWIPE_WITH_SIDE_VIEWS -> {
            }
            SwipeBehaviour.FULL_SWIPE -> {
            }
        }

        surfaceView?.first?.setOnTouchListener { v, e ->
            when (swipeBehaviour) {
                SwipeBehaviour.ONLY_SURFACE_AND_BOTTOM -> swipeOnlySurfaceAndBottomView(v, e)
                SwipeBehaviour.SWIPE_LOCKED_TO_SIDE_VIEWS -> swipeLockedToSideViews(v, e)
                SwipeBehaviour.FULL_SWIPE_WITH_SIDE_VIEWS -> swipeOnlySurfaceAndBottomView(v, e)
                SwipeBehaviour.FULL_SWIPE -> swipeOnlySurfaceAndBottomView(v, e)
            }
        }
    }

    // Swipe behaviours

    private fun swipeOnlySurfaceAndBottomView(v: View, e: MotionEvent): Boolean {
        return when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                touchX = e.x
                true
            }
            MotionEvent.ACTION_MOVE -> {
                val translation = e.x - touchX
                if (v.translationX + translation >= maxTranslationLeft && v.translationX + translation <= maxTranslationRight) {
                    translateBy(translation)
                } else if (v.translationX + translation < maxTranslationLeft) {
                    translateTo(maxTranslationLeft.toFloat())
                } else if (v.translationX + translation > maxTranslationRight) {
                    translateTo(maxTranslationRight.toFloat())
                }
                getSwipeDirection(v.translationX)
                true
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                val localSwipeDirection = if (v.translationX < 0) {
                    SwipeDirection.LEFT
                } else {
                    SwipeDirection.RIGHT
                }

                val ratio = abs(v.translationX / surfaceViewWidth)

                if (ratio >= swipeRatio) {
                    when (localSwipeDirection) {
                        SwipeDirection.LEFT -> {
                            animateTranslation(-surfaceViewWidth.toFloat())
                        }
                        SwipeDirection.RIGHT -> {
                            animateTranslation(surfaceViewWidth.toFloat())
                        }
                        SwipeDirection.NONE -> {
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

    private fun swipeLockedToSideViews(v: View, e: MotionEvent): Boolean {
        return when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                touchX = e.x
                true
            }
            MotionEvent.ACTION_MOVE -> {
                val translation = e.x - touchX
                if (v.translationX + translation >= maxTranslationLeft && v.translationX + translation <= maxTranslationRight) {
                    translateBy(translation)
                } else if (v.translationX + translation < maxTranslationLeft) {
                    translateTo(maxTranslationLeft.toFloat())
                } else if (v.translationX + translation > maxTranslationRight) {
                    translateTo(maxTranslationRight.toFloat())
                }
                true
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                var localSwipeDirection = SwipeDirection.LEFT
                val ratio = if (v.translationX < 0) {
                    abs(v.translationX / maxTranslationLeft)
                } else {
                    localSwipeDirection = SwipeDirection.RIGHT
                    abs(v.translationX / maxTranslationRight)
                }

                if (ratio >= swipeRatio) {
                    when (localSwipeDirection) {
                        SwipeDirection.LEFT -> {
                            animateTranslation(maxTranslationLeft.toFloat())
                            unlockNextRightView()
                        }
                        SwipeDirection.RIGHT -> {
                            animateTranslation(maxTranslationRight.toFloat())
                            unlockNextLeftView()
                        }
                        SwipeDirection.NONE -> {
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

    private fun getSwipeDirection(nextX: Float) {
        swipeDirection = when {
            nextX > previousTouchX -> {
                SwipeDirection.RIGHT
            }
            nextX < previousTouchX -> {
                SwipeDirection.LEFT
            }
            else -> {
                SwipeDirection.NONE
            }
        }
        previousTouchX = nextX
    }

    private fun unlockNextRightView() {
        if (childSwipeBehaviour == ChildSwipeBehaviour.SEQUENTIALLY) {
            rightViews?.let {
                if (currentRightIndex + 1 < it.size) {
                    currentRightIndex++
                    maxTranslationLeft -= rightViews?.get(currentRightIndex)?.first?.measuredWidth
                        ?: 0
                }
            }
        }
    }

    private fun unlockNextLeftView() {
        if (childSwipeBehaviour == ChildSwipeBehaviour.SEQUENTIALLY) {
            leftViews?.let {
                if (currentLeftIndex + 1 < it.size) {
                    currentLeftIndex++
                    maxTranslationRight += rightViews?.get(currentLeftIndex)?.first?.measuredWidth
                        ?: 0
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

        if (childSwipeBehaviour == ChildSwipeBehaviour.SEQUENTIALLY) {
            rightViews?.apply {
                if (this.isNotEmpty()) {
                    maxTranslationLeft =
                        -(rightViews?.get(currentRightIndex)?.first?.measuredWidth ?: 0)
                }
            }
            leftViews?.apply {
                if (this.isNotEmpty()) {
                    maxTranslationRight =
                        leftViews?.get(currentLeftIndex)?.first?.measuredWidth ?: 0
                }
            }
        }
        currentRightIndex = 0
        currentLeftIndex = 0
    }

    private fun animateTranslation(translation: Float) {
        animatorList.clear()
        translatableViews?.forEach {
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
        translatableViews?.forEach {
            it.translateBy(translation)
        }
    }

    private fun translateTo(translation: Float) {
        translatableViews?.forEach {
            it.translateTo(translation)
        }
    }

    private fun recreateFields() {
        maxTranslationLeft = 0
        maxTranslationRight = 0
        fullTranslation = measuredWidth
        rightViewsWidth = 0
        leftViewsWidth = 0
        surfaceViewWidth = 0

        leftViews = ArrayList()
        rightViews = ArrayList()
        bottomViews = ArrayList()
        translatableViews = ArrayList()
    }

    private fun measureViewsWidths() {
        rightViewsWidth = rightViews?.map { it.first.measuredWidth }?.sum() ?: 0
        leftViewsWidth = leftViews?.map { it.first.measuredWidth }?.sum() ?: 0
        surfaceViewWidth = surfaceView?.first?.measuredWidth ?: 0

        surfaceView?.let {
            if (it.second) {
                if (!rightViews.isNullOrEmpty()) {
                    rightViewsWidth -= cornerRadius
                }
                if (!leftViews.isNullOrEmpty()) {
                    leftViewsWidth -= cornerRadius
                }
            }
        }

        rightViews?.let {
            if (it.size > 1) {
                for (i in 0 until it.size - 1) {
                    if (it[i].second) {
                        rightViewsWidth -= cornerRadius
                    }
                }
            }
        }

        leftViews?.let {
            if (it.size > 1) {
                for (i in 0 until it.size - 1) {
                    if (it[i].second) {
                        leftViewsWidth -= cornerRadius
                    }
                }
            }
        }

    }


    private fun setViews() {
        recreateFields()

        for (i in 0 until childCount) {
            var arrangement = BOTTOM
            if (i < childrenArrangement?.size ?: 0) {
                arrangement = childrenArrangement?.get(i) ?: BOTTOM
            }
            val child = getChildAt(i)
            when (arrangement) {
                SURFACE -> {
                    surfaceView = Pair(child, false)
                    (translatableViews as ArrayList<View>).add(child)
                }
                SURFACE_ROUNDED -> {
                    surfaceView = Pair(child, true)
                    (translatableViews as ArrayList<View>).add(child)
                }
                LEFT -> {
                    (leftViews as ArrayList<Pair<View, Boolean>>).add(Pair(child, false))
                    (translatableViews as ArrayList<View>).add(child)
                }
                RIGHT -> {
                    (rightViews as ArrayList<Pair<View, Boolean>>).add(Pair(child, false))
                    (translatableViews as ArrayList<View>).add(child)
                }
                BOTTOM -> {
                    (bottomViews as ArrayList<View>).add(child)
                }
                RIGHT_ROUNDED -> {
                    (rightViews as ArrayList<Pair<View, Boolean>>).add(Pair(child, true))
                    (translatableViews as ArrayList<View>).add(child)
                }
                LEFT_ROUNDED -> {
                    (leftViews as ArrayList<Pair<View, Boolean>>).add(Pair(child, true))
                    (translatableViews as ArrayList<View>).add(child)
                }
            }
        }
    }

    // PIN VIEWS

    private fun pinSurfaceView() {
        surfaceView?.first?.let { sv ->
            val set = ConstraintSet()
            set.clone(this)
            sv.constraintTopToTopOfParent(set)
            sv.constraintBottomToBottomOfParent(set)
            sv.constraintStartToStartOfParent(set)
            sv.constraintEndToEndOfParent(set)
            set.applyTo(this)
        }
    }

    private fun pinRightView(view: Pair<View, Boolean>) {
        val v = view.first
        surfaceView?.first?.let { sv ->
            lastRightView?.first?.let { rv ->
                val set = ConstraintSet()
                set.clone(this)
                v.constraintTopToTopOf(set, sv)
                v.constraintBottomToBottomOf(set,sv)
                v.constraintStartToStartOf(set, rv)
                set.applyTo(this)

                val lp = v.layoutParams as MarginLayoutParams
                if (lp.width == ViewGroup.LayoutParams.MATCH_PARENT) {
                    lp.width = sv.measuredWidth
                }

                lastRightView?.second?.let { isRounded ->
                    if (isRounded) {
                        lp.marginStart = rv.measuredWidth - cornerRadius
                        //  lp.width = v.measuredWidth + (cornerRadius / 2)
                    } else {
                        lp.marginStart = rv.measuredWidth
                    }
                }

                v.layoutParams = lp
                lastRightView = view
            }
        }
    }

    private fun pinLeftView(view: Pair<View, Boolean>) {
        val v = view.first
        surfaceView?.first?.let { sv ->
            lastLeftView?.first?.let { lv ->
                val set = ConstraintSet()
                set.clone(this)
                v.constraintTopToTopOf(set, sv)
                v.constraintBottomToBottomOf(set,sv)
                v.constraintEndToEndOf(set, lv)
                set.applyTo(this)

                val lp = v.layoutParams as MarginLayoutParams
                if (lp.width == ViewGroup.LayoutParams.MATCH_PARENT) {
                    lp.width = sv.measuredWidth
                }

                lastLeftView?.second?.let { isRounded ->
                    if (isRounded) {
                        lp.marginEnd = lv.measuredWidth - cornerRadius
                        //   lp.width = v.measuredWidth + cornerRadius
                    } else {
                        lp.marginEnd = lv.measuredWidth
                    }
                }
                v.layoutParams = lp
                lastLeftView = view
            }
        }
    }

    private fun rearrangeViews() {
        removeAllViews()
        bottomViews?.forEach { addView(it) }
        leftViews?.reversed()?.forEach { addView(it.first) }
        rightViews?.reversed()?.forEach { addView(it.first) }
        if (cornerRadius > 0) createOutlineRoundedView()
        surfaceView?.let { addView(it.first) }
    }

    private fun createOutlineRoundedView(){
        val view = OuterRoundedView(context)
        view.id = View.generateViewId()

        val set = ConstraintSet()
        set.clone(this)
        view.constraintTopToTopOfParent(set)
        view.constraintStartToStartOfParent(set)
        view.constraintEndToEndOfParent(set)
        view.constraintBottomToBottomOfParent(set)

        view.setPadding(surfaceView?.first?.marginStart ?: 0, surfaceView?.first?.marginTop ?: 0, surfaceView?.first?.marginEnd ?: 0, surfaceView?.first?.marginBottom ?: 0)
        addView(view)
        set.applyTo(this)

        var color = Color.WHITE

        if (parent is View){
            ((parent as View).background as? ColorDrawable)?.let { color = it.color }
        }
        view.create(cornerRadius + 2, color)
    }

    private fun pinViews() {
        pinSurfaceView()
        lastLeftView = surfaceView
        lastRightView = surfaceView

        rightViews?.forEach { pinRightView(it) }
        leftViews?.forEach { pinLeftView(it) }

    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setViews()
        rearrangeViews()
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        pinViews()
        measureViewsWidths()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            initSwipe()
        }
    }

    interface OnSwipeListener {
        fun onSwipe(progress: Float, direction: SwipeDirection)
        fun onSwiped(direction: SwipeDirection)
    }

    enum class ViewArrangement {
        SURFACE,
        SURFACE_ROUNDED,
        BOTTOM,
        RIGHT_ROUNDED,
        LEFT_ROUNDED,
        RIGHT,
        LEFT
    }

    enum class SwipeDirection {
        NONE,
        LEFT,
        RIGHT
    }

    enum class ChildSwipeBehaviour {
        SEQUENTIALLY,
        TO_LAST
    }

    enum class SwipeBehaviour {
        ONLY_SURFACE_AND_BOTTOM,
        SWIPE_LOCKED_TO_SIDE_VIEWS,
        FULL_SWIPE_WITH_SIDE_VIEWS,
        FULL_SWIPE
    }

//    if (allowToCompleteShift) {
//        val rightViewsWidth = rightViews?.map { it.measuredWidth }?.sum() ?: 0
//        val leftViewsWidth = leftViews?.map { it.measuredWidth }?.sum() ?: 0
//        maxTranslationLeft = -measuredWidth - rightViewsWidth
//        maxTranslationRight = measuredWidth + leftViewsWidth
//    } else {
//        if (childSwipeBehaviour == ChildSwipeBehaviour.SEQUENTIALLY) {
//            rightViews?.apply {
//                if (this.isNotEmpty()) {
//                    maxTranslationLeft =
//                        -(rightViews?.get(currentRightIndex)?.measuredWidth ?: 0)
//                }
//            }
//            leftViews?.apply {
//                if (this.isNotEmpty()) {
//                    maxTranslationRight = leftViews?.get(currentLeftIndex)?.measuredWidth ?: 0
//                }
//            }
//        }
//    }

//    when (e.action) {
//        MotionEvent.ACTION_DOWN -> {
//            touchX = e.x
//            true
//        }
//        MotionEvent.ACTION_MOVE -> {
//            val translation = e.x - touchX
//            if (allowToCompleteShift) {
//                translateBy(translation)
//            } else {
//                if (v.translationX + translation >= maxTranslationLeft && v.translationX + translation <= maxTranslationRight) {
//                    translateBy(translation)
//                } else if (v.translationX + translation < maxTranslationLeft) {
//                    translateTo(maxTranslationLeft.toFloat())
//                } else if (v.translationX + translation > maxTranslationRight) {
//                    translateTo(maxTranslationRight.toFloat())
//                }
//            }
//
//            true
//        }
//        MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
//            var swipeDirection =
//                SwipeDirection.LEFT
//            var ratio = if (v.translationX < 0) {
//                abs(v.translationX / maxTranslationLeft)
//            } else {
//                swipeDirection =
//                    SwipeDirection.RIGHT
//                abs(v.translationX / maxTranslationRight)
//            }
//
//            if (ratio >= swipeRatio) {
//                when (swipeDirection) {
//                    SwipeDirection.LEFT -> {
//                        animateTranslation(maxTranslationLeft.toFloat())
//                        unlockNextRightView()
//                    }
//                    SwipeDirection.RIGHT -> {
//                        animateTranslation(maxTranslationRight.toFloat())
//                        unlockNextLeftView()
//                    }
//                }
//            } else {
//                reset(true)
//            }
//            false
//        }
//        else -> false
//    }

}