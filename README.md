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

## Verificações

O GitHub Actions (`.github/workflows/ci.yml`) roda em cada pull request e a cada
push na `master`:

| Job | O que roda |
|---|---|
| `build` | `./gradlew build`: compila, roda os testes e gera o jar. Não precisa de MySQL nem de credenciais da Pluggy |
| `openspec` | `openspec validate --all`, com a CLI do OpenSpec na versão 1.16.0 |

Para rodar os mesmos comandos localmente:

```sh
./gradlew build
npm install -g @fission-ai/openspec@1.16.0   # uma vez
openspec validate --all
```

O CI não sobe a aplicação, então um erro de montagem de beans só aparece ao
executá-la.

## Endpoints

| Método e rota | Descrição |
|---|---|
| `GET /accounts?owner=` | Contas por dono. Sem `owner`, traz todos os donos |
| `GET /credit-cards?owner=` | Cartões de crédito por dono |
| `GET /loans?owner=` | Empréstimos e financiamentos por dono. Sem `owner`, traz todos os donos |
| `GET /owners/{owner}/accounts/{accountId}/transactions?dateFrom=&dateTo=&after=` | Transações de uma conta. Datas em `yyyy-mm-dd`. Para a próxima página, envie `after` com o `nextCursor` da resposta anterior |
| `GET /owners/{owner}/accounts/{accountId}/bills?page=&pageSize=` | Faturas de uma conta de cartão (`pageSize` até 500) |
| `GET /summary?dateFrom=&dateTo=` | Resumo dos donos em BRL: `incomes` (o que há e o que entrou nas contas bancárias) e `expenses` (o que o cartão cobra). Datas em `yyyy-mm-dd` |

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

## Resumo (`GET /summary`)

Soma, em BRL, o que os donos têm e devem. Só entram contas e valores em BRL.

| Parte | O que traz |
|---|---|
| `incomes.available` | Saldo atual das contas `BANK`. Não depende do período |
| `incomes.transactions` | Entradas (`CREDIT`) das contas `BANK` no período |
| `expenses.owed` | Faturas dos cartões com `dueDate` no período, até o fim do mês de `dateTo`. Não informa se a fatura já foi paga |
| `expenses.transactions` | Compras do cartão ainda sem fatura (`DEBIT`, `PENDING`, sem `billId`), buscadas pela data em que chegaram à Pluggy, numa janela de cada cartão. `amountInAccountCurrency` aparece só em compra fora de BRL, e o total usa esse valor. `installment` (`number` e `total`) aparece só em compra parcelada |

Cada parte tem `total` (`value` e `currencyCode`) e `detail`, um mapa de dono
para a lista dele. `dateTo` é opcional e assume hoje. `dateFrom` é opcional e
assume o dia 1 do mês de `dateTo`.

`expenses.transactions` mostra o estado atual e não usa `dateFrom` nem `dateTo`.
A janela é de cada cartão e vem da fatura mais recente que a Pluggy devolve para
ele: a compra entra se chegou à Pluggy a partir de 10 dias antes do fechamento
anterior e se a data dela vai até 5 dias depois do vencimento da fatura aberta.
Isso mantém as parcelas da fatura aberta, mesmo com data de compra antiga, e deixa
de fora as parcelas de faturas futuras. Cartão sem fatura devolvida usa o dia 1 do
mês anterior até hoje.

Se a consulta de um dono falhar, a resposta é 200, `partial` é `true`, o dono
sai da parte afetada e entra em `errors`. Se as contas falharem para todos os
donos, a resposta é 502.

```json
{
  "incomes": {
    "available": {
      "total": { "value": 500.50, "currencyCode": "BRL" },
      "detail": { "marcelo": [ { "id": "...", "name": "Nubank", "balance": 500.50, "currencyCode": "BRL" } ] }
    },
    "transactions": {
      "total": { "value": 19.99, "currencyCode": "BRL" },
      "detail": { "marcelo": [ { "id": "...", "description": "PIX Recebido", "amount": 19.99,
                                 "currencyCode": "BRL", "date": "...", "accountId": "..." } ] }
    }
  },
  "expenses": {
    "owed": {
      "total": { "value": 250.40, "currencyCode": "BRL" },
      "detail": { "marcelo": [ { "id": "...", "accountId": "...", "name": "Cartão Nubank",
                                 "dueDate": "2026-11-20T00:00:00", "totalAmount": 250.40, "currencyCode": "BRL" } ] }
    },
    "transactions": {
      "total": { "value": 478.19, "currencyCode": "BRL" },
      "detail": { "marcelo": [
        { "id": "...", "accountId": "...", "description": "Netflix", "currencyCode": "USD", "amount": 58,
          "date": "...", "category": "Video streaming", "amountInAccountCurrency": 298.19 },
        { "id": "...", "accountId": "...", "description": "Mercado", "currencyCode": "BRL", "amount": 30,
          "date": "...", "category": "Groceries" },
        { "id": "...", "accountId": "...", "description": "Notebook", "currencyCode": "BRL", "amount": 150,
          "date": "...", "category": "Electronics", "installment": { "number": 3, "total": 10 } } ] }
    }
  },
  "partial": true,
  "errors": [ { "owner": "luana", "status": 502, "message": "Erro ao consultar a Pluggy" } ]
}
```

## Empréstimos (`GET /loans`)

Traz os empréstimos de todos os itens de cada dono, com a mesma regra de falha
parcial de `/accounts`: a entrada do dono com falha traz `error`, e a resposta é
502 se todos falharem. A instituição só devolve o que informa, então os campos
opcionais podem vir `null`. Só `id`, `itemId`, `productName`, `kind` e
`currencyCode` são garantidos.

| Campo | O que é |
|---|---|
| `kind` | `LOAN`, `FINANCING`, `INVOICE_FINANCING`, `UNARRANGED_ACCOUNT_OVERDRAFT` ou `UNKNOWN` |
| `type` | Tipo do Open Finance, como texto |
| `dueDate` | Fim do contrato |
| `installments` | `total`, `paid`, `remaining` e `pastDue` |
| `outstandingBalance` | Saldo para quitar o contrato |
| `lastPayment` | Pagamento regular mais recente (`paidDate` e `paidAmount`), sem contar pagamento avulso |

A API não informa o valor da parcela mensal.

```json
{
  "owners": [
    { "owner": "marcelo",
      "loans": [ {
        "id": "...", "itemId": "...", "contractNumber": "000000721792794",
        "productName": "Crédito Pessoal Consignado", "kind": "LOAN",
        "type": "CREDITO_PESSOAL_COM_CONSIGNACAO", "contractAmount": 50000, "currencyCode": "BRL",
        "contractDate": "2022-08-01", "dueDate": "2028-01-15",
        "installments": { "total": 120, "paid": 65, "remaining": 55, "pastDue": 0 },
        "outstandingBalance": 25000,
        "lastPayment": { "paidDate": "2023-07-15", "paidAmount": 680.50 } } ],
      "error": null },
    { "owner": "luana", "loans": [],
      "error": { "status": 502, "error": "Bad Gateway", "message": "..." } }
  ]
}
```

Erros seguem o formato `{"status", "error", "message"}`:

| Situação | Status |
|---|---|
| Parâmetro inválido | 400 |
| Dono não configurado, ou conta não encontrada na Pluggy | 404 |
| Limite de requisições da Pluggy | 429 |
| Credenciais rejeitadas ou outro erro da Pluggy | 502 |
| Pluggy indisponível ou sem resposta | 504 |
