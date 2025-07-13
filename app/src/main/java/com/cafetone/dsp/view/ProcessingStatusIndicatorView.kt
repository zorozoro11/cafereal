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
 * Phase 4: Visual Enhancements - Processing Status Indicator
 * 
 * Animated indicator showing café mode processing status
 */
class ProcessingStatusIndicatorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    // Status states
    enum class ProcessingStatus {
        INACTIVE,
        ACTIVE,
        CONNECTING,
        ERROR
    }
    
    private var currentStatus = ProcessingStatus.INACTIVE
    private var pulseAnimator: ValueAnimator? = null
    private var pulseValue = 0f
    
    // Paint objects
    private val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    
    // Colors for different states
    private val statusColors = mapOf(
        ProcessingStatus.INACTIVE to ContextCompat.getColor(context, R.color.status_inactive),
        ProcessingStatus.ACTIVE to ContextCompat.getColor(context, R.color.status_active),
        ProcessingStatus.CONNECTING to ContextCompat.getColor(context, R.color.status_connecting),
        ProcessingStatus.ERROR to ContextCompat.getColor(context, R.color.status_error)
    )
    
    init {
        setupView()
    }
    
    private fun setupView() {
        updateContentDescription()
    }
    
    /**
     * Update processing status with animation
     */
    fun setStatus(status: ProcessingStatus) {
        if (currentStatus == status) return
        
        currentStatus = status
        updateContentDescription()
        
        when (status) {
            ProcessingStatus.ACTIVE -> startPulseAnimation()
            ProcessingStatus.CONNECTING -> startPulseAnimation()
            else -> stopPulseAnimation()
        }
        
        invalidate()
    }
    
    /**
     * Get current status
     */
    fun getStatus(): ProcessingStatus = currentStatus
    
    private fun startPulseAnimation() {
        pulseAnimator?.cancel()
        
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1500 // 1.5 second pulse
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener { animation ->
                pulseValue = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }
    
    private fun stopPulseAnimation() {
        pulseAnimator?.cancel()
        pulseValue = 0f
        invalidate()
    }
    
    private fun updateContentDescription() {
        val statusText = when (currentStatus) {
            ProcessingStatus.INACTIVE -> context.getString(R.string.processing_inactive)
            ProcessingStatus.ACTIVE -> context.getString(R.string.processing_active)
            ProcessingStatus.CONNECTING -> "connecting"
            ProcessingStatus.ERROR -> "error"
        }
        contentDescription = context.getString(R.string.processing_status_desc, statusText)
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val width = width.toFloat()
        val height = height.toFloat()
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = min(width, height) / 2f - 20f
        
        // Get color for current status
        val baseColor = statusColors[currentStatus] ?: statusColors[ProcessingStatus.INACTIVE]!!
        
        // Draw main status circle
        statusPaint.color = baseColor
        canvas.drawCircle(centerX, centerY, radius, statusPaint)
        
        // Draw pulse ring for active states
        if (currentStatus == ProcessingStatus.ACTIVE || currentStatus == ProcessingStatus.CONNECTING) {
            val pulseRadius = radius + (pulseValue * 15f)
            val pulseAlpha = (255 * (1f - pulseValue)).toInt()
            
            ringPaint.color = Color.argb(pulseAlpha, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))
            canvas.drawCircle(centerX, centerY, pulseRadius, ringPaint)
        }
        
        // Draw status icon
        drawStatusIcon(canvas, centerX, centerY, radius * 0.5f)
    }
    
    private fun drawStatusIcon(canvas: Canvas, centerX: Float, centerY: Float, iconRadius: Float) {
        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            strokeWidth = 3f
        }
        
        when (currentStatus) {
            ProcessingStatus.INACTIVE -> {
                // Draw pause icon (two vertical bars)
                val barWidth = iconRadius * 0.3f
                val barHeight = iconRadius * 1.2f
                val spacing = iconRadius * 0.4f
                
                canvas.drawRect(
                    centerX - spacing - barWidth,
                    centerY - barHeight / 2f,
                    centerX - spacing,
                    centerY + barHeight / 2f,
                    iconPaint
                )
                
                canvas.drawRect(
                    centerX + spacing,
                    centerY - barHeight / 2f,
                    centerX + spacing + barWidth,
                    centerY + barHeight / 2f,
                    iconPaint
                )
            }
            
            ProcessingStatus.ACTIVE -> {
                // Draw sound waves
                iconPaint.style = Paint.Style.STROKE
                iconPaint.strokeCap = Paint.Cap.ROUND
                
                for (i in 1..3) {
                    val waveRadius = iconRadius * (0.3f + i * 0.2f)
                    val alpha = (255 * (1f - i * 0.2f)).toInt()
                    iconPaint.alpha = alpha
                    canvas.drawCircle(centerX, centerY, waveRadius, iconPaint)
                }
            }
            
            ProcessingStatus.CONNECTING -> {
                // Draw rotating dots
                iconPaint.style = Paint.Style.FILL
                val dotRadius = iconRadius * 0.15f
                val orbitRadius = iconRadius * 0.6f
                
                for (i in 0..2) {
                    val angle = (pulseValue * 360f + i * 120f) * Math.PI / 180f
                    val dotX = centerX + orbitRadius * cos(angle).toFloat()
                    val dotY = centerY + orbitRadius * sin(angle).toFloat()
                    
                    val alpha = (255 * (0.3f + 0.7f * sin(pulseValue * Math.PI + i * Math.PI / 1.5))).toInt().coerceIn(50, 255)
                    iconPaint.alpha = alpha
                    canvas.drawCircle(dotX, dotY, dotRadius, iconPaint)
                }
            }
            
            ProcessingStatus.ERROR -> {
                // Draw X icon
                iconPaint.style = Paint.Style.STROKE
                iconPaint.strokeCap = Paint.Cap.ROUND
                
                val lineLength = iconRadius * 0.8f
                canvas.drawLine(
                    centerX - lineLength / 2f, centerY - lineLength / 2f,
                    centerX + lineLength / 2f, centerY + lineLength / 2f,
                    iconPaint
                )
                canvas.drawLine(
                    centerX + lineLength / 2f, centerY - lineLength / 2f,
                    centerX - lineLength / 2f, centerY + lineLength / 2f,
                    iconPaint
                )
            }
        }
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pulseAnimator?.cancel()
    }
}