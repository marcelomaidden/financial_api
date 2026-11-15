# Spec Delta

## ADDED Requirements

### Requirement: Compras parceladas trazem a parcela
O sistema SHALL incluir `installment`, com `number` e `total`, em cada compra de `expenses.transactions` que seja uma parcela, e SHALL omitir o campo nas demais compras.

#### Scenario: Parcela de uma compra parcelada
- **WHEN** uma compra pendente sem fatura é a parcela 3 de 10
- **THEN** a compra traz `installment` com `number` 3 e `total` 10

#### Scenario: Compra à vista
- **WHEN** uma compra pendente sem fatura não tem informação de parcela
- **THEN** a compra não traz o campo `installment`

#### Scenario: Informação de parcela incompleta
- **WHEN** a Pluggy informa só o número da parcela, sem o total
- **THEN** a compra não traz o campo `installment`

## MODIFIED Requirements

### Requirement: Transactions lista compras ainda não faturadas
O sistema SHALL compor `expenses.transactions` com transações de tipo `DEBIT`, `status` `PENDING` e sem `billId` em `creditCardMetadata`, das contas `CREDIT` em BRL. Cada compra SHALL ter `id`, `accountId`, `description`, `currencyCode`, `amount`, `date` e `category`. O sistema SHALL buscar as compras de cada cartão pela data em que a transação chegou à Pluggy, e não pela data da compra, e SHALL percorrer todas as páginas. `dateFrom` e `dateTo` SHALL NOT afetar esta parte.

#### Scenario: Compra pendente sem fatura
- **WHEN** um cartão tem uma transação `DEBIT`, `PENDING` e sem `billId`
- **THEN** a transação aparece em `expenses.transactions` do dono

#### Scenario: Compra já faturada ou liquidada
- **WHEN** uma transação tem `billId`, ou tem `status` diferente de `PENDING`, ou é do tipo `CREDIT`
- **THEN** ela não aparece em `expenses.transactions`

#### Scenario: Compra do mês anterior ainda sem fatura
- **WHEN** uma compra pendente sem fatura é do mês anterior e chegou à Pluggy dentro da janela do cartão
- **THEN** a compra aparece em `expenses.transactions`

#### Scenario: Parcela com data de compra antiga
- **WHEN** uma parcela pendente sem fatura tem `date` de meses atrás e chegou à Pluggy dentro da janela do cartão
- **THEN** a parcela aparece em `expenses.transactions`

#### Scenario: Janela derivada da fatura mais recente do cartão
- **WHEN** hoje é 15/11, a fatura mais recente do cartão fecha em 4/11 e vence em 16/11
- **THEN** o sistema considera as compras que chegaram à Pluggy a partir de 25/10, e só as com `date` até 21/12, que é o vencimento da fatura aberta (16/12) mais 5 dias de folga

#### Scenario: Parcela da fatura aberta com data no vencimento
- **WHEN** uma parcela pendente sem fatura tem `date` em 16/12 e o vencimento da fatura aberta é 16/12
- **THEN** a parcela aparece em `expenses.transactions`

#### Scenario: Parcela futura de outra fatura
- **WHEN** uma parcela pendente sem fatura tem `date` depois do vencimento da fatura aberta mais a folga
- **THEN** a parcela não aparece em `expenses.transactions`

#### Scenario: Cartão sem fatura devolvida
- **WHEN** a Pluggy não devolve nenhuma fatura do cartão
- **THEN** o sistema considera as compras que chegaram a partir do dia 1 do mês anterior, com `date` até hoje

#### Scenario: Falha na consulta de faturas
- **WHEN** a consulta de faturas de um cartão falha
- **THEN** as compras desse cartão usam a janela do cartão sem fatura devolvida

#### Scenario: Datas informadas pelo cliente
- **WHEN** o cliente chama `GET /summary` com `dateFrom` e `dateTo`
- **THEN** `expenses.transactions` é igual ao de uma chamada sem datas
