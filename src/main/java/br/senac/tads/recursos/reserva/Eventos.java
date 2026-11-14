package br.senac.tads.recursos.reserva;

import java.time.LocalDateTime;

/** Eventos publicados pelo servico de reservas e tratados fora da transacao principal. */
public final class Eventos {

    private Eventos() {
    }

    public record StatusAlterado(Long reservaId, StatusReserva anterior, StatusReserva novo, String solicitante,
            String autor) {
    }

    public record ConflitoDetectado(Long recursoId, LocalDateTime inicio, LocalDateTime fim) {
    }
}
