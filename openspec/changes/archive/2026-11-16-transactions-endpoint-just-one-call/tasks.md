# Tasks

## 1. Erros e resultado

- [x] 1.1 Criar `InvalidRequestException` (400) e `AllAccountsFailedException` (502) em `exception` e mapeá-las em `ApiErrors.from`. Verificar com um `ApiErrorsTest` que cada exceção vira o status e a mensagem esperados e que as já existentes continuam mapeadas como antes, e `./gradlew test` passando
- [x] 1.2 Criar `OwnedTransaction`, `AccountFailure` e `TransactionListResult` em `domain.result`. Verificar que `./gradlew compileJava` passa

## 2. Serviço

- [x] 2.1 Criar `TransactionListService.list(owner, accountId, dateFrom, dateTo)` com a validação, o período, a consulta por conta, a ordenação e a falha por conta. Verificar com `TransactionListServiceTest` (serviços simulados e `Clock` fixo) cobrindo: todos os donos e contas, um dono, uma conta, `accountId` sem `owner` (400, sem chamar a Pluggy), dono desconhecido (404), conta de outro dono (erro normal), período sem datas, só `dateFrom`, só `dateTo` e os dois, ordem por data decrescente, empate por dono e por conta, ordem da Pluggy mantida na mesma conta e data, data nula por último, uma conta falhando (as outras ficam), um dono falhando ao listar as contas, dono sem contas, e nenhuma consulta com sucesso (502), e `./gradlew test` passando

## 3. Resposta e rota

- [x] 3.1 Criar `ConsolidatedTransactionsResponse`, `ConsolidatedTransaction` e `TransactionError` em `resource.dto`, o mapeamento no `ResponseMapper` e `GET /transactions` em `FinancialDataController`. Verificar com `ResponseMapperTest` (cada campo, `accountId` nulo no erro, `partial` pelos erros) e um teste que serializa a resposta com o `JsonMapper` e confere os nomes de campo do JSON, e `./gradlew test` passando

## 4. Documentação e dados reais

- [x] 4.1 Documentar `GET /transactions` no README (parâmetros, período padrão, ordem, falha parcial e um exemplo de resposta). Verificar que os campos do exemplo batem com os do teste de JSON
- [ ] 4.2 Verificar com a Pluggy real (exige as credenciais): que a ordem devolvida pela Pluggy é da data mais recente para a mais antiga, o tempo de resposta de um mês com todas as contas, o 404 de uma conta que é de outro dono e o tamanho da lista. Registrar o resultado no design.md
