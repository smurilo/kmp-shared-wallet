import SwiftUI
import Shared

/// Bridges the shared Kotlin store into SwiftUI's observation system.
@MainActor
final class WalletObservable: ObservableObject {
    @Published private(set) var state: WalletState
    private let store: WalletStore
    private var subscription: Closeable?

    init(store: WalletStore = IosFactoryKt.makeWalletStore(baseUrl: "https://api.carteira-exemplo.com.br/")) {
        self.store = store
        self.state = store.state.value as! WalletState
        subscription = store.watch { [weak self] newState in self?.state = newState }
        store.refresh()
    }

    deinit { subscription?.close() }

    func refresh() { store.refresh() }

    func add(description: String, amount: String, category: Category) -> String? {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return store.addExpense(description: description, amountInput: amount, category: category, date: formatter.string(from: Date()))
    }
}

struct WalletView: View {
    @StateObject private var model = WalletObservable()
    @State private var description = ""
    @State private var amount = ""
    @State private var error: String?

    var body: some View {
        NavigationStack {
            List {
                Section {
                    Text("Total: \(model.state.totalFormatted)").font(.title2.bold())
                    if let message = model.state.error { Text(message).foregroundStyle(.red) }
                }
                Section("Por categoria") {
                    ForEach(model.state.summaries, id: \.category.name) { summary in
                        VStack(alignment: .leading) {
                            Text("\(summary.category.emoji) \(summary.category.label) — \(Money.shared.format(cents: summary.spentCents))")
                            if let ratio = summary.usedRatio?.doubleValue {
                                ProgressView(value: min(ratio, 1))
                                    .tint(summary.status == .ok ? .accentColor : .red)
                            }
                        }
                    }
                }
                Section("Nova despesa") {
                    TextField("Descrição", text: $description)
                    TextField("Valor (R$)", text: $amount).keyboardType(.decimalPad)
                    if let error { Text(error).foregroundStyle(.red) }
                    Button("Adicionar") {
                        error = model.add(description: description, amount: amount, category: .food)
                        if error == nil { description = ""; amount = "" }
                    }
                }
                Section("Recentes") {
                    ForEach(model.state.recent, id: \.id) { expense in
                        HStack {
                            Text(expense.description_)
                            Spacer()
                            Text(Money.shared.format(cents: expense.amountCents))
                        }
                    }
                }
            }
            .navigationTitle("Gastos de \(model.state.month)")
            .refreshable { model.refresh() }
        }
    }
}
