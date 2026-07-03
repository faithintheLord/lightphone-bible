package com.lightphone.bible.util

import com.lightphone.bible.data.model.BibleReference
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object VerseOfDay {
    private val canonicalVerses = listOf(
        "JHN.3.16", "PSA.23.1", "ROM.8.28", "PHP.4.13", "PRO.3.5", "ISA.41.10",
        "JER.29.11", "MAT.6.33", "JOS.1.9", "PSA.46.1", "ROM.12.2", "GAL.5.22",
        "EPH.2.8", "HEB.11.1", "1CO.13.4", "2TI.1.7", "PSA.119.105", "ISA.40.31",
        "MAT.11.28", "JHN.14.6", "ROM.5.8", "1JN.4.19", "PSA.27.1", "PRO.18.10",
        "MIC.6.8", "MAT.5.16", "LUK.6.31", "ACT.1.8", "COL.3.23", "1PE.5.7",
        "PSA.37.4", "ISA.55.8", "JER.33.3", "MAT.28.20", "MRK.10.27", "LUK.1.37",
        "JHN.1.1", "ROM.1.16", "1CO.10.13", "2CO.5.17", "GAL.2.20", "EPH.6.11",
        "PHP.4.6", "COL.1.17", "1TH.5.16", "2TH.3.3", "1TI.4.12", "2TI.3.16",
        "TIT.3.5", "HEB.13.8", "JAS.1.5", "1PE.2.9", "2PE.1.3", "1JN.1.9",
        "REV.21.4", "GEN.1.1", "DEU.6.5", "PSA.1.1", "PSA.19.1", "PSA.51.10",
        "PSA.91.1", "PSA.100.4", "PSA.103.1", "PSA.139.14", "PRO.4.23", "ECC.3.1",
        "ISA.9.6", "ISA.53.5", "DAN.3.17", "JOL.2.28", "AMO.5.24", "ZEC.4.6",
        "MAL.3.10", "MAT.5.3", "MAT.6.9", "MAT.7.7", "MAT.16.24", "MAT.22.37",
        "MRK.9.23", "LUK.2.11", "LUK.15.7", "LUK.18.27", "JHN.8.32", "JHN.10.10",
        "JHN.15.13", "ACT.4.12", "ROM.3.23", "ROM.6.23", "ROM.8.1", "ROM.10.9",
        "ROM.15.13", "1CO.1.18", "1CO.15.57", "2CO.12.9", "GAL.5.1", "EPH.4.32",
        "PHP.2.3", "COL.3.2", "1TH.5.17", "1TI.6.12", "2TI.2.15", "HEB.4.12",
        "HEB.12.2", "JAS.4.7", "1PE.3.15", "1JN.3.1", "REV.3.20", "NUM.6.24",
        "EXO.14.14", "JDG.6.12", "1SA.16.7", "2CH.7.14", "NEH.8.10", "JOB.19.25",
        "PSA.34.8", "PSA.55.22", "PSA.62.1", "PSA.84.11", "PSA.121.1", "PSA.145.18",
        "PRO.16.3", "PRO.22.6", "ECC.12.13", "SNG.2.4", "ISA.26.3", "ISA.43.1",
        "ISA.61.1", "LAM.3.22", "EZK.36.26", "HOS.6.6", "JON.2.2", "NAH.1.7",
        "HAB.2.4", "ZEP.3.17", "HAG.2.4", "ZEC.9.9", "MAL.4.2", "MAT.4.4",
        "MAT.5.14", "MAT.7.12", "MAT.10.29", "MAT.18.20", "MAT.19.26", "MAT.24.35",
        "MRK.1.15", "MRK.11.24", "LUK.6.38", "LUK.11.9", "LUK.12.7", "LUK.19.10",
        "JHN.3.30", "JHN.6.35", "JHN.11.25", "JHN.16.33", "ACT.2.38", "ACT.16.31",
        "ROM.8.18", "ROM.8.38", "ROM.11.33", "ROM.14.8", "1CO.6.19", "1CO.15.58",
        "2CO.4.17", "2CO.9.8", "GAL.6.9", "EPH.1.7", "EPH.3.20", "EPH.5.1",
        "PHP.1.6", "PHP.4.19", "COL.2.6", "1TH.4.3", "2TI.1.9", "HEB.10.23",
        "HEB.11.6", "JAS.1.17", "JAS.5.16", "1PE.1.3", "1PE.5.10", "2PE.3.9",
        "1JN.4.8", "1JN.5.14", "REV.1.8", "REV.22.13",
    )

    fun referenceFor(date: LocalDate = LocalDate.now(), translationId: String = "BSB"): BibleReference {
        val dayIndex = ChronoUnit.DAYS.between(LocalDate.of(date.year, 1, 1), date).toInt()
        val canonical = canonicalVerses[dayIndex % canonicalVerses.size]
        return BibleReference.parseCanonical(canonical, translationId)
            ?: BibleReference(translationId, "JHN", 3, 16)
    }
}
