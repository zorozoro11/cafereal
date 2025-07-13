package com.cafetone.dsp.activity

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.HapticFeedbackConstants
import android.widget.SeekBar
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.cafetone.dsp.BuildConfig
import com.cafetone.dsp.MainApplication
import com.cafetone.dsp.R
import com.cafetone.dsp.databinding.ActivityCafeModeBinding
import com.cafetone.dsp.flavor.CrashlyticsImpl
import com.cafetone.dsp.service.BaseAudioProcessorService
import com.cafetone.dsp.service.RootlessAudioProcessorService
import com.cafetone.dsp.utils.Constants
import com.cafetone.dsp.utils.SdkCheck
import com.cafetone.dsp.utils.extensions.AssetManagerExtensions.installPrivateAssets
import com.cafetone.dsp.utils.extensions.ContextExtensions.check
import com.cafetone.dsp.utils.extensions.ContextExtensions.getAppName
import com.cafetone.dsp.utils.extensions.ContextExtensions.registerLocalReceiver
import com.cafetone.dsp.utils.extensions.ContextExtensions.requestIgnoreBatteryOptimizations
import com.cafetone.dsp.utils.extensions.ContextExtensions.sendLocalBroadcast
import com.cafetone.dsp.utils.extensions.ContextExtensions.toast
import com.cafetone.dsp.utils.extensions.ContextExtensions.unregisterLocalReceiver
import com.cafetone.dsp.utils.extensions.PermissionExtensions.hasDumpPermission
import com.cafetone.dsp.utils.extensions.PermissionExtensions.hasRecordPermission
import com.cafetone.dsp.utils.isRootless
import com.cafetone.dsp.utils.sdkAbove
import com.cafetone.dsp.view.FloatingToggleButton
import org.koin.core.component.inject
import timber.log.Timber
import com.cafetone.dsp.audio.CafeModeParameterSmoother
import com.cafetone.dsp.audio.CafeModePerformanceMonitor
import com.cafetone.dsp.model.CafeModePreset
import com.cafetone.dsp.model.CafeModePresetManager
import com.cafetone.dsp.view.ProcessingStatusIndicatorView
import com.cafetone.dsp.view.AudioLevelMeterView
import com.cafetone.dsp.ui.UIAnimationHelper
import java.util.Timer
import java.util.TimerTask

class MainActivity : BaseActivity() {
    /* UI bindings */
    private lateinit var binding: ActivityCafeModeBinding
    
    /* Café mode controls */
    private var intensityLevel: Int = 70  // Default 70%
    private var spatialWidthLevel: Int = 60  // Default 60%
    private var distanceLevel: Int = 80  // Default 80%
    
    /* Phase 3: Audio Quality Refinement Components */
    private lateinit var parameterSmoother: CafeModeParameterSmoother
    private lateinit var performanceMonitor: CafeModePerformanceMonitor
    
    /* Phase 4: Advanced Features Components */
    private lateinit var presetManager: CafeModePresetManager
    private lateinit var processingStatus: ProcessingStatusIndicatorView
    private lateinit var audioLevelMeter: AudioLevelMeterView
    private var audioLevelTimer: Timer? = null
    
    /* Audio processing service */
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var capturePermissionLauncher: ActivityResultLauncher<Intent>
    private var processorService: BaseAudioProcessorService? = null
    private var processorServiceBound: Boolean = false

    private val processorServiceConnection by lazy {
        object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName, service: IBinder) {
                Timber.d("Service connected")
                processorService = (service as BaseAudioProcessorService.LocalBinder).service
                processorServiceBound = true
                binding.powerToggle.isToggled = true
                
                // Phase 4: Update processing status
                if (::processingStatus.isInitialized) {
                    processingStatus.setStatus(ProcessingStatusIndicatorView.ProcessingStatus.ACTIVE)
                }
            }

