@file:Suppress("NOTHING_TO_INLINE")
package com.cafetone.dsp.utils

import me.timschneeberger.rootlessjamesdsp.BuildConfig

inline fun isRootless() = BuildConfig.ROOTLESS && !BuildConfig.PLUGIN
inline fun isRoot() = !BuildConfig.ROOTLESS && !BuildConfig.PLUGIN
inline fun isPlugin() = BuildConfig.PLUGIN
