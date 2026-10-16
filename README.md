# Carteira — Kotlin Multiplatform (Android + iOS)

Controle de gastos pessoais com **regras de negócio, rede e apresentação escritas uma vez em Kotlin** e compartilhadas entre **Android (Jetpack Compose)** e **iOS (SwiftUI)**. Cada plataforma mantém UI 100% nativa.

> Segmento: **Financeiro** • Multiplataforma

## Stack
**Kotlin Multiplatform** (androidTarget, iosX64/iosArm64/iosSimulatorArm64, jvm) • **Ktor Client 3** (OkHttp no Android, Darwin/NSURLSession no iOS) • kotlinx.serialization • Coroutines + StateFlow • `expect`/`actual` • Jetpack Compose • **SwiftUI** + `ObservableObject` • XcodeGen • kotlin-test • Ktor `MockEngine`

## O que é compartilhado

```
shared/
├── commonMain/
│   ├── model/          Expense, Budget, Category, CategorySummary (status de orçamento)
│   ├── domain/         Money (formatação/parsing pt-BR sem java.text), BudgetCalculator (resumo + projeção)
│   ├── data/           WalletApi (Ktor) + WalletRepository (atualização otimista com rollback)
│   └── presentation/   WalletStore: StateFlow<WalletState> + validação de formulário
├── androidMain/ jvmMain/   actual platformEngine() = OkHttp
└── iosMain/                actual platformEngine() = Darwin  +  makeWalletStore() para o Swift
androidApp/   Compose: ViewModel fino que só fornece o viewModelScope ao WalletStore
iosApp/       SwiftUI: WalletObservable faz a ponte do StateFlow para @Published
```

### Decisões técnicas
- **Dinheiro em centavos (`Long`)**: sem erro de ponto flutuante e sem depender de `BigDecimal`, que não existe no iOS.
- **Formatação monetária em Kotlin puro**: mesmo resultado nas duas plataformas, independente da localidade do aparelho.
- **Atualização otimista**: a despesa aparece na hora; se o servidor rejeitar, é removida e o erro é exibido.
- **Ponte Flow → Swift** por um `Closeable` simples (`watch {}`), sem depender de bibliotecas extras.
- **Alvo `jvm`** para rodar a suíte compartilhada rapidamente em qualquer CI (inclusive Linux).

## Como executar
```bash
./gradlew :shared:jvmTest :shared:testDebugUnitTest   # testes compartilhados (domínio + rede com MockEngine)
./gradlew :androidApp:assembleDebug

# iOS (macOS + Xcode 15+)
cd iosApp && xcodegen && open iosApp.xcodeproj
```
