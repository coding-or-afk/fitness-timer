package com.example.fitnesstimer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/** 남은 시간 비율을 원형 링으로 그리는 뷰. 12시 방향에서 시계 방향으로 채워진다. */
class ProgressRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val stroke = 10f * resources.displayMetrics.density
    private val bounds = RectF()

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
        color = context.getColor(R.color.ring_track)
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
        strokeCap = Paint.Cap.ROUND
        color = context.getColor(R.color.accent)
    }

    /** 0f(빈 링) ~ 1f(가득 찬 링) */
    var progress = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val radius = (minOf(w, h) - stroke) / 2
        bounds.set(w / 2 - radius, h / 2 - radius, w / 2 + radius, h / 2 + radius)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawOval(bounds, trackPaint)
        if (progress > 0f) canvas.drawArc(bounds, -90f, 360f * progress, false, progressPaint)
    }
}
