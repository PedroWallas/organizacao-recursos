package br.senac.tads.recursos.reserva;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.senac.tads.recursos.recurso.Recurso;
import br.senac.tads.recursos.recurso.TipoRecurso;
import br.senac.tads.recursos.seguranca.Papel;
import br.senac.tads.recursos.suporte.TesteIntegracao;

class RegrasReservaTest extends TesteIntegracao {

    private String ana;

    @BeforeEach
    void solicitante() {
        ana = token(Papel.SOLICITANTE);
    }

    @Test
    void recursoLivreJaNasceAprovado() throws Exception {
        reservar(ana, AMANHA_8H, AMANHA_8H.plusHours(2), sala("Sala 1"), professor("Prof. Lia"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APROVADA"))
                .andExpect(jsonPath("$.recursos.length()").value(2));
    }

    @Test
    void recursoRestritoAguardaAprovacao() throws Exception {
        reservar(ana, AMANHA_8H, AMANHA_8H.plusHours(2), sala("Sala 1"), salaRestrita("Lab Química"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SOLICITADA"));
    }

    @Test
    void terminoPrecisaSerDepoisDoInicio() throws Exception {
        Recurso sala = sala("Sala 1");
        reservar(ana, AMANHA_8H, AMANHA_8H.minusMinutes(30), sala)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("O término precisa ser depois do início"));
        reservar(ana, AMANHA_8H, AMANHA_8H, sala).andExpect(status().isBadRequest());
    }

    /** Regressao do DEF-02. */
    @Test
    void naoReservaNemRemarcaParaOPassado() throws Exception {
        Recurso sala = sala("Sala 1");
        LocalDateTime ontem = AMANHA_8H.minusDays(2);

        reservar(ana, ontem, ontem.plusHours(1), sala)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value("Não é possível reservar um horário que já passou"));

        Long id = novaReserva(ana, AMANHA_8H, AMANHA_8H.plusHours(1), sala);
        alterar(id, ana, ontem, ontem.plusHours(1), sala).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void camposObrigatorios() throws Exception {
        enviar("/reservas", ana, Map.of("recursoIds", List.of())).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes.length()").value(3));
    }

    @Test
    void mesmaSalaNoMesmoHorarioRetorna409() throws Exception {
        Recurso sala = sala("Sala 1");
        novaReserva(ana, AMANHA_8H, AMANHA_8H.plusHours(2), sala);

        reservar(token(Papel.SOLICITANTE), AMANHA_8H.plusHours(1), AMANHA_8H.plusHours(3), sala)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Sala 1 já está reservado nesse horário"));
    }

    @Test
    void periodoQueEnvolveOutroTambemConflita() throws Exception {
        Recurso sala = sala("Sala 1");
        novaReserva(ana, AMANHA_8H.plusHours(1), AMANHA_8H.plusHours(2), sala);

        reservar(ana, AMANHA_8H, AMANHA_8H.plusHours(3), sala).andExpect(status().isConflict());
    }

    @Test
    void horarioEncostadoNaoConflita() throws Exception {
        Recurso sala = sala("Sala 1");
        novaReserva(ana, AMANHA_8H, AMANHA_8H.plusHours(2), sala);

        reservar(ana, AMANHA_8H.plusHours(2), AMANHA_8H.plusHours(4), sala).andExpect(status().isCreated());
        reservar(ana, AMANHA_8H.minusHours(1), AMANHA_8H, sala).andExpect(status().isCreated());
    }

    @Test
    void professorNaoPodeEstarEmDuasSalasAoMesmoTempo() throws Exception {
        Recurso professor = professor("Prof. Lia");
        novaReserva(ana, AMANHA_8H, AMANHA_8H.plusHours(2), sala("Sala 1"), professor);

        reservar(ana, AMANHA_8H.plusMinutes(30), AMANHA_8H.plusHours(1), sala("Sala 2"), professor)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Prof. Lia já está reservado nesse horário"));
    }

    @Test
    void recursoEmManutencaoNaoPodeSerReservado() throws Exception {
        Recurso quebrado = recurso("Projetor 2", TipoRecurso.MATERIAL, null, null, null, false, true);

        reservar(ana, AMANHA_8H, AMANHA_8H.plusHours(1), quebrado)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value("Projetor 2 está em manutenção"));
    }

    @Test
    void recursoInexistenteRetorna404() throws Exception {
        enviar("/reservas", ana, Map.of("recursoIds", List.of(999), "inicio", AMANHA_8H.toString(),
                "fim", AMANHA_8H.plusHours(1).toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void reservaCanceladaOuRejeitadaLiberaOHorario() throws Exception {
        Recurso sala = salaRestrita("Lab 1");
        Long cancelada = novaReserva(ana, AMANHA_8H, AMANHA_8H.plusHours(1), sala);
        acao("/reservas/" + cancelada + "/cancelar", ana).andExpect(status().isOk());

        Long rejeitada = novaReserva(ana, AMANHA_8H, AMANHA_8H.plusHours(1), sala);
        acao("/reservas/" + rejeitada + "/rejeitar", token(Papel.RESPONSAVEL)).andExpect(status().isOk());

        reservar(ana, AMANHA_8H, AMANHA_8H.plusHours(1), sala).andExpect(status().isCreated());
    }

    @Test
    void alterarParaHorarioLivre() throws Exception {
        Recurso sala = sala("Sala 1");
        Long id = novaReserva(ana, AMANHA_8H, AMANHA_8H.plusHours(2), sala);

        alterar(id, ana, AMANHA_8H.plusHours(1), AMANHA_8H.plusHours(3), sala)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inicio").value(AMANHA_8H.plusHours(1).toString() + ":00"));
    }

    @Test
    void alterarParaHorarioOcupadoRetorna409() throws Exception {
        Recurso sala = sala("Sala 1");
        novaReserva(ana, AMANHA_8H, AMANHA_8H.plusHours(1), sala);
        Long id = novaReserva(ana, AMANHA_8H.plusHours(2), AMANHA_8H.plusHours(3), sala);

        alterar(id, ana, AMANHA_8H.plusMinutes(30), AMANHA_8H.plusHours(3), sala).andExpect(status().isConflict());
    }

    @Test
    void incluirRecursoRestritoNaAlteracaoVoltaParaAprovacao() throws Exception {
        Recurso sala = sala("Sala 1");
        Long id = novaReserva(ana, AMANHA_8H, AMANHA_8H.plusHours(1), sala);

        alterar(id, ana, AMANHA_8H, AMANHA_8H.plusHours(1), sala, salaRestrita("Lab 2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SOLICITADA"));
    }

    @Test
    void naoAlteraReservaEmUso() throws Exception {
        Recurso sala = sala("Sala 1");
        Long id = novaReserva(ana, AMANHA_8H, AMANHA_8H.plusHours(1), sala);
        acao("/reservas/" + id + "/iniciar", token(Papel.RESPONSAVEL)).andExpect(status().isOk());

        alterar(id, ana, AMANHA_8H, AMANHA_8H.plusHours(2), sala).andExpect(status().isConflict());
    }

    @Test
    void alterarComPeriodoInvalidoRetorna400() throws Exception {
        Recurso sala = sala("Sala 1");
        Long id = novaReserva(ana, AMANHA_8H, AMANHA_8H.plusHours(1), sala);

        alterar(id, ana, AMANHA_8H, AMANHA_8H.minusHours(1), sala).andExpect(status().isBadRequest());
    }

    @Test
    void reservaInexistenteRetorna404() throws Exception {
        consultar("/reservas/999", ana).andExpect(status().isNotFound());
        acao("/reservas/999/cancelar", ana).andExpect(status().isNotFound());
    }
}
