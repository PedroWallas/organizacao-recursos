# Resultado 02 do teste de carga — REPROVADO no tempo de resposta

| Campo | Valor |
|---|---|
| Data | 2026-11-13 (execução às 00:42 UTC de 2026-11-14) |
| Commit | `e95e01a` (main após PR #4, com a correção do DEF-03) |
| Execução | https://github.com/PedroWallas/organizacao-recursos/actions/runs/43535945141 |
| Ambiente | O mesmo da rodada 01 |

## Números

| Métrica | Meta | Medido | Resultado |
|---|---|---|---|
| p95 consulta | ≤ 300 ms | 1,05 s | ✗ |
| Erro consulta | ≤ 1% | 0% (0 de 250) | ✓ |
| p95 reserva | ≤ 500 ms | 1,20 s | ✗ |
| Erro reserva (fora 201/409) | ≤ 1% | 0% (0 de 250) | ✓ |
| Reservas criadas | exatamente 5 | 5 | ✓ |
| Conflitos (409) | 245 | 245 | ✓ |
| Respostas inesperadas | 0 | 0 | ✓ |

Mediana: consulta 359 ms, reserva 426 ms. Teste inteiro em 10 s. Nenhum `Connection is not available` no log.

## Análise

A correção do DEF-03 resolveu o travamento: todas as 622 requisições responderam, sem erro, e a regra de uma reserva por horário continuou certa.

O tempo, porém, ficou acima da meta. O job do Actions agora fica vermelho, como deveria. Causas encontradas no código:

1. **Consulta com N+1**: a pesquisa por disponibilidade fazia uma consulta ao banco **por recurso** (21 salas = 21 consultas por requisição).
2. **Fila na trava**: todo pedido de reserva entrava na fila da trava do recurso, mesmo quando o horário já estava ocupado. Com 50 pedidos para o mesmo horário, 49 esperavam a vez só para ouvir "409". Enquanto esperavam, seguravam conexões do pool, e as consultas também esperavam.

Registrado como [DEF-04](../defeitos/DEF-04.md).

## Próximo passo

Corrigir as duas causas e repetir. Ver [resultado-03.md](resultado-03.md).
