# Design

## Context

Estado atual, observado no repositório:
- Existe um único workflow, `.github/workflows/copilot-setup-steps.yml`, gerado pelo OpenSpec. O job dele precisa se chamar `copilot-setup-steps`, roda só quando o arquivo muda e já instala a CLI com `npm install -g @fission-ai/openspec`.
- O remoto é `marcelomaidden/financial_api` no GitHub, a branch padrão é `master`, e o histórico tem pull requests (`archive-loan-endpoint-change`).
- Gradle 9.5.0 pelo wrapper, `gradlew` no índice como executável (`100755`) e com `LF`. O toolchain pede Java 21 e usa o resolvedor da Foojay, que baixaria um JDK se faltar.
- Os 105 testes são unitários e nenhum usa `@SpringBootTest`: não precisam de MySQL, de credenciais da Pluggy nem de rede. `./gradlew build` leva cerca de 9 segundos com cache quente.
- A CLI do OpenSpec em uso é a 1.16.0, e `openspec validate --all` passa hoje, com 2 avisos de requisito longo em `financial-summary`.

Veja proposal.md para a motivação e o escopo.

## Goals / Non-Goals

**Goals:**
- Barrar no pull request o que quebra a compilação, os testes ou os specs.
- Rodar sem nenhum segredo, de modo que valha para qualquer pull request.

**Non-Goals:**
- Sem teste de contexto do Spring: o CI verde não garante que a aplicação sobe. Isso fica para outra change.
- Sem Pluggy real, sem MySQL e sem publicação de artefatos de release.
- Sem matriz de versões de Java: o projeto declara o Java 21.

## Decisions

**1. Um arquivo novo, `ci.yml`.**
O `copilot-setup-steps.yml` não muda. Misturar os dois exigiria manter o nome do job e o gatilho restrito dele.

**2. Gatilhos.**
`pull_request` e `push` na `master`. O `push` cobre o merge e o commit direto, e o `pull_request` cobre o resto. Branches de trabalho não disparam duas vezes. A branch está fixada em `master` porque é a `origin/HEAD`.

**3. Job `build`.**
`actions/checkout`, `actions/setup-java` (Temurin, versão 21), `gradle/actions/setup-gradle` e `./gradlew build`. Com o Java 21 já instalado, o resolvedor da Foojay não baixa JDK. O `setup-gradle` valida o wrapper e cuida do cache, com `cache-provider: basic`: o provedor padrão da v6 (`enhanced`) usa uma biblioteca proprietária, gratuita em repositório público e em "preview" em privado, e o `basic` é de código aberto (MIT) e gratuito em qualquer repositório. `build` roda `check` e gera o `bootJar`, o que cobre a compilação do pacote final. Em falha, `actions/upload-artifact` guarda `build/reports/tests` e `build/test-results`.

**4. Job `openspec`.**
`npm install -g @fission-ai/openspec@1.16.0` e `openspec validate --all`, sem `--strict`. O modo estrito falharia por avisos que já existem. A versão é fixada porque a CLI evolve rápido, e uma atualização do formato quebraria o CI sem mudança no repositório. Subir a versão passa a ser uma decisão explícita.

**5. Permissões e execuções repetidas.**
`permissions: contents: read` no workflow. `concurrency` por workflow e referência, com `cancel-in-progress` só em pull request, para um push seguido de outro não desperdiçar execução e a `master` nunca ser cancelada. `timeout-minutes` em cada job.

**6. Versões das actions.**
Fixadas pela tag maior (por exemplo `actions/checkout@v4`, como no workflow existente). Conferidas na implementação com `git ls-remote` e pelos `action.yml` oficiais: `actions/checkout@v7`, `actions/setup-java@v6`, `gradle/actions/setup-gradle@v6` e `actions/upload-artifact@v7`, todas com as entradas que o workflow usa.

**7. Como verificar sem rodar no GitHub.**
Não há como executar o Actions localmente. A verificação é por duas vias: o YAML é lido por um parser, e os dois comandos que os jobs executam (`./gradlew build` e `openspec validate --all`) são rodados à mão. A execução real é a última tarefa e exige um push.

## Risks / Trade-offs

- [CI verde não prova que a aplicação sobe, porque nenhum teste inicia o contexto do Spring] → Registrado como fora de escopo. Um erro de montagem de beans passa em branco e só aparece ao subir a aplicação.
- [A versão fixada da CLI do OpenSpec envelhece] → Aceito. Subir a versão é um ajuste de uma linha, feito quando o repositório for atualizado.
- [O primeiro run no GitHub é mais lento, por baixar o Gradle e o JDK] → Aceito. O cache acelera os seguintes.
- [Sem regra de proteção, o CI não bloqueia o merge] → Fora de escopo: é uma configuração da `master` no GitHub, feita à mão depois que as verificações existirem.
- [Mudança de nome da branch padrão deixa o gatilho de `push` sem efeito] → Aceito. É improvável, e o `pull_request` continua funcionando.
