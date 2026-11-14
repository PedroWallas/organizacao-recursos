package br.senac.tads.recursos.relatorio;

import java.time.Duration;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import br.senac.tads.recursos.recurso.Recurso;
import br.senac.tads.recursos.recurso.RecursoRepository;
import br.senac.tads.recursos.recurso.TipoRecurso;
import br.senac.tads.recursos.reserva.ConflitoEvitadoRepository;
import br.senac.tads.recursos.reserva.ConflitoEvitadoRepository.TotalPorRecurso;
import br.senac.tads.recursos.reserva.Reserva;
import br.senac.tads.recursos.reserva.ReservaRepository;
import br.senac.tads.recursos.reserva.StatusReserva;

@RestController
public class RelatorioController {

    /** Reservas que de fato usaram (ou vao usar) o recurso. Canceladas e rejeitadas ficam de fora. */
    private static final EnumSet<StatusReserva> CONTAM_USO = EnumSet.of(StatusReserva.APROVADA, StatusReserva.EM_USO,
            StatusReserva.CONCLUIDA);

    public record LinhaUtilizacao(Long recursoId, String recurso, TipoRecurso tipo, long reservas, double horas,
            long conflitosEvitados) {
    }

    private final RecursoRepository recursos;
    private final ReservaRepository reservas;
    private final ConflitoEvitadoRepository conflitos;

    public RelatorioController(RecursoRepository recursos, ReservaRepository reservas,
            ConflitoEvitadoRepository conflitos) {
        this.recursos = recursos;
        this.reservas = reservas;
        this.conflitos = conflitos;
    }

    // shortcut: agrega em memoria todas as reservas; ok para o volume de uma escola. Se crescer, agregar no banco por periodo.
    @GetMapping("/relatorios/utilizacao")
    @Transactional(readOnly = true)
    public List<LinhaUtilizacao> utilizacao() {
        Map<Long, Long> quantidade = new HashMap<>();
        Map<Long, Duration> tempo = new HashMap<>();
        for (Reserva reserva : reservas.buscarComRecursos(CONTAM_USO)) {
            for (Recurso recurso : reserva.getRecursos()) {
                quantidade.merge(recurso.getId(), 1L, Long::sum);
                tempo.merge(recurso.getId(), reserva.duracao(), Duration::plus);
            }
        }
        Map<Long, Long> barrados = new HashMap<>();
        for (TotalPorRecurso total : conflitos.totalPorRecurso()) {
            barrados.put(total.getRecursoId(), total.getTotal());
        }
        return recursos.findAll().stream()
                .sorted(Comparator.comparing(Recurso::getNome))
                .map(r -> new LinhaUtilizacao(r.getId(), r.getNome(), r.getTipo(),
                        quantidade.getOrDefault(r.getId(), 0L),
                        tempo.getOrDefault(r.getId(), Duration.ZERO).toMinutes() / 60.0,
                        barrados.getOrDefault(r.getId(), 0L)))
                .toList();
    }
}
