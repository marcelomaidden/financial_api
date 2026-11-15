# Proposal

## Why

Hoje é preciso chamar `/accounts` e `/transactions` e somar tudo à mão para saber quanto há em conta e quanto entrou no mês. Um resumo único dos dois donos (`marcelo` e `luana`) evita essa conta manual.

## What Changes

- Novo endpoint `GET /summary` com a seção `incomes`, composta por duas partes:
  - `available`: saldo das contas bancárias (`type = BANK`). Traz `total` (`{value, currencyCode}`) e `detail` por dono, com `id`, `name`, `balance` e `currencyCode` de cada conta.
  - `transactions`: entradas de dinheiro (`type = CREDIT`) do mês corrente nas contas `BANK`. Traz `total` e `detail` por dono, com `id`, `description`, `amount`, `currencyCode` e `accountId`.
- Só BRL. Contas e transações em outra moeda ficam fora de `incomes`.
- Transferências entre os donos continuam contando como entrada.
- Falha de um dono não derruba a resposta: o dono com falha sai da parte afetada, entra em `errors` e `partial` é `true`.
- Fora de escopo: seção `expenses` (cartões de crédito), conversão entre moedas, filtro por dono e período configurável.

## Capabilities

### New Capabilities
- `financial-summary`: resumo consolidado dos saldos e das entradas do mês nas contas bancárias dos donos configurados.

### Modified Capabilities

## Impact

- Código: `SummaryService` (novo, reaproveitando `AccountService.getAccounts` e `TransactionService`), `FinancialDataController` (nova rota) e novos DTOs em `resource/dto`. `OwnerFanOut` não muda.
- API: nova rota `GET /summary`. Rotas existentes não mudam.
- Pluggy: além de `GET /accounts`, passa a chamar `GET /transactions` por conta `BANK` de cada dono.
- README: documentar a rota.
