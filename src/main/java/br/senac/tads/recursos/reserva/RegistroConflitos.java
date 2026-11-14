package br.senac.tads.recursos.reserva;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import br.senac.tads.recursos.reserva.Eventos.ConflitoDetectado;

/**
 * Grava o conflito depois do rollback da reserva recusada. Se gravasse dentro da mesma transacao,
 * o registro seria desfeito junto; gravando antes do rollback, seguraria a trava do recurso por mais tempo.
 */
@Component
public class RegistroConflitos {

    private final ConflitoEvitadoRepository conflitos;

    public RegistroConflitos(ConflitoEvitadoRepository conflitos) {
        this.conflitos = conflitos;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(ConflitoDetectado evento) {
        conflitos.save(new ConflitoEvitado(evento.recursoId(), evento.inicio(), evento.fim()));
    }
}
