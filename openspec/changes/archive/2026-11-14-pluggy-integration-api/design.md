# Design

## Context

Projeto novo (`financial_api`), sem código. Consome a API da Pluggy (`https://api.pluggy.ai`). A autenticação é `POST /auth` com `clientId` e `clientSecret`, que devolve uma `apiKey` de curta duração (cerca de 2 horas, a confirmar na documentação). A chave segue no header `X-API-KEY`. Cada pessoa tem sua própria conta de desenvolvedor na Pluggy, então há uma credencial (e uma `apiKey`) por dono, e um `accountId` só é válido na credencial que o devolveu. Contas dependem de `itemId` (`GET /accounts?itemId=`), transações e faturas dependem de `accountId`. Cartão de crédito é uma conta de tipo `CREDIT`. Veja proposal.md para motivação e escopo.

## Goals / Non-Goals

**Goals:**
- Estrutura de packages simples e coerente: `client`, `config`, `service`, `resource`, `exception`.
- Isolar o formato da Pluggy do formato exposto pela API.

**Non-Goals:**
- Sem entidades, repositórios nem persistência de dados financeiros nesta etapa. Só a infraestrutura de banco fica pronta. Também sem cache de dados financeiros, fila ou agendamento.
- Sem autenticação própria na API: o uso é local e pessoal.
- Sem testes automatizados nesta etapa.

## Decisions

**1. Cliente HTTP próprio com `RestClient` e HTTP Interface**
Interface declarativa (`@HttpExchange`) em `client`, montada com `HttpServiceProxyFactory`. Alternativas: Feign (exige Spring Cloud só por isso) e `pluggy-java` (Retrofit/OkHttp, distribuído via GitHub Packages com token pessoal, pouco integrado ao Spring). Para quatro endpoints, o cliente próprio custa pouco e deixa o tratamento de erro sob controle.

**2. Gestão da `apiKey` por dono em componente próprio**
Um componente em `client` autentica, guarda a chave em memória com horário de expiração e a renova com margem de segurança, mantendo uma entrada separada por dono. O cliente HTTP recebe o dono como argumento da chamada, e um `ClientHttpRequestInterceptor` do `RestClient` injeta o `X-API-KEY` desse dono. Em resposta 401 o interceptor invalida a chave daquele dono e repete a chamada uma vez. A renovação é sincronizada por dono para evitar autenticações concorrentes. Alternativa: autenticar a cada chamada, descartada por custo e risco de rate limit.

**3. Dois conjuntos de DTOs**
- `client/dto`: espelham o JSON da Pluggy, como `records` com `@JsonIgnoreProperties(ignoreUnknown = true)`.
- `resource/dto`: contrato da própria API.
O `PluggyService` faz o mapeamento. O custo é um mapeamento a mais, e o ganho é que mudanças na Pluggy não alteram o contrato exposto.

**4. Configuração por dono**
`@ConfigurationProperties("pluggy")` em `config` com `baseUrl`, timeouts e `owners`, uma lista de objetos com `name`, `clientId`, `clientSecret` e `itemIds`, validada com Bean Validation (`@NotEmpty`, `@NotBlank`) e por uma checagem de nomes únicos. No yml, por exemplo `client-id: ${PLUGGY_MARCELO_CLIENT_ID}` e `client-secret: ${PLUGGY_MARCELO_CLIENT_SECRET}` para cada dono, sem valor padrão. `application.yml` com o comum e `application-<perfil>.yml` por ambiente. Os `itemIds` de cada dono ficam no yml do ambiente.

**5. Tratamento de erros**
`PluggyException` carrega o status HTTP e a mensagem vindos da Pluggy. Um `ResponseErrorHandler` do `RestClient` converte respostas 4xx e 5xx nela. Um `@RestControllerAdvice` mapeia: 404 para 404, 429 para 429, demais erros da Pluggy para 502, falha de conexão ou timeout para 504, e validação para 400. O corpo é `{status, error, message}`. Credenciais nunca entram em mensagens nem em logs. Dono desconhecido gera 404 antes de qualquer chamada à Pluggy.

**6. Endpoints e falha parcial**
`GET /accounts` e `GET /credit-cards` (com `owner` opcional) e `GET /owners/{owner}/accounts/{accountId}/transactions` e `.../bills`. O `PluggyService` consulta cada dono em sequência (dois donos não justificam concorrência) e monta, por dono, uma entrada `{owner, accounts, error}`. Se um dono falhar, o erro (`status`, `message`) fica na entrada dele, a lista fica vazia, e as outras entradas seguem. A resposta é 200 se ao menos um dono tiver sucesso, e 502 se todos falharem em uma consulta sem filtro. Com `owner` na consulta, a falha segue o mapeamento normal. Transações e faturas usam as credenciais do dono da rota, e uma conta de outro dono resulta em 404 da Pluggy, que a API repassa como 404. Não há verificação de que a conta pertence a um item configurado, aceita para uso pessoal.

