# financial-summary Specification

## Purpose

Entrega um resumo consolidado do que os donos têm em contas bancárias e do que entrou no período, em BRL, detalhado por dono.

## Requirements

### Requirement: Resumo de incomes
O sistema SHALL expor `GET /summary` retornando a seção `incomes` com `available` e `transactions`. Cada parte SHALL conter `total`, com `value` e `currencyCode`, e `detail`, um mapa de nome do dono para a lista dele.

#### Scenario: Resumo dos dois donos
- **WHEN** o cliente chama `GET /summary` e a consulta tem sucesso para todos os donos
- **THEN** a resposta é 200, `incomes.available.detail` e `incomes.transactions.detail` têm uma entrada por dono e `partial` é `false`

### Requirement: Available soma contas bancárias
O sistema SHALL compor `incomes.available` apenas com contas `BANK` em BRL. Cada conta no detalhe SHALL ter `id`, `name`, `balance` e `currencyCode`, e `total.value` SHALL somar o `balance` dessas contas de todos os donos.

#### Scenario: Soma dos saldos
- **WHEN** os donos têm contas `BANK` em BRL
- **THEN** `available.total.value` é a soma dos saldos e `available.total.currencyCode` é `BRL`

#### Scenario: Outros tipos de conta
- **WHEN** um dono tem conta de tipo diferente de `BANK`
- **THEN** essa conta não aparece em `incomes`

#### Scenario: Outra moeda
- **WHEN** um dono tem conta `BANK` em moeda diferente de BRL
- **THEN** essa conta não aparece no detalhe nem entra no total

### Requirement: Transactions lista entradas do período
O sistema SHALL compor `incomes.transactions` com transações de tipo `CREDIT` em BRL, das contas `BANK`, dentro do período pedido. O período SHALL ser definido pelos parâmetros opcionais `dateFrom` e `dateTo` (`yyyy-mm-dd`). Sem `dateTo`, o sistema SHALL usar a data de hoje no fuso `America/Sao_Paulo`. Sem `dateFrom`, o sistema SHALL usar o dia 1 do mês de `dateTo`. Cada transação SHALL ter `id`, `description`, `amount`, `currencyCode`, `date` e `accountId`, e `total.value` SHALL somar o `amount` delas. O sistema SHALL percorrer todas as páginas de cada conta. O período SHALL afetar só `transactions`, e `available` SHALL continuar sendo o saldo atual.

#### Scenario: Entrada do mês corrente
- **WHEN** o cliente chama `GET /summary` sem `dateFrom` e `dateTo`, e uma conta `BANK` tem uma transação `CREDIT` no mês corrente
- **THEN** a transação aparece no detalhe do dono, com `date`, e entra em `transactions.total`

#### Scenario: Período informado
- **WHEN** o cliente chama `GET /summary` com `dateFrom` e `dateTo`
- **THEN** o sistema consulta as transações de `dateFrom` a `dateTo`

#### Scenario: Só dateFrom
- **WHEN** o cliente informa `dateFrom` e não informa `dateTo`
- **THEN** o período vai de `dateFrom` até hoje

#### Scenario: Só dateTo
- **WHEN** o cliente informa `dateTo` e não informa `dateFrom`
- **THEN** o período vai do dia 1 do mês de `dateTo` até `dateTo`

#### Scenario: Saída ou fora do período
- **WHEN** uma transação é `DEBIT` ou está fora do período
- **THEN** ela não aparece em `incomes.transactions`

#### Scenario: Transferência entre donos
- **WHEN** um dono recebe uma transação `CREDIT` vinda do outro dono
- **THEN** a transação aparece como entrada de quem recebeu

### Requirement: Falha parcial no resumo
O sistema SHALL, quando a consulta de um dono falhar e a de outro tiver sucesso, responder 200, excluir o dono com falha das partes afetadas, listar o dono em `errors` com `status` e `message` e marcar `partial` como `true`. Falha na consulta de contas SHALL excluir o dono de todas as partes de `incomes` e de `expenses`. Falha na consulta de transações de uma conta bancária SHALL excluir o dono apenas de `incomes.transactions` e de `expenses.accountDebits`, com um único erro. Falha na consulta de faturas SHALL excluir o dono apenas de `expenses.owed`. Falha na consulta de transações de um cartão SHALL excluir o dono apenas de `expenses.transactions`. O sistema SHALL responder 502 no formato padrão de erro quando a consulta de contas falhar para todos os donos.

