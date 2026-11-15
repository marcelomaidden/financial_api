# Tasks

## 1. Modelo de domínio

- [x] 1.1 Criar em `domain.enums` os enums `AccountTypesEnum`, `TransactionTypesEnum` e `CurrencyEnum`, com valor `UNKNOWN` nos dois primeiros e `fromPluggy(String)` que devolve `UNKNOWN` para nulo ou valor novo. Os enums de `service.enums` ficam até a tarefa 4.3. Verificar com testes unitários de `fromPluggy` para valor conhecido, nulo e desconhecido, e `./gradlew test` passando
- [x] 1.2 Criar `Account` (com `credit` opcional), `Transaction` e `Bill` em `domain.model`, com as regras de conta bancária em BRL com saldo e de entrada em BRL com valor. Verificar com testes unitários dessas regras (tipo, moeda e valor nulo) e `./gradlew test` passando

## 2. Cliente e mapper da Pluggy

- [x] 2.1 Voltar o campo `type` de `PluggyAccount` e `PluggyTransaction` para `String` e remover o import de `service.enums`. Verificar que `./gradlew compileJava` passa e `client.dto` não importa `domain` nem `service` (busca por `import` no pacote)
- [x] 2.2 Fazer o `PluggyMapper` converter `client.dto` em `domain.model`, usando `fromPluggy` para os tipos e levando `creditData` para `Account.credit`. Verificar com testes unitários do mapper para conta bancária, cartão com e sem `creditData`, transação e fatura, incluindo um `type` desconhecido que vira `UNKNOWN`

## 3. Services

- [x] 3.1 Criar em `domain.result` os tipos `TransactionPage`, `BillPage`, `OwnerAccountsResult` e `OwnerCreditCardsResult`. Fazer `TransactionService` e `BillService` devolverem `TransactionPage` e `BillPage`, e `AccountService` devolver `OwnersResponse<OwnerAccountsResult>` e `OwnersResponse<OwnerCreditCardsResult>`, sem alterar o `OwnerFanOut`. Verificar que `./gradlew compileJava` passa e que `OwnerFanOut.java` não aparece em `git diff`
- [x] 3.2 Fazer o `SummaryService` ler `Account` e `Transaction` e usar as regras do domínio, mantendo o `SummaryResponse`. Atualizar `SummaryServiceTest` para montar os tipos de domínio e verificar que os 8 testes existentes continuam passando

## 4. Resposta da API

- [x] 4.1 Criar `ResponseMapper` em `resource` (domínio para `resource.dto`, com `name()` nos enums) e usá-lo no `FinancialDataController` para `/accounts`, `/credit-cards`, `/transactions` e `/bills`. Verificar com testes unitários do mapper que cada campo da resposta recebe o valor esperado
- [x] 4.2 Tirar o enum e as regras de `AccountResponse`, `TransactionResponse` e demais DTOs de `resource.dto`, deixando `String` nos campos de tipo. Verificar que `./gradlew test` passa e que `resource.dto` não importa `domain`
- [x] 4.3 Remover o pacote `service.enums`. Verificar com busca que nenhum arquivo importa `service.enums` e `./gradlew test` passa
- [ ] 4.4 Verificação final com `curl` contra a Pluggy em `/accounts`, `/credit-cards`, `/transactions`, `/bills` e `/summary`, comparando o JSON com o de antes da mudança. Exige as credenciais da Pluggy configuradas
