# Proposal

## Why

`GET /bill/{id}` devolve as compras internacionais só com o valor na moeda da compra, por exemplo `amount` -58 e `currencyCode` `USD`. O valor convertido para a moeda do cartão, que é o que pesa na fatura, fica de fora. A Pluggy já envia esse valor em `amountInAccountCurrency`, e o domínio já o guarda em `Transaction`, mas a resposta do detalhe o descarta. Sem ele, o frontend não consegue mostrar quanto a compra custou em reais.

## What Changes

- Cada compra em `transactions` de `GET /bill/{id}` passa a trazer `amountInAccountCurrency` quando a `currencyCode` da compra não for BRL e a Pluggy informar o valor convertido. O campo é omitido nas compras em BRL e quando a Pluggy não o informa.
- A regra é a mesma de `expenses.transactions` em `GET /summary`. O valor sai como a Pluggy o envia, com o mesmo sinal de `amount`.
- Os demais campos, a ordem, a janela de datas e os erros de `GET /bill/{id}` não mudam. A mudança é aditiva e compatível com o frontend atual, que ignora o campo novo.

Fora de escopo: o total da fatura em BRL ou a soma das compras, a conversão de encargos e pagamentos, e mudanças em outros endpoints.

## Capabilities

### New Capabilities

### Modified Capabilities
- `pluggy-financial-data`: ganha o requisito "Valor na moeda da conta nas compras da fatura". Ele inclui `amountInAccountCurrency` nas compras em moeda diferente de BRL e o omite nas compras em BRL. O requisito "Compras da fatura no detalhe" não muda.

## Impact

- Código: `BillDetailResponse.BillTransaction` (campo novo com `@JsonInclude(NON_NULL)`) e `ResponseMapper.toBillTransaction`. `PluggyTransaction`, `Transaction` e `PluggyMapper` já leem o campo.
- Testes: `ResponseMapperTest` e `BillDetailJsonTest` (campo presente em compra USD e ausente em compra BRL).
- README: a seção do detalhe da fatura e o exemplo de resposta passam a mostrar o campo.
- Frontend: a change `show-bill-transaction-account-currency` no `financial_api_frontend` consome o campo.
