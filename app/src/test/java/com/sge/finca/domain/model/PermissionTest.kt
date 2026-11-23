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
        Permission.FORWARD_ALERTS,
    )

    /** Solo del propietario por defecto, pero él puede otorgárselos a un trabajador (#67). */
    private val grantable = listOf(
        Permission.VIEW_BALANCE,
    )

    private val operational = listOf(
        Permission.CREATE_TRANSACTION,
        Permission.VIEW_TRANSACTIONS,
        Permission.REGISTER_ANIMAL,
        Permission.START_INSPECTION,
        Permission.MANAGE_CONTACTS,
        Permission.MANAGE_REMINDERS,
        Permission.CHAT,
    )

    private fun worker(grants: Set<Permission> = emptySet(), isActive: Boolean = true) = Session(
        uid = "u2",
        farmId = "f1",
        member = FarmMember("u2", "f1", FarmRole.WORKER, isActive, joinedAt = 0, grants = grants),
        loading = false,
    )

    @Test
    fun everyPermissionIsClassified() {
        assertEquals(Permission.entries.toSet(), (ownerOnly + grantable + operational).toSet())
        assertEquals(grantable.toSet(), Permission.entries.filter { it.grantable }.toSet())
    }

    @Test
    fun ownerCanDoEverything() {
        Permission.entries.forEach { assertTrue(it.name, FarmRole.OWNER.can(it)) }
    }

    @Test
    fun workerOnlyHasOperationalPermissions() {
        operational.forEach { assertTrue(it.name, FarmRole.WORKER.can(it)) }
        (ownerOnly + grantable).forEach { assertFalse(it.name, FarmRole.WORKER.can(it)) }
    }

    @Test
    fun workerSeesBalanceOnlyWhenGranted() {
        assertFalse(worker().can(Permission.VIEW_BALANCE))
        assertTrue(worker(grants = setOf(Permission.VIEW_BALANCE)).can(Permission.VIEW_BALANCE))
    }

    @Test
    fun grantsNeverUnlockOwnerOnlyPermissions() {
        val everything = worker(grants = Permission.entries.toSet())
        ownerOnly.forEach { assertFalse(it.name, everything.can(it)) }
    }

    @Test
    fun inactiveWorkerLosesGrants() {
        assertFalse(worker(grants = setOf(Permission.VIEW_BALANCE), isActive = false).can(Permission.VIEW_BALANCE))
    }

    @Test
    fun noRoleCanDoNothing() {
        val none: FarmRole? = null
        Permission.entries.forEach { assertFalse(it.name, none.can(it, Permission.entries.toSet())) }
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
