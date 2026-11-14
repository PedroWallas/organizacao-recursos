# Checklist da entrega

Verificação feita como avaliador externo, em **clone limpo**: `git clone` num diretório temporário, sem nada reaproveitado da pasta de desenvolvimento. Seguindo apenas o README.

| Campo | Valor |
|---|---|
| Data | 2026-11-14 |
| Commit avaliado | `6d08bb5` (main após PR #6) |
| Máquina | Windows 11, JDK 21 (Temurin 21.0.13), **sem Docker** |

## Passos do README no clone limpo

| Passo | Resultado |
|---|---|
| `git clone` | OK |
| Java 21 disponível | OK |
| `docker compose up -d db` | **Não executado**: máquina sem Docker (o README lista Docker como pré-requisito, então a documentação está correta) |
| `./mvnw -B -DskipTests package` | OK, gerou `target/organizacao-recursos-1.0.0-SNAPSHOT.jar` |
| `./mvnw -B verify` | Falhou localmente com "Could not find a valid Docker environment", como o README avisa (Testcontainers precisa de Docker) |
| Suíte completa em máquina limpa | OK no CI: cada execução clona do zero num runner novo e roda `./mvnw -B verify` (90 testes, 0 falhas) |
| Subir a aplicação e checar `/actuator/health` | OK no workflow de carga: clone do zero + PostgreSQL + `java -jar` + health respondeu antes dos testes |

## Entregáveis da especificação

Formato: item → existe → atualizado → caminho.

| Item | Existe | Atualizado | Caminho |
|---|---|---|---|
| Repositório com revisão por pares | Parcial | Sim | Todo código entrou por PR com CI obrigatório: https://github.com/PedroWallas/organizacao-recursos/pulls?q=is%3Apr (trabalho individual, sem revisor humano — risco aceito) |
| Aplicação executável | Sim | Sim | `src/`, `pom.xml`, `docker-compose.yml`; sobe no workflow de carga |
| README | Sim | Sim | `README.md` |
| Documentação da API | Sim | Sim | Tabela de endpoints no `README.md` e Swagger em `/swagger-ui.html` |
| RTM | Sim | Sim | `docs/rtm.md` |
| Diagrama de contexto | Sim | Sim | `docs/arquitetura.md` |
| Diagrama de componentes | Sim | Sim | `docs/arquitetura.md` |
| Diagramas de sequências críticas | Sim | Sim | `docs/arquitetura.md` (reserva simultânea; ciclo de aprovação) |
| Plano de testes | Sim | Sim | `docs/testes/plano.md` |
| Relatório de testes | Sim | Sim | `docs/testes/relatorio.md` |
| Pipeline de CI | Sim | Sim | `.github/workflows/ci.yml` |
| Relatório do SonarCloud | Sim | Sim | https://sonarcloud.io/summary/overall?id=PedroWallas_organizacao-recursos |
| Relatório do JaCoCo | Sim | Sim | Artifact `relatorios-<commit>` de cada execução do CI |
| Plano do teste de carga | Sim | Sim | `docs/performance/plano.md`, `perf/carga.js`, `.github/workflows/carga.yml` |
| Resultado do teste de carga | Sim | Sim | `docs/performance/resultado-01.md` a `resultado-04.md` |
| Registro de defeitos | Sim | Sim | `docs/defeitos/` |
| Relatório final de qualidade | Sim | Sim | `docs/relatorio-qualidade.md` |

## Bloqueadores (em ordem)

| # | Bloqueador | Situação |
|---|---|---|
| 1 | `docs/testes/relatorio.md` não existia no commit avaliado | Corrigido no PR da entrega |
| 2 | `docs/relatorio-qualidade.md` não existia no commit avaliado | Corrigido no PR da entrega |
| 3 | Faltavam `CHANGELOG.md`, índice de evidências e decisão de qualidade | Corrigido no PR da entrega |

## Pontos de atenção (não bloqueiam)

- Rodar os testes exige Docker. Está no README, mas quem não tiver Docker só consegue compilar.
- Revisão por pares não existe (trabalho individual).
- A tela web foi conferida manualmente pelo autor depois deste checklist; não tem teste automatizado.
