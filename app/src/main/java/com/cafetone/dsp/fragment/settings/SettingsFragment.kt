package com.cafetone.dsp.fragment.settings

import android.os.Bundle
import androidx.preference.Preference
import com.cafetone.dsp.R
import com.cafetone.dsp.utils.isPlugin
import com.cafetone.dsp.utils.isRootless


class SettingsFragment : SettingsBaseFragment() {

    private val processing by lazy { findPreference<Preference>(getString(R.string.key_audio_format)) }
    private val troubleshooting by lazy { findPreference<Preference>(getString(R.string.key_troubleshooting)) }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.app_preferences, rootKey)

        processing?.summary = getString(
            when {
                isRootless() -> R.string.audio_format_summary
                isPlugin() -> R.string.audio_format_summary_plugin
                else -> R.string.audio_format_summary_root
            }
        )
        troubleshooting?.isVisible = isRootless()
    }

    companion object {
        fun newInstance(): SettingsFragment {
            return SettingsFragment()
        }
    }
}