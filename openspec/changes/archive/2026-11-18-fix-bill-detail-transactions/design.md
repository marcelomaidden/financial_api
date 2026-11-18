# Design

## Context

Estado atual, observado no código:
- `BillService.getBillDetail(owner, id, accountId)` faz quatro passos em sequência:
  1. `owners.require(owner)`;
  2. `pluggyClient.retrieveBill(owner, id)`, mapeado para `BillDetail`;
  3. `transactionService.getAllByBill(owner, accountId, id)`;
  4. ordena as compras por `date` decrescente.
- `TransactionService.getAllByBill` usa o `drain` sobre `getByBill`, que chama `PluggyClient.listTransactionsByBill` → `PluggyApi.listTransactionsByBill`, com `GET /v2/transactions?accountId&billId&after`. É essa chamada que a Pluggy recusa (veja proposal.md, Why).
- `TransactionService.getAllTransactions(owner, accountId, dateFrom, dateTo)` já existe. Ela pagina `GET /v2/transactions?accountId&dateFrom&dateTo&after` até o fim, e `PluggyClient.formatDate` omite a data quando ela é nula.
- `PluggyMapper.toTransaction` já copia `creditCardMetadata.billId` para `Transaction.billId`, e `installmentOf` já preenche a parcela.
- `BillDetail` tem `dueDate` e `closingDate` como `LocalDate` (dia lido, sem fuso). `closingDate` é opcional, e `dueDate` vem sempre pela documentação da Pluggy, mas o código o trata como anulável.
- `ExpensesSummaryService` usa folgas de 10 dias antes e 5 depois numa janela derivada da fatura (`BillingCycle`). É o precedente para a folga.

Dados reais (veja proposal.md, What Changes): numa instituição, o `date` da parcela é o `billPostDate`, e não a data da compra. Uma parcela 2/4 de uma compra de 2026-09-18 tem `date` `2026-10-21T03:00:00.000Z` e está na fatura de novembro.

## Goals / Non-Goals

**Goals:**
- Trazer as compras da fatura sem o parâmetro `billId` na Pluggy, reusando a listagem por datas que já existe.
- Manter o contrato de `GET /bill/{id}` e o comportamento de erro: falha em qualquer página derruba a requisição, sem resposta parcial.

**Non-Goals:**
- Usar o `GET /transactions` legado, mesmo enquanto ele existe.
- Paginar o histórico inteiro do cartão.
- Detectar automaticamente que a janela perdeu compras, por exemplo comparando a soma das compras com `totalAmount`.

## Decisions

**1. Filtro por `billId` no backend, sobre a listagem por datas.**
`BillService.getBillDetail` passa a chamar `transactionService.getAllTransactions(owner, accountId, from, to)` e guarda só as transações com `id.equals(tx.billId())`. Saem `TransactionService.getAllByBill` e `getByBill`, `PluggyClient.listTransactionsByBill` e `PluggyApi.listTransactionsByBill`. Os testes deles migram para a regra nova.
Alternativas descartadas:
- `GET /transactions?billId=` (legado): sai do ar em 2026-12-31.
- `createdAtFrom`: não tem limite superior, e na carga inicial do item todas as transações têm o mesmo `createdAt`.
- Sem janela: completude total, mas pagina cerca de 12 meses do cartão a cada abertura do detalhe.

**2. Janela no domínio, em `BillDetail`.**
Um método `BillDetail.transactionsWindow()` devolve `Optional<DateRange>`, com `from` e `to` como `LocalDate` (record novo em `domain.model`, ou aninhado em `BillDetail`):
- com `closingDate` C: de `C.minusMonths(1).minusDays(10)` a `C.plusDays(10)`;
- sem `closingDate` e com `dueDate` D: de `D.minusMonths(1).minusDays(20)` a `D`;
- sem as duas: vazio. O serviço chama `getAllTransactions` com `dateFrom` e `dateTo` nulos, e o filtro por `billId` continua valendo.

As folgas ficam como constantes nomeadas (`WINDOW_SLACK_DAYS = 10` e `DUE_TO_CLOSING_DAYS = 10`), no estilo de `ExpensesSummaryService`. A janela pelo vencimento é a mesma da janela pelo fechamento quando o fechamento vem 10 dias antes do vencimento, que é o caso comum.
A folga é generosa de propósito: buscar a mais não traz dado errado, porque o filtro descarta o que não é da fatura. A folga cobre três casos:
- **Fuso:** o `date` chega em UTC, então uma compra às 22h em São Paulo cai no dia seguinte.
- **Lançamento tardio:** um banco que lança a compra depois do fechamento.
- **Fechamento anterior estimado:** ele é calculado como "fechamento menos 1 mês".

Alternativa descartada: reusar `BillingCycle`. Ele projeta a fatura *aberta* a partir da mais recente, e aqui a fatura é dada e quase sempre já fechou.

**3. Ordem e mapeamento sem mudança.**
O filtro vem antes da ordenação atual (`nullsLast(reverseOrder())`, estável). `installment`, `category` e `date` em `yyyy-mm-dd` continuam saindo de `PluggyMapper.toTransaction` e `ResponseMapper.toBillTransaction`.

**4. Falhas.**
Sem `try/catch` novo. A falha em qualquer página de `getAllTransactions` propaga como hoje, com o mesmo mapeamento de `ApiErrors`: 404, 429, 502 e 504.

## Risks / Trade-offs

- [Outra instituição pode usar a data da compra no `date` das parcelas] → A parcela de uma compra antiga cai fora da janela e some sem erro. A amostra cobre um banco só. Mitigação: a tarefa 3.1 confere uma fatura parcelada de outra instituição (cartão da luana). Se o problema aparecer, a janela pode crescer pelo `totalInstallments`, numa change própria.
- [Fatura sem fechamento nem vencimento busca sem janela] → O comportamento da Pluggy sem `dateFrom` não está documentado. Pode trazer tudo ou um padrão curto. É um caso raro, porque `dueDate` vem sempre pela documentação. Ele fica registrado e não ganha tratamento especial.
- [A janela de cerca de 50 dias traz transações de duas faturas] → Esperado e descartado pelo filtro. Custa no máximo uma página a mais.
- [Compra da fatura com `date` nula] → Ela não entra em nenhuma janela de data da Pluggy, então não aparece. Antes ela aparecia por último. Aceito, porque uma transação de cartão sem `date` não foi vista em dados reais.

## Migration Plan

Sem migração de dados. O deploy é só o do backend. O rollback reverte o código, mas a versão anterior volta a falhar, porque a Pluggy recusa o `billId`.
