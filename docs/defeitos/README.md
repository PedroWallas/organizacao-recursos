# Registro de defeitos

Cada defeito tem um arquivo com passos para reproduzir, causa, correção e teste de regressão.

## Classificação

- **Severidade**: Alta = perda de dados, falha de segurança ou regra crítica quebrada; Média = resultado errado com contorno; Baixa = cosmético.
- **Situação**: Aberto, Corrigido, Aceito (fica como risco conhecido).

## Defeitos

| Id | Título | Severidade | Situação | Teste de regressão |
|---|---|---|---|---|
| [DEF-01](DEF-01.md) | Reserva podia ser encerrada ou alterada com material ainda retirado | Alta | Corrigido | `RetiradaMaterialTest.naoCancelaNemMarcaAusenciaComMaterialPendente`, `...naoAlteraReservaComMaterialPendente` |
| [DEF-02](DEF-02.md) | Sistema aceitava reserva com início no passado | Média | Corrigido | `RegrasReservaTest.naoReservaNemRemarcaParaOPassado` |
| [DEF-03](DEF-03.md) | Pool de conexões travava sob carga quando havia conflito de reserva | Alta | Corrigido | `ConcorrenciaTest` com 40 pedidos simultâneos; teste de carga |
| [DEF-04](DEF-04.md) | Tempo de resposta sob carga acima das metas RNF-01 e RNF-02 | Média | Parcial: RNF-02 atendido, RNF-01 aceito como risco | Teste de carga (rodadas 03 e 04) |
