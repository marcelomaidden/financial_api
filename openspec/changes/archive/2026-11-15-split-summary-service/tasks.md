# Tasks

## 1. Paginação comum

- [x] 1.1 Adicionar `TransactionService.getAllTransactions(owner, accountId, from, to)`, que percorre `nextCursor` e devolve todas as transações. Verificar com `TransactionServiceTest` (cliente da Pluggy simulado) para uma página, duas páginas encadeadas por `after` e página vazia, e `./gradlew test` passando

## 2. Serviço de incomes

- [x] 2.1 Criar `SectionResult<T>` (seção e lista de `SummaryError`) e `IncomesSummaryService.summarize(owners, from, to)`, com a lógica atual de `available` e `transactions`, usando `getAllTransactions` e o `try/catch` por dono. Verificar com `IncomesSummaryServiceTest`, movendo os cenários de soma, filtros de tipo, moeda e período, transferência entre donos e falha só nas transações, e `./gradlew test` passando

## 3. Fachada

- [x] 3.1 Reduzir `SummaryService` a fachada: calcular o período, chamar `accountService.getAccounts(null)` uma vez, registrar os erros de contas, repassar à seção só os donos sem erro, juntar seção, erros e `partial`. Verificar com `SummaryServiceTest` cobrindo: contas buscadas uma vez, erro de contas registrado uma vez e dono fora das duas partes, erro de transações, ordem de `errors` (contas antes de transações), propagação do 502 quando todos falham, e período sem parâmetros, só `dateFrom`, só `dateTo` e os dois
- [x] 3.2 Adicionar um teste que serializa o `SummaryResponse` da fachada com o `JsonMapper` e confere os campos `incomes.available.total.value`, `incomes.available.detail`, `incomes.transactions.detail`, `partial` e `errors`. Verificar com `./gradlew test` passando
- [x] 3.3 Verificar que `SummaryService` não injeta mais `TransactionService`, que `./gradlew test` passa por inteiro e que `OwnerFanOut` e as rotas do `FinancialDataController` não aparecem em `git diff`
