# Pacote de evidências — v1.0.0-rc1

Todas as evidências se referem ao commit da tag `v1.0.0-rc1`.

| Evidência | Onde |
|---|---|
| Código na versão avaliada | https://github.com/PedroWallas/organizacao-recursos/tree/v1.0.0-rc1 |
| Execução do CI no commit da tag | https://github.com/PedroWallas/organizacao-recursos/actions/workflows/ci.yml?query=branch%3Av1.0.0-rc1 |
| Relatório do JaCoCo (cobertura) e relatórios dos testes | Artifact `relatorios-<commit>` da execução acima (seção *Artifacts*) |
| Quality Gate do SonarCloud | https://sonarcloud.io/summary/overall?id=PedroWallas_organizacao-recursos |
| Resultado do teste de carga | [docs/performance/resultado-04.md](../performance/resultado-04.md) (rodadas anteriores: [01](../performance/resultado-01.md), [02](../performance/resultado-02.md), [03](../performance/resultado-03.md)) |
| Plano do teste de carga | [docs/performance/plano.md](../performance/plano.md) |
| Matriz de rastreabilidade | [docs/rtm.md](../rtm.md) |
| Registro de defeitos | [docs/defeitos/](../defeitos/README.md) |
| Plano de testes | [docs/testes/plano.md](../testes/plano.md) |
| Relatório de testes | [docs/testes/relatorio.md](../testes/relatorio.md) |
| Relatório de qualidade | [docs/relatorio-qualidade.md](../relatorio-qualidade.md) |
| Arquitetura e diagramas | [docs/arquitetura.md](../arquitetura.md) |
| Checklist da entrega (clone limpo) | [docs/entrega/checklist.md](../entrega/checklist.md) |
| Decisão de qualidade | [docs/decisao-qualidade.md](../decisao-qualidade.md) |
| Mudanças da versão | [CHANGELOG.md](../../CHANGELOG.md) |
| Histórico de revisão (pull requests) | https://github.com/PedroWallas/organizacao-recursos/pulls?q=is%3Apr |

## Observações

- O CI roda em todo push na `main`, em todo pull request e em toda tag `v*`. A execução da tag é a evidência oficial desta versão.
- O teste de carga é um workflow manual. As rodadas estão documentadas com o link de cada execução. A rodada 04 foi feita no código desta versão (mesmas mudanças de aplicação, antes do merge na `main`).
- Se for preciso corrigir algo depois, a próxima versão será `v1.0.0-rc2`. Esta tag não será movida.
