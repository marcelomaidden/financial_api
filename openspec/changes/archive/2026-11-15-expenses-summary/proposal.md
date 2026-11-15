# Proposal

## Why

`GET /summary` mostra o que entrou nas contas (`incomes`), mas não o que saiu pelo cartão de crédito. Para saber quanto já virou fatura e quanto ainda vai virar, hoje é preciso chamar `/credit-cards`, `/bills` e `/transactions` à mão e conferir `billId` e `status` de cada compra.

## What Changes

- `GET /summary` passa a devolver também a seção `expenses`, no mesmo formato de `incomes` (`total` com `value` e `currencyCode`, e `detail` por dono). A seção tem duas partes:
  - `owed`: faturas das contas `CREDIT` com `dueDate` no período. Cada fatura traz `id`, `accountId`, `name` do cartão, `dueDate`, `totalAmount` e `currencyCode`. O total soma `totalAmount`.
  - `transactions`: compras ainda não faturadas, ou seja, transações `DEBIT` com `status = PENDING` e sem `billId` em `creditCardMetadata`. Cada compra traz `id`, `accountId`, `description`, `currencyCode`, `amount`, `date` e `category`, e traz `amountInAccountCurrency` quando a moeda não é BRL. O total soma `amount` em BRL e `amountInAccountCurrency` nas demais moedas.
- Período de `owed`: `dueDate` entre `dateFrom` e o último dia do mês de `dateTo`. Sem `dateFrom`, o dia 1 do mês de `dateTo`. Sem `dateTo`, hoje.
- Período de `transactions`: de `dateFrom` até `dateTo`. Sem `dateFrom`, o dia 1 do mês anterior ao de `dateTo`, porque uma compra do mês passado ainda sem fatura pertence à fatura aberta.
- `owed` e `transactions` só consideram contas `CREDIT` em BRL.
- A falha de um dono segue a regra de `incomes`: o dono sai da parte afetada, entra em `errors` e `partial` é `true`.
- A rota `GET /summary` passa a constar no README, com `incomes` e `expenses`.
- Fora de escopo: saber se uma fatura já foi paga (a Pluggy não informa), conversão entre moedas, cartões com conta em outra moeda, filtro por dono e fatura aberta que a instituição ainda não devolve.

## Capabilities

### New Capabilities

### Modified Capabilities
- `financial-summary`: o resumo passa a incluir `expenses` (`owed` e `transactions`), e a regra de falha parcial passa a cobrir as novas partes.

## Impact

- Código: `ExpensesSummaryService` (novo), `SummaryService` (junta a seção e calcula os períodos), `BillService` (todas as páginas de faturas), `PluggyTransaction`, `Transaction` e `PluggyMapper` (novos campos `billId` e `amountInAccountCurrency`), `Account` (regra de cartão em BRL), DTOs novos em `resource.dto` e `SummaryResponse` com o campo `expenses`.
- API: `GET /summary` ganha o campo `expenses`. Os campos existentes não mudam.
- Pluggy: para cada cartão, chama `GET /bills` (todas as páginas) e `GET /v2/transactions`. O número de chamadas cresce, e o limite de requisições (429) é um risco.
- README: documentar `GET /summary`.
- Ordem: depende da `split-summary-service`, já arquivada.
