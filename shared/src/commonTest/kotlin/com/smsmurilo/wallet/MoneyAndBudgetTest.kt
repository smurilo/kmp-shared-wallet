package com.smsmurilo.wallet

import com.smsmurilo.wallet.domain.BudgetCalculator
import com.smsmurilo.wallet.domain.Money
import com.smsmurilo.wallet.model.Budget
import com.smsmurilo.wallet.model.BudgetStatus
import com.smsmurilo.wallet.model.Category
import com.smsmurilo.wallet.model.Expense
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MoneyAndBudgetTest {

    @Test
    fun formatsAndParsesBrazilianCurrency() {
        assertEquals("R$ 1.234.567,89", Money.format(123456789))
        assertEquals("R$ 0,05", Money.format(5))
        assertEquals("-R$ 10,00", Money.format(-1000))
        assertEquals(123456, Money.parse("1.234,56"))
        assertEquals(1250, Money.parse("R$ 12,5"))
        assertEquals(1250, Money.parse("12.50"))
        assertNull(Money.parse("12,3,4"))
        assertNull(Money.parse("abc"))
    }

    @Test
    fun summarizesMonthAgainstBudgets() {
        val expenses = listOf(
            Expense("1", "Mercado", 45_000, Category.FOOD, "2026-03-02"),
            Expense("2", "Restaurante", 12_000, Category.FOOD, "2026-03-10"),
            Expense("3", "Uber", 3_000, Category.TRANSPORT, "2026-03-11"),
            Expense("4", "Mercado fev", 99_000, Category.FOOD, "2026-02-27"),
        )
        val budgets = listOf(Budget(Category.FOOD, 60_000), Budget(Category.HEALTH, 20_000))

        val summary = BudgetCalculator.summarize(expenses, budgets, "2026-03")

        assertEquals(listOf(Category.FOOD, Category.TRANSPORT, Category.HEALTH), summary.map { it.category })
        assertEquals(57_000, summary[0].spentCents)
        assertEquals(BudgetStatus.WARNING, summary[0].status)
        assertEquals(BudgetStatus.NO_BUDGET, summary[1].status)
        assertEquals(BudgetStatus.OK, summary[2].status)
    }

    @Test
    fun projectsMonthEndSpending() {
        assertEquals(93_000, BudgetCalculator.projectMonthEnd(30_000, 10, 31))
    }
}