#### Scenario: Um dono falha nas contas
- **WHEN** a consulta de contas falha para um dono e tem sucesso para o outro
- **THEN** a resposta é 200, `partial` é `true`, `errors` identifica o dono com falha, e os totais de `incomes` e de `expenses` somam só o dono com sucesso

#### Scenario: Um dono falha nas transações
- **WHEN** a consulta de transações de uma conta bancária falha para um dono
- **THEN** a resposta é 200, `partial` é `true`, `errors` identifica o dono uma vez, ele continua em `incomes.available` e fica fora de `incomes.transactions` e de `expenses.accountDebits`

#### Scenario: Um dono falha nas faturas
- **WHEN** a consulta de faturas falha para um dono
- **THEN** a resposta é 200, `partial` é `true`, `errors` identifica o dono, ele continua em `expenses.transactions` e fica fora de `expenses.owed`

#### Scenario: Um dono falha nas compras do cartão
- **WHEN** a consulta de transações de um cartão falha para um dono
- **THEN** a resposta é 200, `partial` é `true`, `errors` identifica o dono, ele continua em `expenses.owed` e fica fora de `expenses.transactions`

#### Scenario: Todos os donos falham
- **WHEN** a consulta de contas falha para todos os donos
- **THEN** o sistema responde 502 no formato padrão de erro

### Requirement: Resumo de expenses
O sistema SHALL devolver em `GET /summary` a seção `expenses`, com as partes `owed`, `transactions` e `accountDebits`. Cada parte SHALL conter `total`, com `value` e `currencyCode`, e `detail`, um mapa de nome do dono para a lista dele.

#### Scenario: Resumo dos dois donos
- **WHEN** o cliente chama `GET /summary` e a consulta tem sucesso para todos os donos
- **THEN** `expenses.owed.detail`, `expenses.transactions.detail` e `expenses.accountDebits.detail` têm uma entrada por dono e `incomes` continua na resposta

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

### Requirement: AccountDebits lista saídas do período
O sistema SHALL compor `expenses.accountDebits` com transações de tipo `DEBIT` em BRL, com valor informado, das contas `BANK` em BRL, no mesmo período de `incomes.transactions`. Cada saída SHALL ter `id`, `description`, `amount`, `currencyCode`, `date` e `accountId`, e `total.value` SHALL somar o `amount` delas. O sistema SHALL usar a mesma consulta de transações de cada conta que alimenta `incomes.transactions`.

#### Scenario: Saída do mês corrente
- **WHEN** o cliente chama `GET /summary` sem `dateFrom` e `dateTo`, e uma conta `BANK` tem uma transação `DEBIT` em BRL no mês corrente
- **THEN** a transação aparece em `expenses.accountDebits` do dono, com `date` e `accountId`, e entra no total

#### Scenario: Período informado
- **WHEN** o cliente chama `GET /summary` com `dateFrom` e `dateTo`
- **THEN** `expenses.accountDebits` e `incomes.transactions` cobrem o mesmo intervalo

#### Scenario: Entrada, outra moeda ou sem valor
- **WHEN** uma transação de conta `BANK` é `CREDIT`, está em moeda diferente de BRL ou não tem `amount`
- **THEN** ela não aparece em `expenses.accountDebits`

#### Scenario: Cartão de crédito
- **WHEN** um dono tem conta `CREDIT`
- **THEN** as transações dessa conta não aparecem em `expenses.accountDebits`

#### Scenario: Uma consulta por conta
- **WHEN** o resumo monta `incomes.transactions` e `expenses.accountDebits`
- **THEN** o sistema consulta as transações de cada conta `BANK` uma única vez

### Requirement: Saídas com valor positivo
O sistema SHALL devolver o `amount` de cada saída de `expenses.accountDebits` como valor positivo, seja qual for o sinal informado pela Pluggy.

#### Scenario: Débito com sinal negativo
- **WHEN** a Pluggy devolve um `DEBIT` em BRL com `amount` de -12.50
- **THEN** a saída traz `amount` 12.50 e o total soma 12.50

### Requirement: Saídas são o débito bruto
O sistema SHALL NOT excluir de `expenses.accountDebits` pagamentos de fatura de cartão nem transferências entre contas dos donos.

#### Scenario: Pagamento de fatura
- **WHEN** uma conta `BANK` tem um `DEBIT` de pagamento de fatura de cartão no período
- **THEN** a transação aparece em `expenses.accountDebits` e entra no total
