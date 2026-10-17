package com.lightphone.bible.data.catalog

import com.lightphone.bible.data.model.TranslationCatalog
import com.lightphone.bible.data.model.TranslationEntry
import com.lightphone.bible.data.provider.BibleContentProvider
import com.lightphone.bible.data.provider.BsbDataOutputInterlinearProvider
import com.lightphone.bible.data.provider.HelloAoContentProvider
import com.lightphone.bible.data.provider.InterlinearProvider
import kotlinx.serialization.json.Json
import com.thelightphone.sdk.SealedLightContext

class TranslationRegistry(
    private val context: SealedLightContext,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private val contentProviders: Map<String, BibleContentProvider> = mapOf(
        "helloao" to HelloAoContentProvider(),
    )

    private val interlinearProviders: Map<String, InterlinearProvider> = mapOf(
        "bsb-data-output" to BsbDataOutputInterlinearProvider(),
    )

    private val loadedCatalog: TranslationCatalog by lazy { loadCatalog() }

    fun getCatalog(): TranslationCatalog = loadedCatalog

    fun getTranslation(id: String): TranslationEntry? =
        loadedCatalog.translations.find { it.id == id }

    fun contentProviderFor(entry: TranslationEntry): BibleContentProvider? =
        contentProviders[entry.provider]

    fun interlinearProviderFor(entry: TranslationEntry): InterlinearProvider? =
        entry.interlinearProvider?.let { interlinearProviders[it] }

    fun defaultTranslations(): List<TranslationEntry> =
        loadedCatalog.translations.filter { it.enabledByDefault }

    fun translationsToDownloadOnLaunch(): List<TranslationEntry> =
        loadedCatalog.translations.filter { it.downloadOnFirstLaunch }

    private fun loadCatalog(): TranslationCatalog {
        val text = context.readAsset(CATALOG_ASSET).decodeToString()
        return json.decodeFromString<TranslationCatalog>(text)
    }

    companion object {
        const val CATALOG_ASSET = "translations/catalog.json"
    }
}
