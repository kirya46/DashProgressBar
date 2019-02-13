package com.example.dashedprogressbar

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

/**
 * Created by Kirill Stoianov on 2/13/19.
 *
 * Dashed progress bar.
 *
 * Looks like: - - - - - - -
 */
class DashedProgressBar(context: Context, attributeSet: AttributeSet?, def: Int) : View(context, attributeSet, def) {

    companion object {
        val TAG: String = DashedProgressBar::class.java.simpleName
    }

    /**
     * Property for set max progress dashes count
     */
    var maxDashCount: Int = 1
        set(value) {
            field = value
            invalidate()
        }

    /**
     * Property for set current progress.
     */
    var currentDashCount: Int = 1
        set(value) {
            field = value
            invalidate()
        }

    /**
     * Color for filled dashes.
     */
    var dashColor = Color.CYAN
        set(value) {
            field = value
            dashPaint.color = value
            invalidate()
        }

    /**
     * Color for background dashes.
     */
    var dashBackgroundColor = Color.GRAY
        set(value) {
            field = value
            backgroundPaint.color = value
            invalidate()
        }

    private val dashPaint by lazy {
        Paint().apply {
            color = dashColor
            isAntiAlias = true
        }
    }

    private val excludePaint by lazy {
        Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        }
    }

    private val backgroundPaint by lazy {
        Paint().apply {
            color = dashBackgroundColor
            isAntiAlias = true
        }
    }

    private var filledWidth: Float = 0f

    private var animator = ValueAnimator()

    fun increase() {
        animator.removeAllUpdateListeners()
        animator.removeAllListeners()
        animator.cancel()

        currentDashCount += 1

        val newFilledWidth = when (currentDashCount) {
            1 -> filledWidth + getDashWidth()+getDashSeparatorWidth()
            else -> {
                filledWidth +(((getDashWidth() * (currentDashCount-1)) + (getDashSeparatorWidth() * (currentDashCount-1))-filledWidth))
            }
        }

        animator.setFloatValues(filledWidth, newFilledWidth)
        animator.addUpdateListener { filledWidth = it.animatedValue as Float }
        animator.duration = 300
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
    }


    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)

        context.theme.obtainStyledAttributes(attributeSet, R.styleable.DashedProgressBar, 0, 0)
            .apply {
                try {
                    dashColor = getColor(R.styleable.DashedProgressBar_dashColor, Color.CYAN)
                    dashBackgroundColor = getColor(R.styleable.DashedProgressBar_dashBackgroundColor, Color.GRAY)
                    maxDashCount = getInteger(R.styleable.DashedProgressBar_maxDashCount, 0)
                    currentDashCount = getInteger(R.styleable.DashedProgressBar_currentDashCount, 0)
                } finally {
                    recycle()
                }
            }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.apply {
            drawBackground(this)
            drawFilled(this)
            drawPunktirs(this)
//            drawMask(this)
        }
    }

    private fun drawBackground(canvas: Canvas) {
        val backgroundRect = getRoundRect(
            0f, 0f, width.toFloat(), height.toFloat(), getCornerRadius(), getCornerRadius(),
            true, true, true, true
        )
        canvas.drawPath(backgroundRect, backgroundPaint)
    }

    private fun drawFilled(canvas: Canvas) {
        val backgroundRect = getRoundRect(
            0f, 0f, filledWidth, height.toFloat(), getCornerRadius(), getCornerRadius(),
            true, true, true, true
        )
        canvas.drawPath(backgroundRect, dashPaint)
    }

    private fun drawPunktirs(canvas: Canvas) {
        (1..maxDashCount).forEach { number ->
            val dashLeft = getDashWidth() * number
            val backgroundRect = getRoundRect(
                dashLeft,
                0f,
                dashLeft + getDashSeparatorWidth(),
                height.toFloat(),
                getCornerRadius(),
                getCornerRadius(),
                false,
                false,
                false,
                false
            )
            canvas.drawPath(backgroundRect, excludePaint)
        }
    }

    @Deprecated("")
    private fun drawDashes(canvas: Canvas) {

        (1..maxDashCount).forEach { dashNumber ->
            when (dashNumber) {
                1 -> {
                    val backgroundRect = getRoundRect(
                        0f, 0f, getDashWidth(), measuredHeight.toFloat(), getCornerRadius(), getCornerRadius(),
                        true, false, false, true
                    )
                    canvas.drawPath(backgroundRect, getPaintForDash(dashNumber))
                }
                maxDashCount -> {
                    val dashLeft =
                        (getDashWidth() * (maxDashCount - 1)) + (getDashSeparatorWidth() * (maxDashCount - 1))
                    val backgroundRect = getRoundRect(
                        dashLeft,
                        0f,
                        dashLeft + getDashWidth(),
                        measuredHeight.toFloat(),
                        getCornerRadius(),
                        getCornerRadius(),
                        false,
                        true,
                        true,
                        false
                    )
                    canvas.drawPath(backgroundRect, getPaintForDash(dashNumber))
                }
                else -> {
                    val dashLeft =
                        (getDashWidth() * (dashNumber - 1)) + (getDashSeparatorWidth() * (dashNumber - 1))
                    val backgroundRect = getRoundRect(
                        dashLeft,
                        0f,
                        dashLeft + getDashWidth(),
                        measuredHeight.toFloat(),
                        getCornerRadius(),
                        getCornerRadius(),
                        false,
                        false,
                        false,
                        false
                    )
                    canvas.drawPath(backgroundRect, getPaintForDash(dashNumber))
                }
            }
        }
    }

    private fun getCornerRadius(): Float = Math.min(width, height) / 1.5f

    private fun getDashWidth(): Float {
        return (width / maxDashCount) - getCornerRadius()
    }

    private fun getDashSeparatorWidth(): Float {
        return getCornerRadius()
    }

    private fun getPaintForDash(dashNumber: Int): Paint {
        return if (dashNumber <= currentDashCount) dashPaint
        else backgroundPaint
    }

    private fun getRoundRect(
        left: Float, top: Float, right: Float, bottom: Float, rx: Float, ry: Float,
        tl: Boolean, tr: Boolean, br: Boolean, bl: Boolean
    ): Path {
        var rx = rx
        var ry = ry
        val path = Path()
        if (rx < 0) rx = 0f
        if (ry < 0) ry = 0f
        val width = right - left
        val height = bottom - top
        if (rx > width / 2) rx = width / 2
        if (ry > height / 2) ry = height / 2
        val widthMinusCorners = width - 2 * rx
        val heightMinusCorners = height - 2 * ry

        path.moveTo(right, top + ry)
        if (tr)
            path.rQuadTo(0f, -ry, -rx, -ry)//top-right corner
        else {
            path.rLineTo(0f, -ry)
            path.rLineTo(-rx, 0f)
        }
        path.rLineTo(-widthMinusCorners, 0f)
        if (tl)
            path.rQuadTo(-rx, 0f, -rx, ry) //top-left corner
        else {
            path.rLineTo(-rx, 0f)
            path.rLineTo(0f, ry)
        }
        path.rLineTo(0f, heightMinusCorners)

        if (bl)
            path.rQuadTo(0f, ry, rx, ry)//bottom-left corner
        else {
            path.rLineTo(0f, ry)
            path.rLineTo(rx, 0f)
        }

        path.rLineTo(widthMinusCorners, 0f)
        if (br)
            path.rQuadTo(rx, 0f, rx, -ry) //bottom-right corner
        else {
            path.rLineTo(rx, 0f)
            path.rLineTo(0f, -ry)
        }

        path.rLineTo(0f, -heightMinusCorners)

        path.close()//Given close, last lineto can be removed.

        return path
    }
}