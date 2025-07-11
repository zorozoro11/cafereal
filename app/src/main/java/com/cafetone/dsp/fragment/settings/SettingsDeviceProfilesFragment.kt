package com.cafetone.dsp.fragment.settings

import android.os.Bundle
import androidx.preference.Preference
import com.cafetone.dsp.R
import com.cafetone.dsp.utils.Constants
import com.cafetone.dsp.utils.extensions.ContextExtensions.showAlert

class SettingsDeviceProfilesFragment : SettingsBaseFragment() {

    private val profilesInfo by lazy { findPreference<Preference>(getString(R.string.key_device_profiles_info)) }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = Constants.PREF_APP
        setPreferencesFromResource(R.xml.app_device_profiles_preferences, rootKey)

        profilesInfo?.setOnPreferenceClickListener {
            context?.showAlert(
                R.string.profiles_info_title,
                R.string.profiles_info_content
            )
            true
        }
    }

    companion object {
        fun newInstance(): SettingsDeviceProfilesFragment {
            return SettingsDeviceProfilesFragment()
        }
    }
}