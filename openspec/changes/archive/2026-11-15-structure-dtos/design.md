# Design

## Context

Estado atual, observado no código:
- `AccountService`, `TransactionService`, `BillService` e `SummaryService` importam tipos de `resource.dto` (`OwnersResponse`, `OwnerAccounts`, `TransactionsResponse`, `BillsResponse`, `SummaryResponse`, `ApiError`).
- `PluggyMapper` (em `service`) converte `client.dto` direto para `resource.dto`.
- `client.dto.PluggyAccount`, `client.dto.PluggyTransaction`, `resource.dto.AccountResponse` e `resource.dto.TransactionResponse` importam enums de `service.enums`.
- `AccountResponse.isBrlBankAccount()` e `TransactionResponse.isBrazilianCredit()` guardam regra de negócio no DTO de resposta.
- `OwnerFanOut` é genérico e devolve `OwnersResponse<E>`, usando `ApiError` para a falha de um dono.

Veja proposal.md para a motivação.

## Goals / Non-Goals

**Goals:**
- Dependências em uma direção só: `resource -> service -> domain` e `service -> client`.
- `client.dto` e `resource.dto` sem dependência de `domain`.
- Regras de negócio no modelo de domínio.

**Non-Goals:**
- Sem banco, entidades nem JPA.
- Sem mudança no `OwnerFanOut`, em `OwnersResponse` e em `ApiError`.
- Sem mudança no `SummaryResponse` e nos tipos filhos.
- Sem mudança no JSON da API.

## Decisions

**1. Três pacotes de domínio.**
`domain.model` guarda os conceitos (`Account`, `Transaction`, `Bill`). `domain.enums` guarda os enums. `domain.result` guarda o que um service devolve e que não é um conceito do negócio: `TransactionPage`, `BillPage`, `OwnerAccountsResult` e `OwnerCreditCardsResult`. Os dois são `records`.

**2. `client.dto` e `resource.dto` ficam com `String` nos campos de tipo.**
Os enums não aparecem nos dois DTOs. O `PluggyMapper` converte `String` em enum, e o mapper de `resource` converte enum em `String` (`name()`). Assim só os dois mappers conhecem `domain`, e o ciclo some. Alternativa descartada: manter o enum nos DTOs, o que preserva a dependência de `client.dto` e `resource.dto` em `domain`.

**3. Valor `UNKNOWN` nos enums.**
`AccountTypesEnum`, `TransactionTypesEnum` ganham `UNKNOWN`, e `fromPluggy(String)` devolve `UNKNOWN` para valor nulo ou novo. Isso evita falha do Jackson com tipo desconhecido, sem configurar o `JsonMapper`. Efeito: o JSON mostra `UNKNOWN` no lugar do texto cru da Pluggy. Alternativa descartada: `READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE`, que mexe na configuração global do Jackson.

**4. Regras no domínio.**
`Account` ganha a regra de conta bancária em BRL com saldo, e `Transaction` ganha a regra de entrada em BRL com valor. Os DTOs de resposta ficam só com dados. O `CurrencyEnum` mantém o texto da moeda, e `currencyCode` continua `String` nos modelos, porque a Pluggy pode devolver outras moedas.

**5. Cartão é uma `Account` com dados de crédito.**
`Account` tem um campo opcional `credit` (`brand`, limites, datas). O mapper de `resource` monta `CreditCardResponse` a partir dele. Não há tipo `CreditCard` no domínio.

**6. Envelope e erro ficam em `resource.dto` nesta etapa.**
`OwnersResponse<T>` e `ApiError` continuam sendo usados por `OwnerFanOut` e pelos services. O `OwnerFanOut` não muda. Dependência residual aceita: `service` importa `OwnersResponse` e `ApiError` de `resource.dto`. A segunda etapa (fora desta change) a remove. Para isso, `OwnerAccountsResult` e `OwnerCreditCardsResult` levam `ApiError` e viajam dentro de `OwnersResponse`, e o controller os converte para `OwnerAccounts` e `OwnerCreditCards` (tipos de resposta que já existem).

**7. Resumo fica como está.**
`SummaryService` passa a ler `Account` e `Transaction`, mas continua montando `SummaryResponse`, `IncomeAccount` e `IncomeTransaction` de `resource.dto`. É uma dependência residual da mesma natureza da decisão 6.

**8. Mapper de resposta.**
Classe `ResponseMapper` em `resource`, com métodos estáticos de `domain` para `resource.dto`. O `FinancialDataController` chama o mapper, e os services não conhecem o formato da resposta (exceto os resíduos das decisões 6 e 7).

**9. Pacote dos enums.**
Os três enums vão de `service.enums` para `domain.enums`, separados dos modelos, com os nomes atuais (`AccountTypesEnum`, `TransactionTypesEnum`, `CurrencyEnum`).

## Risks / Trade-offs

- [Mais um tipo por conceito (`PluggyAccount`, `Account`, `AccountResponse`) e dois mappers] → Aceito. É o custo da separação, e os mappers são pequenos.
- [`UNKNOWN` esconde o valor original da Pluggy no JSON] → Aceito. Não havia caso real de tipo novo, e o tipo desconhecido já não retorna 500.
- [Dependência residual de `service` em `resource.dto`] → Documentada nas decisões 6 e 7, tratada em outra etapa.
- [Refactor sem mudança de comportamento pode quebrar o JSON sem aviso] → Os testes da tarefa 3 comparam o JSON de `/accounts`, `/credit-cards`, `/transactions` e `/bills` com o formato atual.
