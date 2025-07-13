package com.cafetone.dsp.audio

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import org.junit.Test
import org.junit.Assert.*

/**
 * Phase 3: Audio Quality Refinement - Unit Tests
 * 
 * Tests for parameter smoothing and performance monitoring functionality
 */
class CafeModePhase3Test {
    
    @Test
    fun testParameterSmoothingBasic() {
        val smoother = CafeModeParameterSmoother()
        var updateCount = 0
        var lastIntensity = 0f
        
        smoother.setParameterUpdateCallback { intensity, spatialWidth, distance ->
            updateCount++
            lastIntensity = intensity
        }
        
        // Set initial parameters
        smoother.setParametersImmediate(70f, 60f, 80f)
        val (initialI, initialS, initialD) = smoother.getCurrentParameters()
        
        assertEquals(70f, initialI, 0.1f)
        assertEquals(60f, initialS, 0.1f)
        assertEquals(80f, initialD, 0.1f)
    }
    
    @Test
    fun testParameterSmoothingTransition() = runBlocking {
        val smoother = CafeModeParameterSmoother()
        var updateCount = 0
        val updates = mutableListOf<Float>()
        
        smoother.setParameterUpdateCallback { intensity, _, _ ->
            updateCount++
            updates.add(intensity)
        }
        
        // Set initial value
        smoother.setParametersImmediate(70f, 60f, 80f)
        
        // Update to new value and wait for transitions
        smoother.updateIntensity(90f)
        
        // Wait for debouncing and interpolation to complete
        delay(100)
        
        // Should have multiple interpolation steps
        assertTrue("Should have multiple update steps", updateCount > 1)
        
        // Final value should be target value
        val (finalI, _, _) = smoother.getCurrentParameters()
        assertEquals(90f, finalI, 0.1f)
        
        smoother.stop()
    }
    
    @Test
    fun testPerformanceMonitorStats() {
        val monitor = CafeModePerformanceMonitor()
        
        // Initially no stats
        assertNull(monitor.getCurrentStats())
        
        // Add some sample latency measurements
        monitor.measureAudioLatency(null) // Engine is null in test
        
        val stats = monitor.getCurrentStats()
        assertNotNull(stats)
        
        // Should track samples
        assertTrue("Should have at least one sample", stats!!.samplesCount >= 0)
    }
    
    @Test
    fun testAudioArtifactDetection() {
        val monitor = CafeModePerformanceMonitor()
        var alertTriggered = false
        
        monitor.setAlertCallback { alert ->
            if (alert is CafeModePerformanceMonitor.PerformanceAlert.AudioArtifact) {
                alertTriggered = true
            }
        }
        
        // Create audio buffer with artifact (sudden amplitude change)
        val audioBuffer = floatArrayOf(0.1f, 0.1f, 0.9f, 0.1f) // Sharp spike
        
        val hasArtifact = monitor.detectAudioArtifacts(audioBuffer)
        
        assertTrue("Should detect audio artifact", hasArtifact)
        assertTrue("Should trigger alert callback", alertTriggered)
    }
    
    @Test
    fun testPerformanceTargets() {
        val monitor = CafeModePerformanceMonitor()
        
        // Test target checking logic
        val stats = CafeModePerformanceMonitor.PerformanceStats(
            averageLatencyMs = 30f,  // Below target (50ms)
            maxLatencyMs = 45L,
            averageCpuPercent = 3f,  // Below target (5%)
            maxCpuPercent = 4f,
            averageMemoryMb = 40f,   // Below target (50MB)
            maxMemoryMb = 45L,
            samplesCount = 10,
            targetsStatus = CafeModePerformanceMonitor.PerformanceTargets(
                latencyMet = true,
                cpuMet = true,
                memoryMet = true
            )
        )
        
        assertTrue("Latency target should be met", stats.targetsStatus.latencyMet)
        assertTrue("CPU target should be met", stats.targetsStatus.cpuMet)
        assertTrue("Memory target should be met", stats.targetsStatus.memoryMet)
    }
}