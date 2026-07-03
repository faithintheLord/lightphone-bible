package com.lightphone.bible

import com.lightphone.bible.repository.BibleSyncState
import com.thelightphone.sdk.LightJob
import com.thelightphone.sdk.LightJobHandler
import com.thelightphone.sdk.LightJobResult

@LightJob(BibleApp.SYNC_JOB_KEY)
val bibleSync: LightJobHandler = bibleSync@{ ctx, input ->
    val app = BibleApp.ensureInitialized(ctx)
    val translationFilter = input["translationId"]

    try {
        app.preferences.setSyncState(BibleSyncState.SYNCING)
        app.contentRepository.ensureCatalogInDatabase()

        val entries = if (translationFilter != null) {
            listOfNotNull(app.registry.getTranslation(translationFilter))
        } else {
            app.registry.translationsToDownloadOnLaunch()
        }

        for (entry in entries) {
            val existing = app.contentRepository.getTranslation(entry.id)
            if (existing?.isDownloaded == true && translationFilter == null) {
                continue
            }

            val provider = app.contentRepository.contentProviderFor(entry)
                ?: return@bibleSync LightJobResult.Error(
                    mapOf("error" to "No content provider for ${entry.provider}"),
                )

            app.contentRepository.syncTranslation(entry, provider)

            if (entry.supportsWordStudy && entry.interlinearProvider != null) {
                val interlinearProvider = app.interlinearRepository.interlinearProviderFor(entry)
                if (interlinearProvider != null) {
                    val books = app.contentRepository.getBooks(entry.id)
                    for (book in books) {
                        app.interlinearRepository.syncBook(
                            translationId = entry.id,
                            bookId = book.bookId,
                            provider = interlinearProvider,
                        )
                    }
                }
            }
        }

        app.strongsRepository.ensureLoaded()
        app.preferences.setSyncState(BibleSyncState.COMPLETE)
        LightJobResult.Success()
    } catch (error: Exception) {
        app.preferences.setSyncState(
            BibleSyncState.FAILED,
            error.message ?: "Bible sync failed",
        )
        LightJobResult.Retry
    }
}
