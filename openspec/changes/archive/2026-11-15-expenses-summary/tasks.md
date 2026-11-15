# Tasks

## 1. Dados do cartão no cliente e no domínio

- [x] 1.1 Adicionar `amountInAccountCurrency` e `creditCardMetadata.billId` a `PluggyTransaction`, e `billId` e `amountInAccountCurrency` a `Transaction`, levando os valores no `PluggyMapper`. Ajustar os construtores nos testes existentes. Verificar com `PluggyMapperTest` (transação com e sem `creditCardMetadata`, e com `amountInAccountCurrency`) e `./gradlew test` passando
- [x] 1.2 Adicionar `Transaction.isUnbilledPendingDebit()` e `amountInBrl()`, `Account.isBrlCreditCard()` e `Bill.isBrl()`. Verificar com `ModelRulesTest` cobrindo tipo, status, `billId` nulo e vazio, moeda BRL e estrangeira, valor nulo, e `./gradlew test` passando

## 2. Faturas

- [x] 2.1 Adicionar `BillService.getAllBills(owner, accountId)`, que percorre as páginas com `pageSize` 500 até a última. Verificar com `BillServiceTest` (cliente da Pluggy simulado) para uma página, três páginas e página vazia, e `./gradlew test` passando

## 3. Seção expenses

- [x] 3.1 Criar em `resource.dto` os records `OwedBill`, `ExpenseTransaction` (com `amountInAccountCurrency` omitido do JSON quando nulo) e `ExpensesSummary`, e acrescentar `expenses` a `SummaryResponse`. Ajustar os usos do construtor. Verificar que `./gradlew compileJava` passa
- [x] 3.2 Criar `ExpensesPeriod` e `ExpensesSummaryService.summarize(owners, period)`, com `owed` e `transactions`, os totais em BRL e os dois `try/catch` independentes. Verificar com `ExpensesSummaryServiceTest` cobrindo: fatura que vence depois de hoje no mês, fatura fora do período, fatura em outra moeda, conta `BANK` ignorada, compra pendente sem fatura, compra com `billId`, compra `POSTED`, compra `CREDIT`, compra em USD com `amountInAccountCurrency`, USD sem o valor (fora do total), falha só nas faturas e falha só nas compras, e `./gradlew test` passando

## 4. Fachada

- [x] 4.1 Fazer o `SummaryService` calcular o `ExpensesPeriod`, chamar `ExpensesSummaryService` com os donos sem erro de contas, juntar `expenses` e unificar erros idênticos. Verificar com `SummaryServiceTest` cobrindo os quatro casos de `dateFrom` e `dateTo` para os dois períodos (por exemplo, sem parâmetros: `owed` de 1/11 a 30/11 e `transactions` de 1/10 a 15/11), ordem e unificação de `errors`, e `./gradlew test` passando
- [x] 4.2 Estender `SummaryJsonTest` para conferir os campos de `expenses.owed` e `expenses.transactions`, incluindo `amountInAccountCurrency` presente na compra em USD e ausente na compra em BRL. Verificar com `./gradlew test` passando

## 5. Documentação e dados reais

- [x] 5.1 Documentar `GET /summary` na tabela de endpoints do README, com `dateFrom` e `dateTo`, e um exemplo de resposta com `incomes` e `expenses`. Verificar que os nomes de campo do exemplo batem com os do `SummaryJsonTest`
- [ ] 5.2 Verificar com a Pluggy real (exige as credenciais): que `creditCardMetadata.billId` existe e fica vazio nas compras pendentes, qual o sinal do `amount` nas compras do cartão, que `type` e moeda das contas e das faturas são os esperados, e que o JSON de `GET /summary` confere com `/credit-cards`, `/bills` e `/transactions`. Registrar o resultado no design.md
