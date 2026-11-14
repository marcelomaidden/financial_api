# financial-api

API REST pessoal que lê contas, cartões de crédito, transações e faturas da
[Pluggy](https://docs.pluggy.ai) para mais de um dono (hoje `marcelo` e `luana`),
cada um com a própria conta de desenvolvedor na Pluggy.

Stack: Java 21, Spring Boot 4, Gradle, MySQL 8.4 (Docker Compose), Flyway.

## Requisitos

- JDK 17 ou superior para rodar o Gradle. O Gradle baixa o JDK 21 do projeto (toolchain).
- Docker com Docker Compose, para o MySQL local.

## Variáveis de ambiente

| Variável | Uso |
|---|---|
| `PLUGGY_MARCELO_CLIENT_ID` / `PLUGGY_MARCELO_CLIENT_SECRET` | Credenciais Pluggy do Marcelo |
| `PLUGGY_LUANA_CLIENT_ID` / `PLUGGY_LUANA_CLIENT_SECRET` | Credenciais Pluggy da Luana |
| `MYSQL_PASSWORD` / `MYSQL_ROOT_PASSWORD` | Senhas do MySQL local (`compose.yaml`). `MYSQL_USER` é opcional (padrão `financial_api`) |
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | Conexão com o banco, só no perfil `prod` |

A aplicação não inicia se faltar alguma credencial Pluggy. As senhas do MySQL
podem ficar num arquivo `.env` nesta pasta (ignorado pelo git). Nunca versione
credenciais.

## Itens da Pluggy

Os `itemId` de cada dono ficam fixos no yml do perfil
(`src/main/resources/application-dev.yml` e `application-prod.yml`):

```yaml
pluggy:
  owners:
    - name: marcelo
      client-id: ${PLUGGY_MARCELO_CLIENT_ID:}
      client-secret: ${PLUGGY_MARCELO_CLIENT_SECRET:}
      item-ids:
        - <item-id-do-marcelo>
```

Os itens são criados fora desta API (dashboard ou widget Pluggy Connect).

## Executar

Perfil `dev` (padrão). O Spring Boot sobe o MySQL do `compose.yaml` e aplica as
migrações do Flyway na inicialização:

```sh
export PLUGGY_MARCELO_CLIENT_ID=... PLUGGY_MARCELO_CLIENT_SECRET=...
export PLUGGY_LUANA_CLIENT_ID=...   PLUGGY_LUANA_CLIENT_SECRET=...
export MYSQL_PASSWORD=... MYSQL_ROOT_PASSWORD=...
./gradlew bootRun
```

Perfil `prod` (jar, banco externo):

```sh
./gradlew bootJar
DB_URL=jdbc:mysql://host:3306/financial_api DB_USER=... DB_PASSWORD=... \
  java -jar build/libs/financial-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

Se a rede inspeciona TLS (proxy corporativo) e o Gradle falhar com
`PKIX path building failed`, aponte a JVM para um truststore que tenha a CA do
proxy, por exemplo `./gradlew bootRun -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts -Djavax.net.ssl.trustStorePassword=changeit`.

## Endpoints

| Método e rota | Descrição |
|---|---|
| `GET /accounts?owner=` | Contas por dono. Sem `owner`, traz todos os donos |
| `GET /credit-cards?owner=` | Cartões de crédito por dono |
| `GET /owners/{owner}/accounts/{accountId}/transactions?dateFrom=&dateTo=&after=` | Transações de uma conta. Datas em `yyyy-mm-dd`. Para a próxima página, envie `after` com o `nextCursor` da resposta anterior |
| `GET /owners/{owner}/accounts/{accountId}/bills?page=&pageSize=` | Faturas de uma conta de cartão (`pageSize` até 500) |

Em `/accounts` e `/credit-cards`, a falha de um dono não descarta os demais: a
resposta é 200 e a entrada do dono com falha traz `error`. Confira o campo
`error` de cada entrada. Se todos os donos falharem, a resposta é 502.

```json
{
  "owners": [
    { "owner": "marcelo", "accounts": [ { "id": "...", "type": "BANK" } ], "error": null },
    { "owner": "luana", "accounts": [],
      "error": { "status": 502, "error": "Bad Gateway", "message": "..." } }
  ]
}
```

O `id` de cada conta vale só para o dono que o devolveu. Use o mesmo `owner` nas
rotas de transações e faturas.

Erros seguem o formato `{"status", "error", "message"}`:

| Situação | Status |
|---|---|
| Parâmetro inválido | 400 |
| Dono não configurado, ou conta não encontrada na Pluggy | 404 |
| Limite de requisições da Pluggy | 429 |
| Credenciais rejeitadas ou outro erro da Pluggy | 502 |
| Pluggy indisponível ou sem resposta | 504 |
