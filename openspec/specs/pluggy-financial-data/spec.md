# pluggy-financial-data Specification

## Purpose

Permite consultar, por uma API REST própria, contas, cartões de crédito, transações e faturas obtidos da Pluggy para mais de um dono, cada um com suas credenciais e seus itens configurados.

## Requirements

### Requirement: Donos configurados
O sistema SHALL ler de configuração uma lista de donos, cada um com nome único, `clientId`, `clientSecret` e ao menos um `itemId`, e SHALL consultar a Pluggy usando as credenciais de cada dono para os dados dele.

#### Scenario: Configuração inválida
- **WHEN** a aplicação inicia com lista de donos vazia, nome repetido, dono sem credencial ou dono sem `itemId`
- **THEN** a aplicação falha na inicialização indicando a propriedade inválida, sem exibir valores de credenciais

### Requirement: Autenticação por dono na Pluggy
O sistema SHALL autenticar na Pluggy separadamente para cada dono, com o `clientId` e o `clientSecret` dele, e SHALL enviar a chave desse dono em toda chamada feita em nome dele.

#### Scenario: Primeira chamada sem chave
- **WHEN** uma consulta é feita para um dono que ainda não possui chave válida
- **THEN** o sistema obtém uma chave na Pluggy com as credenciais desse dono antes de executar a consulta

#### Scenario: Chave reaproveitada
- **WHEN** duas consultas para o mesmo dono são feitas dentro do prazo de validade da chave
- **THEN** o sistema autentica uma única vez para esse dono

#### Scenario: Chaves independentes
- **WHEN** a chave de um dono expira ou é rejeitada
- **THEN** o sistema renova apenas a chave desse dono

#### Scenario: Chave rejeitada
- **WHEN** a Pluggy responde 401 a uma consulta de um dono
- **THEN** o sistema obtém uma nova chave para esse dono e repete a consulta uma vez

### Requirement: Segredos fora do código
O sistema SHALL ler `clientId` e `clientSecret` de variáveis de ambiente e SHALL NOT registrá-los em logs nem devolvê-los em respostas.

#### Scenario: Variável ausente
- **WHEN** a aplicação inicia sem o `clientId` ou o `clientSecret` de algum dono
- **THEN** a aplicação falha na inicialização com uma mensagem que indica qual propriedade falta, sem exibir valores

### Requirement: Listagem de contas por dono
O sistema SHALL expor `GET /accounts` retornando, para cada dono, as contas de todos os itens dele, agrupadas sob o nome do dono. O endpoint SHALL aceitar o parâmetro opcional `owner` para restringir a consulta a um dono. Cada conta SHALL ter identificador, tipo, subtipo, nome, saldo e moeda.

#### Scenario: Todos os donos
- **WHEN** o cliente chama `GET /accounts` sem parâmetros
- **THEN** a resposta contém uma entrada por dono, cada uma com o nome do dono e a lista das contas dele

#### Scenario: Dono específico
- **WHEN** o cliente chama `GET /accounts?owner=<nome>` com um dono configurado
- **THEN** a resposta contém apenas a entrada desse dono

#### Scenario: Dono desconhecido
- **WHEN** o cliente chama `GET /accounts?owner=<nome>` com um dono que não está na configuração
- **THEN** o sistema responde 404 sem consultar a Pluggy

### Requirement: Falha parcial entre donos
O sistema SHALL devolver os dados dos donos consultados com sucesso e, para cada dono que falhar, um campo de erro com `status` e `message` na entrada desse dono, sem descartar os demais. O sistema SHALL responder 200 quando ao menos um dono tiver sucesso.

#### Scenario: Um dono falha
- **WHEN** a consulta de `GET /accounts` falha para um dono e tem sucesso para o outro
- **THEN** a resposta é 200, a entrada do dono com sucesso contém as contas e a entrada do dono com falha contém o erro e uma lista de contas vazia

#### Scenario: Todos os donos falham
- **WHEN** a consulta falha para todos os donos consultados em `GET /accounts` ou `GET /credit-cards` sem filtro
- **THEN** o sistema responde 502 no formato padrão de erro

#### Scenario: Falha em consulta de um único dono
- **WHEN** o cliente restringe a consulta a um dono com o parâmetro `owner` e a Pluggy falha para ele
- **THEN** o sistema responde com o erro normal mapeado para essa falha

### Requirement: Listagem de cartões de crédito por dono
O sistema SHALL expor `GET /credit-cards` retornando, para cada dono, as contas do tipo cartão de crédito dos itens dele, com limite de crédito, limite disponível e datas de fechamento e vencimento quando a Pluggy as informar. O endpoint SHALL aceitar o parâmetro opcional `owner` e SHALL seguir as regras de falha parcial de `GET /accounts`.

#### Scenario: Somente cartões
- **WHEN** o cliente chama `GET /credit-cards`
- **THEN** cada entrada de dono contém apenas contas do tipo cartão de crédito

#### Scenario: Dono sem cartões
- **WHEN** um dono consultado não possui cartão de crédito
- **THEN** a entrada dele tem lista vazia e sem erro

