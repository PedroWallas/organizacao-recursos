package br.senac.tads.recursos.material;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import br.senac.tads.recursos.recurso.Recurso;
import br.senac.tads.recursos.reserva.Reserva;

/** Uma retirada de material ligada a uma reserva. Fica aberta ate a devolucao ser registrada. */
@Entity
@Table(name = "retirada_material")
public class RetiradaMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id")
    private Reserva reserva;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id")
    private Recurso material;

    @Column(name = "retirada_em", nullable = false)
    private LocalDateTime retiradaEm;

    @Column(name = "retirado_por", nullable = false, length = 160)
    private String retiradoPor;

    @Column(name = "devolvida_em")
    private LocalDateTime devolvidaEm;

    @Column(name = "recebido_por", length = 160)
    private String recebidoPor;

    protected RetiradaMaterial() {
    }

    public RetiradaMaterial(Reserva reserva, Recurso material, String responsavel) {
        this.reserva = reserva;
        this.material = material;
        this.retiradoPor = responsavel;
        this.retiradaEm = LocalDateTime.now();
    }

    void devolver(String responsavel) {
        this.devolvidaEm = LocalDateTime.now();
        this.recebidoPor = responsavel;
    }

    public Long getId() {
        return id;
    }

    public Recurso getMaterial() {
        return material;
    }

    public LocalDateTime getRetiradaEm() {
        return retiradaEm;
    }

    public String getRetiradoPor() {
        return retiradoPor;
    }

    public LocalDateTime getDevolvidaEm() {
        return devolvidaEm;
    }

    public String getRecebidoPor() {
        return recebidoPor;
    }
}
