package com.lightphone.bible

import com.lightphone.bible.repository.BibleSyncState
import com.thelightphone.sdk.LightJob
import com.thelightphone.sdk.LightJobHandler
import com.thelightphone.sdk.LightJobResult
import kotlinx.coroutines.CancellationException

@LightJob(BibleApp.SYNC_JOB_KEY)
val bibleSync: LightJobHandler = bibleSync@{ ctx, input ->
    val app = BibleApp.ensureInitialized(ctx)
    val translationFilter = decodeTranslationInput(input["translationId"])

    try {
        app.preferences.setSyncState(BibleSyncState.SYNCING)
        app.contentRepository.ensureCatalogInDatabase()

        val entries = if (translationFilter != null) {
            val entry = app.registry.getTranslation(translationFilter)
                ?: throw IllegalArgumentException("Unknown translation: $translationFilter")
            listOf(entry)
        } else {
            app.registry.translationsToDownloadOnLaunch()
        }

        for (entry in entries) {
            if (!app.contentRepository.isTranslationComplete(entry.id)) {
                val provider = app.contentRepository.contentProviderFor(entry)
                    ?: throw IllegalArgumentException("No content provider for ${entry.provider}")
                app.contentRepository.syncTranslation(entry, provider)
            }
        }

        // Text is usable before word-study downloads; resume missing books on every retry.
        for (entry in entries) {
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
                } else {
                    throw IllegalArgumentException("No word-study provider for ${entry.id}")
                }
            }
        }

        app.strongsRepository.ensureLoaded()
        app.preferences.setSyncState(BibleSyncState.COMPLETE)
        LightJobResult.Success(outputFilePath = null, message = "Bible downloads complete")
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: IllegalArgumentException) {
        val message = error.message ?: "Invalid Bible download data"
        app.preferences.setSyncState(BibleSyncState.FAILED, message)
        LightJobResult.Error(reason = message)
    } catch (error: Exception) {
        app.preferences.setSyncState(
            BibleSyncState.FAILED,
            error.message ?: "Bible sync failed",
        )
        LightJobResult.Retry
    }
}

// SDK v0.2 serializes WorkManager values as map entries; accept both SDK representations.
internal fun decodeTranslationInput(value: String?): String? =
    value?.removePrefix("translationId=")?.takeIf { it.isNotBlank() }
