package br.senac.tads.recursos.reserva;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import br.senac.tads.recursos.comum.ErroApi;
import br.senac.tads.recursos.recurso.Recurso;
import br.senac.tads.recursos.seguranca.Usuario;

@Entity
@Table(name = "reserva")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitante_id")
    private Usuario solicitante;

    @ManyToMany
    @JoinTable(name = "reserva_recurso",
            joinColumns = @JoinColumn(name = "reserva_id"),
            inverseJoinColumns = @JoinColumn(name = "recurso_id"))
    private Set<Recurso> recursos = new HashSet<>();

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(nullable = false)
    private LocalDateTime fim;

    @Column(length = 200)
    private String finalidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusReserva status;

    @Version
    private Long versao;

    protected Reserva() {
    }

    public Reserva(Usuario solicitante, Set<Recurso> recursos, LocalDateTime inicio, LocalDateTime fim,
            String finalidade) {
        this.solicitante = solicitante;
        definir(recursos, inicio, fim, finalidade);
    }

    /** Define recursos e periodo. Recurso restrito faz a reserva voltar para aprovacao. */
    final void definir(Set<Recurso> novosRecursos, LocalDateTime novoInicio, LocalDateTime novoFim,
            String novaFinalidade) {
        this.recursos = new HashSet<>(novosRecursos);
        this.inicio = novoInicio;
        this.fim = novoFim;
        this.finalidade = novaFinalidade;
        this.status = exigeAprovacao() ? StatusReserva.SOLICITADA : StatusReserva.APROVADA;
    }

    void mudarPara(StatusReserva destino) {
        if (!status.podeIrPara(destino)) {
            throw ErroApi.conflito("Não é possível passar a reserva de " + status + " para " + destino);
        }
        this.status = destino;
    }

    boolean podeSerAlterada() {
        return status == StatusReserva.SOLICITADA || status == StatusReserva.APROVADA;
    }

    boolean exigeAprovacao() {
        return recursos.stream().anyMatch(Recurso::isRestrito);
    }

    public Duration duracao() {
        return Duration.between(inicio, fim);
    }

    public Long getId() {
        return id;
    }

    public Usuario getSolicitante() {
        return solicitante;
    }

    public Set<Recurso> getRecursos() {
        return recursos;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public LocalDateTime getFim() {
        return fim;
    }

    public String getFinalidade() {
        return finalidade;
    }

    public StatusReserva getStatus() {
        return status;
    }
}
