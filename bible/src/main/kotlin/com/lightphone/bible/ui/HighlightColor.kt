package com.lightphone.bible.ui

enum class HighlightColor(val storageKey: String, val argb: Int) {
    YELLOW("yellow", 0x33FFD54F.toInt()),
    GREEN("green", 0x3381C784.toInt()),
    BLUE("blue", 0x3364B5F6.toInt()),
    PINK("pink", 0x33F06292.toInt()),
    ;

    companion object {
        fun fromStorageKey(key: String?): HighlightColor =
            entries.find { it.storageKey == key } ?: YELLOW

        fun fromArgb(argb: Int): HighlightColor =
            entries.find { it.argb == argb } ?: YELLOW
    }
}
