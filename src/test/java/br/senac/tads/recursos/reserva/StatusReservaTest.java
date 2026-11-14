package br.senac.tads.recursos.reserva;

import static br.senac.tads.recursos.reserva.StatusReserva.APROVADA;
import static br.senac.tads.recursos.reserva.StatusReserva.CANCELADA;
import static br.senac.tads.recursos.reserva.StatusReserva.CONCLUIDA;
import static br.senac.tads.recursos.reserva.StatusReserva.EM_USO;
import static br.senac.tads.recursos.reserva.StatusReserva.NAO_COMPARECEU;
import static br.senac.tads.recursos.reserva.StatusReserva.REJEITADA;
import static br.senac.tads.recursos.reserva.StatusReserva.SOLICITADA;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class StatusReservaTest {

    /** Tabela esperada da maquina de estados. O que nao esta aqui deve ser recusado. */
    private static final Map<StatusReserva, Set<StatusReserva>> ESPERADO = Map.of(
            SOLICITADA, EnumSet.of(APROVADA, REJEITADA, CANCELADA),
            APROVADA, EnumSet.of(EM_USO, CANCELADA, NAO_COMPARECEU),
            EM_USO, EnumSet.of(CONCLUIDA));

    @ParameterizedTest
    @EnumSource(StatusReserva.class)
    void segueATabelaDeTransicoes(StatusReserva origem) {
        Set<StatusReserva> permitidos = ESPERADO.getOrDefault(origem, Set.of());
        for (StatusReserva destino : StatusReserva.values()) {
            assertThat(origem.podeIrPara(destino))
                    .as("%s -> %s", origem, destino)
                    .isEqualTo(permitidos.contains(destino));
        }
    }

    @Test
    void statusFinaisNaoSaemDoLugar() {
        assertThat(EnumSet.allOf(StatusReserva.class)).filteredOn(StatusReserva::isFinal)
                .containsExactlyInAnyOrder(REJEITADA, CANCELADA, CONCLUIDA, NAO_COMPARECEU);
    }

    @Test
    void soReservasAtivasOcupamAgenda() {
        assertThat(StatusReserva.OCUPAM_AGENDA).containsExactlyInAnyOrder(SOLICITADA, APROVADA, EM_USO);
    }
}
