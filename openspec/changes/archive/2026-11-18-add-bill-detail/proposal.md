# Proposal

## Why

`GET /owners/{owner}/accounts/{accountId}/bills` lista as faturas de um cartão só com vencimento, fechamento e valor total. Não há como ver o que compõe uma fatura: as compras, os encargos (IOF, juros) e os pagamentos já feitos. A Pluggy já devolve esses dados (`GET /bills/{id}` e `GET /v2/transactions` filtrado por fatura), então um endpoint de detalhe os entrega num lugar só.

## What Changes

- Novo endpoint `GET /bill/{id}?owner=<dono>&accountId=<id da conta do cartão>`. `owner` e `accountId` são obrigatórios, e a ausência de qualquer um responde 400 no formato padrão de erro. O `id` é o `bills[].id` que a listagem de faturas já devolve.
- A API consulta a Pluggy com as credenciais do `owner`:
  - `GET /bills/{id}` para o resumo da fatura, os encargos e os pagamentos;
  - `GET /v2/transactions` com `accountId` e `billId` para as compras da fatura, percorrendo todas as páginas.
- Resposta 200: `owner`, `accountId`, `bill` e `transactions`.
  - `bill`: `id`, `dueDate`, `closingDate`, `totalAmount`, `currencyCode`, `minimumPaymentAmount`, `allowsInstallments`, `financeCharges` (`id`, `type`, `amount`, `currencyCode`, `additionalInfo`) e `payments` (`id`, `valueType`, `paymentDate`, `paymentMode`, `amount`, `currencyCode`).
  - `transactions`: `id`, `date`, `description`, `amount`, `currencyCode`, `category`, `type`, `status` e `installment` (`number` e `total`, só em compra parcelada), da data mais recente para a mais antiga.
  - Datas em `yyyy-mm-dd`. Os campos que a Pluggy não informa ficam nulos, e `financeCharges` e `payments` sem dados viram lista vazia.
- Falhas: fatura inexistente na Pluggy responde 404, dono desconhecido responde o mesmo erro dos outros endpoints, e a falha da Pluggy ao buscar a fatura ou as compras derruba a requisição inteira com o tratamento de erro existente, sem resposta parcial.
- O endpoint fica atrás da mesma chave de API e do mesmo CORS dos demais, sem configuração nova.
- Fora de escopo: edição ou pagamento de fatura, cache, paginação da resposta e qualquer mudança nos endpoints existentes.

## Capabilities

### New Capabilities

### Modified Capabilities
- `pluggy-financial-data`: ganha o detalhe de uma fatura de cartão (`GET /bill/{id}`), com resumo, encargos, pagamentos e compras.

## Impact

- Código: `PluggyApi` e `PluggyClient` (`retrieveBill` e listagem de transações por `billId`), `PluggyBillDetail` em `client.dto`, `BillDetail` e o resultado do detalhe em `domain`, `BillService` e `TransactionService` (compras por fatura, todas as páginas), `PluggyMapper`, `BillDetailResponse` e DTOs relacionados em `resource.dto`, `ResponseMapper` e `FinancialDataController`.
- API: nova rota `GET /bill/{id}`. As rotas existentes não mudam, e o `GET .../bills` continua devolvendo as datas como hoje.
- Pluggy: uma chamada `GET /bills/{id}` e uma ou mais chamadas `GET /v2/transactions` (uma por página) por requisição.
- Controle de acesso e erros: nenhuma mudança. `api-access-control` e `pluggy-error-handling` já valem para a rota nova.
- README: documentar a rota.
- Limite conhecido: a API não confere se `accountId` é a conta da fatura. Com uma conta que não é a da fatura, a Pluggy devolve a fatura e nenhuma compra, e a resposta sai 200 com `transactions` vazio.
- Dados reais: o filtro `billId` em `GET /v2/transactions` e os campos de `GET /bills/{id}` vêm da documentação e ainda não foram vistos em dados reais.
