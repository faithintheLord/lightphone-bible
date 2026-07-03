package com.lightphone.bible.ui.home

import androidx.lifecycle.viewModelScope

import com.lightphone.bible.BibleApp
import com.lightphone.bible.data.model.BibleReference
import com.lightphone.bible.repository.BibleSyncState
import com.lightphone.bible.util.VerseOfDay
import com.thelightphone.sdk.LightJobState
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.LightWork
import com.thelightphone.sdk.SealedLightContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BibleHomeUiState(
    val isLoading: Boolean = true,
    val hasDownloadedData: Boolean = false,
    val syncState: BibleSyncState = BibleSyncState.IDLE,
    val syncJobState: LightJobState = LightJobState.NotScheduled,
    val syncMessage: String? = null,
    val primaryTranslationId: String = "BSB",
    val verseOfDayText: String? = null,
    val verseOfDayReference: BibleReference? = null,
    val continueReadingLabel: String? = null,
    val continueReadingReference: BibleReference? = null,
)

class BibleHomeViewModel(
    private val lightContext: SealedLightContext,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : LightViewModel<Unit>() {
    private val app = BibleApp.ensureInitialized(lightContext)

    private val _uiState = MutableStateFlow(BibleHomeUiState())
    val uiState: StateFlow<BibleHomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(ioDispatcher) {
            if (app.needsSync()) {
                BibleApp.enqueueSyncIfNeeded(lightContext)
            } else {
                app.markSyncIdleIfComplete()
            }
        }
        viewModelScope.launch(ioDispatcher) {
            combine(
                app.preferences.syncState,
                LightWork.observe(lightContext, BibleApp.SYNC_JOB_KEY),
            ) { syncState, jobState ->
                syncState to jobState
            }.collect { (syncState, jobState) ->
                refreshContent(syncState, jobState)
            }
        }
    }

    fun retrySync() {
        BibleApp.enqueueSyncIfNeeded(lightContext)
    }

    private suspend fun refreshContent(
        syncState: BibleSyncState,
        jobState: LightJobState,
    ) {
        val primaryId = app.preferences.getPrimaryTranslationId()
            ?: app.registry.defaultTranslations().firstOrNull()?.id
            ?: "BSB"

        val translations = app.contentRepository.getTranslations()
        val hasData = translations.any { it.isDownloaded }

        val verseRef = VerseOfDay.referenceFor(translationId = primaryId)
        val verseText = if (hasData) {
            app.contentRepository.getVerse(verseRef)?.text
        } else {
            null
        }

        val progress = if (hasData) {
            app.userDataRepository.getReadingProgress(primaryId)
        } else {
            null
        }
        val continueRef = progress?.let {
            BibleReference(primaryId, it.bookId, it.chapter, it.verse)
        }

        val syncMessage = when {
            !hasData && jobState is LightJobState.Running -> "Downloading Bible content…"
            !hasData && jobState is LightJobState.Enqueued -> "Preparing download…"
            !hasData && syncState == BibleSyncState.FAILED -> "Download failed. Check your connection."
            !hasData -> "Download required before reading."
            else -> null
        }

        withContext(Dispatchers.Main) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    hasDownloadedData = hasData,
                    syncState = syncState,
                    syncJobState = jobState,
                    syncMessage = syncMessage,
                    primaryTranslationId = primaryId,
                    verseOfDayText = verseText,
                    verseOfDayReference = verseRef,
                    continueReadingReference = continueRef,
                    continueReadingLabel = continueRef?.let { ref ->
                        val book = com.lightphone.bible.data.model.BibleBooks.find(ref.bookId)
                        "${book?.name ?: ref.bookId} ${ref.chapter}:${ref.verse}"
                    },
                )
            }
        }
    }
}
