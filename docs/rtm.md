# Matriz de rastreabilidade (RTM)

Liga cada requisito ao código que o implementa e ao teste que prova. Testes em `src/test/java/br/senac/tads/recursos/`.

## Requisitos funcionais

| Id | Requisito | Implementação | Testes | Situação |
|---|---|---|---|---|
| RF-01 | Autenticação e autorização por perfil | `seguranca/SegurancaConfig`, `TokenService`, `UsuarioService` | `AutenticacaoTest`, `AutorizacaoTest` | Atendido |
| RF-02 | Cadastro e consulta de salas, professores e materiais | `recurso/RecursoService`, `RecursoController` | `RecursoTest.administradorCriaAtualizaEExclui`, `RecursoTest.dadosInvalidosRetornam400` | Atendido |
| RF-03 | Pesquisa por tipo, capacidade, localização, competência e disponibilidade | `RecursoService.pesquisar` | `RecursoTest.pesquisaPorTipoCapacidadeLocalECompetencia`, `RecursoTest.pesquisaPorDisponibilidadeTiraOcupadosEEmManutencao` | Atendido |
| RF-04 | Criação, alteração e cancelamento de reservas | `reserva/ReservaService.criar/alterar/cancelar` | `RegrasReservaTest`, `AutorizacaoTest.solicitanteNaoVeNemMexeNaReservaDeOutro` | Atendido |
| RF-05 | Detecção de sobreposição, inclusive agenda do professor | `ReservaRepository.existeReservaNoPeriodo`, `ReservaService.travarDisponiveis` | `RegrasReservaTest.mesmaSalaNoMesmoHorarioRetorna409`, `...professorNaoPodeEstarEmDuasSalasAoMesmoTempo`, `...horarioEncostadoNaoConflita` | Atendido |
| RF-06 | Proteção contra dupla reserva em pedidos simultâneos | `RecursoRepository.travar` (FOR UPDATE) | `ConcorrenciaTest` (2 cenários), teste de carga (`docs/performance`) | Atendido |
| RF-07 | Aprovação obrigatória para recursos restritos | `Recurso.restrito`, `Reserva.definir`, rotas de aprovação só para gestores | `RegrasReservaTest.recursoRestritoAguardaAprovacao`, `CicloDeVidaReservaTest`, cenários BDD `aprovacao.feature` | Atendido |
| RF-08 | Bloqueio de recursos em manutenção | `ReservaService.travarDisponiveis`, `RecursoService.estaLivre` | `RegrasReservaTest.recursoEmManutencaoNaoPodeSerReservado`, `RecursoTest.pesquisaPorDisponibilidade...` | Atendido |
| RF-09 | Registro de retirada e devolução de materiais | `material/RetiradaMaterialService` | `RetiradaMaterialTest` | Atendido |
| RF-10 | Histórico auditável de mudanças | `HistoricoReserva`, `ReservaService.registrar` | `CicloDeVidaReservaTest.fluxoCompletoFicaRegistradoNoHistorico`, `AuditoriaAtomicaTest` | Atendido |
| RF-11 | Notificação simulada ou integração externa | `notificacao/Notificador` | `NotificacaoTest` (WireMock) | Atendido |
| RF-12 | Relatório de uso por recurso, carga horária e conflitos evitados | `relatorio/RelatorioController`, `RegistroConflitos` | `RelatorioUtilizacaoTest`, `ConcorrenciaTest` (conta conflitos) | Atendido |
| RF-13 | Interface responsiva e mensagens de erro compreensíveis | `static/index.html`, `app.js`, `estilo.css`, `TratamentoErros` | Mensagens checadas nos testes de API (`$.mensagem`); tela sem teste automatizado | Parcial |
| RF-14 | Documentação da API ou dos fluxos públicos | `README.md`, Swagger em `/swagger-ui.html` | `AutenticacaoTest.paginaEDocumentacaoSaoPublicas` | Atendido |

## Regras críticas

| Regra | Onde | Teste |
|---|---|---|
| Término depois do início | `ReservaService.validarPeriodo` | `RegrasReservaTest.terminoPrecisaSerDepoisDoInicio`, `...alterarComPeriodoInvalidoRetorna400` |
| Mesmo recurso não se sobrepõe | `ReservaRepository.existeReservaNoPeriodo` | `RegrasReservaTest` (sobreposição parcial, envolvente e encostada) |
| Pedidos simultâneos geram uma reserva só | `RecursoRepository.travar` | `ConcorrenciaTest` |
| Recurso em manutenção não é reservado | `ReservaService.travarDisponiveis` | `RegrasReservaTest.recursoEmManutencaoNaoPodeSerReservado` |
| Máquina de estados completa | `StatusReserva` | `StatusReservaTest` (todas as 49 combinações), `CicloDeVidaReservaTest` |
| Só responsável e administrador aprovam | `SegurancaConfig` | `AutorizacaoTest.papelAlcancaSoOQueLheCabe`, BDD "Solicitante não consegue aprovar" |
| Reserva em uso não é cancelada | `StatusReserva` | `CicloDeVidaReservaTest.reservaEmUsoNaoPodeSerCancelada` |
| Toda mudança de estado gera auditoria | `ReservaService.registrar` | `CicloDeVidaReservaTest.fluxoCompletoFicaRegistradoNoHistorico`, `AuditoriaAtomicaTest` |
| Cadastro público não cria administrador | `UsuarioService.cadastrar` | `AutenticacaoTest.cadastroPublicoSempreCriaSolicitanteMesmoPedindoOutroPapel` |
| Solicitante só mexe nas próprias reservas | `ReservaService.exigirDonoOuGestor` | `AutorizacaoTest.solicitanteNaoVeNemMexeNaReservaDeOutro`, `...listagemDoSolicitanteTrazSoAsDele` |
| Não conclui com material pendente | `ReservaService.concluir` | `RetiradaMaterialTest.naoConcluiComMaterialPendente` |

## Requisitos não funcionais

| Id | Requisito | Meta | Evidência |
|---|---|---|---|
| RNF-01 | Consulta de disponibilidade sob carga | 50 usuários, p95 ≤ 300 ms, erro ≤ 1% | `docs/performance/resultado-01.md` |
| RNF-02 | Criação de reserva disputada sob carga | 50 usuários no mesmo horário, p95 ≤ 500 ms, exatamente 1 reserva por horário, nenhum 5xx | `docs/performance/resultado-01.md` |
| RNF-03 | Cobertura de testes | ≥ 80% linhas, ≥ 70% branches (build falha abaixo disso) | JaCoCo no CI |
| RNF-04 | Qualidade estática | Quality Gate do SonarCloud aprovado | Link no `docs/evidencias/README.md` |
| RNF-05 | Reprodutibilidade | Subir só com o README em máquina limpa | `docs/entrega/checklist.md` |
