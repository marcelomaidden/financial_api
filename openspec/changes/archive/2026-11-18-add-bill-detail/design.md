# Design

## Context

Estado atual, observado no código:
- `PluggyApi` (interface `@HttpExchange`) tem `listBills` (`GET /bills?accountId=`) e `listTransactions` (`GET /v2/transactions` com `accountId`, `dateFrom`, `dateTo` e `after`). Não há `GET /bills/{id}` nem filtro por `billId`. `PluggyClient` expõe cada método recebendo o dono e lança `UnknownOwnerException` se o dono não existe.
- `PluggyBill` (`client.dto`) lê só `id`, `dueDate`, `billClosingDate`, `totalAmount`, `totalAmountCurrencyCode` e `minimumPaymentAmount`, com datas como `LocalDateTime`. Não lê `allowsInstallments`, `financeCharges` nem `payments`.
- `PluggyTransaction` já lê `creditCardMetadata` com `billId`, `installmentNumber` e `totalInstallments`. `PluggyMapper.toTransaction` já produz `Transaction` com `billId` e `installment` (só quando número e total vêm preenchidos), o mesmo mapeamento do `expenses.transactions` do resumo. `Transaction.date` é `OffsetDateTime`.
- `TransactionService` já pagina por cursor: `getTransactions` converte `next` em `after` (`cursorFrom`) e o método privado `drain` percorre até `next` nulo.
- `BillService.getBills` chama `owners.require(owner)` antes da Pluggy (dono desconhecido vira 404 sem consulta) e `getAllBills` percorre as páginas de faturas.
- Erros: `GlobalExceptionHandler` converte `MissingServletRequestParameterException` em 400 (`Parâmetro 'x' obrigatório`) e `HandlerMethodValidationException` em 400 (`Parâmetros inválidos`). `ApiErrors.from` mapeia `PluggyException` 404 para 404, 429 para 429, demais para 502, e `ResourceAccessException` para 504.
- A chave de API e o CORS valem para toda rota, sem registro por rota.
- O JSON de `GET .../bills` sai com `LocalDateTime` (`2026-11-20T00:00:00`, como no README). `LoanResponse` usa `LocalDate` e sai como `yyyy-mm-dd`.

Da documentação da Pluggy:
- `GET /bills/{id}` responde 200 com `id`, `dueDate`, `billClosingDate` (opcional), `totalAmount`, `totalAmountCurrencyCode`, `minimumPaymentAmount` (opcional), `allowsInstallments` (opcional), `financeCharges` (`id`, `type`, `amount`, `currencyCode`, `additionalInfo`) e `payments` (`id`, `valueType`, `paymentDate`, `paymentMode`, `amount`, `currencyCode`), ou 404. As datas chegam como `2023-07-20T00:00:00`.

Veja proposal.md para a motivação e o escopo, e specs para os requisitos.

## Goals / Non-Goals

**Goals:**
- Entregar a fatura e as compras dela numa resposta só, reusando o cliente, a paginação por cursor, o mapeamento de parcela e o tratamento de erros existentes.
- Falhar a requisição inteira quando qualquer uma das duas consultas à Pluggy falhar.

**Non-Goals:**
- Sem cache, sem paginação da resposta, sem edição ou pagamento de fatura.
- Sem alterar `GET .../bills`, `PluggyBill`, `Bill` nem `BillResponse`.
- Sem validar que `accountId` é a conta da fatura.
- Sem consulta em paralelo da fatura e das compras.

## Decisions

**1. Rota `GET /bill/{id}` com `owner` e `accountId` obrigatórios por query.**
`FinancialDataController.billDetail(@PathVariable String id, @RequestParam @NotBlank String owner, @RequestParam @NotBlank String accountId)`. `@RequestParam` sem `required = false` já faz o `GlobalExceptionHandler` responder 400 quando o parâmetro falta. `@NotBlank` cobre `owner=` vazio pelo mesmo caminho de `HandlerMethodValidationException` (400), no mesmo estilo de `@Min` e `@Max` de `bills`. O `id` do caminho é repassado à Pluggy sem validação própria: um id inexistente vira 404 da Pluggy. Alternativa descartada: `GET /owners/{owner}/accounts/{accountId}/bills/{id}`, que seguiria as rotas existentes, mas o contrato com o cliente é `/bill/{id}`.

**2. Cliente: `retrieveBill` e transações por fatura.**
- `PluggyApi.retrieveBill(@PathVariable("id") String id)` com `@GetExchange("/bills/{id}")`, devolvendo `PluggyBillDetail`.
- `PluggyApi.listTransactionsByBill(accountId, billId, after)` com `@GetExchange("/v2/transactions")`, devolvendo `PluggyCursorPage<PluggyTransaction>`. Segue o padrão de `listTransactionsCreatedSince`: um método próprio, porque `dateFrom` e `dateTo` não entram nessa consulta.
- `PluggyClient.retrieveBill(owner, id)` e `PluggyClient.listTransactionsByBill(owner, accountId, billId, after)`, ambos por `api(owner)`.
Alternativa descartada: acrescentar `billId` ao `listTransactions`, que obrigaria todos os chamadores atuais a passar `null`.

