# Design

## Context

Estado atual, observado no código:
- `AccountService` consulta `PluggyClient.listAccounts(owner, itemId)` para cada item do dono (`OwnerRegistry.require(owner).itemIds()`), usa `PluggyPage.resultsOrEmpty()` sem percorrer páginas, e entrega o resultado por dono pelo `OwnerFanOut.collect`, que isola a falha de cada dono e lança `AllOwnersFailedException` (502) se todos falharem.
- O resultado por dono vive em `domain.result` (`OwnerAccountsResult`, `OwnerCreditCardsResult`), vai dentro de `OwnersResponse<T>` e é traduzido pelo `ResponseMapper` para `OwnerAccounts` e `OwnerCreditCards` em `resource.dto`.
- Enums do domínio têm `UNKNOWN` e `fromPluggy(String)` (`AccountTypesEnum`, `TransactionTypesEnum`).
- Datas das faturas chegam como `2026-11-16T00:00:00.000Z` e são lidas como `LocalDateTime` sem converter fuso.

Da documentação da Pluggy:
- `GET /loans` recupera os empréstimos coletados para o item informado, e responde `{page, total, totalPages, results}`, o mesmo formato de `/accounts`.
- Um empréstimo traz `id`, `itemId`, `productName`, `kind`, `currencyCode`, `date` como campos obrigatórios. O resto é opcional: `contractNumber`, `type`, `contractAmount`, `contractDate`, `dueDate`, `installments` (`totalNumberOfInstallments`, `paidInstallments`, `contractRemainingNumber`, `pastDueInstallments`) e `payments` (`contractOutstandingBalance` e `releases`, com `paidDate`, `paidAmount` e `isOverParcelPayment`).
- Nenhum campo traz o valor da parcela mensal.

Veja proposal.md para a motivação e o escopo.

## Goals / Non-Goals

**Goals:**
- Expor os empréstimos com o mesmo contrato de falha parcial de `/accounts` e `/credit-cards`.
- Mostrar o que a Pluggy informa, sem inventar valor ausente.

**Non-Goals:**
- Sem seção de empréstimos no `/summary`.
- Sem estimar parcela mensal ou próximo vencimento.
- Sem juros, tarifas, garantias nem a lista completa de pagamentos.
- Sem paginar `GET /loans`: segue a premissa das contas, e a lista de empréstimos de um item é curta.
- Sem filtro por `kind` e sem tratamento especial do cheque especial.

## Decisions

**1. Mesmo caminho das contas.**
`PluggyApi.listLoans(itemId)` devolve `PluggyPage<PluggyLoan>`, `PluggyClient.listLoans(owner, itemId)` o expõe, e `LoanService.getLoans(owner)` percorre os itens do dono e usa `OwnerFanOut`. O resultado por dono é `OwnerLoansResult(owner, loans, error)` em `domain.result`, e a resposta é `OwnersResponse<OwnerLoans>`, como nos cartões. Alternativa descartada: acrescentar o método a `AccountService`, que passaria a misturar duas Pluggy diferentes sem ganho.

**2. Modelo `Loan` no domínio.**
`Loan(id, itemId, contractNumber, productName, kind, type, contractAmount, currencyCode, contractDate, dueDate, installments, outstandingBalance, lastPayment)`:
- `kind` é o enum `LoanKindEnum` (`LOAN`, `FINANCING`, `INVOICE_FINANCING`, `UNARRANGED_ACCOUNT_OVERDRAFT`, `UNKNOWN`), com `fromPluggy`, em `domain.enums`.
- `type` fica como texto. O Open Finance define dezenas de valores, e nenhuma regra do sistema depende dele.
- `Installments(Integer total, Integer paid, Integer remaining, Integer pastDue)`, nulo quando a Pluggy não informa o bloco.
- `Payment(LocalDate paidDate, BigDecimal paidAmount)`.
- `contractDate` e `dueDate` são `LocalDate`: só o dia importa, e a conversão usa a data do valor lido, sem converter fuso. Isso difere de `Bill`, que mantém `LocalDateTime`, mas o JSON de empréstimo fica sem hora.

**3. `lastPayment` ignora pagamento avulso.**
É o `release` de maior `paidDate` com `isOverParcelPayment` diferente de `true` e com `paidDate` e `paidAmount` preenchidos. Um pagamento avulso não representa a parcela regular, e o objetivo do campo é permitir estimar a parcela em uma change futura. Sem nenhum pagamento regular, vem nulo.

**4. `PluggyLoan` só com o que usamos.**
Record com `@JsonIgnoreProperties(ignoreUnknown = true)`, como os outros. Lê os campos da decisão 2 e os blocos aninhados `installments` e `payments` (com `releases`). Os demais ficam de fora.

**5. DTOs de resposta.**
`OwnerLoans(owner, loans, error)` e `LoanResponse` em `resource.dto`, com `kind` como `String` e as datas como `LocalDate`. O `ResponseMapper` traduz. Campos nulos saem como `null` no JSON, como em `/accounts`.

**6. Rota.**
`GET /loans` em `FinancialDataController`, com `owner` opcional, chamando `LoanService` e o mapper.

## Risks / Trade-offs

- [Formato da resposta e nomes de campo vêm da documentação, não de dados reais] → Teste de leitura com o JSON de exemplo da documentação, e a tarefa 3.2 confere com a Pluggy real. Campo com nome errado vira nulo, sem erro.
- [Parâmetro `itemId` de `GET /loans` não visto em chamada real] → Tarefa 3.2.
- [Instituição sem suporte a empréstimos devolve lista vazia ou erro] → Lista vazia sem erro é o caso normal. Um erro da Pluggy entra como falha do dono, como nas contas.
- [Não paginar corta empréstimos além da primeira página] → Aceito, igual às contas. Se aparecer, vira uma change própria.
- [`kind` desconhecido vira `UNKNOWN`] → Aceito, como nos outros enums.
- [`lastPayment` pode não refletir a parcela atual (reajuste, SAC)] → Registrado. A change do resumo decide se usa o campo.
