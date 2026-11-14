# Changelog

## [1.0.0-rc1] - 2026-11-13

Primeira versão candidata para avaliação.

### Funcionalidades

- Login com token JWT e perfis solicitante, responsável e administrador; cadastro público sempre cria solicitante.
- Cadastro de salas, professores e materiais, com marcação de recurso restrito e em manutenção.
- Pesquisa de recursos por tipo, capacidade mínima, localização, competência e horário livre.
- Reserva de um ou mais recursos no mesmo período, com alteração e cancelamento.
- Bloqueio de sobreposição para qualquer recurso, inclusive a agenda do professor.
- Proteção contra reserva dupla em pedidos simultâneos (trava na linha do recurso).
- Aprovação obrigatória para recursos restritos; ciclo SOLICITADA → APROVADA → EM_USO → CONCLUIDA, com REJEITADA, CANCELADA e NAO_COMPARECEU.
- Retirada e devolução de material vinculadas à reserva.
- Histórico auditável de cada mudança, gravado na mesma transação.
- Aviso por webhook a cada mudança de status (ou só no log, se não configurado).
- Relatório de uso por recurso: reservas, horas e conflitos evitados.
- Tela web responsiva e documentação da API no Swagger.

### Correções

- DEF-01: reserva não pode mais ser encerrada ou alterada com material retirado e não devolvido.
- DEF-02: reserva e remarcação para horário que já passou passam a ser recusadas.
- DEF-03: pool de conexões não trava mais sob carga; o conflito evitado é gravado depois da transação da reserva.
- DEF-04: pesquisa por disponibilidade com uma consulta só e checagem prévia sem trava na reserva (p95 da reserva de 1,20 s para 449 ms).
- Apontamentos da primeira análise do SonarCloud (9 bugs e 1 vulnerabilidade revisada).

### Qualidade

- Testes de integração com PostgreSQL real (Testcontainers), WireMock, cenários BDD e teste de concorrência com 40 pedidos simultâneos.
- CI no GitHub Actions com cobertura mínima de 80% de linhas e 70% de branches.
- SonarCloud com Quality Gate aprovado.
- Teste de carga com k6 para consulta e reserva disputada, em 4 rodadas documentadas.

### Ressalvas conhecidas

- RNF-01: p95 da consulta em 375 ms para meta de 300 ms no ambiente do CI (risco aceito, ver DEF-04).
- Tela web sem teste automatizado.
