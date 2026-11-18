# Tasks

## 1. Janela e filtro

- [x] 1.1 Criar a janela de datas em `BillDetail` (decisão 2): `transactionsWindow()` devolvendo `Optional` com `from` e `to`, constantes `WINDOW_SLACK_DAYS = 10` e `DUE_TO_CLOSING_DAYS = 10`. Verificar com um teste de domínio (no molde de `BillingCycleTest`) cobrindo: fechamento 2026-11-05 dá 2026-09-25 a 2026-11-15; sem fechamento e vencimento 2026-11-15 dá 2026-09-25 a 2026-11-15; fechamento no fim de mês (2026-03-31 dá início em 2026-02-18); sem as duas datas dá vazio. `./gradlew test` passando
- [x] 1.2 Em `BillService.getBillDetail`, trocar `getAllByBill` por `transactionService.getAllTransactions(owner, accountId, from, to)` com a janela (ou datas nulas sem janela) e filtrar `id.equals(tx.billId())` antes da ordenação (decisões 1 e 3). Verificar com `BillServiceTest`: janela repassada a partir do fechamento e a partir do vencimento; sem datas chama com `null`, `null`; transações de outra fatura e sem `billId` ficam de fora; parcela 2/4 com `date` no ciclo entra; os casos atuais (dono desconhecido, fatura inexistente sem buscar compras, falha nas compras sem resultado, ordem, empate, data nula, parcela, lista vazia) seguem passando. `./gradlew test` passando

## 2. Remoção do caminho por `billId`

- [x] 2.1 Remover `PluggyApi.listTransactionsByBill`, `PluggyClient.listTransactionsByBill`, `TransactionService.getByBill` e `getAllByBill`, e os testes `byBill*` de `TransactionServiceTest`. Verificar que `grep -rn "billId" src/main/java/com/mfaraujo/financialapi/client/PluggyApi.java` não encontra nada e que `./gradlew build` passa
- [x] 2.2 Atualizar a seção "Detalhe de uma fatura" do README: as compras vêm de `GET /v2/transactions` por datas, numa janela derivada da fatura (fechamento menos 1 mês e 10 dias até o fechamento mais 10 dias, ou pelo vencimento), filtradas por `billId` na API. Registrar o limite de uma compra com `date` fora da janela. Verificar que o texto não cita mais o filtro `billId` na Pluggy e que `openspec validate --all` passa

## 3. Dados reais

- [ ] 3.1 Com as credenciais, chamar `GET /bill/{id}?owner=marcelo&accountId=<cartão>` para a fatura `73595646-5e0b-4dab-9179-56244b7f8d30` e conferir que a resposta é 200 e traz as parcelas 1/2 (2026-10-30) e 2/4 (2026-10-21). Repetir para uma fatura com compra parcelada de um cartão da luana e conferir que as parcelas 2 em diante aparecem. Registrar no design.md, em Risks, o resultado e a instituição de cada uma
