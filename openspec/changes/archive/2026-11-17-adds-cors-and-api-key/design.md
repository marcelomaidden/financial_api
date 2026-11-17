# Design

## Context

Estado atual, observado no código:
- Spring MVC (`spring-boot-starter-webmvc`), sem Spring Security, sem filtro nem interceptor. Nenhuma configuração de CORS. As 7 rotas são `GET`.
- `PluggyProperties` é um `record` com `@ConfigurationProperties` e `@Validated`: a aplicação não inicia se faltar credencial, e o `toString` do `Owner` não imprime segredos. As variáveis usam `${VAR:}` e o `@NotBlank` dá a mensagem. O perfil `prod` não tem valores padrão, e o `dev` tem alguns.
- `JsonMapper` (Jackson 3) é um bean, usado pelo `PluggyClient`. O erro padrão é `ApiError(status, error, message)`, criado por `ApiErrors.of`.
- O front é um SPA em `https://financial-api-frontend-dc3ceb1ba406.herokuapp.com`. O navegador bloqueia as respostas ("blocked by CORS policy"), e o Postman, que não aplica CORS, funciona sem chave.

Veja proposal.md para a motivação e o escopo.

## Goals / Non-Goals

**Goals:**
- O front autorizado consegue ler a API, e quem chama sem a chave recebe 401.
- A chave e as origens vêm do ambiente, e a aplicação em produção não sobe sem chave.

**Non-Goals:**
- Sem login de usuário, sem Spring Security e sem limite de requisições.
- Sem proteger o front: com a chave no JavaScript do SPA público, quem abrir o front vê os dados.
- Sem rotação de chave: trocar a chave é mudar a variável na API e no build do front.

## Decisions

**1. Dois filtros de servlet, na ordem CORS e depois chave.**
`CorsFilter` (do `spring-web`) e `ApiKeyFilter` (`OncePerRequestFilter`), cada um registrado por `FilterRegistrationBean`, com `Ordered.HIGHEST_PRECEDENCE + 10` e `+ 20`. Os filtros não são `@Component`, para o Spring Boot não registrá-los duas vezes. O CORS vem primeiro porque, se a chave rejeitasse antes, o 401 sairia sem `Access-Control-Allow-Origin` e o navegador mostraria "CORS error" no lugar do 401. Alternativa descartada: `WebMvcConfigurer.addCorsMappings`, que só roda depois dos filtros de servlet, e portanto depois do filtro da chave.

**2. Configuração de CORS.**
`CorsConfiguration` única para `/**`: origens da lista, métodos `GET` e `OPTIONS`, cabeçalhos permitidos `X-API-Key`, `Content-Type` e `Accept`, `allowCredentials` falso e `maxAge` de 1 hora. `Content-Type` e `Accept` entram porque bibliotecas de requisição costumam mandá-los, e um cabeçalho não permitido derrubaria o preflight de um front que funcionaria de outro modo. Origens vazias ou em branco na lista são ignoradas. `*` não é usado. Uma origem não listada que mande o cabeçalho `Origin` é rejeitada pelo `CorsFilter` com 403 "Invalid CORS request" e sem cabeçalhos de CORS, e isso vale também para a lista vazia. Requisições sem `Origin`, como as do Postman, não são afetadas.

**3. `ApiKeyFilter`.**
- `OPTIONS` passa direto. O preflight válido nem chega ao filtro, porque o `CorsFilter` o responde antes.
- Sem chave configurada (em branco), o filtro deixa tudo passar. Isso só acontece em desenvolvimento, porque produção não inicia sem chave.
- Com chave configurada, compara o cabeçalho `X-API-Key` pelo hash SHA-256 dos dois valores com `MessageDigest.isEqual`, o que dá tempo constante e esconde o tamanho da chave. Ausente ou diferente, responde 401 e interrompe a cadeia.
- O 401 usa `ApiErrors.of(HttpStatus.UNAUTHORIZED, "Chave de API ausente ou inválida")` escrito com o `JsonMapper`, com `Content-Type: application/json`. A mensagem é a mesma nos dois casos e não contém valores.
- O filtro roda antes da resolução da rota, então uma rota inexistente sem chave dá 401, e não 404.
- Não registra a chave. O log do 401 fica em nível de depuração e traz só o método e o caminho.

**4. Propriedades.**
`ApiAccessProperties` com o prefixo `app.access`: `apiKey` (texto) e `cors.allowedOrigins` (lista). O `toString` não imprime a chave, como o `PluggyProperties.Owner`. Em `application.yml`, os valores vêm de `${APP_API_KEY:}` e `${APP_CORS_ALLOWED_ORIGINS:}`. A conversão de texto separado por vírgula para lista é a do Spring.

**5. Chave obrigatória em produção.**
Um bean `@Profile("prod")` verifica a chave na inicialização e lança `IllegalStateException` citando `APP_API_KEY`, sem valores, se estiver em branco. Isso cobre o caso de a variável existir e estar vazia, que um placeholder sem valor padrão não pegaria. Em desenvolvimento a chave é opcional, e um aviso no log diz que a verificação está desligada.

**6. Perfis.**
`application-dev.yml` permite as origens `http://localhost:3000` e `http://localhost:5173` por padrão, ainda substituíveis pela variável. `application-prod.yml` não tem origem padrão: a lista vem de `APP_CORS_ALLOWED_ORIGINS`, e sem ela ninguém tem acesso por navegador.

**7. Como testar sem subir a aplicação.**
O `ApiKeyFilter` é testado com `MockHttpServletRequest` e `MockFilterChain`. A ordem e a interação com o CORS são testadas com `MockMvc` em modo autônomo (`standaloneSetup`) com os dois filtros registrados na ordem do projeto, e um controller de teste. Nenhum teste inicia o contexto do Spring, como no resto do projeto.

## Risks / Trade-offs

- [Com a chave no JavaScript do SPA público, quem abrir o front vê os dados] → Registrado como fora de escopo. A chave barra quem chama a API direto, e fechar o front pede senha nele ou login, num passo à parte.
- [A configuração real do filtro (ordem, registro) não é exercitada sem subir a aplicação] → O teste autônomo prova a ordem e os cabeçalhos. A tarefa 4.1 verifica na aplicação real, depois do deploy.
- [Quebra de compatibilidade: o Postman e qualquer cliente existente passam a precisar do cabeçalho] → Registrado na spec e no README.
- [Chave comprometida] → Trocar a variável na API e no build do front, que é um deploy dos dois.
- [Duas origens do front (por exemplo, homologação) exigem acrescentar à variável] → Aceito. A lista aceita várias origens.
- [Origem escrita errada em `APP_CORS_ALLOWED_ORIGINS` (esquema ou porta diferentes, ou sem esquema) não casa com a do navegador e é rejeitada com 403] → O README mostra o formato exato. O Spring tolera a barra no final da origem configurada.
