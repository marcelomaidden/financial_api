# Proposal

## Why

O front-end (um SPA em `https://financial-api-frontend-dc3ceb1ba406.herokuapp.com`) não consegue chamar a API: o navegador bloqueia as respostas com "blocked by CORS policy", porque a API não devolve `Access-Control-Allow-Origin`. A mesma API responde normalmente no Postman, sem nenhuma chave, o que mostra um segundo problema: hoje qualquer pessoa que souber a URL lê saldos, transações e empréstimos dos dois donos.

## What Changes

- **CORS:** a API passa a permitir as origens listadas em `APP_CORS_ALLOWED_ORIGINS` (lista separada por vírgula, cada origem com esquema e sem barra no final). Em produção, só a origem do front acima. Permite os métodos `GET` e `OPTIONS` e o cabeçalho `X-API-Key`, sem credenciais. Origem fora da lista não recebe os cabeçalhos de CORS. Em desenvolvimento, as origens de `localhost` também são permitidas.
- **Chave de API:** toda rota exige o cabeçalho `X-API-Key` com o valor de `APP_API_KEY`, comparado em tempo constante. Chave ausente ou errada responde 401 no formato `{status, error, message}`. A requisição `OPTIONS` (preflight) passa sem chave. **BREAKING:** quem chama a API hoje, como o Postman, precisa enviar o cabeçalho.
- **Ordem dos filtros:** o filtro de CORS roda antes do filtro da chave, para que uma resposta 401 também leve os cabeçalhos de CORS. Sem isso, o navegador mostraria "CORS error" no lugar do 401.
- **Inicialização:** em produção, a aplicação não inicia sem `APP_API_KEY`, como já acontece com as credenciais da Pluggy. Em desenvolvimento a chave é opcional, e vazia desliga a verificação.
- A chave nunca é registrada em log nem devolvida em resposta.
- O README documenta as duas variáveis, como gerar e configurar a chave no Heroku (`heroku config:set`) e que o front precisa enviá-la.
- Fora de escopo:
  - autenticação de usuário (login, sessão ou token);
  - proteger o próprio front: com a chave dentro do JavaScript do SPA, quem abrir o front público vê os dados, e fechar isso exige senha no front, num passo separado;
  - correções de deploy no Heroku;
  - limite de requisições e rotação automática da chave;
  - Spring Security.

## Capabilities

### New Capabilities
- `api-access-control`: quem pode chamar a API. Cobre a chave de API obrigatória, a resposta 401 e as origens permitidas por CORS.

### Modified Capabilities

## Impact

- Código: um filtro de CORS e um filtro de chave de API, registrados na ordem certa, uma classe de propriedades para as duas variáveis, e a configuração em `application.yml`, `application-dev.yml` e `application-prod.yml`. Sem mudança nas rotas nem nos serviços.
- API: toda rota passa a exigir `X-API-Key`. O corpo das respostas não muda, e o 401 segue o formato padrão de erro.
- Variáveis de ambiente novas: `APP_API_KEY` (obrigatória em produção) e `APP_CORS_ALLOWED_ORIGINS`.
- Heroku: `APP_API_KEY` e `APP_CORS_ALLOWED_ORIGINS` precisam ser configuradas na app da API, e a chave também no build do front.
- Testes: os dois filtros, com o cenário de 401 com cabeçalhos de CORS.
- README: documentar as variáveis.
