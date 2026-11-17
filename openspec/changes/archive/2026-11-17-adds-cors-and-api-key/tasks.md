# Tasks

## 1. Configuração

- [x] 1.1 Criar `ApiAccessProperties` (prefixo `app.access`: `apiKey` e `cors.allowedOrigins`, com `toString` sem a chave), as entradas em `application.yml`, `application-dev.yml` (origens de `localhost`) e `application-prod.yml`, e o bean `@Profile("prod")` que falha a inicialização sem chave. Verificar com testes unitários de: lista separada por vírgula, lista vazia, espaços e origens em branco ignorados, `toString` sem a chave, e falha de inicialização em produção com a chave ausente ou em branco, com mensagem que cita `APP_API_KEY`, e `./gradlew test` passando

## 2. Filtros

- [x] 2.1 Criar `ApiKeyFilter`. Verificar com testes unitários (`MockHttpServletRequest` e `MockFilterChain`) cobrindo: chave correta passa, ausente dá 401, errada dá 401, mensagem igual nos dois casos e sem o valor enviado, `OPTIONS` sem chave passa, chave configurada em branco desliga a verificação, corpo no formato `{status, error, message}` com `Content-Type` JSON, e a cadeia não é chamada no 401, e `./gradlew test` passando
- [x] 2.2 Criar a configuração que registra o `CorsFilter` (origens, métodos `GET` e `OPTIONS`, cabeçalho `X-API-Key`, sem credenciais, `maxAge` de 1 hora) e o `ApiKeyFilter`, nessa ordem, por `FilterRegistrationBean`. Verificar com um teste `MockMvc` autônomo, com os dois filtros na ordem do projeto, cobrindo: origem permitida recebe `Access-Control-Allow-Origin`, origem não listada não recebe, preflight de origem permitida pedindo `X-API-Key` dá 200 sem chave, o 401 de origem permitida traz os cabeçalhos de CORS, rota inexistente sem chave dá 401, e lista vazia não libera origem, e `./gradlew test` passando

## 3. Documentação

- [x] 3.1 Documentar no README as variáveis `APP_API_KEY` e `APP_CORS_ALLOWED_ORIGINS` na tabela de variáveis, uma seção sobre o acesso (cabeçalho `X-API-Key`, o 401, o formato exato das origens, como gerar a chave e configurá-la no Heroku com `heroku config:set`, e que o front precisa enviá-la) e o aviso de que a chave no JavaScript do SPA público não esconde os dados. Verificar que os nomes das variáveis e do cabeçalho do README batem com o código e com os testes

## 4. Verificação no deploy

- [ ] 4.1 Verificar no Heroku, depois do deploy e de configurar `APP_API_KEY` e `APP_CORS_ALLOWED_ORIGINS`: `curl` sem chave dá 401 em JSON, `curl` com a chave dá 200, o front consegue ler a API sem erro de CORS, e a aplicação não inicia se `APP_API_KEY` for removida. Exige o deploy, que é feito por você
