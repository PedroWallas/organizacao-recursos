# DEF-01 Reserva podia ser encerrada ou alterada com material ainda retirado

| Campo | Valor |
|---|---|
| Severidade | Alta (material fica "preso": não há como registrar a devolução) |
| Prioridade | Alta |
| Encontrado em | 2026-11-13, revisão das regras de material depois do merge do PR #1 |
| Técnica | Revisão de código + teste exploratório da máquina de estados × retirada |
| Versão afetada | commit `df8c994` (main após PR #1) |
| Situação | Corrigido |

## Como reproduzir

1. Crie uma reserva com uma sala e um projetor (recursos livres, nasce APROVADA).
2. Como responsável, `POST /reservas/{id}/retiradas` com o projetor.
3. Escolha um dos caminhos:
   - a) `POST /reservas/{id}/cancelar` como solicitante; ou
   - b) `POST /reservas/{id}/nao-compareceu` como responsável; ou
   - c) `PUT /reservas/{id}` tirando o projetor da lista de recursos.

## Resultado esperado

O sistema recusa (409) enquanto houver material retirado sem devolução.

## Resultado obtido

- a) e b): a reserva vai para CANCELADA / NAO_COMPARECEU com a retirada em aberto.
- c): a alteração passa; depois `POST /reservas/{id}/devolucoes` responde 422 "não faz parte desta reserva". O projetor nunca mais pode ser devolvido no sistema.

Só a conclusão (`/concluir`) checava material pendente.

## Causa

A checagem de pendência estava só no método `concluir`, e não na regra geral de saída para um status final. A alteração não checava pendência nenhuma.

## Correção

- `ReservaService.mudar`: qualquer passagem para status final (cancelada, concluída, não compareceu) é recusada se houver retirada aberta.
- `ReservaService.alterar`: recusa alteração com retirada aberta.

## Testes de regressão

- `RetiradaMaterialTest.naoCancelaNemMarcaAusenciaComMaterialPendente`
- `RetiradaMaterialTest.naoAlteraReservaComMaterialPendente`
- `RetiradaMaterialTest.naoConcluiComMaterialPendente` (já existia, continua passando)
