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
import kotlin.properties.Delegates
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
    private var defaultBackgroundColor by Delegates.notNull<Int>()
    private var loadingBackgroundColor by Delegates.notNull<Int>()
    private var textColor by Delegates.notNull<Int>()
    private var progressCircleBackgroundColor by Delegates.notNull<Int>()
    private lateinit var defaultText: CharSequence
    private lateinit var text: CharSequence

    private var widthSize by Delegates.notNull<Int>()
    private var heightSize by Delegates.notNull<Int>()

    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var buttonText = ""
    private val buttonTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        textSize = 60f
    }

    private lateinit var buttonTextBounds: Rect

    private val progressCircleRect = RectF()
    private var progressCircleSize = 0f

    private val animatorSet: AnimatorSet = AnimatorSet().apply {
        duration = THREE_SECONDS
        doOnStart { this@LoadingButton.isEnabled = false }
        doOnEnd { this@LoadingButton.isEnabled = true }
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
                buttonText = text.toString()

                //calculate buttonText bounds and progressCircle when buttonText is first initialized
                if (!::buttonTextBounds.isInitialized) {
                    buttonTextBounds = Rect()
                    buttonTextPaint.getTextBounds(
                        buttonText,
                        0,
                        buttonText.length,
                        buttonTextBounds
                    )
                    computeProgressCircle()
                }

                animatorSet.start()
            }
            else -> {
                buttonText = defaultText.toString()

                newState.takeIf { it == ButtonState.Completed }?.run { animatorSet.cancel() }
            }
        }
    }


    private fun computeProgressCircle() {
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
        initializeStyle(context, attrs)
        buttonText = defaultText.toString()
        progressCircleBackgroundColor = ContextCompat.getColor(context, R.color.colorAccent)
    }

    private fun initializeStyle(context: Context, attrs: AttributeSet?) {
        context.withStyledAttributes(attrs, R.styleable.LoadingButton) {
            defaultBackgroundColor =
                getColor(R.styleable.LoadingButton_loadingDefaultBackgroundColor, 0)
            loadingBackgroundColor =
                getColor(R.styleable.LoadingButton_loadingBackgroundColor, 0)
            defaultText =
                getText(R.styleable.LoadingButton_loadingDefaultText)
            textColor =
                getColor(R.styleable.LoadingButton_loadingTextColor, 0)
            text =
                getText(R.styleable.LoadingButton_loadingText)
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


        canvas?.apply {
            //background
            when (buttonState) {
                ButtonState.Loading -> {
                    drawLoadingBackgroundColor(canvas)
                    drawDefaultBackgroundColor(canvas)
                }

                else -> drawColor(defaultBackgroundColor)
            }
            drawButtonText(canvas)
            drawProgressCircleIfLoading(canvas)
        }
    }


    private fun drawButtonText(canvas: Canvas) {
        buttonTextPaint.color = textColor
        canvas.drawText(
            buttonText,
            (widthSize / BY_HALF),
            (heightSize / BY_HALF) + ((buttonTextPaint.descent() - buttonTextPaint.ascent()) / 2) - buttonTextPaint.descent(),
            buttonTextPaint
        )
    }




    private fun drawLoadingBackgroundColor(canvas: Canvas) = buttonPaint.apply {
        color = loadingBackgroundColor
        canvas.drawRect(
            0f,
            0f,
            currentButtonBackgroundAnimationValue,
            heightSize.toFloat(),
            buttonPaint
        )
    }


    private fun drawDefaultBackgroundColor(canvas: Canvas) = buttonPaint.apply {
        color = defaultBackgroundColor
        canvas.drawRect(
            currentButtonBackgroundAnimationValue,
            0f,
            widthSize.toFloat(),
            heightSize.toFloat(),
            buttonPaint
        )
    }


    private fun drawProgressCircleIfLoading(canvas: Canvas) =
        buttonState.takeIf { it == ButtonState.Loading }?.let { drawProgressCircle(canvas) }

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


    private fun createButtonBackgroundAnimator() {
        ValueAnimator.ofFloat(0f, widthSize.toFloat()).apply {
            repeatMode = ValueAnimator.RESTART
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                currentButtonBackgroundAnimationValue = it.animatedValue as Float
                invalidate()
            }
            buttonBackgroundAnimator = this
            animatorSet.playProgressCircleAndButtonBackgroundTogether()
        }
    }


    private fun AnimatorSet.playProgressCircleAndButtonBackgroundTogether() =
        apply { playTogether(progressCircleAnimator, buttonBackgroundAnimator) }


    companion object {
        private const val PROGRESS_CIRCLE_SIZE_MULTIPLIER = 0.4f
        private const val PROGRESS_CIRCLE_LEFT_MARGIN_OFFSET = 16f
        private const val BY_HALF = 2f
        private const val FULL_ANGLE = 360f
        private val THREE_SECONDS = TimeUnit.SECONDS.toMillis(3)
    }


}