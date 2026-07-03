package com.lightphone.bible.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TranslationCatalog(
    val translations: List<TranslationEntry> = emptyList(),
)

@Serializable
data class TranslationEntry(
    val id: String,
    val shortName: String,
    val displayName: String,
    val provider: String,
    val providerTranslationId: String,
    val language: String,
    val licenseName: String,
    val licenseUrl: String,
    val attribution: String,
    val enabledByDefault: Boolean = false,
    val downloadOnFirstLaunch: Boolean = false,
    val supportsWordStudy: Boolean = false,
    val interlinearProvider: String? = null,
)
