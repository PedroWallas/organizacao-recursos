package br.senac.tads.recursos.relatorio;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import br.senac.tads.recursos.recurso.Recurso;
import br.senac.tads.recursos.seguranca.Papel;
import br.senac.tads.recursos.suporte.TesteIntegracao;

class RelatorioUtilizacaoTest extends TesteIntegracao {

    @Test
    void somaHorasReservasEConflitosPorRecurso() throws Exception {
        Recurso auditorio = sala("Auditório");
        Recurso sala = sala("Sala 2");
        String ana = token(Papel.SOLICITANTE);

        novaReserva(ana, AMANHA_8H, AMANHA_8H.plusHours(2), auditorio);
        novaReserva(ana, AMANHA_8H.plusHours(3), AMANHA_8H.plusMinutes(210), auditorio);
        Long cancelada = novaReserva(ana, AMANHA_8H.plusHours(5), AMANHA_8H.plusHours(9), auditorio);
        acao("/reservas/" + cancelada + "/cancelar", ana).andExpect(status().isOk());
        reservar(ana, AMANHA_8H.plusHours(1), AMANHA_8H.plusHours(2), auditorio).andExpect(status().isConflict());

        consultar("/relatorios/utilizacao", token(Papel.RESPONSAVEL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].recurso").value("Auditório"))
                .andExpect(jsonPath("$[0].reservas").value(2))
                .andExpect(jsonPath("$[0].horas").value(2.5))
                .andExpect(jsonPath("$[0].conflitosEvitados").value(1))
                .andExpect(jsonPath("$[1].recurso").value(sala.getNome()))
                .andExpect(jsonPath("$[1].reservas").value(0))
                .andExpect(jsonPath("$[1].horas").value(0.0));
    }
}
