package com.lightphone.bible.data.model

data class BibleReference(
    val translationId: String,
    val bookId: String,
    val chapter: Int,
    val verse: Int,
) {
    fun format(): String = "$translationId:$bookId.$chapter.$verse"

    companion object {
        private val REFERENCE_REGEX =
            Regex("""^(?<translation>[A-Za-z0-9]+):(?<book>[A-Z0-9]{2,3})\.(?<chapter>\d+)\.(?<verse>\d+)$""")

        fun parse(raw: String): BibleReference? {
            val match = REFERENCE_REGEX.matchEntire(raw.trim()) ?: return null
            return BibleReference(
                translationId = match.groups["translation"]!!.value,
                bookId = match.groups["book"]!!.value,
                chapter = match.groups["chapter"]!!.value.toInt(),
                verse = match.groups["verse"]!!.value.toInt(),
            )
        }

        fun parseOrThrow(raw: String): BibleReference =
            parse(raw) ?: throw IllegalArgumentException("Invalid Bible reference: $raw")

        private val CANONICAL_REGEX =
            Regex("""^(?<book>[A-Z0-9]{2,3})\.(?<chapter>\d+)(?:\.(?<verse>\d+))?$""")

        /** Parses canonical refs such as `GEN.1` or `MAT.5.3` without a translation prefix. */
        fun parseCanonical(
            raw: String,
            translationId: String = "BSB",
        ): BibleReference? {
            val match = CANONICAL_REGEX.matchEntire(raw.trim()) ?: return null
            return BibleReference(
                translationId = translationId,
                bookId = match.groups["book"]!!.value,
                chapter = match.groups["chapter"]!!.value.toInt(),
                verse = match.groups["verse"]?.value?.toInt() ?: 1,
            )
        }
    }
}

data class BibleBook(
    val id: String,
    val name: String,
    val testament: Testament,
    val order: Int,
)

enum class Testament {
    OLD,
    NEW,
}

object BibleBooks {
    val oldTestament: List<BibleBook> = listOf(
        BibleBook("GEN", "Genesis", Testament.OLD, 1),
        BibleBook("EXO", "Exodus", Testament.OLD, 2),
        BibleBook("LEV", "Leviticus", Testament.OLD, 3),
        BibleBook("NUM", "Numbers", Testament.OLD, 4),
        BibleBook("DEU", "Deuteronomy", Testament.OLD, 5),
        BibleBook("JOS", "Joshua", Testament.OLD, 6),
        BibleBook("JDG", "Judges", Testament.OLD, 7),
        BibleBook("RUT", "Ruth", Testament.OLD, 8),
        BibleBook("1SA", "1 Samuel", Testament.OLD, 9),
        BibleBook("2SA", "2 Samuel", Testament.OLD, 10),
        BibleBook("1KI", "1 Kings", Testament.OLD, 11),
        BibleBook("2KI", "2 Kings", Testament.OLD, 12),
        BibleBook("1CH", "1 Chronicles", Testament.OLD, 13),
        BibleBook("2CH", "2 Chronicles", Testament.OLD, 14),
        BibleBook("EZR", "Ezra", Testament.OLD, 15),
        BibleBook("NEH", "Nehemiah", Testament.OLD, 16),
        BibleBook("EST", "Esther", Testament.OLD, 17),
        BibleBook("JOB", "Job", Testament.OLD, 18),
        BibleBook("PSA", "Psalms", Testament.OLD, 19),
        BibleBook("PRO", "Proverbs", Testament.OLD, 20),
        BibleBook("ECC", "Ecclesiastes", Testament.OLD, 21),
        BibleBook("SNG", "Song of Solomon", Testament.OLD, 22),
        BibleBook("ISA", "Isaiah", Testament.OLD, 23),
        BibleBook("JER", "Jeremiah", Testament.OLD, 24),
        BibleBook("LAM", "Lamentations", Testament.OLD, 25),
        BibleBook("EZK", "Ezekiel", Testament.OLD, 26),
        BibleBook("DAN", "Daniel", Testament.OLD, 27),
        BibleBook("HOS", "Hosea", Testament.OLD, 28),
        BibleBook("JOL", "Joel", Testament.OLD, 29),
        BibleBook("AMO", "Amos", Testament.OLD, 30),
        BibleBook("OBA", "Obadiah", Testament.OLD, 31),
        BibleBook("JON", "Jonah", Testament.OLD, 32),
        BibleBook("MIC", "Micah", Testament.OLD, 33),
        BibleBook("NAM", "Nahum", Testament.OLD, 34),
        BibleBook("HAB", "Habakkuk", Testament.OLD, 35),
        BibleBook("ZEP", "Zephaniah", Testament.OLD, 36),
        BibleBook("HAG", "Haggai", Testament.OLD, 37),
        BibleBook("ZEC", "Zechariah", Testament.OLD, 38),
        BibleBook("MAL", "Malachi", Testament.OLD, 39),
    )

