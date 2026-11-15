# Spec Delta

## ADDED Requirements

### Requirement: Resumo de expenses
O sistema SHALL devolver em `GET /summary` a seção `expenses`, com as partes `owed` e `transactions`. Cada parte SHALL conter `total`, com `value` e `currencyCode`, e `detail`, um mapa de nome do dono para a lista dele.

#### Scenario: Resumo dos dois donos
- **WHEN** o cliente chama `GET /summary` e a consulta tem sucesso para todos os donos
- **THEN** `expenses.owed.detail` e `expenses.transactions.detail` têm uma entrada por dono e `incomes` continua na resposta

### Requirement: Owed soma faturas com vencimento no período
O sistema SHALL compor `expenses.owed` com as faturas em BRL das contas `CREDIT` em BRL cujo `dueDate` esteja entre `dateFrom` e o último dia do mês de `dateTo`. Cada fatura SHALL ter `id`, `accountId`, `name` do cartão, `dueDate`, `totalAmount` e `currencyCode`, e `total.value` SHALL somar o `totalAmount`. O sistema SHALL percorrer todas as páginas de faturas de cada cartão.

#### Scenario: Fatura que vence depois de hoje no mesmo mês
- **WHEN** hoje é dia 15 e um cartão tem fatura com vencimento no dia 20 do mesmo mês
- **THEN** a fatura aparece em `expenses.owed` e entra no total

#### Scenario: Sem datas informadas
- **WHEN** o cliente chama `GET /summary` sem `dateFrom` e `dateTo`
- **THEN** `dateTo` é hoje no fuso `America/Sao_Paulo`, `dateFrom` é o dia 1 do mês de hoje, e o período de vencimento termina no último dia desse mês

#### Scenario: Fatura fora do período
- **WHEN** uma fatura vence no mês seguinte ao de `dateTo`
- **THEN** ela não aparece em `expenses.owed`

#### Scenario: Conta que não é cartão ou fatura em outra moeda
- **WHEN** um dono tem conta `BANK`, ou uma fatura em moeda diferente de BRL
- **THEN** a conta ou a fatura não aparece em `expenses.owed`

### Requirement: Transactions lista compras ainda não faturadas
O sistema SHALL compor `expenses.transactions` com transações de tipo `DEBIT`, `status` `PENDING` e sem `billId` em `creditCardMetadata`, das contas `CREDIT` em BRL. O período SHALL ir de `dateFrom` até `dateTo`. Sem `dateTo`, o sistema SHALL usar hoje. Sem `dateFrom`, SHALL usar o dia 1 do mês anterior ao de `dateTo`. Cada compra SHALL ter `id`, `accountId`, `description`, `currencyCode`, `amount`, `date` e `category`. O sistema SHALL percorrer todas as páginas de cada cartão.

#### Scenario: Compra pendente sem fatura
- **WHEN** um cartão tem uma transação `DEBIT`, `PENDING` e sem `billId`
- **THEN** a transação aparece em `expenses.transactions` do dono

#### Scenario: Compra já faturada ou liquidada
- **WHEN** uma transação tem `billId`, ou tem `status` diferente de `PENDING`, ou é do tipo `CREDIT`
- **THEN** ela não aparece em `expenses.transactions`

#### Scenario: Compra do mês anterior ainda sem fatura
- **WHEN** não há `dateFrom` e uma compra pendente sem fatura é do mês anterior
- **THEN** a compra aparece em `expenses.transactions`

### Requirement: Compras em moeda estrangeira trazem o valor na moeda da conta
O sistema SHALL incluir `amountInAccountCurrency` em cada compra de `expenses.transactions` cuja `currencyCode` não seja BRL, e SHALL omitir o campo nas compras em BRL. `expenses.transactions.total.value` SHALL somar o `amount` das compras em BRL e o `amountInAccountCurrency` das demais, em BRL.

#### Scenario: Compra em dólar
- **WHEN** uma compra pendente sem fatura tem `currencyCode` `USD` e `amountInAccountCurrency` informado
- **THEN** a compra traz `amountInAccountCurrency` e o total soma esse valor

#### Scenario: Compra em BRL
- **WHEN** uma compra pendente sem fatura tem `currencyCode` `BRL`
- **THEN** a compra não traz `amountInAccountCurrency` e o total soma o `amount`

#### Scenario: Moeda estrangeira sem valor na moeda da conta
- **WHEN** uma compra em moeda diferente de BRL não tem `amountInAccountCurrency`
- **THEN** a compra aparece no detalhe e não entra no total

## MODIFIED Requirements

### Requirement: Falha parcial no resumo
O sistema SHALL, quando a consulta de um dono falhar e a de outro tiver sucesso, responder 200, excluir o dono com falha das partes afetadas, listar o dono em `errors` com `status` e `message` e marcar `partial` como `true`. Falha na consulta de contas SHALL excluir o dono de todas as partes de `incomes` e de `expenses`. Falha na consulta de transações de uma conta bancária SHALL excluir o dono apenas de `incomes.transactions`. Falha na consulta de faturas SHALL excluir o dono apenas de `expenses.owed`. Falha na consulta de transações de um cartão SHALL excluir o dono apenas de `expenses.transactions`. O sistema SHALL responder 502 no formato padrão de erro quando a consulta de contas falhar para todos os donos.

#### Scenario: Um dono falha nas contas
- **WHEN** a consulta de contas falha para um dono e tem sucesso para o outro
- **THEN** a resposta é 200, `partial` é `true`, `errors` identifica o dono com falha, e os totais de `incomes` e de `expenses` somam só o dono com sucesso

#### Scenario: Um dono falha nas transações
- **WHEN** a consulta de transações de uma conta bancária falha para um dono
- **THEN** a resposta é 200, `partial` é `true`, `errors` identifica o dono, ele continua em `incomes.available` e fica fora de `incomes.transactions`

#### Scenario: Um dono falha nas faturas
- **WHEN** a consulta de faturas falha para um dono
- **THEN** a resposta é 200, `partial` é `true`, `errors` identifica o dono, ele continua em `expenses.transactions` e fica fora de `expenses.owed`

#### Scenario: Um dono falha nas compras do cartão
- **WHEN** a consulta de transações de um cartão falha para um dono
- **THEN** a resposta é 200, `partial` é `true`, `errors` identifica o dono, ele continua em `expenses.owed` e fica fora de `expenses.transactions`

#### Scenario: Todos os donos falham
- **WHEN** a consulta de contas falha para todos os donos
- **THEN** o sistema responde 502 no formato padrão de erro
