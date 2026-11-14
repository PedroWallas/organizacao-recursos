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
