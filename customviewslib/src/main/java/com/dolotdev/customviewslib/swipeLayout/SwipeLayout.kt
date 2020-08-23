package com.dolotdev.customviewslib.swipeLayout

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.*
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.dolotdev.customviewslib.R
import com.dolotdev.customviewslib.extension.translateBy
import com.dolotdev.customviewslib.extension.translateTo
import kotlin.math.abs


class SwipeLayout @JvmOverloads constructor(
	context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

	private var surfaceView: SwipeView? = null
	private var leftViews: MutableList<SwipeView> = ArrayList()
	private var rightViews: MutableList<SwipeView> = ArrayList()
	private var bottomViews: MutableList<SwipeView> = ArrayList()
	private var lastRightView: SwipeView? = null
	private var lastLeftView: SwipeView? = null
	private var translatableViews: MutableList<SwipeView> = ArrayList()

	// translations

	private var fullTranslationLeft = 0
	private var fullTranslationRight = 0
	private var maxTranslationLeft = 0
	private var maxTranslationRight = 0
	private var currentMaxTranslationLeft = 0
	private var currentMaxTranslationRight = 0

	private var currentRightIndex = 0
	private var currentLeftIndex = 0


	private var touchX = 0f
	private var touchTime = 0L
	private var swipeRatio = 0.4f
	private var childrenArrangement: List<ViewArrangement>? = null

	private var childSwipeBehaviour = ChildSwipeBehaviour.NONE
	private var swipeBehaviour: SwipeBehaviour = SwipeBehaviour.SWIPE_LOCKED_TO_SIDE_VIEWS

	private var lockedSideViews = false
	private var allowToCompleteShift = true
	private var canSwipeLeft = CanSwipe.NON_SET
	private var canSwipeRight = CanSwipe.NON_SET
	private var swipeDirection: SwipeDirection = SwipeDirection.NONE
	private var currentSwipeRatio = 0.0f

	private var cornerRadius = 0

	var isMoving = false
	private var slinkSwipeDetected = false
	private var isSideViewExpanded = false
		set(value) {
			field = value
			currentSwipeRatio = 0.0f
		}

	// animations
	private var isAnimating = false
	private val animatorSet = AnimatorSet()
	private val animatorList = ArrayList<Animator>()
	var slinkSwipeDuration = 750L
	var swipeDuration = 1000L

	// Listeners
	private var gestureDetector: GestureDetectorCompat? = null
	private var swipeListener: OnSwipeListener? = null
	private var clickListener: OnClickListener? = null
	private var longClickListener: OnLongClickListener? = null
	private var onSequentiallyExpandListener: OnSequentiallyExpandListener? = null

	init {
		initAttrs(context, attrs, defStyleAttr)
		initGestureListener()
		initAnimatorListener()
	}

	private fun initAttrs(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
		val a = context.theme.obtainStyledAttributes(
			attrs,
			R.styleable.SwipeLayout,
			defStyleAttr,
			0
		)
		childSwipeBehaviour =
			ChildSwipeBehaviour.values()[a.getInteger(R.styleable.SwipeLayout_swipeChildBehaviour, 2)]
		childrenArrangement = a.getString(R.styleable.SwipeLayout_childrenArrangement)?.split(";")
			?.map { ViewArrangement.valueOf(it) }

		canSwipeLeft = CanSwipe.values()[a.getInteger(R.styleable.SwipeLayout_canSwipeLeft, 2)]
		canSwipeRight = CanSwipe.values()[a.getInteger(R.styleable.SwipeLayout_canSwipeRight, 2)]

		swipeBehaviour =
			SwipeBehaviour.values()[a.getInteger(R.styleable.SwipeLayout_swipeBehaviour, 0)]

		cornerRadius = a.getDimensionPixelSize(R.styleable.SwipeLayout_radius, 0)
		a.recycle()
	}

	private fun initAnimatorListener() {
		animatorSet.addListener(object : Animator.AnimatorListener {
			override fun onAnimationRepeat(animation: Animator?) {}
			override fun onAnimationEnd(animation: Animator?) {
				when (surfaceView?.view?.translationX) {
					0f -> {
						swipeListener?.onCollapse()
						isSideViewExpanded = false
					}
					maxTranslationLeft.toFloat() -> {
						if (maxTranslationLeft == fullTranslationLeft) {
							swipeListener?.onFullSwiped(SwipeDirection.LEFT)
						} else {
							isSideViewExpanded = true
							swipeListener?.onSideViewSwiped(SwipeDirection.LEFT)
						}
					}
					maxTranslationRight.toFloat() -> {
						if (maxTranslationRight == fullTranslationRight) {
							swipeListener?.onFullSwiped(SwipeDirection.RIGHT)
						} else {
							isSideViewExpanded = true
							swipeListener?.onSideViewSwiped(SwipeDirection.RIGHT)
						}
					}
					fullTranslationLeft.toFloat() -> {
						isSideViewExpanded = true
						swipeListener?.onFullSwiped(SwipeDirection.LEFT)
					}
					fullTranslationRight.toFloat() -> {
						isSideViewExpanded = true
						swipeListener?.onFullSwiped(SwipeDirection.RIGHT)
					}
				}
				isAnimating = false
				slinkSwipeDetected = false
			}

			override fun onAnimationCancel(animation: Animator?) {}
			override fun onAnimationStart(animation: Animator?) {}

		})
	}

	private fun initGestureListener() {
		gestureDetector =
			GestureDetectorCompat(context, object : GestureDetector.SimpleOnGestureListener() {
				override fun onDown(e: MotionEvent?): Boolean {
					isMoving = false
					touchX = e?.x ?: 0f
					touchTime = System.currentTimeMillis()
					return true
				}

				override fun onSingleTapUp(e: MotionEvent?): Boolean {
					val clickTime = System.currentTimeMillis() - touchTime
					if (e?.action == MotionEvent.ACTION_UP && clickTime < 100) {
						clickListener?.setOnClickListener()
					}
					return false
				}

				override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
					if (abs(distanceY) < 5f) {
						parent.requestDisallowInterceptTouchEvent(true)
					}
					return false
				}

				override fun onFling(
					p0: MotionEvent?,
					p1: MotionEvent?,
					p2: Float,
					p3: Float
				): Boolean {
					if (allowToCompleteShift && !isAnimating) {
						if (abs(p2) > 1500f) {
							slinkSwipeDetected = true
							if (p2 > 0f) {
								swipeDirection = SwipeDirection.RIGHT
								animateTranslation(fullTranslationRight.toFloat(), slinkSwipeDuration, lockedSideViews)
							} else {
								animateTranslation(fullTranslationLeft.toFloat(), slinkSwipeDuration, lockedSideViews)
								swipeDirection = SwipeDirection.LEFT
							}
						}
					}
					return false
				}

				override fun onLongPress(e: MotionEvent?) {
					if (!isMoving) {
						longClickListener?.setOnLongClickListener()
					}
				}
			})
	}

	private fun initSwipe() {
		surfaceView?.view?.setOnTouchListener { v, e ->
			gestureDetector?.let {
				if (it.onTouchEvent(e)) {
					true
				} else {
					onSwipeTouch(v, e)
				}
			}
			true
		}
	}

	// Swipe behaviours

	private fun onSwipeTouch(v: View, e: MotionEvent): Boolean {
		return when (swipeBehaviour) {
			SwipeBehaviour.ONLY_SURFACE_AND_BOTTOM -> onlySurfaceAndBottomOrFullSwipeBehaviour(v, e)
			SwipeBehaviour.SWIPE_LOCKED_TO_SIDE_VIEWS -> swipeLockedToSideViewsSwipeBehaviour(v, e)
			SwipeBehaviour.FULL_SWIPE_WITH_LOCKED_SIDE_VIEWS -> fullSwipeWithLockedSideViewsSwipeBehaviour(v, e)
			SwipeBehaviour.FULL_SWIPE -> onlySurfaceAndBottomOrFullSwipeBehaviour(v, e)
			SwipeBehaviour.FULL_SWIPE_SEQUENTIALLY -> fullSwipeSequentiallyBehaviour(v, e)
		}

	}

	private fun onlySurfaceAndBottomOrFullSwipeBehaviour(v: View, e: MotionEvent): Boolean {
		return when (e.action) {
			MotionEvent.ACTION_MOVE -> {
				if (!isAnimating) {
					val translation = e.x - touchX
					val nextTranslation = v.translationX + translation
					if (abs(translation) > 0) {
						isMoving = true
						getSwipeDirectionAndRatio(translation)
						translateOnMove(translation, nextTranslation, fullTranslationLeft, fullTranslationRight)
					}
				}
				true
			}
			MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
				if (!slinkSwipeDetected) {
					getSwipeDirectionAndRatio()
					if (currentSwipeRatio >= swipeRatio) {
						when (swipeDirection) {
							SwipeDirection.NONE -> TODO()
							SwipeDirection.LEFT -> animateTranslation(fullTranslationLeft.toFloat(), swipeDuration)
							SwipeDirection.RIGHT -> animateTranslation(fullTranslationRight.toFloat(), swipeDuration)
						}
					} else {
						animateTranslation(0f, swipeDuration)
					}
				}
				false
			}
			else -> false
		}
	}

	private fun swipeLockedToSideViewsSwipeBehaviour(v: View, e: MotionEvent): Boolean {
		return when (e.action) {
			MotionEvent.ACTION_MOVE -> {
				if (!isAnimating) {
					val translation = e.x - touchX
					val nextTranslation = v.translationX + translation
					if (abs(translation) > 0) {
						isMoving = true
						when (childSwipeBehaviour) {
							ChildSwipeBehaviour.SEQUENTIALLY -> {
								getSwipeDirectionAndRatio(translation, currentMaxTranslationLeft, currentMaxTranslationRight)
								translateOnMove(translation, nextTranslation, currentMaxTranslationLeft, currentMaxTranslationRight)
							}
							ChildSwipeBehaviour.TO_LAST -> {
								getSwipeDirectionAndRatio(translation, maxTranslationLeft, maxTranslationRight)
								translateOnMove(translation, nextTranslation, maxTranslationLeft, maxTranslationRight)
							}
							ChildSwipeBehaviour.NONE -> TODO()
						}
					}
				}
				true
			}
			MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
				if (!slinkSwipeDetected) {
					getSwipeDirectionAndRatio()
					if (currentSwipeRatio >= swipeRatio) {
						when (swipeDirection) {
							SwipeDirection.NONE -> TODO()
							SwipeDirection.LEFT -> {
								when (childSwipeBehaviour) {
									ChildSwipeBehaviour.SEQUENTIALLY -> {
										animateTranslation(currentMaxTranslationLeft.toFloat(), swipeDuration)
										unlockNextRightView()
									}
									ChildSwipeBehaviour.TO_LAST -> {
										animateTranslation(maxTranslationLeft.toFloat(), swipeDuration)
									}
									ChildSwipeBehaviour.NONE -> TODO()
								}
							}
							SwipeDirection.RIGHT -> {
								when (childSwipeBehaviour) {
									ChildSwipeBehaviour.SEQUENTIALLY -> {
										animateTranslation(currentMaxTranslationRight.toFloat(), swipeDuration)
										unlockNextLeftView()
									}
									ChildSwipeBehaviour.TO_LAST -> {
										animateTranslation(maxTranslationRight.toFloat(), swipeDuration)
									}
									ChildSwipeBehaviour.NONE -> TODO()
								}
							}
						}
					} else {
						moveToStart()
					}
				}
				false
			}
			else -> false
		}
	}

	private fun fullSwipeWithLockedSideViewsSwipeBehaviour(v: View, e: MotionEvent): Boolean {
		return when (e.action) {
			MotionEvent.ACTION_MOVE -> {
				if (!isAnimating) {
					val translation = e.x - touchX
					val nextTranslation = v.translationX + translation
					if (abs(translation) > 0) {
						isMoving = true
						if (isSideViewExpanded) {
							getSwipeDirection(translation)
							when (swipeDirection) {
								SwipeDirection.NONE -> TODO()
								SwipeDirection.LEFT -> {
									getSwipeDirectionAndRatio(translation, fullTranslationLeft, fullTranslationRight, maxTranslationLeft)
									translateOnMove(translation, nextTranslation, fullTranslationLeft, fullTranslationRight, true)
								}
								SwipeDirection.RIGHT -> {
									getSwipeDirectionAndRatio(translation, fullTranslationLeft, fullTranslationRight, maxTranslationRight)
									translateOnMove(translation, nextTranslation, fullTranslationLeft, fullTranslationRight, true)
								}
							}
						} else {
							getSwipeDirectionAndRatio(translation)
							translateOnMove(translation, nextTranslation, maxTranslationLeft, maxTranslationRight, true)
						}

					}
				}
				true
			}
			MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
				if (!slinkSwipeDetected && !isAnimating) {
					if (currentSwipeRatio > swipeRatio) {
						when (swipeDirection) {
							SwipeDirection.NONE -> TODO()
							SwipeDirection.LEFT -> {
								if (isSideViewExpanded) {
									animateTranslation(fullTranslationLeft.toFloat(), swipeDuration, true)
								} else {
									animateTranslation(maxTranslationLeft.toFloat(), swipeDuration, true)
								}
							}
							SwipeDirection.RIGHT -> {
								if (isSideViewExpanded) {
									animateTranslation(fullTranslationRight.toFloat(), swipeDuration, true)
								} else {
									animateTranslation(maxTranslationRight.toFloat(), swipeDuration, true)
								}
							}
						}

					} else {
						if (isSideViewExpanded) {
							when (swipeDirection) {
								SwipeDirection.NONE -> {
								}
								SwipeDirection.LEFT -> {
									animateTranslation(maxTranslationLeft.toFloat(), swipeDuration, true)
								}
								SwipeDirection.RIGHT -> {
									animateTranslation(maxTranslationRight.toFloat(), swipeDuration, true)
								}
							}
						} else {
							isSideViewExpanded = false
							animateTranslation(0f, swipeDuration, true)
						}
					}
				}
				false
			}
			else -> false
		}
	}

	private fun fullSwipeSequentiallyBehaviour(v: View, e: MotionEvent): Boolean {
		return when (e.action) {
			MotionEvent.ACTION_MOVE -> {
				if (!isAnimating) {
					val translation = e.x - touchX
					val nextTranslation = v.translationX + translation
					if (abs(translation) > 0) {
						isMoving = true
						if (isSideViewExpanded) {
							if ((v.translationX == maxTranslationLeft.toFloat() && translation > 0) || (v.translationX == maxTranslationRight.toFloat() && translation < 0)) {
								isSideViewExpanded = false
							}
						}
						if (isSideViewExpanded) {
							getSwipeDirection(translation)
							when (swipeDirection) {
								SwipeDirection.NONE -> TODO()
								SwipeDirection.LEFT -> {
									getSwipeDirectionAndRatio(translation, fullTranslationLeft, fullTranslationRight, maxTranslationLeft)
									translateOnMove(translation, nextTranslation, fullTranslationLeft, fullTranslationRight)
								}
								SwipeDirection.RIGHT -> {
									getSwipeDirectionAndRatio(translation, fullTranslationLeft, fullTranslationRight, maxTranslationRight)
									translateOnMove(translation, nextTranslation, fullTranslationLeft, fullTranslationRight)
								}
							}
						} else {
							getSwipeDirectionAndRatio(translation)
							translateOnMove(translation, nextTranslation, maxTranslationLeft, maxTranslationRight)
						}
					}
				}
				true
			}
			MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
				parent.requestDisallowInterceptTouchEvent(false)
				if (!slinkSwipeDetected && !isAnimating) {
					if (currentSwipeRatio > swipeRatio) {
						when (swipeDirection) {
							SwipeDirection.NONE -> TODO()
							SwipeDirection.LEFT -> {
								if (isSideViewExpanded) {
									animateTranslation(fullTranslationLeft.toFloat(), swipeDuration)
								} else {
									isSideViewExpanded = true
									animateTranslation(maxTranslationLeft.toFloat(), swipeDuration)
								}
							}
							SwipeDirection.RIGHT -> {
								if (isSideViewExpanded) {
									animateTranslation(fullTranslationRight.toFloat(), swipeDuration)
								} else {
									isSideViewExpanded = true
									animateTranslation(maxTranslationRight.toFloat(), swipeDuration)
								}
							}
						}

					} else {
						if (isSideViewExpanded) {
							when (swipeDirection) {
								SwipeDirection.NONE -> {
								}
								SwipeDirection.LEFT -> {
									animateTranslation(maxTranslationLeft.toFloat(), swipeDuration)
								}
								SwipeDirection.RIGHT -> {
									animateTranslation(maxTranslationRight.toFloat(), swipeDuration)
								}
							}
						} else {
							isSideViewExpanded = false
							animateTranslation(0f, swipeDuration)
						}
					}
				}
				false
			}
			else -> false
		}
	}

	private fun translateOnMove(
		translation: Float, nextTranslation: Float,
		maxTranslationLeft: Int,
		maxTranslationRight: Int,
		special: Boolean = false
	) {
		if (nextTranslation >= maxTranslationLeft && nextTranslation <= maxTranslationRight) {
			if (special) {
				specialTranslateBy(translation)
			} else {
				translateBy(translation)
			}
		} else if (nextTranslation < maxTranslationLeft) {
			if (special) {
				specialTranslateTo(maxTranslationLeft.toFloat())
			} else {
				translateTo(maxTranslationLeft.toFloat())
			}

		} else if (nextTranslation > maxTranslationRight) {
			if (special) {
				specialTranslateTo(maxTranslationRight.toFloat())
			} else {
				translateTo(maxTranslationRight.toFloat())
			}
		}
	}

	private fun getSwipeDirection(translation: Float) {
		surfaceView?.view?.let { v ->
			swipeDirection = when {
				v.translationX + translation > 0 -> {
					SwipeDirection.RIGHT
				}
				v.translationX + translation < 0 -> {
					SwipeDirection.LEFT
				}
				else -> SwipeDirection.NONE
			}
		}
	}

	private fun getSwipeDirectionAndRatio(
		translation: Float = 0f,
		maxTranslationLeft: Int = this.maxTranslationLeft,
		maxTranslationRight: Int = this.maxTranslationRight,
		shift: Int = 0
	) {
		var maxLeft = maxTranslationLeft
		var maxRight = maxTranslationRight
		if (shift > 0) {
			val svWidth = surfaceView?.view?.measuredWidth ?: 0
			maxRight = svWidth - shift
		} else if (shift < 0) {
			val svWidth = surfaceView?.view?.measuredWidth ?: 0
			maxLeft = -svWidth - shift
		}
		surfaceView?.view?.let { v ->
			swipeDirection = when {
				v.translationX + translation > 0 -> {
					currentSwipeRatio = (v.translationX - shift) / maxRight.toFloat()
					SwipeDirection.RIGHT
				}
				v.translationX + translation < 0 -> {
					currentSwipeRatio = (v.translationX - shift) / maxLeft.toFloat()
					SwipeDirection.LEFT
				}
				else -> SwipeDirection.NONE
			}

			swipeListener?.onSwipe(currentSwipeRatio, swipeDirection)
		}
	}

	private fun moveToStart() {
		currentLeftIndex = 0
		currentRightIndex = 0
		onSequentiallyExpandListener?.onNext(0, SwipeDirection.NONE)
		onSequentiallyExpandListener?.onReset()
		initSwipeChildBehaviour()
		animateTranslation(0f, swipeDuration)
	}

	private fun unlockNextRightView() {
		if (currentRightIndex + 1 < rightViews.size) {
			currentRightIndex++
			currentMaxTranslationLeft -= rightViews[currentRightIndex].view.measuredWidth
			if (rightViews[currentRightIndex - 1].isRounded) {
				currentMaxTranslationLeft += cornerRadius
			}
			onSequentiallyExpandListener?.onNext(currentRightIndex, SwipeDirection.LEFT)
		}
	}

	private fun unlockNextLeftView() {
		if (currentLeftIndex + 1 < leftViews.size) {
			currentLeftIndex++
			currentMaxTranslationRight += leftViews[currentLeftIndex].view.measuredWidth

			if (leftViews[currentLeftIndex - 1].isRounded) {
				currentMaxTranslationRight -= cornerRadius
			}
			onSequentiallyExpandListener?.onNext(currentLeftIndex, SwipeDirection.RIGHT)
		}
	}

