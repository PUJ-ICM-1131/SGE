package com.sge.finca.domain.model

import java.security.SecureRandom
import java.util.Locale

object InviteCode {

    private const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    private val random = SecureRandom()
    private val format = Regex("^[A-HJ-KM-NP-Z2-9]{4}-[A-HJ-KM-NP-Z2-9]{4}$")

    fun generate(): String {
        val characters = (1..8)
            .map { ALPHABET[random.nextInt(ALPHABET.length)] }
            .joinToString("")

        return "${characters.take(4)}-${characters.drop(4)}"
    }

    fun normalize(input: String): String =
        input.trim().uppercase(Locale.ROOT)

    fun isValid(input: String): Boolean =
        format.matches(normalize(input))
}