**3. `PluggyBillDetail` separado de `PluggyBill`.**
Record novo em `client.dto` com `@JsonIgnoreProperties(ignoreUnknown = true)`: `id`, `dueDate`, `billClosingDate`, `totalAmount`, `totalAmountCurrencyCode`, `minimumPaymentAmount`, `allowsInstallments` (`Boolean`), `financeCharges` e `payments`, com os blocos aninhados `FinanceCharge` e `Payment` e `...OrEmpty()` para listas nulas, como `PluggyLoan`. As datas são `LocalDateTime`, como em `PluggyBill`. Alternativa descartada: ampliar `PluggyBill`. Funcionaria, porque campo ausente vira nulo, mas mexe no DTO da listagem e do resumo sem necessidade, e o `GET /bills` não devolve esses campos.

**4. Modelo de domínio com `LocalDate`.**
`BillDetail(id, dueDate, closingDate, totalAmount, currencyCode, minimumPaymentAmount, allowsInstallments, financeCharges, payments)` em `domain.model`, com `FinanceCharge` e `Payment` aninhados, e `PluggyMapper.toBillDetail`. As datas viram `LocalDate` pelo dia lido (`toLocalDate()`, sem conversão de fuso), como `Loan`. O resultado do serviço é `BillDetailResult(BillDetail bill, List<Transaction> transactions)` em `domain.result`. `Bill` e `PluggyMapper.toBill` não mudam.

**5. Compras por fatura em `TransactionService`.**
`getAllByBill(owner, accountId, billId)` usa o `drain` existente com uma função que chama `pluggyClient.listTransactionsByBill` e converte `next` com `cursorFrom`, no mesmo molde de `getAllCreatedSince`. Reusa `PluggyMapper.toTransaction`, que já preenche `installment`. Uma falha em qualquer página propaga a exceção e a lista parcial é descartada.

**6. Orquestração em `BillService.getBillDetail`.**
`BillService` ganha `TransactionService` no construtor. `getBillDetail(owner, id, accountId)`:
1. `owners.require(owner)`, que dá 404 sem consultar a Pluggy.
2. `pluggyClient.retrieveBill(owner, id)`. Um 404 aqui sobe como `PluggyException` e o handler responde 404, sem buscar as compras.
3. `transactionService.getAllByBill(owner, accountId, id)`.
4. Ordena as compras por `date` decrescente, com `Comparator.nullsLast(Comparator.reverseOrder())`, em ordenação estável. Isso mantém a ordem da Pluggy dentro da mesma data, como `TransactionListService`.
Não há `try/catch`: qualquer exceção segue o handler global (404, 429, 502, 504), sem resposta parcial. Alternativa descartada: ler as compras primeiro ou em paralelo. A chamada em sequência evita paginar compras de uma fatura que não existe e mantém o fluxo simples.

**7. DTOs de resposta e mapeamento.**
Em `resource.dto`: `BillDetailResponse(owner, accountId, BillDetail bill, List<BillTransaction> transactions)`, com os aninhados `BillSummary`, `FinanceChargeResponse` e `PaymentResponse`, e `BillTransaction(id, date, description, amount, currencyCode, category, type, status, installment)`. Todas as datas são `LocalDate`, e o `ResponseMapper` converte a `date` das compras com `toLocalDate()` do `OffsetDateTime`, sem trocar o fuso. `installment` reusa o record `Installment` existente, com `@JsonInclude(NON_NULL)` só nesse campo, como em `ExpenseTransaction`. Os demais campos nulos saem como `null`, como em `LoanResponse`. `type` sai como `getType()` do enum, como em `TransactionResponse`. Alternativa descartada: reusar `TransactionResponse`, que expõe `OffsetDateTime` e não tem `installment`.

**8. Sem mudança em controle de acesso nem em erros.**
O filtro de chave e o CORS atuais já cobrem a rota nova. Um teste confirma isso com a rota nova, sem alterar código de segurança.

## Risks / Trade-offs

- [`accountId` que não bate com a fatura] → A Pluggy devolve a fatura e nenhuma compra, e a resposta é 200 com `transactions` vazio. Aceito e registrado, sem validação extra. Se virar problema, uma change própria pode comparar com as contas do dono.
- [O filtro `billId` em `GET /v2/transactions` e os campos de `GET /bills/{id}` vêm da documentação, não de dados reais] → Testes de leitura com o JSON de exemplo da documentação, e a tarefa 3.2 confere com a Pluggy real. Campo com nome errado vira nulo, sem erro.
- [Duas ou mais chamadas à Pluggy por requisição, sem cache] → Aceito. Uma fatura tem poucas dezenas de compras, e a Pluggy limita por taxa (429 já mapeado).
- [Compra de fatura com `date` nula some no fim da ordenação] → `nullsLast`, como em `TransactionListService`.
- [As datas do detalhe saem em `yyyy-mm-dd`, e as de `GET .../bills` seguem como `2026-11-20T00:00:00`] → Aceito: o contrato do detalhe pede `yyyy-mm-dd` e mudar a listagem está fora do escopo. Quem consome as duas precisa tratar os dois formatos.
- [`date` da compra vem como `OffsetDateTime` e vira só o dia] → O dia lido, sem conversão de fuso. Uma compra perto da meia-noite pode cair no dia da Pluggy, e não no dia local. Registrado.
- [`id` da fatura de outro dono ou de outro cartão] → A Pluggy responde 404 com as credenciais do dono errado. Com credenciais certas e a conta errada, vale o primeiro risco.
