# Proposal

## Why

A Pluggy tem um produto de empréstimos, e a API não o expõe. Antes de decidir como empréstimos entram no resumo, é preciso ver o que as instituições dos donos devolvem de verdade: os campos de parcela e de saldo são opcionais, e o valor da parcela mensal não existe como campo. Um endpoint de leitura, no mesmo formato de `/accounts` e `/credit-cards`, entrega esse dado sem antecipar a modelagem do resumo.

## What Changes

- Novo endpoint `GET /loans?owner=`, com a mesma forma de `GET /credit-cards`: uma entrada por dono, com a lista de empréstimos dele e um campo `error` na entrada do dono que falhar. Sem `owner`, traz todos os donos. Se todos falharem, a resposta é 502.
- A consulta é por item (`GET /loans?itemId=` na Pluggy), percorrendo todos os itens de cada dono, como as contas.
- Cada empréstimo traz:
  - identificação: `id`, `itemId`, `contractNumber`, `productName`;
  - `kind` (`LOAN`, `FINANCING`, `INVOICE_FINANCING`, `UNARRANGED_ACCOUNT_OVERDRAFT`, com `UNKNOWN` para um valor que a Pluggy passe a devolver) e `type` como texto, porque o Open Finance define dezenas de tipos;
  - contrato: `contractAmount`, `currencyCode`, `contractDate` e `dueDate` (fim do contrato);
  - parcelas: total, pagas, restantes e em atraso;
  - `outstandingBalance`, o saldo para quitar (`contractOutstandingBalance`);
  - `lastPayment` (`paidDate` e `paidAmount`), o pagamento regular mais recente, sem contar pagamentos avulsos, quando houver.
- Os campos que a instituição não informa ficam nulos.
- Fora de escopo: a seção de empréstimos no `GET /summary`, estimativa do valor ou da data da próxima parcela, juros, tarifas, garantias e o detalhe de cada pagamento, e tratamento especial do cheque especial.

## Capabilities

### New Capabilities

### Modified Capabilities
- `pluggy-financial-data`: ganha a listagem de empréstimos por dono, com a mesma regra de falha parcial de contas e cartões.

## Impact

- Código: `PluggyApi` e `PluggyClient` (chamada de empréstimos por item), `PluggyLoan` em `client.dto`, `Loan` e um enum de `kind` em `domain`, `LoanService` e o resultado por dono em `domain.result`, `OwnerLoans` e `LoanResponse` em `resource.dto`, `ResponseMapper` e `FinancialDataController`.
- API: nova rota `GET /loans`. As rotas existentes não mudam.
- Pluggy: uma chamada de empréstimos por item de cada dono.
- README: documentar a rota.
- Dados reais: a verificação do formato da resposta exige as credenciais da Pluggy. O formato exato (nomes e se `GET /loans` pagina) vem da documentação e ainda não foi visto em dados reais.
- Seguinte: uma change própria para a seção de empréstimos do resumo, desenhada depois de ver os dados reais.
