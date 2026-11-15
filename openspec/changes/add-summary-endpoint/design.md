# Design

## Context

`AccountService` já consulta `GET /accounts` por item de cada dono, e `PluggyAccount` traz `id`, `type`, `name`, `balance` e `currencyCode`. `TransactionService` já consulta transações de uma conta com `dateFrom`, `dateTo` e paginação por `after`/`nextCursor`. A falha parcial entre donos já existe em `/accounts` e `/credit-cards`, implementada em `OwnerFanOut`. Veja proposal.md para motivação e escopo.

## Goals / Non-Goals

**Goals:**
- Reaproveitar `AccountService` e `TransactionService`.
- Manter a regra de falha parcial coerente com `/accounts`.

**Non-Goals:**
- Sem cache, persistência nem conversão de moeda.
- Sem filtro por dono nem período configurável nesta etapa.
- Sem `expenses` (cartões de crédito).
- Sem mudança no `OwnerFanOut`.

## Decisions

**1. Filtros.**
`available` usa contas `type = BANK`. `transactions` usa transações `type = CREDIT` (entrada de dinheiro) das contas `BANK`. Outros tipos ficam fora.

**2. Só BRL.**
Contas e transações com `currencyCode` diferente de `BRL` (ou nulo) ficam fora do detalhe e dos totais. `total` é `{value, currencyCode: "BRL"}`, sem conversão.

**3. Valores decimais.**
`balance` e `amount` seguem o decimal que a Pluggy devolve (por exemplo `19.99`). Os totais somam esses valores.

**4. Período.**
`transactions` cobre do dia 1 do mês corrente até hoje, no fuso `America/Sao_Paulo`. Percorre todas as páginas via `after`/`nextCursor`.

**5. Transferências entre donos.**
Não são detectadas nem excluídas. Contam como entrada em quem recebeu.

**6. Falha parcial com `errors` no topo.**
A resposta traz `errors` (lista de `owner`, `status`, `message`) e `partial`.
- Falha na consulta de contas de um dono: o dono sai de `available` e de `transactions`. O erro vem do `ApiError` que `AccountService.getAccounts(null)` já devolve em `OwnerAccounts.error`.
- Falha na consulta de transações de uma conta: o dono sai só de `transactions`, e o erro entra em `errors`. O `SummaryService` captura essa falha com `try/catch`, sem passar pelo `OwnerFanOut`.
- Se todos os donos falharem na consulta de contas, `OwnerFanOut` já lança `AllOwnersFailedException` (502), e o resumo não repete essa regra.

**7. Montagem em um `SummaryService`.**
Serviço novo que chama `AccountService.getAccounts(null)`, filtra, consulta as transações do mês de cada conta `BANK` e agrega por dono. O controller injeta o `SummaryService` e só delega. DTOs novos em `resource/dto`.

## Risks / Trade-offs

- [`type` das contas (`BANK`) e das transações (`CREDIT`) ainda não confirmados com dados reais] → Verificar antes de concluir.
- [Pagamentos e estornos podem aparecer como `CREDIT`] → Aceito nesta etapa; avaliar com dados reais.
- [Uma chamada de transações por conta `BANK`, com paginação] → Aceito; sem cache nesta etapa.
- [Conta ou transação sem `currencyCode` ou valor nulo] → Fica fora do detalhe e dos totais.
- [Total parcial parece completo] → `partial` e `errors` explícitos na resposta.
