package com.lightphone.bible

import com.thelightphone.sdk.LightJobState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BibleSyncSchedulingTest {
    @Test
    fun explicitRetryCanReplaceBackoffWhileStartupPreservesPendingDownload() {
        assertFalse(shouldEnqueueBibleSync(LightJobState.Enqueued))
        assertTrue(shouldEnqueueBibleSync(LightJobState.Enqueued, restartPending = true))
    }

    @Test
    fun neitherStartupNorExplicitRetryCancelsRunningDownload() {
        assertFalse(shouldEnqueueBibleSync(LightJobState.Running))
        assertFalse(shouldEnqueueBibleSync(LightJobState.Running, restartPending = true))
    }

    @Test
    fun unscheduledAndTerminalDownloadsCanStartAgain() {
        val states = listOf(
            LightJobState.NotScheduled,
            LightJobState.Cancelled,
            LightJobState.Failed(emptyMap()),
            LightJobState.Succeeded(emptyMap()),
        )
        states.forEach { state ->
            assertTrue(shouldEnqueueBibleSync(state))
            assertTrue(shouldEnqueueBibleSync(state, restartPending = true))
        }
    }
}
