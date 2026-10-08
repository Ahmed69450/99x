package com.byd.voiceassistant.ai.sanitizer

// Kotlin extension function for idiomatic usage: text.toSpokenTts()
fun String.toSpokenTts(): String = TtsSanitizer.sanitize(this)
