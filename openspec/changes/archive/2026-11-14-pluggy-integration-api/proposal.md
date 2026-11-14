# Proposal

## Why

Não existe hoje uma forma própria de consultar as contas, os cartões de crédito e as transações que o autor mantém conectados à Pluggy (Open Finance Brasil). Uma API REST pessoal em Spring Boot que autentica na Pluggy e expõe esses dados é a base para qualquer uso posterior (consolidação, relatórios, automações). Esta é a primeira etapa: só leitura, uso pessoal por duas pessoas (o autor e a esposa), cada uma com sua própria conta de desenvolvedor na Pluggy e, portanto, suas próprias credenciais.

## What Changes

- Criar o projeto `financial_api` (Java 21, Spring Boot 3.2 ou superior), em repositório git próprio, com `.gitignore` cobrindo o padrão Spring Boot, arquivos do IntelliJ e o arquivo de credenciais.
- Configurar uma lista de donos (hoje "marcelo" e "luana"), cada um com `clientId`, `clientSecret` e `itemIds` próprios, e autenticar na Pluggy (`POST /auth`) separadamente por dono, com a `apiKey` em cache por dono, renovada quando expirar ou quando a Pluggy responder 401.
- Expor endpoints REST de leitura para contas e cartões de crédito (agrupados por dono, com falha parcial: o sucesso de um dono não é descartado pelo erro do outro), e para transações e faturas de uma conta, que levam o dono na rota porque o `accountId` só vale para a credencial que o devolveu.
- Ler os `itemId` de cada dono de uma lista fixa no yml. Nenhum endpoint cria itens ou connect tokens.
- Traduzir erros da Pluggy em uma exceção própria e em respostas HTTP consistentes.
- Preparar a infraestrutura de banco para uso futuro: Spring Data JPA, MySQL, Flyway (migrações automáticas na inicialização) e `compose.yaml` com o MySQL local. Sem entidades nem repositórios nesta etapa.
- Receber os `clientId` e `clientSecret` de cada dono por variáveis de ambiente referenciadas no yml do ambiente.
- Fora de escopo: criação de itens e connect token, escrita ou pagamentos, donos além dos configurados no yml, autenticação da própria API, testes automatizados, definição final de período e paginação de transações (ficam como parâmetros a decidir na implementação).

## Capabilities

### New Capabilities
- `pluggy-financial-data`: leitura de contas, cartões de crédito, transações e faturas na Pluggy para mais de um dono, incluindo a autenticação por dono, a gestão da `apiKey` e a falha parcial entre donos.
- `pluggy-error-handling`: tradução dos erros da Pluggy e de validação em respostas HTTP padronizadas pela API.

### Modified Capabilities

Nenhuma. O projeto ainda não tem specs.

## Impact

- Código novo: projeto `financial_api` com os packages `client`, `config`, `service`, `resource` e `exception`.
- Dependências: Spring Web (RestClient e HTTP Interface), Spring Validation, Spring Boot Configuration Processor, Spring Data JPA, MySQL Connector/J, Flyway (`flyway-core` e `flyway-mysql`) e suporte a Docker Compose do Spring Boot (somente desenvolvimento). Não usa Spring Cloud, Feign nem o SDK `pluggy-java`.
- Sistemas externos: API da Pluggy (`https://api.pluggy.ai`) e um banco MySQL, que a aplicação passa a exigir para iniciar.
- Segredos: o arquivo `financial_api_credentials.txt` fica fora do repositório novo e a aplicação lê apenas variáveis de ambiente.
