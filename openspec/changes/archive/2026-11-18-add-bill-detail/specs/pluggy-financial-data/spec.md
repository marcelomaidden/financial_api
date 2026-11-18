# Spec Delta

## ADDED Requirements

### Requirement: Detalhe de fatura de cartão
O sistema SHALL expor `GET /bill/{id}` com os parâmetros obrigatórios `owner` e `accountId`, retornando a fatura de id `id` e as compras dela, consultadas na Pluggy com as credenciais do dono informado. O `id` SHALL ser o mesmo que `GET /owners/{owner}/accounts/{accountId}/bills` devolve em `bills[].id`. A resposta SHALL conter `owner`, `accountId`, `bill` e `transactions`. O sistema SHALL responder 400 no formato padrão de erro, sem consultar a Pluggy, quando `owner` ou `accountId` estiver ausente ou em branco.

#### Scenario: Fatura com compras
- **WHEN** o cliente chama `GET /bill/{id}?owner=<nome>&accountId=<id>` com um dono configurado, uma conta de cartão desse dono e uma fatura dessa conta
- **THEN** a resposta é 200 com `owner` e `accountId` iguais aos informados, a fatura em `bill` e as compras dela em `transactions`

#### Scenario: Id da listagem de faturas
- **WHEN** o cliente usa em `GET /bill/{id}` um `bills[].id` devolvido pela listagem de faturas da mesma conta
- **THEN** a resposta traz essa fatura em `bill.id`

#### Scenario: Owner ausente
- **WHEN** o cliente chama `GET /bill/{id}?accountId=<id>` sem `owner`
- **THEN** o sistema responde 400 no formato padrão de erro, sem consultar a Pluggy

#### Scenario: AccountId ausente
- **WHEN** o cliente chama `GET /bill/{id}?owner=<nome>` sem `accountId`
- **THEN** o sistema responde 400 no formato padrão de erro, sem consultar a Pluggy

#### Scenario: Parâmetro em branco
- **WHEN** o cliente chama `GET /bill/{id}` com `owner=` ou `accountId=` em branco
- **THEN** o sistema responde 400 no formato padrão de erro, sem consultar a Pluggy

#### Scenario: Dono desconhecido
- **WHEN** o cliente chama `GET /bill/{id}?owner=<nome>&accountId=<id>` com um dono que não está na configuração
- **THEN** o sistema responde 404 sem consultar a Pluggy, com o mesmo erro dos outros endpoints

#### Scenario: Fatura inexistente
- **WHEN** a Pluggy responde 404 à consulta da fatura com as credenciais do dono informado
- **THEN** o sistema responde 404 ao cliente

#### Scenario: Falha da Pluggy ao buscar a fatura
- **WHEN** a Pluggy falha ao devolver a fatura, com erro diferente de 404
- **THEN** o sistema responde com o erro mapeado para essa falha, sem consultar as compras

#### Scenario: Falha da Pluggy ao buscar as compras
- **WHEN** a Pluggy devolve a fatura e falha ao devolver as compras, em qualquer página
- **THEN** o sistema responde com o erro mapeado para essa falha, sem corpo parcial com a fatura e sem compras

#### Scenario: Chave de API ausente
- **WHEN** o cliente chama `GET /bill/{id}` sem o cabeçalho `X-API-Key` ou com chave errada
- **THEN** o sistema responde 401 sem consultar a Pluggy

#### Scenario: Conta que não é a da fatura
- **WHEN** o cliente informa um `accountId` que existe para o dono, mas não é a conta da fatura
- **THEN** a resposta é 200 com a fatura e `transactions` vazio, porque o sistema não confere a conta contra a fatura

### Requirement: Campos da fatura no detalhe
O sistema SHALL devolver em `bill` o `id`, `dueDate`, `closingDate`, `totalAmount`, `currencyCode`, `minimumPaymentAmount`, `allowsInstallments`, `financeCharges` e `payments`. As datas SHALL ser `yyyy-mm-dd`, com o dia lido da Pluggy, sem conversão de fuso, e os valores SHALL ser numéricos. Cada item de `financeCharges` SHALL ter `id`, `type`, `amount`, `currencyCode` e `additionalInfo`. Cada item de `payments` SHALL ter `id`, `valueType`, `paymentDate`, `paymentMode`, `amount` e `currencyCode`. O sistema SHALL deixar nulos `closingDate`, `minimumPaymentAmount`, `allowsInstallments`, `additionalInfo`, `paymentDate` e `paymentMode` quando a Pluggy não os informar, e SHALL devolver lista vazia quando a Pluggy não informar encargos ou pagamentos.

#### Scenario: Fatura completa
- **WHEN** a Pluggy informa vencimento `2026-11-20T00:00:00`, fechamento `2026-11-13T00:00:00`, total 500, mínimo 50, `allowsInstallments` verdadeiro, um encargo de IOF de 20.51 e um pagamento de 500
- **THEN** `bill` traz `dueDate` `2026-11-20`, `closingDate` `2026-11-13`, esses valores como números, o encargo em `financeCharges` e o pagamento em `payments`

#### Scenario: Campos opcionais ausentes
- **WHEN** a Pluggy não informa fechamento, pagamento mínimo, `allowsInstallments`, `additionalInfo` do encargo nem data e modo do pagamento
- **THEN** esses campos vêm nulos e a fatura aparece na resposta

#### Scenario: Sem encargos e sem pagamentos
- **WHEN** a Pluggy devolve a fatura sem encargos e sem pagamentos, ou sem esses campos
- **THEN** `financeCharges` e `payments` vêm como listas vazias

### Requirement: Compras da fatura no detalhe
O sistema SHALL devolver em `transactions` as compras que a Pluggy associa à fatura e à conta informadas, percorrendo todas as páginas da Pluggy sem exigir cursor do cliente. Cada compra SHALL ter `id`, `date`, `description`, `amount`, `currencyCode`, `type` e `status`, e `category` quando a Pluggy a informar. Compra parcelada SHALL ter `installment` com `number` e `total`, e compra sem parcela SHALL NOT ter o campo `installment`. O sistema SHALL ordenar as compras por `date` da mais recente para a mais antiga, mantendo a ordem da Pluggy entre compras da mesma data, e SHALL devolver `date` em `yyyy-mm-dd`.

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
- **WHEN** a Pluggy não devolve nenhuma compra para a fatura
- **THEN** `transactions` é uma lista vazia e a resposta é 200

#### Scenario: Categoria ausente
- **WHEN** a Pluggy não informa a categoria de uma compra
- **THEN** `category` vem nula e a compra aparece na lista
