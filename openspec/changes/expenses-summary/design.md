# Design

## Context

Estado atual, observado no código:
- `SummaryService` é a fachada: calcula o período de `incomes`, busca as contas uma vez, registra o erro de contas de cada dono e chama `IncomesSummaryService`. `SectionResult<T>` carrega a seção e os erros dela.
- `TransactionService.getAllTransactions(owner, accountId, from, to)` percorre as páginas por cursor.
- `BillService.getBills(owner, accountId, page, pageSize)` devolve uma página. `Bill` (domínio) tem `id`, `dueDate`, `closingDate`, `totalAmount`, `currencyCode` e `minimumPaymentAmount`. A Pluggy não filtra faturas por data nem informa se a fatura foi paga.
- `PluggyTransaction` não lê `creditCardMetadata.billId` nem `amountInAccountCurrency`. `Transaction` (domínio) também não.
- `SummarySection<T>(total, detail)`, `Money` e `SummaryError` já existem em `resource.dto`.

Da documentação da Pluggy: em cartão, `PENDING` é a compra ainda não incluída em fatura. A transação vira `POSTED` com `billId` quando a fatura aparece. A fatura aberta só é devolvida depois de fechada ou vencida, conforme a instituição. `amountInAccountCurrency` só vem quando a moeda difere da moeda da conta.

Veja proposal.md para a motivação e o escopo.

## Goals / Non-Goals

**Goals:**
- `expenses` com a mesma forma de `incomes`, no mesmo `GET /summary`.
- Reaproveitar a fachada, `SectionResult` e a paginação existentes.

**Non-Goals:**
- Sem saber se a fatura foi paga.
- Sem conversão de moeda: só cartões com conta em BRL.
- Sem interface genérica para seções: `ExpensesSummaryService` é a segunda implementação, e a decisão de abstrair continua adiada.
- Sem mudança em `/accounts`, `/credit-cards`, `/transactions` e `/bills`.

## Decisions

**1. Novos campos nas transações da Pluggy.**
`PluggyTransaction` ganha `amountInAccountCurrency` e `creditCardMetadata` (record com `billId`). `Transaction` ganha `billId` e `amountInAccountCurrency`. O `ResponseMapper` não usa os novos campos, então `/transactions` não muda.

**2. Regras no domínio.**
- `Transaction.isUnbilledPendingDebit()`: `type = DEBIT`, `status = PENDING` e `billId` nulo ou vazio.
- `Transaction.amountInBrl()`: o `amount` quando a moeda é BRL, o `amountInAccountCurrency` quando não é, e nulo quando faltam os dois.
- `Account.isBrlCreditCard()`: tipo `CREDIT` e moeda BRL. Com isso, o valor `amountInAccountCurrency` de uma compra do cartão está em BRL.
- `Bill.isBrl()`: moeda BRL e valor informado.
`status` continua `String`, e `PENDING` vira constante em `Transaction`. Um enum de status fica para quando houver outro uso.

**3. `BillService.getAllBills(owner, accountId)`.**
Percorre as páginas com `pageSize = 500` até `page >= totalPages`. O filtro por `dueDate` é feito no serviço de `expenses`, porque a Pluggy não filtra por data.

**4. Três intervalos de data, calculados pela fachada.**
O record `ExpensesPeriod(dueFrom, dueTo, transactionsFrom, transactionsTo)` é calculado em `SummaryService`:
- `to` = `dateTo` ou hoje.
- `owed`: `dueFrom` = `dateFrom` ou o dia 1 do mês de `to`; `dueTo` = o último dia do mês de `to`. Uma fatura que vence no dia 20 aparece mesmo que hoje seja dia 15.
- `transactions`: `transactionsFrom` = `dateFrom` ou o dia 1 do mês anterior ao de `to`; `transactionsTo` = `to`. Uma compra de 28/10 ainda sem fatura aparece em novembro.
O período de `incomes` não muda.

**5. `ExpensesSummaryService.summarize(owners, period)`.**
Devolve `SectionResult<ExpensesSummary>`. Recebe só donos sem erro de contas, como `IncomesSummaryService`. Para cada dono, filtra `Account.isBrlCreditCard`:
- `owed`: para cada cartão, `getAllBills`, filtra `Bill.isBrl()` e `dueDate` entre `dueFrom` e `dueTo`. Cada fatura vira `OwedBill(id, accountId, name, dueDate, totalAmount, currencyCode)`.
- `transactions`: para cada cartão, `getAllTransactions(transactionsFrom, transactionsTo)`, filtra `isUnbilledPendingDebit()`. Cada compra vira `ExpenseTransaction(id, accountId, description, currencyCode, amount, date, category, amountInAccountCurrency)`. `amountInAccountCurrency` só vai preenchido quando a moeda não é BRL, e o campo é omitido do JSON quando nulo.
Os dois `try/catch` são independentes: falha de faturas tira o dono só de `owed`, e falha de transações tira o dono só de `transactions`.

**6. Totais.**
`owed.total` soma `totalAmount`. `transactions.total` soma `amountInBrl()` e ignora o valor nulo. Os dois totais usam `currencyCode = BRL`. Uma compra em moeda estrangeira sem `amountInAccountCurrency` aparece no detalhe e fica fora do total.

**7. Resposta e erros.**
`SummaryResponse` ganha o campo `expenses`. Novos DTOs em `resource.dto`: `ExpensesSummary`, `OwedBill` e `ExpenseTransaction`. A fachada junta os erros de contas, de `incomes` e de `expenses`, e remove erros idênticos (mesmo dono, status e mensagem) com `distinct()`. Sem isso, um dono em que faturas e compras falham pelo mesmo motivo aparece duas vezes.

**8. Sinal dos valores.**
Os valores seguem o que a Pluggy devolve. Não sei se o `amount` de compra de cartão vem positivo ou negativo, e isso muda o sinal do total. A tarefa 5.2 verifica com dados reais, e o design e o README são atualizados se o sinal precisar de tratamento.

## Risks / Trade-offs

- [`creditCardMetadata.billId` não confirmado com dados reais] → Tarefa 5.2. Se o campo vier em outro lugar, a regra de compra não faturada não pega nada.
- [Fatura já paga entra em `owed`] → Aceito. A Pluggy não informa o pagamento, e o resumo diz "faturas com vencimento no período", não "em aberto".
- [Fatura aberta não devolvida pela instituição] → As compras dela aparecem em `transactions`, que é o motivo de as duas partes existirem.
- [Mais chamadas à Pluggy: faturas e transações por cartão, com paginação] → Aceito. Risco de 429, e sem cache nesta etapa.
- [Cartão com conta em outra moeda] → Fica fora do resumo. Não há caso conhecido.
- [Um dono pode aparecer duas vezes em `errors` com causas diferentes] → Aceito. Só erros idênticos são unificados.
