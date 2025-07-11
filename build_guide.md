# Cafetone Build Guide

## Project Status: IN_PROGRESS
Last Updated: March 2025

## Project Overview
Transforming **RootlessJamesDSP** into **Cafetone** - a specialized audio processing app that replicates Sony's "Background Listening Mode" café effect.

**Original Source**: RootlessJamesDSP (https://github.com/timschneeb/RootlessJamesDSP.git)  
**Target**: Simplified café mode app with 3 controls (Intensity, Spatial Width, Distance)

## Completed Tasks:
- [x] Initial RootlessJamesDSP repository analysis
- [x] Codebase structure mapping
- [x] Architecture analysis and DSP component identification
- [x] Build guide creation
- [x] Updated app branding (name, package, icons)
- [x] Simplified build configuration (removed complex flavors)
- [x] Updated AndroidConfig for Cafetone
- [x] Updated gradle dependencies
- [x] Created café mode MainActivity
- [x] Created café mode UI layout
- [x] Added string resources for café mode
- [x] Updated AndroidManifest for Cafetone
- [ ] DSP algorithm implementation
- [ ] Café mode testing
- [ ] Final integration and testing

## Current Phase: Phase 1 - Project Setup & Build Guide Creation
**What's Done:**
- Analyzed complete RootlessJamesDSP codebase structure
- Identified key DSP components suitable for café mode
- Mapped existing DSP functions to café mode requirements
- Created comprehensive build guide
- **Updated app branding**: Changed app name to "Cafetone", package to "com.cafetone.dsp"
- **Simplified build configuration**: Removed complex flavor system, kept only essential variants
- **Updated AndroidConfig**: Reset version to 1.0.0, maintained rootless focus
- **Streamlined dependencies**: Removed unused dependencies (Room, Retrofit, etc.)
- **Updated gradle namespace**: Changed from "me.timschneeberger.rootlessjamesdsp" to "com.cafetone.dsp"

**What's Working:**
- Original RootlessJamesDSP app architecture identified
- Build configuration updated for Cafetone
- Identified perfect DSP building blocks:
  - Convolver (for café space acoustics)
  - Multi-band EQ (for distance frequency shaping)
  - Reverb (for café ambience)
  - Crossfeed (for spatial positioning)
  - Stereo Enhancement (for spatial width)
  - Post Gain (for distance simulation)

**Known Issues:**
- Gradle build is taking long time (common with Android builds)
- Need to test build configuration changes

## Next Steps:
1. **Phase 1 Completion**: Update branding (app name, package, icons)
2. **Phase 2**: DSP algorithm implementation for café mode
3. **Phase 3**: UI simplification to 3-slider interface
4. **Phase 4**: Service architecture streamlining
5. **Phase 5**: Testing and validation

## Architecture Changes:
**Original RootlessJamesDSP components to keep:**
- BaseAudioProcessorService architecture
- RootlessAudioProcessorService (Shizuku integration)
- JamesDspWrapper (C++ JNI bridge)
- Core DSP engine (libjamesdsp)
- Convolver engine (essential for café space simulation)
- Basic EQ, Reverb, Crossfeed, Stereo Enhancement
- Notification system
- Permission handling system

**Components to remove/modify:**
- Complex fragment-based UI → Simple 3-slider interface
- Multiple effect categories → Single café mode
- Preset management system → Fixed café mode
- File management for impulse responses → Built-in café IR
- Advanced settings → Minimal settings
- Multiple build variants → Rootless only
- DDC, VDC, LiveProg, Bass Boost, Tube Simulation, Compander → Remove
- Spectrum analyzer, measurement tools → Remove

**New café mode components to add:**
- Café Mode DSP Algorithm (using existing functions)
- Built-in café impulse response
- Simplified MainActivity with 3 sliders
- Café-specific UI design

## Key Decisions Made:
1. **Keep existing service architecture** - Proven, stable, efficient
2. **Reuse existing DSP functions** - No need to modify C++ core extensively
3. **Focus on rootless variant only** - Most common use case
4. **Use Shizuku for elevated permissions** - Existing proven system
5. **Keep convolver for space simulation** - Essential for café acoustics
6. **Simplify UI drastically** - From complex fragments to 3 sliders

## Files to Modify:
- `app/build.gradle.kts` - Update app name, package, dependencies
- `app/src/main/AndroidManifest.xml` - Update branding and permissions
- `app/src/main/java/me/timschneeberger/rootlessjamesdsp/activity/MainActivity.kt` - Simplify to café mode UI
- `app/src/main/java/me/timschneeberger/rootlessjamesdsp/service/` - Simplify service classes
- `app/src/main/res/` - Update layouts, strings, icons
- `app/src/main/assets/` - Add café impulse response
- `settings.gradle` - Update project name
- Various UI layout files - Simplify to 3-slider interface

## DSP Algorithm Design:
**Café Mode Implementation using existing DSP functions:**

### Three Main Controls:
1. **Intensity (0-100%, default: 70%)** - Overall café effect strength
2. **Spatial Width (0-100%, default: 60%)** - Stereo positioning  
3. **Distance (0-100%, default: 80%)** - Perceived distance simulation

### DSP Processing Chain:
**Distance Simulation** (Distance slider):
- Multi-band EQ with frequency shaping (setMultiEqualizer)
- Reverb mix adjustment (setReverb)
- Post gain reduction (setPostGain)

**Spatial & Positioning** (Spatial Width slider):
- Crossfeed for spatial positioning (setCrossfeed)
- Stereo enhancement for width (setStereoEnhancement)

**Café Space Acoustics** (Intensity slider):
- Convolver with café impulse response (setConvolver)
- Overall wet/dry mix control

## Build Configuration:
**Target Configuration:**
- App Name: "Cafetone"
- Package Name: `com.cafetone.dsp`
- Build Variant: Rootless only
- Min SDK: 29 (Android 10+)
- Target SDK: Latest

## Testing Status:
- [ ] Unit tests for café mode DSP
- [ ] Integration tests for service lifecycle
- [ ] Manual testing of café mode effect
- [ ] Performance testing
- [ ] Battery optimization testing
- [ ] Shizuku integration testing

## Current Development Focus:
**Phase 1 Tasks:**
1. Update app branding (name, package, icons)
2. Simplify build configuration
3. Update manifest and gradle files
4. Prepare for DSP algorithm implementation

## Handoff Notes for Next AI:
- **Critical**: This build guide contains all architectural decisions and progress
- **Architecture**: Keep existing service architecture, only simplify UI and DSP usage
- **DSP Strategy**: Use existing DSP functions, don't modify C++ core
- **UI Strategy**: Replace complex fragments with simple 3-slider interface
- **Build Strategy**: Focus on rootless variant only, keep Shizuku integration
- **Testing Strategy**: Test café mode effect thoroughly, validate service lifecycle

## Development Environment:
- Android Studio IDE
- Kotlin/Java for app logic
- C++ for DSP processing (existing libjamesdsp)
- Gradle build system
- Material Design components
- Shizuku for rootless permissions

---
*Build guide will be updated after each significant change*