package br.senac.tads.recursos.reserva;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Linha de auditoria: quem mudou a reserva, quando e de qual status para qual. Nunca e alterada. */
@Entity
@Table(name = "historico_reserva")
public class HistoricoReserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id")
    private Reserva reserva;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", length = 20)
    private StatusReserva statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false, length = 20)
    private StatusReserva statusNovo;

    @Column(nullable = false, length = 160)
    private String autor;

    @Column(nullable = false, length = 40)
    private String acao;

    @Column(nullable = false)
    private LocalDateTime momento;

    protected HistoricoReserva() {
    }

    public HistoricoReserva(Reserva reserva, StatusReserva statusAnterior, String autor, String acao) {
        this.reserva = reserva;
        this.statusAnterior = statusAnterior;
        this.statusNovo = reserva.getStatus();
        this.autor = autor;
        this.acao = acao;
        this.momento = LocalDateTime.now();
    }

    public StatusReserva getStatusAnterior() {
        return statusAnterior;
    }

    public StatusReserva getStatusNovo() {
        return statusNovo;
    }

    public String getAutor() {
        return autor;
    }

    public String getAcao() {
        return acao;
    }

    public LocalDateTime getMomento() {
        return momento;
    }
}
