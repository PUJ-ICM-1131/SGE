package com.sge.finca.domain.model

import java.security.SecureRandom
import java.util.Locale

/**
 * Código de invitación de una finca (#29).
 *
 * - Formato actual: `XXXX-XXXX` (8 caracteres, ~850 mil millones de combinaciones).
 * - Formato anterior: 6 caracteres sin guion (fincas creadas antes del #29, p. ej. `JXH2EQ`).
 *   Se siguen aceptando para unirse; el propietario puede regenerar para pasar al nuevo.
 * - Sin caracteres que se confunden al dictarlos o escribirlos: 0/O, 1/I/L.
 */
object InviteCode {

    private const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    private const val CHAR = "[A-HJKMNP-Z2-9]"
    private val current = Regex("^$CHAR{4}-$CHAR{4}$")
    private val legacy = Regex("^$CHAR{6}$")
    private val random = SecureRandom()

    fun generate(): String {
        val characters = (1..8)
            .map { ALPHABET[random.nextInt(ALPHABET.length)] }
            .joinToString("")
        return "${characters.take(4)}-${characters.drop(4)}"
    }

    /**
     * Deja el código como se guarda: mayúsculas, sin espacios, y con guion si escribieron los
     * 8 caracteres seguidos. "abcd efgh" → "ABCD-EFGH"; " jxh2eq " → "JXH2EQ".
     */
    fun normalize(input: String): String {
        val compact = input.uppercase(Locale.ROOT).filter { it.isLetterOrDigit() }
        return if (compact.length == 8) "${compact.take(4)}-${compact.drop(4)}" else compact
    }

    /** Acepta el formato actual y el anterior (ya normalizados o no). */
    fun isValid(input: String): Boolean {
        val code = normalize(input)
        return current.matches(code) || legacy.matches(code)
    }
}
