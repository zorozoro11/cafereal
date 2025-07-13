package com.cafetone.dsp.ui

import android.animation.ValueAnimator
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.getSystemService

/**
 * Phase 4: UI Enhancement - Animation and Feedback Helper
 * 
 * Provides smooth animations, haptic feedback, and UI transitions
 */
object UIAnimationHelper {
    
    /**
     * Animate slider feedback with scale effect
     */
    fun animateSliderFeedback(view: View, callback: (() -> Unit)? = null) {
        val scaleAnimator = ValueAnimator.ofFloat(1f, 1.05f, 1f)
        scaleAnimator.duration = 150 // 150ms as specified in Phase 4
        scaleAnimator.interpolator = AccelerateDecelerateInterpolator()
        
        scaleAnimator.addUpdateListener { animation ->
            val scale = animation.animatedValue as Float
            view.scaleX = scale
            view.scaleY = scale
        }
        
        scaleAnimator.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                callback?.invoke()
            }
        })
        
        scaleAnimator.start()
    }
    
    /**
     * Animate button press with scale and alpha effect
     */
    fun animateButtonPress(view: View, callback: (() -> Unit)? = null) {
        val scaleDown = ValueAnimator.ofFloat(1f, 0.95f)
        scaleDown.duration = 50
        
        val scaleUp = ValueAnimator.ofFloat(0.95f, 1f)
        scaleUp.duration = 100
        scaleUp.startDelay = 50
        
        scaleDown.addUpdateListener { animation ->
            val scale = animation.animatedValue as Float
            view.scaleX = scale
            view.scaleY = scale
            view.alpha = 0.8f + (scale - 0.95f) * 4f // Subtle alpha change
        }
        
        scaleUp.addUpdateListener { animation ->
            val scale = animation.animatedValue as Float
            view.scaleX = scale
            view.scaleY = scale
            view.alpha = 0.8f + (scale - 0.95f) * 4f
        }
        
        scaleUp.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                view.alpha = 1f
                callback?.invoke()
            }
        })
        
        scaleDown.start()
        scaleUp.start()
    }
    
    /**
     * Animate card appearance with slide and fade
     */
    fun animateCardAppearance(view: View, delay: Long = 0) {
        view.alpha = 0f
        view.translationY = 50f
        
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setStartDelay(delay)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }
    
    /**
     * Animate preset selection with highlight effect
     */
    fun animatePresetSelection(selectedView: View, otherViews: List<View>) {
        // Animate selected view
        selectedView.animate()
            .scaleX(1.05f)
            .scaleY(1.05f)
            .setDuration(200)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
        
        // Animate other views back to normal
        otherViews.forEach { view ->
            if (view != selectedView) {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }
        }
    }
    
    /**
     * Provide haptic feedback with 10ms duration as specified
     */
    fun provideHapticFeedback(context: Context, type: HapticType = HapticType.LIGHT) {
        val vibrator = context.getSystemService<Vibrator>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = when (type) {
                HapticType.LIGHT -> VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE)
                HapticType.MEDIUM -> VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE)
                HapticType.STRONG -> VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
            }
            vibrator?.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(when (type) {
                HapticType.LIGHT -> 10
                HapticType.MEDIUM -> 20
                HapticType.STRONG -> 50
            })
        }
    }
    
    /**
     * Animate value changes with smooth transitions
     */
    fun animateValueChange(
        fromValue: Float,
        toValue: Float,
        duration: Long = 200,
        callback: (Float) -> Unit
    ) {
        val animator = ValueAnimator.ofFloat(fromValue, toValue)
        animator.duration = duration
        animator.interpolator = AccelerateDecelerateInterpolator()
        
        animator.addUpdateListener { animation ->
            callback(animation.animatedValue as Float)
        }
        
        animator.start()
    }
    
    /**
     * Create pulse animation for processing indicators
     */
    fun createPulseAnimation(view: View, minScale: Float = 0.95f, maxScale: Float = 1.05f): ValueAnimator {
        val animator = ValueAnimator.ofFloat(minScale, maxScale)
        animator.duration = 1500 // 1.5 second pulse as specified
        animator.repeatCount = ValueAnimator.INFINITE
        animator.repeatMode = ValueAnimator.REVERSE
        animator.interpolator = AccelerateDecelerateInterpolator()
        
        animator.addUpdateListener { animation ->
            val scale = animation.animatedValue as Float
            view.scaleX = scale
            view.scaleY = scale
        }
        
        return animator
    }
    
    /**
     * Fade transition between views
     */
    fun fadeTransition(fromView: View, toView: View, duration: Long = 300) {
        fromView.animate()
            .alpha(0f)
            .setDuration(duration / 2)
            .withEndAction {
                fromView.visibility = View.GONE
                toView.visibility = View.VISIBLE
                toView.alpha = 0f
                toView.animate()
                    .alpha(1f)
                    .setDuration(duration / 2)
                    .start()
            }
            .start()
    }
    
    /**
     * Types of haptic feedback
     */
    enum class HapticType {
        LIGHT,
        MEDIUM,
        STRONG
    }
}