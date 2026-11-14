package br.senac.tads.recursos.reserva;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;

import br.senac.tads.recursos.comum.ErroApi;
import br.senac.tads.recursos.recurso.Recurso;

/** Recurso ja ocupado no periodo pedido. Leva os dados para registrar o conflito evitado. */
class ConflitoDeAgenda extends ErroApi {

    private final Long recursoId;
    private final transient LocalDateTime inicio;
    private final transient LocalDateTime fim;

    ConflitoDeAgenda(Recurso recurso, LocalDateTime inicio, LocalDateTime fim) {
        super(HttpStatus.CONFLICT, recurso.getNome() + " já está reservado nesse horário");
        this.recursoId = recurso.getId();
        this.inicio = inicio;
        this.fim = fim;
    }

    Long getRecursoId() {
        return recursoId;
    }

    LocalDateTime getInicio() {
        return inicio;
    }

    LocalDateTime getFim() {
        return fim;
    }
}
