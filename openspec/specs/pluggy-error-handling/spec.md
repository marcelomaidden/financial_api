# pluggy-error-handling Specification

## Purpose

Define como a API responde quando a Pluggy falha ou quando a requisição é inválida, garantindo um formato de erro único e sem vazamento de segredos.

## Requirements

### Requirement: Formato único de erro
O sistema SHALL responder todos os erros com um corpo JSON contendo `status`, `error` e `message`. O erro de um dono dentro de uma resposta de falha parcial SHALL conter ao menos `status` e `message`.

#### Scenario: Erro de validação
- **WHEN** o cliente envia um parâmetro inválido
- **THEN** o sistema responde 400 com corpo no formato padrão de erro

### Requirement: Erros da Pluggy
O sistema SHALL converter respostas de erro da Pluggy em respostas HTTP da API, preservando 404 como 404 e 429 como 429. Os demais erros 4xx e 5xx da Pluggy SHALL resultar em 502.

#### Scenario: Recurso não encontrado na Pluggy
- **WHEN** a Pluggy responde 404
- **THEN** o sistema responde 404 com a mensagem de erro da Pluggy, quando houver

#### Scenario: Limite de requisições
- **WHEN** a Pluggy responde 429
- **THEN** o sistema responde 429

#### Scenario: Falha da Pluggy
- **WHEN** a Pluggy responde 500 ou outro erro não mapeado
- **THEN** o sistema responde 502 com mensagem genérica

#### Scenario: Pluggy indisponível
- **WHEN** a conexão com a Pluggy falha ou excede o tempo limite
- **THEN** o sistema responde 504

### Requirement: Falha de credenciais
O sistema SHALL tratar a rejeição das credenciais de um dono pela Pluggy como falha de autenticação desse dono, SHALL identificar o dono afetado na mensagem e SHALL NOT incluir `clientId`, `clientSecret` ou chave na resposta nem nos logs.

#### Scenario: Credenciais inválidas em consulta de um único dono
- **WHEN** a Pluggy responde 401 à autenticação de um dono em uma consulta restrita a ele
- **THEN** o sistema responde 502 com mensagem que indica falha de autenticação desse dono, sem exibir credenciais

#### Scenario: Credenciais inválidas em consulta de vários donos
- **WHEN** a Pluggy responde 401 à autenticação de um dono e os demais têm sucesso
- **THEN** a entrada desse dono contém o erro de autenticação (`status` 502, sem credenciais) e as demais entradas trazem seus dados
