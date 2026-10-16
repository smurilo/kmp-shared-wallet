package com.smsmurilo.wallet.data

import com.smsmurilo.wallet.model.Budget
import com.smsmurilo.wallet.model.Expense
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Optimistic updates: a new expense shows up instantly; if the server rejects it,
 * it is rolled back and the error surfaces to the caller.
 */
class WalletRepository(private val api: WalletApi) {

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    private val _budgets = MutableStateFlow<List<Budget>>(emptyList())
    val budgets: StateFlow<List<Budget>> = _budgets.asStateFlow()

    suspend fun refresh() {
        _expenses.value = api.expenses().sortedByDescending { it.date }
        _budgets.value = api.budgets()
    }

    suspend fun add(expense: Expense) {
        _expenses.update { listOf(expense) + it }
        try {
            val saved = api.add(expense)
            _expenses.update { list -> list.map { if (it.id == expense.id) saved else it } }
        } catch (e: Exception) {
            _expenses.update { list -> list.filterNot { it.id == expense.id } }
            throw e
        }
    }
}
