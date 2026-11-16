# Proposal

## Why

Para ler as transações, o cliente precisa chamar `GET /accounts` para descobrir os ids, depois uma rota por conta (`/owners/{owner}/accounts/{accountId}/transactions`) e repetir com `after` até acabar cada página. O código interno já sabe percorrer todas as páginas de uma conta, e o resumo o usa. Falta expor isso em uma chamada só.

## What Changes

- Novo endpoint `GET /transactions` com os parâmetros opcionais `owner`, `accountId`, `dateFrom` e `dateTo`:
  - sem `owner`, consulta todos os donos, e com `owner`, só esse dono;
  - sem `accountId`, consulta todas as contas do dono (bancárias e de cartão), e com `accountId`, só essa conta;
  - `accountId` sem `owner` é erro 400, porque o id de uma conta só vale para o dono que o devolveu;
  - percorre todas as páginas de cada conta, sem `after` nem `nextCursor`.
- Período: `dateFrom` e `dateTo` (`yyyy-mm-dd`) são opcionais. `dateTo` assume hoje e `dateFrom` assume o dia 1 do mês de `dateTo`, como no `GET /summary`. Sem nenhuma data, a consulta é do mês corrente, e passar as datas amplia ou muda o período.
- A resposta é uma lista única, `transactions`, em que cada item traz `owner` e `accountId` além dos campos de hoje (`id`, `date`, `description`, `amount`, `currencyCode`, `type`, `status`, `category`).
- Ordem: a da Pluggy, da data mais recente para a mais antiga. Como a lista junta várias contas, ela é ordenada por `date` decrescente e, no empate de data, por `owner` e por `accountId`. Dentro da mesma conta e data, a ordem que a Pluggy devolveu é mantida.
- Falha parcial: se a consulta de uma conta falhar, as transações das demais permanecem, e a resposta registra em `errors` o dono, a conta, o `status` e a `message`, com `partial` verdadeiro. Sem `owner`, a falha ao listar as contas de um dono também entra em `errors`, só com o dono. A resposta é 502 se nenhuma consulta tiver sucesso. Quando o cliente informa `accountId`, ou informa `owner` e a listagem das contas dele falha, a falha segue o fluxo normal de erro (404 ou 502), como nos outros endpoints.
- A rota por conta continua como está.
- Fora de escopo: paginação ou limite da lista, filtro por tipo, status ou categoria, o nome da conta em cada item, cache e a remoção da rota por conta.

## Capabilities

### New Capabilities

### Modified Capabilities
- `pluggy-financial-data`: ganha a listagem consolidada de transações, com a mesma regra de dono desconhecido (404) e de falha parcial dos demais endpoints de lista.

## Impact

- Código: um serviço novo que consulta as contas e as transações de cada uma, DTOs de resposta e de erro em `resource.dto`, o mapeamento no `ResponseMapper` e a rota em `FinancialDataController`. `TransactionService` e `AccountService` são reaproveitados sem mudança de comportamento.
- API: nova rota `GET /transactions`. As rotas existentes não mudam.
- Pluggy: uma chamada de contas por item e, por conta, uma chamada por página de transações. O limite de 360 requisições por minuto por endpoint não é um risco nesta escala.
- Resposta grande: sem paginação, um período longo pode devolver milhares de linhas, e a consulta serial pode demorar. O mês corrente como padrão limita isso.
- README: documentar a rota.
