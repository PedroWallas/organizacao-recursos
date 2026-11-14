# Resultado 03 do teste de carga — melhorou, ainda acima da meta

| Campo | Valor |
|---|---|
| Data | 2026-11-13 (execução às 00:52 UTC de 2026-11-14) |
| Commit | `0fc7b3f` (branch `perf/consulta-e-reserva`, com as duas otimizações do DEF-04) |
| Execução | https://github.com/PedroWallas/organizacao-recursos/actions/runs/43536461574 |
| Ambiente | O mesmo das rodadas anteriores, sem aquecimento |

## Números

| Métrica | Meta | Rodada 02 | Rodada 03 | Resultado |
|---|---|---|---|---|
| p95 consulta | ≤ 300 ms | 1,05 s | 703 ms | ✗ (-33%) |
| Mediana consulta | — | 359 ms | 214 ms | — |
| p95 reserva | ≤ 500 ms | 1,20 s | 817 ms | ✗ (-32%) |
| Mediana reserva | — | 426 ms | 199 ms | — |
| Erros | ≤ 1% | 0% | 0% | ✓ |
| Reservas criadas / conflitos | 5 / 245 | 5 / 245 | 5 / 245 | ✓ |

## Análise

As duas mudanças reduziram o tempo em cerca de um terço, e a mediana caiu para cerca de 200 ms. O p95 continua puxado pelas primeiras requisições: a aplicação sobe do zero a cada execução e recebe 100 usuários de uma vez, enquanto a JVM ainda está compilando o código (JIT), numa máquina de 2 vCPUs dividida com o k6 e o PostgreSQL.

## Próximo passo

Medir o regime normal com aquecimento antes dos cenários (ver plano). Ver [resultado-04.md](resultado-04.md).
