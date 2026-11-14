# Relatório de testes

| Campo | Valor |
|---|---|
| Versão | `v1.0.0-rc1` (código igual ao commit `6d08bb5`, merge do PR #6) |
| Execução de referência | https://github.com/PedroWallas/organizacao-recursos/actions/runs/43542653066 (CI do PR #6, mesmo código da tag) |
| Ambiente | GitHub Actions `ubuntu-latest`, Java 21 (Temurin), PostgreSQL 16 via Testcontainers |
| Comando | `./mvnw -B verify` |
| Plano | [plano.md](plano.md) |

A execução oficial da tag fica no [índice de evidências](../evidencias/README.md).

## Resultado geral

| Indicador | Meta | Resultado |
|---|---|---|
| Testes executados | — | **90** |
| Passaram | 100% | **90 (100%)** |
| Falhas / erros / pulados | 0 | 0 / 0 / 0 |
| Cobertura de linhas | ≥ 80% | **98,6%** (495 de 502) |
| Cobertura de branches | ≥ 70% | **98,1%** (102 de 104) |
| Tempo da suíte | — | ~46 s |

## Por classe de teste

| Classe | Nível | Testes | O que prova |
|---|---|---|---|
| `StatusReservaTest` | Unidade | 9 | As 49 transições da máquina de estados (tabela de decisão) |
| `AdministradorInicialTest` | Unidade | 3 | Criação do primeiro administrador sem duplicar |
| `AutenticacaoTest` | API | 10 | Cadastro sempre solicitante, login, 401 sem token e com token adulterado, validação |
| `AutorizacaoTest` | API | 16 | Matriz papel × rota; solicitante não vê nem mexe em reserva alheia |
| `RecursoTest` | API | 7 | CRUD, validação, pesquisa por tipo/capacidade/local/competência/disponibilidade |
| `RegrasReservaTest` | API | 18 | Período válido, sobreposição (parcial, envolvente, encostada), professor, manutenção, alteração, passado |
| `CicloDeVidaReservaTest` | API | 7 | Fluxo completo com histórico, rejeição, ausência, cancelamento de reserva em uso |
| `RetiradaMaterialTest` | API | 10 | Retirada e devolução, material pendente bloqueia encerramento e alteração |
| `RelatorioUtilizacaoTest` | API | 1 | Horas, reservas e conflitos evitados por recurso |
| `ConcorrenciaTest` | Concorrência | 2 | 40 pedidos simultâneos geram 1 reserva (sala e agenda do professor) |
| `AuditoriaAtomicaTest` | Falha injetada | 1 | Falha no histórico desfaz a reserva |
| `NotificacaoTest` | Integração externa (WireMock) | 3 | Webhook recebe avisos; webhook fora do ar não derruba a reserva |
| `AprovacaoBddTest` | Aceitação (Cucumber) | 3 | Cenários de aprovação em Gherkin |

## O que não está coberto

| Trecho | Motivo |
|---|---|
| `OrganizacaoRecursosApplication.main` (2 linhas) | Ponto de entrada; a aplicação sobe nos testes pelo Spring Boot Test e no workflow de carga |
| `TratamentoErros` (3 linhas) | Tratadores de bloqueio otimista e de violação de integridade, difíceis de provocar de forma determinística |
| `SegurancaConfig` (segredo JWT curto) | Validação de configuração na subida; exigiria subir outro contexto só para isso |
| `ConflitoEvitado.getRecursoId`, 1 branch de `Reserva` | Acessores e ramo trivial |
| Tela web | Sem teste automatizado de interface (risco aceito na decisão de qualidade) |

## Defeitos encontrados pelos testes

| Defeito | Encontrado por |
|---|---|
| [DEF-01](../defeitos/DEF-01.md) | Teste exploratório da máquina de estados × retirada de material |
| [DEF-02](../defeitos/DEF-02.md) | Valor limite no período |
| [DEF-03](../defeitos/DEF-03.md) | Teste de carga. O teste de concorrência foi reforçado (10 → 40 pedidos) e **falhou no código antigo** ([execução](https://github.com/PedroWallas/organizacao-recursos/actions/runs/43535337874)) antes da correção |
| [DEF-04](../defeitos/DEF-04.md) | Teste de carga |

## Teste de carga

| Rodada | p95 consulta | p95 reserva | Erros | Situação |
|---|---|---|---|---|
| [01](../performance/resultado-01.md) | 30,15 s | 30,69 s | 59% | Reprovado (DEF-03) |
| [02](../performance/resultado-02.md) | 1,05 s | 1,20 s | 0% | Reprovado (DEF-04) |
| [03](../performance/resultado-03.md) | 703 ms | 817 ms | 0% | Reprovado |
| [04](../performance/resultado-04.md) | 375 ms | 449 ms | 0% | RNF-02 ✓, RNF-01 ✗ (aceito) |

Em todas as rodadas: exatamente 1 reserva por horário disputado.

## Tela web

**Não verificada nesta versão.** A máquina do autor não tinha Docker durante a preparação da rc1, então a aplicação não foi aberta no navegador. O que existe de evidência:
- a página e seus arquivos são servidos sem login (`AutenticacaoTest.paginaEDocumentacaoSaoPublicas`);
- todas as rotas que a tela chama são testadas na API, com as mensagens de erro (`$.mensagem`).

Roteiro de verificação manual pendente: login; pesquisa com filtros; reserva de recurso livre e de recurso restrito; mensagem de conflito; aprovação pelo responsável; layout em largura de celular.

## Conclusão

Os critérios de saída do plano foram atendidos, com duas exceções: RNF-01 ficou acima da meta e a tela não tem teste automatizado. Ambas estão registradas como risco aceito na [decisão de qualidade](../decisao-qualidade.md).
