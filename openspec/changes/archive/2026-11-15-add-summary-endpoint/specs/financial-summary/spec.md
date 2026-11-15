# Spec Delta

## Purpose

Entrega um resumo consolidado do que os donos têm em contas bancárias e do que entrou no período, em BRL, detalhado por dono.

## ADDED Requirements

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
O sistema SHALL, quando a consulta de um dono falhar e a de outro tiver sucesso, responder 200, excluir o dono com falha das partes afetadas, listar o dono em `errors` com `status` e `message` e marcar `partial` como `true`. Falha na consulta de contas SHALL excluir o dono de `available` e de `transactions`. Falha na consulta de transações de uma conta SHALL excluir o dono apenas de `transactions`. O sistema SHALL responder 502 no formato padrão de erro quando a consulta de contas falhar para todos os donos.

#### Scenario: Um dono falha nas contas
- **WHEN** a consulta de contas falha para um dono e tem sucesso para o outro
- **THEN** a resposta é 200, `partial` é `true`, `errors` identifica o dono com falha e os totais somam só o dono com sucesso

#### Scenario: Um dono falha nas transações
- **WHEN** a consulta de transações de uma conta falha para um dono
- **THEN** a resposta é 200, `partial` é `true`, `errors` identifica o dono, ele continua em `available` e fica fora de `transactions`

#### Scenario: Todos os donos falham
- **WHEN** a consulta de contas falha para todos os donos
- **THEN** o sistema responde 502 no formato padrão de erro
