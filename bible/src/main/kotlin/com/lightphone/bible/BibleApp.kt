package com.lightphone.bible

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.lightphone.bible.data.catalog.TranslationRegistry
import com.lightphone.bible.data.db.BibleDatabase
import com.lightphone.bible.repository.BibleContentRepository
import com.lightphone.bible.repository.BiblePreferences
import com.lightphone.bible.repository.BibleSyncState
import com.lightphone.bible.repository.InterlinearRepository
import com.lightphone.bible.repository.PlansRepository
import com.lightphone.bible.repository.SearchRepository
import com.lightphone.bible.repository.StrongsRepository
import com.lightphone.bible.repository.UserDataRepository
import com.thelightphone.sdk.LightWork
import com.thelightphone.sdk.SealedLightContext
import com.thelightphone.sdk.buildDatabase

class BibleApp private constructor(
    val database: BibleDatabase,
    val registry: TranslationRegistry,
    val preferences: BiblePreferences,
    val contentRepository: BibleContentRepository,
    val userDataRepository: UserDataRepository,
    val searchRepository: SearchRepository,
    val strongsRepository: StrongsRepository,
    val interlinearRepository: InterlinearRepository,
    val plansRepository: PlansRepository,
) {
    companion object {
        const val DATABASE_NAME = "bible.db"
        const val SYNC_JOB_KEY = "bible-sync"

        @Volatile
        private var instance: BibleApp? = null

        @Volatile
        private var startupSyncPending: Boolean = false

        fun get(): BibleApp =
            instance ?: error("BibleApp not initialized. Call ensureInitialized() first.")

        fun getOrNull(): BibleApp? = instance

        fun requestStartupSync() {
            startupSyncPending = true
        }

        fun ensureInitialized(lightContext: SealedLightContext): BibleApp {
            val app = instance ?: synchronized(this) {
                instance ?: create(lightContext, lightContext.dataStore).also { instance = it }
            }
            if (startupSyncPending) {
                startupSyncPending = false
                enqueueSyncIfNeeded(lightContext)
            }
            return app
        }

        fun enqueueSyncIfNeeded(lightContext: SealedLightContext, translationId: String? = null) {
            val input = buildMap {
                if (translationId != null) put("translationId", translationId)
            }
            LightWork.enqueue(lightContext, SYNC_JOB_KEY, input)
        }

        private fun create(
            lightContext: SealedLightContext,
            dataStore: DataStore<Preferences>,
        ): BibleApp {
            val appContext = lightContext.applicationContext
            val database = lightContext.buildDatabase(
                BibleDatabase::class.java,
                DATABASE_NAME,
            )
            val registry = TranslationRegistry(appContext)
            val preferences = BiblePreferences(dataStore)
            return BibleApp(
                database = database,
                registry = registry,
                preferences = preferences,
                contentRepository = BibleContentRepository(database, registry),
                userDataRepository = UserDataRepository(database),
                searchRepository = SearchRepository(database),
                strongsRepository = StrongsRepository(appContext, database),
                interlinearRepository = InterlinearRepository(database, registry),
                plansRepository = PlansRepository(appContext, database),
            )
        }
    }

    suspend fun needsSync(): Boolean {
        contentRepository.ensureCatalogInDatabase()
        val catalog = registry.translationsToDownloadOnLaunch()
        if (catalog.isEmpty()) return false
        val translations = contentRepository.getTranslations()
        val missingDownload = catalog.any { entry ->
            translations.find { it.id == entry.id }?.isDownloaded != true
        }
        if (missingDownload) return true
        val bsb = catalog.find { it.id == "BSB" && it.supportsWordStudy }
        return bsb != null && !interlinearRepository.hasInterlinear(bsb.id)
    }

    suspend fun markSyncIdleIfComplete() {
        if (!needsSync()) {
            preferences.setSyncState(BibleSyncState.COMPLETE)
        }
    }
}