            override fun onServiceDisconnected(arg0: ComponentName) {
                Timber.d("Service disconnected")
                processorService = null
                processorServiceBound = false
                
                // Phase 4: Update processing status
                if (::processingStatus.isInitialized) {
                    processingStatus.setStatus(ProcessingStatusIndicatorView.ProcessingStatus.INACTIVE)
                }
            }
        }
    }

    private var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Constants.ACTION_SERVICE_STOPPED -> {
                    binding.powerToggle.isToggled = false
                }
                Constants.ACTION_SERVICE_STARTED -> {
                    binding.powerToggle.isToggled = true
                }
            }
        }
    }

    @SuppressLint("BatteryLife")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val firstBoot = prefsVar.get<Boolean>(R.string.key_first_boot)
        assets.installPrivateAssets(this, force = firstBoot)

        mediaProjectionManager = getSystemService<MediaProjectionManager>()!!
        binding = ActivityCafeModeBinding.inflate(layoutInflater)

        val check = applicationContext.check()
        if(check != 0) {
            toast("($check) Cannot launch application. Please re-download the latest version.")
            Timber.e(UnsupportedOperationException("Launch error $check; package=${packageName}; app_name=${getAppName()}"))
            finish()
            return
        }

        // Setup views
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        actionBar?.setDisplayHomeAsUpEnabled(false)
        actionBar?.setHomeButtonEnabled(false)
        actionBar?.setDisplayShowTitleEnabled(true)
        actionBar?.title = "Cafetone"
        binding.appBarLayout.statusBarForeground = MaterialShapeDrawable.createWithElevationOverlay(this)

        // Setup café mode controls
        setupCafeModeControls()
        
        // Phase 3: Initialize audio quality refinement components
        initializePhase3Components()
        
        // Phase 4: Initialize advanced features
        initializePhase4Components()

        // Check permissions and launch onboarding if required
        if(SdkCheck.isQ && isRootless() && (!hasDumpPermission() || !hasRecordPermission())) {
            Timber.i("Launching onboarding (first boot: $firstBoot)")
            startActivity(Intent(this, OnboardingActivity::class.java).apply {
                putExtra(OnboardingActivity.EXTRA_FIX_PERMS, !firstBoot)
            })
            this.finish()
            return
        }

        // Register broadcast receivers
        IntentFilter(Constants.ACTION_SERVICE_STOPPED).apply {
            addAction(Constants.ACTION_SERVICE_STARTED)
            registerLocalReceiver(broadcastReceiver, this)
        }

        // Setup power toggle
        binding.powerToggle.toggleOnClick = false
        binding.powerToggle.setOnToggleClickListener(object : FloatingToggleButton.OnToggleClickListener{
            override fun onClick() {
                sdkAbove(Build.VERSION_CODES.R) {
                    binding.powerToggle.performHapticFeedback(
                        if(binding.powerToggle.isToggled)
                            HapticFeedbackConstants.CONFIRM
                        else
                            HapticFeedbackConstants.REJECT
                    )
                }

                if(SdkCheck.isQ && isRootless()) {
                    if (binding.powerToggle.isToggled) {
                        // Currently on, let's turn it off
                        RootlessAudioProcessorService.stop(this@MainActivity)
                        binding.powerToggle.isToggled = false
                    } else {
                        // Currently off, let's turn it on
                        requestCapturePermission()
                    }
                }
            }
        })

        // Setup media projection launcher
        if (SdkCheck.isQ && isRootless()) {
            capturePermissionLauncher = registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == RESULT_OK && isRootless()) {
                    app.mediaProjectionStartIntent = result.data
                    binding.powerToggle.isToggled = true
                    RootlessAudioProcessorService.start(this, result.data)
                } else {
                    binding.powerToggle.isToggled = false
                }
            }
        }

        // Request capture permission if redirected from onboarding
        if (SdkCheck.isQ && isRootless()) {
            if (intent.getBooleanExtra(EXTRA_FORCE_SHOW_CAPTURE_PROMPT, false)) {
                requestCapturePermission()
            }
        }

        // Load preference states
        loadCafeModeSettings()
        sendLocalBroadcast(Intent(Constants.ACTION_PREFERENCES_UPDATED))
    }

    private fun setupCafeModeControls() {
        // Setup Intensity slider
        binding.intensitySlider.progress = intensityLevel
        binding.intensityValue.text = "$intensityLevel%"
        binding.intensitySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    intensityLevel = progress
                    binding.intensityValue.text = "$progress%"
                    
                    // Phase 4: Add haptic feedback and accessibility
                    UIAnimationHelper.provideHapticFeedback(this@MainActivity, UIAnimationHelper.HapticType.LIGHT)
                    binding.intensitySlider.contentDescription = "Café intensity: ${progress}%"
                    
                    // Phase 3: Use parameter smoother for artifact-free transitions
                    parameterSmoother.updateIntensity(progress.toFloat())
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Measure UI responsiveness
                val startTime = System.nanoTime()
                binding.intensitySlider.tag = startTime
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Calculate UI response time (target: <100ms)
                val startTime = binding.intensitySlider.tag as? Long ?: return
                val responseTime = (System.nanoTime() - startTime) / 1_000_000
                Timber.d("Intensity slider response time: ${responseTime}ms")
                
                if (responseTime > 100) {
                    Timber.w("UI responsiveness target missed: ${responseTime}ms > 100ms")
                }
            }
        })

        // Setup Spatial Width slider
        binding.spatialWidthSlider.progress = spatialWidthLevel
        binding.spatialWidthValue.text = "$spatialWidthLevel%"
        binding.spatialWidthSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    spatialWidthLevel = progress
                    binding.spatialWidthValue.text = "$progress%"
                    
                    // Phase 4: Add haptic feedback and accessibility
                    UIAnimationHelper.provideHapticFeedback(this@MainActivity, UIAnimationHelper.HapticType.LIGHT)
                    binding.spatialWidthSlider.contentDescription = "Spatial width: ${progress}%"
                    
                    // Phase 3: Use parameter smoother for artifact-free transitions
                    parameterSmoother.updateSpatialWidth(progress.toFloat())
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                val startTime = System.nanoTime()
                binding.spatialWidthSlider.tag = startTime
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val startTime = binding.spatialWidthSlider.tag as? Long ?: return
                val responseTime = (System.nanoTime() - startTime) / 1_000_000
                Timber.d("Spatial width slider response time: ${responseTime}ms")
                
                if (responseTime > 100) {
                    Timber.w("UI responsiveness target missed: ${responseTime}ms > 100ms")
                }
            }
        })

        // Setup Distance slider
        binding.distanceSlider.progress = distanceLevel
        binding.distanceValue.text = "$distanceLevel%"
        binding.distanceSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    distanceLevel = progress
                    binding.distanceValue.text = "$progress%"
                    
                    // Phase 4: Add haptic feedback and accessibility
                    UIAnimationHelper.provideHapticFeedback(this@MainActivity, UIAnimationHelper.HapticType.LIGHT)
                    binding.distanceSlider.contentDescription = "Distance effect: ${progress}%"
                    
                    // Phase 3: Use parameter smoother for artifact-free transitions
                    parameterSmoother.updateDistance(progress.toFloat())
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                val startTime = System.nanoTime()
                binding.distanceSlider.tag = startTime
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val startTime = binding.distanceSlider.tag as? Long ?: return
                val responseTime = (System.nanoTime() - startTime) / 1_000_000
                Timber.d("Distance slider response time: ${responseTime}ms")
                
                if (responseTime > 100) {
                    Timber.w("UI responsiveness target missed: ${responseTime}ms > 100ms")
                }
            }
        })
    }

    private fun applyCafeModeSettings() {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // Apply café mode DSP settings using Sony café mode algorithm
                Timber.d("Applying café mode settings: Intensity=$intensityLevel%, SpatialWidth=$spatialWidthLevel%, Distance=$distanceLevel%")
                
                // Get the DSP engine from the service
                val engine = processorService?.getDspEngine()
                
                if (engine != null) {
                    // Apply Distance Simulation EQ Profile
                    val distanceEqBands = getDistanceEqBands(distanceLevel.toFloat())
                    engine.setMultiEqualizer(true, 0, 0, distanceEqBands)
                    
                    // Apply Spatial & Positioning effects
                    val spatialSettings = getSpatialSettings(spatialWidthLevel.toFloat())
                    engine.setCrossfeedCustom(true, spatialSettings.crossfeedFcut, spatialSettings.crossfeedFeed)
                    engine.setStereoEnhancement(true, spatialSettings.stereoWidth)
                    
                    // Apply Café Ambience
                    val ambienceSettings = getCafeAmbienceSettings(intensityLevel.toFloat())
                    engine.setReverb(true, ambienceSettings.reverbPreset)
                    engine.setOutputControl(-0.1f, 60f, ambienceSettings.overallGainReduction)
                    
                    // Send preference update to sync with service
                    sendLocalBroadcast(Intent(Constants.ACTION_PREFERENCES_UPDATED))
                    
                    Timber.i("Café mode settings applied successfully")
                } else {
                    Timber.w("DSP engine not available - service may not be running")
                }
                
                // Save settings for persistence
                saveCafeModeSettings()
                
            } catch (ex: Exception) {
                Timber.e(ex, "Failed to apply café mode settings")
            }
        }
    }
    
    /**
     * Phase 3: Apply café mode settings with smooth parameter transitions
     * Used by the parameter smoother for artifact-free updates
     */
    private fun applyCafeModeSettingsSmooth(intensity: Float, spatialWidth: Float, distance: Float) {
        try {
            val startTime = System.nanoTime()
            
            // Get the DSP engine from the service
            val engine = processorService?.getDspEngine()
            
            if (engine != null) {
                // Measure latency for performance monitoring
                performanceMonitor.measureAudioLatency(engine)
                
                // Apply Distance Simulation EQ Profile
                val distanceEqBands = getDistanceEqBands(distance)
                engine.setMultiEqualizer(true, 0, 0, distanceEqBands)
                
                // Apply Spatial & Positioning effects
                val spatialSettings = getSpatialSettings(spatialWidth)
                engine.setCrossfeedCustom(true, spatialSettings.crossfeedFcut, spatialSettings.crossfeedFeed)
                engine.setStereoEnhancement(true, spatialSettings.stereoWidth)
                
                // Apply Café Ambience
                val ambienceSettings = getCafeAmbienceSettings(intensity)
                engine.setReverb(true, ambienceSettings.reverbPreset)
                engine.setOutputControl(-0.1f, 60f, ambienceSettings.overallGainReduction)
                
                // Update UI values on main thread
                CoroutineScope(Dispatchers.Main).launch {
                    binding.intensityValue.text = "${intensity.toInt()}%"
                    binding.spatialWidthValue.text = "${spatialWidth.toInt()}%"
                    binding.distanceValue.text = "${distance.toInt()}%"
                }
                
                val endTime = System.nanoTime()
                val latencyMs = (endTime - startTime) / 1_000_000
                
                // Log smooth transition performance
                Timber.d("Smooth café mode settings applied in ${latencyMs}ms: Intensity=${intensity}%, SpatialWidth=${spatialWidth}%, Distance=${distance}%")
                
                // Check if we exceeded latency target
                if (latencyMs > 50) {
                    Timber.w("Smooth parameter update exceeded latency target: ${latencyMs}ms > 50ms")
                }
                
            } else {
                Timber.w("DSP engine not available for smooth parameter update")
            }
            
        } catch (ex: Exception) {
            Timber.e(ex, "Failed to apply smooth café mode settings")
        }
    }
    
    // Sony Café Mode DSP Processing Chain Implementation
    
    /**
     * Distance Simulation EQ Profile (Distance slider: 0-100% → 80% default)
     * Maps distance percentage to EQ curve that simulates acoustic distance
     */
    private fun getDistanceEqBands(distancePercent: Float): String {
        val intensity = distancePercent / 100f
        
        // 15-band EQ configuration for distance simulation
        // Format: frequencies;gains (30 values total - 15 frequencies + 15 gains)
        val frequencies = arrayOf(
            25.0, 40.0, 63.0, 100.0, 160.0, 250.0, 400.0, 630.0, 1000.0, 1600.0, 2500.0, 4000.0, 6300.0, 10000.0, 16000.0
        )
        
        val gains = arrayOf(
            -4.0f * intensity,      // Sub-bass roll-off
            -6.0f * intensity,      // Bass reduction
            -5.0f * intensity,      // Low-bass
            -4.0f * intensity,      // Low-mid bass
            -3.5f * intensity,      // Low-mid scoop
            -3.0f * intensity,      // Mid scoop
            -2.5f * intensity,      // Mid transparency
            -2.0f * intensity,      // High-mid start
            -1.5f * intensity,      // Mid presence
            -2.0f * intensity,      // High-mid
            -4.0f * intensity,      // Upper-mid
            -5.0f * intensity,      // Air absorption
            -7.0f * intensity,      // Treble softening
            -11.0f * intensity,     // Ultra-high cut
            -15.0f * intensity      // Extreme high cut
        )
        
        // Combine frequencies and gains in the expected format
        val allValues = frequencies.map { it.toString() } + gains.map { it.toString() }
        return allValues.joinToString(";")
    }
    
    /**
     * Spatial & Positioning (Spatial Width slider: 0-100% → 60% default)
     * Maps spatial width percentage to stereo effects
     */
    private fun getSpatialSettings(spatialPercent: Float): SpatialSettings {
        val width = spatialPercent / 100f
        return SpatialSettings(
            crossfeedFcut = (700 + (width * 400)).toInt(),      // 700Hz to 1100Hz
            crossfeedFeed = (10 + (width * 25)).toInt(),        // 10% to 35% feed
            stereoWidth = 1.2f + (width * 0.8f)                 // 120% to 200% width
        )
    }
    
    /**
     * Café Ambience (Intensity slider: 0-100% → 70% default)
     * Maps intensity percentage to café ambience settings
     */
    private fun getCafeAmbienceSettings(intensityPercent: Float): AmbienceSettings {
        val intensity = intensityPercent / 100f
        return AmbienceSettings(
            reverbPreset = if (intensity > 0.5f) 2 else 1,      // Room reverb type
            overallGainReduction = -3.0f - (intensity * 7.0f)   // -3dB to -10dB
        )
    }
    
    // Data classes for DSP settings
    private data class SpatialSettings(
        val crossfeedFcut: Int,
        val crossfeedFeed: Int,
        val stereoWidth: Float
    )
    
    private data class AmbienceSettings(
        val reverbPreset: Int,
        val overallGainReduction: Float
    )

    private fun loadCafeModeSettings() {
        intensityLevel = prefsApp.get<Int>(R.string.key_cafe_intensity) ?: 70
        spatialWidthLevel = prefsApp.get<Int>(R.string.key_cafe_spatial_width) ?: 60
        distanceLevel = prefsApp.get<Int>(R.string.key_cafe_distance) ?: 80
        
        binding.intensitySlider.progress = intensityLevel
        binding.intensityValue.text = "$intensityLevel%"
        binding.spatialWidthSlider.progress = spatialWidthLevel
        binding.spatialWidthValue.text = "$spatialWidthLevel%"
        binding.distanceSlider.progress = distanceLevel
        binding.distanceValue.text = "$distanceLevel%"
        
        // Phase 3: Update parameter smoother with loaded values
        if (::parameterSmoother.isInitialized) {
            parameterSmoother.setParametersImmediate(
                intensityLevel.toFloat(),
                spatialWidthLevel.toFloat(),
                distanceLevel.toFloat()
            )
        }
    }

    private fun saveCafeModeSettings() {
        prefsApp.set(R.string.key_cafe_intensity, intensityLevel)
        prefsApp.set(R.string.key_cafe_spatial_width, spatialWidthLevel)
        prefsApp.set(R.string.key_cafe_distance, distanceLevel)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestCapturePermission() {
        if(app.mediaProjectionStartIntent != null && isRootless()) {
            binding.powerToggle.isToggled = true
            RootlessAudioProcessorService.start(this, app.mediaProjectionStartIntent)
            return
        }
        try {
            capturePermissionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
        } catch (ex: ActivityNotFoundException) {
            toast("Error: Projection API missing")
            Timber.e(ex)
        }
    }

    override fun onStart() {
        super.onStart()
        bindProcessorService()
    }

    override fun onStop() {
        super.onStop()
        unbindProcessorService()
    }

    override fun onPause() {
        super.onPause()
        saveCafeModeSettings()
        prefsVar.set(R.string.key_is_activity_active, false)
    }

    override fun onDestroy() {
        unregisterLocalReceiver(broadcastReceiver)
        
        // Phase 3: Clean up audio quality refinement components
        if (::parameterSmoother.isInitialized) {
            parameterSmoother.stop()
        }
        if (::performanceMonitor.isInitialized) {
            performanceMonitor.stopMonitoring()
        }
        
        try {
            if (processorService != null && processorServiceBound)
                unbindService(processorServiceConnection)
        } catch (_: Exception) {}

        processorService = null
        processorServiceBound = false
        prefsVar.set(R.string.key_is_activity_active, false)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        prefsVar.set(R.string.key_is_activity_active, true)
        
        if(isRootless())
            binding.powerToggle.isToggled = processorService != null
    }

    private fun bindProcessorService() {
        if (isRootless()) {
            sdkAbove(Build.VERSION_CODES.Q) {
                Intent(this, RootlessAudioProcessorService::class.java).also { intent ->
                    val ret = bindService(intent, processorServiceConnection, 0)
                    if (!ret)
                        requestCapturePermission()
                }
            }
        }
    }

    private fun unbindProcessorService() {
        try {
            unbindService(processorServiceConnection)
        } catch (ex: IllegalArgumentException) {
            Timber.d("Failed to unbind service connection. Not registered?")
            Timber.i(ex)
        }
    }

    companion object {
        const val EXTRA_FORCE_SHOW_CAPTURE_PROMPT = "ForceShowCapturePrompt"
    }
    
    /**
     * Phase 3: Initialize Audio Quality Refinement Components
     */
    private fun initializePhase3Components() {
        Timber.i("Phase 3: Initializing audio quality refinement components")
        
        // Initialize parameter smoother
        parameterSmoother = CafeModeParameterSmoother()
        parameterSmoother.setParametersImmediate(
            intensityLevel.toFloat(),
            spatialWidthLevel.toFloat(), 
            distanceLevel.toFloat()
        )
        
        // Set parameter update callback for smooth transitions
        parameterSmoother.setParameterUpdateCallback { intensity, spatialWidth, distance ->
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    applyCafeModeSettingsSmooth(intensity, spatialWidth, distance)
                } catch (ex: Exception) {
                    Timber.e(ex, "Error in smooth parameter callback")
                }
            }
        }
        
        // Initialize performance monitor
        performanceMonitor = CafeModePerformanceMonitor()
        
        // Set performance monitoring callbacks
        performanceMonitor.setPerformanceCallback { stats ->
            // Log performance statistics
            Timber.d("Performance: Latency: ${stats.averageLatencyMs}ms, CPU: ${stats.averageCpuPercent}%, Memory: ${stats.averageMemoryMb}MB")
            
            // Update UI with performance indicators (could be added later)
            if (!stats.targetsStatus.latencyMet || !stats.targetsStatus.cpuMet || !stats.targetsStatus.memoryMet) {
                Timber.w("Performance targets not met: Latency=${stats.targetsStatus.latencyMet}, CPU=${stats.targetsStatus.cpuMet}, Memory=${stats.targetsStatus.memoryMet}")
            }
        }
        
        performanceMonitor.setAlertCallback { alert ->
            when (alert) {
                is CafeModePerformanceMonitor.PerformanceAlert.HighLatency -> {
                    Timber.w("High latency detected: ${alert.latencyMs}ms (target: <50ms)")
                }
                is CafeModePerformanceMonitor.PerformanceAlert.HighCpuUsage -> {
                    Timber.w("High CPU usage detected: ${alert.cpuPercent}% (target: <5%)")
                }
                is CafeModePerformanceMonitor.PerformanceAlert.HighMemoryUsage -> {
                    Timber.w("High memory usage detected: ${alert.memoryMb}MB (target: <50MB)")
                }
                is CafeModePerformanceMonitor.PerformanceAlert.AudioArtifact -> {
                    Timber.w("Audio artifact detected: ${alert.description}")
                }
            }
        }
        
        // Start performance monitoring
        performanceMonitor.startMonitoring()
        
        Timber.i("Phase 3: Audio quality refinement components initialized successfully")
    }
    
    /**
     * Phase 4: Initialize Advanced Features & User Experience Components
     */
    private fun initializePhase4Components() {
        Timber.i("Phase 4: Initializing advanced features and user experience components")
        
        // Initialize preset manager
        presetManager = CafeModePresetManager(this)
        
        // Initialize visual components
        processingStatus = binding.processingStatus
        audioLevelMeter = binding.audioLevelMeter
        
        // Setup preset buttons
        setupPresetButtons()
        
        // Setup visual enhancements
        setupVisualEnhancements()
        
        // Setup accessibility features
        setupAccessibilityFeatures()
        
        // Load current preset
        loadCurrentPreset()
        
        // Start audio level monitoring
        startAudioLevelMonitoring()
        
        Timber.i("Phase 4: Advanced features initialized successfully")
    }
    
    /**
     * Setup preset button handlers
     */
    private fun setupPresetButtons() {
        val presetButtons = mapOf(
            binding.presetCozyCorner to "cozy_corner",
            binding.presetBusyCoffee to "busy_coffee_shop", 
            binding.presetQuietStudy to "quiet_study",
            binding.presetOutdoorTerrace to "outdoor_terrace"
        )
        
        presetButtons.forEach { (button, presetId) ->
            button.setOnClickListener { 
                applyPreset(presetId, button)
            }
        }
    }
    
    /**
     * Apply preset with animations and haptic feedback
     */
    private fun applyPreset(presetId: String, selectedButton: View) {
        val preset = CafeModePreset.getPresetById(presetId) ?: return
        
        // Provide haptic feedback
        UIAnimationHelper.provideHapticFeedback(this, UIAnimationHelper.HapticType.LIGHT)
        
        // Animate button press
        UIAnimationHelper.animateButtonPress(selectedButton) {
            // Apply preset settings
            presetManager.applyPreset(preset) { intensity, spatialWidth, distance ->
                runOnUiThread {
                    // Update sliders without triggering listeners
                    binding.intensitySlider.progress = intensity.toInt()
                    binding.spatialWidthSlider.progress = spatialWidth.toInt()
                    binding.distanceSlider.progress = distance.toInt()
                    
                    // Update internal values
                    intensityLevel = intensity.toInt()
                    spatialWidthLevel = spatialWidth.toInt()
                    distanceLevel = distance.toInt()
                    
                    // Update parameter smoother
                    parameterSmoother.setParametersImmediate(intensity, spatialWidth, distance)
                    
                    // Update UI
                    updatePresetUI(preset)
                    
                    // Show feedback
                    toast(getString(R.string.preset_applied, getString(preset.nameResId)))
                }
            }
        }
        
        // Animate preset selection
        val otherButtons = listOf(
            binding.presetCozyCorner,
            binding.presetBusyCoffee,
            binding.presetQuietStudy,
            binding.presetOutdoorTerrace
        )
        UIAnimationHelper.animatePresetSelection(selectedButton, otherButtons)
    }
    
    /**
     * Setup visual enhancements
     */
    private fun setupVisualEnhancements() {
        // Animate cards on startup
        UIAnimationHelper.animateCardAppearance(binding.intensitySlider.parent as View, 100)
        UIAnimationHelper.animateCardAppearance(binding.spatialWidthSlider.parent as View, 200)
        UIAnimationHelper.animateCardAppearance(binding.distanceSlider.parent as View, 300)
        
        // Setup processing status
        processingStatus.setStatus(ProcessingStatusIndicatorView.ProcessingStatus.INACTIVE)
    }
    
    /**
     * Setup accessibility features
     */
    private fun setupAccessibilityFeatures() {
        // Update slider content descriptions
        binding.intensitySlider.contentDescription = "Café intensity: ${intensityLevel}%"
        binding.spatialWidthSlider.contentDescription = "Spatial width: ${spatialWidthLevel}%"
        binding.distanceSlider.contentDescription = "Distance effect: ${distanceLevel}%"
        
        // Setup preset button descriptions
        binding.presetCozyCorner.contentDescription = getString(R.string.preset_button_desc, getString(R.string.preset_cozy_corner))
        binding.presetBusyCoffee.contentDescription = getString(R.string.preset_button_desc, getString(R.string.preset_busy_coffee_shop))
        binding.presetQuietStudy.contentDescription = getString(R.string.preset_button_desc, getString(R.string.preset_quiet_study))
        binding.presetOutdoorTerrace.contentDescription = getString(R.string.preset_button_desc, getString(R.string.preset_outdoor_terrace))
    }
    
    /**
     * Load current preset and update UI
     */
    private fun loadCurrentPreset() {
        val currentPreset = presetManager.getCurrentPreset()
        updatePresetUI(currentPreset)
    }
    
    /**
     * Update UI to reflect current preset
     */
    private fun updatePresetUI(preset: CafeModePreset) {
        binding.currentPresetText.text = getString(preset.nameResId)
    }
    
    /**
     * Start audio level monitoring for visual feedback
     */
    private fun startAudioLevelMonitoring() {
        audioLevelTimer = Timer()
        audioLevelTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val engine = processorService?.getDspEngine()
                if (engine != null) {
                    // Simulate audio level (in real implementation, get from DSP engine)
                    val simulatedLevel = kotlin.random.Random.nextFloat() * 0.8f
                    
                    runOnUiThread {
                        audioLevelMeter.updateLevel(simulatedLevel)
                    }
                } else {
                    runOnUiThread {
                        audioLevelMeter.updateLevel(0f)
                    }
                }
            }
        }, 0, 50) // Update every 50ms for smooth animation
    }
}