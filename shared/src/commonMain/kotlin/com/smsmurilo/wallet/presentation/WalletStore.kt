package com.smsmurilo.wallet.presentation

import com.smsmurilo.wallet.data.WalletRepository
import com.smsmurilo.wallet.domain.BudgetCalculator
import com.smsmurilo.wallet.domain.Money
import com.smsmurilo.wallet.model.Category
import com.smsmurilo.wallet.model.CategorySummary
import com.smsmurilo.wallet.model.Expense
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WalletState(
    val month: String,
    val loading: Boolean = false,
    val totalCents: Long = 0,
    val summaries: List<CategorySummary> = emptyList(),
    val recent: List<Expense> = emptyList(),
    val error: String? = null,
) {
    val totalFormatted get() = Money.format(totalCents)
}

/**
 * Shared presentation logic. Android wraps it in a ViewModel; iOS observes it through
 * [watch], which turns the Flow into a simple callback Swift can consume.
 */
class WalletStore(
    private val repository: WalletRepository,
    month: String,
    private val idGenerator: () -> String,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {
    private val status = MutableStateFlow(Triple(month, false, null as String?))

    val state: StateFlow<WalletState> = combine(repository.expenses, repository.budgets, status) { expenses, budgets, (m, loading, error) ->
        val summaries = BudgetCalculator.summarize(expenses, budgets, m)
        WalletState(
            month = m,
            loading = loading,
            totalCents = summaries.sumOf { it.spentCents },
            summaries = summaries,
            recent = expenses.filter { it.date.startsWith(m) }.take(20),
            error = error,
        )
    }.stateIn(scope, SharingStarted.Eagerly, WalletState(month))

    fun refresh() {
        scope.launch {
            status.value = status.value.copy(second = true, third = null)
            val error = runCatching { repository.refresh() }.exceptionOrNull()
            status.value = status.value.copy(second = false, third = error?.let { "Sem conexão. Tente novamente." })
        }
    }

    /** Returns a validation message, or null when the expense was submitted. */
    fun addExpense(description: String, amountInput: String, category: Category, date: String): String? {
        val cents = Money.parse(amountInput)
        when {
            description.isBlank() -> return "Informe uma descrição"
            cents == null || cents <= 0 -> return "Valor inválido"
        }
        scope.launch {
            runCatching { repository.add(Expense(idGenerator(), description.trim(), cents!!, category, date)) }
                .onFailure { status.value = status.value.copy(third = "Não foi possível salvar a despesa") }
        }
        return null
    }

    fun watch(onEach: (WalletState) -> Unit): Closeable {
        val job: Job = state.onEach(onEach).launchIn(scope)
        return Closeable { job.cancel() }
    }

    fun close() = scope.cancel()
}

fun interface Closeable {
    fun close()
}

@Suppress("unused") // helper for Swift
fun <T> Flow<T>.asCloseable(scope: CoroutineScope, onEach: (T) -> Unit): Closeable {
    val job = onEach(onEach).launchIn(scope)
    return Closeable { job.cancel() }
}
