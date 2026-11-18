# Tasks

## 1. Resposta do detalhe

- [x] 1.1 Em `BillDetailResponse.BillTransaction`, acrescentar `amountInAccountCurrency` (`BigDecimal`, `@JsonInclude(NON_NULL)`, como em `ExpenseTransaction`). Em `ResponseMapper.toBillTransaction`, preencher o campo com `tx.amountInAccountCurrency()` só quando `tx.currencyCode()` não for BRL (`CurrencyEnum.BRL.getType()`), e com `null` nos demais casos. Verificar com `ResponseMapperTest`: compra USD com -298.19 traz o valor, compra BRL com valor informado traz `null`, e compra USD sem valor traz `null`. `./gradlew test` passando
- [x] 1.2 Em `BillDetailJsonTest`, verificar que a compra USD serializa `amountInAccountCurrency` como número, e que a compra BRL e a compra USD sem valor não têm o campo (`has("amountInAccountCurrency")` falso). `./gradlew test` passando

## 2. Documentação

- [x] 2.1 Na seção "Detalhe de uma fatura" do README, dizer que `amountInAccountCurrency` aparece só em compra fora de BRL, e acrescentar ao exemplo de resposta uma compra em USD com o campo. Verificar que os nomes e formatos do exemplo batem com o `BillDetailJsonTest` e que `./gradlew build` e `openspec validate --all` passam
