package com.lightphone.bible.util

/**
 * Decodes Robinson-style morphology codes (e.g. "N-NSM", "V-2AAI-3S") into readable English.
 */
object MorphologyDecoder {
    fun decode(code: String?): String {
        if (code.isNullOrBlank()) return ""
        val parts = code.split("-", limit = 2)
        val pos = decodePartOfSpeech(parts[0])
        val detail = if (parts.size > 1) decodeDetails(parts[1]) else ""
        return if (detail.isEmpty()) pos else "$pos, $detail"
    }

    private fun decodePartOfSpeech(code: String): String = when (code) {
        "N" -> "noun"
        "V" -> "verb"
        "A" -> "adjective"
        "C" -> "conjunction"
        "D" -> "adverb"
        "P" -> "preposition"
        "RA", "R" -> "article"
        "I" -> "interjection"
        "X" -> "particle"
        "T" -> "definite article"
        else -> code.lowercase()
    }

    private fun decodeDetails(code: String): String {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < code.length) {
            when {
                code.startsWith("2", i) -> {
                    tokens += "2nd person"
                    i += 1
                }
                code.startsWith("3", i) -> {
                    tokens += "3rd person"
                    i += 1
                }
                code.startsWith("1", i) -> {
                    tokens += "1st person"
                    i += 1
                }
                code.startsWith("AA", i) -> {
                    tokens += "aorist active"
                    i += 2
                }
                code.startsWith("PA", i) -> {
                    tokens += "present active"
                    i += 2
                }
                code.startsWith("FA", i) -> {
                    tokens += "future active"
                    i += 2
                }
                code.startsWith("PP", i) -> {
                    tokens += "perfect passive"
                    i += 2
                }
                code.startsWith("AP", i) -> {
                    tokens += "aorist passive"
                    i += 2
                }
                code.startsWith("MP", i) -> {
                    tokens += "middle/passive"
                    i += 2
                }
                code.startsWith("S", i) -> {
                    tokens += "singular"
                    i += 1
                }
                code.startsWith("P", i) -> {
                    tokens += "plural"
                    i += 1
                }
                code.startsWith("M", i) -> {
                    tokens += "masculine"
                    i += 1
                }
                code.startsWith("F", i) -> {
                    tokens += "feminine"
                    i += 1
                }
                code.startsWith("N", i) -> {
                    tokens += "neuter"
                    i += 1
                }
                code.startsWith("G", i) -> {
                    tokens += "genitive"
                    i += 1
                }
                code.startsWith("D", i) -> {
                    tokens += "dative"
                    i += 1
                }
                code.startsWith("A", i) -> {
                    tokens += "accusative"
                    i += 1
                }
                code.startsWith("V", i) -> {
                    tokens += "vocative"
                    i += 1
                }
                code.startsWith("I", i) -> {
                    tokens += "imperative"
                    i += 1
                }
                code.startsWith("P", i) -> {
                    tokens += "participle"
                    i += 1
                }
                else -> i += 1
            }
        }
        return tokens.joinToString(", ")
    }
}
