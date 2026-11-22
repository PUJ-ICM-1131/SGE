package com.sge.finca.data.sync

import java.io.IOException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncErrorsTest {

    @Test
    fun rulesAndInvalidDataArePermanent() {
        listOf("PERMISSION_DENIED", "INVALID_ARGUMENT", "FAILED_PRECONDITION", "OUT_OF_RANGE")
            .forEach { assertTrue(it, SyncErrors.isPermanentCode(it)) }
    }

    @Test
    fun networkAndServerErrorsAreTransient() {
        listOf("UNAVAILABLE", "DEADLINE_EXCEEDED", "UNAUTHENTICATED", "RESOURCE_EXHAUSTED", "INTERNAL", "ABORTED")
            .forEach { assertFalse(it, SyncErrors.isPermanentCode(it)) }
    }

    @Test
    fun invalidPathIsPermanentAndIoIsTransient() {
        assertTrue(SyncErrors.isPermanent(IllegalArgumentException("Id de Firestore inválido")))
        assertFalse(SyncErrors.isPermanent(IOException("sin red")))
    }
}