// translate methods
    /**
     * @param duration means duration of the animation swipe
     *
     */
	fun fullExpandLeft(duration: Long = 1000) {
		if (canSwipeLeft == CanSwipe.TRUE) {
			animateTranslation(fullTranslationLeft.toFloat(), duration)
		}
	}

	fun fullExpandRight(duration: Long = 1000) {
		if (canSwipeLeft == CanSwipe.TRUE) {
			animateTranslation(fullTranslationLeft.toFloat(), duration)
		}
	}

	fun expandLeft(duration: Long = 500) {
		if (canSwipeLeft == CanSwipe.TRUE) {
			animateTranslation(maxTranslationLeft.toFloat(), duration)
		}
	}

	fun expandRight(duration: Long = 500) {
		if (canSwipeLeft == CanSwipe.TRUE) {
			animateTranslation(maxTranslationRight.toFloat(), duration)
		}
	}


	fun reset(animation: Boolean) {
		if (animation) {
			animateTranslation(0f, 1000)
		} else {
			translateTo(0f)
		}

		currentRightIndex = 0
		currentLeftIndex = 0
	}

	private fun animateTranslation(
		translation: Float,
		animDuration: Long,
		special: Boolean = false
	) {
		animatorList.clear()
		isAnimating = true

		if (special) {
			when (translation.toInt()) {
				0 -> {
					translatableViews.forEach {
						animatorList.add(createAnimator(it.view, it.startX + translation))
					}
				}
				maxTranslationRight, maxTranslationLeft -> {
					translatableViews.forEach {
						animatorList.add(createAnimator(it.view, it.startX + translation))
					}
				}
				fullTranslationLeft, fullTranslationRight -> {
					surfaceView?.let {
						animatorList.add(createAnimator(it.view, it.startX + translation))
					}
					when (swipeDirection) {
						SwipeDirection.NONE -> TODO()
						SwipeDirection.LEFT -> {
							rightViews.forEach {
								animatorList.add(createAnimator(it.view, it.startX + maxTranslationLeft))
							}
							leftViews.forEach {
								animatorList.add(createAnimator(it.view, it.startX + maxTranslationLeft))
							}
						}
						SwipeDirection.RIGHT -> {
							rightViews.forEach {
								animatorList.add(createAnimator(it.view, it.startX + maxTranslationRight))
							}
							leftViews.forEach {
								animatorList.add(createAnimator(it.view, it.startX + maxTranslationRight))
							}
						}
					}

				}
			}
		} else {
			translatableViews.forEach {
				animatorList.add(createAnimator(it.view, it.startX + translation))
			}
		}


		animatorSet.apply {
			playTogether(animatorList)
			duration = animDuration
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
		translatableViews.forEach {
			it.view.translateBy(translation)
		}
	}

	private fun specialTranslateBy(translation: Float) {
		surfaceView?.view?.let { surfaceView ->
			if ((surfaceView.translationX + translation < maxTranslationLeft) || (surfaceView.translationX + translation > maxTranslationRight)) {
				if (!isSideViewExpanded) {
					when (swipeDirection) {
						SwipeDirection.NONE -> TODO()
						SwipeDirection.LEFT -> translateTo(maxTranslationLeft.toFloat())
						SwipeDirection.RIGHT -> translateTo(maxTranslationRight.toFloat())
					}
					isSideViewExpanded = true
				} else {
					surfaceView.translateBy(translation)
				}

			} else {
				if (isSideViewExpanded) {
					when (swipeDirection) {
						SwipeDirection.NONE -> TODO()
						SwipeDirection.LEFT -> translateTo(maxTranslationLeft.toFloat())
						SwipeDirection.RIGHT -> translateTo(maxTranslationRight.toFloat())
					}
					isSideViewExpanded = false
				} else {
					translateBy(translation)
				}
			}
		}
	}

	private fun translateTo(translation: Float) {
		translatableViews.forEach {
			it.view.translateTo(it.startX + translation)
		}
	}

	private fun specialTranslateTo(translation: Float) {
		surfaceView?.let { surfaceView ->
			if ((surfaceView.view.translationX + translation < maxTranslationLeft) || (surfaceView.view.translationX + translation > maxTranslationRight)) {
				if (!isSideViewExpanded) {
					when (swipeDirection) {
						SwipeDirection.NONE -> TODO()
						SwipeDirection.LEFT -> translateTo(maxTranslationLeft.toFloat())
						SwipeDirection.RIGHT -> translateTo(maxTranslationRight.toFloat())
					}
					isSideViewExpanded = true
				} else {
					surfaceView.view.translateTo(surfaceView.startX + translation)
				}
			} else {
				if (isSideViewExpanded) {
					when (swipeDirection) {
						SwipeDirection.NONE -> TODO()
						SwipeDirection.LEFT -> translateTo(maxTranslationLeft.toFloat())
						SwipeDirection.RIGHT -> translateTo(maxTranslationRight.toFloat())
					}
					isSideViewExpanded = false
				} else {
					translateTo(translation)
				}

			}
		}
	}

	// init methods

	private fun measureTranslations() {
		if (swipeBehaviour == SwipeBehaviour.ONLY_SURFACE_AND_BOTTOM) {
			rightViews.forEach { removeView(it.view) }
			leftViews.forEach { removeView(it.view) }
			rightViews.clear()
			leftViews.clear()
		}

		setCanSwipeLeft()
		setCanSwipeRight()

		surfaceView?.let { surfaceView ->
			val surfaceViewWidth =
				surfaceView.view.measuredWidth - (surfaceView.view.marginStart + surfaceView.view.marginEnd)
			if (canSwipeLeft == CanSwipe.TRUE) {
				if (rightViews.isEmpty() && swipeBehaviour == SwipeBehaviour.ONLY_SURFACE_AND_BOTTOM) {
					maxTranslationLeft = -surfaceViewWidth
					fullTranslationLeft = maxTranslationLeft
				} else if (rightViews.isNotEmpty()) {
					val rightViewsWidth = rightViews.map { it.view.measuredWidth }.sum()
					maxTranslationLeft = -(rightViewsWidth) + cornerRadius

					if (rightViews.size > 1) {
						for (i in 0..rightViews.size - 2) {
							if (rightViews[i].isRounded) {
								maxTranslationLeft += cornerRadius
							}
						}
					}
					fullTranslationLeft = maxTranslationLeft - surfaceViewWidth
				}
			}

			if (canSwipeRight == CanSwipe.TRUE) {
				if (leftViews.isEmpty() && swipeBehaviour == SwipeBehaviour.ONLY_SURFACE_AND_BOTTOM) {
					maxTranslationRight = surfaceViewWidth
					fullTranslationRight = maxTranslationRight
				} else if (leftViews.isNotEmpty()) {
					val leftViewsWidth = leftViews.map { it.view.measuredWidth }.sum()
					maxTranslationRight = leftViewsWidth - cornerRadius
					if (leftViews.size > 1) {
						for (i in 0..leftViews.size - 2) {
							if (leftViews[i].isRounded) maxTranslationRight -= cornerRadius
						}
					}

					fullTranslationRight = maxTranslationRight + surfaceViewWidth + cornerRadius
				}
			}
		}
	}

	private fun initBehaviours() {
		when (swipeBehaviour) {
			SwipeBehaviour.ONLY_SURFACE_AND_BOTTOM -> {
				allowToCompleteShift = true
				maxTranslationLeft = fullTranslationLeft
				maxTranslationRight = fullTranslationRight
			}
			SwipeBehaviour.SWIPE_LOCKED_TO_SIDE_VIEWS -> {
				allowToCompleteShift = false
				initSwipeChildBehaviour()
			}
			SwipeBehaviour.FULL_SWIPE_WITH_LOCKED_SIDE_VIEWS -> {
				allowToCompleteShift = true
				lockedSideViews = true
				initSwipeChildBehaviour()
			}
			SwipeBehaviour.FULL_SWIPE -> {
				allowToCompleteShift = true
				maxTranslationLeft = fullTranslationLeft
				maxTranslationRight = fullTranslationRight
			}
			SwipeBehaviour.FULL_SWIPE_SEQUENTIALLY -> {
				allowToCompleteShift = true
			}
		}
	}

	private fun initSwipeChildBehaviour() {
		when (childSwipeBehaviour) {
			ChildSwipeBehaviour.SEQUENTIALLY -> {
				if (leftViews.isNotEmpty()) {
					currentMaxTranslationRight = leftViews[0].view.measuredWidth - cornerRadius
				}
				if (rightViews.isNotEmpty()) {
					currentMaxTranslationLeft = -rightViews[0].view.measuredWidth + cornerRadius
				}
			}
			ChildSwipeBehaviour.TO_LAST -> {
			} // Don't need implement
			ChildSwipeBehaviour.NONE -> {
			} // Don't need implement
		}
	}

	private fun setCanSwipeLeft() {
		if (canSwipeLeft == CanSwipe.NON_SET) {
			canSwipeLeft = CanSwipe.TRUE
		}
	}

	private fun setCanSwipeRight() {
		if (canSwipeRight == CanSwipe.NON_SET) {
			canSwipeRight = CanSwipe.TRUE
		}
	}

// PIN VIEWS

	private fun setViews() {
		for (i in 0 until childCount) {
			var arrangement = ViewArrangement.BOTTOM
			if (i < childrenArrangement?.size ?: 0) {
				arrangement = childrenArrangement?.get(i) ?: ViewArrangement.BOTTOM
			}
			val child = getChildAt(i)
			when (arrangement) {
				ViewArrangement.SURFACE -> {
					ViewCompat.setTranslationZ(child, (childCount * 4f) + 4f)
					val v = SwipeView(child)
					surfaceView = v
					translatableViews.add(v)
				}
				ViewArrangement.SURFACE_ROUNDED -> {
					ViewCompat.setTranslationZ(child, (childCount * 4f) + 4f)
					val v = SwipeView(child, true)
					surfaceView = v
					translatableViews.add(v)
				}
				ViewArrangement.LEFT -> {
					ViewCompat.setTranslationZ(child, ((childCount - i) * 4f))
					val v = SwipeView(child)
					translatableViews.add(v)
					leftViews.add(v)
				}
				ViewArrangement.RIGHT -> {
					ViewCompat.setTranslationZ(child, ((childCount - i) * 4f))
					val v = SwipeView(child)
					translatableViews.add(v)
					rightViews.add(v)
				}
				ViewArrangement.BOTTOM -> {
					val v = SwipeView(child)
					bottomViews.add(v)
				}
				ViewArrangement.RIGHT_ROUNDED -> {
					ViewCompat.setTranslationZ(child, ((childCount - i) * 4f))
					val v = SwipeView(child, true)
					translatableViews.add(v)
					rightViews.add(v)
				}
				ViewArrangement.LEFT_ROUNDED -> {
					ViewCompat.setTranslationZ(child, ((childCount - i) * 4f))
					val v = SwipeView(child, true)
					translatableViews.add(v)
					leftViews.add(v)
				}
				ViewArrangement.IGNORE -> {
					ViewCompat.setTranslationZ(child, (childCount * 4f) + 2f)
				}
			}
		}
	}

// arrange views

	private fun pinRightView(view: SwipeView) {
		val v = view.view
		surfaceView?.view?.let { sv ->
			lastRightView?.view?.let { rv ->
				v.x = rv.marginStart + rv.x + rv.measuredWidth.toFloat() - rv.paddingEnd
				lastRightView?.isRounded?.let { isRounded ->
					if (isRounded) {
						v.x = v.x - cornerRadius
					}
				}
				view.startX = v.x
				lastRightView = view
			}
		}
	}

	private fun pinLeftView(view: SwipeView) {
		val v = view.view
		surfaceView?.view?.let { sv ->
			lastLeftView?.view?.let { lv ->
				v.x = (lv.x - v.measuredWidth.toFloat()) + lv.marginEnd + lv.paddingStart
				lastLeftView?.isRounded?.let { isRounded ->
					if (isRounded) {
						v.x = v.x + cornerRadius
					}
				}
				view.startX = v.x
				lastLeftView = view
			}
		}
	}

	private fun pinViews() {
		lastLeftView = surfaceView
		lastRightView = surfaceView

		rightViews.forEach { pinRightView(it) }
		leftViews.forEach { pinLeftView(it) }

	}

// override methods

	private fun onRestart() {
		translatableViews.forEach { it.view.translateTo(0f) }

		fullTranslationLeft = 0
		fullTranslationRight = 0
		maxTranslationLeft = 0
		maxTranslationRight = 0
		currentMaxTranslationLeft = 0
		currentMaxTranslationRight = 0

		currentRightIndex = 0
		currentLeftIndex = 0

		touchX = 0f
		touchTime = 0L
		swipeRatio = 0.4f

		swipeDirection = SwipeDirection.NONE
		currentSwipeRatio = 0.0f

		isMoving = false
		slinkSwipeDetected = false
		isSideViewExpanded = false

		isAnimating = false
		animatorSet.cancel()
		animatorList.clear()
		leftViews.clear()
		rightViews.clear()
		bottomViews.clear()
		translatableViews.clear()
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec)
		onRestart()
		setViews()
		pinViews()
	}

	override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
		super.onLayout(changed, left, top, right, bottom)
		measureTranslations()
		initBehaviours()
		initSwipe()
	}

