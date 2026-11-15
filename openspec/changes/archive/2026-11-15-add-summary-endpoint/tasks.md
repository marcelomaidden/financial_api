# Tasks

## 1. Verificação dos dados

- [ ] 1.1 Consultar `GET /accounts` e as transações de cada dono e confirmar o `type` das contas bancárias (`BANK`), o `type` das entradas (`CREDIT`) e a moeda (`BRL`), registrando o resultado no design.md

## 2. Resumo no service

- [x] 2.1 Criar os DTOs de resposta do resumo em `resource/dto` (`incomes` com `available` e `transactions`, cada um com `total` e `detail`; conta com `id`, `name`, `balance`, `currencyCode`; transação com `id`, `description`, `amount`, `currencyCode`, `accountId`; `errors` e `partial`) e verificar que `./gradlew compileJava` passa
- [x] 2.2 Implementar o `SummaryService` (usando `AccountService.getAccounts(null)` e `TransactionService`) que agrega saldos e entradas `CREDIT` do mês corrente das contas `BANK` em BRL, com falha parcial e 502 quando todos falham, e verificar com testes unitários dos cenários de soma, filtro de tipo, moeda e mês, transferência entre donos, falha nas contas, falha nas transações e todos falhando

## 3. Endpoint e documentação

- [ ] 3.1 Adicionar `GET /summary` em `FinancialDataController` e verificar com `curl` contra a Pluggy que `incomes` e `partial` aparecem e os totais conferem com `/accounts` e `/transactions`
- [ ] 3.2 Documentar `GET /summary` na tabela de endpoints do README, com um exemplo de resposta, e verificar que o exemplo bate com a resposta real
