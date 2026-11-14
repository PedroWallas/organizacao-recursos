# DEF-02 Sistema aceitava reserva com início no passado

| Campo | Valor |
|---|---|
| Severidade | Média (distorce agenda e relatório; não perde dados) |
| Prioridade | Média |
| Encontrado em | 2026-11-13, teste exploratório de valores limite no período |
| Técnica | Análise de valor limite (início antes de "agora") |
| Versão afetada | commit `df8c994` (main após PR #1) |
| Situação | Corrigido |

## Como reproduzir

1. Logado como solicitante, envie `POST /reservas` com `inicio` e `fim` de ontem para um recurso livre.

## Resultado esperado

422 com mensagem clara: não dá para reservar horário que já passou.

## Resultado obtido

201, reserva APROVADA. Ela aparece no relatório de utilização como horas usadas, e uma remarcação (`PUT`) também aceitava jogar a reserva para o passado.

## Causa

`validarPeriodo` checava só se o término vinha depois do início.

## Correção

`ReservaService.validarPeriodo` recusa início anterior ao momento atual, tanto na criação quanto na alteração.

## Testes de regressão

- `RegrasReservaTest.naoReservaNemRemarcaParaOPassado`
