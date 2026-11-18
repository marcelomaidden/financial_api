# Spec Delta

## MODIFIED Requirements

### Requirement: Compras da fatura no detalhe
O sistema SHALL devolver em `transactions` as transações da conta informada que a Pluggy marca com o identificador da fatura (`creditCardMetadata.billId` igual ao `id` da fatura), percorrendo todas as páginas da Pluggy sem exigir cursor do cliente. Cada compra SHALL ter `id`, `date`, `description`, `amount`, `currencyCode`, `type` e `status`, e `category` quando a Pluggy a informar. Compra parcelada SHALL ter `installment` com `number` e `total`, e compra sem parcela SHALL NOT ter o campo `installment`. O sistema SHALL ordenar as compras por `date` da mais recente para a mais antiga, mantendo a ordem da Pluggy entre compras da mesma data, e SHALL devolver `date` em `yyyy-mm-dd`.

#### Scenario: Várias páginas de compras
- **WHEN** a fatura tem mais compras do que cabem em uma página da Pluggy
- **THEN** `transactions` contém as compras de todas as páginas

#### Scenario: Ordem por data
- **WHEN** a Pluggy devolve compras de datas diferentes fora de ordem
- **THEN** `transactions` as lista da data mais recente para a mais antiga

#### Scenario: Mesma data
- **WHEN** duas compras têm a mesma `date`
- **THEN** elas aparecem na ordem em que a Pluggy as devolveu

#### Scenario: Compra parcelada
- **WHEN** a Pluggy informa a parcela 3 de 10 em uma compra
- **THEN** a compra traz `installment` com `number` 3 e `total` 10

#### Scenario: Compra sem parcela
- **WHEN** a Pluggy não informa o número ou o total de parcelas de uma compra
- **THEN** a compra não traz o campo `installment`

#### Scenario: Fatura sem compras
- **WHEN** a Pluggy não devolve nenhuma transação marcada com o `id` da fatura
- **THEN** `transactions` é uma lista vazia e a resposta é 200

#### Scenario: Categoria ausente
- **WHEN** a Pluggy não informa a categoria de uma compra
- **THEN** `category` vem nula e a compra aparece na lista

#### Scenario: Transação de outra fatura
- **WHEN** a conta tem, nas mesmas datas, transações marcadas com outra fatura ou sem fatura
- **THEN** essas transações não aparecem em `transactions`

## ADDED Requirements

### Requirement: Janela de datas das compras da fatura
O sistema SHALL buscar as transações da fatura na Pluggy dentro de uma janela de datas derivada da própria fatura: do fechamento menos 1 mês e 10 dias até o fechamento mais 10 dias. Sem fechamento, a janela SHALL ir do vencimento menos 1 mês e 20 dias até o vencimento. Sem as duas datas, o sistema SHALL buscar sem janela. Uma transação da fatura com `date` fora da janela SHALL NOT aparecer em `transactions`.

#### Scenario: Janela pelo fechamento
- **WHEN** a fatura tem fechamento em 2026-11-05
- **THEN** o sistema busca as transações com `date` de 2026-09-25 a 2026-11-15

#### Scenario: Janela pelo vencimento
- **WHEN** a fatura não tem fechamento e vence em 2026-11-15
- **THEN** o sistema busca as transações com `date` de 2026-09-25 a 2026-11-15

#### Scenario: Fatura sem datas
- **WHEN** a fatura não tem fechamento nem vencimento
- **THEN** o sistema busca as transações da conta sem janela de datas e devolve só as marcadas com o `id` da fatura

#### Scenario: Parcela de compra antiga lançada no ciclo
- **WHEN** a parcela 2 de 4 de uma compra de 2026-09-18 está na fatura com fechamento em 2026-11-05 e tem `date` 2026-10-21
- **THEN** a parcela aparece em `transactions`

#### Scenario: Transação da fatura fora da janela
- **WHEN** uma transação marcada com o `id` da fatura tem `date` anterior ao início da janela
- **THEN** ela não aparece em `transactions`, e a resposta é 200 com as demais compras
