# Cafetone App Cleanup Plan

## Files to Keep (Essential for Cafetone functionality)

### Core Cafetone Features
1. `/app/app/src/main/java/com/cafetone/dsp/activity/MainActivity.kt` - Main café mode interface
2. `/app/app/src/main/java/com/cafetone/dsp/model/CafeModePreset.kt` - Preset system
3. `/app/app/src/main/java/com/cafetone/dsp/audio/CafeModeParameterSmoother.kt` - Parameter smoothing
4. `/app/app/src/main/java/com/cafetone/dsp/audio/CafeModePerformanceMonitor.kt` - Performance monitoring
5. `/app/app/src/main/java/com/cafetone/dsp/ui/UIAnimationHelper.kt` - UI animations
6. `/app/app/src/main/java/com/cafetone/dsp/view/ProcessingStatusIndicatorView.kt` - Status indicator
7. `/app/app/src/main/java/com/cafetone/dsp/view/AudioLevelMeterView.kt` - Audio level meter

### Essential Service Infrastructure
8. `/app/app/src/main/java/com/cafetone/dsp/service/BaseAudioProcessorService.kt` - Base service
9. `/app/app/src/main/java/com/cafetone/dsp/service/RootlessAudioProcessorService.kt` - Rootless service

### DSP Engine and Interop (Required by services)
10. `/app/app/src/main/java/com/cafetone/dsp/interop/JamesDspLocalEngine.kt` - Local DSP engine
11. `/app/app/src/main/java/com/cafetone/dsp/interop/JamesDspBaseEngine.kt` - Base DSP engine
12. `/app/app/src/main/java/com/cafetone/dsp/interop/JamesDspWrapper.kt` - DSP wrapper

### Essential Utils and Extensions (Required by MainActivity and services)
13. `/app/app/src/main/java/com/cafetone/dsp/utils/Constants.kt` - Constants
14. `/app/app/src/main/java/com/cafetone/dsp/utils/SdkCheck.kt` - SDK version checks
15. `/app/app/src/main/java/com/cafetone/dsp/utils/extensions/ContextExtensions.kt` - Context extensions
16. `/app/app/src/main/java/com/cafetone/dsp/utils/extensions/PermissionExtensions.kt` - Permission extensions
17. `/app/app/src/main/java/com/cafetone/dsp/utils/extensions/AssetManagerExtensions.kt` - Asset extensions
18. `/app/app/src/main/java/com/cafetone/dsp/utils/preferences/Preferences.kt` - Preferences

### Other Required Files
19. `/app/app/src/main/java/com/cafetone/dsp/MainApplication.kt` - Main application class
20. `/app/app/src/main/java/com/cafetone/dsp/BuildConfig.kt` - Build configuration
21. `/app/app/src/main/java/com/cafetone/dsp/R.kt` - Resources (generated)

## Files to Remove (Unused for Cafetone)

### AutoEQ Integration (Not used in cafetone)
- `/app/app/src/main/java/com/cafetone/dsp/api/AutoEqClient.kt`
- `/app/app/src/main/java/com/cafetone/dsp/api/AutoEqService.kt`
- `/app/app/src/main/java/com/cafetone/dsp/contract/AutoEqSelectorContract.kt`

### Session Management (Not used in cafetone)
- All files in `/app/app/src/main/java/com/cafetone/dsp/session/`

### Complex UI Fragments (Not used in cafetone simple UI)
- All files in `/app/app/src/main/java/com/cafetone/dsp/fragment/` except base classes if needed
- All files in `/app/app/src/main/java/com/cafetone/dsp/adapter/`

### Preferences and File Management (Not used in cafetone)
- Most files in `/app/app/src/main/java/com/cafetone/dsp/preference/`
- `/app/app/src/main/java/com/cafetone/dsp/backup/`
- `/app/app/src/main/java/com/cafetone/dsp/editor/`

### LiveProg Integration (Not used in cafetone)
- All files in `/app/app/src/main/java/com/cafetone/dsp/liveprog/`

### Other Unused Features
- `/app/app/src/main/java/com/cafetone/dsp/delegates/`
- Root-specific files (keeping only rootless for cafetone)

## Total Estimate
- Current files: ~176 Kotlin files
- Essential files: ~25-30 files
- Files to remove: ~140-150 files