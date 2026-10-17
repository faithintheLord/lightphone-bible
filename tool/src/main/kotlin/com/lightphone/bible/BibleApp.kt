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
import com.thelightphone.sdk.LightJobState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

        private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val enqueueMutex = Mutex()

        fun get(): BibleApp =
            instance ?: error("BibleApp not initialized. Call ensureInitialized() first.")

        fun getOrNull(): BibleApp? = instance

        fun ensureInitialized(lightContext: SealedLightContext): BibleApp {
            return instance ?: synchronized(this) {
                instance ?: create(lightContext, lightContext.dataStore).also { instance = it }
            }
        }

        fun enqueueSyncIfNeeded(
            lightContext: SealedLightContext,
            translationId: String? = null,
            restartPending: Boolean = false,
        ) {
            val input = buildMap {
                if (translationId != null) put("translationId", translationId)
            }
            syncScope.launch {
                enqueueMutex.withLock {
                    val state = LightWork.getState(lightContext, SYNC_JOB_KEY)
                    // SDK enqueue uses REPLACE. Only explicit retry actions bypass pending backoff.
                    if (shouldEnqueueBibleSync(state, restartPending)) {
                        if (!LightWork.enqueue(lightContext, SYNC_JOB_KEY, input)) {
                            getOrNull()?.preferences?.setSyncState(BibleSyncState.FAILED, "Download job unavailable")
                        }
                    }
                }
            }
        }

        private fun create(
            lightContext: SealedLightContext,
            dataStore: DataStore<Preferences>,
        ): BibleApp {
            val database = lightContext.buildDatabase(
                BibleDatabase::class.java,
                DATABASE_NAME,
            )
            val registry = TranslationRegistry(lightContext)
            val preferences = BiblePreferences(dataStore)
            return BibleApp(
                database = database,
                registry = registry,
                preferences = preferences,
                contentRepository = BibleContentRepository(database, registry, preferences),
                userDataRepository = UserDataRepository(database),
                searchRepository = SearchRepository(database),
                strongsRepository = StrongsRepository(lightContext, database),
                interlinearRepository = InterlinearRepository(database, registry, preferences),
                plansRepository = PlansRepository(lightContext, database),
            )
        }
    }

    suspend fun needsSync(): Boolean {
        contentRepository.ensureCatalogInDatabase()
        val catalog = registry.translationsToDownloadOnLaunch()
        if (catalog.isEmpty()) return false
        return catalog.any { entry ->
            !contentRepository.isTranslationComplete(entry.id) ||
                (entry.supportsWordStudy && !interlinearRepository.hasInterlinear(entry.id))
        }
    }

    suspend fun markSyncIdleIfComplete() {
        if (!needsSync()) {
            preferences.setSyncState(BibleSyncState.COMPLETE)
        }
    }
}

internal fun shouldEnqueueBibleSync(state: LightJobState, restartPending: Boolean = false): Boolean =
    state !is LightJobState.Running && (state !is LightJobState.Enqueued || restartPending)
