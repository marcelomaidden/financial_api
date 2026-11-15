# Proposal

## Why

`expenses.transactions` mostra as compras de cartão ainda sem fatura, mas hoje filtra pela data da compra (`date`) e corta no dia de hoje. Uma parcela chega à Pluggy com a data da compra original, que fica fora da janela, e no Padrão A (todas as parcelas de uma vez) as parcelas futuras têm data futura e também ficam fora. O total da fatura aberta sai menor do que a fatura real, e a compra parcelada aparece sem a informação de qual parcela é.

## What Changes

- `expenses.transactions` deixa de filtrar por `date`. A consulta usa `createdAtFrom` da Pluggy, que é a data em que a transação chegou, e não pode ser combinada com `dateFrom`.
- A janela passa a ser por cartão, derivada da fatura mais recente que a Pluggy devolve para ele (`billClosingDate` e `dueDate`):
  - Próximo fechamento: o primeiro `billClosingDate + k meses` depois de hoje. Vencimento da fatura aberta: o próximo fechamento mais a distância entre fechamento e vencimento da fatura mais recente.
  - Limite inferior (`createdAtFrom`): o fechamento anterior menos 10 dias.
  - Limite superior (por `date`): o vencimento da fatura aberta, mais 5 dias de folga para o vencimento que escorrega em fim de semana. Isso exclui as parcelas futuras do Padrão A.
  - Cartão sem fatura devolvida, ou com a consulta de faturas falhando: janela padrão, do dia 1 do mês anterior até hoje.
- Cada compra de `expenses.transactions` que for parcela traz `installment` com `number` e `total`. Compra à vista não traz o campo.
- `dateFrom` e `dateTo` deixam de afetar `expenses.transactions`, porque o estado "sem fatura" é o do momento atual. Eles continuam valendo para `owed` e `incomes`.
- Fora de escopo: projeção de parcelas futuras (quanto já está comprometido nos próximos meses), webhooks da Pluggy, mudança em `/transactions` e em `owed`, e conversão de moeda.

## Capabilities

### New Capabilities

### Modified Capabilities
- `financial-summary`: a regra de `expenses.transactions` passa a usar a janela por cartão e a data de chegada, e as compras parceladas passam a trazer a parcela.

## Impact

- Código: `PluggyApi`, `PluggyClient` e `TransactionService` (consulta por `createdAtFrom`), `PluggyTransaction` e `Transaction` (campos de parcela), `ExpensesSummaryService` e `ExpensesPeriod` (janela por cartão), um cálculo novo de ciclo da fatura, `ExpenseTransaction` (campo `installment`) e `PluggyBill` (comentário das datas).
- API: `GET /summary` muda o conteúdo de `expenses.transactions` e ganha o campo `installment`. `dateFrom` e `dateTo` deixam de afetar essa parte.
- Pluggy: as faturas, que já são buscadas para `owed`, passam a servir também para a janela. As transações do cartão passam a ser buscadas por `createdAtFrom`.
- README: atualizar a descrição de `expenses.transactions` e dos períodos.
- Ordem: parte do estado atual da `expenses-summary`, já arquivada.
