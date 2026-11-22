package com.sge.finca.data.sync

import com.sge.finca.data.local.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncConflictsTest {

    @Test
    fun remoteAlwaysWinsWhenLocalIsSyncedOrMissing() {
        assertEquals(MergeDecision.APPLY_REMOTE, SyncConflicts.decide(null, null, 100))
        assertEquals(MergeDecision.APPLY_REMOTE, SyncConflicts.decide(SyncStatus.SYNCED, 500, 100))
    }

    @Test
    fun newerRemoteWinsOverPendingLocal() {
        assertEquals(MergeDecision.APPLY_REMOTE, SyncConflicts.decide(SyncStatus.PENDING, 100, 200))
        assertEquals(MergeDecision.APPLY_REMOTE, SyncConflicts.decide(SyncStatus.FAILED, 100, 200))
    }

    @Test
    fun newerOrEqualLocalIsKept() {
        assertEquals(MergeDecision.KEEP_LOCAL, SyncConflicts.decide(SyncStatus.PENDING, 300, 200))
        assertEquals(MergeDecision.KEEP_LOCAL, SyncConflicts.decide(SyncStatus.PENDING, 200, 200))
    }

    @Test
    fun withoutTimestampsPendingLocalIsKept() {
        // Categorías: no tienen updatedAt.
        assertEquals(MergeDecision.KEEP_LOCAL, SyncConflicts.decide(SyncStatus.PENDING, null, null))
        assertEquals(MergeDecision.APPLY_REMOTE, SyncConflicts.decide(SyncStatus.SYNCED, null, null))
    }

    @Test
    fun conflictOnlyWhenLocalHasUnsyncedChanges() {
        assertFalse(SyncConflicts.isConflict(null))
        assertFalse(SyncConflicts.isConflict(SyncStatus.SYNCED))
        assertTrue(SyncConflicts.isConflict(SyncStatus.PENDING))
        assertTrue(SyncConflicts.isConflict(SyncStatus.FAILED))
    }
}
