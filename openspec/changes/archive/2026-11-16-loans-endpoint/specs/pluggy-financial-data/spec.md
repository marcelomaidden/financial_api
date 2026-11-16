# Spec Delta

## ADDED Requirements

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
