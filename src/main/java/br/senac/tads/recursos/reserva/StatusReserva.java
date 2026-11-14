package br.senac.tads.recursos.reserva;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum StatusReserva {
    SOLICITADA,
    APROVADA,
    REJEITADA,
    CANCELADA,
    EM_USO,
    CONCLUIDA,
    NAO_COMPARECEU;

    /** Status que seguram o horario do recurso. */
    public static final Set<StatusReserva> OCUPAM_AGENDA = EnumSet.of(SOLICITADA, APROVADA, EM_USO);

    private static final Map<StatusReserva, Set<StatusReserva>> TRANSICOES = Map.of(
            SOLICITADA, EnumSet.of(APROVADA, REJEITADA, CANCELADA),
            APROVADA, EnumSet.of(EM_USO, CANCELADA, NAO_COMPARECEU),
            EM_USO, EnumSet.of(CONCLUIDA));

    public boolean podeIrPara(StatusReserva destino) {
        return TRANSICOES.getOrDefault(this, Set.of()).contains(destino);
    }

    public boolean isFinal() {
        return !TRANSICOES.containsKey(this);
    }
}
