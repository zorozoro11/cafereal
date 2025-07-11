package com.cafetone.dsp.receiver

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import com.cafetone.dsp.BuildConfig
import com.cafetone.dsp.service.RootAudioProcessorService
import com.cafetone.dsp.session.rootless.RootlessSessionDatabase
import com.cafetone.dsp.utils.Constants
import com.cafetone.dsp.utils.extensions.ContextExtensions.sendLocalBroadcast
import com.cafetone.dsp.utils.isPlugin
import com.cafetone.dsp.utils.isRoot
import timber.log.Timber

class SessionReceiver : BroadcastReceiver() {

    @SuppressLint("BinaryOperationInTimber")
    override fun onReceive(context: Context?, intent: Intent?) {
        if (isPlugin() || intent == null || context == null) {
            return
        }

        if(intent.getIntExtra(RootlessSessionDatabase.EXTRA_IGNORE, 0) == 1) {
            Timber.d("Control close intent ignored")
            return
        }

        Timber.i(
            "Action: ${intent.action}; " +
                    "session: ${intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, AudioEffect.ERROR)}; " +
                    "package ${intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME)}")

        context.sendLocalBroadcast(
            Intent(Constants.ACTION_SESSION_CHANGED)
                .apply {
                    putExtras(intent)
                }
        )

        if(isRoot()) {
            Intent(context, RootAudioProcessorService::class.java)
                .apply { this.action = intent.action }
                .apply { putExtras(intent) }
                .run { RootAudioProcessorService.startService(context, this) }
        }
    }
}