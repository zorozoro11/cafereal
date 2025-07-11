package com.cafetone.dsp.session.dump.data

import com.cafetone.dsp.model.AudioSessionDumpEntry

data class AudioServiceDump(override val sessions: HashMap<Int /* sid */, AudioSessionDumpEntry>) :
    ISessionInfoDump
{
    override fun toString(): String {
        val sb = StringBuilder("\n--> Session stack\n")
        sessions.forEach { (key, value) ->
            sb.append("sid=$key\t-> $value\n")
        }
        return sb.toString()
    }
}