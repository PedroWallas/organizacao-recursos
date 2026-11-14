# DEF-04 Tempo de resposta sob carga acima das metas RNF-01 e RNF-02

| Campo | Valor |
|---|---|
| Severidade | Média (sistema responde certo, mas devagar sob pico) |
| Prioridade | Média |
| Encontrado em | 2026-11-13, segunda rodada do teste de carga ([resultado-02](../performance/resultado-02.md)) |
| Técnica | Teste de carga (k6) + análise das consultas feitas por requisição |
| Versão afetada | commit `e95e01a` (main após PR #4) |
| Situação | **Parcialmente corrigido. Restante aceito como risco** |

## Como reproduzir

Rodar o workflow **Teste de carga** no commit afetado.

## Resultado esperado

p95 da consulta ≤ 300 ms e p95 da reserva ≤ 500 ms com 50 usuários em cada cenário.

## Resultado obtido

p95 da consulta 1,05 s e p95 da reserva 1,20 s, sem erros.

## Causas encontradas

1. **N+1 na pesquisa por disponibilidade**: uma consulta ao banco por recurso (21 por requisição no teste).
2. **Fila desnecessária na trava**: pedidos para horário já ocupado entravam na fila do `SELECT ... FOR UPDATE` só para receber 409, segurando conexões do pool enquanto esperavam.

## Correção

1. `ReservaRepository.recursosOcupados`: uma consulta traz todos os recursos ocupados no período. `RecursoService.pesquisar` usa esse conjunto.
2. `ReservaService.travarDisponiveis`: primeira checagem **sem trava** (recusa rápido quem já perdeu) e segunda checagem **com trava** (mantém a garantia contra reserva dupla).

A garantia de uma reserva por horário continua coberta por `ConcorrenciaTest` (40 pedidos) e pelo teste de carga (5 de 5 horários com exatamente uma reserva em todas as rodadas).

## Resultado depois da correção

| Rodada | p95 consulta | p95 reserva |
|---|---|---|
| 02 (antes) | 1,05 s | 1,20 s |
| 03 (correção) | 703 ms | 817 ms |
| 04 (correção + aquecimento da JVM) | 375 ms | 449 ms |

RNF-02 atendido. RNF-01 ficou 75 ms acima da meta no p95.

## Risco aceito

| Campo | Valor |
|---|---|
| Risco | Em pico de 50 consultas simultâneas, 5% das consultas podem levar entre 300 e 630 ms |
| Impacto | Baixo: a tela demora um pouco mais no pico; nenhum dado errado e nenhuma reserva dupla |
| Justificativa | A medição roda com app, banco e gerador de carga na mesma máquina de 2 vCPUs. Mediana de 123 ms. Mais otimização agora teria retorno incerto nesse ambiente |
| Responsável | Pedro Wallas Santana da Silva |
| Ação futura | Medir em ambiente com o gerador de carga separado; se continuar acima, paginar a pesquisa e filtrar tipo/capacidade no banco |
