# Design

## Context

Estado atual, observado no código:
- `SummaryService.getSummary(dateFrom, dateTo)` define o período (`dateTo` = hoje, `dateFrom` = dia 1 do mês de `dateTo`), busca as contas com `accountService.getAccounts(null)`, percorre os donos e monta `SummaryResponse`.
- No laço, um dono com erro de contas vai para `errors` e é pulado. Para os demais, filtra contas `BANK` em BRL (`available`) e pagina as transações de cada conta (`transactions`), com `try/catch` que registra o erro do dono.
- A paginação (`do { getTransactions(...); after = nextCursor } while (after != null)`) vive dentro do `SummaryService`.
- `TransactionService.getTransactions` devolve uma página (`TransactionPage`).

Veja proposal.md para a motivação.

## Goals / Non-Goals

**Goals:**
- `SummaryService` fica pequeno: período, busca de contas, junção das seções.
- A lógica de `incomes` fica em um serviço próprio, sem conhecer o fluxo de contas nem o cálculo do período.
- A paginação de transações fica reutilizável.

**Non-Goals:**
- Sem `ExpensesSummaryService` e sem seção `expenses`.
- Sem interface genérica para seções. Com uma só seção, o formato de cada uma ainda é diferente.
- Sem mudança de rota, de JSON ou de regra de negócio.

## Decisions

**1. A fachada trata o erro de contas, e as seções só recebem donos com sucesso.**
Se cada seção olhasse `OwnerAccountsResult.error()`, o mesmo erro entraria em `errors` uma vez por seção. A fachada registra os erros de contas uma única vez e passa às seções apenas os donos sem erro. Cada seção cuida só das falhas que ela mesma provoca (consulta de transações).

**2. Contrato do serviço de seção.**
`IncomesSummaryService.summarize(List<OwnerAccountsResult> owners, LocalDate from, LocalDate to)` devolve `SectionResult<IncomesSummary>`, um record com a seção e a lista de `SummaryError` dela. `SectionResult<T>` é genérico só para ser reaproveitado, e não implica uma interface de seção. A seção não recebe `Clock` nem calcula período.

**3. Contas buscadas uma vez.**
A fachada chama `accountService.getAccounts(null)` uma vez e repassa a lista. O número de chamadas à Pluggy não aumenta quando houver mais de uma seção. A regra de 502 quando todos os donos falham continua vindo de `AccountService`, e a fachada não a repete.

**4. Paginação em `TransactionService.getAllTransactions`.**
Novo método `getAllTransactions(owner, accountId, from, to)` devolve `List<Transaction>` percorrendo `nextCursor` até acabar. Fica em `TransactionService` porque ele já conhece `cursorFrom` e o cliente da Pluggy. Alternativa descartada: helper separado, que criaria uma classe a mais sem outro uso hoje.

**5. A fachada junta o resultado.**
`SummaryResponse(incomes, partial, errors)`, com `errors` = erros de contas, seguidos dos erros de cada seção, e `partial` = `errors` não vazio.

**6. Efeito observável aceito: ordem de `errors`.**
Hoje os erros saem na ordem dos donos (um erro de contas e um erro de transações aparecem intercalados). Depois da mudança, os erros de contas vêm antes dos erros de transações. O conteúdo é o mesmo, e só a ordem da lista muda quando há falhas de tipos diferentes em donos diferentes. A spec `financial-summary` não define ordem.

**7. Tipos de resposta ficam em `resource.dto`.**
`IncomesSummary`, `SummaryError` e `SummaryResponse` continuam onde estão. A dependência residual de `service` em `resource.dto` é a mesma da change `structure-dtos` e fica para a etapa 2 daquela reestruturação.

## Risks / Trade-offs

- [Refactor muda o JSON sem querer] → Teste que serializa o `SummaryResponse` da fachada com o `JsonMapper` e confere os nomes de campo de `incomes`, `partial` e `errors`.
- [Ordem de `errors` muda] → Aceito na decisão 6. Um teste fixa a nova ordem.
- [Os cenários de `dateFrom` e `dateTo` não tinham teste] → Os testes da fachada passam a cobrir sem parâmetros, só `dateFrom`, só `dateTo` e os dois.
- [`SectionResult` genérico parece abstração antecipada] → É só um record de dois campos. Uma interface de seção só entra quando houver a segunda implementação.
