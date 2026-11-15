# Proposal

## Why

O `SummaryService` reúne tudo do resumo de `incomes`: busca as contas, percorre os donos, acumula erros, pagina as transações e monta a resposta. A seção `expenses` (change `expenses-summary`) precisa do mesmo laço por dono, da mesma paginação e do mesmo tratamento de falha parcial. Sem separar antes, o serviço dobra de tamanho e duplica a paginação. Separar agora, sem mudar comportamento, mantém o diff de `expenses` só com o conteúdo novo.

## What Changes

- `SummaryService` passa a ser a fachada de `GET /summary`. Ele busca as contas uma única vez, chama o serviço de cada seção, une os erros e calcula `partial`.
- Novo `IncomesSummaryService`, que recebe a lógica atual de `incomes` (contas `BANK` em BRL, entradas `CREDIT` do período, falha por dono). Ele devolve a seção e os erros dela.
- A paginação de transações do período sai do `SummaryService` e vai para um ponto comum, para os serviços das seções reutilizarem.
- Sem mudança de comportamento: mesma rota, mesmo JSON, mesmas regras de falha parcial e de 502. Por isso a change declara `skip_specs`.
- Fora de escopo: `ExpensesSummaryService` e a seção `expenses` (change `expenses-summary`), interface genérica para seções, cache e mudança de contrato.

## Capabilities

### New Capabilities

### Modified Capabilities

Nenhuma. É uma reorganização interna, e os requisitos de `financial-summary` continuam valendo sem alteração.

## Impact

- Código: `SummaryService` (reduzido a fachada), `IncomesSummaryService` (novo), o ponto comum de paginação (novo, ou método em `TransactionService`) e `SummaryServiceTest`, que se divide entre fachada e serviço de `incomes`.
- API: sem mudança nas rotas nem no JSON.
- Pluggy: o número de chamadas não aumenta. A fachada busca as contas uma vez e as repassa.
- Ordem: aplicar antes da `expenses-summary`.
