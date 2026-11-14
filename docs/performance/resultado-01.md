# Resultado 01 do teste de carga — REPROVADO

| Campo | Valor |
|---|---|
| Data | 2026-11-13 (execução às 00:29–00:32 UTC de 2026-11-14) |
| Commit | `de52332` (main após PR #2) |
| Execução | https://github.com/PedroWallas/organizacao-recursos/actions/runs/43534471752 |
| Ambiente | GitHub Actions `ubuntu-latest`, PostgreSQL 16 (serviço), app via `java -jar`, k6 na mesma máquina |
| Plano | [plano.md](plano.md) |

## Números

| Métrica | Meta | Medido | Resultado |
|---|---|---|---|
| p95 consulta | ≤ 300 ms | 30,15 s | ✗ |
| Erro consulta | ≤ 1% | 71,59% (179 de 250) | ✗ |
| p95 reserva | ≤ 500 ms | 30,69 s | ✗ |
| Erro reserva (fora 201/409) | ≤ 1% | 75,59% (189 de 250) | ✗ |
| Reservas criadas | exatamente 5 (uma por horário) | 5 | ✓ |
| Respostas inesperadas | 0 | 368 | ✗ |

Outros dados: 622 requisições no total, 56 conflitos (409) respondidos a tempo, mediana geral de 30 s (o timeout do k6).

## Análise

A regra de negócio aguentou: nenhum horário teve duas reservas. O sistema, porém, parou de responder. O log da aplicação teve 771 erros `HikariPool-1 - Connection is not available, request timed out after 30000ms (total=10, active=10, idle=0)`.

Causa: cada pedido recusado por conflito precisava de uma segunda conexão do pool para gravar o "conflito evitado" enquanto ainda segurava a primeira. Com dezenas de pedidos recusados juntos, as 10 conexões ficaram presas esperando umas pelas outras. Registrado como [DEF-03](../defeitos/DEF-03.md).

## Problema no próprio teste

O job terminou verde mesmo com o k6 reprovando. O `k6 run ... | tee` sem `pipefail` devolvia o código de saída do `tee`. Corrigido junto com o DEF-03 (`shell: bash` no passo).

## Próximo passo

Corrigir o DEF-03 e repetir a carga. Ver [resultado-02.md](resultado-02.md).
