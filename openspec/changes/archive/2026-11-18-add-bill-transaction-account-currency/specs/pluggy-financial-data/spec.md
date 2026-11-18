# Spec Delta

## ADDED Requirements

### Requirement: Valor na moeda da conta nas compras da fatura
O sistema SHALL incluir `amountInAccountCurrency` em cada compra de `transactions` de `GET /bill/{id}` cuja `currencyCode` não seja BRL, com o valor que a Pluggy informa, no mesmo sinal de `amount`. O sistema SHALL omitir o campo nas compras em BRL e quando a Pluggy não informar o valor.

#### Scenario: Compra em dólar com valor convertido
- **WHEN** uma compra da fatura tem `amount` -58, `currencyCode` `USD` e a Pluggy informa `amountInAccountCurrency` -298.19
- **THEN** a compra traz `amountInAccountCurrency` -298.19, com `amount` -58 e `currencyCode` `USD`

#### Scenario: Compra em BRL
- **WHEN** uma compra da fatura tem `currencyCode` `BRL`
- **THEN** a compra não traz o campo `amountInAccountCurrency`, mesmo que a Pluggy o informe

#### Scenario: Compra em outra moeda sem valor convertido
- **WHEN** uma compra da fatura tem `currencyCode` `USD` e a Pluggy não informa `amountInAccountCurrency`
- **THEN** a compra não traz o campo `amountInAccountCurrency` e aparece normalmente em `transactions`
