package com.sge.finca.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionTest {

    private val ownerOnly = listOf(
        Permission.EDIT_TRANSACTION,
        Permission.DELETE_TRANSACTION,
        Permission.MANAGE_CATEGORIES,
        Permission.MANAGE_WORKERS,
        Permission.VIEW_INVITE_CODE,
        Permission.CONFIGURE_FARM,
    )

    private val operational = listOf(
        Permission.CREATE_TRANSACTION,
        Permission.VIEW_TRANSACTIONS,
        Permission.VIEW_REPORTS,
        Permission.REGISTER_ANIMAL,
        Permission.START_INSPECTION,
        Permission.MANAGE_CONTACTS,
        Permission.MANAGE_REMINDERS,
    )

    @Test
    fun everyPermissionIsClassified() {
        assertEquals(Permission.entries.toSet(), (ownerOnly + operational).toSet())
    }

    @Test
    fun ownerCanDoEverything() {
        Permission.entries.forEach { assertTrue(it.name, FarmRole.OWNER.can(it)) }
    }

    @Test
    fun workerOnlyHasOperationalPermissions() {
        operational.forEach { assertTrue(it.name, FarmRole.WORKER.can(it)) }
        ownerOnly.forEach { assertFalse(it.name, FarmRole.WORKER.can(it)) }
    }

    @Test
    fun noRoleCanDoNothing() {
        val none: FarmRole? = null
        Permission.entries.forEach { assertFalse(it.name, none.can(it)) }
    }

    @Test
    fun inactiveMemberHasNoRole() {
        val session = Session(
            uid = "u1",
            farmId = "f1",
            member = FarmMember("u1", "f1", FarmRole.OWNER, isActive = false, joinedAt = 0),
            loading = false,
        )
        assertNull(session.role)
        assertFalse(session.can(Permission.CREATE_TRANSACTION))
    }

    @Test
    fun sessionWithoutFarmHasNoPermissions() {
        val session = Session(uid = "u1", loading = false)
        assertNull(session.role)
        assertFalse(session.isOwner)
        assertFalse(session.can(Permission.VIEW_TRANSACTIONS))
    }
}
