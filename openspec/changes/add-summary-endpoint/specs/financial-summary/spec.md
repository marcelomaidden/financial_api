# Spec Delta

## Purpose

Entrega um resumo consolidado do que os donos têm em contas bancárias e do que entrou no mês, em BRL, detalhado por dono.

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

### Requirement: Transactions lista entradas do mês
O sistema SHALL compor `incomes.transactions` com transações de tipo `CREDIT` em BRL, das contas `BANK`, do dia 1 do mês corrente até hoje (fuso `America/Sao_Paulo`). Cada transação SHALL ter `id`, `description`, `amount`, `currencyCode` e `accountId`, e `total.value` SHALL somar o `amount` delas. O sistema SHALL percorrer todas as páginas de cada conta.

#### Scenario: Entrada do mês
- **WHEN** uma conta `BANK` tem uma transação `CREDIT` no mês corrente
- **THEN** a transação aparece no detalhe do dono e entra em `transactions.total`

#### Scenario: Saída ou fora do mês
- **WHEN** uma transação é `DEBIT` ou é de outro mês
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
