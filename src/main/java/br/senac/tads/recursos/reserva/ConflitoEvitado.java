package br.senac.tads.recursos.reserva;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Tentativa de reserva barrada por choque de horario. Alimenta o relatorio de conflitos evitados.
 * Guarda so o id do recurso (sem chave estrangeira) para nao disputar a trava da linha do recurso.
 */
@Entity
@Table(name = "conflito_evitado")
public class ConflitoEvitado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recurso_id", nullable = false)
    private Long recursoId;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(nullable = false)
    private LocalDateTime fim;

    @Column(nullable = false)
    private LocalDateTime momento;

    protected ConflitoEvitado() {
    }

    public ConflitoEvitado(Long recursoId, LocalDateTime inicio, LocalDateTime fim) {
        this.recursoId = recursoId;
        this.inicio = inicio;
        this.fim = fim;
        this.momento = LocalDateTime.now();
    }

    public Long getRecursoId() {
        return recursoId;
    }
}
