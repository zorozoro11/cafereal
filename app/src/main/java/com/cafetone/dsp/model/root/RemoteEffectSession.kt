package com.cafetone.dsp.model.root

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import com.cafetone.dsp.interop.JamesDspRemoteEngine
import com.cafetone.dsp.model.IEffectSession

data class RemoteEffectSession(
    override var packageName: String,
    override var uid: Int,
    var effect: JamesDspRemoteEngine?
) : CoroutineScope by CoroutineScope(Dispatchers.Default), IEffectSession {
    override fun toString(): String {
        return "package=$packageName; uid=$uid"
    }
}