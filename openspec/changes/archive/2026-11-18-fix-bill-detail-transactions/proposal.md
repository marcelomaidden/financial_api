# Proposal

## Why

`GET /bill/{id}` falha ao buscar as compras da fatura. A Pluggy recusa o parâmetro `billId` em `GET /v2/transactions` com "property billId should not exist". Pela especificação da Pluggy, `billId` só existe no `GET /transactions` legado, que está depreciado e sai do ar em 2026-12-31. O `GET /v2/transactions` aceita apenas `accountId`, `ids`, `dateFrom`, `dateTo`, `createdAtFrom` e `after`. Por isso o detalhe da fatura não funciona com dados reais, e a tarefa 3.2 da `add-bill-detail` (conferência com a Pluggy real) não chegou a ser feita.

## What Changes

- As compras da fatura passam a vir de `GET /v2/transactions` com `accountId`, `dateFrom` e `dateTo` (todas as páginas). O backend guarda só as transações cujo `creditCardMetadata.billId` é o `id` da fatura. A chamada à Pluggy deixa de enviar `billId`.
- A janela de datas sai da fatura buscada antes, em `GET /bills/{id}`: de `closingDate` menos 1 mês menos uma folga até `closingDate` mais uma folga.
- O contrato de `GET /bill/{id}` não muda: os mesmos parâmetros, os mesmos campos, a mesma ordem e os mesmos erros. O frontend não muda.
- Sai `PluggyApi.listTransactionsByBill` e o método correspondente em `PluggyClient`. A busca usa o `listTransactions` que já existe.

Evidência com dados reais (um cartão do marcelo, fatura `73595646-...`, `billForecastDate` `2026-11`):
- Parcela 1/2: compra em 2026-10-28, `billPostDate` 2026-10-30.
- Parcela 2/4: compra em 2026-09-18, `billPostDate` 2026-10-21, `date` `2026-10-21T03:00:00.000Z`.

O `date` da parcela é o `billPostDate`, e não a data da compra. Por isso uma janela do tamanho de um ciclo da fatura alcança as parcelas de compras antigas, sem paginar 12 meses de histórico.

Decisões a confirmar no design:
1. **Tamanho da folga.** A proposta é 10 dias antes e 10 dias depois, numa janela de cerca de 50 dias. O resumo usa 10 antes e 5 depois.
2. **Fatura sem `closingDate`.** O campo é opcional na Pluggy. A proposta é ancorar no `dueDate`: de `dueDate` menos 1 mês menos 20 dias até `dueDate`.
3. **Amostra de uma instituição só.** Os dois exemplos são da mesma fatura. Se outra instituição usar a data da compra no `date`, as parcelas antigas ficam fora da janela e somem sem erro. É preciso decidir se isso fica registrado como risco com conferência numa fatura da luana, ou se a janela já nasce mais larga.

Fora de escopo: o endpoint legado `GET /transactions`, cache, validação de `accountId` contra a fatura e mudanças no frontend.

## Capabilities

### New Capabilities

### Modified Capabilities
- `pluggy-financial-data`: o requisito "Compras da fatura no detalhe" passa a dizer que a seleção das compras é feita pelo `billId` da transação, dentro de uma janela de datas derivada da fatura. Isso registra o limite conhecido: uma compra com `date` fora da janela não aparece.

## Impact

- **Código:** `PluggyApi` e `PluggyClient` (sai `listTransactionsByBill`), `TransactionService.getAllByBill` (janela por data e filtro por `billId`), `BillService.getBillDetail` (repassa as datas da fatura), testes de `TransactionServiceTest` e `BillServiceTest`.
- **Pluggy:** uma chamada `GET /bills/{id}` e uma ou mais chamadas `GET /v2/transactions?accountId&dateFrom&dateTo` por requisição. Com a janela de cerca de 50 dias, deve ser uma página na maioria dos cartões.
- **API e frontend:** sem mudança de contrato.
- **README:** a seção do detalhe da fatura deixa de citar o filtro `billId` na Pluggy e passa a descrever a janela e o limite.
- **Limite conhecido:** continua igual. Um `accountId` que não é a conta da fatura responde 200 com `transactions` vazio.
