package com.cafetone.dsp.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.cafetone.dsp.interop.JamesDspLocalEngine

abstract class BaseAudioProcessorService : Service() {
    private val binder: IBinder = LocalBinder()

    inner class LocalBinder : Binder() {
        val service: BaseAudioProcessorService
            get() = this@BaseAudioProcessorService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        activeServices++
        super.onCreate()
    }

    override fun onDestroy() {
        activeServices--
        super.onDestroy()
    }
    
    // Abstract method for subclasses to provide DSP engine access
    abstract fun getDspEngine(): JamesDspLocalEngine?

    companion object {
        var activeServices: Int = 0
            private set
    }
}
