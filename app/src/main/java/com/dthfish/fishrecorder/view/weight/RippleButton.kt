package com.dthfish.fishrecorder.view.weight

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import com.dthfish.fishrecorder.R
import java.util.*

/**
 * Description
 * Author zhaolizhi
 * Date  2019/4/10.
 */
class RippleButton : FrameLayout, View.OnClickListener {

    private val rippleQueue = LinkedList<FloatWrap>()

    private val paint = Paint()
    private var outClickListener: OnClickListener? = null
    private var centerX: Int = 0
    private var centerY: Int = 0
    private var radius: Int = 0
    private val imageView: ImageView = ImageView(context)
    // 波纹起始透明度
    private val rippleStartAlpha = 0.4f
    // 这里的比例是图片里面的圆半径和图片边框的距离的比例
    private val originProportion = 0.2f
    // 这里的比例是图片里面的圆半径和圆边到控件边框的距离的比例
    private var spaceProportion = originProportion
    private var imageProportion = 1 - spaceProportion

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        imageView.setBackgroundColor(Color.TRANSPARENT)
        imageView.isClickable = true
        imageView.setImageResource(R.drawable.bg_ripple_selector)
        val params = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.gravity = Gravity.CENTER
        addView(imageView, params)
        setOnClickListener(null)
        setWillNotDraw(false)

        paint.isAntiAlias = true
        paint.color = Color.parseColor("#FFFF5A7B")
        paint.style = Paint.Style.STROKE
    }


    override fun setOnClickListener(l: OnClickListener?) {
        outClickListener = l
        imageView.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        val fraction = FloatWrap(0f)

        val valueAnimator = ValueAnimator.ofFloat(0f, 1f)
        valueAnimator.duration = 500
        valueAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationCancel(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {
                rippleQueue.poll()
            }

            override fun onAnimationRepeat(animation: Animator) {}

            override fun onAnimationStart(animation: Animator) {
                rippleQueue.offer(fraction)
            }
        })
        valueAnimator.addUpdateListener { animation ->
            fraction.fraction = animation.animatedFraction
            postInvalidateOnAnimation()
        }
        valueAnimator.start()

        if (outClickListener != null) {
            outClickListener!!.onClick(v)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        imageView.post {
            centerX = w / 2
            centerY = h / 2
            radius = Math.min(w, h) / 2
            val imgRadius = imageView.width / 2
            val circleRadius = imgRadius * (1 - originProportion)
            imageProportion = circleRadius / radius
            spaceProportion = 1 - imageProportion
        }
    }

    override fun onDraw(canvas: Canvas) {

        for (ripple in rippleQueue) {
            drawRipple(ripple, canvas, paint)
        }
    }

    private fun drawRipple(ripple: FloatWrap, canvas: Canvas, paint: Paint) {
        if (Math.abs(0 - ripple.fraction) > 0.01) {
            val strokeWidth = radius.toFloat() * spaceProportion * ripple.fraction
            val tempRadius = radius * (imageProportion + spaceProportion * ripple.fraction)
            paint.strokeWidth = strokeWidth
            paint.alpha = (255f * rippleStartAlpha * (1 - ripple.fraction)).toInt()
            canvas.drawCircle(
                centerX.toFloat(),
                centerY.toFloat(),
                tempRadius - strokeWidth / 2,
                paint
            )
        }
    }

    internal class FloatWrap(var fraction: Float)

}
