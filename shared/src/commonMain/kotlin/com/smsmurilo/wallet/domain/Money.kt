package com.smsmurilo.wallet.domain

import kotlin.math.abs

/** Platform-independent pt-BR currency formatting (no java.text on iOS). */
object Money {
    fun format(cents: Long): String {
        val sign = if (cents < 0) "-" else ""
        val value = abs(cents)
        val reais = (value / 100).toString().reversed().chunked(3).joinToString(".").reversed()
        val centavos = (value % 100).toString().padStart(2, '0')
        return "${sign}R$ $reais,$centavos"
    }

    /** Parses user input like "1.234,56", "12,5" or "12.50" into cents; null if invalid. */
    fun parse(input: String): Long? {
        val clean = input.replace("R$", "").replace(" ", "").trim()
        if (clean.isEmpty()) return null
        val normalized = if (clean.contains(',')) clean.replace(".", "").replace(',', '.') else clean
        val parts = normalized.split('.')
        if (parts.size > 2 || parts.any { p -> p.any { !it.isDigit() } }) return null
        val reais = parts[0].ifEmpty { "0" }.toLongOrNull() ?: return null
        val cents = parts.getOrNull(1)?.padEnd(2, '0')?.take(2)?.toLongOrNull() ?: 0
        return reais * 100 + cents
    }
}
