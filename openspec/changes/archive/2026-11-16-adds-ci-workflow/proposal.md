# Proposal

## Why

Nada valida um pull request antes do merge: o repositório só tem o workflow `copilot-setup-steps.yml`, que instala a CLI do OpenSpec e roda apenas quando ele mesmo muda. Os 105 testes unitários e a validação dos specs dependem de alguém rodá-los à mão, e o fluxo do projeto já passa por pull requests no GitHub.

## What Changes

- Novo workflow `.github/workflows/ci.yml`, disparado em `pull_request` e em `push` na `master`, com dois jobs:
  - `build`: Temurin 21, cache do Gradle e `./gradlew build` (compila, roda os testes e gera o `bootJar`). Em falha, publica os relatórios de teste como artefato.
  - `openspec`: instala a CLI do OpenSpec na versão em uso (1.16.0) e roda `openspec validate --all`, em modo não estrito (os avisos de requisito longo não falham o job).
- Permissão `contents: read`. A execução anterior da mesma branch é cancelada em pull request, e não em `master`.
- O README passa a listar as verificações que o CI roda, para rodá-las localmente.
- Sem segredos: os testes não usam MySQL, credenciais da Pluggy nem o contexto do Spring.
- Fora de escopo:
  - um teste que suba o contexto do Spring (pede decisão sobre o banco e fica para uma change própria);
  - verificação contra a Pluggy real, que exigiria segredos e bateria no limite de requisições;
  - exigir o CI como condição de merge, que é uma configuração do repositório no GitHub;
  - `.gitattributes`;
  - alterar o `copilot-setup-steps.yml`, cujo job precisa manter o nome.

## Capabilities

### New Capabilities

### Modified Capabilities

Nenhuma. É infraestrutura de desenvolvimento, e o comportamento da API não muda. Por isso a change declara `skip_specs`.

## Impact

- Arquivos: `.github/workflows/ci.yml` (novo) e `README.md`.
- API e código da aplicação: sem mudança.
- GitHub: passa a haver duas verificações em cada pull request. Elas só bloqueiam o merge depois que a regra de proteção da `master` as exigir, o que é uma configuração manual.
