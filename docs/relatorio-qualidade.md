# Relatório de qualidade

Sistema de Organização de Recursos, versão avaliada `v1.0.0-rc1`.
Autor: Pedro Wallas Santana da Silva. Data: 2026-11-14.

## 1. Resumo

O sistema atende os 14 requisitos funcionais (RF-13 parcial: tela sem teste automatizado) e todas as regras críticas, com testes automatizados no mesmo banco de produção. Os quatro limitadores de nota estão cobertos com evidência: a aplicação executa, o pipeline existe e está verde, a autorização é testada por papel e não há dupla reserva, nem em 40 pedidos simultâneos nem em 250 sob carga.

Durante o trabalho foram encontrados 4 defeitos reais: 3 corrigidos e 1 parcialmente corrigido, com o restante aceito como risco. O RNF-02 de desempenho foi atendido. O RNF-01 ficou 25% acima da meta no p95, no ambiente do CI.

## 2. Indicadores

| Indicador | Meta | Resultado | Fonte |
|---|---|---|---|
| Testes automatizados | 100% passando | ver [relatório de testes](testes/relatorio.md) | CI |
| Cobertura de linhas | ≥ 80% | ver [relatório de testes](testes/relatorio.md) | JaCoCo |
| Cobertura de branches | ≥ 70% | ver [relatório de testes](testes/relatorio.md) | JaCoCo |
| Quality Gate SonarCloud | Aprovado | Aprovado | [painel](https://sonarcloud.io/summary/overall?id=PedroWallas_organizacao-recursos) |
| Bugs / vulnerabilidades no Sonar | 0 | 0 depois do PR #5 (eram 9 bugs e 1 vulnerabilidade na 1ª análise) | SonarCloud |
| Dupla reserva sob concorrência | 0 | 0 (40 threads no teste; 5 horários × 50 usuários na carga) | `ConcorrenciaTest`, carga |
| p95 consulta (RNF-01) | ≤ 300 ms | 375 ms | [resultado-04](performance/resultado-04.md) |
| p95 reserva (RNF-02) | ≤ 500 ms | 449 ms | [resultado-04](performance/resultado-04.md) |
| Erros sob carga | ≤ 1% | 0% | [resultado-04](performance/resultado-04.md) |
| Defeitos de severidade alta abertos | 0 | 0 | [defeitos](defeitos/README.md) |

## 3. Requisitos

Matriz completa em [rtm.md](rtm.md). Todos os RF têm implementação e teste ligados. RF-13 (interface responsiva) tem tela e mensagens de erro claras, mas sem teste automatizado de interface. A tela ainda não foi aberta no navegador nesta versão (sem Docker na máquina de preparação). As mensagens são verificadas pelos testes de API (`$.mensagem`).

## 4. Estratégia de testes aplicada

Plano em [testes/plano.md](testes/plano.md). Resumo do que foi feito:

- Testes de integração da API inteira com **PostgreSQL 16 real** (Testcontainers), banco limpo a cada teste.
- **Tabela de decisão** da máquina de estados: as 49 combinações origem × destino.
- **Matriz de autorização** parametrizada por papel × rota.
- **Valor limite** no período: horários encostados não conflitam; sobreposição parcial e envolvente conflitam; início no passado é recusado.
- **Concorrência** com 40 pedidos liberados juntos (4× o pool de conexões).
- **Falha injetada** no histórico para provar que auditoria e reserva são atômicas.
- **WireMock** para o webhook, incluindo webhook fora do ar.
- **BDD** em português para o fluxo de aprovação.
- **Carga** com k6 no GitHub Actions, com thresholds que reprovam o job.

## 5. Defeitos

| Id | Severidade | Como foi achado | Situação |
|---|---|---|---|
| [DEF-01](defeitos/DEF-01.md) | Alta | Revisão de regras + teste exploratório | Corrigido, com 2 testes de regressão |
| [DEF-02](defeitos/DEF-02.md) | Média | Valor limite (início no passado) | Corrigido, com teste de regressão |
| [DEF-03](defeitos/DEF-03.md) | Alta | Teste de carga (pool travado, 59% de timeouts) | Corrigido. Teste reforçado provou a falha no código antigo antes da correção |
| [DEF-04](defeitos/DEF-04.md) | Média | Teste de carga (lento sem erros) | Parcial: RNF-02 atendido; RNF-01 aceito como risco |

Lição principal: o DEF-03 passou pelos testes de integração porque o teste de concorrência usava o mesmo número de threads que o pool. Só a carga mostrou o problema. O teste foi reforçado para 4× o pool e agora pegaria esse tipo de falha.

## 6. Análise estática

A primeira análise do SonarCloud apontou 9 bugs (promessas sem tratamento no JS, tabelas sem cabeçalho, método `final` em entidade JPA, duração sem fuso) e 1 vulnerabilidade (CSRF desligado). Todos foram tratados no PR #5. O CSRF foi revisado e mantido desligado, com justificativa no código: a API é stateless, o token vai no cabeçalho `Authorization` e não há cookie de sessão. Quality Gate aprovado.

A análise do SonarCloud está no modo automático, que não importa a cobertura. A cobertura oficial é a do JaCoCo, publicada como artifact em cada execução do CI.

## 7. Segurança e robustez

- Senhas com BCrypt; token JWT assinado (HS256) com segredo de no mínimo 32 caracteres, configurável por variável de ambiente.
- Cadastro público nunca cria administrador ou responsável (teste dedicado).
- Solicitante só vê e altera as próprias reservas (teste dedicado).
- Validação de entrada com mensagens por campo (400) e códigos HTTP coerentes (401, 403, 404, 409, 422).
- Tela usa `textContent` para todo dado vindo da API (sem injeção de HTML).
- Webhook com timeout de 2 s e falha isolada: a reserva não depende dele.

## 8. Riscos conhecidos

| Risco | Impacto | Situação |
|---|---|---|
| RNF-01 25% acima da meta no p95 (CI com 2 vCPUs compartilhadas) | Baixo | Aceito ([DEF-04](defeitos/DEF-04.md)) |
| Tela sem teste automatizado e ainda não aberta no navegador nesta versão | Médio | Aceito; verificação manual pendente |
| Sem revisão por pares (trabalho individual) | Médio | Aceito; todo código entrou por PR com CI obrigatório |
| Tabelas criadas pelo Hibernate (`ddl-auto=update`) | Médio em produção | Aceito para a avaliação; migrações antes de produção |
| Credenciais padrão de desenvolvimento no `application.properties` | Alto se usado fora da máquina local | Mitigado: todas sobrescritas por variável de ambiente, documentado no README |

## 9. Conclusão

A evidência sustenta liberar a versão para avaliação **com ressalvas**: funcionalidade e regras críticas comprovadas, CI e análise estática aprovados, desempenho de reserva dentro da meta e o de consulta um pouco acima no ambiente medido. Não sustenta afirmar "pronto para produção". Detalhes em [decisao-qualidade.md](decisao-qualidade.md).
