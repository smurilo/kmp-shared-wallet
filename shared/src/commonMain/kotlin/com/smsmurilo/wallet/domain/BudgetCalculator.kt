package com.smsmurilo.wallet.domain

import com.smsmurilo.wallet.model.Budget
import com.smsmurilo.wallet.model.CategorySummary
import com.smsmurilo.wallet.model.Expense

object BudgetCalculator {

    /** Summaries for the given month (yyyy-MM), most spent first; categories with a budget always appear. */
    fun summarize(expenses: List<Expense>, budgets: List<Budget>, month: String): List<CategorySummary> {
        val spent = expenses.filter { it.date.startsWith(month) }.groupBy { it.category }.mapValues { (_, v) -> v.sumOf { it.amountCents } }
        val limits = budgets.associate { it.category to it.limitCents }
        return (spent.keys + limits.keys)
            .map { CategorySummary(it, spent[it] ?: 0, limits[it]) }
            .sortedByDescending { it.spentCents }
    }

    /** Linear projection of month-end spending from the pace so far. */
    fun projectMonthEnd(spentCents: Long, dayOfMonth: Int, daysInMonth: Int): Long {
        require(dayOfMonth in 1..daysInMonth)
        return spentCents * daysInMonth / dayOfMonth
    }
}
