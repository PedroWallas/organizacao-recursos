# Plano de testes

## Objetivo

Mostrar com testes automatizados que as regras críticas (sem reserva dupla, autorização por papel, máquina de estados, auditoria) funcionam no mesmo banco usado em produção, e que o sistema aguenta a carga pedida.

## Escopo

Dentro: API REST inteira, regras de negócio, autorização, integração com PostgreSQL, integração com webhook, concorrência, desempenho de consulta e reserva.

Fora: teste automatizado da tela web (verificada manualmente), testes de navegador, segurança além de autenticação/autorização (sem pentest), alta disponibilidade.

## Níveis e técnicas

| Nível | Ferramenta | O que cobre | Exemplos |
|---|---|---|---|
| Unidade | JUnit 5, AssertJ, Mockito | Lógica sem banco | `StatusReservaTest` (tabela de decisão com as 49 transições), `AdministradorInicialTest` |
| Integração / API | Spring Boot Test, MockMvc, Testcontainers (PostgreSQL 16) | Requisição HTTP até o banco real | `RegrasReservaTest`, `RecursoTest`, `AutorizacaoTest`, `CicloDeVidaReservaTest` |
| Concorrência | `ExecutorService` + `CountDownLatch` | 10 pedidos liberados ao mesmo tempo | `ConcorrenciaTest` |
| Integração externa | WireMock | Webhook recebendo aviso, webhook fora do ar | `NotificacaoTest` |
| Falha injetada | `@MockitoBean` | Falha no histórico desfaz a reserva | `AuditoriaAtomicaTest` |
| Aceitação (BDD) | Cucumber, Gherkin em português | Fluxo de aprovação descrito pelo negócio | `features/aprovacao.feature` |
| Carga | k6 no GitHub Actions | RNF-01 e RNF-02 | `perf/carga.js`, `docs/performance` |
| Estática | SonarCloud | Bugs, vulnerabilidades, code smells, duplicação | Quality Gate no CI |

## Técnicas de projeto de casos

- **Partição de equivalência e valor limite** no período da reserva: término antes, igual e depois do início; horários encostados (fim de uma = início da outra) que não devem conflitar; sobreposição parcial e envolvente que devem conflitar.
- **Tabela de decisão** para a máquina de estados (`StatusReservaTest` testa todas as combinações origem × destino).
- **Matriz de autorização** parametrizada por papel × rota (`AutorizacaoTest.papelAlcancaSoOQueLheCabe`).
- **Casos de erro** para cada código HTTP usado: 400, 401, 403, 404, 409, 422.

## Ambiente

- Local: Java 21 + Docker; `./mvnw -B verify`.
- CI: GitHub Actions (`ubuntu-latest`), mesmo comando, a cada push na `main`, em todo pull request e em toda tag `v*`.
- Banco de teste: container `postgres:16-alpine`, limpo (`TRUNCATE`) antes de cada teste.

## Critérios de entrada e saída

Entrada: código compila; banco de teste sobe.

Saída (para liberar uma versão):
- 100% dos testes passando no CI do commit avaliado.
- Cobertura ≥ 80% de linhas e ≥ 70% de branches (o build falha abaixo disso).
- Quality Gate do SonarCloud aprovado.
- Metas de carga RNF-01 e RNF-02 atendidas.
- Nenhum defeito de severidade alta aberto.

## Riscos de teste

| Risco | Mitigação |
|---|---|
| Teste de concorrência passar por sorte (threads não colidirem) | Largada única com `CountDownLatch`; 10 pedidos; a carga repete com 50 usuários × 5 horários |
| Diferença entre banco de teste e de produção | Testcontainers com a mesma versão do PostgreSQL |
| Teste depender de data fixa | Datas sempre relativas a "amanhã" |
| Tela sem teste automatizado | Verificação manual descrita no relatório; risco aceito na decisão de qualidade |

## Rastreabilidade

Cada requisito aponta para seus testes em [docs/rtm.md](../rtm.md).