**7. Build com Gradle**
O projeto usa Gradle com o wrapper (`./gradlew`) e DSL Groovy, o formato padrão do Spring Initializr. A suposição da DSL pode mudar para Kotlin sem afetar specs. O Configuration Processor entra como `annotationProcessor`.

**8. Repositório**
A pasta `financial_api` tem seu próprio repositório git. O `.gitignore` cobre `build/`, `.gradle/`, `!gradle/wrapper/gradle-wrapper.jar`, logs, `application-local.yml`, `.idea/`, `*.iml`, `out/` e `financial_api_credentials.txt`. As credenciais ficam fora do repositório e entram pela variável de ambiente.

**9. Infraestrutura de banco (MySQL, JPA, Flyway)**
- Dependências: `spring-boot-starter-data-jpa`, `mysql-connector-j` (runtime), `flyway-core` e `flyway-mysql`, e `spring-boot-docker-compose` como `developmentOnly`.
- O datasource usa `${DB_URL}`, `${DB_USER}` e `${DB_PASSWORD}` no yml, sem valor padrão, como as credenciais da Pluggy. No perfil de desenvolvimento, o suporte a Docker Compose do Spring Boot sobe o MySQL do `compose.yaml` e preenche a conexão automaticamente.
- O Flyway roda automaticamente na inicialização (padrão do Spring Boot), com migrações em `src/main/resources/db/migration`. Uma `V1__baseline.sql` vazia valida o mecanismo. Alternativa descartada: migração manual pelo plugin Gradle, que serve a vários nós ou a um fluxo com DBA, o que não é o caso.
- `spring.jpa.hibernate.ddl-auto=validate`, para que o esquema só mude por migrações. `spring.jpa.open-in-view=false`.
- O `compose.yaml` define o MySQL com volume nomeado e senha por variável de ambiente, sem valores em texto no arquivo. Se o `compose.yaml` ler um `.env`, o `.gitignore` ignora esse `.env`.

**10. Transações via `GET /v2/transactions` (cursor)**
O `GET /transactions` (paginação por página) está deprecated na Pluggy e será removido em 2026-12-31. A API usa o `GET /v2/transactions`: o período vem em `dateFrom` e `dateTo` (`yyyy-mm-dd`) e a paginação é por cursor. A resposta da API repassa o cursor da próxima página (valor `after` extraído do `next` da Pluggy, ou `null`), e o cliente o devolve no parâmetro `after` para buscar a página seguinte. Faturas (`GET /bills`) continuam paginadas por `page` e `pageSize`.

## Risks / Trade-offs

- [Detalhes da API da Pluggy (validade da chave, paginação, nomes de campos) divergem do assumido] → Confirmar na documentação durante a implementação e manter DTOs tolerantes a campos desconhecidos.
- [Respostas paginadas da Pluggy podem exigir várias chamadas] → Aceitar parâmetros de página e período repassados à Pluggy, sem agregação automática nesta etapa.
- [Conta consultada por `accountId` sem checar o item] → Aceito, uso pessoal e sem exposição pública.
- [`accountId` sem dono não identifica a credencial] → O dono entra na rota de transações e faturas, e o `owner` vem na resposta de `/accounts`. Alternativas descartadas: tentar as duas credenciais (gasta chamadas e mistura 404 real com conta de outro dono) e mapa `accountId→owner` em banco ou memória (estado que pode ficar desatualizado).
- [Um dono falhar em `/accounts`] → Falha parcial, com 200 e erro na entrada do dono. Quem consome precisa checar o campo de erro de cada entrada.
- [Chave em memória é perdida no reinício] → Reautentica na primeira chamada.
- [Segredo existente em arquivo texto] → Fora do repositório novo, ignorado no git. Rotacionar o secret se o arquivo já tiver sido compartilhado.
- [A aplicação passa a exigir o MySQL para iniciar, mesmo sem usar o banco] → Aceito, o banco entra "em breve". Em dev, o Docker Compose sobe o MySQL junto com a aplicação.
- [Flyway automático aplica migrações sem confirmação] → Aceito para uso pessoal com um único nó.
- [Sem testes automatizados] → Verificar manualmente contra a conta real ou o sandbox da Pluggy.
