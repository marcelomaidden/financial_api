# Tasks

## 1. Consulta por chegada e dados de parcela

- [x] 1.1 Adicionar `PluggyApi.listTransactionsCreatedSince` (`createdAtFrom` e `after`), o método correspondente em `PluggyClient` e `TransactionService.getAllCreatedSince(owner, accountId, Instant)`, extraindo o laço de paginação de `getAllTransactions` para um método comum. Verificar com `TransactionServiceTest` cobrindo uma página, páginas encadeadas e página vazia da nova consulta, e que `getAllTransactions` continua passando, com `./gradlew test`
- [x] 1.2 Acrescentar `installmentNumber` e `totalInstallments` a `PluggyTransaction.CreditCardMetadata`, `Transaction.Installment` (nulo quando falta um dos dois) e levar os valores no `PluggyMapper`. Ajustar os construtores nos testes existentes. Verificar com `PluggyMapperTest` (parcela completa, sem parcela, só o número) e `./gradlew test` passando

## 2. Ciclo da fatura

- [x] 2.1 Criar `BillingCycle.from(bills, today)` em `domain.model`. Verificar com `BillingCycleTest` cobrindo: o exemplo real (fecha 4/11, vence 16/11, hoje 15/11, dando próximo fechamento 4/12, vencimento 16/12 e fechamento anterior 4/11), hoje antes e depois do fechamento, fatura mais recente já vencida há meses, fechamento em dia 31 com mês curto, escolha da fatura de maior fechamento, fatura sem uma das datas e lista vazia, e `./gradlew test` passando

## 3. Seção expenses

- [x] 3.1 Trocar `ExpensesPeriod` para `(dueFrom, dueTo, today)`, criar o record `Installment` em `resource.dto` e o campo `installment` (omitido quando nulo) em `ExpenseTransaction`, e reescrever a busca de compras de `ExpensesSummaryService`: faturas buscadas uma vez por cartão, janela por cartão com `BillingCycle`, filtro superior por `date`, janela padrão sem ciclo ou com falha de faturas. Verificar com `ExpensesSummaryServiceTest` cobrindo: parcela com `date` antiga incluída, parcela com `date` no vencimento da aberta incluída, parcela de fatura futura excluída, `createdAtFrom` calculado do fechamento anterior menos 10 dias, cartão sem fatura, falha de faturas (dono fora de `owed` e compras com a janela padrão), `installment` presente e omitido, e a falha só nas compras, e `./gradlew test` passando
- [x] 3.2 Fazer `SummaryService` montar o novo `ExpensesPeriod` com a data de hoje. Atualizar `SummaryServiceTest` para conferir `dueFrom`, `dueTo` e `today` nos quatro casos de `dateFrom` e `dateTo`, e que `dateFrom` e `dateTo` não alteram a janela das compras. Estender `SummaryJsonTest` para conferir `installment.number` e `installment.total` na parcela e a ausência do campo na compra à vista. Verificar com `./gradlew test` passando

## 4. Documentação e dados reais

- [x] 4.1 Atualizar a descrição de `expenses.transactions` e dos períodos no README (janela por cartão, `dateFrom` e `dateTo` sem efeito nessa parte, campo `installment`) e corrigir o comentário de `PluggyBill` sobre as datas. Verificar que os campos do exemplo do README batem com os do `SummaryJsonTest`
- [ ] 4.2 Verificar com a Pluggy real (exige as credenciais): que `GET /v2/transactions` aceita `createdAtFrom`, onde ficam `installmentNumber` e `totalInstallments` na resposta, que o ciclo derivado de cada cartão bate com a fatura real (exemplo: fecha 4/11 e vence 16/11 dá próxima fatura que fecha 4/12 e vence 16/12), qual padrão de parcela (A ou B) os bancos dos cartões seguem e o sinal do `amount` nas compras. Registrar o resultado no design.md
