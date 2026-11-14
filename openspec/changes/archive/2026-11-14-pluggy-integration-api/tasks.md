# Tasks

## 1. Repositório e projeto base

- [x] 1.1 Criar a pasta `financial_api`, rodar `git init` nela e verificar que `git -C financial_api status` funciona
- [x] 1.2 Criar o `.gitignore` com o padrão Spring Boot/Gradle (`build/`, `.gradle/`, mantendo `gradle/wrapper/gradle-wrapper.jar` versionado), IntelliJ (`.idea/`, `*.iml`, `out/`), `application-local.yml` e `financial_api_credentials.txt`, e verificar com `git check-ignore -v` que `.idea/`, `build/` e `financial_api_credentials.txt` são ignorados
- [x] 1.3 Gerar o projeto Gradle (wrapper, DSL Groovy) com Java 21 e Spring Boot 3.2 ou superior (Spring Web, Validation, Configuration Processor, Data JPA, MySQL Driver, Flyway Migration com `flyway-mysql`, Docker Compose Support) e verificar que `./gradlew compileJava` termina sem erro
- [x] 1.4 Criar os packages `client`, `client/dto`, `config`, `service`, `resource`, `resource/dto` e `exception`, e verificar a estrutura com `find src -type d`

## 2. Configuração

- [x] 2.1 Criar `PluggyProperties` (`baseUrl`, timeouts e lista `owners` com `name`, `clientId`, `clientSecret`, `itemIds`) validada com Bean Validation e com checagem de nomes únicos, e verificar que a aplicação falha ao iniciar com lista de donos vazia, nome repetido, credencial ausente ou `itemIds` vazio, sem exibir valores
- [x] 2.2 Criar `application.yml` e `application-<perfil>.yml` com os donos "marcelo" e "luana", `client-id: ${PLUGGY_MARCELO_CLIENT_ID}`, `client-secret: ${PLUGGY_MARCELO_CLIENT_SECRET}` (e equivalentes `PLUGGY_LUANA_*`) e `item-ids` de cada um, e verificar que as propriedades são lidas das variáveis (a inicialização completa só passa depois das tarefas 2.3 e 2.4, porque a aplicação exige o MySQL)
- [x] 2.3 Criar o `compose.yaml` com o MySQL (volume nomeado, senha por variável de ambiente) e verificar com `docker compose up -d` que o contêiner fica saudável
- [x] 2.4 Configurar o datasource com `${DB_URL}`, `${DB_USER}` e `${DB_PASSWORD}`, `ddl-auto=validate` e `open-in-view=false`, criar `db/migration/V1__baseline.sql` e verificar na inicialização que o Flyway aplica a V1 (tabela `flyway_schema_history` com uma linha)
- [x] 2.5 Garantir que `.env` e outros arquivos de credenciais do banco estão no `.gitignore` e verificar com `git check-ignore -v .env`

## 3. Cliente Pluggy

- [x] 3.1 Criar os DTOs de `client/dto` (auth, accounts, transactions, bills, página de resultados) como `records` tolerantes a campos desconhecidos e conferir os nomes de campo na documentação da Pluggy
- [ ] 3.2 Implementar o componente de `apiKey` por dono (autenticação via `POST /auth` com as credenciais do dono, cache com expiração por dono, renovação sincronizada) e verificar com chamadas reais que cada dono obtém a própria chave e a reutiliza na segunda chamada
- [ ] 3.3 Criar o `RestClient` com interceptor de `X-API-KEY` do dono da chamada (repetindo uma vez após 401 e renovando só a chave desse dono), timeouts e `ResponseErrorHandler` que lança `PluggyException`, e verificar com uma chamada real ao `/accounts` de cada dono
- [ ] 3.4 Criar a HTTP Interface com `accounts`, `transactions` e `bills`, recebendo o dono em cada chamada, e verificar que cada método retorna dados de um item real ou do sandbox para cada dono

## 4. Erros

- [x] 4.1 Criar `PluggyException` e o `@RestControllerAdvice` com o formato `{status, error, message}` e o mapeamento 404, 429, 502, 504 e 400, e verificar com `curl` um caso de 404 (dono desconhecido) e um de 400 (parâmetro inválido)
- [x] 4.2 Verificar que credenciais e chaves não aparecem em logs nem em respostas de erro, forçando uma credencial inválida em um dono e inspecionando a saída

## 5. Service e endpoints

- [x] 5.1 Implementar `PluggyService` com `getAccounts`, `getCreditCards`, `getTransactions` e `getBills`, mapeando DTOs de `client` para DTOs de `resource`, e verificar o mapeamento nos endpoints do grupo
- [x] 5.2 Implementar no `PluggyService` a consulta por dono com falha parcial (entrada `{owner, accounts, error}` por dono, 200 se ao menos um dono tiver sucesso, 502 se todos falharem sem filtro), e verificar forçando credencial inválida em um dono e depois nos dois
- [x] 5.3 Criar o controller com `GET /accounts` e `GET /credit-cards`, com `owner` opcional validado contra a configuração, e verificar com `curl` as respostas para todos os donos, para um dono e para um dono desconhecido
- [ ] 5.4 Adicionar `GET /owners/{owner}/accounts/{accountId}/transactions` repassando período e página à Pluggy, definindo os nomes dos parâmetros pela documentação, e verificar com `curl` uma conta real de cada dono e uma conta de outro dono (esperando 404)
- [ ] 5.5 Adicionar `GET /owners/{owner}/accounts/{accountId}/bills` e verificar com `curl` uma conta de cartão real

## 6. Verificação final e documentação

- [x] 6.1 Escrever um `README.md` curto com as variáveis de ambiente por dono, a configuração de `item-ids`, como executar e os endpoints, e verificar que os comandos documentados funcionam como escritos
- [ ] 6.2 Executar o fluxo completo (autenticar, listar contas e cartões dos dois donos, transações, faturas) contra a Pluggy e confirmar que todos os cenários das specs `pluggy-financial-data` e `pluggy-error-handling` se comportam como descrito