### Requirement: Listagem de transações
O sistema SHALL expor `GET /owners/{owner}/accounts/{accountId}/transactions` retornando as transações da conta, consultada com as credenciais do dono informado. Cada transação SHALL ter data, descrição, valor, tipo e categoria quando disponíveis. O endpoint SHALL aceitar parâmetros opcionais de período e de página, cuja forma exata é definida na implementação.

#### Scenario: Transações de uma conta
- **WHEN** o cliente chama o endpoint com um dono configurado e uma conta que pertence a esse dono
- **THEN** a resposta contém as transações dessa conta

#### Scenario: Dono desconhecido
- **WHEN** o dono da rota não está na configuração
- **THEN** o sistema responde 404 sem consultar a Pluggy

#### Scenario: Conta de outro dono ou inexistente
- **WHEN** a Pluggy responde 404 para a conta com as credenciais do dono informado
- **THEN** o sistema responde 404 ao cliente

### Requirement: Listagem de faturas de cartão
O sistema SHALL expor `GET /owners/{owner}/accounts/{accountId}/bills` retornando as faturas de uma conta de cartão de crédito, consultada com as credenciais do dono informado, com data de vencimento e valor total.

#### Scenario: Faturas de um cartão
- **WHEN** o cliente chama o endpoint com um dono configurado e uma conta de cartão desse dono
- **THEN** a resposta contém as faturas dessa conta

#### Scenario: Dono desconhecido
- **WHEN** o dono da rota não está na configuração
- **THEN** o sistema responde 404 sem consultar a Pluggy

### Requirement: Listagem de empréstimos por dono
O sistema SHALL expor `GET /loans` retornando, para cada dono, os empréstimos de todos os itens dele, agrupados sob o nome do dono. O endpoint SHALL aceitar o parâmetro opcional `owner` e SHALL seguir as regras de falha parcial de `GET /accounts`. Cada empréstimo SHALL ter `id`, `itemId`, `productName`, `kind`, `currencyCode` e, quando a Pluggy os informar, `contractNumber`, `type`, `contractAmount`, `contractDate`, `dueDate`, as parcelas, `outstandingBalance` e `lastPayment`.

#### Scenario: Todos os donos
- **WHEN** o cliente chama `GET /loans` sem parâmetros
- **THEN** a resposta contém uma entrada por dono, cada uma com o nome do dono e a lista dos empréstimos dele

#### Scenario: Dono específico
- **WHEN** o cliente chama `GET /loans?owner=<nome>` com um dono configurado
- **THEN** a resposta contém apenas a entrada desse dono

#### Scenario: Dono desconhecido
- **WHEN** o cliente chama `GET /loans?owner=<nome>` com um dono que não está na configuração
- **THEN** o sistema responde 404 sem consultar a Pluggy

#### Scenario: Dono sem empréstimos
- **WHEN** um dono consultado não tem empréstimos em nenhum item
- **THEN** a entrada dele tem lista vazia e sem erro

#### Scenario: Um dono falha
- **WHEN** a consulta de empréstimos falha para um dono e tem sucesso para o outro
- **THEN** a resposta é 200, a entrada do dono com falha traz o erro e uma lista vazia, e a do outro traz os empréstimos

#### Scenario: Todos os donos falham
- **WHEN** a consulta de empréstimos falha para todos os donos sem o filtro `owner`
- **THEN** o sistema responde 502 no formato padrão de erro

### Requirement: Detalhe de parcelas e saldo do empréstimo
O sistema SHALL devolver em cada empréstimo as parcelas (`total`, `paid`, `remaining` e `pastDue`), o saldo para quitar em `outstandingBalance` e o pagamento regular mais recente em `lastPayment`, com `paidDate` e `paidAmount`. O sistema SHALL deixar nulo cada campo que a instituição não informar, e SHALL NOT contar pagamento avulso como `lastPayment`.

#### Scenario: Empréstimo com parcelas e pagamentos
- **WHEN** a Pluggy informa 120 parcelas no total, 65 pagas, 55 restantes e um saldo de 25000
- **THEN** o empréstimo traz essas parcelas e `outstandingBalance` 25000

#### Scenario: Pagamento mais recente
- **WHEN** o empréstimo tem vários pagamentos regulares
- **THEN** `lastPayment` é o de `paidDate` mais recente

#### Scenario: Pagamento avulso
- **WHEN** o pagamento mais recente é um pagamento avulso
- **THEN** `lastPayment` é o pagamento regular mais recente, e não o avulso

#### Scenario: Instituição sem dados de parcela
- **WHEN** a Pluggy não informa parcelas, saldo nem pagamentos
- **THEN** esses campos vêm nulos e o empréstimo aparece na lista

### Requirement: Tipo de empréstimo desconhecido
O sistema SHALL devolver `kind` como `LOAN`, `FINANCING`, `INVOICE_FINANCING` ou `UNARRANGED_ACCOUNT_OVERDRAFT`, e SHALL devolver `UNKNOWN` quando a Pluggy informar outro valor ou nenhum, sem falhar a resposta.

#### Scenario: Valor conhecido
- **WHEN** a Pluggy informa `kind` igual a `FINANCING`
- **THEN** o empréstimo traz `kind` `FINANCING`

#### Scenario: Valor novo da Pluggy
- **WHEN** a Pluggy informa um `kind` que o sistema não conhece
- **THEN** o empréstimo traz `kind` `UNKNOWN` e a resposta continua 200

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
