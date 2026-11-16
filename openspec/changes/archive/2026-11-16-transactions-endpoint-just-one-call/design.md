# Design

## Context

Estado atual, observado no código:
- `GET /owners/{owner}/accounts/{accountId}/transactions` devolve uma página (`TransactionsResponse` com `nextCursor`), pela `TransactionService.getTransactions`.
- `TransactionService.getAllTransactions(owner, accountId, from, to)` já percorre todas as páginas e é usado por `IncomesSummaryService`.
- `AccountService.getAccounts(owner)` lista as contas de todos os donos, ou só de `owner`. Ele usa o `OwnerFanOut`: sem `owner`, a falha de um dono vira `error` na entrada dele e `AllOwnersFailedException` (502) se todos falharem. Com `owner`, a falha propaga como erro normal, e dono desconhecido dá 404 sem consultar a Pluggy.
- `SummaryService` calcula o período (`dateTo` = hoje, `dateFrom` = dia 1 do mês de `dateTo`) com um `Clock` no fuso `America/Sao_Paulo`.
- `GlobalExceptionHandler` converte `RuntimeException` pelo `ApiErrors.from`. Exceções desconhecidas viram 500, e só as do Spring (parâmetro com tipo errado ou ausente) viram 400.
- A `date` de uma transação da Pluggy é um dia à meia-noite UTC, então várias transações do mesmo dia empatam. A Pluggy devolve da data mais recente para a mais antiga.

Da documentação: o `GET /v2/transactions` traz até 500 transações por página, e o limite da Pluggy é de 360 requisições por minuto por endpoint.

Veja proposal.md para a motivação e o escopo.

## Goals / Non-Goals

**Goals:**
- Uma chamada devolve as transações de todos os donos e contas, ou das que o cliente restringir.
- Reaproveitar `AccountService`, `TransactionService` e a regra de erro dos outros endpoints de lista.

**Non-Goals:**
- Sem paginação nem limite da lista, e sem filtro por tipo, status ou categoria.
- Sem nome da conta em cada item.
- Sem paralelizar as consultas: são seriais, como as do resumo.
- Sem remover nem alterar a rota por conta.

## Decisions

**1. Serviço novo, `TransactionListService`.**
`list(owner, accountId, dateFrom, dateTo)` devolve `TransactionListResult` (lista e falhas). O controller só delega e traduz. `TransactionService` e `AccountService` não mudam. Alternativa descartada: acrescentar o método a `TransactionService`, que passaria a depender de `AccountService` e misturaria a consulta de uma conta com a de todas.

**2. Validação e consulta.**
- `accountId` sem `owner` lança `InvalidRequestException` (nova, em `exception`), mapeada para 400 em `ApiErrors`, antes de qualquer chamada à Pluggy.
- Com `owner` e `accountId`, consulta só essa conta com `getAllTransactions`. Não lista as contas, porque a Pluggy já responde 404 para uma conta que não é do dono. A falha propaga como erro normal.
- Sem `accountId`, chama `accountService.getAccounts(owner)` e consulta cada conta devolvida, de qualquer tipo.
- Um dono que veio com `error` da listagem de contas vira uma falha sem conta. Com `owner` informado, o `AccountService` já propaga o erro, e o serviço não precisa tratá-lo.

**3. Falha por conta.**
Cada conta é consultada dentro de um `try/catch`. A falha vira `AccountFailure(owner, accountId, ApiError)` (em `domain.result`) e a consulta segue. Sem nenhuma conta com sucesso e com ao menos uma falha, o serviço lança `AllAccountsFailedException` (nova, mapeada para 502). Dono sem contas é sucesso com lista vazia, e não conta como falha.

**4. Período.**
`dateTo` assume hoje e `dateFrom` assume o dia 1 do mês de `dateTo`, com um `Clock` no fuso `America/Sao_Paulo`, no mesmo molde do `SummaryService` (um construtor público e outro com `Clock` para teste). Não há validação de `dateFrom` depois de `dateTo`, como no resumo: a Pluggy devolve vazio.

**5. Ordem.**
As transações entram no resultado por dono, depois por conta e, dentro da conta, na ordem da Pluggy. Depois, uma ordenação estável por `date` decrescente (nulos por último), `owner` e `accountId`. Como a ordenação é estável, a ordem da Pluggy se mantém dentro da mesma conta e data.

**6. Tipo de cada item.**
`OwnedTransaction(owner, accountId, Transaction)` em `domain.result`. O modelo `Transaction` não ganha `owner` nem `accountId`, porque eles descrevem de onde veio a consulta, e não a transação.

**7. Resposta.**
Em `resource.dto`: `ConsolidatedTransactionsResponse(transactions, partial, errors)`, `ConsolidatedTransaction` (os campos de `TransactionResponse` mais `owner` e `accountId`) e `TransactionError(owner, accountId, status, message)`, com `accountId` nulo quando a falha foi ao listar as contas. O `ResponseMapper` traduz. `partial` é verdadeiro quando `errors` não está vazio.

**8. Rota.**
`GET /transactions` em `FinancialDataController`, com `owner`, `accountId` e as duas datas (`@DateTimeFormat(iso = DATE)`, como na rota por conta), delegando ao serviço.

## Risks / Trade-offs

- [Resposta grande e consulta serial: um período longo com muitas contas pode devolver milhares de linhas e passar de 30 segundos] → O mês corrente como padrão limita. Paginar a lista e paralelizar as consultas ficam para uma change própria se for preciso.
- [Ordenar exige carregar tudo na memória] → Aceito em escala pessoal.
- [Uma conta falhando num pedido sem filtro fica só em `errors`, e o cliente precisa olhar `partial`] → Mesmo contrato do `/summary`, documentado no README.
- [`accountId` de outro dono dá 404 só depois de chamar a Pluggy] → Aceito. É o mesmo comportamento da rota por conta.
- [A documentação da Pluggy diz que o endpoint é feito para sincronização, e não para ser chamado a cada pedido] → Aceito neste porte. Com um banco de dados no futuro, a lista consolidada passaria a ler da cópia local.
- [Ordem real da Pluggy e tempo de resposta não vistos em dados reais] → Tarefa 4.2.
