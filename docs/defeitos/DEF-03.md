# DEF-03 Pool de conexões travava sob carga quando havia conflito de reserva

| Campo | Valor |
|---|---|
| Severidade | Alta (sistema para de responder com 50 usuários; RNF-01 e RNF-02 reprovados) |
| Prioridade | Alta |
| Encontrado em | 2026-11-13, primeira rodada do teste de carga ([resultado-01](../performance/resultado-01.md)) |
| Técnica | Teste de carga (k6) + leitura do log da aplicação |
| Versão afetada | commit `de52332` (main após PR #2) |
| Situação | Corrigido |

## Como reproduzir

1. Rodar o workflow **Teste de carga** no commit afetado (50 usuários consultando + 50 disputando a mesma sala).
2. Ou: disparar mais pedidos simultâneos para o mesmo horário do que o tamanho do pool (10).

## Resultado esperado

Uma reserva por horário, as demais 409, todas em menos de 500 ms (p95).

## Resultado obtido

- 59% das requisições falharam por timeout de 30 s; p95 de 30 s nas consultas e nas reservas.
- O log teve 771 linhas `HikariPool-1 - Connection is not available, request timed out after 30000ms (total=10, active=10, idle=0, waiting=14)`, várias saindo de `TransactionSynchronization.afterCompletion`.
- A regra de negócio continuou certa: 5 reservas para 5 horários, nenhuma reserva dupla.

## Causa

O conflito evitado era gravado num listener `@TransactionalEventListener(AFTER_ROLLBACK)` com `REQUIRES_NEW`. Nessa fase o Spring ainda não devolveu a conexão da transação que sofreu rollback. Cada pedido recusado segurava uma conexão e pedia outra. Com muitos pedidos recusados ao mesmo tempo, as 10 conexões ficavam todas presas esperando uma livre, até o timeout.

O `ConcorrenciaTest` original usava 10 threads, o mesmo tamanho do pool, e por isso não pegou o problema.

### Sintoma extra confirmado no CI

Com o teste reforçado e o código antigo (execução https://github.com/PedroWallas/organizacao-recursos/actions/runs/43535337874), o listener gravou **0 de 39** conflitos, sem nenhum erro no log, e o `RelatorioUtilizacaoTest` também falhou (`conflitosEvitados` 0 em vez de 1). Nas execuções anteriores esse teste tinha passado. Ou seja, gravar dentro de `afterCompletion` não era confiável: às vezes travava o pool, às vezes simplesmente não gravava. Não isolei a causa exata desse segundo comportamento. Como o mecanismo foi removido, os dois problemas saem juntos.

## Correção

- `ReservaService.criar` e `alterar` passaram a usar `TransactionTemplate`. O conflito sai como `ConflitoDeAgenda`, a transação termina e devolve a conexão, e só depois o conflito é gravado.
- Removido o listener `RegistroConflitos`.
- Workflow de carga: o passo do k6 usa `shell: bash` (com `pipefail`). Antes, o `| tee` escondia a falha do k6 e o job ficava verde mesmo reprovado.

## Testes de regressão

- `ConcorrenciaTest` agora dispara 40 pedidos simultâneos (4× o pool) e o pool de teste tem timeout de 5 s, então o travamento faria o teste falhar.
- Segunda rodada do teste de carga: [resultado-02](../performance/resultado-02.md).
