package com.cafetone.dsp.model

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.cafetone.dsp.R

/**
 * Phase 4: Advanced Features - Café Mode Preset System
 * 
 * Defines predefined café atmosphere presets for common scenarios
 */
data class CafeModePreset(
    val id: String,
    @StringRes val nameResId: Int,
    @StringRes val descriptionResId: Int,
    @DrawableRes val iconResId: Int,
    val intensity: Float,
    val spatialWidth: Float,
    val distance: Float,
    val isDefault: Boolean = false
) {
    companion object {
        /**
         * Get all available café mode presets as specified in Phase 4
         */
        fun getAllPresets(): List<CafeModePreset> {
            return listOf(
                // Cozy Corner: Intimate, warm café (Intensity: 85%, Spatial: 45%, Distance: 90%)
                CafeModePreset(
                    id = "cozy_corner",
                    nameResId = R.string.preset_cozy_corner,
                    descriptionResId = R.string.preset_cozy_corner_desc,
                    iconResId = R.drawable.ic_cozy_corner,
                    intensity = 85f,
                    spatialWidth = 45f,
                    distance = 90f
                ),
                
                // Busy Coffee Shop: Active café ambience (Intensity: 60%, Spatial: 75%, Distance: 70%)
                CafeModePreset(
                    id = "busy_coffee_shop",
                    nameResId = R.string.preset_busy_coffee_shop,
                    descriptionResId = R.string.preset_busy_coffee_shop_desc,
                    iconResId = R.drawable.ic_busy_coffee_shop,
                    intensity = 60f,
                    spatialWidth = 75f,
                    distance = 70f,
                    isDefault = true // Default preset
                ),
                
                // Quiet Study: Minimal café effect (Intensity: 40%, Spatial: 30%, Distance: 95%)
                CafeModePreset(
                    id = "quiet_study",
                    nameResId = R.string.preset_quiet_study,
                    descriptionResId = R.string.preset_quiet_study_desc,
                    iconResId = R.drawable.ic_quiet_study,
                    intensity = 40f,
                    spatialWidth = 30f,
                    distance = 95f
                ),
                
                // Outdoor Terrace: Open space café (Intensity: 70%, Spatial: 85%, Distance: 60%)
                CafeModePreset(
                    id = "outdoor_terrace",
                    nameResId = R.string.preset_outdoor_terrace,
                    descriptionResId = R.string.preset_outdoor_terrace_desc,
                    iconResId = R.drawable.ic_outdoor_terrace,
                    intensity = 70f,
                    spatialWidth = 85f,
                    distance = 60f
                )
            )
        }
        
        /**
         * Get default preset
         */
        fun getDefaultPreset(): CafeModePreset {
            return getAllPresets().find { it.isDefault } ?: getAllPresets()[1]
        }
        
        /**
         * Find preset by ID
         */
        fun getPresetById(id: String): CafeModePreset? {
            return getAllPresets().find { it.id == id }
        }
        
        /**
         * Create custom preset from current settings
         */
        fun createCustomPreset(
            name: String,
            description: String,
            intensity: Float,
            spatialWidth: Float,
            distance: Float
        ): CafeModePreset {
            return CafeModePreset(
                id = "custom_${System.currentTimeMillis()}",
                nameResId = R.string.preset_custom, // Will need to handle custom names differently
                descriptionResId = R.string.preset_custom_desc,
                iconResId = R.drawable.ic_custom_preset,
                intensity = intensity,
                spatialWidth = spatialWidth,
                distance = distance
            )
        }
    }
}

/**
 * Preset Manager for handling user presets and preferences
 */
class CafeModePresetManager(private val context: Context) {
    companion object {
        private const val PREFS_NAME = "cafe_mode_presets"
        private const val KEY_CURRENT_PRESET = "current_preset_id"
        private const val KEY_CUSTOM_PRESETS = "custom_presets"
    }
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Save current preset selection
     */
    fun saveCurrentPreset(presetId: String) {
        prefs.edit()
            .putString(KEY_CURRENT_PRESET, presetId)
            .apply()
    }
    
    /**
     * Get current preset ID
     */
    fun getCurrentPresetId(): String? {
        return prefs.getString(KEY_CURRENT_PRESET, null)
    }
    
    /**
     * Get current preset or default if none selected
     */
    fun getCurrentPreset(): CafeModePreset {
        val currentId = getCurrentPresetId()
        return if (currentId != null) {
            CafeModePreset.getPresetById(currentId) ?: CafeModePreset.getDefaultPreset()
        } else {
            CafeModePreset.getDefaultPreset()
        }
    }
    
    /**
     * Apply preset settings to sliders and DSP
     */
    fun applyPreset(preset: CafeModePreset, callback: (Float, Float, Float) -> Unit) {
        // Save as current preset
        saveCurrentPreset(preset.id)
        
        // Apply values via callback
        callback(preset.intensity, preset.spatialWidth, preset.distance)
    }
    
    /**
     * Check if current settings match any preset
     */
    fun findMatchingPreset(intensity: Float, spatialWidth: Float, distance: Float): CafeModePreset? {
        val tolerance = 2f // 2% tolerance for matching
        
        return CafeModePreset.getAllPresets().find { preset ->
            kotlin.math.abs(preset.intensity - intensity) <= tolerance &&
            kotlin.math.abs(preset.spatialWidth - spatialWidth) <= tolerance &&
            kotlin.math.abs(preset.distance - distance) <= tolerance
        }
    }
}