    val newTestament: List<BibleBook> = listOf(
        BibleBook("MAT", "Matthew", Testament.NEW, 40),
        BibleBook("MRK", "Mark", Testament.NEW, 41),
        BibleBook("LUK", "Luke", Testament.NEW, 42),
        BibleBook("JHN", "John", Testament.NEW, 43),
        BibleBook("ACT", "Acts", Testament.NEW, 44),
        BibleBook("ROM", "Romans", Testament.NEW, 45),
        BibleBook("1CO", "1 Corinthians", Testament.NEW, 46),
        BibleBook("2CO", "2 Corinthians", Testament.NEW, 47),
        BibleBook("GAL", "Galatians", Testament.NEW, 48),
        BibleBook("EPH", "Ephesians", Testament.NEW, 49),
        BibleBook("PHP", "Philippians", Testament.NEW, 50),
        BibleBook("COL", "Colossians", Testament.NEW, 51),
        BibleBook("1TH", "1 Thessalonians", Testament.NEW, 52),
        BibleBook("2TH", "2 Thessalonians", Testament.NEW, 53),
        BibleBook("1TI", "1 Timothy", Testament.NEW, 54),
        BibleBook("2TI", "2 Timothy", Testament.NEW, 55),
        BibleBook("TIT", "Titus", Testament.NEW, 56),
        BibleBook("PHM", "Philemon", Testament.NEW, 57),
        BibleBook("HEB", "Hebrews", Testament.NEW, 58),
        BibleBook("JAS", "James", Testament.NEW, 59),
        BibleBook("1PE", "1 Peter", Testament.NEW, 60),
        BibleBook("2PE", "2 Peter", Testament.NEW, 61),
        BibleBook("1JN", "1 John", Testament.NEW, 62),
        BibleBook("2JN", "2 John", Testament.NEW, 63),
        BibleBook("3JN", "3 John", Testament.NEW, 64),
        BibleBook("JUD", "Jude", Testament.NEW, 65),
        BibleBook("REV", "Revelation", Testament.NEW, 66),
    )

    val all: List<BibleBook> = oldTestament + newTestament

    private val byId: Map<String, BibleBook> = all.associateBy { it.id }

    fun find(bookId: String): BibleBook? = byId[bookId]

    fun require(bookId: String): BibleBook =
        find(bookId) ?: throw IllegalArgumentException("Unknown book id: $bookId")

    fun chapterCount(bookId: String): Int =
        CHAPTER_COUNTS[bookId] ?: find(bookId)?.let { 1 } ?: 1

    private val CHAPTER_COUNTS: Map<String, Int> = mapOf(
        "GEN" to 50, "EXO" to 40, "LEV" to 27, "NUM" to 36, "DEU" to 34,
        "JOS" to 24, "JDG" to 21, "RUT" to 4, "1SA" to 31, "2SA" to 24,
        "1KI" to 22, "2KI" to 25, "1CH" to 29, "2CH" to 36, "EZR" to 10,
        "NEH" to 13, "EST" to 10, "JOB" to 42, "PSA" to 150, "PRO" to 31,
        "ECC" to 12, "SNG" to 8, "ISA" to 66, "JER" to 52, "LAM" to 5,
        "EZK" to 48, "DAN" to 12, "HOS" to 14, "JOL" to 3, "AMO" to 9,
        "OBA" to 1, "JON" to 4, "MIC" to 7, "NAM" to 3, "HAB" to 3,
        "ZEP" to 3, "HAG" to 2, "ZEC" to 14, "MAL" to 4,
        "MAT" to 28, "MRK" to 16, "LUK" to 24, "JHN" to 21, "ACT" to 28,
        "ROM" to 16, "1CO" to 16, "2CO" to 13, "GAL" to 6, "EPH" to 6,
        "PHP" to 4, "COL" to 4, "1TH" to 5, "2TH" to 3, "1TI" to 6,
        "2TI" to 4, "TIT" to 3, "PHM" to 1, "HEB" to 13, "JAS" to 5,
        "1PE" to 5, "2PE" to 3, "1JN" to 5, "2JN" to 1, "3JN" to 1,
        "JUD" to 1, "REV" to 22,
    )
}
