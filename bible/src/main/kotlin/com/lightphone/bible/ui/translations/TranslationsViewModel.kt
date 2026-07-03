package com.lightphone.bible.ui.translations

import androidx.lifecycle.viewModelScope

import com.lightphone.bible.BibleApp
import com.lightphone.bible.data.db.TranslationEntity
import com.lightphone.bible.repository.BibleSyncState
import com.thelightphone.sdk.LightJobState
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.LightWork
import com.thelightphone.sdk.SealedLightContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TranslationListItem(
    val entity: TranslationEntity,
    val statusLabel: String,
    val isPrimary: Boolean,
)

data class TranslationsUiState(
    val isLoading: Boolean = true,
    val translations: List<TranslationListItem> = emptyList(),
    val syncState: BibleSyncState = BibleSyncState.IDLE,
)

class TranslationsViewModel(
    private val lightContext: SealedLightContext,
) : LightViewModel<Unit>() {
    private val app = BibleApp.ensureInitialized(lightContext)

    private val _uiState = MutableStateFlow(TranslationsUiState())
    val uiState: StateFlow<TranslationsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            combine(
                app.preferences.syncState,
                LightWork.observe(lightContext, BibleApp.SYNC_JOB_KEY),
            ) { syncState, jobState ->
                syncState to jobState
            }.collect { (syncState, jobState) ->
                refresh(syncState, jobState)
            }
        }
    }

    fun setPrimary(translationId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            app.preferences.setPrimaryTranslationId(translationId)
            val syncState = app.preferences.syncState.first()
            val jobState = LightWork.getState(lightContext, BibleApp.SYNC_JOB_KEY)
            refresh(syncState, jobState)
        }
    }

    fun download(translationId: String) {
        BibleApp.enqueueSyncIfNeeded(lightContext, translationId)
    }

    fun syncAll() {
        BibleApp.enqueueSyncIfNeeded(lightContext)
    }

    private suspend fun refresh(syncState: BibleSyncState, jobState: LightJobState) {
        app.contentRepository.ensureCatalogInDatabase()
        val primaryId = app.preferences.getPrimaryTranslationId()
        val entities = app.contentRepository.getTranslations()
        val items = entities.map { entity ->
            val status = when {
                jobState is LightJobState.Running && !entity.isDownloaded -> "Downloading…"
                entity.isDownloaded -> "Downloaded"
                syncState == BibleSyncState.FAILED -> "Failed"
                else -> "Not downloaded"
            }
            TranslationListItem(
                entity = entity,
                statusLabel = status,
                isPrimary = entity.id == primaryId,
            )
        }
        withContext(Dispatchers.Main) {
            _uiState.update {
                it.copy(isLoading = false, translations = items, syncState = syncState)
            }
        }
    }
}
