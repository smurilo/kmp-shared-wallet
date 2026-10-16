package com.smsmurilo.wallet.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.smsmurilo.wallet.data.WalletApi
import com.smsmurilo.wallet.data.WalletRepository
import com.smsmurilo.wallet.domain.Money
import com.smsmurilo.wallet.model.BudgetStatus
import com.smsmurilo.wallet.model.Category
import com.smsmurilo.wallet.presentation.WalletStore
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

/** Thin Android wrapper: lifecycle-aware scope around the shared store. */
class WalletViewModel : ViewModel() {
    val store = WalletStore(
        repository = WalletRepository(WalletApi.create("https://api.carteira-exemplo.com.br/")),
        month = YearMonth.now().toString(),
        idGenerator = { UUID.randomUUID().toString() },
        scope = viewModelScope,
    ).also { it.refresh() }
}

class MainActivity : ComponentActivity() {
    private val viewModel: WalletViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { WalletScreen(viewModel.store) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalletScreen(store: WalletStore) {
    val state by store.state.collectAsStateWithLifecycle()
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(Category.FOOD) }
    var formError by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text("Gastos de ${state.month}") }) }) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("Total: ${state.totalFormatted}", style = MaterialTheme.typography.headlineSmall) }
            state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            items(state.summaries, key = { it.category.name }) { s ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${s.category.emoji} ${s.category.label} — ${Money.format(s.spentCents)}" +
                            (s.limitCents?.let { " de ${Money.format(it)}" } ?: ""))
                        s.usedRatio?.let {
                            LinearProgressIndicator(
                                progress = { it.toFloat().coerceAtMost(1f) },
                                modifier = Modifier.fillMaxWidth(),
                                color = if (s.status == BudgetStatus.OK) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
            item {
                Text("Nova despesa", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(description, { description = it }, label = { Text("Descrição") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(amount, { amount = it }, label = { Text("Valor (R$)") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Category.entries.take(4).forEach { c ->
                        FilterChip(selected = c == category, onClick = { category = c }, label = { Text(c.emoji) })
                    }
                }
                formError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(onClick = {
                    formError = store.addExpense(description, amount, category, LocalDate.now().toString())
                    if (formError == null) { description = ""; amount = "" }
                }) { Text("Adicionar") }
            }
            items(state.recent, key = { it.id }) { e ->
                ListItem(headlineContent = { Text(e.description) }, supportingContent = { Text(e.date) },
                    trailingContent = { Text(Money.format(e.amountCents)) })
            }
        }
    }
}
