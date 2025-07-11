package com.cafetone.dsp.session.dump.data

import com.cafetone.dsp.model.AudioSessionDumpEntry

interface ISessionInfoDump : IDump {
    val sessions: HashMap<Int /* sid */, AudioSessionDumpEntry>
}