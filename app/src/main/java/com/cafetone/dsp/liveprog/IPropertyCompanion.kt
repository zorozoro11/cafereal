package com.cafetone.dsp.liveprog

interface IPropertyCompanion {
    val definitionRegex: Regex
    fun parse(line: String, contents: String): EelBaseProperty?
}