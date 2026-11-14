package br.senac.tads.recursos.reserva;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import br.senac.tads.recursos.comum.ErroApi;
import br.senac.tads.recursos.material.RetiradaMaterialRepository;
import br.senac.tads.recursos.recurso.Recurso;
import br.senac.tads.recursos.recurso.RecursoRepository;
import br.senac.tads.recursos.recurso.TipoRecurso;
import br.senac.tads.recursos.reserva.Eventos.StatusAlterado;
import br.senac.tads.recursos.seguranca.UsuarioLogado;
import br.senac.tads.recursos.seguranca.UsuarioRepository;

@Service
public class ReservaService {

    public record PedidoReserva(
            @NotEmpty List<@NotNull Long> recursoIds,
            @NotNull LocalDateTime inicio,
            @NotNull LocalDateTime fim,
            @Size(max = 200) String finalidade) {
    }

    public record RecursoResumo(Long id, String nome, TipoRecurso tipo) {
    }

    public record ReservaResposta(Long id, String solicitante, List<RecursoResumo> recursos, LocalDateTime inicio,
            LocalDateTime fim, String finalidade, StatusReserva status) {
        static ReservaResposta de(Reserva r) {
            List<RecursoResumo> recursos = r.getRecursos().stream()
                    .sorted(Comparator.comparing(Recurso::getId))
                    .map(rec -> new RecursoResumo(rec.getId(), rec.getNome(), rec.getTipo()))
                    .toList();
            return new ReservaResposta(r.getId(), r.getSolicitante().getEmail(), recursos, r.getInicio(), r.getFim(),
                    r.getFinalidade(), r.getStatus());
        }
    }

    public record HistoricoResposta(StatusReserva de, StatusReserva para, String autor, String acao,
            LocalDateTime momento) {
    }

    private final ReservaRepository reservas;
    private final RecursoRepository recursos;
    private final UsuarioRepository usuarios;
    private final HistoricoReservaRepository historico;
    private final RetiradaMaterialRepository retiradas;
    private final ConflitoEvitadoRepository conflitos;
    private final ApplicationEventPublisher eventos;
    private final TransactionTemplate transacao;

    public ReservaService(ReservaRepository reservas, RecursoRepository recursos, UsuarioRepository usuarios,
            HistoricoReservaRepository historico, RetiradaMaterialRepository retiradas,
            ConflitoEvitadoRepository conflitos, ApplicationEventPublisher eventos, TransactionTemplate transacao) {
        this.reservas = reservas;
        this.recursos = recursos;
        this.usuarios = usuarios;
        this.historico = historico;
        this.retiradas = retiradas;
        this.conflitos = conflitos;
        this.eventos = eventos;
        this.transacao = transacao;
    }

    public ReservaResposta criar(PedidoReserva pedido, UsuarioLogado usuario) {
        return registrandoConflito(() -> transacao.execute(status -> criarNaTransacao(pedido, usuario)));
    }

    public ReservaResposta alterar(Long id, PedidoReserva pedido, UsuarioLogado usuario) {
        return registrandoConflito(() -> transacao.execute(status -> alterarNaTransacao(id, pedido, usuario)));
    }

    /**
     * DEF-03: o conflito so e gravado depois que a transacao da reserva terminou e devolveu a conexao ao pool.
     * Gravar com a conexao ainda presa fazia cada pedido recusado precisar de duas conexoes e travava o pool sob carga.
     */
    private ReservaResposta registrandoConflito(Supplier<ReservaResposta> operacao) {
        try {
            return operacao.get();
        } catch (ConflitoDeAgenda e) {
            conflitos.save(new ConflitoEvitado(e.getRecursoId(), e.getInicio(), e.getFim()));
            throw e;
        }
    }

    private ReservaResposta criarNaTransacao(PedidoReserva pedido, UsuarioLogado usuario) {
        validarPeriodo(pedido);
        List<Recurso> travados = travarDisponiveis(pedido, ReservaRepository.NENHUMA);
        Reserva reserva = new Reserva(usuarios.getReferenceById(usuario.id()), new HashSet<>(travados),
                pedido.inicio(), pedido.fim(), pedido.finalidade());
        reservas.save(reserva);
        registrar(reserva, null, usuario, "CRIACAO");
        return ReservaResposta.de(reserva);
    }

    private ReservaResposta alterarNaTransacao(Long id, PedidoReserva pedido, UsuarioLogado usuario) {
        validarPeriodo(pedido);
        Reserva reserva = carregar(id);
        exigirDonoOuGestor(reserva, usuario);
        if (!reserva.podeSerAlterada()) {
            throw ErroApi.conflito("Só é possível alterar reservas solicitadas ou aprovadas");
        }
        if (temMaterialPendente(reserva)) {
            throw ErroApi.conflito("Devolva o material retirado antes de alterar a reserva");
        }
        StatusReserva anterior = reserva.getStatus();
        List<Recurso> travados = travarDisponiveis(pedido, id);
        reserva.definir(new HashSet<>(travados), pedido.inicio(), pedido.fim(), pedido.finalidade());
        registrar(reserva, anterior, usuario, "ALTERACAO");
        return ReservaResposta.de(reserva);
    }

    @Transactional
    public ReservaResposta cancelar(Long id, UsuarioLogado usuario) {
        Reserva reserva = carregar(id);
        exigirDonoOuGestor(reserva, usuario);
        return mudar(reserva, StatusReserva.CANCELADA, usuario);
    }

