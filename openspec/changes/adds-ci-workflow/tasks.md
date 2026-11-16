# Tasks

## 1. Workflow

- [x] 1.1 Criar `.github/workflows/ci.yml` com os gatilhos, as permissões, o `concurrency` e o job `build` (Temurin 21, cache do Gradle, `./gradlew build` e o upload dos relatórios em falha), conferindo antes as tags mais recentes das actions usadas. Verificar que o YAML é lido por um parser sem erro, que o job tem esses passos, e que `./gradlew build` passa localmente
- [x] 1.2 Acrescentar o job `openspec` ao `ci.yml`, com a CLI na versão fixada em 1.16.0 e `openspec validate --all` sem `--strict`. Verificar que o YAML continua válido com os dois jobs, que `openspec --version` mostra a versão fixada e que `openspec validate --all` termina com código 0

## 2. Documentação

- [x] 2.1 Acrescentar ao README uma seção curta sobre as verificações do CI e como rodá-las localmente (`./gradlew build` e `openspec validate --all`). Verificar que os dois comandos da seção rodam como descritos

## 3. Verificação no GitHub

- [ ] 3.1 Enviar uma branch com o workflow e abrir um pull request. Verificar que os jobs `build` e `openspec` aparecem nas verificações do pull request e terminam verdes. Exige o push, que é feito por você
