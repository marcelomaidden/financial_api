# Spec Delta

## ADDED Requirements

### Requirement: Listagem consolidada de transações
O sistema SHALL expor `GET /transactions` retornando uma lista única com as transações dos donos e contas pedidos. O endpoint SHALL aceitar os parâmetros opcionais `owner`, `accountId`, `dateFrom` e `dateTo`. Sem `owner`, SHALL consultar todos os donos, e sem `accountId`, todas as contas do dono, bancárias e de cartão. Cada transação SHALL ter `owner`, `accountId`, `id`, `date`, `description`, `amount`, `currencyCode`, `type`, `status` e `category` quando disponíveis.

#### Scenario: Todos os donos e contas
- **WHEN** o cliente chama `GET /transactions` sem `owner` e sem `accountId`
- **THEN** a resposta contém as transações de todas as contas de todos os donos, cada uma com o `owner` e o `accountId` de origem

#### Scenario: Dono específico
- **WHEN** o cliente chama `GET /transactions?owner=<nome>` com um dono configurado
- **THEN** a resposta contém só as transações das contas desse dono

#### Scenario: Conta específica
- **WHEN** o cliente chama `GET /transactions?owner=<nome>&accountId=<id>` com uma conta desse dono
- **THEN** a resposta contém só as transações dessa conta

#### Scenario: Conta sem dono
- **WHEN** o cliente chama `GET /transactions?accountId=<id>` sem `owner`
- **THEN** o sistema responde 400 no formato padrão de erro, sem consultar a Pluggy

#### Scenario: Dono desconhecido
- **WHEN** o cliente chama `GET /transactions?owner=<nome>` com um dono que não está na configuração
- **THEN** o sistema responde 404 sem consultar a Pluggy

#### Scenario: Conta de outro dono ou inexistente
- **WHEN** a Pluggy responde 404 para a conta informada com as credenciais do dono informado
- **THEN** o sistema responde 404 ao cliente

#### Scenario: Conta com várias páginas
- **WHEN** uma conta tem mais transações do que cabem em uma página da Pluggy
- **THEN** a resposta contém as transações de todas as páginas, sem exigir cursor do cliente

### Requirement: Período da listagem consolidada
O sistema SHALL consultar, em `GET /transactions`, as transações de `dateFrom` a `dateTo` (`yyyy-mm-dd`). Sem `dateTo`, o sistema SHALL usar hoje no fuso `America/Sao_Paulo`. Sem `dateFrom`, SHALL usar o dia 1 do mês de `dateTo`.

#### Scenario: Sem datas
- **WHEN** o cliente chama `GET /transactions` sem `dateFrom` e `dateTo`
- **THEN** a consulta vai do dia 1 do mês corrente até hoje

#### Scenario: Período informado
- **WHEN** o cliente chama `GET /transactions` com `dateFrom` e `dateTo`
- **THEN** a consulta vai de `dateFrom` a `dateTo`

#### Scenario: Só dateFrom
- **WHEN** o cliente informa `dateFrom` e não informa `dateTo`
- **THEN** a consulta vai de `dateFrom` até hoje

#### Scenario: Só dateTo
- **WHEN** o cliente informa `dateTo` e não informa `dateFrom`
- **THEN** a consulta vai do dia 1 do mês de `dateTo` até `dateTo`

### Requirement: Ordem da listagem consolidada
O sistema SHALL ordenar a lista de `GET /transactions` por `date` da mais recente para a mais antiga e, no empate de data, por `owner` e depois por `accountId`, em ordem alfabética. Dentro da mesma conta e data, o sistema SHALL manter a ordem em que a Pluggy devolveu as transações.

#### Scenario: Contas e donos diferentes
- **WHEN** a lista tem transações de datas diferentes, de vários donos e contas
- **THEN** elas aparecem da data mais recente para a mais antiga

#### Scenario: Empate de data
- **WHEN** duas transações de donos diferentes têm a mesma `date`
- **THEN** a do dono que vem antes em ordem alfabética aparece primeiro

#### Scenario: Mesma conta e data
- **WHEN** duas transações da mesma conta têm a mesma `date`
- **THEN** elas aparecem na ordem em que a Pluggy as devolveu

### Requirement: Falha parcial na listagem consolidada
O sistema SHALL, quando a consulta de uma conta falhar em `GET /transactions`, manter as transações das demais, listar a falha em `errors` com `owner`, `accountId`, `status` e `message` e marcar `partial` como `true`. Sem `owner`, a falha ao listar as contas de um dono SHALL entrar em `errors` só com o `owner`. O sistema SHALL responder 502 no formato padrão de erro quando nenhuma consulta tiver sucesso.

#### Scenario: Uma conta falha
- **WHEN** a consulta de uma conta falha e a de outra tem sucesso
- **THEN** a resposta é 200, `partial` é `true`, `errors` identifica o dono e a conta, e a lista traz as transações da conta que teve sucesso

#### Scenario: Um dono falha ao listar as contas
- **WHEN** o cliente não informa `owner` e a listagem das contas de um dono falha
- **THEN** a resposta é 200, `partial` é `true`, `errors` identifica o dono sem conta, e a lista traz as transações dos outros donos

#### Scenario: Nada tem sucesso
- **WHEN** todas as consultas falham
- **THEN** o sistema responde 502 no formato padrão de erro

#### Scenario: Conta informada falha
- **WHEN** o cliente informa `accountId` e a consulta dessa conta falha
- **THEN** o sistema responde com o erro normal mapeado para essa falha

#### Scenario: Listagem das contas de um dono informado falha
- **WHEN** o cliente informa `owner` sem `accountId` e a listagem das contas dele falha
- **THEN** o sistema responde com o erro normal mapeado para essa falha

#### Scenario: Sem falhas
- **WHEN** todas as consultas têm sucesso
- **THEN** `partial` é `false` e `errors` é uma lista vazia
