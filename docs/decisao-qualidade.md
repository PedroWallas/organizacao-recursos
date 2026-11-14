# Decisão de qualidade

| Campo | Valor |
|---|---|
| Versão avaliada | `v1.0.0-rc1` |
| Data | 2026-11-14 |
| Evidências | [docs/evidencias/README.md](evidencias/README.md) |

## Decisão: **LIBERAR COM RESSALVAS**

Liberar a `v1.0.0-rc1` **para avaliação acadêmica**. A versão **não** está liberada para produção: a evidência vem de testes automatizados e de carga num runner de CI compartilhado, não de um ambiente de produção.

## Critérios, evidência e resultado

| # | Critério | Evidência | Resultado |
|---|---|---|---|
| C1 | Aplicação sobe seguindo só o README | [checklist da entrega](entrega/checklist.md); workflow de carga sobe a aplicação do zero com PostgreSQL | ✓ |
| C2 | 100% dos testes passando no commit da tag | Execução do CI da tag (link no índice de evidências) | ✓ |
| C3 | Cobertura ≥ 80% linhas e ≥ 70% branches | JaCoCo no CI; o build falha abaixo disso ([relatório de testes](testes/relatorio.md)) | ✓ |
| C4 | Quality Gate do SonarCloud aprovado, sem bug ou vulnerabilidade aberta | [SonarCloud](https://sonarcloud.io/summary/overall?id=PedroWallas_organizacao-recursos) | ✓ |
| C5 | Nenhuma dupla reserva, inclusive em pedidos simultâneos | `ConcorrenciaTest` (40 pedidos); 4 rodadas de carga sempre com 1 reserva por horário | ✓ |
| C6 | Autorização por papel e posse da reserva | `AutorizacaoTest` (matriz papel × rota), BDD "solicitante não aprova" | ✓ |
| C7 | Toda mudança de estado auditada, de forma atômica | `CicloDeVidaReservaTest`, `AuditoriaAtomicaTest` | ✓ |
| C8 | Nenhum defeito de severidade alta aberto | [registro de defeitos](defeitos/README.md): DEF-01 e DEF-03 corrigidos | ✓ |
| C9 | RNF-02: p95 da reserva ≤ 500 ms com 50 usuários | 449 ms ([resultado-04](performance/resultado-04.md)) | ✓ |
| C10 | RNF-01: p95 da consulta ≤ 300 ms com 50 usuários | 375 ms ([resultado-04](performance/resultado-04.md)) | ✗ **ressalva** |
| C11 | RF-13: interface responsiva testada | Tela existe e as rotas que ela usa são testadas; sem teste automatizado e ainda não aberta no navegador nesta versão | ~ **ressalva** |

## Riscos aceitos

| Risco | Impacto | Justificativa | Responsável |
|---|---|---|---|
| p95 da consulta 75 ms acima da meta em pico de 50 usuários | Baixo: 5% das consultas levam até ~630 ms; nenhum erro e nenhum dado incorreto | Mediana de 123 ms. A medição divide 2 vCPUs entre app, banco e gerador de carga. Duas otimizações já aplicadas (DEF-04) reduziram o p95 de 1,05 s para 375 ms | Pedro Wallas Santana da Silva |
| Tela web sem teste automatizado e sem verificação manual nesta versão | Médio: um erro na tela passaria pelo CI | As regras e mensagens de erro são testadas na API, que a tela apenas consome. Verificação manual pendente (roteiro no relatório de testes) | Pedro Wallas Santana da Silva |
| Sem revisão por pares | Médio: nenhum segundo olhar humano no código | Trabalho individual. Mitigado com PR obrigatório, CI em todo PR, SonarCloud e revisão própria do diff antes de cada merge | Pedro Wallas Santana da Silva |
| Esquema criado pelo Hibernate e credenciais padrão de desenvolvimento | Alto se for a produção como está | Aceitável para avaliação local. Todas as credenciais são sobrescritas por variável de ambiente (README); migrações versionadas ficam como pré-requisito de produção | Pedro Wallas Santana da Silva |

## O que precisaria mudar para "liberar" sem ressalvas

1. Medir o RNF-01 com o gerador de carga em máquina separada e ficar dentro da meta.
2. Teste automatizado de interface para os fluxos de login, reserva e aprovação.
3. Revisão de código por outra pessoa.
4. Migrações de banco versionadas.

## Declaração do integrante

| Integrante | O que fez |
|---|---|
| Pedro Wallas Santana da Silva | Trabalho individual: implementação da API e da tela, modelagem das regras e da concorrência, testes automatizados (integração, concorrência, BDD, WireMock), pipeline de CI, SonarCloud, teste de carga, registro e correção dos defeitos DEF-01 a DEF-04, documentação e esta decisão |
