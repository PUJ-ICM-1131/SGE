package com.sge.finca.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InviteCodeTest {

    @Test
    fun `genera codigos XXXX-XXXX validos y sin caracteres confundibles`() {
        repeat(200) {
            val code = InviteCode.generate()
            assertTrue(code, InviteCode.isValid(code))
            assertTrue(code, Regex("^.{4}-.{4}$").matches(code))
            assertFalse(code, code.any { it in "ILO01" })
        }
    }

    @Test
    fun `normaliza como se escribe o se dicta`() {
        assertEquals("ABCD-2345", InviteCode.normalize("abcd-2345"))
        assertEquals("ABCD-2345", InviteCode.normalize("ABCD2345"))
        assertEquals("ABCD-2345", InviteCode.normalize(" abcd 2345 "))
        assertEquals("JXH2EQ", InviteCode.normalize(" jxh2eq "))
    }

    @Test
    fun `acepta el formato actual y el anterior de 6 caracteres`() {
        assertTrue(InviteCode.isValid("abcd-2345"))
        assertTrue(InviteCode.isValid("JXH2EQ")) // fincas creadas antes del #29
    }

    @Test
    fun `rechaza codigos mal formados o con letras ambiguas`() {
        assertFalse(InviteCode.isValid("ABCD-234O")) // O
        assertFalse(InviteCode.isValid("ABCI-2345")) // I
        assertFalse(InviteCode.isValid("ABC-2345"))
        assertFalse(InviteCode.isValid("JXH2E"))
        assertFalse(InviteCode.isValid("ABCD-23456"))
        assertFalse(InviteCode.isValid(""))
    }
}
