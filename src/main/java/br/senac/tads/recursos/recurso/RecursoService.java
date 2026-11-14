package br.senac.tads.recursos.recurso;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.senac.tads.recursos.comum.ErroApi;
import br.senac.tads.recursos.reserva.ReservaRepository;

@Service
public class RecursoService {

    public record RecursoDados(
            @NotBlank @Size(max = 120) String nome,
            @NotNull TipoRecurso tipo,
            @Positive Integer capacidade,
            @Size(max = 120) String localizacao,
            @Size(max = 120) String competencia,
            boolean restrito,
            boolean emManutencao) {
    }

    public record RecursoResposta(Long id, String nome, TipoRecurso tipo, Integer capacidade, String localizacao,
            String competencia, boolean restrito, boolean emManutencao) {
        public static RecursoResposta de(Recurso r) {
            return new RecursoResposta(r.getId(), r.getNome(), r.getTipo(), r.getCapacidade(), r.getLocalizacao(),
                    r.getCompetencia(), r.isRestrito(), r.isEmManutencao());
        }
    }

    public record Filtro(TipoRecurso tipo, Integer capacidadeMinima, String localizacao, String competencia,
            LocalDateTime livreDe, LocalDateTime livreAte) {
    }

    private final RecursoRepository recursos;
    private final ReservaRepository reservas;

    public RecursoService(RecursoRepository recursos, ReservaRepository reservas) {
        this.recursos = recursos;
        this.reservas = reservas;
    }

    // shortcut: filtra em memoria; serve para algumas centenas de recursos. Se crescer, trocar por consulta com Specification.
    @Transactional(readOnly = true)
    public List<RecursoResposta> pesquisar(Filtro filtro) {
        boolean filtraDisponibilidade = validarPeriodo(filtro);
        return recursos.findAllByOrderByNomeAsc().stream()
                .filter(r -> filtro.tipo() == null || r.getTipo() == filtro.tipo())
                .filter(r -> filtro.capacidadeMinima() == null
                        || (r.getCapacidade() != null && r.getCapacidade() >= filtro.capacidadeMinima()))
                .filter(r -> contem(r.getLocalizacao(), filtro.localizacao()))
                .filter(r -> contem(r.getCompetencia(), filtro.competencia()))
                .filter(r -> !filtraDisponibilidade || estaLivre(r, filtro.livreDe(), filtro.livreAte()))
                .map(RecursoResposta::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public RecursoResposta buscar(Long id) {
        return RecursoResposta.de(carregar(id));
    }

    @Transactional
    public RecursoResposta criar(RecursoDados dados) {
        Recurso recurso = new Recurso(dados.nome(), dados.tipo());
        aplicar(recurso, dados);
        return RecursoResposta.de(recursos.save(recurso));
    }

    @Transactional
    public RecursoResposta atualizar(Long id, RecursoDados dados) {
        Recurso recurso = carregar(id);
        aplicar(recurso, dados);
        return RecursoResposta.de(recurso);
    }

    @Transactional
    public void excluir(Long id) {
        Recurso recurso = carregar(id);
        if (reservas.existsByRecursos_Id(id)) {
            throw ErroApi.conflito("Recurso já tem reservas. Marque como em manutenção em vez de excluir");
        }
        recursos.delete(recurso);
    }

    private Recurso carregar(Long id) {
        return recursos.findById(id).orElseThrow(() -> ErroApi.naoEncontrado("Recurso " + id + " não encontrado"));
    }

    private static void aplicar(Recurso recurso, RecursoDados d) {
        recurso.atualizar(d.nome().trim(), d.tipo(), d.capacidade(), d.localizacao(), d.competencia(), d.restrito(),
                d.emManutencao());
    }

    private boolean estaLivre(Recurso r, LocalDateTime de, LocalDateTime ate) {
        return !r.isEmManutencao() && !reservas.existeConflito(r.getId(), de, ate, ReservaRepository.NENHUMA);
    }

    private static boolean validarPeriodo(Filtro filtro) {
        if ((filtro.livreDe() == null) != (filtro.livreAte() == null)) {
            throw ErroApi.requisicaoInvalida("Informe livreDe e livreAte juntos");
        }
        if (filtro.livreDe() == null) {
            return false;
        }
        if (!filtro.livreAte().isAfter(filtro.livreDe())) {
            throw ErroApi.requisicaoInvalida("livreAte deve ser depois de livreDe");
        }
        return true;
    }

    private static boolean contem(String valor, String termo) {
        if (termo == null || termo.isBlank()) {
            return true;
        }
        return valor != null && valor.toLowerCase(Locale.ROOT).contains(termo.trim().toLowerCase(Locale.ROOT));
    }
}
