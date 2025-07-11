package com.cafetone.dsp.session.root

import android.content.*
import com.cafetone.dsp.session.dump.DumpManager
import com.cafetone.dsp.session.dump.data.ISessionInfoDump
import com.cafetone.dsp.session.shared.BaseSessionManager


class RootSessionDumpManager(context: Context) : BaseSessionManager(context)
{
    private var onSessionDump: ((sessionDump: ISessionInfoDump) -> Unit)? = null
    private var onDumpMethodChanged: (() -> Unit)? = null

    fun setOnSessionDump(callback: ((sessionDump: ISessionInfoDump) -> Unit)?) {
        onSessionDump = callback
    }

    fun setOnDumpMethodChanged(callback: (() -> Unit)?) {
        onDumpMethodChanged = callback
    }

    override fun handleSessionDump(sessionDump: ISessionInfoDump?) {
        sessionDump?.let { onSessionDump?.invoke(it) }
    }

    override fun onDumpMethodChange(method: DumpManager.Method) {
        onDumpMethodChanged?.invoke()
        super.onDumpMethodChange(method)
    }
}