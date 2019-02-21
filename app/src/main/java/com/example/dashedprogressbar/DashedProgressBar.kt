package com.example.dashedprogressbar

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
    var currentDashCount: Int = 0
        set(value) {
            field = when {
                value < 0 -> 0
                value > maxDashCount -> maxDashCount
                else -> value
            }
            animateProgress()
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
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        }
    }

    private val separatorPaint by lazy {
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

    private var animator = ValueAnimator()

    private var filledWidth: Float = 0f

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

        //fill progress if current progress > 0
        post { animateProgress() }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.apply {
            drawBackground(this)
            drawProgress(this)
            drawSeparators(this)
        }
    }

    private fun drawBackground(canvas: Canvas) {
        val backgroundRect = getRoundRect(
            0f, 0f, width.toFloat(), height.toFloat(), getCornerRadius(), getCornerRadius(),
            true, true, true, true
        )
        canvas.drawPath(backgroundRect, backgroundPaint)
    }

    private fun drawProgress(canvas: Canvas) {
        val backgroundRect = getRoundRect(
            0f, 0f, filledWidth, height.toFloat(), getCornerRadius(), getCornerRadius(),
            true, true, true, true
        )
        canvas.drawPath(backgroundRect, dashPaint)
    }

    private fun drawSeparators(canvas: Canvas) {
        (1..(maxDashCount - 1)).forEach { number ->
            val dashLeft = (getDashWidth() + getDashSeparatorWidth()) * number
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
            canvas.drawPath(backgroundRect, separatorPaint)
        }
    }

    private fun getCornerRadius(): Float = Math.min(width, height) / 1.5f

    private fun getDashWidth(): Float {
        return (width / maxDashCount) - getCornerRadius()
    }

    private fun getDashSeparatorWidth(): Float {
        return getCornerRadius()
    }

    private fun animateProgress() {
        animator.removeAllUpdateListeners()
        animator.removeAllListeners()
        animator.cancel()

        //corner offset need because in dash without rounded corners
        //filled progress Path has rounded corners and it looks ugly
        val cornerOffset = if (currentDashCount <= 0) 0f else getDashSeparatorWidth() / 3f

        val newFilledWidth = ((getDashWidth() + getDashSeparatorWidth()) * currentDashCount) + cornerOffset

        animator.setFloatValues(filledWidth, newFilledWidth)
        animator.addUpdateListener {
            filledWidth = it.animatedValue as Float
            invalidate()
        }
        animator.duration = 500
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
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