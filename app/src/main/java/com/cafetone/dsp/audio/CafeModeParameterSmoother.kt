package com.cafetone.dsp.audio

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.abs

/**
 * Phase 3: Audio Quality Refinement - Parameter Smoothing System
 * 
 * Implements smooth parameter transitions to prevent audio artifacts:
 * - 50ms transition duration
 * - 10ms interpolation steps  
 * - 50ms debounce delay after slider movement
 * - Linear interpolation between values
 */
class CafeModeParameterSmoother {
    companion object {
        private const val TRANSITION_DURATION_MS = 50L
        private const val INTERPOLATION_STEP_MS = 10L
        private const val DEBOUNCE_DELAY_MS = 50L
        private const val INTERPOLATION_STEPS = TRANSITION_DURATION_MS / INTERPOLATION_STEP_MS // 5 steps
    }
    
    // Current parameter values
    private var currentIntensity: Float = 70f
    private var currentSpatialWidth: Float = 60f
    private var currentDistance: Float = 80f
    
    // Target parameter values
    private var targetIntensity: Float = 70f
    private var targetSpatialWidth: Float = 60f
    private var targetDistance: Float = 80f
    
    // Interpolation control
    private var interpolationJob: Job? = null
    private var debounceJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    
    // Performance monitoring
    private var lastUpdateTime = 0L
    private var updateCount = 0L
    private var totalLatency = 0L
    
    // Callback for parameter updates
    private var parameterUpdateCallback: ((intensity: Float, spatialWidth: Float, distance: Float) -> Unit)? = null
    
    /**
     * Set the callback for parameter updates during interpolation
     */
    fun setParameterUpdateCallback(callback: (intensity: Float, spatialWidth: Float, distance: Float) -> Unit) {
        parameterUpdateCallback = callback
    }
    
    /**
     * Update intensity parameter with smoothing
     */
    fun updateIntensity(newValue: Float) {
        if (abs(newValue - targetIntensity) < 0.1f) return // Ignore micro-movements
        
        targetIntensity = newValue
        startParameterSmoothing()
    }
    
    /**
     * Update spatial width parameter with smoothing
     */
    fun updateSpatialWidth(newValue: Float) {
        if (abs(newValue - targetSpatialWidth) < 0.1f) return // Ignore micro-movements
        
        targetSpatialWidth = newValue
        startParameterSmoothing()
    }
    
    /**
     * Update distance parameter with smoothing
     */
    fun updateDistance(newValue: Float) {
        if (abs(newValue - targetDistance) < 0.1f) return // Ignore micro-movements
        
        targetDistance = newValue
        startParameterSmoothing()
    }
    
    /**
     * Set all parameters immediately without smoothing (for initialization)
     */
    fun setParametersImmediate(intensity: Float, spatialWidth: Float, distance: Float) {
        currentIntensity = intensity
        currentSpatialWidth = spatialWidth  
        currentDistance = distance
        targetIntensity = intensity
        targetSpatialWidth = spatialWidth
        targetDistance = distance
    }
    
    /**
     * Start parameter smoothing with debouncing
     */
    private fun startParameterSmoothing() {
        // Cancel existing jobs
        debounceJob?.cancel()
        
        // Start debounce timer
        debounceJob = coroutineScope.launch {
            delay(DEBOUNCE_DELAY_MS)
            performParameterInterpolation()
        }
    }
    
    /**
     * Perform smooth interpolation between current and target values
     */
    private suspend fun performParameterInterpolation() {
        val startTime = System.nanoTime()
        
        // Calculate starting values
        val startIntensity = currentIntensity
        val startSpatialWidth = currentSpatialWidth
        val startDistance = currentDistance
        
        // Calculate deltas
        val deltaIntensity = targetIntensity - startIntensity
        val deltaSpatialWidth = targetSpatialWidth - startSpatialWidth
        val deltaDistance = targetDistance - startDistance
        
        Timber.d("Starting parameter interpolation: " +
                "Intensity: $startIntensity → $targetIntensity, " +
                "SpatialWidth: $startSpatialWidth → $targetSpatialWidth, " +
                "Distance: $startDistance → $targetDistance")
        
        // Cancel any existing interpolation
        interpolationJob?.cancel()
        
        interpolationJob = coroutineScope.launch {
            for (step in 1..INTERPOLATION_STEPS.toInt()) {
                val progress = step.toFloat() / INTERPOLATION_STEPS.toFloat()
                
                // Linear interpolation
                currentIntensity = startIntensity + (deltaIntensity * progress)
                currentSpatialWidth = startSpatialWidth + (deltaSpatialWidth * progress)
                currentDistance = startDistance + (deltaDistance * progress)
                
                // Apply parameters via callback
                parameterUpdateCallback?.invoke(currentIntensity, currentSpatialWidth, currentDistance)
                
                // Wait for next step (except on last iteration)
                if (step < INTERPOLATION_STEPS) {
                    delay(INTERPOLATION_STEP_MS)
                }
            }
            
            // Ensure final values are exact
            currentIntensity = targetIntensity
            currentSpatialWidth = targetSpatialWidth
            currentDistance = targetDistance
            parameterUpdateCallback?.invoke(currentIntensity, currentSpatialWidth, currentDistance)
            
            // Performance monitoring
            val endTime = System.nanoTime()
            val latencyMs = (endTime - startTime) / 1_000_000
            updateCount++
            totalLatency += latencyMs
            
            Timber.d("Parameter interpolation completed in ${latencyMs}ms")
            
            if (updateCount % 10 == 0L) { // Log performance stats every 10 updates
                val avgLatency = totalLatency.toFloat() / updateCount
                Timber.i("Parameter smoothing performance: Average latency: ${avgLatency}ms over $updateCount updates")
            }
        }
    }
    
    /**
     * Get current parameter values
     */
    fun getCurrentParameters(): Triple<Float, Float, Float> {
        return Triple(currentIntensity, currentSpatialWidth, currentDistance)
    }
    
    /**
     * Get performance statistics
     */
    fun getPerformanceStats(): Pair<Long, Float> {
        val avgLatency = if (updateCount > 0) totalLatency.toFloat() / updateCount else 0f
        return Pair(updateCount, avgLatency)
    }
    
    /**
     * Check if interpolation is currently active
     */
    fun isInterpolating(): Boolean {
        return interpolationJob?.isActive == true
    }
    
    /**
     * Stop all interpolation and reset
     */
    fun stop() {
        interpolationJob?.cancel()
        debounceJob?.cancel()
    }
}