    @Transactional
    public ReservaResposta aprovar(Long id, UsuarioLogado usuario) {
        return mudar(carregar(id), StatusReserva.APROVADA, usuario);
    }

    @Transactional
    public ReservaResposta rejeitar(Long id, UsuarioLogado usuario) {
        return mudar(carregar(id), StatusReserva.REJEITADA, usuario);
    }

    @Transactional
    public ReservaResposta iniciar(Long id, UsuarioLogado usuario) {
        return mudar(carregar(id), StatusReserva.EM_USO, usuario);
    }

    @Transactional
    public ReservaResposta concluir(Long id, UsuarioLogado usuario) {
        return mudar(carregar(id), StatusReserva.CONCLUIDA, usuario);
    }

    @Transactional
    public ReservaResposta registrarAusencia(Long id, UsuarioLogado usuario) {
        return mudar(carregar(id), StatusReserva.NAO_COMPARECEU, usuario);
    }

    @Transactional(readOnly = true)
    public List<ReservaResposta> listar(UsuarioLogado usuario) {
        List<Reserva> lista = usuario.isGestor()
                ? reservas.findAllByOrderByInicioAsc()
                : reservas.findBySolicitante_IdOrderByInicioAsc(usuario.id());
        return lista.stream().map(ReservaResposta::de).toList();
    }

    @Transactional(readOnly = true)
    public ReservaResposta buscar(Long id, UsuarioLogado usuario) {
        Reserva reserva = carregar(id);
        exigirDonoOuGestor(reserva, usuario);
        return ReservaResposta.de(reserva);
    }

    @Transactional(readOnly = true)
    public List<HistoricoResposta> historico(Long id, UsuarioLogado usuario) {
        exigirDonoOuGestor(carregar(id), usuario);
        return historico.findByReserva_IdOrderByIdAsc(id).stream()
                .map(h -> new HistoricoResposta(h.getStatusAnterior(), h.getStatusNovo(), h.getAutor(), h.getAcao(),
                        h.getMomento()))
                .toList();
    }

    Reserva carregar(Long id) {
        return reservas.findById(id).orElseThrow(() -> ErroApi.naoEncontrado("Reserva " + id + " não encontrada"));
    }

    private ReservaResposta mudar(Reserva reserva, StatusReserva destino, UsuarioLogado usuario) {
        StatusReserva anterior = reserva.getStatus();
        reserva.mudarPara(destino);
        // DEF-01: reserva encerrada com material fora deixaria a devolucao sem caminho
        if (destino.isFinal() && temMaterialPendente(reserva)) {
            throw ErroApi.conflito("Há material retirado e não devolvido nesta reserva");
        }
        registrar(reserva, anterior, usuario, destino.name());
        return ReservaResposta.de(reserva);
    }

    /** Auditoria na mesma transacao: se o historico falhar, a mudanca tambem e desfeita. */
    private void registrar(Reserva reserva, StatusReserva anterior, UsuarioLogado usuario, String acao) {
        historico.save(new HistoricoReserva(reserva, anterior, usuario.email(), acao));
        eventos.publishEvent(new StatusAlterado(reserva.getId(), anterior, reserva.getStatus(),
                reserva.getSolicitante().getEmail(), usuario.email()));
    }

    private List<Recurso> travarDisponiveis(PedidoReserva pedido, long ignorar) {
        List<Long> ids = pedido.recursoIds().stream().distinct().toList();
        // DEF-04: 1a checagem sem trava. Pedido para horario ja ocupado e recusado aqui, sem entrar na fila da trava.
        conferirDisponiveis(recursos.findAllById(ids), ids.size(), pedido, ignorar);
        // 2a checagem com trava: garante que ninguem reservou entre a primeira checagem e a trava.
        List<Recurso> travados = recursos.travar(ids);
        conferirDisponiveis(travados, ids.size(), pedido, ignorar);
        return travados;
    }

    private void conferirDisponiveis(List<Recurso> encontrados, int pedidos, PedidoReserva pedido, long ignorar) {
        if (encontrados.size() != pedidos) {
            throw ErroApi.naoEncontrado("Um ou mais recursos não existem");
        }
        for (Recurso recurso : encontrados) {
            if (recurso.isEmManutencao()) {
                throw ErroApi.regraViolada(recurso.getNome() + " está em manutenção");
            }
            if (reservas.existeConflito(recurso.getId(), pedido.inicio(), pedido.fim(), ignorar)) {
                throw new ConflitoDeAgenda(recurso, pedido.inicio(), pedido.fim());
            }
        }
    }

    private boolean temMaterialPendente(Reserva reserva) {
        return retiradas.existsByReserva_IdAndDevolvidaEmIsNull(reserva.getId());
    }

    private static void validarPeriodo(PedidoReserva pedido) {
        if (!pedido.fim().isAfter(pedido.inicio())) {
            throw ErroApi.requisicaoInvalida("O término precisa ser depois do início");
        }
        // DEF-02: reserva retroativa ocupava a agenda e distorcia o relatorio de uso
        if (pedido.inicio().isBefore(LocalDateTime.now())) {
            throw ErroApi.regraViolada("Não é possível reservar um horário que já passou");
        }
    }

    private static void exigirDonoOuGestor(Reserva reserva, UsuarioLogado usuario) {
        if (!usuario.isGestor() && !reserva.getSolicitante().getId().equals(usuario.id())) {
            throw ErroApi.proibido("Você só pode mexer nas suas próprias reservas");
        }
    }
}
