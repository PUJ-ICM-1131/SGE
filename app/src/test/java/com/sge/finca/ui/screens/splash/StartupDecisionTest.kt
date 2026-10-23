package com.sge.finca.ui.screens.splash

import com.sge.finca.platform.biometric.BiometricAvailability
import com.sge.finca.ui.screens.auth.AuthNotice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StartupDecisionTest {

    @Test
    fun `sin sesion va al login sin aviso`() {
        BiometricAvailability.entries.forEach { availability ->
            val plan = StartupDecision.decide(
                hasSession = false,
                biometricEnabled = false,
                availability = availability,
            )
            assertEquals(StartupDestination.LOGIN, plan.destination)
            assertNull(plan.notice)
        }
    }

    @Test
    fun `sin sesion ignora una preferencia de huella que haya quedado guardada`() {
        val plan = StartupDecision.decide(
            hasSession = false,
            biometricEnabled = true,
            availability = BiometricAvailability.AVAILABLE,
        )
        assertEquals(StartupDestination.LOGIN, plan.destination)
        assertNull(plan.notice)
    }

    @Test
    fun `con sesion y sin huella activada va directo al home`() {
        BiometricAvailability.entries.forEach { availability ->
            val plan = StartupDecision.decide(
                hasSession = true,
                biometricEnabled = false,
                availability = availability,
            )
            assertEquals(StartupDestination.HOME, plan.destination)
            assertNull(plan.notice)
        }
    }

    @Test
    fun `con sesion y huella activada y disponible muestra el prompt`() {
        val plan = StartupDecision.decide(
            hasSession = true,
            biometricEnabled = true,
            availability = BiometricAvailability.AVAILABLE,
        )
        assertEquals(StartupDestination.BIOMETRIC_PROMPT, plan.destination)
        assertNull(plan.notice)
    }

    @Test
    fun `huella activada pero sin huellas registradas va al login con aviso`() {
        val plan = StartupDecision.decide(
            hasSession = true,
            biometricEnabled = true,
            availability = BiometricAvailability.NONE_ENROLLED,
        )
        assertEquals(StartupDestination.LOGIN, plan.destination)
        assertEquals(AuthNotice.NO_FINGERPRINTS_ENROLLED, plan.notice)
    }

    @Test
    fun `huella activada pero sin sensor o no disponible va al login con aviso`() {
        listOf(BiometricAvailability.NO_HARDWARE, BiometricAvailability.UNAVAILABLE)
            .forEach { availability ->
                val plan = StartupDecision.decide(
                    hasSession = true,
                    biometricEnabled = true,
                    availability = availability,
                )
                assertEquals(StartupDestination.LOGIN, plan.destination)
                assertEquals(AuthNotice.BIOMETRIC_UNAVAILABLE, plan.notice)
            }
    }
}
