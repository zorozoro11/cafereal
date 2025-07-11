package com.cafetone.dsp.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.transition.MaterialSharedAxis
import com.cafetone.dsp.databinding.OnboardingPage2Binding
import com.cafetone.dsp.utils.extensions.ContextExtensions.dpToPx

class LimitationsFragment : Fragment() {
    private lateinit var binding: OnboardingPage2Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        layoutInflater: LayoutInflater,
        viewGroup: ViewGroup?,
        bundle: Bundle?
    ): View {
        val padding = 16.dpToPx
        binding = OnboardingPage2Binding.inflate(layoutInflater, viewGroup, false)
        binding.root.setPadding(padding, 0, padding, padding)
        binding.header.isVisible = false
        binding.notice.isVisible = false
        return binding.root
    }
}