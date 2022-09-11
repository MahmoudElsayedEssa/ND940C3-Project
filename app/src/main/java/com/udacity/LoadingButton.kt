package com.udacity

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.properties.Delegates.observable

class LoadingButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(
    context,
    attrs,
    defStyleAttr
) {
    private var loadingDefaultBackgroundColor = 0
    private var loadingBackgroundColor = 0
    private var loadingDefaultText: CharSequence = ""
    private var loadingText: CharSequence = ""
    private var loadingTextColor = 0
    private var progressCircleBackgroundColor = 0

    private var widthSize = 0
    private var heightSize = 0

    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var buttonText = ""
    private val buttonTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        textSize = 55f
        typeface = Typeface.DEFAULT
    }

    private lateinit var buttonTextBounds: Rect

    private val progressCircleRect = RectF()
    private var progressCircleSize = 0f

    private val animatorSet: AnimatorSet = AnimatorSet().apply {
        duration = THREE_SECONDS
        disableViewDuringAnimation(this@LoadingButton)
    }
    private var currentProgressCircleAnimationValue = 0f
    private val progressCircleAnimator = ValueAnimator.ofFloat(0f, FULL_ANGLE).apply {
        repeatMode = ValueAnimator.RESTART
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            currentProgressCircleAnimationValue = it.animatedValue as Float
            invalidate()
        }
    }
    private var currentButtonBackgroundAnimationValue = 0f
    private lateinit var buttonBackgroundAnimator: ValueAnimator

    private var buttonState: ButtonState by observable(ButtonState.Completed) { _, _, newState ->
        when (newState) {
            ButtonState.Loading -> {
                buttonText = loadingText.toString()

                if (!::buttonTextBounds.isInitialized) {
                    retrieveButtonTextBounds()
                    computeProgressCircleRect()
                }

                animatorSet.start()
            }
            else -> {
                buttonText = loadingDefaultText.toString()

                newState.takeIf { it == ButtonState.Completed }?.run { animatorSet.cancel() }
            }
        }
    }


    private fun retrieveButtonTextBounds() {
        buttonTextBounds = Rect()
        buttonTextPaint.getTextBounds(buttonText, 0, buttonText.length, buttonTextBounds)
    }


    private fun computeProgressCircleRect() {
        val horizontalCenter =
            (buttonTextBounds.right + buttonTextBounds.width() + PROGRESS_CIRCLE_LEFT_MARGIN_OFFSET)
        val verticalCenter = (heightSize / BY_HALF)

        progressCircleRect.set(
            horizontalCenter - progressCircleSize,
            verticalCenter - progressCircleSize,
            horizontalCenter + progressCircleSize,
            verticalCenter + progressCircleSize
        )
    }

    init {
        isClickable = true
        context.withStyledAttributes(attrs, R.styleable.LoadingButton) {
            loadingDefaultBackgroundColor =
                getColor(R.styleable.LoadingButton_loadingDefaultBackgroundColor, 0)
            loadingBackgroundColor =
                getColor(R.styleable.LoadingButton_loadingBackgroundColor, 0)
            loadingDefaultText =
                getText(R.styleable.LoadingButton_loadingDefaultText)
            loadingTextColor =
                getColor(R.styleable.LoadingButton_loadingTextColor, 0)
            loadingText =
                getText(R.styleable.LoadingButton_loadingText)
        }.also {
            buttonText = loadingDefaultText.toString()
            progressCircleBackgroundColor = ContextCompat.getColor(context, R.color.colorAccent)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minWidth = paddingLeft + paddingRight + suggestedMinimumWidth
        val w = resolveSizeAndState(
            minWidth,
            widthMeasureSpec,
            1
        )
        val h = resolveSizeAndState(
            MeasureSpec.getSize(w),
            heightMeasureSpec,
            0
        )
        widthSize = w
        heightSize = h
        setMeasuredDimension(w, h)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        progressCircleSize = (min(w, h) / BY_HALF) * PROGRESS_CIRCLE_SIZE_MULTIPLIER
        createButtonBackgroundAnimator()
    }


    private fun createButtonBackgroundAnimator() {
        ValueAnimator.ofFloat(0f, widthSize.toFloat()).apply {
            repeatMode = ValueAnimator.RESTART
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                currentButtonBackgroundAnimationValue = it.animatedValue as Float
                invalidate()
            }
        }.also {
            buttonBackgroundAnimator = it
            animatorSet.playProgressCircleAndButtonBackgroundTogether()
        }
    }


    private fun AnimatorSet.playProgressCircleAndButtonBackgroundTogether() =
        apply { playTogether(progressCircleAnimator, buttonBackgroundAnimator) }

    override fun performClick(): Boolean {
        super.performClick()
        if (buttonState == ButtonState.Completed) {
            buttonState = ButtonState.Clicked
            invalidate()
        }
        return true
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.let { buttonCanvas ->
            buttonCanvas.apply {
                drawBackgroundColor()
                drawButtonText()
                drawProgressCircleIfLoading()
            }
        }
    }


    private fun Canvas.drawButtonText() {

        buttonTextPaint.color = loadingTextColor
        drawText(
            buttonText,
            (widthSize / BY_HALF),
            (heightSize / BY_HALF) + buttonTextPaint.computeTextOffset(),
            buttonTextPaint
        )
    }


    private fun TextPaint.computeTextOffset() = ((descent() - ascent()) / 2) - descent()


    private fun Canvas.drawBackgroundColor() {
        when (buttonState) {
            ButtonState.Loading -> {
                drawLoadingBackgroundColor()
                drawDefaultBackgroundColor()
            }
            else -> drawColor(loadingDefaultBackgroundColor)
        }
    }


    private fun Canvas.drawLoadingBackgroundColor() = buttonPaint.apply {
        color = loadingBackgroundColor
    }.run {
        drawRect(
            0f,
            0f,
            currentButtonBackgroundAnimationValue,
            heightSize.toFloat(),
            buttonPaint
        )
    }


    private fun Canvas.drawDefaultBackgroundColor() = buttonPaint.apply {
        color = loadingDefaultBackgroundColor
    }.run {
        drawRect(
            currentButtonBackgroundAnimationValue,
            0f,
            widthSize.toFloat(),
            heightSize.toFloat(),
            buttonPaint
        )
    }


    private fun Canvas.drawProgressCircleIfLoading() =
        buttonState.takeIf { it == ButtonState.Loading }?.let { drawProgressCircle(this) }

    private fun drawProgressCircle(buttonCanvas: Canvas) {
        buttonPaint.color = progressCircleBackgroundColor
        buttonCanvas.drawArc(
            progressCircleRect,
            0f,
            currentProgressCircleAnimationValue,
            true,
            buttonPaint
        )
    }

    fun changeButtonState(state: ButtonState) {
        if (state != buttonState) {
            buttonState = state
            invalidate()
        }
    }

    companion object {
        private const val PROGRESS_CIRCLE_SIZE_MULTIPLIER = 0.4f
        private const val PROGRESS_CIRCLE_LEFT_MARGIN_OFFSET = 16f
        private const val BY_HALF = 2f
        private const val FULL_ANGLE = 360f
        private val THREE_SECONDS = TimeUnit.SECONDS.toMillis(3)
    }


    private fun AnimatorSet.disableViewDuringAnimation(view: View) = apply {
        doOnStart { view.isEnabled = false }
        doOnEnd { view.isEnabled = true }
    }
}