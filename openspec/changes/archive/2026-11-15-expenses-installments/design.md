# Design

## Context

Estado atual, observado no código:
- `ExpensesSummaryService` busca as faturas de cada cartão (`BillService.getAllBills`) para `owed` e, separadamente, as transações do cartão com `TransactionService.getAllTransactions(owner, accountId, from, to)`, que filtra por `dateFrom` e `dateTo` na Pluggy. A janela vem de `ExpensesPeriod(dueFrom, dueTo, transactionsFrom, transactionsTo)`, calculado em `SummaryService`.
- `PluggyApi.listTransactions` envia `dateFrom`, `dateTo` e `after`. Não existe chamada com `createdAtFrom`.
- `PluggyTransaction.CreditCardMetadata` só tem `billId`. `Transaction` não tem dados de parcela.
- `PluggyBill` tem `dueDate` e `billClosingDate`. Os dois chegam como `2026-11-16T00:00:00.000Z` (meia-noite UTC) e são lidos como `LocalDateTime`, o que preserva o dia. O comentário da classe diz que "as datas vêm sem fuso", o que não é verdade.
- `Account.credit` guarda `balanceCloseDate` e `balanceDueDate`, de significado incerto.

Da documentação da Pluggy e dos dados reais:
- `GET /v2/transactions` aceita `createdAtFrom`, e esse filtro não pode ser combinado com `dateFrom`. A Pluggy recomenda `createdAtFrom` para achar transações que chegam dias depois, com `date` no passado.
- Parcelas: no Padrão A todas as parcelas chegam de uma vez, com `date` igual ao vencimento de cada fatura. No Padrão B cada parcela chega no fechamento da fatura dela, já com `billId`.
- Uma fatura real: `billClosingDate` 2026-11-04 e `dueDate` 2026-11-16 (o dia 15 caiu em domingo e o vencimento foi para segunda).

Veja proposal.md para a motivação e o escopo.

## Goals / Non-Goals

**Goals:**
- Incluir em `expenses.transactions` as parcelas que hoje saem da janela, sem incluir as parcelas de faturas futuras.
- Janela própria de cada cartão, sem configuração manual.
- Mostrar a parcela da compra.

**Non-Goals:**
- Sem projeção de parcelas futuras.
- Sem webhooks nem cache.
- Sem mudança em `/transactions`, `/bills`, `owed` e `incomes`.

## Decisions

**1. Consulta por data de chegada.**
`PluggyApi.listTransactionsCreatedSince(accountId, createdAtFrom, after)` envia `createdAtFrom` como instante ISO em UTC. `PluggyClient` e `TransactionService.getAllCreatedSince(owner, accountId, Instant)` seguem o padrão dos métodos atuais. O laço de paginação de `getAllTransactions` vira um método privado comum aos dois. Não é preciso ler `createdAt` na resposta, porque o filtro é feito na Pluggy.

**2. Ciclo da fatura em `BillingCycle`.**
Record em `domain.model`, com `static Optional<BillingCycle> from(List<Bill> bills, LocalDate today)`. Usa a fatura de maior `billClosingDate`:
- `gap` = dias entre o fechamento e o vencimento dela.
- `nextClosing` = `closing.plusMonths(k)`, com o menor `k >= 0` que dá uma data depois de hoje. Calcular a partir do fechamento da fatura, e não mês a mês, evita acumular o ajuste de fim de mês.
- `previousClosing` = `closing.plusMonths(k - 1)`, e `nextDue` = `nextClosing + gap`.
- Sem fatura com as duas datas, o resultado é vazio.
Exemplo real: fecha 2026-11-04, vence 2026-11-16, hoje 2026-11-15, então `nextClosing` 2026-12-04, `previousClosing` 2026-11-04 e `nextDue` 2026-12-16. A regra "primeiro fechamento depois de hoje" dá o mesmo resultado quando o banco devolve a fatura só no vencimento.

**3. Janela de cada cartão.**
- Limite inferior: `previousClosing - 10 dias`, à meia-noite no fuso `America/Sao_Paulo`, convertido para instante e enviado como `createdAtFrom`.
- Limite superior: a compra só entra se `date` (a data do `OffsetDateTime`, sem converter fuso) for no máximo `nextDue + 5 dias`. A folga de 5 dias cobre o vencimento que escorrega em fim de semana. Isso exclui as parcelas futuras do Padrão A.
- Constantes nomeadas (`LOWER_SLACK_DAYS = 10`, `UPPER_SLACK_DAYS = 5`) em `ExpensesSummaryService`.
Alternativa descartada: `Account.credit.balanceDueDate`, porque não sei o que significa em cada banco, e a fatura já está disponível.

**4. Cartão sem ciclo.**
Sem `BillingCycle` (nenhuma fatura devolvida, ou a consulta de faturas falhou), a janela é a padrão: `createdAtFrom` no dia 1 do mês anterior a hoje e `date` até hoje.

**5. Faturas buscadas uma vez por cartão.**
`ExpensesSummaryService` busca as faturas de cada cartão uma vez e usa a lista para `owed` e para o ciclo. Se a busca de um cartão falhar, o dono sai de `owed`, o erro é registrado, e as compras daquele cartão usam a janela padrão. As compras continuam isoladas por `try/catch`, como hoje.

**6. Parcela.**
`PluggyTransaction.CreditCardMetadata` ganha `installmentNumber` e `totalInstallments` (`Integer`). `Transaction` ganha `Installment(number, total)`, nulo quando falta qualquer um dos dois. `ExpenseTransaction` ganha `installment` (record `Installment(number, total)` em `resource.dto`), omitido do JSON quando nulo. Os nomes e o lugar dos campos vêm da documentação e ainda não foram vistos em uma resposta real, então a tarefa 4.2 os confirma. Se estiverem em outro lugar, o mapeamento devolve nulo sem erro.

**7. `ExpensesPeriod` e a fachada.**
`ExpensesPeriod(dueFrom, dueTo, today)`. `transactionsFrom` e `transactionsTo` somem, porque `expenses.transactions` não usa mais `dateFrom` e `dateTo`. `SummaryService` passa a data de hoje do `Clock`. `owed` e `incomes` não mudam.

**8. Datas das faturas.**
O comentário de `PluggyBill` passa a dizer que as datas chegam à meia-noite UTC e são tratadas como data, sem conversão de fuso.

## Risks / Trade-offs

- [Nomes e lugar de `installmentNumber` e `totalInstallments` não confirmados] → Tarefa 4.2. O mapeamento falha em silêncio (nulo), por isso a verificação com dados reais é obrigatória.
- [Banco com ciclo diferente do derivado, ou fatura mais recente sem as duas datas] → Janela padrão. Folgas de 10 e 5 dias absorvem desvios pequenos.
- [A fatura aberta só aparece no fechamento, e o ciclo vem dela] → A regra do primeiro fechamento depois de hoje não depende de qual fatura veio por último.
- [Transação apagada e recriada pela Pluggy ganha outra data de chegada] → Aceito. A Pluggy trata isso como raro e a compra é contada uma vez.
- [Compra que nunca ganha `billId` (caso XP) fica enquanto estiver na janela] → Aceito. O `createdAtFrom` a retira com o tempo.
- [`createdAtFrom` na v2 não confirmado com dados reais] → Tarefa 4.2.
- [`dateFrom` e `dateTo` deixam de afetar uma parte do resumo] → Registrado na spec e no README.
