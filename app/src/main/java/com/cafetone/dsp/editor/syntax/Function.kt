package com.cafetone.dsp.editor.syntax

import com.amrdeveloper.codeview.Code

class Function(private val title: String, private val prefix: String = title) : Code {
    override val codeTitle: String
        get() = title
    override val codePrefix: String
        get() = prefix
    override val codeBody: String
        get() = prefix
}