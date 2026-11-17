# api-access-control Specification

## Purpose

Define quem pode chamar a API: a chave de API obrigatória em todas as rotas e as origens de navegador autorizadas por CORS.

## Requirements

### Requirement: Chave de API obrigatória
O sistema SHALL exigir, em toda requisição, o cabeçalho `X-API-Key` com o valor configurado em `APP_API_KEY`, exceto nas requisições `OPTIONS`. O sistema SHALL verificar a chave antes de resolver a rota, e SHALL comparar o valor em tempo constante.

#### Scenario: Chave correta
- **WHEN** o cliente chama uma rota com `X-API-Key` igual à chave configurada
- **THEN** o sistema atende a requisição normalmente

#### Scenario: Chave ausente
- **WHEN** o cliente chama uma rota sem o cabeçalho `X-API-Key`
- **THEN** o sistema responde 401 sem consultar a Pluggy

#### Scenario: Chave errada
- **WHEN** o cliente chama uma rota com `X-API-Key` diferente da chave configurada
- **THEN** o sistema responde 401 sem consultar a Pluggy

#### Scenario: Rota inexistente sem chave
- **WHEN** o cliente chama uma rota que não existe, sem chave
- **THEN** o sistema responde 401 e não 404, para não revelar quais rotas existem

#### Scenario: Preflight sem chave
- **WHEN** o navegador envia uma requisição `OPTIONS` de preflight, que não leva a chave
- **THEN** o sistema não a rejeita por falta de chave

### Requirement: Resposta de acesso negado
O sistema SHALL responder 401 no formato padrão `{status, error, message}` quando a chave estiver ausente ou errada. A mensagem SHALL ser a mesma nos dois casos e SHALL NOT conter a chave enviada nem a configurada.

#### Scenario: Formato do 401
- **WHEN** a chave está ausente ou errada
- **THEN** a resposta é 401 com `status` 401, `error` `Unauthorized` e uma `message`

#### Scenario: Mesma mensagem para ausente e errada
- **WHEN** o cliente envia uma requisição sem chave e outra com chave errada
- **THEN** as duas respostas têm a mesma `message`

#### Scenario: Chave fora da resposta
- **WHEN** o cliente envia uma chave errada
- **THEN** o corpo da resposta não contém o valor enviado

### Requirement: Chave configurada por variável de ambiente
O sistema SHALL ler a chave de `APP_API_KEY`. Em produção, o sistema SHALL falhar a inicialização quando a chave estiver ausente ou em branco, com uma mensagem que cite a variável e não exiba valores. O sistema SHALL NOT registrar a chave em logs nem devolvê-la em respostas.

#### Scenario: Produção sem chave
- **WHEN** a aplicação inicia em produção sem `APP_API_KEY` ou com ela em branco
- **THEN** a aplicação falha na inicialização com uma mensagem que cita `APP_API_KEY`

#### Scenario: Desenvolvimento sem chave
- **WHEN** a aplicação inicia em desenvolvimento sem `APP_API_KEY`
- **THEN** a aplicação inicia, a verificação da chave fica desligada e o log avisa disso, sem exibir valores

### Requirement: Origens permitidas por CORS
O sistema SHALL permitir chamadas de navegador apenas das origens listadas em `APP_CORS_ALLOWED_ORIGINS`, uma lista separada por vírgula. Para uma origem listada, SHALL responder com `Access-Control-Allow-Origin` igual à origem, permitir os métodos `GET` e `OPTIONS` e os cabeçalhos `X-API-Key`, `Content-Type` e `Accept`, sem credenciais. Para qualquer outra origem, SHALL rejeitar a requisição com 403 e SHALL NOT responder com cabeçalhos de CORS.

#### Scenario: Origem permitida
- **WHEN** uma requisição traz o cabeçalho `Origin` de uma origem listada
- **THEN** a resposta traz `Access-Control-Allow-Origin` com essa origem

#### Scenario: Origem não listada
- **WHEN** uma requisição traz o cabeçalho `Origin` de uma origem que não está na lista
- **THEN** o sistema responde 403 sem cabeçalhos de CORS

#### Scenario: Preflight de origem permitida
- **WHEN** o navegador envia um preflight de uma origem listada pedindo o método `GET` e o cabeçalho `X-API-Key`
- **THEN** o sistema responde 200 autorizando o método e o cabeçalho, sem exigir a chave

#### Scenario: Lista vazia
- **WHEN** `APP_CORS_ALLOWED_ORIGINS` está vazia ou ausente
- **THEN** nenhuma origem recebe cabeçalhos de CORS, e uma requisição com `Origin` recebe 403

#### Scenario: Requisição sem origem
- **WHEN** uma requisição não traz o cabeçalho `Origin`, como as do Postman
- **THEN** o CORS não interfere, e vale só a verificação da chave

### Requirement: Erros de acesso legíveis pelo navegador
O sistema SHALL aplicar o CORS antes da verificação da chave, de modo que a resposta 401 a uma origem permitida também traga os cabeçalhos de CORS.

#### Scenario: 401 para origem permitida
- **WHEN** uma origem listada chama uma rota sem chave
- **THEN** a resposta é 401 e traz `Access-Control-Allow-Origin` com essa origem, e o navegador consegue ler o erro
