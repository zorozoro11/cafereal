package com.cafetone.dsp.session.dump.data

import com.cafetone.dsp.model.AppInfo

data class PackageServiceDump(val apps: List<AppInfo>) : IDump {
    override fun toString(): String {
        val sb = StringBuilder("\n--> Apps\n")
        apps.forEach(sb::append)
        return sb.toString()
    }
}