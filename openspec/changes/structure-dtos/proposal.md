# Proposal

## Why

Hoje os services devolvem os DTOs de resposta da API (`resource.dto`), e os DTOs de `client.dto` e `resource.dto` importam enums de `service.enums`. As dependências formam um ciclo entre `service` e `resource`. Também há regras de negócio dentro de DTOs de resposta (`isBrazilianCredit()`, `isBRLBankTransaction()`) e nomes que não dizem o que guardam (`LanguagesEnum` guarda uma moeda). Separar um modelo de domínio do formato da resposta desfaz o ciclo e deixa a API mudar sem mexer nas regras.

## What Changes

- Novo pacote `domain.model` com os conceitos de negócio: `Account`, `Transaction`, `Bill`, `Money` e os enums de tipo de conta, tipo de transação e moeda.
- Os enums saem de `service.enums` e vão para `domain.model`. `LanguagesEnum` passa a ter nome de moeda.
- As regras que hoje estão nos DTOs de resposta (conta bancária em BRL, entrada em BRL) passam para o modelo de domínio, com nomes que dizem o que verificam.
- O `PluggyMapper` converte `client.dto` em `domain.model`. Os services passam a receber e devolver `domain.model`.
- Um mapper em `resource` converte `domain.model` em `resource.dto`. O contrato JSON da API não muda.
- Um `type` desconhecido vindo da Pluggy não derruba `/accounts`, `/credit-cards` nem `/transactions`.
- Fora de escopo: banco de dados e entidades, o envelope de falha parcial (`OwnersResponse`, `ApiError`, `OwnerFanOut`), o `SummaryResponse` e seus tipos filhos, e qualquer mudança de comportamento da API.

## Capabilities

### New Capabilities

### Modified Capabilities

Nenhuma. A mudança é interna, e o comportamento observável da API fica igual.

## Impact

- Código: todos os services (`AccountService`, `TransactionService`, `BillService`, `SummaryService`), `PluggyMapper`, `FinancialDataController`, `client.dto` e `resource.dto`. Pacote `service.enums` removido.
- API: sem mudança nas rotas nem no JSON.
- Testes: `SummaryServiceTest` passa a usar o modelo de domínio nas entradas.
- Ordem: aplicar depois de fechar a `add-summary-endpoint`, porque os dois mexem nos mesmos arquivos.