// listeners

	fun setOnSwipeListener(swipeListener: OnSwipeListener) {
		this.swipeListener = swipeListener
	}

	inline fun setOnSwipeListener(
		crossinline onSwipe: (progress: Float, direction: SwipeDirection) -> Unit,
		crossinline onFullSwiped: (direction: SwipeDirection) -> Unit,
		crossinline onSideViewSwiped: (direction: SwipeDirection) -> Unit,
		crossinline onCollapse: () -> Unit
	) {
		setOnSwipeListener(object :
			OnSwipeListener {
			override fun onSwipe(progress: Float, direction: SwipeDirection) {
				onSwipe(progress, direction)
			}

			override fun onFullSwiped(direction: SwipeDirection) {
				onFullSwiped(direction)
			}

			override fun onSideViewSwiped(direction: SwipeDirection) {
				onSideViewSwiped(direction)
			}

			override fun onCollapse() {
				onCollapse()
			}

		})
	}

	fun setOnClickListener(clickListener: OnClickListener) {
		this.clickListener = clickListener
	}

	fun setOnClickListener(
		onClick: () -> Unit
	) {
		setOnClickListener(object : OnClickListener {
			override fun setOnClickListener() {
				onClick()
			}

		})
	}

	fun setOnLongClickListener(longClickListener: OnLongClickListener) {
		this.longClickListener = longClickListener
	}

	fun setOnLongClickListener(
		onLongClick: () -> Unit
	) {
		setOnLongClickListener(object : OnLongClickListener {
			override fun setOnLongClickListener() {
				onLongClick()
			}

		})
	}

	fun setOnSequentiallyExpandListener(onSequentiallyExpandListener: OnSequentiallyExpandListener) {
		this.onSequentiallyExpandListener = onSequentiallyExpandListener
	}

	inline fun setOnSequentiallyExpandListener(
		crossinline onNext: (position: Int, direction: SwipeDirection) -> Unit,
		crossinline onReset: () -> Unit
	) {
		setOnSequentiallyExpandListener(object : OnSequentiallyExpandListener {
			override fun onNext(position: Int, direction: SwipeDirection) {
				onNext(position, direction)
			}

			override fun onReset() {
				onReset()
			}

		})
	}

	interface OnSwipeListener {
		fun onSwipe(progress: Float, direction: SwipeDirection)
		fun onFullSwiped(direction: SwipeDirection)
		fun onSideViewSwiped(direction: SwipeDirection)
		fun onCollapse()
	}

	interface OnClickListener {
		fun setOnClickListener()
	}

	interface OnLongClickListener {
		fun setOnLongClickListener()
	}

	interface OnSequentiallyExpandListener {
		fun onNext(position: Int, direction: SwipeDirection)
		fun onReset()
	}

	enum class ViewArrangement {
		SURFACE,
		SURFACE_ROUNDED,
		BOTTOM,
		RIGHT_ROUNDED,
		LEFT_ROUNDED,
		RIGHT,
		LEFT,
		IGNORE
	}

	enum class SwipeDirection {
		NONE,
		LEFT,
		RIGHT
	}

	enum class CanSwipe {
		FALSE,
		TRUE,
		NON_SET
	}

	enum class ChildSwipeBehaviour {
		SEQUENTIALLY,
		TO_LAST,
		NONE
	}

	enum class SwipeBehaviour {
		ONLY_SURFACE_AND_BOTTOM,
		SWIPE_LOCKED_TO_SIDE_VIEWS,
		FULL_SWIPE_WITH_LOCKED_SIDE_VIEWS,
		FULL_SWIPE,
		FULL_SWIPE_SEQUENTIALLY
	}

	companion object {

		val TAG: String = this::class.java.simpleName

		private class SwipeView(
			var view: View,
			var isRounded: Boolean = false,
			var startX: Float = 0.0f
		)
	}

}