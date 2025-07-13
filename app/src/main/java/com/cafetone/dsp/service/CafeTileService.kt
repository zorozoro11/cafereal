package com.cafetone.dsp.service

import android.annotation.TargetApi
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.cafetone.dsp.R
import com.cafetone.dsp.activity.MainActivity
import com.cafetone.dsp.utils.isRootless
import timber.log.Timber

/**
 * Phase 4: System Integration - Quick Settings Tile Service
 * 
 * Provides quick toggle control for Cafetone from the Quick Settings panel
 */
@TargetApi(Build.VERSION_CODES.N)
@RequiresApi(Build.VERSION_CODES.N)
class CafeTileService : TileService() {
    
    companion object {
        private const val TILE_STATE_ACTIVE = "active"
        private const val TILE_STATE_INACTIVE = "inactive"
    }
    
    override fun onTileAdded() {
        super.onTileAdded()
        Timber.d("Café tile added to Quick Settings")
        updateTile()
    }
    
    override fun onTileRemoved() {
        super.onTileRemoved()
        Timber.d("Café tile removed from Quick Settings")
    }
    
    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }
    
    override fun onStopListening() {
        super.onStopListening()
    }
    
    override fun onClick() {
        super.onClick()
        
        try {
            if (isCafeModeActive()) {
                // Currently active, turn off
                stopCafeMode()
            } else {
                // Currently inactive, turn on
                startCafeMode()
            }
            
            updateTile()
            
        } catch (e: Exception) {
            Timber.e(e, "Error handling tile click")
            showFailureToast()
        }
    }
    
    /**
     * Update tile appearance based on current state
     */
    private fun updateTile() {
        val tile = qsTile ?: return
        
        try {
            val isActive = isCafeModeActive()
            
            // Set tile state
            tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            
            // Set tile label
            tile.label = getString(R.string.app_name)
            
            // Set tile subtitle (Android 10+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = if (isActive) {
                    getString(R.string.processing_active)
                } else {
                    getString(R.string.processing_inactive)
                }
            }
            
            // Set tile icon
            val iconRes = if (isActive) {
                R.drawable.ic_cafe_active
            } else {
                R.drawable.ic_cafe_inactive
            }
            tile.icon = Icon.createWithResource(this, iconRes)
            
            // Set content description for accessibility
            tile.contentDescription = getString(
                R.string.processing_status_desc,
                if (isActive) getString(R.string.processing_active) else getString(R.string.processing_inactive)
            )
            
            // Update tile
            tile.updateTile()
            
        } catch (e: Exception) {
            Timber.e(e, "Error updating tile")
        }
    }
    
    /**
     * Check if café mode is currently active
     */
    private fun isCafeModeActive(): Boolean {
        return try {
            BaseAudioProcessorService.activeServices > 0
        } catch (e: Exception) {
            Timber.e(e, "Error checking service status")
            false
        }
    }
    
    /**
     * Start café mode service
     */
    private fun startCafeMode() {
        try {
            if (isRootless()) {
                // For rootless version, we need to launch the main activity
                // since media projection permission is required
                val intent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(MainActivity.EXTRA_FORCE_SHOW_CAPTURE_PROMPT, true)
                }
                startActivityAndCollapse(intent)
            } else {
                // For root version, we can start the service directly
                // This code path would be used in root flavor
                Timber.d("Direct service start not implemented for rootless version")
                launchMainActivity()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error starting café mode")
            showFailureToast()
        }
    }
    
    /**
     * Stop café mode service
     */
    private fun stopCafeMode() {
        try {
            if (isRootless()) {
                RootlessAudioProcessorService.stop(this)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error stopping café mode")
            showFailureToast()
        }
    }
    
    /**
     * Launch main activity when direct service control isn't possible
     */
    private fun launchMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivityAndCollapse(intent)
    }
    
    /**
     * Show failure toast (Android 10+)
     */
    private fun showFailureToast() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            showDialog(
                android.app.AlertDialog.Builder(this)
                    .setTitle(getString(R.string.app_name))
                    .setMessage("Failed to toggle café mode. Please use the main app.")
                    .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
                    .create()
            )
        }
    }
}