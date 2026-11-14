# Resultado 04 do teste de carga — reserva aprovada, consulta 25% acima da meta

| Campo | Valor |
|---|---|
| Data | 2026-11-14 (execução no Actions) |
| Commit | `8fa440c` (branch `perf/consulta-e-reserva`: otimizações do DEF-04 + aquecimento) |
| Execução | https://github.com/PedroWallas/organizacao-recursos/actions/runs/43542301409 |
| Ambiente | O mesmo das rodadas anteriores, agora com aquecimento da JVM no setup (fora das métricas) |

## Números

| Métrica | Meta | Medido | Resultado |
|---|---|---|---|
| p95 consulta (RNF-01) | ≤ 300 ms | 374,65 ms | ✗ (+25%) |
| Mediana consulta | — | 123 ms | — |
| Erro consulta | ≤ 1% | 0% (0 de 250) | ✓ |
| p95 reserva (RNF-02) | ≤ 500 ms | 448,95 ms | ✓ |
| Mediana reserva | — | 138 ms | — |
| Erro reserva (fora 201/409) | ≤ 1% | 0% (0 de 250) | ✓ |
| Reservas criadas | exatamente 5 | 5 | ✓ |
| Conflitos (409) | 245 | 245 | ✓ |
| Respostas inesperadas | 0 | 0 | ✓ |

No total foram 1023 requisições. As 150 "falhas" gerais são os 409 esperados das reservas de aquecimento, que ficam fora das métricas dos cenários. O log não teve nenhum erro de pool.

## Evolução

| Rodada | Mudança | p95 consulta | p95 reserva | Erros |
|---|---|---|---|---|
| 01 | — | 30,15 s | 30,69 s | 59% |
| 02 | Correção DEF-03 (pool) | 1,05 s | 1,20 s | 0% |
| 03 | DEF-04: consulta única de ocupados + checagem prévia sem trava | 703 ms | 817 ms | 0% |
| 04 | + aquecimento da JVM | **375 ms** | **449 ms** | 0% |

## Avaliação

- **RNF-02 (reserva disputada): atendido.** p95 de 449 ms, uma reserva por horário, nenhum 5xx.
- **RNF-01 (consulta): não atendido** no ambiente do CI, por 75 ms no p95. A mediana (123 ms) fica bem abaixo da meta.

O que ainda pesa: 100 usuários virtuais, a aplicação, o PostgreSQL e o próprio k6 rodam juntos numa máquina de 2 vCPUs do GitHub Actions. Nesse ambiente as consultas disputam CPU e as 10 conexões do pool com as reservas.

Decisão: **risco aceito**, registrado no [DEF-04](../defeitos/DEF-04.md) e na [decisão de qualidade](../decisao-qualidade.md). O sistema não fica "pronto para produção" com base neste número: antes de uso real, o RNF-01 precisa ser medido de novo em ambiente separado do gerador de carga.
