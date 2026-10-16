package com.smsmurilo.wallet.model

import kotlinx.serialization.Serializable

@Serializable
enum class Category(val label: String, val emoji: String) {
    FOOD("Alimentação", "🍽️"),
    TRANSPORT("Transporte", "🚗"),
    HOUSING("Moradia", "🏠"),
    HEALTH("Saúde", "💊"),
    LEISURE("Lazer", "🎬"),
    OTHER("Outros", "📦"),
}

/** Money is kept in cents (Long) — no floating-point rounding errors and no BigDecimal on iOS. */
@Serializable
data class Expense(
    val id: String,
    val description: String,
    val amountCents: Long,
    val category: Category,
    /** ISO date yyyy-MM-dd */
    val date: String,
)

@Serializable
data class Budget(val category: Category, val limitCents: Long)

data class CategorySummary(
    val category: Category,
    val spentCents: Long,
    val limitCents: Long?,
) {
    val usedRatio: Double? get() = limitCents?.takeIf { it > 0 }?.let { spentCents.toDouble() / it }
    val status: BudgetStatus get() = when {
        usedRatio == null -> BudgetStatus.NO_BUDGET
        usedRatio!! >= 1.0 -> BudgetStatus.EXCEEDED
        usedRatio!! >= 0.8 -> BudgetStatus.WARNING
        else -> BudgetStatus.OK
    }
}

enum class BudgetStatus { NO_BUDGET, OK, WARNING, EXCEEDED }
