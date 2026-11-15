# Proposal

## Why

Hoje os services devolvem os DTOs de resposta da API (`resource.dto`), e `client.dto` e `resource.dto` importam enums de `service.enums`. As dependências formam um ciclo entre `service`, `resource` e `client`. Também há regras de negócio dentro de DTOs de resposta (`isBrlBankAccount()`, `isBrazilianCredit()`). Separar um modelo de domínio do formato da resposta desfaz o ciclo e deixa a API mudar sem mexer nas regras.

## What Changes

- Novo pacote `domain.model` com os conceitos de negócio: `Account`, `Transaction` e `Bill`. Os enums de tipo de conta, tipo de transação e moeda vão de `service.enums` para um pacote próprio, `domain.enums`.
- Novo pacote `domain.result` com os resultados de consulta dos services: página de transações, página de faturas e contas ou cartões por dono.
- As regras que hoje estão nos DTOs de resposta (conta bancária em BRL, entrada em BRL) passam para `Account` e `Transaction`.
- `client.dto` e `resource.dto` deixam de importar enums. O tipo vira `String` nos dois, e o `PluggyMapper` converte para o enum, com valor `UNKNOWN` quando a Pluggy devolve um tipo novo. Assim um tipo desconhecido não derruba `/accounts`, `/credit-cards` nem `/transactions`.
- Os services passam a devolver `domain`. Um mapper em `resource` converte `domain` em `resource.dto` no controller. O contrato JSON da API não muda, exceto um tipo desconhecido da Pluggy, que sai como `UNKNOWN`.
- Fora de escopo: banco de dados e entidades, o `OwnerFanOut`, `OwnersResponse` e `ApiError`, o `SummaryResponse` e seus tipos filhos, e qualquer mudança de comportamento da API.

## Capabilities

### New Capabilities

### Modified Capabilities

Nenhuma. A mudança é interna, e o comportamento da API não muda. Por isso a change declara `skip_specs`.

## Impact

- Código: `AccountService`, `TransactionService`, `BillService`, `SummaryService`, `PluggyMapper`, `FinancialDataController`, `client.dto` e `resource.dto`. O pacote `service.enums` é removido.
- API: sem mudança nas rotas nem no JSON, salvo o caso do tipo desconhecido.
- Testes: `SummaryServiceTest` passa a montar `Account` e `Transaction`. Novos testes para os mappers.
- Ordem: a `add-summary-endpoint` já está arquivada, então não há conflito de arquivos.
