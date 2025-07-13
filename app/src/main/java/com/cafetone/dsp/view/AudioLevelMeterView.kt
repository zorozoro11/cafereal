package com.cafetone.dsp.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.cafetone.dsp.R
import kotlin.math.*

/**
 * Phase 4: Visual Enhancements - Audio Level Meter
 * 
 * Real-time audio level visualization with smooth animations
 */
class AudioLevelMeterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    // Visual properties
    private var currentLevel = 0f
    private var targetLevel = 0f
    private var animatedLevel = 0f
    
    // Paint objects
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.meter_background)
        style = Paint.Style.FILL
    }
    
    private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    private val peakPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.meter_peak)
        style = Paint.Style.FILL
    }
    
    // Animation
    private var levelAnimator: ValueAnimator? = null
    private var peakLevel = 0f
    private var peakHoldTime = 0L
    
    // Gradient colors for level meter
    private val gradientColors = intArrayOf(
        ContextCompat.getColor(context, R.color.meter_low),     // Green
        ContextCompat.getColor(context, R.color.meter_medium),  // Yellow
        ContextCompat.getColor(context, R.color.meter_high)     // Red
    )
    
    init {
        setupView()
    }
    
    private fun setupView() {
        // Setup initial state
        currentLevel = 0f
        targetLevel = 0f
        peakLevel = 0f
        
        // Content description for accessibility
        contentDescription = context.getString(R.string.audio_level_desc, 0)
    }
    
    /**
     * Update audio level with smooth animation
     */
    fun updateLevel(level: Float) {
        val clampedLevel = level.coerceIn(0f, 1f)
        targetLevel = clampedLevel
        
        // Update peak level
        if (clampedLevel > peakLevel) {
            peakLevel = clampedLevel
            peakHoldTime = System.currentTimeMillis()
        } else if (System.currentTimeMillis() - peakHoldTime > 1000) { // Peak hold for 1 second
            peakLevel = max(peakLevel - 0.01f, clampedLevel)
        }
        
        // Animate to new level
        animateToLevel(clampedLevel)
        
        // Update accessibility description
        val levelPercent = (clampedLevel * 100).toInt()
        contentDescription = context.getString(R.string.audio_level_desc, levelPercent)
    }
    
    private fun animateToLevel(newLevel: Float) {
        levelAnimator?.cancel()
        
        levelAnimator = ValueAnimator.ofFloat(animatedLevel, newLevel).apply {
            duration = 100 // Smooth 100ms animation
            addUpdateListener { animation ->
                animatedLevel = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val width = width.toFloat()
        val height = height.toFloat()
        val radius = min(width, height) / 2f - 10f
        val centerX = width / 2f
        val centerY = height / 2f
        
        // Draw background circle
        canvas.drawCircle(centerX, centerY, radius, backgroundPaint)
        
        // Create gradient for active level
        val gradient = SweepGradient(
            centerX, centerY,
            gradientColors,
            floatArrayOf(0f, 0.7f, 1f)
        )
        activePaint.shader = gradient
        
        // Draw active level arc
        val sweepAngle = animatedLevel * 270f // 270 degrees max
        val startAngle = 135f // Start from bottom-left
        
        val rect = RectF(
            centerX - radius + 20f,
            centerY - radius + 20f,
            centerX + radius - 20f,
            centerY + radius - 20f
        )
        
        canvas.drawArc(rect, startAngle, sweepAngle, false, activePaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = 15f
            strokeCap = Paint.Cap.ROUND
        })
        
        // Draw peak indicator
        if (peakLevel > 0.05f) {
            val peakAngle = startAngle + (peakLevel * 270f)
            val peakStartX = centerX + (radius - 30f) * cos(Math.toRadians(peakAngle.toDouble())).toFloat()
            val peakStartY = centerY + (radius - 30f) * sin(Math.toRadians(peakAngle.toDouble())).toFloat()
            val peakEndX = centerX + (radius - 10f) * cos(Math.toRadians(peakAngle.toDouble())).toFloat()
            val peakEndY = centerY + (radius - 10f) * sin(Math.toRadians(peakAngle.toDouble())).toFloat()
            
            canvas.drawLine(peakStartX, peakStartY, peakEndX, peakEndY, peakPaint.apply {
                strokeWidth = 4f
                strokeCap = Paint.Cap.ROUND
            })
        }
        
        // Draw center level text
        val levelText = "${(animatedLevel * 100).toInt()}%"
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.meter_text)
            textSize = radius / 3f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        
        val textY = centerY + textPaint.textSize / 3f
        canvas.drawText(levelText, centerX, textY, textPaint)
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        levelAnimator?.cancel()
    }
}