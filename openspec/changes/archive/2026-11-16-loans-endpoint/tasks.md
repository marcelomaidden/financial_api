# Tasks

## 1. Cliente e modelo

- [x] 1.1 Criar `PluggyLoan` em `client.dto` (campos da decisão 4, com `installments` e `payments` aninhados), `PluggyApi.listLoans(itemId)` e `PluggyClient.listLoans(owner, itemId)`. Verificar com um teste que lê o JSON de exemplo da documentação com o `JsonMapper` e confere os campos, incluindo as datas com `Z`, os blocos aninhados e a tolerância a campos desconhecidos, e `./gradlew test` passando
- [x] 1.2 Criar `LoanKindEnum` em `domain.enums` e `Loan` em `domain.model`, e `PluggyMapper.toLoan`, com `lastPayment` pelo pagamento regular de maior `paidDate`. Verificar com `EnumsTest` (`fromPluggy` para valor conhecido, nulo e novo) e `PluggyMapperTest` cobrindo: empréstimo completo, sem `installments` nem `payments`, só o número de parcelas informado, `kind` desconhecido, pagamento mais recente entre vários, pagamento avulso ignorado, só pagamentos avulsos, e datas viradas em dia sem mudar de dia, com `./gradlew test` passando

## 2. Serviço e rota

- [x] 2.1 Criar `OwnerLoansResult` em `domain.result` e `LoanService.getLoans(owner)`, que percorre os itens do dono e usa `OwnerFanOut`. Verificar com `LoanServiceTest` (cliente da Pluggy e registro de donos simulados) cobrindo: todos os donos, um dono específico, vários itens do mesmo dono somados, dono sem empréstimos, um dono falhando enquanto o outro tem sucesso, todos falhando (502) e dono desconhecido (404), e `./gradlew test` passando
- [x] 2.2 Criar `OwnerLoans` e `LoanResponse` em `resource.dto`, o mapeamento no `ResponseMapper` e `GET /loans?owner=` em `FinancialDataController`. Verificar com `ResponseMapperTest` (cada campo, `kind` como texto e campos nulos) e um teste que serializa a resposta com o `JsonMapper` e confere os nomes de campo do JSON, e `./gradlew test` passando

## 3. Documentação e dados reais

- [x] 3.1 Documentar `GET /loans` na tabela de endpoints do README, com um exemplo de resposta. Verificar que os campos do exemplo batem com os do teste de JSON
- [ ] 3.2 Verificar com a Pluggy real (exige as credenciais): que `GET /loans?itemId=` responde no formato esperado, quais campos as instituições dos donos preenchem (parcelas, saldo, pagamentos), os valores reais de `kind` e de `type`, e se a lista pagina. Registrar o resultado no design.md
